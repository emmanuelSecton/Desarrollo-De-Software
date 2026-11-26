package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class AislamientoBasesTest {

    private static final String CLAVE = "clave-ficticia-de-aislamiento";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        Path script = Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                .toAbsolutePath()
                .normalize();
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "demo_a demo_b")
                .withEnv("CONSULTORIO_DEMO_A_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_A_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_B_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_B_RUNTIME_PASSWORD", CLAVE)
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
    @CsvSource({"demo_a, consultorio_demo_b", "demo_b, consultorio_demo_a"})
    void elRuntimeSeConectaALaPropiaBasePeroNoALaAjena(String slug, String baseAjena) throws Exception {
        String base = "consultorio_" + slug;
        String runtime = base + "_runtime";

        try (Connection conexion = conectar(runtime, base);
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("select current_user, current_database()")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getString(1)).isEqualTo(runtime);
            assertThat(resultado.getString(2)).isEqualTo(base);
        }

        assertThatThrownBy(() -> conectar(runtime, baseAjena).close())
                .isInstanceOf(java.sql.SQLException.class);
    }

    @Test
    void publicNoTieneConnectEnNingunaBase() throws Exception {
        try (Connection conexion = DriverManager.getConnection(
                        contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            for (String slug : new String[] {"demo_a", "demo_b"}) {
                try (ResultSet resultado = sentencia.executeQuery("select has_database_privilege('public', 'consultorio_"
                        + slug + "', 'CONNECT')::text")) {
                    assertThat(resultado.next()).isTrue();
                    assertThat(resultado.getString(1)).isEqualTo("false");
                }
            }
        }
    }

    @Test
    void existeUnVerificadorDeAislamientoQueUsaLasMismasReglas() throws Exception {
        Path verificador = Path.of("..", "deploy", "bin", "verificar-aislamiento.sh")
                .toAbsolutePath()
                .normalize();
        assertThat(verificador).exists();
        String contenido = Files.readString(verificador);
        assertThat(contenido).contains("has_database_privilege")
                .contains("public")
                .contains("CONNECT")
                .contains("CONSULTORIO_SLUGS");
    }

    private static Connection conectar(String usuario, String base) throws Exception {
        String url = contenedor.getJdbcUrl().replace("/" + contenedor.getDatabaseName(), "/" + base);
        return DriverManager.getConnection(url, usuario, CLAVE);
    }
}
