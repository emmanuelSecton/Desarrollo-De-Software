package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConfiguracionComposeTest {

    private static final ObjectMapper MAPEADOR = new ObjectMapper();

    @Test
    void cadaConsultorioTieneSuServicioYcaddyEnrutaPorHttps() throws Exception {
        JsonNode servicios = configuracion().get("services");
        assertThat(servicios.has("app_demo_uno")).isTrue();
        assertThat(servicios.has("app_demo_dos")).isTrue();
        assertThat(servicios.has("caddy")).isTrue();

        String caddy = Files.readString(Path.of("..", "deploy", "caddy", "Caddyfile")
                .toAbsolutePath()
                .normalize());
        assertThat(caddy).contains("demo-uno.{$ODONTO_DOMINIO}")
                .contains("demo-dos.{$ODONTO_DOMINIO}")
                .contains("reverse_proxy app_demo_uno:8080")
                .contains("reverse_proxy app_demo_dos:8080");
        assertThat(servicios.get("caddy").get("ports").toString()).contains("443");
    }

    @Test
    void laBaseNoSePublicaFueraDelHostEnLaDemo() throws Exception {
        JsonNode postgres = configuracion().get("services").get("postgres");
        assertThat(postgres.has("ports")).isFalse();
        String override = Files.readString(Path.of("..", "deploy", "docker-compose.override.yml")
                .toAbsolutePath()
                .normalize());
        assertThat(override).doesNotContain("\"5432:5432\"").doesNotContain(":5432:5432");
    }

    @Test
    void cadaAppDependeDeSuMigracionTerminada() throws Exception {
        JsonNode servicios = configuracion().get("services");
        for (String slug : new String[] {"demo_uno", "demo_dos"}) {
            JsonNode app = servicios.get("app_" + slug);
            assertThat(app.get("depends_on").get("migrate_" + slug).get("condition").asText())
                    .isEqualTo("service_completed_successfully");
        }
    }

    private static JsonNode configuracion() throws Exception {
        ProcessBuilder proceso = new ProcessBuilder(
                "docker", "compose", "-f", "deploy/docker-compose.yml", "config", "--format", "json");
        proceso.directory(Path.of("..").toAbsolutePath().normalize().toFile());
        Process resultado = proceso.start();
        StringBuilder salida = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(resultado.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                salida.append(linea);
            }
        }
        assertThat(resultado.waitFor()).isZero();
        return MAPEADOR.readTree(salida.toString());
    }
}
