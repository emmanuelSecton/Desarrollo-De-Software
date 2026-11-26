package ar.edu.utn.odonto.notificaciones.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Adaptador SMTP contra un servidor SMTP real en memoria (GreenMail): envio, reintentos y logs (C-04). */
class EmailSmtpAdapterTest {

    private static final String DESTINO = "usuario@consultorio.test";
    private static final String ENLACE = "https://consultorio.test/restablecer?token=secreto123";
    private static final List<Duration> ESPERAS = List.of(Duration.ofMillis(10), Duration.ofMillis(10));

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void capturarLogs() {
        logger = (Logger) LoggerFactory.getLogger(EmailSmtpAdapter.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void soltarLogs() {
        logger.detachAppender(appender);
    }

    private static JavaMailSenderImpl enviadorLocal() {
        JavaMailSenderImpl enviador = new JavaMailSenderImpl();
        enviador.setHost("localhost");
        enviador.setPort(ServerSetupTest.SMTP.getPort());
        enviador.setDefaultEncoding("UTF-8");
        return enviador;
    }

    private static MensajeEmail mensaje() {
        return new MensajeEmail("RECUPERACION_PASSWORD", DESTINO, "Restablecé tu contraseña",
                "Abrí " + ENLACE, "<p>Abrí <a href=\"" + ENLACE + "\">el enlace</a></p>");
    }

    private String logs() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.joining(" | "));
    }

    @Test
    void enviaUnEmailConRemitenteAsuntoTextoYHtml() throws Exception {
        new EmailSmtpAdapter(enviadorLocal(), "turnos@consultorio.test", ESPERAS).enviarEmail(mensaje());

        MimeMessage[] recibidos = greenMail.getReceivedMessages();
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos[0].getFrom()[0].toString()).isEqualTo("turnos@consultorio.test");
        assertThat(recibidos[0].getAllRecipients()[0].toString()).isEqualTo(DESTINO);
        assertThat(recibidos[0].getSubject()).isEqualTo("Restablecé tu contraseña");
        String cuerpo = GreenMailUtil.getBody(recibidos[0]);
        assertThat(cuerpo).contains("text/plain").contains("text/html");
        assertThat(appender.list).isEmpty();
    }

    @Test
    void unFalloTransitorioSeReintentaYNoDejaError() {
        AtomicInteger llamadas = new AtomicInteger();
        JavaMailSenderImpl falloUnaVez = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mensaje) {
                if (llamadas.incrementAndGet() == 1) {
                    throw new MailSendException("falla simulada para " + DESTINO);
                }
                super.send(mensaje);
            }
        };
        falloUnaVez.setHost("localhost");
        falloUnaVez.setPort(ServerSetupTest.SMTP.getPort());

        new EmailSmtpAdapter(falloUnaVez, "turnos@consultorio.test", ESPERAS).enviarEmail(mensaje());

        assertThat(greenMail.getReceivedMessages()).hasSize(1);
        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN);
        assertThat(logs()).doesNotContain(DESTINO).doesNotContain("secreto123");
    }

    @Test
    void conElServidorCaidoHaceTresIntentosYUnSoloErrorSinDatos() {
        AtomicInteger llamadas = new AtomicInteger();
        JavaMailSenderImpl siempreFalla = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mensaje) {
                llamadas.incrementAndGet();
                throw new MailSendException("no se pudo conectar para " + DESTINO);
            }
        };

        new EmailSmtpAdapter(siempreFalla, "turnos@consultorio.test", ESPERAS).enviarEmail(mensaje());

        assertThat(llamadas).hasValue(3);
        assertThat(appender.list).filteredOn(e -> e.getLevel() == Level.ERROR).hasSize(1);
        assertThat(logs()).contains("RECUPERACION_PASSWORD").doesNotContain(DESTINO).doesNotContain("secreto123")
                .doesNotContain("Restablecé");
    }

    @Test
    void elMensajeNoImprimeSusDatos() {
        assertThat(mensaje().toString()).doesNotContain(DESTINO).doesNotContain("secreto123");
    }
}
