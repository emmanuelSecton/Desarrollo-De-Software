package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Fallo después del commit: log técnico con MDC, sin PII y sin revertir la operación (D6). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AuditoriaListenerFalloTest {

    @Autowired
    ApplicationEventPublisher editor;

    @Autowired
    PlatformTransactionManager transacciones;

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
    void unDetalleInvalidoSeLogueaConMetadatosYNoRevierteLaOperacion() {
        AtomicBoolean operacionConfirmada = new AtomicBoolean();

        assertThatCode(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado -> {
            editor.publishEvent(evento(Map.of("diagnostico", "Neumonía bilateral")));
            operacionConfirmada.set(true);
        })).doesNotThrowAnyException();

        assertThat(operacionConfirmada).isTrue();
        assertThat(appender.list).anySatisfy(evento -> {
            assertThat(evento.getFormattedMessage()).contains("auditoría");
            assertThat(evento.getMDCPropertyMap())
                    .containsEntry("accion", "CREAR_TURNO")
                    .containsEntry("entidad", "TURNO")
                    .containsEntry("entidad_id", "t-200")
                    .containsEntry("motivo", "AuditoriaInvalidaException");
        });
        assertThat(mensajes()).doesNotContain("Neumonía", "bilateral");
    }

    @Test
    void unaIpInvalidaSeLogueaSinExponerElValorRechazado() {
        assertThatCode(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                editor.publishEvent(new EventoAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1",
                        "TURNO", "t-201", Map.of("turno_id", "t-201"), "consultorio.ejemplo.com"))))
                .doesNotThrowAnyException();

        assertThat(appender.list).anySatisfy(evento ->
                assertThat(evento.getMDCPropertyMap()).containsEntry("entidad_id", "t-201"));
        assertThat(mensajes()).doesNotContain("consultorio.ejemplo.com");
    }

    @Test
    void elMdcSeLimpiaDespuesDelFallo() {
        assertThatCode(() -> new TransactionTemplate(transacciones).executeWithoutResult(estado ->
                editor.publishEvent(evento(Map.of("telefono", "555-1234")))))
                .doesNotThrowAnyException();

        assertThat(MDC.get("accion")).isNull();
        assertThat(MDC.get("entidad_id")).isNull();
        assertThat(mensajes()).doesNotContain("555-1234");
    }

    private static EventoAuditoria evento(Map<String, Object> detalle) {
        return new EventoAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1", "TURNO", "t-200",
                detalle, null);
    }

    private String mensajes() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining(" "));
    }
}
