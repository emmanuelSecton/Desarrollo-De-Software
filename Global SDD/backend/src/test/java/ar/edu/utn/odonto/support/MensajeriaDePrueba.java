package ar.edu.utn.odonto.support;

import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ar.edu.utn.odonto.notificaciones.application.MensajeriaPort;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Doble de {@link MensajeriaPort} que guarda los emails para que el test los espere y los lea (C-04, D10). */
@TestConfiguration(proxyBeanMethods = false)
public class MensajeriaDePrueba {

    /** Emails capturados; el envio real es asincrono, asi que el test espera con {@link #esperar}. */
    public static class Capturados implements MensajeriaPort {
        private final LinkedBlockingQueue<MensajeEmail> recibidos = new LinkedBlockingQueue<>();

        @Override
        public void enviarEmail(MensajeEmail mensaje) {
            recibidos.add(mensaje);
        }

        /** Espera el siguiente email para ese destinatario, descartando los de otros. */
        public Optional<MensajeEmail> esperar(String destinatario, Duration plazo) throws InterruptedException {
            long limite = System.nanoTime() + plazo.toNanos();
            while (System.nanoTime() < limite) {
                MensajeEmail mensaje = recibidos.poll(50, TimeUnit.MILLISECONDS);
                if (mensaje != null && mensaje.destinatario().equalsIgnoreCase(destinatario)) {
                    return Optional.of(mensaje);
                }
            }
            return Optional.empty();
        }

        /** Token en claro del enlace del email. */
        public static String token(MensajeEmail mensaje) {
            String marca = "token=";
            int inicio = mensaje.texto().indexOf(marca) + marca.length();
            int fin = mensaje.texto().indexOf('\n', inicio);
            return mensaje.texto().substring(inicio, fin).trim();
        }
    }

    @Bean
    @Primary
    Capturados mensajeriaCapturada() {
        return new Capturados();
    }
}
