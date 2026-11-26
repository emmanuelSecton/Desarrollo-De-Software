package ar.edu.utn.odonto.pacientes.infrastructure;

import ar.edu.utn.odonto.pacientes.application.CriterioDeBusqueda;
import ar.edu.utn.odonto.pacientes.application.Pagina;
import ar.edu.utn.odonto.pacientes.application.RepositorioPacientes;
import ar.edu.utn.odonto.pacientes.domain.Paciente;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Persistencia JDBC de {@code paciente} (D2), con la busqueda de D5 y el alta idempotente de D6. */
@Repository
public class RepositorioPacientesJdbc implements RepositorioPacientes {

    private static final String COLUMNAS = "id, dni, nombre, apellido, fecha_nacimiento, telefono_e164, email, "
            + "consentimiento_datos_at, politica_privacidad_version, acepta_whatsapp, whatsapp_consentimiento_at, "
            + "ausencias_count, activo, creado_at, actualizado_at";

    private static final String INSERT_SI_NO_EXISTE = "insert into paciente (" + COLUMNAS + ") values (:id, :dni, "
            + ":nombre, :apellido, :fechaNacimiento, :telefono, cast(:email as citext), :consentimientoDatosAt, "
            + ":politicaVersion, :aceptaWhatsapp, :whatsappAt, :ausencias, :activo, :creadoAt, :actualizadoAt) "
            + "on conflict (dni) do nothing";

    private static final String UPDATE = "update paciente set dni = :dni, nombre = :nombre, apellido = :apellido, "
            + "fecha_nacimiento = :fechaNacimiento, telefono_e164 = :telefono, email = cast(:email as citext), "
            + "acepta_whatsapp = :aceptaWhatsapp, whatsapp_consentimiento_at = :whatsappAt, activo = :activo, "
            + "actualizado_at = :actualizadoAt where id = :id";

    /** Texto comparable sin mayusculas ni acentos. */
    private static final String TEXTO_NORMALIZADO = "unaccent(lower(nombre || ' ' || apellido))";

    private static final RowMapper<Paciente> MAPEO = RepositorioPacientesJdbc::mapear;

    private final ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider;

    public RepositorioPacientesJdbc(ObjectProvider<NamedParameterJdbcTemplate> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    private NamedParameterJdbcTemplate jdbc() {
        return jdbcProvider.getObject();
    }

    @Override
    public boolean insertarSiNoExiste(Paciente paciente) {
        return jdbc().update(INSERT_SI_NO_EXISTE, parametros(paciente)
                        .addValue("consentimientoDatosAt", Timestamp.from(paciente.consentimientoDatosAt()))
                        .addValue("politicaVersion", paciente.politicaPrivacidadVersion())
                        .addValue("ausencias", paciente.ausenciasCount())
                        .addValue("creadoAt", Timestamp.from(paciente.creadoAt())))
                == 1;
    }

    @Override
    public void actualizar(Paciente paciente) {
        jdbc().update(UPDATE, parametros(paciente));
    }

    @Override
    public Optional<Paciente> buscarPorId(UUID id) {
        return uno("select " + COLUMNAS + " from paciente where id = :id", new MapSqlParameterSource("id", id));
    }

    @Override
    public Optional<Paciente> buscarPorIdParaActualizar(UUID id) {
        return uno("select " + COLUMNAS + " from paciente where id = :id for update",
                new MapSqlParameterSource("id", id));
    }

    @Override
    public Optional<Paciente> buscarPorDni(String dni) {
        return uno("select " + COLUMNAS + " from paciente where dni = :dni", new MapSqlParameterSource("dni", dni));
    }

    @Override
    public Pagina<Paciente> buscar(CriterioDeBusqueda criterio, boolean incluirInactivos, int pagina, int tamano) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("incluirInactivos", incluirInactivos)
                .addValue("limite", tamano)
                .addValue("desde", (long) pagina * tamano);
        String filtro = "(activo or :incluirInactivos)" + condicion(criterio, parametros);

        Long total = jdbc().queryForObject("select count(*) from paciente where " + filtro, parametros, Long.class);
        List<Paciente> contenido = jdbc().query("select " + COLUMNAS + " from paciente where " + filtro
                + " order by apellido, nombre, id limit :limite offset :desde", parametros, MAPEO);
        return new Pagina<>(contenido, pagina, tamano, total == null ? 0 : total);
    }

    private static String condicion(CriterioDeBusqueda criterio, MapSqlParameterSource parametros) {
        return switch (criterio) {
            case CriterioDeBusqueda.Todos todos -> "";
            case CriterioDeBusqueda.DniOTelefono dni -> {
                parametros.addValue("digitos", dni.digitos());
                yield " and (dni = :digitos or telefono_e164 like '%' || :digitos)";
            }
            case CriterioDeBusqueda.Telefono telefono -> {
                parametros.addValue("telefono", telefono.e164());
                yield " and telefono_e164 = :telefono";
            }
            case CriterioDeBusqueda.Texto texto -> {
                StringBuilder sql = new StringBuilder();
                for (int i = 0; i < texto.palabras().size(); i++) {
                    parametros.addValue("palabra" + i, "%" + escaparLike(texto.palabras().get(i)) + "%");
                    sql.append(" and ").append(TEXTO_NORMALIZADO)
                            .append(" like unaccent(lower(:palabra").append(i).append(")) escape '\\'");
                }
                yield sql.toString();
            }
        };
    }

    /** Los comodines que escribe el usuario se buscan literalmente. */
    static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Optional<Paciente> uno(String sql, MapSqlParameterSource parametros) {
        return jdbc().query(sql, parametros, MAPEO).stream().findFirst();
    }

    private static MapSqlParameterSource parametros(Paciente paciente) {
        Paciente.DatosPersonales datos = paciente.datos();
        return new MapSqlParameterSource()
                .addValue("id", paciente.id())
                .addValue("dni", datos.dni())
                .addValue("nombre", datos.nombre())
                .addValue("apellido", datos.apellido())
                .addValue("fechaNacimiento", datos.fechaNacimiento() == null ? null : Date.valueOf(datos.fechaNacimiento()))
                .addValue("telefono", datos.telefonoE164())
                .addValue("email", datos.email())
                .addValue("aceptaWhatsapp", datos.aceptaWhatsapp())
                .addValue("whatsappAt", timestamp(paciente.whatsappConsentimientoAt()))
                .addValue("activo", paciente.activo())
                .addValue("actualizadoAt", Timestamp.from(paciente.actualizadoAt()));
    }

    private static Paciente mapear(ResultSet fila, int numero) throws SQLException {
        Date nacimiento = fila.getDate("fecha_nacimiento");
        return new Paciente(
                fila.getObject("id", UUID.class),
                new Paciente.DatosPersonales(
                        fila.getString("dni"),
                        fila.getString("nombre"),
                        fila.getString("apellido"),
                        nacimiento == null ? null : nacimiento.toLocalDate(),
                        fila.getString("telefono_e164"),
                        fila.getString("email"),
                        fila.getBoolean("acepta_whatsapp")),
                instante(fila.getTimestamp("consentimiento_datos_at")),
                fila.getString("politica_privacidad_version"),
                instante(fila.getTimestamp("whatsapp_consentimiento_at")),
                fila.getInt("ausencias_count"),
                fila.getBoolean("activo"),
                instante(fila.getTimestamp("creado_at")),
                instante(fila.getTimestamp("actualizado_at")));
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }

    private static Instant instante(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
