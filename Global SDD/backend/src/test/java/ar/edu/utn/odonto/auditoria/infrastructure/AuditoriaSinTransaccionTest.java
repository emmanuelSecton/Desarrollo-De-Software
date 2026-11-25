package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.util.LinkedHashMap;
import java.util.Map;
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

/** Lecturas sin transacción: RN-SEG-05 cubierto por fallbackExecution (D6). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AuditoriaSinTransaccionTest {

    @Autowired
    ApplicationEventPublisher editor;

    @Autowired
    PlatformTransactionManager transacciones;

    @Autowired
    DataSource dataSource;

    @Test
    void unaLecturaSinTransaccionSeRegistra() {
        editor.publishEvent(eventoDeLectura("h-300"));

        Map<String, Object> fila = fila("h-300");
        assertThat(fila).isNotNull();
        assertThat(fila.get("ACCION")).isEqualTo("VER_HISTORIA");
        assertThat(fila.get("DETALLE").toString()).contains("historia_id");
    }

    @Test
    void elRegistroEsEquivalenteAlDeUnaEscrituraTransaccional() {
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> editor.publishEvent(eventoDeLectura("h-301")));
        editor.publishEvent(eventoDeLectura("h-302"));

        Map<String, Object> transaccional = fila("h-301");
        Map<String, Object> sinTransaccion = fila("h-302");
        assertThat(sinTransaccion)
                .containsEntry("ACCION", transaccional.get("ACCION"))
                .containsEntry("ACTOR_TIPO", transaccional.get("ACTOR_TIPO"))
                .containsEntry("ENTIDAD", transaccional.get("ENTIDAD"))
                .containsEntry("DETALLE", transaccional.get("DETALLE"))
                .containsEntry("IP", transaccional.get("IP"));
    }

    @Test
    void unFalloEnLaRutaSinTransaccionNoSePropaga() {
        assertThatCode(() -> editor.publishEvent(new EventoAuditoria(AccionAuditoria.VER_HISTORIA,
                TipoActor.USUARIO, "u-1", "HISTORIA", "h-303", Map.of("diagnostico", "x"), null)))
                .doesNotThrowAnyException();

        assertThat(fila("h-303")).isNull();
    }

    private static EventoAuditoria eventoDeLectura(String id) {
        return new EventoAuditoria(AccionAuditoria.VER_HISTORIA, TipoActor.USUARIO, "u-1", "HISTORIA", id,
                Map.of("historia_id", "h-fijo"), "200.24.18.7");
    }

    private Map<String, Object> fila(String id) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        return jdbc.query(
                        "select accion, actor_tipo, entidad, detalle::text as detalle, host(ip)::text as ip "
                                + "from evento_auditoria where entidad_id = ?",
                        (rs, numero) -> {
                            Map<String, Object> valores = new LinkedHashMap<>();
                            valores.put("ACCION", rs.getString("accion"));
                            valores.put("ACTOR_TIPO", rs.getString("actor_tipo"));
                            valores.put("ENTIDAD", rs.getString("entidad"));
                            valores.put("DETALLE", rs.getString("detalle"));
                            valores.put("IP", rs.getString("ip"));
                            return valores;
                        },
                        id)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
