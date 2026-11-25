package ar.edu.utn.odonto.auditoria.infrastructure;

import ar.edu.utn.odonto.auditoria.application.AuditoriaService;
import ar.edu.utn.odonto.auditoria.application.RegistroAuditoria;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Puente entre eventos de dominio y el registro append-only (D6). */
@Component
public class AuditoriaListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaListener.class);
    private static final String MDC_ACCION = "auditoria.accion";
    private static final String MDC_ENTIDAD = "auditoria.entidad";
    private static final String MDC_MOTIVO = "auditoria.motivo";
    private static final String MDC_CORRELACION = "auditoria.correlacion";

    private final AuditoriaService servicio;

    public AuditoriaListener(AuditoriaService servicio) {
        this.servicio = servicio;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void alConfirmar(EventoAuditoria evento) {
        try {
            servicio.registrar(aRegistro(evento));
        } catch (RuntimeException fallo) {
            loguearFallo(evento, fallo);
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw fallo;
            }
        }
    }

    private static void loguearFallo(EventoAuditoria evento, RuntimeException fallo) {
        try {
            MDC.put(MDC_ACCION, String.valueOf(evento.accion()));
            MDC.put(MDC_ENTIDAD, String.valueOf(evento.entidad()));
            MDC.put(MDC_MOTIVO, fallo.getClass().getSimpleName());
            MDC.put(MDC_CORRELACION, UUID.randomUUID().toString());
            LOG.error("No se pudo registrar el evento de auditoría");
        } finally {
            MDC.remove(MDC_ACCION);
            MDC.remove(MDC_ENTIDAD);
            MDC.remove(MDC_MOTIVO);
            MDC.remove(MDC_CORRELACION);
        }
    }

    private static RegistroAuditoria aRegistro(EventoAuditoria evento) {
        return new RegistroAuditoria(evento.accion(), evento.tipoActor(), evento.actorId(), evento.entidad(),
                evento.entidadId(), evento.detalle(), evento.ip());
    }
}
