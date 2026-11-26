package ar.edu.utn.odonto.identidad.application;

import java.nio.charset.StandardCharsets;

/**
 * Politica de contraseña (NIST SP 800-63B, D8): de 10 caracteres a 72 bytes UTF-8, porque BCrypt
 * ignora lo que excede los 72 bytes. Sin reglas de composicion.
 */
public final class PoliticaPassword {

    public static final int MINIMO_CARACTERES = 10;
    public static final int MAXIMO_BYTES = 72;

    private PoliticaPassword() {}

    /** Los mensajes nunca incluyen la contraseña. */
    public static void validarNueva(String nueva) {
        if (nueva == null || nueva.codePointCount(0, nueva.length()) < MINIMO_CARACTERES) {
            throw new CambioPasswordInvalidoException(
                    "La nueva contraseña debe tener al menos " + MINIMO_CARACTERES + " caracteres");
        }
        if (nueva.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES) {
            throw new CambioPasswordInvalidoException("La nueva contraseña es demasiado larga");
        }
    }
}
