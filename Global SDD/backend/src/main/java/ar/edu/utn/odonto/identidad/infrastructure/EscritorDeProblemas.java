package ar.edu.utn.odonto.identidad.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Escribe errores RFC 7807 desde los filtros de seguridad, que no pasan por el
 * {@code @RestControllerAdvice} (D6). Mismo formato que el resto de la API.
 */
@Component
public class EscritorDeProblemas {

    static final String PROBLEM_JSON = "application/problem+json";

    private final ObjectMapper mapper;

    public EscritorDeProblemas(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escribir(
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            HttpStatus estado,
            URI tipo,
            String titulo,
            String detalle)
            throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(tipo);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        respuesta.setStatus(estado.value());
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.setContentType(PROBLEM_JSON);
        mapper.writeValue(respuesta.getOutputStream(), problema);
    }
}
