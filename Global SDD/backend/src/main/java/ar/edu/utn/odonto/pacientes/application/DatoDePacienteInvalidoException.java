package ar.edu.utn.odonto.pacientes.application;

/** Un dato que pasa la validacion del DTO pero no la normalizacion (DNI, telefono). Se traduce a 400. */
public class DatoDePacienteInvalidoException extends RuntimeException {

    private final String campo;

    public DatoDePacienteInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
