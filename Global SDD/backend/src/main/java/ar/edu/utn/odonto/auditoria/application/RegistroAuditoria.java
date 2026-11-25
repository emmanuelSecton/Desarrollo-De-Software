package ar.edu.utn.odonto.auditoria.application;

import java.util.Map;

/** Contrato público de registro de auditoría (D3). API pública del módulo. */
public record RegistroAuditoria(
        AccionAuditoria accion,
        TipoActor tipoActor,
        String actorId,
        String entidad,
        String entidadId,
        Map<String, Object> detalle,
        String ip) {}
