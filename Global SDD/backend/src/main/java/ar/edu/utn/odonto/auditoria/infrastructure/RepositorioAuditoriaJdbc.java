package ar.edu.utn.odonto.auditoria.infrastructure;

import ar.edu.utn.odonto.auditoria.application.AuditoriaInvalidaException;
import ar.edu.utn.odonto.auditoria.application.EventoAuditoriaPort;
import ar.edu.utn.odonto.auditoria.application.RegistroAuditoria;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC con tipos PostgreSQL; sin entidad JPA (D3). */
@Repository
public class RepositorioAuditoriaJdbc implements EventoAuditoriaPort {

    private static final String INSERT =
            "insert into evento_auditoria "
                    + "(ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle, ip) "
                    + "values (:ocurridoAt, :actorTipo, :actorId, :accion, :entidad, :entidadId, :detalle, :ip)";

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbc;
    private final Clock clock;
    private final ObjectMapper mapper;

    public RepositorioAuditoriaJdbc(
            ObjectProvider<NamedParameterJdbcTemplate> jdbc, Clock clock, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public void guardar(RegistroAuditoria registro) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("ocurridoAt", Timestamp.from(clock.instant()))
                .addValue("actorTipo", registro.tipoActor().valor())
                .addValue("actorId", registro.actorId())
                .addValue("accion", registro.accion().valor())
                .addValue("entidad", registro.entidad())
                .addValue("entidadId", registro.entidadId())
                .addValue("detalle", objetoJson(registro))
                .addValue("ip", objetoInet(registro.ip()));
        jdbc.getObject().update(INSERT, parametros);
    }

    private PGobject objetoJson(RegistroAuditoria registro) {
        try {
            PGobject objeto = new PGobject();
            objeto.setType("jsonb");
            objeto.setValue(mapper.writeValueAsString(registro.detalle()));
            return objeto;
        } catch (JsonProcessingException | SQLException e) {
            throw new AuditoriaInvalidaException("detalle: no se pudo serializar");
        }
    }

    private static PGobject objetoInet(String ip) {
        if (ip == null) {
            return null;
        }
        try {
            PGobject objeto = new PGobject();
            objeto.setType("inet");
            objeto.setValue(ip);
            return objeto;
        } catch (SQLException e) {
            throw new AuditoriaInvalidaException("ip: no se pudo convertir");
        }
    }
}
