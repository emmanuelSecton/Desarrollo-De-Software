package ar.edu.utn.odonto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/** Verifica los roles del init script de despliegue sobre PostgreSQL 16 real (C-02, D2). */
class RolesInicialesTest {

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciarBaseConElScriptDeDespliegue() {
        Path script = Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                .toAbsolutePath()
                .normalize();
        assertThat(script).exists();

        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("APP_DB_NAME", "consultorio")
                .withEnv("APP_DB_MIGRATOR_USER", "app_migrator")
                .withEnv("APP_DB_MIGRATOR_PASSWORD", "clave-ficticia-de-prueba-migrator")
                .withEnv("APP_DB_USER", "app_runtime")
                .withEnv("APP_DB_PASSWORD", "clave-ficticia-de-prueba-runtime")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(script, 0755), "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
    }

    @AfterAll
    static void detenerBase() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @Test
    void laBaseEsPropiedadDelMigratorYElRuntimeNoTieneDdl() throws Exception {
        try (Connection conexion = DriverManager.getConnection(
                        contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {

            assertThat(consultaUnica(sentencia,
                    "select pg_get_userbyid(datdba) from pg_database where datname = 'consultorio'"))
                    .isEqualTo("app_migrator");

            List<String> roles = consultaLista(sentencia,
                    "select rolname from pg_roles where rolname in ('app_migrator', 'app_runtime') order by rolname");
            assertThat(roles).containsExactly("app_migrator", "app_runtime");

            assertThat(consultaUnica(sentencia,
                    "select rolcanlogin::text from pg_roles where rolname = 'app_runtime'"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_database_privilege('app_runtime', 'consultorio', 'CONNECT')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_schema_privilege('app_runtime', 'public', 'USAGE')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_schema_privilege('app_runtime', 'public', 'CREATE')::text"))
                    .isEqualTo("false");
        }
    }

    @Test
    void elEjemploDeEntornoDocumentaLosDosRolesSinSecretos() throws Exception {
        Path ejemplo = Path.of("..", ".env.example").toAbsolutePath().normalize();
        String contenido = Files.readString(ejemplo);

        assertThat(contenido).contains("APP_DB_MIGRATOR_USER=").contains("APP_DB_MIGRATOR_PASSWORD=");
        assertThat(contenido).contains("APP_DB_USER=").contains("APP_DB_PASSWORD=");
        assertThat(contenido).contains("app_runtime");
        assertThat(contenido).doesNotContain("clave-ficticia-de-prueba");
    }

    private static String consultaUnica(Statement sentencia, String sql) throws Exception {
        try (ResultSet resultados = sentencia.executeQuery(sql)) {
            assertThat(resultados.next()).isTrue();
            return resultados.getString(1);
        }
    }

    private static List<String> consultaLista(Statement sentencia, String sql) throws Exception {
        try (ResultSet resultados = sentencia.executeQuery(sql)) {
            List<String> valores = new ArrayList<>();
            while (resultados.next()) {
                valores.add(resultados.getString(1));
            }
            return valores;
        }
    }
}
