package ar.edu.utn.odonto.arquitectura.fixture.reloj;

import java.time.Clock;
import java.time.Instant;

public class UsaClockInyectado {
    private final Clock clock;

    public UsaClockInyectado(Clock clock) {
        this.clock = clock;
    }

    public Instant instante() {
        return Instant.now(clock);
    }
}
