package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.application.evento.EventosDeTurno;
import ar.edu.utn.odonto.agenda.domain.CanceladoPor;
import ar.edu.utn.odonto.agenda.domain.EstadoTurno;
import ar.edu.utn.odonto.agenda.domain.OrigenTurno;
import ar.edu.utn.odonto.agenda.domain.Turno;
import ar.edu.utn.odonto.agenda.domain.TurnoInvalidoException;
import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.pacientes.application.PacienteInexistenteException;
import ar.edu.utn.odonto.pacientes.application.PacienteVista;
import ar.edu.utn.odonto.pacientes.application.RegistroDePacientes;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turnos internos del consultorio (US-011, C-10). La superposicion la decide la base (D4): no hay una consulta
 * previa de disponibilidad. Los cambios usan bloqueo optimista (D5) y publican eventos de dominio (D9).
 */
@Service
public class ServicioDeTurnos {

    static final String ENTIDAD = "TURNO";
    static final Duration RANGO_MAXIMO = Duration.ofDays(31);
    /** SQLState de una exclusion constraint violada (PostgreSQL). */
    private static final String EXCLUSION_VIOLADA = "23P01";

    private final RepositorioTurnos turnos;
    private final RegistroDePacientes pacientes;
    private final ConfiguracionDeAgenda configuracion;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public ServicioDeTurnos(
            RepositorioTurnos turnos,
            RegistroDePacientes pacientes,
            ConfiguracionDeAgenda configuracion,
            Clock reloj,
            ApplicationEventPublisher eventos) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.configuracion = configuracion;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    /** Alta interna (D3). Con {@code sobreturno} se permite la superposicion (RN-AG-07). */
    @Transactional
    public Turno crear(UUID pacienteId, UUID prestacionId, Instant inicio, Integer duracionPedida, boolean sobreturno,
            String actorId) {
        validarPaciente(pacienteId);
        ConfiguracionDeAgenda.PrestacionVista prestacion = prestacionActiva(prestacionId);
        Turno turno = Turno.nuevo(pacienteId, prestacionId, prestacion.duracionMinutos(), duracionPedida, inicio,
                sobreturno, OrigenTurno.INTERNO, null, ahora());
        insertar(turno);
        auditar(AccionAuditoria.CREAR_TURNO, turno.id(), actorId, Map.of("paciente_id", pacienteId.toString()));
        eventos.publishEvent(new EventosDeTurno.TurnoCreado(turno.id(), pacienteId, turno.inicio(), turno.origen()));
        return turno;
    }

    @Transactional(readOnly = true)
    public Turno obtener(UUID id) {
        return turnos.buscar(id).orElseThrow(() -> new RecursoInexistenteException("un turno"));
    }

    @Transactional(readOnly = true)
    public List<TurnoListado> listar(Instant desde, Instant hasta) {
        if (!desde.isBefore(hasta)) {
            throw new TurnoInvalidoException("hasta", "debe ser posterior a desde");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new TurnoInvalidoException("hasta", "el rango no puede superar los 31 días");
        }
        return turnos.listar(desde, hasta);
    }

    @Transactional
    public Turno confirmar(UUID id, int version, String actorId) {
        Turno turno = paraModificar(id, version);
        EstadoTurno desde = turno.estado();
        turno.confirmar(ahora());
        guardar(turno);
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, id, actorId, Map.of("referencia", "confirmacion"));
        eventos.publishEvent(new EventosDeTurno.TurnoEstadoCambiado(id, desde, turno.estado()));
        return obtener(id);
    }

    /** El motivo no se audita: puede contener datos de salud (D10). */
    @Transactional
    public Turno cancelar(UUID id, int version, CanceladoPor por, String motivo, String actorId) {
        Turno turno = paraModificar(id, version);
        EstadoTurno desde = turno.estado();
        turno.cancelar(por, motivo, ahora());
        guardar(turno);
        auditar(AccionAuditoria.ANULAR_ENTIDAD, id, actorId, Map.of("motivo_codigo", por.name()));
        eventos.publishEvent(new EventosDeTurno.TurnoCancelado(id, turno.pacienteId(), por));
        eventos.publishEvent(new EventosDeTurno.TurnoEstadoCambiado(id, desde, turno.estado()));
        return obtener(id);
    }

    /**
     * Reprograma en una transaccion (RN-TU-03, D6): cancela el original y crea el nuevo vinculado. Si el horario
     * nuevo esta ocupado, la excepcion revierte todo y el original queda intacto.
     */
    @Transactional
    public Turno reprogramar(UUID id, int version, Instant nuevoInicio, Integer duracionPedida, String actorId) {
        Turno original = paraModificar(id, version);
        int duracionPrestacion = configuracion.prestacion(original.prestacionId())
                .map(ConfiguracionDeAgenda.PrestacionVista::duracionMinutos)
                .orElseThrow(() -> new RecursoInexistenteException("una prestación"));
        Turno nuevo = original.reprogramar(nuevoInicio, duracionPedida, duracionPrestacion, ahora());
        guardar(original);
        insertar(nuevo);

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("turno_id", nuevo.id().toString());
        detalle.put("referencia", "reprogramacion");
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, id, actorId, Map.copyOf(detalle));
        auditar(AccionAuditoria.CREAR_TURNO, nuevo.id(), actorId, Map.of("paciente_id", nuevo.pacienteId().toString()));
        eventos.publishEvent(new EventosDeTurno.TurnoReprogramado(id, nuevo.id(), nuevo.pacienteId()));
        return nuevo;
    }

    private void validarPaciente(UUID pacienteId) {
        PacienteVista paciente;
        try {
            paciente = pacientes.obtener(pacienteId);
        } catch (PacienteInexistenteException e) {
            throw new TurnoInvalidoException("pacienteId", "no existe un paciente con ese id");
        }
        if (!paciente.activo()) {
            throw new TurnoInvalidoException("pacienteId", "el paciente está dado de baja");
        }
    }

    private ConfiguracionDeAgenda.PrestacionVista prestacionActiva(UUID prestacionId) {
        return configuracion.prestacion(prestacionId)
                .filter(ConfiguracionDeAgenda.PrestacionVista::activa)
                .orElseThrow(() -> new TurnoInvalidoException("prestacionId", "no existe o está dada de baja"));
    }

    /** Lee el turno y verifica de entrada que la version del cliente siga vigente (D5). */
    private Turno paraModificar(UUID id, int version) {
        Turno turno = obtener(id);
        if (turno.version() != version) {
            throw new TurnoModificadoException();
        }
        return turno;
    }

    private void guardar(Turno turno) {
        if (!turnos.actualizarSiNoCambio(turno)) {
            throw new TurnoModificadoException();
        }
    }

    private void insertar(Turno turno) {
        try {
            turnos.insertar(turno);
        } catch (DataIntegrityViolationException violacion) {
            String estado = violacion.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
            if (EXCLUSION_VIOLADA.equals(estado)) {
                throw new TurnoSuperpuestoException();
            }
            throw violacion;
        }
    }

    private Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private void auditar(AccionAuditoria accion, UUID id, String actorId, Map<String, Object> detalle) {
        eventos.publishEvent(new EventoAuditoria(
                accion, TipoActor.USUARIO, actorId, ENTIDAD, id.toString(), detalle, null));
    }
}
