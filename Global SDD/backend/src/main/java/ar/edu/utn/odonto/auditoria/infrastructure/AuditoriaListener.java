package ar.edu.utn.odonto.auditoria.infrastructure;

import ar.edu.utn.odonto.auditoria.application.AuditoriaService;
import ar.edu.utn.odonto.auditoria.application.RegistroAuditoria;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Puente entre eventos de dominio y el registro append-only (D6). */
@Component
public class AuditoriaListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaListener.class);

    private final AuditoriaService servicio;

    public AuditoriaListener(AuditoriaService servicio) {
        this.servicio = servicio;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alConfirmar(EventoAuditoria evento) {
        try {
            servicio.registrar(aRegistro(evento));
        } catch (RuntimeException fallo) {
            loguearFallo(evento, fallo);
        }
    }

    private static void loguearFallo(EventoAuditoria evento, RuntimeException fallo) {
        try {
            MDC.put("accion", evento.accion().name());
            MDC.put("entidad", evento.entidad());
            MDC.put("entidad_id", String.valueOf(evento.entidadId()));
            MDC.put("motivo", fallo.getClass().getSimpleName());
            LOG.error("No se pudo registrar el evento de auditoría después del commit");
        } finally {
            MDC.clear();
        }
    }

    private static RegistroAuditoria aRegistro(EventoAuditoria evento) {
        return new RegistroAuditoria(evento.accion(), evento.tipoActor(), evento.actorId(), evento.entidad(),
                evento.entidadId(), evento.detalle(), evento.ip());
    }
}
