package ar.edu.utn.odonto.agenda.domain;

/** Un dato del turno que no cumple una regla (duracion, inicio). Se traduce a 400 con el campo. */
public class TurnoInvalidoException extends ConfiguracionInvalidaException {

    public TurnoInvalidoException(String campo, String mensaje) {
        super(campo, mensaje);
    }
}
