package ar.edu.utn.odonto.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reloj UTC que el test puede adelantar: bloqueos y ventanas sin esperas reales (C-03, D14). */
@TestConfiguration(proxyBeanMethods = false)
public class RelojDePrueba {

    public static final Instant INICIO = Instant.parse("2026-11-25T12:00:00Z");

    /** Reloj mutable y seguro entre hilos. */
    public static class RelojMutable extends Clock {
        private volatile Instant ahora = INICIO;

        public void adelantar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        public void fijar(Instant instante) {
            ahora = instante;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Bean
    @Primary
    RelojMutable relojMutable() {
        return new RelojMutable();
    }
}
