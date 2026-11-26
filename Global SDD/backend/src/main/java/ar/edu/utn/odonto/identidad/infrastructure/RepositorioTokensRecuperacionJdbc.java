package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.RepositorioTokensRecuperacion;
import ar.edu.utn.odonto.identidad.domain.TokenDeRecuperacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC de {@code token_recuperacion} (C-04, D3). */
@Repository
public class RepositorioTokensRecuperacionJdbc implements RepositorioTokensRecuperacion {

    private static final String COLUMNAS = "id, usuario_id, token_hash, creado_at, vence_at, usado_at";

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider;

    public RepositorioTokensRecuperacionJdbc(ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return jdbcProvider.getObject();
    }

    @Override
    public void insertar(TokenDeRecuperacion token) {
        jdbc().update("insert into token_recuperacion (" + COLUMNAS + ") values (:id, :usuarioId, :hash, "
                        + ":creadoAt, :venceAt, :usadoAt)",
                new MapSqlParameterSource()
                        .addValue("id", token.id())
                        .addValue("usuarioId", token.usuarioId())
                        .addValue("hash", token.hash())
                        .addValue("creadoAt", Timestamp.from(token.creadoAt()))
                        .addValue("venceAt", Timestamp.from(token.venceAt()))
                        .addValue("usadoAt", timestamp(token.usadoAt())));
    }

    @Override
    public Optional<TokenDeRecuperacion> buscarPorHashParaActualizar(String hash) {
        return jdbc().query("select " + COLUMNAS + " from token_recuperacion where token_hash = :hash for update",
                        new MapSqlParameterSource("hash", hash), RepositorioTokensRecuperacionJdbc::mapear)
                .stream()
                .findFirst();
    }

    @Override
    public void invalidarVigentes(UUID usuarioId, Instant ahora) {
        jdbc().update("update token_recuperacion set usado_at = :ahora where usuario_id = :usuarioId "
                        + "and usado_at is null",
                new MapSqlParameterSource().addValue("usuarioId", usuarioId).addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public void marcarUsado(TokenDeRecuperacion token) {
        jdbc().update("update token_recuperacion set usado_at = :usadoAt where id = :id",
                new MapSqlParameterSource().addValue("id", token.id()).addValue("usadoAt", timestamp(token.usadoAt())));
    }

    private static TokenDeRecuperacion mapear(ResultSet fila, int numero) throws SQLException {
        return new TokenDeRecuperacion(
                fila.getObject("id", UUID.class),
                fila.getObject("usuario_id", UUID.class),
                fila.getString("token_hash"),
                fila.getTimestamp("creado_at").toInstant(),
                fila.getTimestamp("vence_at").toInstant(),
                fila.getTimestamp("usado_at") == null ? null : fila.getTimestamp("usado_at").toInstant());
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }
}
