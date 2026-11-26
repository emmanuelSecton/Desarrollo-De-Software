package ar.edu.utn.odonto.identidad.application;

/** Motivo de un login rechazado. Solo se registra en auditoria; la respuesta HTTP es siempre la misma (D5). */
public enum MotivoRechazo {
    CREDENCIALES,
    BLOQUEADA,
    INACTIVA
}
