package ar.edu.utn.odonto.identidad.application;

/** Token inexistente, vencido, usado o de un usuario inactivo: el mismo mensaje para todos (C-04, D6). */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException() {
        super("El enlace no es válido o ya venció. Pedí uno nuevo.");
    }
}
