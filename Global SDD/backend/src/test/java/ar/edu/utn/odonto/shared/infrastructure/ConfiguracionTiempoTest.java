package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.shared.application.ZonaHorariaConsultorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

class ConfiguracionTiempoTest {

    private static final Instant FIJO = Instant.parse("2026-11-24T13:00:00Z");

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionApp.class, ConfiguracionTiempo.class)
            .withPropertyValues("app.base-url=https://x.ar", "app.db-url=jdbc:postgresql://h/d");

    /** Componente cualquiera que pide la hora: debe obtenerla del Clock. */
    record ConsumidorDeHora(Clock clock) {
        Instant ahora() {
            return Instant.now(clock);
        }
    }

    @Configuration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(FIJO, ZoneOffset.UTC);
        }

        @Bean
        ConsumidorDeHora consumidor(Clock clock) {
            return new ConsumidorDeHora(clock);
        }
    }

    @Test
    void elClockPorDefectoEsElRelojDelSistemaEnUtc() {
        runner.run(ctx -> {
            Clock clock = ctx.getBean(Clock.class);
            assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
            assertThat(Duration.between(Instant.now(clock), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        });
    }

    @Test
    void unTestPuedeFijarElRelojEnUnInstanteDeterminado() {
        runner.withUserConfiguration(RelojFijo.class)
                .run(ctx -> assertThat(ctx.getBean(ConsumidorDeHora.class).ahora()).isEqualTo(FIJO));
    }

    @Test
    void laZonaDelConsultorioSaleDeAppTimezoneYPorDefectoEsBuenosAires() {
        runner.run(ctx -> {
            ZonaHorariaConsultorio zona = ctx.getBean(ZonaHorariaConsultorio.class);
            assertThat(zona.zona()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
            assertThat(zona.aLocal(FIJO)).isEqualTo(LocalDateTime.parse("2026-11-24T10:00"));
        });
        runner.withPropertyValues("app.timezone=Europe/Madrid")
                .run(ctx -> assertThat(ctx.getBean(ZonaHorariaConsultorio.class).aLocal(FIJO))
                        .isEqualTo(LocalDateTime.parse("2026-11-24T14:00")));
    }
}
