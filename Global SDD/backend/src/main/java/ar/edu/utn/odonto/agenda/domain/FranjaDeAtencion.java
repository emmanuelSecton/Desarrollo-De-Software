package ar.edu.utn.odonto.agenda.domain;

import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Franja de atencion de un dia de la semana (1 = lunes a 7 = domingo), en multiplos de 5 minutos (C-05, D4).
 * Que no se superponga con otra del mismo dia lo garantiza la base.
 */
public record FranjaDeAtencion(UUID id, int diaSemana, LocalTime desde, LocalTime hasta) {

    public FranjaDeAtencion {
        Objects.requireNonNull(id, "id");
        if (diaSemana < 1 || diaSemana > 7) {
            throw new ConfiguracionInvalidaException("diaSemana", "debe estar entre 1 (lunes) y 7 (domingo)");
        }
        multiploDe5("desde", desde);
        multiploDe5("hasta", hasta);
        if (!desde.isBefore(hasta)) {
            throw new ConfiguracionInvalidaException("hasta", "debe ser posterior a la hora de inicio");
        }
    }

    public static FranjaDeAtencion nueva(int diaSemana, LocalTime desde, LocalTime hasta) {
        return new FranjaDeAtencion(UUID.randomUUID(), diaSemana, desde, hasta);
    }

    private static void multiploDe5(String campo, LocalTime hora) {
        if (hora == null) {
            throw new ConfiguracionInvalidaException(campo, "es obligatoria");
        }
        if (hora.getMinute() % 5 != 0 || hora.getSecond() != 0 || hora.getNano() != 0) {
            throw new ConfiguracionInvalidaException(campo, "debe ser múltiplo de 5 minutos");
        }
    }
}
