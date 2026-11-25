package ar.edu.utn.odonto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

/** El init script de tests crea app_runtime sobre la MISMA URL del contenedor singleton. */
public class ConexionRuntimeTest {

    public static final String USUARIO = "app_runtime";
    public static final String CLAVE = "clave-ficticia-de-prueba-runtime";

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
    void elDataSourceRuntimeEstaDisponibleParaElRepositorio() throws Exception {
        try (Connection conexion = dataSourceRuntime().getConnection()) {
            assertThat(conexion.getMetaData().getUserName()).isEqualTo(USUARIO);
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

    public static DataSource dataSourceRuntime() {
        PostgreSQLContainer<?> contenedor = PostgresTestConfig.POSTGRES;
        DriverManagerDataSource fuente = new DriverManagerDataSource(contenedor.getJdbcUrl(), USUARIO, CLAVE);
        fuente.setDriverClassName("org.postgresql.Driver");
        return fuente;
    }

    private static Connection conectar() throws Exception {
        PostgreSQLContainer<?> contenedor = PostgresTestConfig.POSTGRES;
        return DriverManager.getConnection(contenedor.getJdbcUrl(), USUARIO, CLAVE);
    }
}
