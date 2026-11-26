package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class AlertasRuntimeTest {

    private final List<String> alertas = new ArrayList<>();

    @AfterEach
    void limpiar() {
        RegistroAlertas.registrar(null);
        RegistroAlertas.configurarWebhook(null);
        RegistroAlertas.configurarSlug(null);
    }

    @Test
    void healthCaidoEmiteAppDownConSlugYWebhook() throws Exception {
        AtomicReference<String> cuerpoWebhook = new AtomicReference<>("");
        HttpServer webhook = servidor("/webhook", intercambio -> {
            cuerpoWebhook.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(intercambio, "{}");
        });
        HttpServer health = servidor("/actuator/health",
                intercambio -> responder(intercambio, "{\"status\":\"DOWN\"}"));
        webhook.start();
        health.start();
        RegistroAlertas.configurarWebhook("http://127.0.0.1:" + webhook.getAddress().getPort() + "/webhook");
        RegistroAlertas.configurarSlug("consultorio_uno");
        RegistroAlertas.registrar(alerta -> alertas.add(alerta.mensaje() + "|" + alerta.slug()));
        try (PostgreSQLContainer<?> base = base()) {
            base.start();
            MonitorSaludAlertas monitor = monitor(base, "http://127.0.0.1:" + health.getAddress().getPort()
                    + "/actuator/health");

            monitor.verificar();

            assertThat(alertas).anyMatch(alerta -> alerta.contains(TipoAlerta.APP_DOWN.name())
                    && alerta.contains("consultorio_uno"));
            assertThat(cuerpoWebhook.get()).contains(TipoAlerta.APP_DOWN.name())
                    .contains("consultorio_uno")
                    .doesNotContain("telefono")
                    .doesNotContain("diagnostico");
        } finally {
            health.stop(0);
            webhook.stop(0);
        }
    }

    @Test
    void baseCaidaEmiteBaseDownFueraDelRollout() throws Exception {
        AtomicReference<String> cuerpoWebhook = new AtomicReference<>("");
        HttpServer webhook = servidor("/webhook", intercambio -> {
            cuerpoWebhook.set(new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            responder(intercambio, "{}");
        });
        HttpServer health = servidor("/actuator/health",
                intercambio -> responder(intercambio, "{\"status\":\"UP\"}"));
        webhook.start();
        health.start();
        RegistroAlertas.configurarWebhook("http://127.0.0.1:" + webhook.getAddress().getPort() + "/webhook");
        RegistroAlertas.configurarSlug("consultorio_dos");
        RegistroAlertas.registrar(alerta -> alertas.add(alerta.mensaje() + "|" + alerta.slug()));
        PostgreSQLContainer<?> base = base();
        base.start();
        MonitorSaludAlertas monitor = monitor(base, "http://127.0.0.1:" + health.getAddress().getPort()
                + "/actuator/health");
        try {
            base.stop();

            monitor.verificar();

            assertThat(alertas).anyMatch(alerta -> alerta.contains(TipoAlerta.BASE_DOWN.name())
                    && alerta.contains("consultorio_dos"));
            assertThat(cuerpoWebhook.get()).contains(TipoAlerta.BASE_DOWN.name()).contains("consultorio_dos");
        } finally {
            health.stop(0);
            webhook.stop(0);
        }
    }

    private static PostgreSQLContainer<?> base() {
        return new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withInitScript("db/init/auditoria-roles.sql");
    }

    private static MonitorSaludAlertas monitor(PostgreSQLContainer<?> base, String healthUrl) {
        DataSource dataSource = new DriverManagerDataSource(
                base.getJdbcUrl(), base.getUsername(), base.getPassword());
        DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
        beans.registerSingleton("dataSource", dataSource);
        ObjectProvider<DataSource> proveedor = beans.getBeanProvider(DataSource.class);
        return new MonitorSaludAlertas(proveedor, healthUrl, "consultorio_prueba");
    }

    private interface Manejador {
        void manejar(HttpExchange intercambio) throws IOException;
    }

    private static HttpServer servidor(String ruta, Manejador manejador) throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(ruta, manejador::manejar);
        return servidor;
    }

    private static void responder(HttpExchange intercambio, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.sendResponseHeaders(200, bytes.length);
        intercambio.getResponseBody().write(bytes);
        intercambio.close();
    }
}
