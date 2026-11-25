package ar.edu.utn.odonto.auditoria.application.evento;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import java.util.Map;

/** Evento publicado por otros módulos; vive en application para ser API pública (D3). */
public record EventoAuditoria(
        AccionAuditoria accion,
        TipoActor tipoActor,
        String actorId,
        String entidad,
        String entidadId,
        Map<String, Object> detalle,
        String ip) {}
