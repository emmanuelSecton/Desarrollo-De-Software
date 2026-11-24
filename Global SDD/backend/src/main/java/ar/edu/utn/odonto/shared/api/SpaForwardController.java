package ar.edu.utn.odonto.shared.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Reenvia a {@code /index.html} las rutas GET del cliente (SPA) para que funcionen los enlaces
 * directos (D10). Quedan fuera {@code /api/**}, {@code /actuator/**} y cualquier ruta cuyo ultimo
 * segmento tenga un punto (archivos estaticos): esas siguen su camino normal (404 RFC 7807 o recurso).
 */
@Controller
public class SpaForwardController {

    // Segmentos sin punto; el primero no puede ser "api" ni "actuator".
    private static final String S1 = "{s1:(?!api$|actuator$)[^.]+}";
    private static final String S2 = "{s2:[^.]+}";
    private static final String S3 = "{s3:[^.]+}";
    private static final String S4 = "{s4:[^.]+}";
    private static final String S5 = "{s5:[^.]+}";
    private static final String S6 = "{s6:[^.]+}";

    @GetMapping({
        "/",
        "/" + S1,
        "/" + S1 + "/" + S2,
        "/" + S1 + "/" + S2 + "/" + S3,
        "/" + S1 + "/" + S2 + "/" + S3 + "/" + S4,
        "/" + S1 + "/" + S2 + "/" + S3 + "/" + S4 + "/" + S5,
        "/" + S1 + "/" + S2 + "/" + S3 + "/" + S4 + "/" + S5 + "/" + S6
    })
    public String indexHtml() {
        return "forward:/index.html";
    }
}
