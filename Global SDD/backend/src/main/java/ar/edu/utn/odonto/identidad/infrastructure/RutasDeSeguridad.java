package ar.edu.utn.odonto.identidad.infrastructure;

import jakarta.servlet.http.HttpServletRequest;

/** Rutas con tratamiento especial de seguridad (03_actores_y_roles.md §Rutas públicas). */
final class RutasDeSeguridad {

    static final String AUTH = "/api/v1/auth";
    static final String LOGIN = AUTH + "/login";
    static final String LOGOUT = AUTH + "/logout";
    static final String CSRF = AUTH + "/csrf";
    static final String ME = AUTH + "/me";
    static final String PASSWORD = AUTH + "/password";
    static final String PASSWORD_RECOVER = AUTH + "/password/recover";
    static final String PASSWORD_RESET = AUTH + "/password/reset";
    static final String PUBLICO = "/api/v1/public/";
    static final String WEBHOOKS = "/api/v1/webhooks/";
    static final String API = "/api/";

    private RutasDeSeguridad() {}

    static String ruta(HttpServletRequest peticion) {
        String uri = peticion.getRequestURI();
        String contexto = peticion.getContextPath();
        return contexto != null && !contexto.isEmpty() && uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
    }

    /** Login, pedido y restablecimiento de contraseña: comparten el cupo por IP del login (C-04, D8). */
    static boolean esAutenticacion(HttpServletRequest peticion) {
        String ruta = ruta(peticion);
        return "POST".equals(peticion.getMethod())
                && (LOGIN.equals(ruta) || PASSWORD_RECOVER.equals(ruta) || PASSWORD_RESET.equals(ruta));
    }

    static boolean esPublica(HttpServletRequest peticion) {
        return ruta(peticion).startsWith(PUBLICO);
    }

    /** Lo unico que puede hacer un usuario con el cambio de contraseña pendiente (D8). */
    static boolean permitidaConCambioPendiente(HttpServletRequest peticion) {
        String ruta = ruta(peticion);
        String metodo = peticion.getMethod();
        return !ruta.startsWith(API)
                || ("GET".equals(metodo) && (ME.equals(ruta) || CSRF.equals(ruta)))
                || ("POST".equals(metodo) && (LOGOUT.equals(ruta) || PASSWORD.equals(ruta) || LOGIN.equals(ruta)))
                || ruta.startsWith(PUBLICO)
                || ruta.startsWith(WEBHOOKS)
                || ruta.equals(PASSWORD_RECOVER)
                || ruta.equals(PASSWORD_RESET);
    }
}
