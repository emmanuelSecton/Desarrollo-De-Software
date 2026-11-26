package ar.edu.utn.odonto.pacientes.application;

import java.util.UUID;

/** Ya existe un paciente con ese DNI. Se traduce a 409 con el id del existente (D6). */
public class PacienteDuplicadoException extends RuntimeException {

    private final UUID existente;

    public PacienteDuplicadoException(UUID existente) {
        super("Ya existe un paciente con ese DNI");
        this.existente = existente;
    }

    /** null cuando el choque se detecta solo por la constraint de la base. */
    public UUID existente() {
        return existente;
    }
}
