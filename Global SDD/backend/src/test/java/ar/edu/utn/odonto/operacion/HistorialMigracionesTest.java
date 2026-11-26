package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class HistorialMigracionesTest {

    private static final String V1 = "f71904b1c7c2fed46bca7d6d6ef03f6b5b7d8f039732e4def008b4878478fa9f";
    private static final String V2 = "fba9447c52309b138ab381a14df6cb7544900e86d3a26319d48e056997d346da";
    private static final String CLAVE = "clave-ficticia-de-historial";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "hist_uno hist_dos")
                .withEnv("CONSULTORIO_HIST_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_HIST_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_HIST_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_HIST_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @Test
    void V1YV2NoSeEditanYConservanElPlaceholder() throws Exception {
        assertThat(hash("src/main/resources/db/migration/V1__baseline.sql")).isEqualTo(V1);
        assertThat(hash("src/main/resources/db/migration/V2__auditoria.sql")).isEqualTo(V2);
        String v2 = Files.readString(Path.of("src/main/resources/db/migration/V2__auditoria.sql"));
        assertThat(v2).contains("${runtime_role}").doesNotContain("app_runtime");
    }

    @Test
    void reaplicarFlywayNoReescribeElHistorialNiCambiaElRuntime() throws Exception {
        for (String slug : new String[] {"hist_uno", "hist_dos"}) {
            MigracionesC35.aplicar(contenedor, slug, CLAVE);
            MigracionesC35.aplicar(contenedor, slug, CLAVE);
            try (Connection conexion = DriverManager.getConnection(
                            MigracionesC35.urlBase(contenedor, slug), contenedor.getUsername(), contenedor.getPassword());
                    Statement sentencia = conexion.createStatement()) {
                assertThat(MigracionesC35.consultaLista(sentencia,
                        "select version from flyway_schema_history where success order by installed_rank"))
                        .containsExactly("1", "2");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_table_privilege('consultorio_" + slug + "_runtime', 'evento_auditoria', 'INSERT')::text"))
                        .isEqualTo("true");
            }
        }
    }

    @Test
    void existeUnVerificadorDeHistorialSinFlywayUndo() throws Exception {
        Path verificador = Path.of("..", "deploy", "bin", "verificar-historial.sh")
                .toAbsolutePath()
                .normalize();
        assertThat(verificador).exists();
        assertThat(Files.readString(verificador)).doesNotContain("flyway undo")
                .contains("V1__baseline.sql")
                .contains("V2__auditoria.sql");
        assertThat(Files.readString(Path.of("..", "deploy", "migraciones.sha256")
                .toAbsolutePath()
                .normalize()))
                .contains(V1)
                .contains(V2);
    }

    private static String hash(String ruta) throws Exception {
        byte[] datos = Files.readAllBytes(Path.of(ruta));
        return hexadecimal(MessageDigest.getInstance("SHA-256").digest(datos));
    }

    private static String hexadecimal(byte[] bytes) {
        StringBuilder resultado = new StringBuilder();
        for (byte b : bytes) {
            resultado.append(String.format("%02x", b & 0xff));
        }
        return resultado.toString();
    }
}
