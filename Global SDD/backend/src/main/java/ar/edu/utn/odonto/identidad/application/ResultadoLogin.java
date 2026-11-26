package ar.edu.utn.odonto.identidad.application;

/** Un login rechazado es un resultado, no una excepcion: la transaccion se confirma igual (D5). */
public sealed interface ResultadoLogin {

    record Exitoso(UsuarioAutenticado usuario) implements ResultadoLogin {}

    record Rechazado(MotivoRechazo motivo) implements ResultadoLogin {}
}
