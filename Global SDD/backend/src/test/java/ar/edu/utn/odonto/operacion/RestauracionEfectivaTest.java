package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class RestauracionEfectivaTest {

    private static final String CLAVE = "clave-ficticia-restore-efectivo";
    private static final String SLUG = "efectivo_uno";
    private static final String BASE = "consultorio_" + SLUG;

    @Test
    void restauraSobreLaBaseRealDelSlugYRecuperaGrantsDelRuntime() throws Exception {
        try (PostgreSQLContainer<?> contenedor = contenedor()) {
            contenedor.start();
            MigracionesC35.aplicar(contenedor, SLUG, CLAVE);
            insertar(contenedor, "MARCA_ORIGINAL");

            contenedor.execInContainer("/bin/sh", "-c", "pg_dump -U " + contenedor.getUsername()
                    + " -d " + BASE + " -Fc -f /tmp/efectivo.dump");
            insertar(contenedor, "CAMBIO_POST_DUMP");

            RestauracionConsultorio.Resultado resultado = RestauracionConsultorio.ejecutar(
                    SLUG, "/tmp/efectivo.dump", restaurador(contenedor));
            assertThat(resultado.correcto()).isTrue();

            try (Connection conexion = DriverManager.getConnection(
                            MigracionesC35.urlBase(contenedor, SLUG), contenedor.getUsername(), contenedor.getPassword());
                    Statement sentencia = conexion.createStatement()) {
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select count(*)::text from evento_auditoria where detalle->>'marca' = 'MARCA_ORIGINAL'"))
                        .isEqualTo("1");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select count(*)::text from evento_auditoria where detalle->>'marca' = 'CAMBIO_POST_DUMP'"))
                        .isEqualTo("0");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_database_privilege('consultorio_" + SLUG + "_runtime', '" + BASE + "', 'CONNECT')::text"))
                        .isEqualTo("true");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_schema_privilege('consultorio_" + SLUG + "_runtime', 'public', 'USAGE')::text"))
                        .isEqualTo("true");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_table_privilege('consultorio_" + SLUG + "_runtime', 'evento_auditoria', 'INSERT')::text"))
                        .isEqualTo("true");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select has_sequence_privilege('consultorio_" + SLUG
                                + "_runtime', 'evento_auditoria_id_seq', 'USAGE')::text"))
                        .isEqualTo("true");
            }
        }
    }

    @Test
    void rechazaUnSlugQueNoPerteneceALaInstancia() {
        RestauracionConsultorio.Restaurador nunca = new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {
                throw new AssertionError("no debe restaurar");
            }

            @Override
            public void reaplicarGrants(String base, String slug) {
                throw new AssertionError("no debe reaplicar grants");
            }
        };
        RestauracionConsultorio.Resultado resultado = RestauracionConsultorio.ejecutar(
                "slugInvalido", "/tmp/efectivo.dump", nunca);
        assertThat(resultado.correcto()).isFalse();
        assertThat(resultado.mensaje()).contains("slug");
    }

    @Test
    void elScriptUsaCleanYReaplicaGrantsCompletos() throws Exception {
        String script = java.nio.file.Files.readString(Path.of("..", "deploy", "bin", "restaurar-consultorio.sh")
                .toAbsolutePath()
                .normalize());
        assertThat(script).contains("--clean")
                .contains("GRANT USAGE ON SCHEMA public")
                .contains("GRANT SELECT, INSERT ON ALL TABLES")
                .contains("GRANT USAGE, SELECT ON ALL SEQUENCES");
    }

    private static PostgreSQLContainer<?> contenedor() {
        return new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", SLUG)
                .withEnv("CONSULTORIO_EFECTIVO_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_EFECTIVO_UNO_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
    }

    private static void insertar(PostgreSQLContainer<?> contenedor, String marca) throws Exception {
        try (Connection conexion = DriverManager.getConnection(
                        MigracionesC35.urlBase(contenedor, SLUG), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("insert into evento_auditoria "
                    + "(ocurrido_at, actor_tipo, accion, entidad, entidad_id, detalle) values ("
                    + "timestamptz '2026-01-02 03:04:05+00', 'SISTEMA', 'MARCA', 'prueba', 'restore', "
                    + "'{\"marca\": \"" + marca + "\"}'::jsonb)");
        }
    }

    private static RestauracionConsultorio.Restaurador restaurador(PostgreSQLContainer<?> contenedor) {
        return new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {
                ejecutar(contenedor, "pg_restore --clean --if-exists --exit-on-error -U "
                        + contenedor.getUsername() + " -d " + base + " " + archivo);
            }

            @Override
            public void reaplicarGrants(String base, String slug) {
                ejecutar(contenedor, "psql -v ON_ERROR_STOP=1 -U " + contenedor.getUsername() + " -d " + base
                        + " -c \"REVOKE CONNECT ON DATABASE \\\"" + base + "\\\" FROM PUBLIC\""
                        + " -c \"GRANT CONNECT ON DATABASE \\\"" + base + "\\\" TO consultorio_" + slug + "_runtime\""
                        + " -c \"GRANT USAGE ON SCHEMA public TO consultorio_" + slug + "_runtime\""
                        + " -c \"GRANT SELECT, INSERT ON evento_auditoria TO consultorio_" + slug + "_runtime\""
                        + " -c \"GRANT USAGE, SELECT ON SEQUENCE evento_auditoria_id_seq TO consultorio_"
                        + slug + "_runtime\"");
            }
        };
    }

    private static void ejecutar(PostgreSQLContainer<?> contenedor, String comando) {
        try {
            var resultado = contenedor.execInContainer("/bin/sh", "-c", comando);
            assertThat(resultado.getExitCode()).as(resultado.getStderr()).isEqualTo(0);
        } catch (Exception fallo) {
            throw new IllegalStateException(fallo);
        }
    }
}
