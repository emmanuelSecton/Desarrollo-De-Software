package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** CHECK condicional de actor: SISTEMA sin id, USUARIO/PACIENTE con id (C-02, D1). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class CheckActorTest {

    private static final Timestamp INSTANTE = Timestamp.from(Instant.parse("2026-11-24T13:00:00Z"));

    private static final String INSERT =
            "insert into evento_auditoria (ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle) "
                    + "values (?, ?, ?, 'LOGIN', 'USUARIO', 'u-1', '{}'::jsonb)";

    @Autowired
    DataSource dataSource;

    @Test
    void losTiposDeActorValidosSeAceptan() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long antes = total(jdbc);

        insertar(jdbc, "SISTEMA", null);
        insertar(jdbc, "USUARIO", "u-42");
        insertar(jdbc, "PACIENTE", "p-42");

        assertThat(total(jdbc)).isEqualTo(antes + 3);
        assertThat(actorId(jdbc, "SISTEMA")).isNull();
        assertThat(actorId(jdbc, "USUARIO")).isEqualTo("u-42");
        assertThat(actorId(jdbc, "PACIENTE")).isEqualTo("p-42");
    }

    @Test
    void sistemaConIdEsRechazado() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertRechazado(() -> insertar(jdbc, "SISTEMA", "u-1"));
    }

    @Test
    void usuarioYPacienteSinIdSonRechazados() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertRechazado(() -> insertar(jdbc, "USUARIO", null));
        assertRechazado(() -> insertar(jdbc, "PACIENTE", null));
    }

    private static void assertRechazado(Runnable sentencia) {
        assertThatThrownBy(sentencia::run)
                .isInstanceOf(DataAccessException.class)
                .satisfies(t -> assertThat(t.getCause().getMessage()).contains("ck_evento_auditoria_actor"));
    }

    private static void insertar(JdbcTemplate jdbc, String tipo, String id) {
        jdbc.update(INSERT, INSTANTE, tipo, id);
    }

    private static long total(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select count(*) from evento_auditoria", Long.class);
    }

    private static String actorId(JdbcTemplate jdbc, String tipo) {
        List<String> valores = jdbc.queryForList(
                "select actor_id from evento_auditoria where actor_tipo = ? order by id desc limit 1",
                String.class, tipo);
        return valores.isEmpty() ? null : valores.get(0);
    }
}
