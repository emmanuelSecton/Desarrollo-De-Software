package ar.edu.utn.odonto.agenda.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * Turno del consultorio (C-10). Aplica las reglas de duracion (RN-AG-02), inicio y estados (RN-TU-01).
 * Que no se superponga con otro activo lo garantiza la base (RN-AG-01); aca nunca se consulta la agenda.
 * Recibe siempre el instante actual: nunca consulta el reloj por su cuenta.
 */
public class Turno {

    public static final int DURACION_MAXIMA = 480;
    static final String MOTIVO_REPROGRAMADO = "Reprogramado";

    private final UUID id;
    private final UUID pacienteId;
    private final UUID prestacionId;
    private final Instant inicio;
    private final Instant fin;
    private EstadoTurno estado;
    private final OrigenTurno origen;
    private final boolean sobreturno;
    private final UUID reprogramadoDe;
    private final String tokenPublico;
    private CanceladoPor canceladoPor;
    private String motivoCancelacion;
    private final Instant creadoAt;
    private Instant actualizadoAt;
    private final int version;

    public Turno(
            UUID id,
            UUID pacienteId,
            UUID prestacionId,
            Instant inicio,
            Instant fin,
            EstadoTurno estado,
            OrigenTurno origen,
            boolean sobreturno,
            UUID reprogramadoDe,
            String tokenPublico,
            CanceladoPor canceladoPor,
            String motivoCancelacion,
            Instant creadoAt,
            Instant actualizadoAt,
            int version) {
        this.id = Objects.requireNonNull(id, "id");
        this.pacienteId = Objects.requireNonNull(pacienteId, "pacienteId");
        this.prestacionId = Objects.requireNonNull(prestacionId, "prestacionId");
        this.inicio = Objects.requireNonNull(inicio, "inicio");
        this.fin = Objects.requireNonNull(fin, "fin");
        this.estado = Objects.requireNonNull(estado, "estado");
        this.origen = Objects.requireNonNull(origen, "origen");
        this.sobreturno = sobreturno;
        this.reprogramadoDe = reprogramadoDe;
        this.tokenPublico = Objects.requireNonNull(tokenPublico, "tokenPublico");
        this.canceladoPor = canceladoPor;
        this.motivoCancelacion = motivoCancelacion;
        this.creadoAt = Objects.requireNonNull(creadoAt, "creadoAt");
        this.actualizadoAt = Objects.requireNonNull(actualizadoAt, "actualizadoAt");
        this.version = version;
    }

    /**
     * Turno nuevo RESERVADO. La duracion es la de la prestacion, o la pedida si es mayor o igual, en bloques
     * de 5 minutos (RN-AG-02); el inicio es futuro y en multiplos de 5 minutos.
     */
    public static Turno nuevo(
            UUID pacienteId,
            UUID prestacionId,
            int duracionPrestacion,
            Integer duracionPedida,
            Instant inicio,
            boolean sobreturno,
            OrigenTurno origen,
            UUID reprogramadoDe,
            Instant ahora) {
        int duracion = duracionPedida == null ? duracionPrestacion : duracionPedida;
        if (duracion < duracionPrestacion || duracion % 5 != 0 || duracion > DURACION_MAXIMA) {
            throw new TurnoInvalidoException("duracionMinutos", "debe ser de al menos " + duracionPrestacion
                    + " minutos (la de la prestación), múltiplo de 5 y de hasta " + DURACION_MAXIMA);
        }
        var hora = inicio.atOffset(ZoneOffset.UTC);
        if (hora.getMinute() % 5 != 0 || hora.getSecond() != 0 || hora.getNano() != 0) {
            throw new TurnoInvalidoException("inicio", "debe ser en múltiplos de 5 minutos");
        }
        if (!inicio.isAfter(ahora)) {
            throw new TurnoInvalidoException("inicio", "debe ser posterior al momento actual");
        }
        return new Turno(UUID.randomUUID(), pacienteId, prestacionId, inicio, inicio.plus(Duration.ofMinutes(duracion)),
                EstadoTurno.RESERVADO, origen, sobreturno, reprogramadoDe, TokenPublico.generar(), null, null, ahora,
                ahora, 0);
    }

    public void confirmar(Instant ahora) {
        cambiarA(EstadoTurno.CONFIRMADO, ahora);
    }

    public void cancelar(CanceladoPor por, String motivo, Instant ahora) {
        if (motivo == null || motivo.isBlank()) {
            throw new TurnoInvalidoException("motivo", "es obligatorio");
        }
        cambiarA(EstadoTurno.CANCELADO, ahora);
        this.canceladoPor = Objects.requireNonNull(por, "por");
        this.motivoCancelacion = motivo.trim();
    }

    /**
     * Reprograma (RN-TU-03): cancela este turno y devuelve el nuevo, vinculado, para el mismo paciente,
     * prestacion y origen. Con {@code duracionPedida} nula conserva la duracion actual.
     */
    public Turno reprogramar(Instant nuevoInicio, Integer duracionPedida, int duracionPrestacion, Instant ahora) {
        int duracion = duracionPedida == null ? duracionMinutos() : duracionPedida;
        cancelar(CanceladoPor.CONSULTORIO, MOTIVO_REPROGRAMADO, ahora);
        return nuevo(pacienteId, prestacionId, duracionPrestacion, duracion, nuevoInicio, sobreturno, origen, id, ahora);
    }

    private void cambiarA(EstadoTurno destino, Instant ahora) {
        if (!estado.puedePasarA(destino)) {
            throw new TransicionInvalidaException(estado, destino);
        }
        this.estado = destino;
        this.actualizadoAt = ahora;
    }

    public int duracionMinutos() {
        return (int) Duration.between(inicio, fin).toMinutes();
    }

    public UUID id() {
        return id;
    }

    public UUID pacienteId() {
        return pacienteId;
    }

    public UUID prestacionId() {
        return prestacionId;
    }

    public Instant inicio() {
        return inicio;
    }

    public Instant fin() {
        return fin;
    }

    public EstadoTurno estado() {
        return estado;
    }

    public OrigenTurno origen() {
        return origen;
    }

    public boolean sobreturno() {
        return sobreturno;
    }

    public UUID reprogramadoDe() {
        return reprogramadoDe;
    }

    public String tokenPublico() {
        return tokenPublico;
    }

    public CanceladoPor canceladoPor() {
        return canceladoPor;
    }

    public String motivoCancelacion() {
        return motivoCancelacion;
    }

    public Instant creadoAt() {
        return creadoAt;
    }

    public Instant actualizadoAt() {
        return actualizadoAt;
    }

    /** Version leida de la base; el repositorio la usa para el bloqueo optimista (D5). */
    public int version() {
        return version;
    }
}
