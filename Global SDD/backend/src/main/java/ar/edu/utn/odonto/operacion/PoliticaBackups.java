package ar.edu.utn.odonto.operacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PoliticaBackups {

    private static final Duration RETENCION = Duration.ofDays(30);
    private static final Pattern FECHA = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})T");

    private PoliticaBackups() {}

    public record Resultado(boolean correcto, String mensaje) {}

    public static boolean debeConservar(String nombreArchivo, Instant hoy) {
        Matcher coincidencia = FECHA.matcher(nombreArchivo);
        if (!coincidencia.find()) {
            return false;
        }
        Instant fecha = Instant.parse(coincidencia.group(1) + "T00:00:00Z");
        return !fecha.isBefore(hoy.minus(RETENCION));
    }

    public static Resultado verificar(int codigoSalida) {
        if (codigoSalida == 0) {
            return new Resultado(true, "backup correcto");
        }
        RegistroAlertas.alertar(TipoAlerta.BACKUP_FALLIDO);
        return new Resultado(false, "fallo de backup con codigo " + codigoSalida);
    }

    public static int aplicarRetencion(Path directorio, Instant hoy) {
        int eliminados = 0;
        try (Stream<Path> archivos = Files.list(directorio)) {
            for (Path archivo : archivos.filter(Files::isRegularFile)
                    .filter(ruta -> ruta.getFileName().toString().startsWith("consultorio_"))
                    .filter(ruta -> ruta.getFileName().toString().endsWith(".dump"))
                    .toList()) {
                if (!debeConservar(archivo.getFileName().toString(), hoy)) {
                    Files.delete(archivo);
                    eliminados++;
                }
            }
        } catch (IOException fallo) {
            throw new UncheckedIOException(fallo);
        }
        return eliminados;
    }
}
