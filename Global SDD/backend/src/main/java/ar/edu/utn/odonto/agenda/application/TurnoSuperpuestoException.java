package ar.edu.utn.odonto.agenda.application;

/** El horario se superpone con otro turno activo; lo detecta la exclusion constraint (D4). Se traduce a 409. */
public class TurnoSuperpuestoException extends RuntimeException {

    public TurnoSuperpuestoException() {
        super("Ese horario se superpone con otro turno. Si querés darlo igual, crealo como sobreturno.");
    }
}
