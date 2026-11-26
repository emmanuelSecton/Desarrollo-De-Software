package ar.edu.utn.odonto.notificaciones.infrastructure;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ar.edu.utn.odonto.notificaciones.application.MensajeriaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Sin {@code SMTP_HOST} la instancia arranca igual y no envia emails (C-04, D2). */
@Component
@ConditionalOnExpression("'${spring.mail.host:}'.isBlank()")
public class EmailDeshabilitadoAdapter implements MensajeriaPort {

    private static final Logger LOG = LoggerFactory.getLogger(EmailDeshabilitadoAdapter.class);

    @Override
    public void enviarEmail(MensajeEmail mensaje) {
        LOG.warn("Envío de email deshabilitado (SMTP_HOST vacío): no se envió un email de tipo {}", mensaje.tipo());
    }
}
