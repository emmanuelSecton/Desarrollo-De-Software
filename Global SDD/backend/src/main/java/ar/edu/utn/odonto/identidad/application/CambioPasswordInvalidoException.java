package ar.edu.utn.odonto.identidad.application;

/** La contraseña actual no coincide o la nueva no cumple la politica (D8). Se traduce a 400. */
public class CambioPasswordInvalidoException extends RuntimeException {

    public CambioPasswordInvalidoException(String mensaje) {
        super(mensaje);
    }
}
