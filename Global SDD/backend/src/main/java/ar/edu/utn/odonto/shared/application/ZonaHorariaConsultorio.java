package ar.edu.utn.odonto.shared.application;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Zona horaria del consultorio ({@code APP_TIMEZONE}). Convierte entre instantes UTC (lo que se
 * guarda y transmite, RN-GL-02) y fecha/hora local (lo que ve el usuario).
 */
public final class ZonaHorariaConsultorio {

    private final ZoneId zona;

    public ZonaHorariaConsultorio(ZoneId zona) {
        this.zona = Objects.requireNonNull(zona, "zona");
    }

    public ZoneId zona() {
        return zona;
    }

    public LocalDateTime aLocal(Instant instante) {
        return LocalDateTime.ofInstant(instante, zona);
    }

    public ZonedDateTime aZonificada(Instant instante) {
        return instante.atZone(zona);
    }

    public Instant aInstante(LocalDateTime local) {
        return local.atZone(zona).toInstant();
    }
}
