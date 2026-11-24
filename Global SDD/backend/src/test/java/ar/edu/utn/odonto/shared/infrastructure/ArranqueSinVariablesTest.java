package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.OdontoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Arranque real de la aplicacion (application.yml verdadero, sin perfil test) sin las variables
 * obligatorias. Sin base: se excluye la autoconfiguracion de datos para fallar solo por la variable.
 * Requiere que DB_URL, APP_BASE_URL y APP_TIMEZONE no esten definidas en el entorno del proceso.
 */
class ArranqueSinVariablesTest {

    private static final String SIN_BASE = "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration";

    private static String arrancarYCapturar(String... propiedades) {
        try {
            new SpringApplicationBuilder(OdontoApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties(propiedades)
                    .run()
                    .close();
        } catch (Exception error) {
            StringBuilder acumulado = new StringBuilder();
            for (Throwable t = error; t != null; t = t.getCause()) {
                acumulado.append(t.getMessage()).append(System.lineSeparator());
            }
            return acumulado.toString();
        }
        throw new AssertionError("El arranque debio fallar y termino correctamente");
    }

    @Test
    void sinAppBaseUrlElArranqueRealFallaNombrandoLaVariable() {
        assertThat(arrancarYCapturar(SIN_BASE, "DB_URL=jdbc:postgresql://h/d")).contains("APP_BASE_URL");
    }

    @Test
    void sinDbUrlElArranqueRealFallaNombrandoLaVariable() {
        assertThat(arrancarYCapturar(SIN_BASE, "APP_BASE_URL=https://x.ar")).contains("DB_URL");
    }

    @Test
    void conZonaInvalidaElArranqueRealFallaNombrandoAppTimezone() {
        assertThat(arrancarYCapturar(
                        SIN_BASE,
                        "DB_URL=jdbc:postgresql://h/d",
                        "APP_BASE_URL=https://x.ar",
                        "APP_TIMEZONE=Marte/Olympus"))
                .contains("APP_TIMEZONE");
    }
}
