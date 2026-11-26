package ar.edu.utn.odonto.agenda.domain;

/** Un valor de configuracion que no cumple una regla del dominio (C-05). Se traduce a 400 con el campo. */
public class ConfiguracionInvalidaException extends RuntimeException {

    private final String campo;

    public ConfiguracionInvalidaException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
