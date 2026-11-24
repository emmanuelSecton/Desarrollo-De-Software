package ar.edu.utn.odonto.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Unico {@code @RestControllerAdvice} de la aplicacion: todo error sale como {@code ProblemDetail}
 * (RFC 7807 / 9457, {@code application/problem+json}) sin filtrar informacion interna (D5).
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

    static final String DETALLE_VALIDACION = "La solicitud contiene datos inválidos";
    static final String DETALLE_CUERPO_INVALIDO = "El cuerpo de la petición no es válido";
    static final String DETALLE_ERROR_INESPERADO = "Ocurrió un error inesperado";

    /** Un campo inválido: se serializa como {@code {"campo": ..., "mensaje": ...}}. */
    public record ErrorDeCampo(String campo, String mensaje) {}

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDeCampo> errores = new ArrayList<>();
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            String campo = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            errores.add(new ErrorDeCampo(campo, error.getDefaultMessage()));
        }
        return conErrores(errores, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDeCampo> errores = new ArrayList<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String campo = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors()
                    .forEach(error -> errores.add(new ErrorDeCampo(campo, error.getDefaultMessage())));
        });
        return conErrores(errores, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        // Sin el mensaje del parser: podria contener fragmentos del cuerpo (Ley 25.326).
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, DETALLE_CUERPO_INVALIDO);
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<ErrorDeCampo> errores = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing((ConstraintViolation<?> v) -> v.getPropertyPath().toString()))
                .map(v -> new ErrorDeCampo(nombreDeCampo(v), v.getMessage()))
                .toList();
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, DETALLE_VALIDACION);
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleInesperada(Exception ex, HttpServletRequest request) {
        // Solo tipo de excepcion y ruta: ni mensaje, ni stack trace, ni cuerpo ni parametros (Ley 25.326).
        LOG.error("Error no controlado {} en {}", ex.getClass().getName(), request.getRequestURI());
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, DETALLE_ERROR_INESPERADO);
        problema.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respuesta != null
                && respuesta.getBody() instanceof ProblemDetail problema
                && problema.getInstance() == null
                && request instanceof ServletWebRequest servletRequest) {
            problema.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }
        return respuesta;
    }

    private ResponseEntity<Object> conErrores(List<ErrorDeCampo> errores, WebRequest request) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, DETALLE_VALIDACION);
        problema.setProperty("errores", errores);
        return handleExceptionInternal(null, problema, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    private static String nombreDeCampo(ConstraintViolation<?> violacion) {
        String ruta = violacion.getPropertyPath().toString();
        return ruta.substring(ruta.lastIndexOf('.') + 1);
    }
}
