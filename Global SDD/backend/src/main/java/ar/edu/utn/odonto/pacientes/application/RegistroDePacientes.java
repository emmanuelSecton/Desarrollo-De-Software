package ar.edu.utn.odonto.pacientes.application;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.pacientes.domain.Paciente;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, modificacion, baja logica y busqueda de pacientes (C-09). Es la API publica del modulo: otros
 * modulos usan {@link #obtenerOCrear} para asociar un paciente por DNI sin pisar sus datos (D6).
 */
@Service
public class RegistroDePacientes {

    static final String ENTIDAD = "PACIENTE";
    static final String CAMPO_DNI = "dni";
    static final int TAMANO_MAXIMO = 100;

    private final RepositorioPacientes pacientes;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;
    private final String versionPolitica;

    public RegistroDePacientes(
            RepositorioPacientes pacientes,
            Clock reloj,
            ApplicationEventPublisher eventos,
            @Value("${pacientes.politica-privacidad-version:v0-provisoria}") String versionPolitica) {
        this.pacientes = pacientes;
        this.reloj = reloj;
        this.eventos = eventos;
        this.versionPolitica = versionPolitica;
    }

    /** Alta interna. @throws PacienteDuplicadoException con el id del existente si el DNI ya esta cargado */
    @Transactional
    public PacienteVista alta(DatosDePaciente datos, Actor actor) {
        Paciente nuevo = Paciente.nuevo(normalizar(datos), versionPolitica, ahora());
        if (!pacientes.insertarSiNoExiste(nuevo)) {
            UUID existente = pacientes.buscarPorDni(nuevo.datos().dni()).map(Paciente::id).orElse(null);
            throw new PacienteDuplicadoException(existente);
        }
        auditar(AccionAuditoria.CREAR_ENTIDAD, nuevo.id(), actor, null);
        return PacienteVista.de(nuevo);
    }

    /**
     * Para otros modulos (C-10, C-20): devuelve el paciente con ese DNI SIN modificar sus datos o, si no existe,
     * lo crea con los datos recibidos. Dos llamadas simultaneas con el mismo DNI terminan en un solo paciente.
     */
    @Transactional
    public PacienteVista obtenerOCrear(DatosDePaciente datos, Actor actor) {
        Paciente nuevo = Paciente.nuevo(normalizar(datos), versionPolitica, ahora());
        if (pacientes.insertarSiNoExiste(nuevo)) {
            auditar(AccionAuditoria.CREAR_ENTIDAD, nuevo.id(), actor, null);
            return PacienteVista.de(nuevo);
        }
        return pacientes.buscarPorDni(nuevo.datos().dni())
                .map(PacienteVista::de)
                .orElseThrow(() -> new IllegalStateException("El paciente con DNI duplicado no se encontró"));
    }

    @Transactional(readOnly = true)
    public PacienteVista obtener(UUID id) {
        return pacientes.buscarPorId(id).map(PacienteVista::de).orElseThrow(PacienteInexistenteException::new);
    }

    @Transactional(readOnly = true)
    public Pagina<PacienteVista> buscar(String q, boolean incluirInactivos, int pagina, int tamano) {
        int tamanoAcotado = Math.min(Math.max(tamano, 1), TAMANO_MAXIMO);
        return pacientes.buscar(CriterioDeBusqueda.de(q), incluirInactivos, Math.max(pagina, 0), tamanoAcotado)
                .map(PacienteVista::de);
    }

    /** No modifica el consentimiento de datos, su version ni las ausencias (D7). */
    @Transactional
    public PacienteVista modificar(UUID id, DatosDePaciente datos, Actor actor) {
        Paciente paciente = paraActualizar(id);
        Paciente.DatosPersonales nuevos = normalizar(datos);
        pacientes.buscarPorDni(nuevos.dni())
                .filter(otro -> !otro.id().equals(id))
                .ifPresent(otro -> {
                    throw new PacienteDuplicadoException(otro.id());
                });
        paciente.modificar(nuevos, ahora());
        pacientes.actualizar(paciente);
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, id, actor, null);
        return PacienteVista.de(paciente);
    }

    /** Baja logica: el paciente nunca se borra (RN-HC-03). */
    @Transactional
    public void darDeBaja(UUID id, Actor actor) {
        Paciente paciente = paraActualizar(id);
        paciente.darDeBaja(ahora());
        pacientes.actualizar(paciente);
        auditar(AccionAuditoria.ANULAR_ENTIDAD, id, actor, null);
    }

    @Transactional
    public void reactivar(UUID id, Actor actor) {
        Paciente paciente = paraActualizar(id);
        paciente.reactivar(ahora());
        pacientes.actualizar(paciente);
        auditar(AccionAuditoria.MODIFICAR_ENTIDAD, id, actor, "reactivacion");
    }

    /** Instante del reloj con la precision de TIMESTAMPTZ (microsegundos): lo devuelto coincide con lo guardado. */
    private Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private Paciente paraActualizar(UUID id) {
        return pacientes.buscarPorIdParaActualizar(id).orElseThrow(PacienteInexistenteException::new);
    }

    static Paciente.DatosPersonales normalizar(DatosDePaciente datos) {
        String dni = datos.dni() == null ? "" : datos.dni().replaceAll("[\\s.\\-]", "");
        if (!dni.matches("[0-9]{7,9}")) {
            throw new DatoDePacienteInvalidoException(CAMPO_DNI, "debe tener entre 7 y 9 dígitos");
        }
        String email = datos.email() == null || datos.email().isBlank() ? null : datos.email().trim();
        return new Paciente.DatosPersonales(
                dni,
                datos.nombre().trim(),
                datos.apellido().trim(),
                datos.fechaNacimiento(),
                NormalizadorDeTelefono.normalizar(datos.telefono()),
                email,
                datos.aceptaWhatsapp());
    }

    /** Solo el id del paciente: nunca DNI, nombre, telefono ni email (Ley 25.326, D9). */
    private void auditar(AccionAuditoria accion, UUID id, Actor actor, String referencia) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("paciente_id", id.toString());
        if (referencia != null) {
            detalle.put("referencia", referencia);
        }
        eventos.publishEvent(new EventoAuditoria(
                accion, actor.tipo(), actor.id(), ENTIDAD, id.toString(), Map.copyOf(detalle), null));
    }
}
