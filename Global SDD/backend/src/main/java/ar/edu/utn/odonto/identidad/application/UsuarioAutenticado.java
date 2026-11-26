package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.identidad.domain.Rol;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.util.UUID;

/** Datos del usuario de la sesion; nunca incluye el hash de la contraseña. */
public record UsuarioAutenticado(UUID id, String nombre, String email, Rol rol, boolean debeCambiarPassword) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(
                usuario.id(), usuario.nombre(), usuario.email(), usuario.rol(), usuario.debeCambiarPassword());
    }
}
