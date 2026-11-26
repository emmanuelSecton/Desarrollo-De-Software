package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class InicializacionNoExclusivaTest {

    private static final String CLAVE = "clave-ficticia-inicializacion";

    @Test
    void conLegacyYSlugsCreaTodasLasBasesYTodosLosRoles() throws Exception {
        try (PostgreSQLContainer<?> contenedor = contenedor()
                .withEnv("APP_DB_NAME", "consultorio_legacy")
                .withEnv("APP_DB_MIGRATOR_USER", "legacy_migrator")
                .withEnv("APP_DB_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("APP_DB_USER", "legacy_runtime")
                .withEnv("APP_DB_PASSWORD", CLAVE)) {
            contenedor.start();
            assertThat(bases(contenedor)).contains("consultorio_legacy", "consultorio_ini_uno", "consultorio_ini_dos");
            assertThat(roles(contenedor)).contains("legacy_migrator", "legacy_runtime",
                    "consultorio_ini_uno_migrator", "consultorio_ini_uno_runtime",
                    "consultorio_ini_dos_migrator", "consultorio_ini_dos_runtime");
        }
    }

    @Test
    void sinVariablesLegacyLaBaseLegacyPorDefectoTambienExiste() throws Exception {
        try (PostgreSQLContainer<?> contenedor = contenedor()) {
            contenedor.start();
            assertThat(bases(contenedor)).contains("consultorio", "consultorio_ini_uno", "consultorio_ini_dos");
            assertThat(roles(contenedor)).contains("app_migrator", "app_runtime",
                    "consultorio_ini_uno_runtime", "consultorio_ini_dos_runtime");
            try (Connection conexion = DriverManager.getConnection(
                            MigracionesC35.url(contenedor, "consultorio"),
                            contenedor.getUsername(), contenedor.getPassword());
                    Statement sentencia = conexion.createStatement()) {
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_database_privilege('app_runtime', 'consultorio', 'CONNECT')::text"))
                        .isEqualTo("true");
            }
        }
    }

    private static PostgreSQLContainer<?> contenedor() {
        return new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", "ini_uno ini_dos")
                .withEnv("CONSULTORIO_INI_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_INI_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_INI_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_INI_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
    }

    private static List<String> bases(PostgreSQLContainer<?> contenedor) throws Exception {
        try (Connection conexion = MigracionesC35.administrador(contenedor);
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaLista(sentencia,
                    "select datname from pg_database where datname like 'consultorio%' order by datname");
        }
    }

    private static List<String> roles(PostgreSQLContainer<?> contenedor) throws Exception {
        try (Connection conexion = MigracionesC35.administrador(contenedor);
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaLista(sentencia,
                    "select rolname from pg_roles where rolname like '%migrator' or rolname like '%runtime' order by rolname");
        }
    }
}
