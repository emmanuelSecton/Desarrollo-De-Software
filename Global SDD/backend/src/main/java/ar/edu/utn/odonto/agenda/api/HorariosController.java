package ar.edu.utn.odonto.agenda.api;

import ar.edu.utn.odonto.agenda.application.ServicioDeConfiguracion;
import ar.edu.utn.odonto.agenda.domain.FranjaDeAtencion;
import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.shared.api.Api;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalTime;
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
import org.springframework.web.bind.annotation.RestController;

/** Franjas de atencion por dia (US-001, C-05). Lee el personal; modifica el odontologo. */
@RestController
@RequestMapping(Api.V1 + "/horarios")
public class HorariosController extends ControladorDeAgenda {

    public record Franja(
            @NotNull @Min(1) @Max(7) Integer diaSemana,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime desde,
            @NotNull @JsonFormat(pattern = "HH:mm") LocalTime hasta) {}

    public record FranjaRespuesta(
            UUID id,
            int diaSemana,
            @JsonFormat(pattern = "HH:mm") LocalTime desde,
            @JsonFormat(pattern = "HH:mm") LocalTime hasta) {

        static FranjaRespuesta de(FranjaDeAtencion franja) {
            return new FranjaRespuesta(franja.id(), franja.diaSemana(), franja.desde(), franja.hasta());
        }
    }

    private final ServicioDeConfiguracion servicio;

    public HorariosController(ServicioDeConfiguracion servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize(Permisos.PERSONAL)
    public List<FranjaRespuesta> listar() {
        return servicio.horarios().stream().map(FranjaRespuesta::de).toList();
    }

    @PostMapping
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<FranjaRespuesta> crear(@Valid @RequestBody Franja franja, Authentication autenticacion) {
        FranjaDeAtencion creada =
                servicio.crearHorario(franja.diaSemana(), franja.desde(), franja.hasta(), actor(autenticacion));
        return ResponseEntity.created(URI.create(Api.V1 + "/horarios/" + creada.id())).body(FranjaRespuesta.de(creada));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public FranjaRespuesta modificar(
            @PathVariable UUID id, @Valid @RequestBody Franja franja, Authentication autenticacion) {
        return FranjaRespuesta.de(servicio.modificarHorario(
                id, franja.diaSemana(), franja.desde(), franja.hasta(), actor(autenticacion)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permisos.SOLO_ODONTOLOGO)
    public ResponseEntity<Void> borrar(@PathVariable UUID id, Authentication autenticacion) {
        servicio.borrarHorario(id, actor(autenticacion));
        return ResponseEntity.noContent().build();
    }
}
