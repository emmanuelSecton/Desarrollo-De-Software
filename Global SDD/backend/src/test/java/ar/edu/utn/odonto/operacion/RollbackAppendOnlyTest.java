package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class RollbackAppendOnlyTest {

    private static final String CLAVE = "clave-ficticia-de-rollback";
    private static final String SLUG = "rollback_uno";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", SLUG)
                .withEnv("CONSULTORIO_ROLLBACK_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_ROLLBACK_UNO_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
        MigracionesC35.aplicar(contenedor, SLUG, CLAVE);
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @Test
    void rollbackConservaAuditoriaYRegistraLaCorreccionComoEventoNuevo() throws Exception {
        try (Connection conexion = runtime(); Statement sentencia = conexion.createStatement()) {
            insertar(sentencia, "TURNO_CREADO", "turno", "turno-1", "2026-01-02T03:04:05Z");

            try (Connection administrador = DriverManager.getConnection(
                            MigracionesC35.urlBase(contenedor, SLUG), contenedor.getUsername(), contenedor.getPassword());
                    Statement sentenciaAdmin = administrador.createStatement()) {
                assertThatThrownBy(() -> sentenciaAdmin.executeUpdate(
                        "update evento_auditoria set accion = 'BORRADO' where entidad_id = 'turno-1'"))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("append-only");
                assertThatThrownBy(() -> sentenciaAdmin.executeUpdate(
                        "delete from evento_auditoria where entidad_id = 'turno-1'"))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("append-only");
            }

            insertar(sentencia, "ENMIENDA", "turno", "turno-1", "2026-01-02T04:00:00Z");

            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select count(*)::text from evento_auditoria where entidad_id = 'turno-1'"))
                    .isEqualTo("2");
            assertThat(MigracionesC35.consultaLista(sentencia,
                    "select accion from evento_auditoria where entidad_id = 'turno-1' order by id"))
                    .containsExactly("TURNO_CREADO", "ENMIENDA");
        }
    }

    @Test
    void elRunbookVuelveAlDigestAnteriorConPgDumpSinComandoUndo() throws Exception {
        String runbook = Files.readString(Path.of("..", "deploy", "RUNBOOK-operacion.md")
                .toAbsolutePath()
                .normalize());
        assertThat(runbook).contains("pg_dump")
                .contains("digest")
                .contains("rollback.sh")
                .doesNotContain("flyway undo")
                .doesNotContain("UPDATE evento_auditoria")
                .doesNotContain("DELETE FROM evento_auditoria")
                .doesNotContain("TRUNCATE evento_auditoria");
    }

    @Test
    void elScriptDeRollbackUsaTagODigestAnteriorYNoTocaAuditoria() throws Exception {
        String script = Files.readString(Path.of("..", "deploy", "bin", "rollback.sh")
                .toAbsolutePath()
                .normalize());
        assertThat(script).contains("pg_dump")
                .contains("ODONTO_IMAGEN_ANTERIOR")
                .contains("set -euo pipefail")
                .doesNotContain("TRUNCATE")
                .doesNotContain("DELETE FROM evento_auditoria")
                .doesNotContain("UPDATE evento_auditoria");
    }

    private static Connection runtime() throws SQLException {
        return DriverManager.getConnection(
                MigracionesC35.urlBase(contenedor, SLUG), "consultorio_" + SLUG + "_runtime", CLAVE);
    }

    private static void insertar(Statement sentencia, String accion, String entidad, String id, String ocurrido)
            throws SQLException {
        sentencia.executeUpdate("insert into evento_auditoria "
                + "(ocurrido_at, actor_tipo, accion, entidad, entidad_id, detalle) values ("
                + "timestamp '" + ocurrido + "+00', 'SISTEMA', '" + accion + "', '" + entidad + "', '" + id
                + "', '{}'::jsonb)");
    }
}
