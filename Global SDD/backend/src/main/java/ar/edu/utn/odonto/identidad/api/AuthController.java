package ar.edu.utn.odonto.identidad.api;

import ar.edu.utn.odonto.identidad.application.Autoridades;
import ar.edu.utn.odonto.identidad.application.CambioPasswordInvalidoException;
import ar.edu.utn.odonto.identidad.application.Problemas;
import ar.edu.utn.odonto.identidad.application.ResultadoLogin;
import ar.edu.utn.odonto.identidad.application.ServicioDeCuenta;
import ar.edu.utn.odonto.identidad.application.ServicioDeLogin;
import ar.edu.utn.odonto.identidad.application.UsuarioAutenticado;
import ar.edu.utn.odonto.shared.api.Api;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sesion del personal interno (Flujo 6, DD-08, D4). El paciente no tiene cuenta (RN-SEG-01). */
@RestController
@RequestMapping(Api.V1 + "/auth")
public class AuthController {

    static final String TITULO_CREDENCIALES = "Credenciales inválidas";
    /** Identico para todo rechazo: no revela si la cuenta existe, esta inactiva o bloqueada (D5). */
    static final String DETALLE_CREDENCIALES =
            "El email o la contraseña no son válidos, o la cuenta está bloqueada temporalmente";
    static final String TITULO_PASSWORD_INVALIDA = "Contraseña inválida";

    public record Credenciales(
            @NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 200) String password) {

        /** Nunca imprime la contraseña. */
        @Override
        public String toString() {
            return "Credenciales[***]";
        }
    }

    public record CambioPassword(@NotNull @Size(max = 200) String actual, @NotNull @Size(max = 200) String nueva) {

        @Override
        public String toString() {
            return "CambioPassword[***]";
        }
    }

    public record UsuarioActual(UUID id, String nombre, String email, String rol, boolean debeCambiarPassword) {

        static UsuarioActual de(UsuarioAutenticado usuario) {
            return new UsuarioActual(usuario.id(), usuario.nombre(), usuario.email(), usuario.rol().name(),
                    usuario.debeCambiarPassword());
        }
    }

    public record TokenCsrf(String token, String cabecera) {}

    private final ServicioDeLogin login;
    private final ServicioDeCuenta cuentas;
    private final SecurityContextRepository contextos;
    private final Duration inactividad;

    public AuthController(
            ServicioDeLogin login,
            ServicioDeCuenta cuentas,
            SecurityContextRepository contextos,
            @Value("${identidad.inactividad-sesion:30m}") Duration inactividad) {
        this.login = login;
        this.cuentas = cuentas;
        this.contextos = contextos;
        this.inactividad = inactividad;
    }

    /** Fuerza la emision del token CSRF (cookie {@code XSRF-TOKEN}) antes del primer POST (D7). */
    @GetMapping("/csrf")
    public TokenCsrf csrf(CsrfToken token) {
        return new TokenCsrf(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody Credenciales credenciales, HttpServletRequest peticion, HttpServletResponse respuesta) {
        ResultadoLogin resultado =
                login.autenticar(credenciales.email(), credenciales.password(), peticion.getRemoteAddr());
        return switch (resultado) {
            case ResultadoLogin.Rechazado rechazo -> credencialesInvalidas(peticion);
            case ResultadoLogin.Exitoso exito -> {
                iniciarSesion(exito.usuario(), peticion, respuesta);
                yield ResponseEntity.ok(UsuarioActual.de(exito.usuario()));
            }
        };
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication autenticacion, HttpServletRequest peticion) {
        Optional<UsuarioAutenticado> usuario = idDe(autenticacion).flatMap(cuentas::buscar);
        if (usuario.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema(
                    HttpStatus.UNAUTHORIZED, Problemas.NO_AUTENTICADO, "No autenticado",
                    "Se requiere iniciar sesión", peticion));
        }
        return ResponseEntity.ok(UsuarioActual.de(usuario.get()));
    }

    @PostMapping("/password")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambioPassword cambio,
            Authentication autenticacion,
            HttpServletRequest peticion,
            HttpServletResponse respuesta) {
        UUID id = idDe(autenticacion)
                .orElseThrow(() -> new CambioPasswordInvalidoException("La contraseña actual no es correcta"));
        UsuarioAutenticado usuario =
                cuentas.cambiarPassword(id, cambio.actual(), cambio.nueva(), peticion.getRemoteAddr());
        // Nuevas autoridades (su rol) y nuevo id de sesion tras el cambio (D8).
        iniciarSesion(usuario, peticion, respuesta);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(CambioPasswordInvalidoException.class)
    ResponseEntity<ProblemDetail> passwordInvalida(CambioPasswordInvalidoException ex, HttpServletRequest peticion) {
        return ResponseEntity.badRequest().body(problema(
                HttpStatus.BAD_REQUEST, Problemas.PASSWORD_INVALIDA, TITULO_PASSWORD_INVALIDA, ex.getMessage(),
                peticion));
    }

    private void iniciarSesion(UsuarioAutenticado usuario, HttpServletRequest peticion, HttpServletResponse respuesta) {
        if (peticion.getSession(false) != null) {
            peticion.changeSessionId();
        }
        HttpSession sesion = peticion.getSession(true);
        sesion.setMaxInactiveInterval((int) inactividad.toSeconds());

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario.id(), null, Autoridades.de(usuario)));
        SecurityContextHolder.setContext(contexto);
        contextos.saveContext(contexto, peticion, respuesta);
    }

    private static ResponseEntity<ProblemDetail> credencialesInvalidas(HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema(
                HttpStatus.UNAUTHORIZED, Problemas.CREDENCIALES_INVALIDAS, TITULO_CREDENCIALES, DETALLE_CREDENCIALES,
                peticion));
    }

    private static ProblemDetail problema(
            HttpStatus estado, URI tipo, String titulo, String detalle, HttpServletRequest peticion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setType(tipo);
        problema.setTitle(titulo);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return problema;
    }

    private static Optional<UUID> idDe(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getPrincipal() instanceof UUID id
                ? Optional.of(id)
                : Optional.empty();
    }
}
