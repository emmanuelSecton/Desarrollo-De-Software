package ar.edu.utn.odonto.agenda.infrastructure;

import ar.edu.utn.odonto.agenda.application.RepositorioTurnos;
import ar.edu.utn.odonto.agenda.application.TurnoListado;
import ar.edu.utn.odonto.agenda.domain.CanceladoPor;
import ar.edu.utn.odonto.agenda.domain.EstadoTurno;
import ar.edu.utn.odonto.agenda.domain.OrigenTurno;
import ar.edu.utn.odonto.agenda.domain.Turno;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC de {@code turno} (C-10, D1, D5, D8). */
@Repository
public class RepositorioTurnosJdbc implements RepositorioTurnos {

    private static final String COLUMNAS = "id, paciente_id, prestacion_id, inicio, fin, estado, origen, sobreturno, "
            + "reprogramado_de, token_publico, cancelado_por, motivo_cancelacion, creado_at, actualizado_at, version";

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider;

    public RepositorioTurnosJdbc(ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return jdbcProvider.getObject();
    }

    @Override
    public void insertar(Turno turno) {
        jdbc().update("insert into turno (" + COLUMNAS + ") values (:id, :paciente, :prestacion, :inicio, :fin, "
                        + ":estado, :origen, :sobreturno, :reprogramadoDe, :token, :canceladoPor, :motivo, :creado, "
                        + ":actualizado, 0)",
                new MapSqlParameterSource()
                        .addValue("id", turno.id())
                        .addValue("paciente", turno.pacienteId())
                        .addValue("prestacion", turno.prestacionId())
                        .addValue("inicio", Timestamp.from(turno.inicio()))
                        .addValue("fin", Timestamp.from(turno.fin()))
                        .addValue("estado", turno.estado().name())
                        .addValue("origen", turno.origen().name())
                        .addValue("sobreturno", turno.sobreturno())
                        .addValue("reprogramadoDe", turno.reprogramadoDe())
                        .addValue("token", turno.tokenPublico())
                        .addValue("canceladoPor", turno.canceladoPor() == null ? null : turno.canceladoPor().name())
                        .addValue("motivo", turno.motivoCancelacion())
                        .addValue("creado", Timestamp.from(turno.creadoAt()))
                        .addValue("actualizado", Timestamp.from(turno.actualizadoAt())));
    }

    /** Bloqueo optimista (D5): solo actualiza si nadie cambio la version desde que se leyo. */
    @Override
    public boolean actualizarSiNoCambio(Turno turno) {
        return jdbc().update("update turno set estado = :estado, cancelado_por = :canceladoPor, "
                        + "motivo_cancelacion = :motivo, actualizado_at = :actualizado, version = version + 1 "
                        + "where id = :id and version = :version",
                new MapSqlParameterSource()
                        .addValue("id", turno.id())
                        .addValue("version", turno.version())
                        .addValue("estado", turno.estado().name())
                        .addValue("canceladoPor", turno.canceladoPor() == null ? null : turno.canceladoPor().name())
                        .addValue("motivo", turno.motivoCancelacion())
                        .addValue("actualizado", Timestamp.from(turno.actualizadoAt())))
                == 1;
    }

    @Override
    public Optional<Turno> buscar(UUID id) {
        return jdbc().query("select " + COLUMNAS + " from turno where id = :id", new MapSqlParameterSource("id", id),
                        RepositorioTurnosJdbc::turno)
                .stream()
                .findFirst();
    }

    /**
     * Lectura del listado con JOIN a paciente y prestacion (D8): evita una consulta por turno. Es solo lectura;
     * las escrituras pasan siempre por las APIs de cada modulo.
     */
    @Override
    public List<TurnoListado> listar(Instant desde, Instant hasta) {
        return jdbc().query("select t.id, t.inicio, t.fin, t.estado, t.origen, t.sobreturno, t.version, "
                        + "p.id as paciente_id, p.nombre as paciente_nombre, p.apellido as paciente_apellido, "
                        + "pr.id as prestacion_id, pr.nombre as prestacion_nombre "
                        + "from turno t join paciente p on p.id = t.paciente_id "
                        + "join prestacion pr on pr.id = t.prestacion_id "
                        + "where t.inicio >= :desde and t.inicio < :hasta order by t.inicio, t.id",
                new MapSqlParameterSource()
                        .addValue("desde", Timestamp.from(desde))
                        .addValue("hasta", Timestamp.from(hasta)),
                (fila, numero) -> new TurnoListado(
                        fila.getObject("id", UUID.class),
                        fila.getTimestamp("inicio").toInstant(),
                        fila.getTimestamp("fin").toInstant(),
                        fila.getString("estado"),
                        fila.getString("origen"),
                        fila.getBoolean("sobreturno"),
                        fila.getInt("version"),
                        fila.getObject("paciente_id", UUID.class),
                        fila.getString("paciente_nombre"),
                        fila.getString("paciente_apellido"),
                        fila.getObject("prestacion_id", UUID.class),
                        fila.getString("prestacion_nombre")));
    }

    private static Turno turno(ResultSet fila, int numero) throws SQLException {
        String canceladoPor = fila.getString("cancelado_por");
        return new Turno(
                fila.getObject("id", UUID.class),
                fila.getObject("paciente_id", UUID.class),
                fila.getObject("prestacion_id", UUID.class),
                fila.getTimestamp("inicio").toInstant(),
                fila.getTimestamp("fin").toInstant(),
                EstadoTurno.valueOf(fila.getString("estado")),
                OrigenTurno.valueOf(fila.getString("origen")),
                fila.getBoolean("sobreturno"),
                fila.getObject("reprogramado_de", UUID.class),
                fila.getString("token_publico"),
                canceladoPor == null ? null : CanceladoPor.valueOf(canceladoPor),
                fila.getString("motivo_cancelacion"),
                fila.getTimestamp("creado_at").toInstant(),
                fila.getTimestamp("actualizado_at").toInstant(),
                fila.getInt("version"));
    }
}
