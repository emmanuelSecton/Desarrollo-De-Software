package ar.edu.utn.odonto.arquitectura.fixture.reloj;

import java.time.Instant;
import java.time.LocalDate;

public class UsaNow {
    public Instant instante() {
        return Instant.now();
    }

    public LocalDate fecha() {
        return LocalDate.now();
    }
}
