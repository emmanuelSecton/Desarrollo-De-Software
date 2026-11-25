package ar.edu.utn.odonto.auditoria.application;

/** Puerto de salida: la application no depende de infrastructure (D3). */
public interface EventoAuditoriaPort {

    void guardar(RegistroAuditoria registro);
}
