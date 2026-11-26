package ar.edu.utn.odonto.identidad.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tabla token_recuperacion de V5 (C-04, D3) contra PostgreSQL 16 real. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaTokenRecuperacionTest {

    private static final Timestamp CREADO = Timestamp.from(Instant.parse("2026-11-26T12:00:00Z"));
    private static final Timestamp VENCE = Timestamp.from(Instant.parse("2026-11-26T13:00:00Z"));

    @Autowired
    DataSource dataSource;

    private static String hashUnico() {
        return (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "").substring(0, 64);
    }

    private static UUID insertar(JdbcTemplate jdbc, UUID usuario, String hash, Timestamp creado, Timestamp vence) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into token_recuperacion (id, usuario_id, token_hash, creado_at, vence_at) "
                + "values (?, ?, ?, ?, ?)", id, usuario, hash, creado, vence);
        return id;
    }

    @Test
    void elHashEsUnicoYElVencimientoPosteriorALaCreacion() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID usuario = UsuariosDePrueba.secretaria(jdbc).id();
        String hash = hashUnico();
        insertar(jdbc, usuario, hash, CREADO, VENCE);

        assertThatThrownBy(() -> insertar(jdbc, usuario, hash, CREADO, VENCE))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, usuario, hashUnico(), VENCE, CREADO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elUsuarioDebeExistir() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> insertar(jdbc, UUID.randomUUID(), hashUnico(), CREADO, VENCE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasFechasNoTienenDefault() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(jdbc.queryForList("select column_default from information_schema.columns "
                        + "where table_name = 'token_recuperacion' and data_type like 'timestamp%'", String.class))
                .hasSize(3)
                .containsOnlyNulls();
    }

    @Test
    void elRolRuntimeLeeInsertaYActualizaPeroNoBorra() {
        JdbcTemplate admin = new JdbcTemplate(dataSource);
        JdbcTemplate runtime = new JdbcTemplate(ConexionRuntimeTest.dataSourceRuntime());
        UUID id = insertar(runtime, UsuariosDePrueba.secretaria(admin).id(), hashUnico(), CREADO, VENCE);

        assertThat(runtime.update("update token_recuperacion set usado_at = ? where id = ?", VENCE, id)).isEqualTo(1);
        assertThatThrownBy(() -> runtime.update("delete from token_recuperacion where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause().getMessage())
                        .contains("permission denied"));
    }
}
