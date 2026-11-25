package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Publicación AFTER_COMMIT y transacciones independientes (C-02, D6). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AuditoriaListenerTest {

    @Autowired
    ApplicationEventPublisher editor;

    @Autowired
    PlatformTransactionManager transacciones;

    @Autowired
    DataSource dataSource;

    @Test
    void elEventoSePersisteSoloDespuesDelCommit() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        AtomicBoolean dentroDeLaTransaccion = new AtomicBoolean();

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            editor.publishEvent(evento("t-100", Map.of("turno_id", "t-100")));
            dentroDeLaTransaccion.set(conteo(jdbc, "t-100") > 0);
        });

        assertThat(dentroDeLaTransaccion.get()).isFalse();
        assertThat(conteo(jdbc, "t-100")).isEqualTo(1);
    }

    @Test
    void unRollbackDeLaOperacionNoDejaAuditoria() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            editor.publishEvent(evento("t-101", Map.of("turno_id", "t-101")));
            throw new IllegalStateException("rollback de negocio");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(conteo(jdbc, "t-101")).isZero();
    }

    @Test
    void dosEventosDelMismoCommitSeRegistran() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            editor.publishEvent(evento("t-102", Map.of("turno_id", "t-102")));
            editor.publishEvent(evento("t-103", Map.of("turno_id", "t-103")));
        });

        assertThat(conteo(jdbc, "t-102")).isEqualTo(1);
        assertThat(conteo(jdbc, "t-103")).isEqualTo(1);
    }

    private static EventoAuditoria evento(String id, Map<String, Object> detalle) {
        return new EventoAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1", "TURNO", id, detalle,
                "200.24.18.7");
    }

    private static int conteo(JdbcTemplate jdbc, String id) {
        return jdbc.queryForObject(
                "select count(*) from evento_auditoria where entidad_id = ?", Integer.class, id);
    }
}
