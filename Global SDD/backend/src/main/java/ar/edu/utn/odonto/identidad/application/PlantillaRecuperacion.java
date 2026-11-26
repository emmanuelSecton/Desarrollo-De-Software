package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import org.springframework.web.util.HtmlUtils;

/** Email de recuperacion de contraseña (C-04, D9): texto y HTML simples, sin imagenes remotas ni datos clinicos. */
public final class PlantillaRecuperacion {

    public static final String TIPO = "RECUPERACION_PASSWORD";
    static final String ASUNTO = "Restablecé tu contraseña";

    private PlantillaRecuperacion() {}

    public static MensajeEmail armar(EventosDeRecuperacion.RecuperacionSolicitada pedido) {
        String texto = "Hola " + pedido.nombre() + ",\n\n"
                + "Recibimos un pedido para restablecer tu contraseña. Para elegir una nueva, abrí este enlace:\n\n"
                + pedido.enlace() + "\n\n"
                + "El enlace vence en 1 hora y sirve una sola vez.\n"
                + "Si no fuiste vos, ignorá este mensaje: tu contraseña no cambia.\n";
        String nombre = HtmlUtils.htmlEscape(pedido.nombre());
        String enlace = HtmlUtils.htmlEscape(pedido.enlace());
        String html = "<!doctype html><html lang=\"es\"><body style=\"font-family:Arial,sans-serif;color:#1f2933\">"
                + "<p>Hola " + nombre + ",</p>"
                + "<p>Recibimos un pedido para restablecer tu contraseña.</p>"
                + "<p><a href=\"" + enlace + "\" style=\"color:#0b5cad\">Elegir una contraseña nueva</a></p>"
                + "<p>El enlace vence en 1 hora y sirve una sola vez.</p>"
                + "<p>Si no fuiste vos, ignorá este mensaje: tu contraseña no cambia.</p>"
                + "</body></html>";
        return new MensajeEmail(TIPO, pedido.destinatario(), ASUNTO, texto, html);
    }
}
