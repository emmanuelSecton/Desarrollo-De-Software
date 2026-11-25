package ar.edu.utn.odonto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

/** El init script de tests crea app_runtime sobre la MISMA URL del contenedor singleton. */
class ConexionRuntimeTest {

    static final String USUARIO = "app_runtime";
    static final String CLAVE = "clave-ficticia-de-prueba-runtime";

    @Test
    void elRolRuntimeAbreUnaSegundaConexionALaMismaBase() throws Exception {
        try (Connection conexion = conectar();
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery("select current_user")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getString(1)).isEqualTo(USUARIO);
        }
    }

    @Test
    void elRolRuntimeTieneUSAGEEnPublicPeroNoDDL() throws Exception {
        try (Connection conexion = conectar();
                Statement sentencia = conexion.createStatement();
                ResultSet resultado = sentencia.executeQuery(
                        "select has_schema_privilege(current_user, 'public', 'USAGE'), "
                                + "has_schema_privilege(current_user, 'public', 'CREATE')")) {
            assertThat(resultado.next()).isTrue();
            assertThat(resultado.getBoolean(1)).isTrue();
            assertThat(resultado.getBoolean(2)).isFalse();
        }
    }

    private static Connection conectar() throws Exception {
        PostgreSQLContainer<?> contenedor = PostgresTestConfig.POSTGRES;
        return DriverManager.getConnection(contenedor.getJdbcUrl(), USUARIO, CLAVE);
    }
}
