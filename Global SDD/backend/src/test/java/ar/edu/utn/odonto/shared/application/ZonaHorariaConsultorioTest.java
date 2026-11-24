package ar.edu.utn.odonto.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class ZonaHorariaConsultorioTest {

    private static final Instant TRECE_UTC = Instant.parse("2026-11-24T13:00:00Z");

    private final ZonaHorariaConsultorio buenosAires =
            new ZonaHorariaConsultorio(ZoneId.of("America/Argentina/Buenos_Aires"));

    @Test
    void convierteUtcAHoraLocalDeBuenosAires() {
        assertThat(buenosAires.aLocal(TRECE_UTC)).isEqualTo(LocalDateTime.parse("2026-11-24T10:00"));
    }

    @Test
    void volverALocalYDeVueltaDevuelveElMismoInstante() {
        LocalDateTime local = buenosAires.aLocal(TRECE_UTC);

        assertThat(buenosAires.aInstante(local)).isEqualTo(TRECE_UTC);
    }

    @Test
    void laHoraLocalSeInterpretaEnLaZonaDelConsultorioNoEnLaDelHost() {
        assertThat(buenosAires.aInstante(LocalDateTime.parse("2026-11-24T10:00"))).isEqualTo(TRECE_UTC);
    }

    @Test
    void respetaLasReglasDeHorarioDeVeranoDeLaZonaConfigurada() {
        ZonaHorariaConsultorio madrid = new ZonaHorariaConsultorio(ZoneId.of("Europe/Madrid"));

        assertThat(madrid.aLocal(Instant.parse("2026-01-15T12:00:00Z")))
                .isEqualTo(LocalDateTime.parse("2026-01-15T13:00"));
        assertThat(madrid.aLocal(Instant.parse("2026-07-15T12:00:00Z")))
                .isEqualTo(LocalDateTime.parse("2026-07-15T14:00"));
    }

    @Test
    void exponeLaZonaYLaConversionConOffset() {
        assertThat(buenosAires.zona()).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
        assertThat(buenosAires.aZonificada(TRECE_UTC).toString())
                .isEqualTo("2026-11-24T10:00-03:00[America/Argentina/Buenos_Aires]");
    }
}
