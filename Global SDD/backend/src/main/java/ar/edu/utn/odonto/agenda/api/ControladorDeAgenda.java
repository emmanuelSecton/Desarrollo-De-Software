package ar.edu.utn.odonto.agenda.api;

import ar.edu.utn.odonto.agenda.application.HorarioSuperpuestoException;
import ar.edu.utn.odonto.agenda.application.PrestacionDuplicadaException;
import ar.edu.utn.odonto.agenda.application.RecursoInexistenteException;
import ar.edu.utn.odonto.agenda.application.TurnoModificadoException;
import ar.edu.utn.odonto.agenda.application.TurnoSuperpuestoException;
import ar.edu.utn.odonto.agenda.domain.TransicionInvalidaException;
import ar.edu.utn.odonto.agenda.domain.ConfiguracionInvalidaException;
import ar.edu.utn.odonto.shared.api.ManejadorGlobalDeErrores.ErrorDeCampo;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Errores comunes de los controladores de la agenda: configuracion (C-05, D7) y turnos (C-10). Es una clase base y no un
 * {@code @RestControllerAdvice}: el unico advice de la aplicacion sigue siendo el global de C-01.
 */
abstract class ControladorDeAgenda {

    static final URI TIPO_SUPERPUESTO = URI.create("/problemas/horario-superpuesto");
    static final URI TIPO_DUPLICADA = URI.create("/problemas/prestacion-duplicada");
    static final URI TIPO_NO_ENCONTRADO = URI.create("/problemas/no-encontrado");
    static final URI TIPO_TURNO_SUPERPUESTO = URI.create("/problemas/turno-superpuesto");
    static final URI TIPO_TURNO_MODIFICADO = URI.create("/problemas/turno-modificado");
    static final URI TIPO_TRANSICION_INVALIDA = URI.create("/problemas/transicion-invalida");
    static final String DETALLE_VALIDACION = "La solicitud contiene datos inválidos";

    static String actor(Authentication autenticacion) {
        return autenticacion.getName();
    }

    @ExceptionHandler(ConfiguracionInvalidaException.class)
    ResponseEntity<ProblemDetail> invalida(ConfiguracionInvalidaException ex, HttpServletRequest peticion) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, null, DETALLE_VALIDACION, peticion);
        problema.setProperty("errores", List.of(new ErrorDeCampo(ex.campo(), ex.getMessage())));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(HorarioSuperpuestoException.class)
    ResponseEntity<ProblemDetail> superpuesto(HorarioSuperpuestoException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problema(HttpStatus.CONFLICT, TIPO_SUPERPUESTO, ex.getMessage(), peticion));
    }

    @ExceptionHandler(PrestacionDuplicadaException.class)
    ResponseEntity<ProblemDetail> duplicada(PrestacionDuplicadaException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(problema(HttpStatus.CONFLICT, TIPO_DUPLICADA, ex.getMessage(), peticion));
    }

    @ExceptionHandler(RecursoInexistenteException.class)
    ResponseEntity<ProblemDetail> inexistente(RecursoInexistenteException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problema(HttpStatus.NOT_FOUND, TIPO_NO_ENCONTRADO, ex.getMessage(), peticion));
    }

    @ExceptionHandler(TurnoSuperpuestoException.class)
    ResponseEntity<ProblemDetail> turnoSuperpuesto(TurnoSuperpuestoException ex, HttpServletRequest peticion) {
        return conflicto(TIPO_TURNO_SUPERPUESTO, ex.getMessage(), peticion);
    }

    @ExceptionHandler(TurnoModificadoException.class)
    ResponseEntity<ProblemDetail> turnoModificado(TurnoModificadoException ex, HttpServletRequest peticion) {
        return conflicto(TIPO_TURNO_MODIFICADO, ex.getMessage(), peticion);
    }

    @ExceptionHandler(TransicionInvalidaException.class)
    ResponseEntity<ProblemDetail> transicionInvalida(TransicionInvalidaException ex, HttpServletRequest peticion) {
        return conflicto(TIPO_TRANSICION_INVALIDA, ex.getMessage(), peticion);
    }

    private static ResponseEntity<ProblemDetail> conflicto(URI tipo, String detalle, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(HttpStatus.CONFLICT, tipo, detalle, peticion));
    }

    private static ProblemDetail problema(HttpStatus estado, URI tipo, String detalle, HttpServletRequest peticion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        if (tipo != null) {
            problema.setType(tipo);
        }
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return problema;
    }
}
