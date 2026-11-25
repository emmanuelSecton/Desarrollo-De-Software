package ar.edu.utn.odonto.auditoria.application;

/** Violación del contrato de auditoría. El mensaje nunca incluye el valor rechazado. */
public class AuditoriaInvalidaException extends RuntimeException {

    public AuditoriaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
