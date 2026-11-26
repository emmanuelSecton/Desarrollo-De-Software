package ar.edu.utn.odonto.identidad.application;

/**
 * Expresiones de {@code @PreAuthorize} para la matriz RBAC (03_actores_y_roles.md, D9).
 * Son constantes de compilacion: otros modulos las usan sin depender de clases internas de identidad.
 */
public final class Permisos {

    /** Solo el odontologo: historia clinica, auditoria, integraciones, usuarios (RN-SEG-02). */
    public static final String SOLO_ODONTOLOGO = "hasRole('ODONTOLOGO')";

    /** Todo el personal interno: odontologo y secretarias. */
    public static final String PERSONAL = "hasAnyRole('ODONTOLOGO','SECRETARIA')";

    /** Autoridad del usuario que todavia debe cambiar la contraseña (D8). No otorga ningun rol. */
    public static final String CAMBIO_PASSWORD_PENDIENTE = "CAMBIO_PASSWORD_PENDIENTE";

    private Permisos() {}
}
