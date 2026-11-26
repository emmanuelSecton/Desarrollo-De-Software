package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

final class MigracionesC35 {

    private MigracionesC35() {}

    static String urlBase(PostgreSQLContainer<?> contenedor, String slug) {
        return url(contenedor, "consultorio_" + slug);
    }

    static String url(PostgreSQLContainer<?> contenedor, String base) {
        return contenedor.getJdbcUrl().replace("/" + contenedor.getDatabaseName(), "/" + base);
    }

    static void aplicar(PostgreSQLContainer<?> contenedor, String slug, String password) {
        String base = "consultorio_" + slug;
        Flyway.configure()
                .dataSource(urlBase(contenedor, slug), base + "_migrator", password)
                .locations("filesystem:src/main/resources/db/migration")
                .placeholders(Map.of("runtime_role", base + "_runtime"))
                .load()
                .migrate();
    }

    static Connection administrador(PostgreSQLContainer<?> contenedor) throws Exception {
        return DriverManager.getConnection(contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
    }

    static String consultaUnica(Statement sentencia, String sql) throws Exception {
        try (ResultSet resultados = sentencia.executeQuery(sql)) {
            assertThat(resultados.next()).isTrue();
            return resultados.getString(1);
        }
    }

    static List<String> consultaLista(Statement sentencia, String sql) throws Exception {
        try (ResultSet resultados = sentencia.executeQuery(sql)) {
            java.util.List<String> valores = new java.util.ArrayList<>();
            while (resultados.next()) {
                valores.add(resultados.getString(1));
            }
            return valores;
        }
    }
}
