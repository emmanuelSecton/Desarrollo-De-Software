package ar.edu.utn.odonto.identidad.application;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Autoridades de la sesion: el rol, o solo la marca de cambio pendiente si debe cambiar la contraseña (D8). */
public final class Autoridades {

    private Autoridades() {}

    public static List<GrantedAuthority> de(UsuarioAutenticado usuario) {
        if (usuario.debeCambiarPassword()) {
            return List.of(new SimpleGrantedAuthority(Permisos.CAMBIO_PASSWORD_PENDIENTE));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name()));
    }
}
