package ar.edu.utn.odonto.agenda.application;

import java.time.Instant;
import java.util.UUID;

/** Fila del listado de turnos con paciente y prestacion para mostrar (lectura, D8). */
public record TurnoListado(
        UUID id,
        Instant inicio,
        Instant fin,
        String estado,
        String origen,
        boolean sobreturno,
        int version,
        UUID pacienteId,
        String pacienteNombre,
        String pacienteApellido,
        UUID prestacionId,
        String prestacionNombre) {

    /** Nunca imprime datos personales (Ley 25.326). */
    @Override
    public String toString() {
        return "TurnoListado[id=" + id + "]";
    }
}
