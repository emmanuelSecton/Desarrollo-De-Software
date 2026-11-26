package ar.edu.utn.odonto.notificaciones.application;

import java.util.Objects;

/**
 * Email saliente con version de texto y HTML (C-04, D1). {@code tipo} identifica el mensaje en los logs;
 * el resto (destinatario, asunto, cuerpos) nunca se imprime (Ley 25.326).
 */
public record MensajeEmail(String tipo, String destinatario, String asunto, String texto, String html) {

    public MensajeEmail {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(destinatario, "destinatario");
        Objects.requireNonNull(asunto, "asunto");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(html, "html");
    }

    @Override
    public String toString() {
        return "MensajeEmail[tipo=" + tipo + "]";
    }
}
