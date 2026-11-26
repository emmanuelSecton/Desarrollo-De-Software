package ar.edu.utn.odonto.agenda.application;

/** La franja o la prestacion pedida no existe. Se traduce a 404. */
public class RecursoInexistenteException extends RuntimeException {

    public RecursoInexistenteException(String recurso) {
        super("No existe " + recurso + " con ese id");
    }
}
