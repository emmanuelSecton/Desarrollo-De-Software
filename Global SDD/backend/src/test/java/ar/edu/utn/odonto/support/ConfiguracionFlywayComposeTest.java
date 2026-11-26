package ar.edu.utn.odonto.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

/** Verifica que el compose separe las credenciales Flyway (migrator) de las de la app (runtime). */
class ConfiguracionFlywayComposeTest {

    private static final Path BASE = Path.of(".");

    @Test
    void elComposeDaALaAppSoloLasCredencialesRuntime() throws Exception {
        String compose = leer("..", "deploy", "docker-compose.yml");

        assertThat(compose).contains("DB_USER: ${APP_DB_USER:-}");
        assertThat(compose).contains("DB_PASSWORD: ${APP_DB_PASSWORD:-}");
        assertThat(compose).doesNotContain("SPRING_FLYWAY_URL").doesNotContain("SPRING_FLYWAY_USER")
                .doesNotContain("SPRING_FLYWAY_PASSWORD");
    }

    @Test
    void elPerfilDeTestNoFijaCredencialesFlywayParaNoRomperServiceConnection() throws Exception {
        String aplicacion = leer("src", "main", "resources", "application.yml");
        String test = leer("src", "test", "resources", "application-test.yml");

        assertThat(aplicacion).doesNotContain("user: ${SPRING_FLYWAY_USER").doesNotContain("user: ${FLYWAY_USER");
        assertThat(aplicacion).doesNotContain("SPRING_FLYWAY_ENABLED");
        // Solo importa Flyway: otras propiedades (por ejemplo management.health.mail.enabled, C-04) pueden ser false.
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new FileSystemResource(BASE.resolve("src/main/resources/application.yml")));
        assertThat(yaml.getObject().getProperty("spring.flyway.enabled")).isNull();
        assertThat(test).doesNotContain("flyway:").doesNotContain("username:").doesNotContain("password:");
    }

    @Test
    void laMigracionDeAuditoriaNoFijaElRolRuntimeEnElSql() throws Exception {
        String migracion = leer("src", "main", "resources", "db", "migration", "V2__auditoria.sql");
        String aplicacion = leer("src", "main", "resources", "application.yml");

        assertThat(migracion).contains("${runtime_role}").doesNotContain("app_runtime");
        assertThat(aplicacion).contains("\"[runtime_role]\": ${APP_DB_USER:app_runtime}");
    }

    @Test
    void ambosPermisosDelMigradorUsanElMismoPlaceholder() throws Exception {
        String migracion = leer("src", "main", "resources", "db", "migration", "V2__auditoria.sql");

        assertThat(migracion)
                .contains("ON SEQUENCE evento_auditoria_id_seq TO ${runtime_role}")
                .contains("ON evento_auditoria TO ${runtime_role}");
    }

    @Test
    void elServicioMigrateCorreFlywayYLaAppSoloRecibeCredencialesRuntime() throws Exception {
        String compose = leer("..", "deploy", "docker-compose.yml");

        assertThat(compose).contains("  migrate:");
        assertThat(compose).contains("image: flyway/flyway:11.7.2");
        assertThat(compose).contains("FLYWAY_USER: ${APP_DB_MIGRATOR_USER:-}");
        assertThat(compose).contains("FLYWAY_PASSWORD: ${APP_DB_MIGRATOR_PASSWORD:-}");
        assertThat(compose).contains("FLYWAY_PLACEHOLDERS_RUNTIME_ROLE: ${APP_DB_USER:-app_runtime}");
        assertThat(compose).contains("../backend/src/main/resources/db/migration:/flyway/sql:ro");
        assertThat(compose).contains("condition: service_completed_successfully");
        assertThat(compose).contains("SPRING_FLYWAY_ENABLED: \"false\"");
        assertThat(compose).doesNotContain("SPRING_FLYWAY_URL").doesNotContain("SPRING_FLYWAY_USER")
                .doesNotContain("SPRING_FLYWAY_PASSWORD");
    }

    @Test
    void elEjemploDeEntornoDocumentaBaseMigratorYRuntime() throws Exception {
        String ejemplo = leer("..", ".env.example");

        assertThat(ejemplo)
                .contains("APP_DB_NAME=")
                .contains("APP_DB_MIGRATOR_USER=")
                .contains("APP_DB_MIGRATOR_PASSWORD=")
                .contains("APP_DB_USER=")
                .contains("APP_DB_PASSWORD=")
                .contains("FLYWAY_PLACEHOLDERS_RUNTIME_ROLE")
                .contains("app_runtime");
    }

    @Test
    void elRepositorioFuerzaEolLfParaLosScripts() throws Exception {
        Path atributos = Path.of("..", ".gitattributes").toAbsolutePath().normalize();

        assertThat(atributos).exists();
        assertThat(Files.readAllLines(atributos)).map(String::trim).contains("*.sh text eol=lf");
    }

    @Test
    void elScriptDeInicializacionYaEstaEnLf() throws Exception {
        Path script = Path.of("..", "deploy", "postgres", "init", "01-base-consultorio.sh")
                .toAbsolutePath()
                .normalize();
        String contenido = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);

        assertThat(contenido).doesNotContain("\r");
        assertThat(contenido.lines()).anyMatch(linea -> linea.startsWith("#!/bin/bash"));
    }

    private static String leer(String... partes) throws Exception {
        Path ruta = BASE;
        for (String parte : partes) {
            ruta = ruta.resolve(parte);
        }
        return Files.readString(ruta.toAbsolutePath().normalize());
    }
}
