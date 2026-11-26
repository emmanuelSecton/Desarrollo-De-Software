package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class MigratePorBaseTest {

    private static final String CLAVE = "clave-ficticia-de-migracion";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        Path script = Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                .toAbsolutePath()
                .normalize();
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "migra_uno migra_dos")
                .withEnv("CONSULTORIO_MIGRA_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_MIGRA_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_MIGRA_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_MIGRA_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(script, 0755), "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"migra_uno", "migra_dos"})
    void cadaBaseAplicaV1YV2ConSuPropioRuntime(String slug) throws Exception {
        MigracionesC35.aplicar(contenedor, slug, CLAVE);
        String base = "consultorio_" + slug;

        try (Connection conexion = DriverManager.getConnection(
                        MigracionesC35.urlBase(contenedor, slug), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            assertThat(MigracionesC35.consultaLista(sentencia,
                    "select version from flyway_schema_history where success order by installed_rank"))
                    .containsExactly("1", "2");
            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select has_table_privilege('" + base + "_runtime', 'evento_auditoria', 'INSERT')::text"))
                    .isEqualTo("true");
            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select has_table_privilege('" + base + "_runtime', 'evento_auditoria', 'UPDATE')::text"))
                    .isEqualTo("false");
        }
    }

    @Test
    void laAppNoEjecutaFlywayYEperaAlMigrateDeSuBase() throws Exception {
        JsonNode servicios = configuracion().get("services");
        for (String nombre : new String[] {"app_demo_uno", "app_demo_dos"}) {
            JsonNode app = servicios.get(nombre);
            assertThat(app.get("environment").get("SPRING_FLYWAY_ENABLED").asText()).isEqualTo("false");
            assertThat(app.get("depends_on").toString()).contains("service_completed_successfully");
        }
    }

    @Test
    void existeUnScriptDeMigracionPorBase() throws Exception {
        String contenido = Files.readString(Path.of("..", "deploy", "bin", "migrar-base.sh")
                .toAbsolutePath()
                .normalize());
        assertThat(contenido).contains("flyway/flyway:11.7.2")
                .contains("FLYWAY_PLACEHOLDERS_RUNTIME_ROLE")
                .contains("APP_DB_USER")
                .contains("service_completed_successfully");
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
        return new ObjectMapper().readTree(salida.toString());
    }
}
