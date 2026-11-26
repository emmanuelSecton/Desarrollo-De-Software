package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.Consultorio;
import ar.edu.utn.odonto.agenda.domain.FranjaDeAtencion;
import ar.edu.utn.odonto.agenda.domain.Prestacion;
import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracion del consultorio, franjas de atencion y prestaciones (US-001, C-05). Las reglas que cruzan
 * filas (franjas superpuestas, nombre repetido) las garantiza la base; aca se traducen a errores de negocio.
 */
@Service
public class ServicioDeConfiguracion {

    static final String ENTIDAD_CONSULTORIO = "CONSULTORIO";
    static final String ENTIDAD_HORARIO = "HORARIO_ATENCION";
    static final String ENTIDAD_PRESTACION = "PRESTACION";
    /** SQLState de una exclusion constraint violada (PostgreSQL). */
    private static final String EXCLUSION_VIOLADA = "23P01";
    /** SQLState de una clave unica violada. */
    private static final String UNICIDAD_VIOLADA = "23505";

    private final RepositorioConsultorio consultorios;
    private final RepositorioHorarios horarios;
    private final RepositorioPrestaciones prestaciones;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public ServicioDeConfiguracion(
            RepositorioConsultorio consultorios,
            RepositorioHorarios horarios,
            RepositorioPrestaciones prestaciones,
            Clock reloj,
            ApplicationEventPublisher eventos) {
        this.consultorios = consultorios;
        this.horarios = horarios;
        this.prestaciones = prestaciones;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    // ---- Consultorio -------------------------------------------------------

    @Transactional(readOnly = true)
    public Consultorio consultorio() {
        return consultorios.obtener();
    }

    @Transactional
    public Consultorio modificarConsultorio(Consultorio.Datos datos, Consultorio.Parametros parametros, String actorId) {
        Consultorio consultorio = consultorios.obtenerParaActualizar();
        consultorio.modificar(datos, parametros, reloj.instant().truncatedTo(ChronoUnit.MICROS));
        consultorios.actualizar(consultorio);
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, ENTIDAD_CONSULTORIO, consultorio.id(), actorId);
        return consultorio;
    }

    // ---- Franjas de atencion -----------------------------------------------

    @Transactional(readOnly = true)
    public List<FranjaDeAtencion> horarios() {
        return horarios.listar();
    }

    @Transactional
    public FranjaDeAtencion crearHorario(int dia, LocalTime desde, LocalTime hasta, String actorId) {
        FranjaDeAtencion franja = FranjaDeAtencion.nueva(dia, desde, hasta);
        conTraduccion(() -> {
            horarios.insertar(franja);
            return null;
        });
        auditar(AccionAuditoria.CREAR_ENTIDAD, ENTIDAD_HORARIO, franja.id(), actorId);
        return franja;
    }

    @Transactional
    public FranjaDeAtencion modificarHorario(
            UUID id, int dia, LocalTime desde, LocalTime hasta, String actorId) {
        horarios.buscar(id).orElseThrow(() -> new RecursoInexistenteException("una franja de atención"));
        FranjaDeAtencion franja = new FranjaDeAtencion(id, dia, desde, hasta);
        conTraduccion(() -> {
            horarios.actualizar(franja);
            return null;
        });
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, ENTIDAD_HORARIO, id, actorId);
        return franja;
    }

    @Transactional
    public void borrarHorario(UUID id, String actorId) {
        horarios.buscar(id).orElseThrow(() -> new RecursoInexistenteException("una franja de atención"));
        horarios.borrar(id);
        auditar(AccionAuditoria.ANULAR_ENTIDAD, ENTIDAD_HORARIO, id, actorId);
    }

    // ---- Prestaciones --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Prestacion> prestaciones(boolean incluirInactivas) {
        return prestaciones.listar(incluirInactivas);
    }

    @Transactional
    public Prestacion crearPrestacion(Prestacion.Datos datos, String actorId) {
        Prestacion prestacion = Prestacion.nueva(datos);
        conTraduccion(() -> {
            prestaciones.insertar(prestacion);
            return null;
        });
        auditar(AccionAuditoria.CREAR_ENTIDAD, ENTIDAD_PRESTACION, prestacion.id(), actorId);
        return prestacion;
    }

    @Transactional
    public Prestacion modificarPrestacion(UUID id, Prestacion.Datos datos, String actorId) {
        Prestacion prestacion = prestacion(id);
        prestacion.modificar(datos);
        conTraduccion(() -> {
            prestaciones.actualizar(prestacion);
            return null;
        });
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, ENTIDAD_PRESTACION, id, actorId);
        return prestacion;
    }

    /** Baja logica: la prestacion nunca se borra (D5). */
    @Transactional
    public void darDeBajaPrestacion(UUID id, String actorId) {
        Prestacion prestacion = prestacion(id);
        prestacion.darDeBaja();
        prestaciones.actualizar(prestacion);
        auditar(AccionAuditoria.ANULAR_ENTIDAD, ENTIDAD_PRESTACION, id, actorId);
    }

    @Transactional
    public void reactivarPrestacion(UUID id, String actorId) {
        Prestacion prestacion = prestacion(id);
        prestacion.reactivar();
        conTraduccion(() -> {
            prestaciones.actualizar(prestacion);
            return null;
        });
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, ENTIDAD_PRESTACION, id, actorId);
    }

    private Prestacion prestacion(UUID id) {
        return prestaciones.buscarPrestacion(id).orElseThrow(() -> new RecursoInexistenteException("una prestación"));
    }

    /** Traduce las violaciones de la exclusion constraint y del indice unico a errores de negocio (409). */
    private static <T> T conTraduccion(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (DataIntegrityViolationException violacion) {
            String estado = violacion.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
            if (EXCLUSION_VIOLADA.equals(estado)) {
                throw new HorarioSuperpuestoException();
            }
            if (UNICIDAD_VIOLADA.equals(estado)) {
                throw new PrestacionDuplicadaException();
            }
            throw violacion;
        }
    }

    private void auditar(AccionAuditoria accion, String entidad, UUID id, String actorId) {
        eventos.publishEvent(new EventoAuditoria(
                accion, TipoActor.USUARIO, actorId, entidad, id.toString(), Map.of(), null));
    }
}
