package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.CierreDeSesiones;
import ar.edu.utn.odonto.identidad.application.EventosDeRecuperacion;
import ar.edu.utn.odonto.identidad.application.PlantillaRecuperacion;
import ar.edu.utn.odonto.notificaciones.application.MensajeriaPort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lo que pasa despues de confirmar la recuperacion (C-04). El email sale en segundo plano para que el
 * tiempo de respuesta no revele si la cuenta existe (D5); si la transaccion se revierte, no sale nada.
 */
@Component
public class ReaccionesDeRecuperacion {

    private final MensajeriaPort mensajeria;
    private final CierreDeSesiones sesiones;

    public ReaccionesDeRecuperacion(MensajeriaPort mensajeria, CierreDeSesiones sesiones) {
        this.mensajeria = mensajeria;
        this.sesiones = sesiones;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarEnlace(EventosDeRecuperacion.RecuperacionSolicitada pedido) {
        mensajeria.enviarEmail(PlantillaRecuperacion.armar(pedido));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void cerrarSesiones(EventosDeRecuperacion.PasswordRestablecida restablecida) {
        sesiones.cerrarTodas(restablecida.usuarioId());
    }
}
