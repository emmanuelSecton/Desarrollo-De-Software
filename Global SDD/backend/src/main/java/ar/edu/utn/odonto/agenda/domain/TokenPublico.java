package ar.edu.utn.odonto.agenda.domain;

import java.security.SecureRandom;
import java.util.Base64;

/** Token del enlace publico del turno: 256 bits aleatorios, no secuencial (RN-GL-03, C-10 D7). */
public final class TokenPublico {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private TokenPublico() {}

    /** 43 caracteres Base64 URL-safe, sin relleno. */
    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
