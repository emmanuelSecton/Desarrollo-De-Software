package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class RestoreUnConsultorioTest {

    private static final String CLAVE = "clave-ficticia-de-restore";
    private static final String UNO = "rest_uno";
    private static final String DOS = "rest_dos";
    private static final String RESTAURADA = "consultorio_rest_uno_restaurada";

    private static PostgreSQLContainer<?> contenedor;

    @BeforeAll
    static void iniciar() throws Exception {
        contenedor = new PostgreSQLContainer<>(DockerImageName.parse(PostgresTestConfig.IMAGEN))
                .withEnv("CONSULTORIO_SLUGS", UNO + " " + DOS)
                .withEnv("CONSULTORIO_REST_UNO_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_REST_UNO_RUNTIME_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_REST_DOS_MIGRATOR_PASSWORD", CLAVE)
                .withEnv("CONSULTORIO_REST_DOS_RUNTIME_PASSWORD", CLAVE)
                .withCopyFileToContainer(MountableFile.forHostPath(
                                Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                                        .toAbsolutePath()
                                        .normalize(),
                                0755),
                        "/docker-entrypoint-initdb.d/01-base-consultorio.sh");
        contenedor.start();
        for (String slug : new String[] {UNO, DOS}) {
            MigracionesC35.aplicar(contenedor, slug, CLAVE);
        }
        insertarMarca(UNO, "marca-uno");
        insertarMarca(DOS, "marca-dos");
    }

    @AfterAll
    static void detener() {
        if (contenedor != null) {
            contenedor.stop();
        }
    }

    @Test
    void restaurarUnaBaseNoTocaLaOtraYConservaRolesYGrants() throws Exception {
        contenedor.execInContainer("/bin/sh", "-c",
                "pg_dump -U " + contenedor.getUsername() + " -d consultorio_" + UNO + " > /tmp/restore-uno.sql");

        try (Connection administrador = MigracionesC35.administrador(contenedor);
                Statement sentencia = administrador.createStatement()) {
            sentencia.executeUpdate("CREATE DATABASE " + RESTAURADA);
            sentencia.executeUpdate("GRANT CONNECT ON DATABASE " + RESTAURADA
                    + " TO consultorio_" + UNO + "_runtime");
            sentencia.executeUpdate("REVOKE CONNECT ON DATABASE " + RESTAURADA + " FROM PUBLIC");
        }

        ExecResult resultado = contenedor.execInContainer("/bin/sh", "-c", "psql -v ON_ERROR_STOP=1 -U "
                + contenedor.getUsername() + " -d " + RESTAURADA + " -f /tmp/restore-uno.sql");
        assertThat(resultado.getExitCode()).isEqualTo(0);

        try (Connection restaurada = DriverManager.getConnection(
                        MigracionesC35.url(contenedor, RESTAURADA), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = restaurada.createStatement()) {
            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select detalle->>'marca' from evento_auditoria limit 1")).isEqualTo("marca-uno");
            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select has_database_privilege('consultorio_" + UNO + "_runtime', '"
                            + RESTAURADA + "', 'CONNECT')::text")).isEqualTo("true");
        }

        try (Connection vecina = DriverManager.getConnection(
                        MigracionesC35.urlBase(contenedor, DOS), "consultorio_" + DOS + "_runtime", CLAVE);
                Statement sentencia = vecina.createStatement()) {
            assertThat(MigracionesC35.consultaUnica(sentencia,
                    "select detalle->>'marca' from evento_auditoria limit 1")).isEqualTo("marca-dos");
        }
    }

    @Test
    void elScriptRestauraSoloLaBaseDelSlugSinTocarLasDemas() throws Exception {
        String script = Files.readString(Path.of("..", "deploy", "bin", "restaurar-consultorio.sh")
                .toAbsolutePath()
                .normalize());
        assertThat(script).contains("pg_restore")
                .contains("REVOKE CONNECT ON DATABASE")
                .contains("GRANT CONNECT ON DATABASE")
                .contains("set -euo pipefail")
                .contains("CONSULTORIO_SLUGS")
                .doesNotContain("TRUNCATE");
    }

    private static void insertarMarca(String slug, String marca) throws SQLException {
        try (Connection conexion = DriverManager.getConnection(
                        MigracionesC35.urlBase(contenedor, slug), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("insert into evento_auditoria "
                    + "(ocurrido_at, actor_tipo, accion, entidad, entidad_id, detalle) values ("
                    + "timestamp '2026-01-02T03:04:05Z+00', 'SISTEMA', 'MARCA', 'prueba', 'restore', "
                    + "'{\"marca\": \"" + marca + "\"}'::jsonb)");
        }
    }
}
