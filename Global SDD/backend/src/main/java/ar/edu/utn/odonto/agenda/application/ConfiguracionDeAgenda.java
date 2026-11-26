package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.Consultorio;
import ar.edu.utn.odonto.agenda.domain.FranjaDeAtencion;
import ar.edu.utn.odonto.agenda.domain.Prestacion;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de solo lectura de la configuracion para otros modulos y changes (C-10, C-14, C-20; D1).
 * Devuelve vistas propias para que nadie dependa de las clases de dominio de agenda.
 */
@Service
public class ConfiguracionDeAgenda {

    public record ParametrosVista(
            String nombreConsultorio,
            String slugReserva,
            int recordatorioHorasAntes,
            int confirmacionLimiteHoras,
            int cancelacionMinimaHoras,
            int ofertaVencimientoMinutos) {}

    public record FranjaVista(UUID id, int diaSemana, LocalTime desde, LocalTime hasta) {}

    public record PrestacionVista(
            UUID id, String nombre, int duracionMinutos, BigDecimal precioReferencia, boolean reservableOnline,
            boolean activa) {}

    private final RepositorioConsultorio consultorios;
    private final RepositorioHorarios horarios;
    private final RepositorioPrestaciones prestaciones;

    public ConfiguracionDeAgenda(
            RepositorioConsultorio consultorios, RepositorioHorarios horarios, RepositorioPrestaciones prestaciones) {
        this.consultorios = consultorios;
        this.horarios = horarios;
        this.prestaciones = prestaciones;
    }

    @Transactional(readOnly = true)
    public ParametrosVista parametros() {
        Consultorio consultorio = consultorios.obtener();
        Consultorio.Parametros p = consultorio.parametros();
        return new ParametrosVista(consultorio.datos().nombre(), consultorio.datos().slugReserva(),
                p.recordatorioHorasAntes(), p.confirmacionLimiteHoras(), p.cancelacionMinimaHoras(),
                p.ofertaVencimientoMinutos());
    }

    @Transactional(readOnly = true)
    public List<FranjaVista> franjas() {
        return horarios.listar().stream().map(ConfiguracionDeAgenda::vista).toList();
    }

    @Transactional(readOnly = true)
    public Optional<PrestacionVista> prestacion(UUID id) {
        return prestaciones.buscarPrestacion(id).map(ConfiguracionDeAgenda::vista);
    }

    private static FranjaVista vista(FranjaDeAtencion franja) {
        return new FranjaVista(franja.id(), franja.diaSemana(), franja.desde(), franja.hasta());
    }

    private static PrestacionVista vista(Prestacion prestacion) {
        Prestacion.Datos d = prestacion.datos();
        return new PrestacionVista(prestacion.id(), d.nombre(), d.duracionMinutos(), d.precioReferencia(),
                d.reservableOnline(), prestacion.activa());
    }
}
