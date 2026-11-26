package ar.edu.utn.odonto.identidad.application;

import java.util.UUID;

/** Eventos internos de la recuperacion de contraseña; se atienden despues del commit (C-04, D5, D7). */
public final class EventosDeRecuperacion {

    private EventosDeRecuperacion() {}

    /** Hay que enviar el enlace. Lleva el token en claro solo en memoria; nunca se imprime. */
    public record RecuperacionSolicitada(String destinatario, String nombre, String enlace) {

        @Override
        public String toString() {
            return "RecuperacionSolicitada[***]";
        }
    }

    /** Se restablecio la contraseña: hay que cerrar las sesiones abiertas del usuario. */
    public record PasswordRestablecida(UUID usuarioId) {}
}
