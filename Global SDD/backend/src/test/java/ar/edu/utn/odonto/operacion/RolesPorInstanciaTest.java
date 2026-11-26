package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class RolesPorInstanciaTest {

    private static final String CLAVE = "clave-ficticia-de-prueba-por-instancia";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciarConSlugsDeDemo() throws Exception {
        Path script = Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                .toAbsolutePath()
                .normalize();
        assertThat(script).exists();
        assertThat(Files.readString(script)).doesNotContain("\r");

        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "demo_uno demo_dos")
                .withEnv("CONSULTORIO_DEMO_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_DEMO_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(
                        MountableFile.forHostPath(script, 0755), "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
        aplicarMigraciones("demo_uno", "consultorio_demo_uno_migrator", "consultorio_demo_uno_runtime");
        aplicarMigraciones("demo_dos", "consultorio_demo_dos_migrator", "consultorio_demo_dos_runtime");
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo_uno", "demo_dos"})
    void laBaseYLosRolesSiguenLaConvencionPorSlug(String slug) throws Exception {
        String base = "consultorio_" + slug;
        String migrador = base + "_migrator";
        String runtime = base + "_runtime";
        String otra = "consultorio_" + (slug.equals("demo_uno") ? "demo_dos" : "demo_uno");

        try (Connection conexion = administrador();
                Statement sentencia = conexion.createStatement()) {
            assertThat(consultaUnica(sentencia,
                    "select pg_get_userbyid(datdba) from pg_database where datname = '" + base + "'"))
                    .isEqualTo(migrador);
            assertThat(consultaUnica(sentencia,
                    "select count(*) from pg_roles where rolname in ('" + migrador + "', '" + runtime + "')"))
                    .isEqualTo("2");
            assertThat(consultaUnica(sentencia,
                    "select rolcanlogin::text from pg_roles where rolname = '" + runtime + "'"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_database_privilege('" + runtime + "', '" + base + "', 'CONNECT')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_database_privilege('" + runtime + "', '" + otra + "', 'CONNECT')::text"))
                    .isEqualTo("false");
            assertThat(consultaUnica(sentencia,
                    "select has_database_privilege('public', '" + base + "', 'CONNECT')::text"))
                    .isEqualTo("false");
            assertThat(consultaUnica(sentencia,
                    "select has_schema_privilege('" + runtime + "', 'public', 'USAGE')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_schema_privilege('" + runtime + "', 'public', 'CREATE')::text"))
                    .isEqualTo("false");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo_uno", "demo_dos"})
    void elRuntimeNoPuedeModificarNiBorrarAuditoria(String slug) throws Exception {
        String base = "consultorio_" + slug;
        String runtime = base + "_runtime";
        String url = contenedor.getJdbcUrl().replace("/" + contenedor.getDatabaseName(), "/" + base);

        try (Connection conexion = DriverManager.getConnection(url, runtime, CLAVE);
                Statement sentencia = conexion.createStatement()) {
            assertThat(consultaUnica(sentencia,
                    "select has_table_privilege(current_user, 'evento_auditoria', 'SELECT')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_table_privilege(current_user, 'evento_auditoria', 'INSERT')::text"))
                    .isEqualTo("true");
            assertThat(consultaUnica(sentencia,
                    "select has_table_privilege(current_user, 'evento_auditoria', 'UPDATE')::text"))
                    .isEqualTo("false");
            assertThat(consultaUnica(sentencia,
                    "select has_table_privilege(current_user, 'evento_auditoria', 'DELETE')::text"))
                    .isEqualTo("false");
            assertThatThrownBy(() -> sentencia.executeUpdate(
                    "update evento_auditoria set accion = 'CAMBIO' where id = 1"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sentencia.executeUpdate("delete from evento_auditoria"))
                    .isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() -> sentencia.executeUpdate("truncate evento_auditoria"))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    private static void aplicarMigraciones(String slug, String migrador, String runtime) {
        String base = "consultorio_" + slug;
        String url = contenedor.getJdbcUrl().replace("/" + contenedor.getDatabaseName(), "/" + base);
        Flyway.configure()
                .dataSource(url, migrador, CLAVE)
                .locations("filesystem:src/main/resources/db/migration")
                .placeholders(Map.of("runtime_role", runtime))
                .load()
                .migrate();
    }

    private static Connection administrador() throws Exception {
        return DriverManager.getConnection(contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
    }

    private static String consultaUnica(Statement sentencia, String sql) throws Exception {
        try (ResultSet resultados = sentencia.executeQuery(sql)) {
            assertThat(resultados.next()).isTrue();
            return resultados.getString(1);
        }
    }
}
