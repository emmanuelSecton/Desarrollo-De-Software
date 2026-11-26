package ar.edu.utn.odonto.agenda.api;

import ar.edu.utn.odonto.agenda.application.ServicioDeConfiguracion;
import ar.edu.utn.odonto.agenda.domain.Prestacion;
import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.shared.api.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de prestaciones (US-001, C-05). Lee el personal; modifica el odontologo. Baja logica (D5). */
@RestController
@RequestMapping(Api.V1 + "/prestaciones")
public class PrestacionesController extends ControladorDeAgenda {

    public record DatosPrestacion(
            @NotBlank @Size(max = 80) String nombre,
            @NotNull @Min(5) @Max(Prestacion.DURACION_MAXIMA) Integer duracionMinutos,
            @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal precioReferencia,
            Boolean reservableOnline) {

        /** Si no se indica, la prestacion se puede reservar online. */
        Prestacion.Datos datos() {
            return new Prestacion.Datos(nombre, duracionMinutos, precioReferencia,
                    reservableOnline == null || reservableOnline);
        }
    }

    public record PrestacionRespuesta(
            UUID id, String nombre, int duracionMinutos, BigDecimal precioReferencia, boolean reservableOnline,
            boolean activa) {

        static PrestacionRespuesta de(Prestacion prestacion) {
            Prestacion.Datos d = prestacion.datos();
            return new PrestacionRespuesta(prestacion.id(), d.nombre(), d.duracionMinutos(), d.precioReferencia(),
                    d.reservableOnline(), prestacion.activa());
        }
    }

    private final ServicioDeConfiguracion servicio;

    public PrestacionesController(ServicioDeConfiguracion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Permisos.PERSONAL)
    public List<PrestacionRespuesta> listar(@RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return servicio.prestaciones(incluirInactivas).stream().map(PrestacionRespuesta::de).toList();
    }

    @PostMapping
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<PrestacionRespuesta> crear(
            @Valid @RequestBody DatosPrestacion datos, Authentication autenticacion) {
        Prestacion creada = servicio.crearPrestacion(datos.datos(), actor(autenticacion));
        return ResponseEntity.created(URI.create(Api.V1 + "/prestaciones/" + creada.id()))
                .body(PrestacionRespuesta.de(creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public PrestacionRespuesta modificar(
            @PathVariable UUID id, @Valid @RequestBody DatosPrestacion datos, Authentication autenticacion) {
        return PrestacionRespuesta.de(servicio.modificarPrestacion(id, datos.datos(), actor(autenticacion)));
    }

    /** Baja logica: la prestacion sigue existiendo para los turnos que la usan. */
    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<Void> darDeBaja(@PathVariable UUID id, Authentication autenticacion) {
        servicio.darDeBajaPrestacion(id, actor(autenticacion));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reactivar")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<Void> reactivar(@PathVariable UUID id, Authentication autenticacion) {
        servicio.reactivarPrestacion(id, actor(autenticacion));
        return ResponseEntity.noContent().build();
    }
}
