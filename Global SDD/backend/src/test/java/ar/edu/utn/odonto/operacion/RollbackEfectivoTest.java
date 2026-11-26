package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class RollbackEfectivoTest {

    private static final String CLAVE = "clave-ficticia-rollback-efectivo";
    private static final String SLUG = "rb_efectivo";
    private static final String BASE = "consultorio_" + SLUG;

    @Test
    void restauraDumpAnteriorYVolteaLaImagen() throws Exception {
        try (PostgreSQLContainer<?> contenedor = contenedor()) {
            contenedor.start();
            MigracionesC35.aplicar(contenedor, SLUG, CLAVE);
            insertar(contenedor, "MARCA_ANTERIOR");
            ejecutar(contenedor, "pg_dump -U " + contenedor.getUsername() + " -d " + BASE
                    + " -Fc -f /tmp/rb-anterior.dump");
            insertar(contenedor, "CAMBIO_POSTERIOR");
            List<String> imagenes = new ArrayList<>();

            RollbackDespliegue.Resultado resultado = RollbackDespliegue.ejecutar(
                    SLUG, "/tmp/rb-anterior.dump", "odonto:1.2.3", puerto(contenedor, imagenes),
                    restaurador(contenedor));

            assertThat(resultado.correcto()).as(resultado.mensaje()).isTrue();
            assertThat(imagenes).containsExactly("odonto:1.2.3");
            assertThat(contenedor.execInContainer("/bin/sh", "-c", "test -s /tmp/rollback-actual.dump")
                    .getExitCode()).isEqualTo(0);
            try (Connection conexion = DriverManager.getConnection(
                            MigracionesC35.urlBase(contenedor, SLUG), contenedor.getUsername(), contenedor.getPassword());
                    Statement sentencia = conexion.createStatement()) {
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select count(*)::text from evento_auditoria where detalle->>'marca' = 'MARCA_ANTERIOR'"))
                        .isEqualTo("1");
                assertThat(MigracionesC35.consultaUnica(sentencia,
                        "select count(*)::text from evento_auditoria where detalle->>'marca' = 'CAMBIO_POSTERIOR'"))
                        .isEqualTo("0");
            }
        }
    }

    @Test
    void sinImagenAnteriorNoRestaurNiDespliega() throws Exception {
        try (PostgreSQLContainer<?> contenedor = contenedor()) {
            contenedor.start();
            MigracionesC35.aplicar(contenedor, SLUG, CLAVE);
            insertar(contenedor, "MARCA_ANTERIOR");
            List<String> imagenes = new ArrayList<>();
            boolean[] restauro = {false};
            RestauracionConsultorio.Restaurador restaurador = new RestauracionConsultorio.Restaurador() {
                @Override
                public void restaurar(String base, String archivo) {
                    restauro[0] = true;
                }

                @Override
                public void reaplicarGrants(String base, String slug) {
                    restauro[0] = true;
                }
            };

            RollbackDespliegue.Resultado resultado = RollbackDespliegue.ejecutar(
                    SLUG, "/tmp/rb-anterior.dump", "", puerto(contenedor, imagenes), restaurador);

            assertThat(resultado.correcto()).isFalse();
            assertThat(resultado.mensaje()).contains("ODONTO_IMAGEN_ANTERIOR");
            assertThat(imagenes).isEmpty();
            assertThat(restauro[0]).isFalse();
        }
    }

    private static PostgreSQLContainer<?> contenedor() {
        return new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", SLUG)
                .withEnv("CONSULTORIO_RB_EFECTIVO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_RB_EFECTIVO_RUNTIME_PASSWORD", CLAVE)
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
                    + "timestamptz '2026-01-02 03:04:05+00', 'SISTEMA', 'MARCA', 'prueba', 'rollback', "
                    + "'{\"marca\": \"" + marca + "\"}'::jsonb)");
        }
    }

    private static RollbackDespliegue.Port puerto(PostgreSQLContainer<?> contenedor, List<String> imagenes) {
        return new RollbackDespliegue.Port() {
            @Override
            public void dump(String base, String archivo) {
                ejecutar(contenedor, "pg_dump -U " + contenedor.getUsername() + " -d " + BASE
                        + " -Fc -f " + archivo);
            }

            @Override
            public void imagen(String slug, String tag) {
                imagenes.add(tag);
            }
        };
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
                        + " -c \"GRANT USAGE ON SCHEMA public TO consultorio_" + slug + "_runtime\""
                        + " -c \"GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA public TO consultorio_"
                        + slug + "_runtime\"");
            }
        };
    }

    private static void ejecutar(PostgreSQLContainer<?> contenedor, String comando) {
        try {
            var respuesta = contenedor.execInContainer("/bin/sh", "-c", comando);
            assertThat(respuesta.getExitCode()).as(respuesta.getStderr()).isEqualTo(0);
        } catch (Exception fallo) {
            throw new IllegalStateException(fallo);
        }
    }
}
