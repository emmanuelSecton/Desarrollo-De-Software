package ar.edu.utn.odonto.operacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public final class MonitorSaludAlertas {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorSaludAlertas.class);

    private final ObjectProvider<DataSource> dataSource;
    private final String healthUrl;
    private final String slug;
    private final AtomicBoolean appCaida = new AtomicBoolean();
    private final AtomicBoolean baseCaida = new AtomicBoolean();

    @Value("${alertas.webhook-url:}")
    private String webhookUrl;

    public MonitorSaludAlertas(ObjectProvider<DataSource> dataSource,
            @Value("${alertas.salud.url:${APP_BASE_URL:http://localhost:8080}/actuator/health}") String healthUrl,
            @Value("${consultorio.slug:}") String slug) {
        this.dataSource = dataSource;
        this.healthUrl = healthUrl;
        this.slug = slug;
    }

    @PostConstruct
    void configurar() {
        RegistroAlertas.configurarSlug(slug);
        RegistroAlertas.configurarWebhook(webhookUrl);
        RegistroAlertas.registrar(alerta -> LOG.info("alerta {} del consultorio {}",
                alerta.tipo().name(), alerta.slug()));
    }

    @Scheduled(
            fixedDelayString = "${alertas.salud.intervalo-ms:30000}",
            initialDelayString = "${alertas.salud.intervalo-ms:30000}")
    public void verificar() {
        boolean appSaludable = appSaludable();
        if (appSaludable) {
            appCaida.set(false);
        } else if (appCaida.compareAndSet(false, true)) {
            RegistroAlertas.alertar(TipoAlerta.APP_DOWN);
        }
        boolean baseSaludable = baseSaludable();
        if (baseSaludable) {
            baseCaida.set(false);
        } else if (baseCaida.compareAndSet(false, true)) {
            RegistroAlertas.alertar(TipoAlerta.BASE_DOWN);
        }
    }

    private boolean appSaludable() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            String cuerpo = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(1))
                    .build()
                    .send(peticion, BodyHandlers.ofString())
                    .body();
            return "UP".equals(new ObjectMapper().readTree(cuerpo).path("status").asText());
        } catch (Exception fallo) {
            return false;
        }
    }

    private boolean baseSaludable() {
        DataSource fuente = dataSource.getIfAvailable();
        if (fuente == null) {
            return false;
        }
        try (Connection conexion = fuente.getConnection(); Statement sentencia = conexion.createStatement()) {
            sentencia.execute("select 1");
            return true;
        } catch (Exception fallo) {
            return false;
        }
    }
}
