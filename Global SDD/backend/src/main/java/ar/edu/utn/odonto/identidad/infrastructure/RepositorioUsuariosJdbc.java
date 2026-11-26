package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.RepositorioUsuarios;
import ar.edu.utn.odonto.identidad.domain.Rol;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC de {@code usuario}; sin entidad JPA, igual que auditoria (C-02). */
@Repository
public class RepositorioUsuariosJdbc implements RepositorioUsuarios {

    private static final String COLUMNAS = "id, email, password_hash, nombre, rol, activo, debe_cambiar_password, "
            + "intentos_fallidos, primer_fallo_at, bloqueado_hasta, ultimo_login_at, creado_at";

    private static final String INSERT = "insert into usuario (" + COLUMNAS + ") values (:id, :email, :passwordHash, "
            + ":nombre, :rol, :activo, :debeCambiarPassword, :intentosFallidos, :primerFalloAt, :bloqueadoHasta, "
            + ":ultimoLoginAt, :creadoAt)";

    private static final String UPDATE = "update usuario set password_hash = :passwordHash, "
            + "debe_cambiar_password = :debeCambiarPassword, intentos_fallidos = :intentosFallidos, "
            + "primer_fallo_at = :primerFalloAt, bloqueado_hasta = :bloqueadoHasta, ultimo_login_at = :ultimoLoginAt "
            + "where id = :id";

    private static final RowMapper<Usuario> MAPEO = RepositorioUsuariosJdbc::mapear;

    /** Perezoso, como en auditoria: el contexto arranca aunque no haya base configurada todavia. */
    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider;

    public RepositorioUsuariosJdbc(ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return jdbcProvider.getObject();
    }

    @Override
    public Optional<Usuario> buscarPorEmailParaActualizar(String email) {
        // email es CITEXT: el cast hace que la comparacion no distinga mayusculas (con texto plano si distingue).
        return jdbc().query("select " + COLUMNAS + " from usuario where email = cast(:email as citext) for update",
                        new MapSqlParameterSource("email", email.trim()), MAPEO)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return jdbc().query("select " + COLUMNAS + " from usuario where id = :id",
                        new MapSqlParameterSource("id", id), MAPEO)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorIdParaActualizar(UUID id) {
        return jdbc().query("select " + COLUMNAS + " from usuario where id = :id for update",
                        new MapSqlParameterSource("id", id), MAPEO)
                .stream()
                .findFirst();
    }

    @Override
    public boolean existeOdontologo() {
        Boolean existe = jdbc().queryForObject(
                "select exists (select 1 from usuario where rol = 'ODONTOLOGO')",
                new MapSqlParameterSource(),
                Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    @Override
    public void insertar(Usuario usuario) {
        jdbc().update(INSERT, parametros(usuario)
                .addValue("email", usuario.email())
                .addValue("nombre", usuario.nombre())
                .addValue("rol", usuario.rol().name())
                .addValue("activo", usuario.activo())
                .addValue("creadoAt", Timestamp.from(usuario.creadoAt())));
    }

    @Override
    public void actualizar(Usuario usuario) {
        jdbc().update(UPDATE, parametros(usuario));
    }

    private static MapSqlParameterSource parametros(Usuario usuario) {
        return new MapSqlParameterSource()
                .addValue("id", usuario.id())
                .addValue("passwordHash", usuario.passwordHash())
                .addValue("debeCambiarPassword", usuario.debeCambiarPassword())
                .addValue("intentosFallidos", usuario.intentosFallidos())
                .addValue("primerFalloAt", timestamp(usuario.primerFalloAt()))
                .addValue("bloqueadoHasta", timestamp(usuario.bloqueadoHasta()))
                .addValue("ultimoLoginAt", timestamp(usuario.ultimoLoginAt()));
    }

    private static Usuario mapear(ResultSet fila, int numero) throws SQLException {
        return new Usuario(
                fila.getObject("id", UUID.class),
                fila.getString("email"),
                fila.getString("password_hash"),
                fila.getString("nombre"),
                Rol.valueOf(fila.getString("rol")),
                fila.getBoolean("activo"),
                fila.getBoolean("debe_cambiar_password"),
                fila.getInt("intentos_fallidos"),
                instante(fila.getTimestamp("primer_fallo_at")),
                instante(fila.getTimestamp("bloqueado_hasta")),
                instante(fila.getTimestamp("ultimo_login_at")),
                instante(fila.getTimestamp("creado_at")));
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }

    private static Instant instante(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
