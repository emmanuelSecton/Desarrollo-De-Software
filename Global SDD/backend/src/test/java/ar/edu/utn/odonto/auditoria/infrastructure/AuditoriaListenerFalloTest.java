package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.AuditoriaInvalidaException;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Fallo en BEFORE_COMMIT: log técnico con MDC y rollback de la operación (RN-GL-01). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AuditoriaListenerFalloTest {

    @Autowired
    ApplicationEventPublisher editor;

    @Autowired
    PlatformTransactionManager transacciones;

    @Autowired
    DataSource dataSource;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void adjuntarAppender() {
        appender = new ListAppender<>();
        appender.start();
        logger = (Logger) LoggerFactory.getLogger(AuditoriaListener.class);
        logger.addAppender(appender);
    }

    @AfterEach
    void despegarAppender() {
        logger.detachAppender(appender);
        MDC.clear();
    }

    @Test
    void unDetalleInvalidoSeLogueaYRevierteLaOperacion() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        AtomicBoolean revertida = new AtomicBoolean();

        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    revertida.set(status == TransactionSynchronization.STATUS_ROLLED_BACK);
                }
            });
            jdbc.update("insert into evento_auditoria "
                            + "(ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle) "
                            + "values (?, 'SISTEMA', null, 'LOGIN', 'SISTEMA', 't-200-marker', '{}'::jsonb)",
                    Timestamp.from(Instant.parse("2026-11-24T13:00:00Z")));
            editor.publishEvent(evento(Map.of("diagnostico", "Neumonía bilateral")));
        })).isInstanceOf(AuditoriaInvalidaException.class);

        assertThat(revertida).isTrue();
        assertThat(conteo(jdbc, "t-200-marker")).isZero();
        assertThat(appender.list).anySatisfy(evento -> {
            assertThat(evento.getFormattedMessage()).contains("auditoría");
            assertThat(evento.getMDCPropertyMap())
                    .containsEntry("auditoria.accion", "CREAR_TURNO")
                    .containsEntry("auditoria.entidad", "TURNO")
                    .containsEntry("auditoria.motivo", "AuditoriaInvalidaException")
                    .containsKey("auditoria.correlacion")
                    .doesNotContainKey("entidad_id")
                    .doesNotContainKey("accion")
                    .doesNotContainKey("entidad")
                    .doesNotContainKey("motivo");
            assertThat(evento.getMDCPropertyMap().get("auditoria.correlacion")).isNotBlank();
            assertThat(evento.getMDCPropertyMap().values())
                    .allSatisfy(valor -> assertThat(valor).doesNotContain("Neumonía", "bilateral", "t-200", "u-1"));
        });
        assertThat(mensajes()).doesNotContain("Neumonía", "bilateral", "t-200", "u-1");
    }

    @Test
    void unaIpInvalidaSeLogueaSinExponerElValorRechazado() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                editor.publishEvent(new EventoAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1",
                        "TURNO", "t-201", Map.of("turno_id", "t-201"), "consultorio.ejemplo.com"))))
                .isInstanceOf(AuditoriaInvalidaException.class);

        assertThat(conteo(jdbc, "t-201")).isZero();
        assertThat(appender.list).anySatisfy(evento -> assertThat(evento.getMDCPropertyMap())
                .containsKey("auditoria.correlacion")
                .doesNotContainKey("entidad_id")
                .allSatisfy((clave, valor) -> assertThat(valor).doesNotContain("consultorio.ejemplo.com", "t-201")));
        assertThat(mensajes()).doesNotContain("consultorio.ejemplo.com", "t-201");
    }

    @Test
    void elMdcSeLimpiaSoloParaLasClavesDelListener() {
        MDC.put("request_id", "r-456");
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                    editor.publishEvent(evento(Map.of("telefono", "555-1234")))))
                    .isInstanceOf(AuditoriaInvalidaException.class);

            assertThat(MDC.get("request_id")).isEqualTo("r-456");
            assertThat(MDC.get("auditoria.accion")).isNull();
            assertThat(MDC.get("auditoria.entidad")).isNull();
            assertThat(MDC.get("auditoria.motivo")).isNull();
            assertThat(MDC.get("auditoria.correlacion")).isNull();
            assertThat(mensajes()).doesNotContain("555-1234", "t-200");
        } finally {
            MDC.remove("request_id");
        }
    }

    @Test
    void fallosDistintosGeneranCorrelationIdsDistintos() {
        for (String id : List.of("t-205", "t-206")) {
            assertThatThrownBy(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                    editor.publishEvent(new EventoAuditoria(AccionAuditoria.EXPORTAR, TipoActor.USUARIO, "u-9",
                            "CAJA", id, Map.of("diagnostico", "x"), null))))
                    .isInstanceOf(AuditoriaInvalidaException.class);
        }

        List<String> correlaciones = appender.list.stream()
                .map(evento -> evento.getMDCPropertyMap().get("auditoria.correlacion"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        assertThat(correlaciones).hasSize(2);
        assertThat(appender.list).allSatisfy(evento ->
                assertThat(evento.getMDCPropertyMap()).doesNotContainKey("entidad_id"));
        assertThat(mensajes()).doesNotContain("t-205", "t-206", "diagnostico");
    }

    private static EventoAuditoria evento(Map<String, Object> detalle) {
        return new EventoAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1", "TURNO", "t-200",
                detalle, null);
    }

    private static int conteo(JdbcTemplate jdbc, String id) {
        return jdbc.queryForObject("select count(*) from evento_auditoria where entidad_id = ?", Integer.class, id);
    }

    private String mensajes() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining(" "));
    }
}
