package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/** Permisos minimos del runtime sobre PostgreSQL real (C-02, D2). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class PermisosRuntimeTest {

    private static final String USUARIO = "app_runtime";
    private static final String CLAVE = "clave-ficticia-de-prueba-runtime";

    @Autowired
    DataSource dataSource;

    @Test
    void elRuntimePuedeSeleccionarEInsertarConLaSecuencia() throws Exception {
        try (Connection conexion = conectarRuntime(); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("insert into evento_auditoria "
                    + "(ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle) values "
                    + "('2026-11-24T13:00:00Z', 'SISTEMA', null, 'LOGIN', 'SISTEMA', 'arranque', '{}'::jsonb)");

            try (var resultado = sentencia.executeQuery(
                    "select count(*) from evento_auditoria where entidad_id = 'arranque'")) {
                assertThat(resultado.next()).isTrue();
                assertThat(resultado.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    void elRuntimeNoPuedeActualizarNiBorrar() throws Exception {
        try (Connection conexion = conectarRuntime(); Statement sentencia = conexion.createStatement()) {
            assertPermisoDenegado(() -> sentencia.executeUpdate(
                    "update evento_auditoria set accion = 'EXPORTAR' where entidad_id = 'arranque'"));
            assertPermisoDenegado(() -> sentencia.executeUpdate(
                    "delete from evento_auditoria where entidad_id = 'arranque'"));
        }
    }

    @Test
    void elRuntimeNoHaceDdlYFlywayNoCorreConElRuntime() throws Exception {
        try (Connection conexion = conectarRuntime(); Statement sentencia = conexion.createStatement()) {
            assertPermisoDenegado(() -> sentencia.executeUpdate(
                    "create table intento_ddl_runtime (id bigint)"));
        }

        String instaladoPor = new JdbcTemplate(dataSource).queryForObject(
                "select installed_by from flyway_schema_history where version = '2'", String.class);
        assertThat(instaladoPor).isNotEqualTo(USUARIO);
    }

    private static Connection conectarRuntime() throws SQLException {
        PostgreSQLContainer<?> contenedor = PostgresTestConfig.POSTGRES;
        return DriverManager.getConnection(contenedor.getJdbcUrl(), USUARIO, CLAVE);
    }

    @FunctionalInterface
    private interface Sentencia {
        void ejecutar() throws SQLException;
    }

    private static void assertPermisoDenegado(Sentencia sentencia) {
        assertThatThrownBy(sentencia::ejecutar)
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("permission denied");
    }
}
