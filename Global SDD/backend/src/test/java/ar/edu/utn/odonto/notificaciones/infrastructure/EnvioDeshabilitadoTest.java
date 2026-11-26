package ar.edu.utn.odonto.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ar.edu.utn.odonto.notificaciones.application.MensajeriaPort;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Sin SMTP_HOST la app arranca y el envio queda deshabilitado, sin datos en el log (C-04, D2). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EnvioDeshabilitadoTest {

    @Autowired
    MensajeriaPort mensajeria;

    @Test
    void sinSmtpHostSeUsaElAdaptadorDeshabilitadoYNoFalla() {
        Logger logger = (Logger) LoggerFactory.getLogger(EmailDeshabilitadoAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(mensajeria).isInstanceOf(EmailDeshabilitadoAdapter.class);

            mensajeria.enviarEmail(new MensajeEmail("RECUPERACION_PASSWORD", "ana@consultorio.test", "Asunto secreto",
                    "texto con token=abc", "<p>html</p>"));

            assertThat(appender.list).singleElement().satisfies(evento -> assertThat(evento.getFormattedMessage())
                    .contains("deshabilitado")
                    .contains("RECUPERACION_PASSWORD")
                    .doesNotContain("ana@consultorio.test")
                    .doesNotContain("Asunto secreto")
                    .doesNotContain("token=abc"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
