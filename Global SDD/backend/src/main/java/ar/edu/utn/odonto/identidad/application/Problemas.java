package ar.edu.utn.odonto.identidad.application;

import java.net.URI;

/** {@code type} estables de los errores de seguridad en RFC 7807 (D6). */
public final class Problemas {

    public static final URI NO_AUTENTICADO = URI.create("/problemas/no-autenticado");
    public static final URI CREDENCIALES_INVALIDAS = URI.create("/problemas/credenciales-invalidas");
    public static final URI ACCESO_DENEGADO = URI.create("/problemas/acceso-denegado");
    public static final URI CAMBIO_PASSWORD_REQUERIDO = URI.create("/problemas/cambio-password-requerido");
    public static final URI DEMASIADAS_PETICIONES = URI.create("/problemas/demasiadas-peticiones");
    public static final URI PASSWORD_INVALIDA = URI.create("/problemas/password-invalida");

    private Problemas() {}
}
