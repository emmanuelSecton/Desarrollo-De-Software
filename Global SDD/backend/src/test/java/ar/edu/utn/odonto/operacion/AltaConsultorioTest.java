package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AltaConsultorioTest {

    @TempDir
    Path temporal;

    private static Map<String, String> secretos(String correo) {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("ADMIN_INITIAL_EMAIL", correo);
        valores.put("APP_DB_PASSWORD", "clave-ficticia-runtime");
        valores.put("APP_DB_MIGRATOR_PASSWORD", "clave-ficticia-migrator");
        valores.put("WA_VERIFY_TOKEN", "");
        return valores;
    }

    @Test
    void elAltaPreparaLaInstanciaSinCrearEntidadesDeNegocio() throws Exception {
        AltaConsultorio.PlanAlta plan = AltaConsultorio.planificar(
                "demo_uno", "demo.example.com", temporal, secretos("odontologo@ejemplo.invalid"));

        assertThat(plan.slug()).isEqualTo("demo_uno");
        assertThat(plan.base()).isEqualTo("consultorio_demo_uno");
        assertThat(plan.migrador()).isEqualTo("consultorio_demo_uno_migrator");
        assertThat(plan.runtime()).isEqualTo("consultorio_demo_uno_runtime");
        assertThat(plan.subdominio()).isEqualTo("demo-uno.demo.example.com");
        assertThat(plan.envFile()).isEqualTo(temporal.resolve("demo_uno.env"));
        assertThat(plan.comandos()).anyMatch(c -> c.contains("--env-file") && c.contains("demo_uno.env"));
        assertThat(plan.comandos()).anyMatch(c -> c.contains("app_demo_uno"));
        assertThat(plan.comandos()).anyMatch(c -> c.contains("/actuator/health"));

        String texto = String.join(" ", plan.comandos()).toLowerCase();
        assertThat(texto).doesNotContain("insert into").doesNotContain("wa_verify_token=")
                .doesNotContain("create user").doesNotContain("create consultorio");
    }

    @Test
    void elEnvFileEsEstableYNoGuardaCredencialesDeWhatsApp() throws Exception {
        AltaConsultorio.PlanAlta plan = AltaConsultorio.planificar(
                "clinica_2", "demo.example.com", temporal, secretos("otro@ejemplo.invalid"));

        Path escrito = AltaConsultorio.escribirEnv(plan);
        String primero = Files.readString(escrito);

        assertThat(primero).contains("ADMIN_INITIAL_EMAIL=otro@ejemplo.invalid")
                .contains("DB_POOL_MAX_SIZE=5")
                .contains("APP_DB_USER=consultorio_clinica_2_runtime")
                .contains("WA_VERIFY_TOKEN=");
        assertThat(primero).doesNotContain("WA_VERIFY_TOKEN=clave");
        assertThat(escrito.toString()).doesNotContain("Global SDD");

        AltaConsultorio.escribirEnv(plan);
        assertThat(Files.readString(escrito)).isEqualTo(primero);
    }

    @Test
    void rechazaSlugsQueNoCumplenLaConvencion() {
        assertThatThrownBy(() -> AltaConsultorio.planificar(
                "Demo Uno", "demo.example.com", temporal, secretos("x@ejemplo.invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("slug");
        assertThatThrownBy(() -> AltaConsultorio.planificar(
                "a", "demo.example.com", temporal, secretos("x@ejemplo.invalid")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(AltaConsultorio.planificar(
                "centro_salud_7", "demo.example.com", temporal, secretos("x@ejemplo.invalid"))
                .base()).isEqualTo("consultorio_centro_salud_7");
    }

    @Test
    void existeUnCliShellQueDelegueEnLaMismaLogica() throws Exception {
        Path cli = Path.of("..", "deploy", "bin", "alta-consultorio.sh").toAbsolutePath().normalize();
        assertThat(cli).exists();
        String contenido = Files.readString(cli);
        assertThat(contenido).contains("ar.edu.utn.odonto.operacion.AltaConsultorio");
        assertThat(contenido).contains("ODONTO_APPLY");
        assertThat(contenido).contains("set -euo pipefail");
        assertThat(contenido).doesNotContain("latest");
    }

    @Test
    void elPuntoDeEntradaDelCliEscribeElEnv() throws Exception {
        Path destino = temporal.resolve("desde_cli");
        AltaConsultorio.main(new String[] {"cli_uno", "demo.example.com", destino.toString()});
        assertThat(destino.resolve("cli_uno.env")).exists();
    }

    @Test
    void verificaSaludDelContenedorComoJsonUp() throws Exception {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/actuator/health", intercambio -> {
            byte[] cuerpo = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(200, cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        servidor.start();
        try {
            String url = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/actuator/health";
            JsonNode respuesta = new ObjectMapper().readTree(AltaConsultorio.verificarSalud(url));
            assertThat(respuesta.get("status").asText()).isEqualTo("UP");
        } finally {
            servidor.stop(0);
        }
    }
}
