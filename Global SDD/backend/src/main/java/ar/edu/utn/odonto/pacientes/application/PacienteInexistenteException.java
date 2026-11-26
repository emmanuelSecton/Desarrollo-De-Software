package ar.edu.utn.odonto.pacientes.application;

/** No existe un paciente con ese id. Se traduce a 404. */
public class PacienteInexistenteException extends RuntimeException {

    public PacienteInexistenteException() {
        super("No existe un paciente con ese id");
    }
}
