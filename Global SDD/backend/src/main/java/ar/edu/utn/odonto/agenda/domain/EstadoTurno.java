package ar.edu.utn.odonto.agenda.domain;

import java.util.EnumSet;
import java.util.Set;

/** Estados del turno y sus transiciones validas (RN-TU-01, C-10 D2). */
public enum EstadoTurno {
    RESERVADO,
    CONFIRMADO,
    CANCELADO,
    ATENDIDO,
    AUSENTE;

    /** Activos: los que ocupan el horario y entran en la garantia anti-solapamiento (RN-AG-01). */
    public static final Set<EstadoTurno> ACTIVOS = EnumSet.of(RESERVADO, CONFIRMADO);

    public boolean puedePasarA(EstadoTurno destino) {
        return switch (this) {
            case RESERVADO -> EnumSet.of(CONFIRMADO, CANCELADO, ATENDIDO, AUSENTE).contains(destino);
            case CONFIRMADO -> EnumSet.of(CANCELADO, ATENDIDO, AUSENTE).contains(destino);
            case CANCELADO, ATENDIDO, AUSENTE -> false;
        };
    }

    public boolean activo() {
        return ACTIVOS.contains(this);
    }
}
