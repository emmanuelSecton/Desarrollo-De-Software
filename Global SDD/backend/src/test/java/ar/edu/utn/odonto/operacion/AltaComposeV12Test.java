package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AltaComposeV12Test {

    @TempDir
    Path temporal;

    @ParameterizedTest
    @ValueSource(strings = {"alta_uno", "clinica_22"})
    void elComposeDinamicoDeclaraMigrateFlywayYLimites(String slug) throws Exception {
        AltaConsultorio.PlanAlta plan = plan(slug);
        AltaConsultorio.escribirEnv(plan);
        Path compose = AltaConsultorio.escribirCompose(plan);

        JsonNode servicios = configuracion(plan, compose).get("services");
        JsonNode migrate = servicios.get("migrate_" + slug);
        assertThat(migrate).as("migrate_" + slug).isNotNull();
        assertThat(migrate.get("image").asText()).isEqualTo("flyway/flyway:11.7.2");
        JsonNode entornoMigrate = migrate.get("environment");
        assertThat(entornoMigrate.get("FLYWAY_URL").asText())
                .isEqualTo("jdbc:postgresql://postgres:5432/consultorio_" + slug);
        assertThat(entornoMigrate.get("FLYWAY_USER").asText()).isEqualTo("consultorio_" + slug + "_migrator");
        assertThat(entornoMigrate.get("FLYWAY_PLACEHOLDERS_RUNTIME_ROLE").asText())
                .isEqualTo("consultorio_" + slug + "_runtime");
        assertThat(entornoMigrate.get("FLYWAY_LOCATIONS").asText()).isEqualTo("filesystem:/flyway/sql");
        assertThat(migrate.get("command").get(0).asText()).isEqualTo("migrate");
        assertThat(migrate.get("volumes").get(0).get("target").asText()).isEqualTo("/flyway/sql");
        assertThat(migrate.get("volumes").get(0).get("read_only").asBoolean()).isTrue();
        assertThat(migrate.get("depends_on").get("postgres").get("condition").asText())
                .isEqualTo("service_healthy");
        assertThat(migrate.get("security_opt").toString()).contains("no-new-privileges:true");
        assertThat(migrate.get("cpus").asDouble()).isEqualTo(0.25);
        assertThat(migrate.get("mem_limit").asLong()).isEqualTo(256L * 1024 * 1024);

        JsonNode app = servicios.get("app_" + slug);
        assertThat(app).as("app_" + slug).isNotNull();
        assertThat(app.get("environment").get("SPRING_FLYWAY_ENABLED").asText()).isEqualTo("false");
        assertThat(app.get("depends_on").get("postgres").get("condition").asText())
                .isEqualTo("service_healthy");
        assertThat(app.get("depends_on").get("migrate_" + slug).get("condition").asText())
                .isEqualTo("service_completed_successfully");
        assertThat(app.get("security_opt").toString()).contains("no-new-privileges:true");
        assertThat(app.get("cpus").asDouble()).isEqualTo(1.0);
        assertThat(app.get("mem_limit").asLong()).isEqualTo(768L * 1024 * 1024);
    }

    private AltaConsultorio.PlanAlta plan(String slug) {
        Map<String, String> secretos = new LinkedHashMap<>();
        secretos.put("ADMIN_INITIAL_EMAIL", "odontologo@ejemplo.invalid");
        secretos.put("APP_DB_PASSWORD", "clave-ficticia-runtime");
        secretos.put("APP_DB_MIGRATOR_PASSWORD", "clave-ficticia-migrator");
        secretos.put("POSTGRES_PASSWORD", "clave-ficticia-postgres");
        secretos.put("ODONTO_IMAGEN", "odonto:1.0.0");
        return AltaConsultorio.planificar(slug, "demo.example.com", temporal, secretos);
    }

    private static JsonNode configuracion(AltaConsultorio.PlanAlta plan, Path compose) throws Exception {
        List<String> argumentos = new ArrayList<>(List.of("docker", "compose", "--env-file",
                plan.envFile().toString(), "-f", "deploy/docker-compose.yml", "-f", compose.toString(),
                "config", "--format", "json"));
        ProcessBuilder proceso = new ProcessBuilder(argumentos);
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
        return new ObjectMapper().readTree(salida.toString());
    }
}
