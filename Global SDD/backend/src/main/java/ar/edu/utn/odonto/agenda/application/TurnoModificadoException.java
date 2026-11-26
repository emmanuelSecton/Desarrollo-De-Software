package ar.edu.utn.odonto.agenda.application;

/** Otra persona modifico el turno despues de leerlo (bloqueo optimista, D5). Se traduce a 409. */
public class TurnoModificadoException extends RuntimeException {

    public TurnoModificadoException() {
        super("El turno cambió desde que lo cargaste. Volvé a abrirlo y repetí la operación.");
    }
}
