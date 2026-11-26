package ar.edu.utn.odonto.agenda.application;

/** Ya hay una prestacion activa con ese nombre (D5). Se traduce a 409. */
public class PrestacionDuplicadaException extends RuntimeException {

    public PrestacionDuplicadaException() {
        super("Ya existe una prestación activa con ese nombre");
    }
}
