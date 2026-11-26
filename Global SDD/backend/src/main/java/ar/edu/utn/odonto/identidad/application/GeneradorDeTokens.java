package ar.edu.utn.odonto.identidad.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de recuperacion: 256 bits de {@link SecureRandom} en Base64 URL-safe, y su SHA-256 para guardar
 * (C-04, D3). Un hash rapido alcanza porque el token es aleatorio: no hay diccionario que probar.
 */
public final class GeneradorDeTokens {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES = 32;

    private GeneradorDeTokens() {}

    public static String nuevo() {
        byte[] bytes = new byte[BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
