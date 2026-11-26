package ar.edu.utn.odonto.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Sesion de prueba con la misma forma que deja el login real de C-03: principal UUID y un rol. */
public final class AutenticacionDePrueba {

    private AutenticacionDePrueba() {}

    public static RequestPostProcessor como(String rol) {
        return como(rol, UUID.randomUUID());
    }

    public static RequestPostProcessor como(String rol, UUID usuarioId) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                usuarioId, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
