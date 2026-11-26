package ar.edu.utn.odonto.notificaciones.infrastructure;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ar.edu.utn.odonto.notificaciones.application.MensajeriaPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envio de email por SMTP (C-04, D1, D2): cualquier proveedor transaccional sirve cambiando {@code SMTP_*}.
 * Hasta {@code 1 + esperas} intentos; nunca relanza ni loguea destinatario, asunto ni contenido.
 */
@Component
@ConditionalOnExpression("!'${spring.mail.host:}'.isBlank()")
public class EmailSmtpAdapter implements MensajeriaPort {

    private static final Logger LOG = LoggerFactory.getLogger(EmailSmtpAdapter.class);

    private final JavaMailSender enviador;
    private final String remitente;
    private final List<Duration> esperas;

    public EmailSmtpAdapter(
            JavaMailSender enviador,
            @Value("${notificaciones.email.remitente}") String remitente,
            @Value("${notificaciones.email.esperas-reintento:1s,3s}") List<Duration> esperas) {
        this.enviador = enviador;
        this.remitente = remitente;
        this.esperas = List.copyOf(esperas);
    }

    @Override
    public void enviarEmail(MensajeEmail mensaje) {
        int intentos = esperas.size() + 1;
        for (int intento = 1; intento <= intentos; intento++) {
            try {
                enviador.send(armar(mensaje));
                return;
            } catch (MailException | MessagingException fallo) {
                // Solo la clase: el mensaje de la excepcion puede incluir el destinatario.
                if (intento == intentos) {
                    LOG.error("No se pudo enviar el email de tipo {} tras {} intentos: {}",
                            mensaje.tipo(), intentos, fallo.getClass().getSimpleName());
                    return;
                }
                LOG.warn("Falló el intento {} de envío del email de tipo {}: {}",
                        intento, mensaje.tipo(), fallo.getClass().getSimpleName());
                if (!esperar(esperas.get(intento - 1))) {
                    return;
                }
            }
        }
    }

    private MimeMessage armar(MensajeEmail mensaje) throws MessagingException {
        MimeMessage mime = enviador.createMimeMessage();
        MimeMessageHelper ayudante = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
        ayudante.setFrom(remitente);
        ayudante.setTo(mensaje.destinatario());
        ayudante.setSubject(mensaje.asunto());
        ayudante.setText(mensaje.texto(), mensaje.html());
        return mime;
    }

    private static boolean esperar(Duration espera) {
        try {
            Thread.sleep(espera.toMillis());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
