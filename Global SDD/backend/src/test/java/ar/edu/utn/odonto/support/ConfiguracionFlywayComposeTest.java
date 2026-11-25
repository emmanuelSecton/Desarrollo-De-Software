package ar.edu.utn.odonto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifica que el compose separe las credenciales Flyway (migrator) de las de la app (runtime). */
class ConfiguracionFlywayComposeTest {

    private static final Path BASE = Path.of(".");

    @Test
    void elComposeUsaElMigratorParaFlywayYElRuntimeParaLaAplicacion() throws Exception {
        String compose = leer("..", "deploy", "docker-compose.yml");

        assertThat(compose).contains("SPRING_FLYWAY_URL:");
        assertThat(compose).contains("SPRING_FLYWAY_USER: ${APP_DB_MIGRATOR_USER:-}");
        assertThat(compose).contains("SPRING_FLYWAY_PASSWORD: ${APP_DB_MIGRATOR_PASSWORD:-}");
        assertThat(compose).contains("DB_USER: ${APP_DB_USER:-}");
        assertThat(compose).contains("DB_PASSWORD: ${APP_DB_PASSWORD:-}");
    }

    @Test
    void elPerfilDeTestNoFijaCredencialesFlywayParaNoRomperServiceConnection() throws Exception {
        String aplicacion = leer("src", "main", "resources", "application.yml");
        String test = leer("src", "test", "resources", "application-test.yml");

        assertThat(aplicacion).doesNotContain("user: ${SPRING_FLYWAY_USER").doesNotContain("user: ${FLYWAY_USER");
        assertThat(test).doesNotContain("flyway:").doesNotContain("username:").doesNotContain("password:");
    }

    private static String leer(String... partes) throws Exception {
        Path ruta = BASE;
        for (String parte : partes) {
            ruta = ruta.resolve(parte);
        }
        return Files.readString(ruta.toAbsolutePath().normalize());
    }
}
