package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Timestamp;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** El trigger append-only se prueba con PostgreSQL real, nunca H2 ni mocks (C-02, D1). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class TriggerAppendOnlyTest {

    @Autowired
    DataSource dataSource;

    @Test
    void unUpdateEsRechazadoYLaFilaQuedaIdentica() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long id = insertar(jdbc);
        String accionOriginal = accion(jdbc, id);
        Timestamp ocurridoOriginal = ocurrido(jdbc, id);

        assertRechazado(() -> jdbc.update(
                "update evento_auditoria set accion = 'EXPORTAR' where id = ?", id));

        assertThat(accion(jdbc, id)).isEqualTo(accionOriginal);
        assertThat(ocurrido(jdbc, id)).isEqualTo(ocurridoOriginal);
    }

    @Test
    void unDeleteEsRechazadoYLaFilaSiguePresente() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long id = insertar(jdbc);

        assertRechazado(() -> jdbc.update("delete from evento_auditoria where id = ?", id));

        assertThat(jdbc.queryForObject(
                        "select count(*) from evento_auditoria where id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void insertarEventosSigueSiendoPermitido() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        Long id = insertar(jdbc);

        assertThat(id).isPositive();
        assertThat(accion(jdbc, id)).isEqualTo("LOGIN");
    }

    private static void assertRechazado(Runnable sentencia) {
        assertThatThrownBy(sentencia::run)
                .isInstanceOf(DataAccessException.class)
                .satisfies(t -> assertThat(t.getCause().getMessage()).contains("append-only"));
    }

    private static Long insertar(JdbcTemplate jdbc) {
        jdbc.update("insert into evento_auditoria "
                        + "(ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle, ip) "
                        + "values (?, 'USUARIO', 'u-1', 'LOGIN', 'USUARIO', 'u-1', '{}'::jsonb, '200.24.18.7')",
                Timestamp.from(Instant.parse("2026-11-24T13:00:00Z")));
        return jdbc.queryForObject("select max(id) from evento_auditoria", Long.class);
    }

    private static String accion(JdbcTemplate jdbc, Long id) {
        return jdbc.queryForObject("select accion from evento_auditoria where id = ?", String.class, id);
    }

    private static Timestamp ocurrido(JdbcTemplate jdbc, Long id) {
        return jdbc.queryForObject("select ocurrido_at from evento_auditoria where id = ?", Timestamp.class, id);
    }
}
