package ar.edu.utn.odonto.operacion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RegistroAlertas {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroAlertas.class);
    private static final Set<TipoAlerta> DIFERIDAS = EnumSet.of(TipoAlerta.WA_TOKEN_EXPIRADO);

    private static volatile Consumer<Alerta> destino;
    private static volatile String webhook = valorEnv("ALERTAS_WEBHOOK_URL");
    private static volatile String slug = valorEnv("CONSULTORIO_SLUG");

    private RegistroAlertas() {}

    public record Alerta(boolean disparada, TipoAlerta tipo, String slug, String mensaje) {}

    public static void registrar(Consumer<Alerta> nuevoDestino) {
        destino = nuevoDestino;
    }

    public static void configurarWebhook(String url) {
        webhook = url == null || url.isBlank() ? "" : url.trim();
    }

    public static void configurarSlug(String identificador) {
        slug = identificador == null || identificador.isBlank() ? "" : identificador.trim();
    }

    public static Alerta alertar(TipoAlerta tipo) {
        if (DIFERIDAS.contains(tipo)) {
            return new Alerta(false, tipo, "", "");
        }
        Alerta alerta = new Alerta(true, tipo, slug == null ? "" : slug, "alerta técnica: " + tipo.name());
        Consumer<Alerta> actual = destino;
        if (actual != null) {
            actual.accept(alerta);
        }
        emitirWebhook(alerta);
        return alerta;
    }

    private static void emitirWebhook(Alerta alerta) {
        String url = webhook;
        if (url == null || url.isBlank()) {
            return;
        }
        String cuerpo = "{\"tipo\":\"" + alerta.tipo().name()
                + "\",\"slug\":\"" + escapar(alerta.slug())
                + "\",\"mensaje\":\"" + escapar(alerta.mensaje()) + "\"}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(cuerpo))
                .build();
        try {
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build()
                    .send(peticion, BodyHandlers.discarding());
        } catch (Exception fallo) {
            LOG.warn("no se pudo emitir la alerta {} por webhook", alerta.tipo().name());
        }
    }

    private static String escapar(String valor) {
        return valor == null ? "" : valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String valorEnv(String clave) {
        String valor = System.getenv(clave);
        return valor == null ? "" : valor;
    }
}
