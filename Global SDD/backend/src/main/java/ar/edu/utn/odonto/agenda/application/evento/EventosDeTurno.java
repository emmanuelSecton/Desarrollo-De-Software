package ar.edu.utn.odonto.agenda.application.evento;

import ar.edu.utn.odonto.agenda.domain.CanceladoPor;
import ar.edu.utn.odonto.agenda.domain.EstadoTurno;
import ar.edu.utn.odonto.agenda.domain.OrigenTurno;
import java.time.Instant;
import java.util.UUID;

/**
 * Eventos de dominio de la agenda (C-10, D9). Se publican dentro de la transaccion; los oyentes de otros
 * modulos usan {@code @TransactionalEventListener(AFTER_COMMIT)} y no se enteran de lo que no se guardo.
 */
public final class EventosDeTurno {

    private EventosDeTurno() {}

    public record TurnoCreado(UUID turnoId, UUID pacienteId, Instant inicio, OrigenTurno origen) {}

    public record TurnoCancelado(UUID turnoId, UUID pacienteId, CanceladoPor canceladoPor) {}

    public record TurnoReprogramado(UUID originalId, UUID nuevoId, UUID pacienteId) {}

    public record TurnoEstadoCambiado(UUID turnoId, EstadoTurno desde, EstadoTurno hacia) {}
}
