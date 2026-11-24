package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AppPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ConfiguracionApp.class);

    @Test
    void conBaseUrlYSinZonaUsaBuenosAiresPorDefecto() {
        runner.withPropertyValues("app.base-url=https://consultorio.ejemplo.com.ar", "app.db-url=jdbc:postgresql://h/d")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    AppProperties props = ctx.getBean(AppProperties.class);
                    assertThat(props.baseUrl()).isEqualTo("https://consultorio.ejemplo.com.ar");
                    assertThat(props.zonaHoraria()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
                });
    }

    @Test
    void conZonaValidaLaRespeta() {
        runner.withPropertyValues(
                        "app.base-url=https://x.ar", "app.db-url=jdbc:postgresql://h/d", "app.timezone=Europe/Madrid")
                .run(ctx -> assertThat(ctx.getBean(AppProperties.class).zonaHoraria())
                        .isEqualTo(ZoneId.of("Europe/Madrid")));
    }

    @Test
    void sinAppBaseUrlElArranqueFallaNombrandoLaVariable() {
        runner.withPropertyValues("app.db-url=jdbc:postgresql://h/d").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(mensajes(ctx.getStartupFailure())).contains("APP_BASE_URL").doesNotContain("DB_URL");
        });
    }

    @Test
    void conAppBaseUrlEnBlancoElArranqueFallaNombrandoLaVariable() {
        runner.withPropertyValues("app.base-url=   ", "app.db-url=jdbc:postgresql://h/d")
                .run(ctx -> assertThat(mensajes(ctx.getStartupFailure())).contains("APP_BASE_URL"));
    }

    @Test
    void sinDbUrlElArranqueFallaNombrandoLaVariable() {
        runner.withPropertyValues("app.base-url=https://x.ar").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(mensajes(ctx.getStartupFailure())).contains("DB_URL").doesNotContain("APP_BASE_URL");
        });
    }

    @Test
    void conZonaInvalidaElArranqueFallaNombrandoLaVariableYElValor() {
        runner.withPropertyValues(
                        "app.base-url=https://x.ar", "app.db-url=jdbc:postgresql://h/d", "app.timezone=Marte/Olympus")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(mensajes(ctx.getStartupFailure())).contains("APP_TIMEZONE").contains("Marte/Olympus");
                });
    }

    /** Concatena los mensajes de toda la cadena de causas (el binder envuelve el motivo real). */
    private static String mensajes(Throwable error) {
        StringBuilder acumulado = new StringBuilder();
        for (Throwable t = error; t != null; t = t.getCause()) {
            acumulado.append(t.getMessage()).append(System.lineSeparator());
        }
        return acumulado.toString();
    }
}
