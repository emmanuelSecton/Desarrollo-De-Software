package ar.edu.utn.odonto.pacientes.api;

import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.pacientes.application.Actor;
import ar.edu.utn.odonto.pacientes.application.DatoDePacienteInvalidoException;
import ar.edu.utn.odonto.pacientes.application.DatosDePaciente;
import ar.edu.utn.odonto.pacientes.application.PacienteDuplicadoException;
import ar.edu.utn.odonto.pacientes.application.PacienteInexistenteException;
import ar.edu.utn.odonto.pacientes.application.PacienteVista;
import ar.edu.utn.odonto.pacientes.application.Pagina;
import ar.edu.utn.odonto.pacientes.application.RegistroDePacientes;
import ar.edu.utn.odonto.shared.api.Api;
import ar.edu.utn.odonto.shared.api.ManejadorGlobalDeErrores.ErrorDeCampo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pacientes del consultorio (C-09). Odontologo y secretaria: CRU; baja y reactivacion: solo odontologo (D8). */
@RestController
@RequestMapping(Api.V1 + "/pacientes")
@PreAuthorize(Permisos.PERSONAL)
public class PacientesController {

    static final URI TIPO_DUPLICADO = URI.create("/problemas/paciente-duplicado");
    static final URI TIPO_INEXISTENTE = URI.create("/problemas/paciente-inexistente");
    static final String DETALLE_VALIDACION = "La solicitud contiene datos inválidos";

    public record AltaPaciente(
            @NotBlank @Size(max = 13) String dni,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @PastOrPresent LocalDate fechaNacimiento,
            @NotBlank @Size(max = 30) String telefono,
            @Email @Size(max = 254) String email,
            boolean aceptaWhatsapp,
            @NotNull @AssertTrue(message = "es obligatorio aceptar el tratamiento de datos") Boolean consentimientoDatos) {

        DatosDePaciente datos() {
            return new DatosDePaciente(dni, nombre, apellido, fechaNacimiento, telefono, email, aceptaWhatsapp);
        }

        @Override
        public String toString() {
            return "AltaPaciente[***]";
        }
    }

    public record CambioPaciente(
            @NotBlank @Size(max = 13) String dni,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 80) String apellido,
            @PastOrPresent LocalDate fechaNacimiento,
            @NotBlank @Size(max = 30) String telefono,
            @Email @Size(max = 254) String email,
            boolean aceptaWhatsapp) {

        DatosDePaciente datos() {
            return new DatosDePaciente(dni, nombre, apellido, fechaNacimiento, telefono, email, aceptaWhatsapp);
        }

        @Override
        public String toString() {
            return "CambioPaciente[***]";
        }
    }

    public record PacienteRespuesta(
            UUID id,
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            String telefono,
            String email,
            boolean aceptaWhatsapp,
            int ausenciasCount,
            boolean activo,
            Instant consentimientoDatosAt) {

        static PacienteRespuesta de(PacienteVista vista) {
            return new PacienteRespuesta(vista.id(), vista.dni(), vista.nombre(), vista.apellido(),
                    vista.fechaNacimiento(), vista.telefono(), vista.email(), vista.aceptaWhatsapp(),
                    vista.ausenciasCount(), vista.activo(), vista.consentimientoDatosAt());
        }
    }

    public record PaginaRespuesta(List<PacienteRespuesta> contenido, int pagina, int tamano, long total) {

        static PaginaRespuesta de(Pagina<PacienteVista> pagina) {
            return new PaginaRespuesta(pagina.contenido().stream().map(PacienteRespuesta::de).toList(),
                    pagina.pagina(), pagina.tamano(), pagina.total());
        }
    }

    private final RegistroDePacientes registro;

    public PacientesController(RegistroDePacientes registro) {
        this.registro = registro;
    }

    @GetMapping
    public PaginaRespuesta buscar(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PaginaRespuesta.de(registro.buscar(q, incluirInactivos, pagina, tamano));
    }

    @PostMapping
    public ResponseEntity<PacienteRespuesta> alta(@Valid @RequestBody AltaPaciente alta, Authentication autenticacion) {
        PacienteVista creado = registro.alta(alta.datos(), actor(autenticacion));
        return ResponseEntity.created(URI.create(Api.V1 + "/pacientes/" + creado.id()))
                .body(PacienteRespuesta.de(creado));
    }

    @GetMapping("/{id}")
    public PacienteRespuesta obtener(@PathVariable UUID id) {
        return PacienteRespuesta.de(registro.obtener(id));
    }

    @PutMapping("/{id}")
    public PacienteRespuesta modificar(
            @PathVariable UUID id, @Valid @RequestBody CambioPaciente cambio, Authentication autenticacion) {
        return PacienteRespuesta.de(registro.modificar(id, cambio.datos(), actor(autenticacion)));
    }

    /** Baja logica: nunca borra (RN-HC-03). */
    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id, Authentication autenticacion) {
        registro.darDeBaja(id, actor(autenticacion));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<Void> reactivar(@PathVariable UUID id, Authentication autenticacion) {
        registro.reactivar(id, actor(autenticacion));
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DatoDePacienteInvalidoException.class)
    ResponseEntity<ProblemDetail> datoInvalido(DatoDePacienteInvalidoException ex, HttpServletRequest peticion) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, null, DETALLE_VALIDACION, peticion);
        problema.setProperty("errores", List.of(new ErrorDeCampo(ex.campo(), ex.getMessage())));
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(PacienteDuplicadoException.class)
    ResponseEntity<ProblemDetail> duplicado(PacienteDuplicadoException ex, HttpServletRequest peticion) {
        ProblemDetail problema = problema(HttpStatus.CONFLICT, TIPO_DUPLICADO, ex.getMessage(), peticion);
        if (ex.existente() != null) {
            problema.setProperty("pacienteId", ex.existente());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema);
    }

    /** Dos cambios de DNI simultaneos: lo frena la constraint UNIQUE de la base. */
    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ProblemDetail> duplicadoEnBase(DuplicateKeyException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problema(
                HttpStatus.CONFLICT, TIPO_DUPLICADO, "Ya existe un paciente con ese DNI", peticion));
    }

    @ExceptionHandler(PacienteInexistenteException.class)
    ResponseEntity<ProblemDetail> inexistente(PacienteInexistenteException ex, HttpServletRequest peticion) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(problema(HttpStatus.NOT_FOUND, TIPO_INEXISTENTE, ex.getMessage(), peticion));
    }

    private static Actor actor(Authentication autenticacion) {
        return Actor.usuario(autenticacion.getName());
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
