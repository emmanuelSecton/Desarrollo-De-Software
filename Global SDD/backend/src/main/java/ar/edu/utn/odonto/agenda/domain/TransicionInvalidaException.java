package ar.edu.utn.odonto.agenda.domain;

/** Cambio de estado no permitido por RN-TU-01. Se traduce a 409. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoTurno desde, EstadoTurno hacia) {
        super("Un turno " + desde.name().toLowerCase() + " no puede pasar a " + hacia.name().toLowerCase());
    }
}
