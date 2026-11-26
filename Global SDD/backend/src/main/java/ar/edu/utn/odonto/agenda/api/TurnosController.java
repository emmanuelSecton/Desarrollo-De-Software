package ar.edu.utn.odonto.agenda.api;

import ar.edu.utn.odonto.agenda.application.ServicioDeTurnos;
import ar.edu.utn.odonto.agenda.application.TurnoListado;
import ar.edu.utn.odonto.agenda.domain.CanceladoPor;
import ar.edu.utn.odonto.agenda.domain.Turno;
import ar.edu.utn.odonto.identidad.application.Permisos;
import ar.edu.utn.odonto.shared.api.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Turnos internos (US-011, C-10, D11). Odontologo y secretaria tienen CRUD de turnos. */
@RestController
@RequestMapping(Api.V1 + "/turnos")
@PreAuthorize(Permisos.PERSONAL)
public class TurnosController extends ControladorDeAgenda {

    public record AltaTurno(
            @NotNull UUID pacienteId,
            @NotNull UUID prestacionId,
            @NotNull OffsetDateTime inicio,
            Integer duracionMinutos,
            boolean sobreturno) {}

    public record Reprogramacion(@NotNull Integer version, @NotNull OffsetDateTime inicio, Integer duracionMinutos) {}

    public record Cancelacion(
            @NotNull Integer version, @NotNull CanceladoPor canceladoPor, @NotBlank @Size(max = 200) String motivo) {

        /** El motivo puede tener datos de salud: no se imprime. */
        @Override
        public String toString() {
            return "Cancelacion[version=" + version + "]";
        }
    }

    public record Confirmacion(@NotNull Integer version) {}

    public record TurnoRespuesta(
            UUID id,
            UUID pacienteId,
            UUID prestacionId,
            Instant inicio,
            Instant fin,
            int duracionMinutos,
            String estado,
            String origen,
            boolean sobreturno,
            UUID reprogramadoDe,
            String canceladoPor,
            String motivoCancelacion,
            String tokenPublico,
            int version) {

        static TurnoRespuesta de(Turno turno) {
            return new TurnoRespuesta(turno.id(), turno.pacienteId(), turno.prestacionId(), turno.inicio(), turno.fin(),
                    turno.duracionMinutos(), turno.estado().name(), turno.origen().name(), turno.sobreturno(),
                    turno.reprogramadoDe(), turno.canceladoPor() == null ? null : turno.canceladoPor().name(),
                    turno.motivoCancelacion(), turno.tokenPublico(), turno.version());
        }
    }

    private final ServicioDeTurnos servicio;

    public TurnosController(ServicioDeTurnos servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<TurnoListado> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
        return servicio.listar(desde.toInstant(), hasta.toInstant());
    }

    @GetMapping("/{id}")
    public TurnoRespuesta obtener(@PathVariable UUID id) {
        return TurnoRespuesta.de(servicio.obtener(id));
    }

    @PostMapping
    public ResponseEntity<TurnoRespuesta> crear(@Valid @RequestBody AltaTurno alta, Authentication autenticacion) {
        Turno turno = servicio.crear(alta.pacienteId(), alta.prestacionId(), alta.inicio().toInstant(),
                alta.duracionMinutos(), alta.sobreturno(), actor(autenticacion));
        return ResponseEntity.created(URI.create(Api.V1 + "/turnos/" + turno.id())).body(TurnoRespuesta.de(turno));
    }

    /** Mover un turno es reprogramarlo (RN-TU-03): devuelve el turno nuevo. */
    @PutMapping("/{id}")
    public TurnoRespuesta reprogramar(
            @PathVariable UUID id, @Valid @RequestBody Reprogramacion cambio, Authentication autenticacion) {
        return TurnoRespuesta.de(servicio.reprogramar(id, cambio.version(), cambio.inicio().toInstant(),
                cambio.duracionMinutos(), actor(autenticacion)));
    }

    @PostMapping("/{id}/cancelar")
    public TurnoRespuesta cancelar(
            @PathVariable UUID id, @Valid @RequestBody Cancelacion cancelacion, Authentication autenticacion) {
        return TurnoRespuesta.de(servicio.cancelar(id, cancelacion.version(), cancelacion.canceladoPor(),
                cancelacion.motivo(), actor(autenticacion)));
    }

    @PostMapping("/{id}/confirmar")
    public TurnoRespuesta confirmar(
            @PathVariable UUID id, @Valid @RequestBody Confirmacion confirmacion, Authentication autenticacion) {
        return TurnoRespuesta.de(servicio.confirmar(id, confirmacion.version(), actor(autenticacion)));
    }
}
