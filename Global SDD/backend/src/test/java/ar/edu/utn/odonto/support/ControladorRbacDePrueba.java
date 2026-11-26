package ar.edu.utn.odonto.support;

import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.shared.api.Api;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador SOLO DE TEST (C-03): simula una ruta de historia clinica restringida al odontologo
 * (RN-SEG-02) y una ruta de negocio para todo el personal. Nunca se empaqueta.
 */
@RestController
@RequestMapping(Api.V1 + "/__test__/rbac")
public class ControladorRbacDePrueba {

    /** Cuantas veces se ejecuto el metodo restringido: un 403 no debe ejecutarlo. */
    public static final AtomicInteger EJECUCIONES_HISTORIA = new AtomicInteger();

    @GetMapping("/historia")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public String historia() {
        EJECUCIONES_HISTORIA.incrementAndGet();
        return "historia";
    }

    @GetMapping("/personal")
    @PreAuthorize(Permisos.PERSONAL)
    public String personal() {
        return "personal";
    }
}
