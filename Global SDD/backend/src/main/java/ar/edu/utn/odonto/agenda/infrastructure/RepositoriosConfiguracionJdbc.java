package ar.edu.utn.odonto.agenda.infrastructure;

import ar.edu.utn.odonto.agenda.application.RepositorioConsultorio;
import ar.edu.utn.odonto.agenda.application.RepositorioHorarios;
import ar.edu.utn.odonto.agenda.application.RepositorioPrestaciones;
import ar.edu.utn.odonto.agenda.domain.Consultorio;
import ar.edu.utn.odonto.agenda.domain.FranjaDeAtencion;
import ar.edu.utn.odonto.agenda.domain.Prestacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC de consultorio, franjas y prestaciones (C-05, D3-D5); sin entidades JPA. */
@Repository
public class RepositoriosConfiguracionJdbc
        implements RepositorioConsultorio, RepositorioHorarios, RepositorioPrestaciones {

    private static final String COLUMNAS_CONSULTORIO = "id, nombre, profesional_nombre, matricula, direccion, "
            + "telefono, email, slug_reserva, recordatorio_horas_antes, confirmacion_limite_horas, "
            + "cancelacion_minima_horas, oferta_vencimiento_minutos, actualizado_at";

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider;

    public RepositoriosConfiguracionJdbc(ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return jdbcProvider.getObject();
    }

    // ---- Consultorio -------------------------------------------------------

    @Override
    public Consultorio obtener() {
        return jdbc().queryForObject("select " + COLUMNAS_CONSULTORIO + " from consultorio",
                new MapSqlParameterSource(), RepositoriosConfiguracionJdbc::consultorio);
    }

    @Override
    public Consultorio obtenerParaActualizar() {
        return jdbc().queryForObject("select " + COLUMNAS_CONSULTORIO + " from consultorio for update",
                new MapSqlParameterSource(), RepositoriosConfiguracionJdbc::consultorio);
    }

    @Override
    public void actualizar(Consultorio consultorio) {
        Consultorio.Datos d = consultorio.datos();
        Consultorio.Parametros p = consultorio.parametros();
        jdbc().update("update consultorio set nombre = :nombre, profesional_nombre = :profesional, "
                        + "matricula = :matricula, direccion = :direccion, telefono = :telefono, "
                        + "email = cast(:email as citext), slug_reserva = :slug, recordatorio_horas_antes = :recordatorio, "
                        + "confirmacion_limite_horas = :confirmacion, cancelacion_minima_horas = :cancelacion, "
                        + "oferta_vencimiento_minutos = :oferta, actualizado_at = :actualizado where id = :id",
                new MapSqlParameterSource()
                        .addValue("id", consultorio.id())
                        .addValue("nombre", d.nombre())
                        .addValue("profesional", d.profesionalNombre())
                        .addValue("matricula", d.matricula())
                        .addValue("direccion", d.direccion())
                        .addValue("telefono", d.telefono())
                        .addValue("email", d.email())
                        .addValue("slug", d.slugReserva())
                        .addValue("recordatorio", p.recordatorioHorasAntes())
                        .addValue("confirmacion", p.confirmacionLimiteHoras())
                        .addValue("cancelacion", p.cancelacionMinimaHoras())
                        .addValue("oferta", p.ofertaVencimientoMinutos())
                        .addValue("actualizado", Timestamp.from(consultorio.actualizadoAt())));
    }

    private static Consultorio consultorio(ResultSet fila, int numero) throws SQLException {
        return new Consultorio(
                fila.getObject("id", UUID.class),
                new Consultorio.Datos(fila.getString("nombre"), fila.getString("profesional_nombre"),
                        fila.getString("matricula"), fila.getString("direccion"), fila.getString("telefono"),
                        fila.getString("email"), fila.getString("slug_reserva")),
                new Consultorio.Parametros(fila.getInt("recordatorio_horas_antes"),
                        fila.getInt("confirmacion_limite_horas"), fila.getInt("cancelacion_minima_horas"),
                        fila.getInt("oferta_vencimiento_minutos")),
                fila.getTimestamp("actualizado_at").toInstant());
    }

    // ---- Franjas de atencion -----------------------------------------------

    @Override
    public List<FranjaDeAtencion> listar() {
        return jdbc().query("select id, dia_semana, hora_desde, hora_hasta from horario_atencion "
                + "order by dia_semana, hora_desde", RepositoriosConfiguracionJdbc::franja);
    }

    @Override
    public Optional<FranjaDeAtencion> buscar(UUID id) {
        return jdbc().query("select id, dia_semana, hora_desde, hora_hasta from horario_atencion where id = :id",
                        new MapSqlParameterSource("id", id), RepositoriosConfiguracionJdbc::franja)
                .stream()
                .findFirst();
    }

    @Override
    public void insertar(FranjaDeAtencion franja) {
        jdbc().update("insert into horario_atencion (id, dia_semana, hora_desde, hora_hasta) "
                + "values (:id, :dia, :desde, :hasta)", parametros(franja));
    }

    @Override
    public void actualizar(FranjaDeAtencion franja) {
        jdbc().update("update horario_atencion set dia_semana = :dia, hora_desde = :desde, hora_hasta = :hasta "
                + "where id = :id", parametros(franja));
    }

    @Override
    public void borrar(UUID id) {
        jdbc().update("delete from horario_atencion where id = :id", new MapSqlParameterSource("id", id));
    }

    private static MapSqlParameterSource parametros(FranjaDeAtencion franja) {
        return new MapSqlParameterSource()
                .addValue("id", franja.id())
                .addValue("dia", franja.diaSemana())
                .addValue("desde", Time.valueOf(franja.desde()))
                .addValue("hasta", Time.valueOf(franja.hasta()));
    }

    private static FranjaDeAtencion franja(ResultSet fila, int numero) throws SQLException {
        return new FranjaDeAtencion(fila.getObject("id", UUID.class), fila.getInt("dia_semana"),
                fila.getTime("hora_desde").toLocalTime(), fila.getTime("hora_hasta").toLocalTime());
    }

    // ---- Prestaciones --------------------------------------------------------

    @Override
    public List<Prestacion> listar(boolean incluirInactivas) {
        return jdbc().query("select id, nombre, duracion_minutos, precio_referencia, reservable_online, activa "
                        + "from prestacion where activa or :incluirInactivas order by lower(nombre), id",
                new MapSqlParameterSource("incluirInactivas", incluirInactivas),
                RepositoriosConfiguracionJdbc::prestacion);
    }

    @Override
    public Optional<Prestacion> buscarPrestacion(UUID id) {
        return jdbc().query("select id, nombre, duracion_minutos, precio_referencia, reservable_online, activa "
                                + "from prestacion where id = :id",
                        new MapSqlParameterSource("id", id), RepositoriosConfiguracionJdbc::prestacion)
                .stream()
                .findFirst();
    }

    @Override
    public void insertar(Prestacion prestacion) {
        jdbc().update("insert into prestacion (id, nombre, duracion_minutos, precio_referencia, reservable_online, "
                + "activa) values (:id, :nombre, :duracion, :precio, :reservable, :activa)", parametros(prestacion));
    }

    @Override
    public void actualizar(Prestacion prestacion) {
        jdbc().update("update prestacion set nombre = :nombre, duracion_minutos = :duracion, "
                + "precio_referencia = :precio, reservable_online = :reservable, activa = :activa where id = :id",
                parametros(prestacion));
    }

    private static MapSqlParameterSource parametros(Prestacion prestacion) {
        Prestacion.Datos d = prestacion.datos();
        return new MapSqlParameterSource()
                .addValue("id", prestacion.id())
                .addValue("nombre", d.nombre())
                .addValue("duracion", d.duracionMinutos())
                .addValue("precio", d.precioReferencia())
                .addValue("reservable", d.reservableOnline())
                .addValue("activa", prestacion.activa());
    }

    private static Prestacion prestacion(ResultSet fila, int numero) throws SQLException {
        return new Prestacion(fila.getObject("id", UUID.class),
                new Prestacion.Datos(fila.getString("nombre"), fila.getInt("duracion_minutos"),
                        fila.getBigDecimal("precio_referencia"), fila.getBoolean("reservable_online")),
                fila.getBoolean("activa"));
    }
}
