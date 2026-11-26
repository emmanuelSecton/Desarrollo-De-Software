package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.identidad.application.Problemas;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mientras el usuario tenga el cambio de contraseña pendiente, solo puede consultar sus datos,
 * cerrar sesion o cambiar la contraseña; el resto de la API responde 403 con un type propio (D8).
 */
public class FiltroCambioPasswordPendiente extends OncePerRequestFilter {

    static final String TITULO = "Cambio de contraseña requerido";
    static final String DETALLE = "Debe cambiar la contraseña antes de continuar";

    private final EscritorDeProblemas escritor;

    public FiltroCambioPasswordPendiente(EscritorDeProblemas escritor) {
        this.escritor = escritor;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (tieneCambioPendiente() && !RutasDeSeguridad.permitidaConCambioPendiente(peticion)) {
            escritor.escribir(peticion, respuesta, HttpStatus.FORBIDDEN,
                    Problemas.CAMBIO_PASSWORD_REQUERIDO, TITULO, DETALLE);
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }

    private static boolean tieneCambioPendiente() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null
                && autenticacion.isAuthenticated()
                && autenticacion.getAuthorities().stream()
                        .anyMatch(a -> Permisos.CAMBIO_PASSWORD_PENDIENTE.equals(a.getAuthority()));
    }
}
