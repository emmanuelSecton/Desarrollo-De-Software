package ar.edu.utn.odonto.shared.api;

/** Constantes de la API REST. Cada controlador de negocio se publica bajo {@link #V1} (D6). */
public final class Api {

    /** Prefijo de todos los endpoints REST de negocio. {@code /actuator} queda fuera. */
    public static final String V1 = "/api/v1";

    private Api() {}
}
