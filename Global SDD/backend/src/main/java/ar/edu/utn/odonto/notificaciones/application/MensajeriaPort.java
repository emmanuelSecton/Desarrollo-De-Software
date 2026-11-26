package ar.edu.utn.odonto.notificaciones.application;

/**
 * Puerto de mensajeria saliente (08_arquitectura_propuesta.md §Patrones). En C-04 solo email; WhatsApp
 * se suma en C-15. Las implementaciones nunca lanzan por fallas de envio: reintentan y registran el error.
 */
public interface MensajeriaPort {

    void enviarEmail(MensajeEmail mensaje);
}
