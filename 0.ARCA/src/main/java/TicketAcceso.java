import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Properties;

/**
 * Ticket de Acceso (TA) emitido por WSAA.
 *
 * AFIP no emite un TA nuevo mientras el anterior siga vigente (error
 * coe.alreadyAuthenticated), así que se guarda en disco y se reutiliza.
 * El archivo contiene credenciales: no debe subirse al repositorio.
 */
public record TicketAcceso(
        String token,
        String sign,
        OffsetDateTime expiracion,
        String servicio,
        String certId) {

    /** Vigente para ese servicio y certificado, con un margen antes del vencimiento. */
    public boolean esValidoPara(String servicio, String certId, Duration margen) {
        return this.servicio.equals(servicio)
                && this.certId.equals(certId)
                && OffsetDateTime.now().plus(margen).isBefore(expiracion);
    }

    /** Lee el TA guardado. Si no existe o está dañado, devuelve vacío. */
    public static Optional<TicketAcceso> cargar(Path archivo) {
        if (!Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            p.load(r);
            String token = p.getProperty("token");
            String sign = p.getProperty("sign");
            String expiracion = p.getProperty("expiracion");
            String servicio = p.getProperty("servicio");
            String certId = p.getProperty("certId");
            if (vacio(token) || vacio(sign) || vacio(expiracion) || vacio(servicio) || vacio(certId)) {
                System.err.println("Aviso: el TA guardado en " + archivo + " está incompleto; se ignora.");
                return Optional.empty();
            }
            return Optional.of(new TicketAcceso(token, sign, OffsetDateTime.parse(expiracion), servicio, certId));
        } catch (IOException | IllegalArgumentException | DateTimeParseException e) {
            System.err.println("Aviso: no se pudo leer el TA guardado en " + archivo + " (" + e.getMessage() + "); se ignora.");
            return Optional.empty();
        }
    }

    /** Guarda el TA. Escribe a un temporal y lo mueve, para no dejar un archivo a medias. */
    public void guardar(Path archivo) throws IOException {
        Properties p = new Properties();
        p.setProperty("token", token);
        p.setProperty("sign", sign);
        p.setProperty("expiracion", expiracion.toString());
        p.setProperty("servicio", servicio);
        p.setProperty("certId", certId);

        Path absoluto = archivo.toAbsolutePath();
        Path tmp = absoluto.resolveSibling(absoluto.getFileName() + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            p.store(w, "Ticket de Acceso WSAA - NO SUBIR AL REPOSITORIO");
        }
        try {
            Files.move(tmp, absoluto, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, absoluto, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** No muestra token ni sign para que no terminen en logs. */
    @Override
    public String toString() {
        return "TicketAcceso[servicio=" + servicio + ", expiracion=" + expiracion + "]";
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
