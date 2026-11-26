package ar.edu.utn.odonto.identidad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tabla usuario de V3 (C-03, D1) contra PostgreSQL 16 real. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaUsuarioTest {

    @Autowired
    DataSource dataSource;

    @Test
    void lasColumnasTienenLosTiposEsperados() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, String> tipos = jdbc.queryForList(
                        "select column_name, udt_name from information_schema.columns where table_name = 'usuario'")
                .stream()
                .collect(Collectors.toMap(f -> (String) f.get("column_name"), f -> (String) f.get("udt_name")));

        assertThat(tipos)
                .containsEntry("id", "uuid")
                .containsEntry("email", "citext")
                .containsEntry("password_hash", "varchar")
                .containsEntry("rol", "varchar")
                .containsEntry("activo", "bool")
                .containsEntry("debe_cambiar_password", "bool")
                .containsEntry("intentos_fallidos", "int4")
                .containsEntry("primer_fallo_at", "timestamptz")
                .containsEntry("bloqueado_hasta", "timestamptz")
                .containsEntry("ultimo_login_at", "timestamptz")
                .containsEntry("creado_at", "timestamptz");
    }

    @Test
    void creadoAtNoTieneDefaultDeReloj() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String porDefecto = jdbc.queryForObject("select column_default from information_schema.columns "
                + "where table_name = 'usuario' and column_name = 'creado_at'", String.class);

        assertThat(porDefecto).isNull();
    }

    @Test
    void existeElIndiceParcialDelOdontologoActivo() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String definicion = jdbc.queryForObject(
                "select indexdef from pg_indexes where indexname = 'uq_usuario_odontologo_activo'", String.class);

        assertThat(definicion).contains("UNIQUE").contains("ODONTOLOGO").contains("activo");
    }

    @Test
    void unEmailRepetidoConOtraCapitalizacionSeRechaza() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UsuariosDePrueba.Creado existente = UsuariosDePrueba.secretaria(jdbc);

        assertThatThrownBy(() -> insertar(jdbc, existente.email().toUpperCase(), "SECRETARIA", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unSegundoOdontologoActivoSeRechaza() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        // El alta inicial ya creo el odontologo activo de la instancia.
        assertThat(jdbc.queryForObject(
                        "select count(*) from usuario where rol = 'ODONTOLOGO' and activo", Integer.class))
                .isEqualTo(1);

        assertThatThrownBy(() -> insertar(jdbc, UsuariosDePrueba.emailUnico("otro"), "ODONTOLOGO", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void activarUnOdontologoInactivoTambienSeRechaza() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UsuariosDePrueba.Creado inactivo = UsuariosDePrueba.odontologoInactivo(jdbc);

        assertThatThrownBy(() -> jdbc.update("update usuario set activo = true where id = ?", inactivo.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sePermitenOdontologosInactivosYVariasSecretarias() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        UsuariosDePrueba.odontologoInactivo(jdbc);
        UsuariosDePrueba.odontologoInactivo(jdbc);
        UsuariosDePrueba.secretaria(jdbc);
        UsuariosDePrueba.secretaria(jdbc);
    }

    @Test
    void unRolFueraDelCatalogoSeRechaza() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> insertar(jdbc, UsuariosDePrueba.emailUnico("paciente"), "PACIENTE", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elRolRuntimeLeeInsertaYActualizaPeroNoBorra() {
        JdbcTemplate runtime = new JdbcTemplate(ConexionRuntimeTest.dataSourceRuntime());
        UUID id = UUID.randomUUID();

        runtime.update("insert into usuario (id, email, password_hash, nombre, rol, creado_at) "
                        + "values (?, ?, 'hash', 'Runtime', 'SECRETARIA', ?)",
                id, UsuariosDePrueba.emailUnico("runtime"), Timestamp.from(Instant.parse("2026-11-25T12:00:00Z")));
        assertThat(runtime.update("update usuario set intentos_fallidos = 1 where id = ?", id)).isEqualTo(1);
        assertThat(runtime.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class, id))
                .isEqualTo(1);

        assertThatThrownBy(() -> runtime.update("delete from usuario where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause().getMessage())
                        .contains("permission denied"));
    }

    private static void insertar(JdbcTemplate jdbc, String email, String rol, boolean activo) {
        jdbc.update("insert into usuario (email, password_hash, nombre, rol, activo, creado_at) "
                        + "values (?, 'hash', 'Prueba', ?, ?, ?)",
                email, rol, activo, Timestamp.from(Instant.parse("2026-11-25T12:00:00Z")));
    }
}
