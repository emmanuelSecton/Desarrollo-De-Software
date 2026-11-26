package ar.edu.utn.odonto.agenda.api;

import ar.edu.utn.odonto.agenda.application.ServicioDeConfiguracion;
import ar.edu.utn.odonto.agenda.domain.Consultorio;
import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.shared.api.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos del consultorio y parametros de notificacion (US-001, C-05). Lee el personal; modifica el odontologo. */
@RestController
@RequestMapping(Api.V1 + "/consultorio")
public class ConsultorioController extends ControladorDeConfiguracion {

    /** Entrada y salida: mismos campos; la regla cruzada (confirmacion < recordatorio) la valida el dominio. */
    public record DatosConsultorio(
            UUID id,
            @NotBlank @Size(max = 120) String nombre,
            @NotBlank @Size(max = 120) String profesionalNombre,
            @Size(max = 30) String matricula,
            @Size(max = 200) String direccion,
            @Size(max = 30) String telefono,
            @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[a-z0-9-]{3,60}",
                    message = "debe tener entre 3 y 60 caracteres en minúscula, dígitos o guiones")
                    String slugReserva,
            @NotNull @Min(1) @Max(168) Integer recordatorioHorasAntes,
            @NotNull @Min(1) @Max(167) Integer confirmacionLimiteHoras,
            @NotNull @Min(0) @Max(168) Integer cancelacionMinimaHoras,
            @NotNull @Min(5) @Max(1440) Integer ofertaVencimientoMinutos) {

        static DatosConsultorio de(Consultorio consultorio) {
            Consultorio.Datos d = consultorio.datos();
            Consultorio.Parametros p = consultorio.parametros();
            return new DatosConsultorio(consultorio.id(), d.nombre(), d.profesionalNombre(), d.matricula(),
                    d.direccion(), d.telefono(), d.email(), d.slugReserva(), p.recordatorioHorasAntes(),
                    p.confirmacionLimiteHoras(), p.cancelacionMinimaHoras(), p.ofertaVencimientoMinutos());
        }

        Consultorio.Datos datos() {
            return new Consultorio.Datos(nombre.trim(), profesionalNombre.trim(), vacioANulo(matricula),
                    vacioANulo(direccion), vacioANulo(telefono), vacioANulo(email), slugReserva);
        }

        Consultorio.Parametros parametros() {
            return new Consultorio.Parametros(recordatorioHorasAntes, confirmacionLimiteHoras, cancelacionMinimaHoras,
                    ofertaVencimientoMinutos);
        }

        private static String vacioANulo(String texto) {
            return texto == null || texto.isBlank() ? null : texto.trim();
        }
    }

    private final ServicioDeConfiguracion servicio;

    public ConsultorioController(ServicioDeConfiguracion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Permisos.PERSONAL)
    public DatosConsultorio obtener() {
        return DatosConsultorio.de(servicio.consultorio());
    }

    @PutMapping
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public DatosConsultorio modificar(@Valid @RequestBody DatosConsultorio datos, Authentication autenticacion) {
        return DatosConsultorio.de(
                servicio.modificarConsultorio(datos.datos(), datos.parametros(), actor(autenticacion)));
    }
}
