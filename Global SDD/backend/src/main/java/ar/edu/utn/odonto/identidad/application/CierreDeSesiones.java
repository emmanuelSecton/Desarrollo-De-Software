package ar.edu.utn.odonto.identidad.application;

import java.util.UUID;

/** Cierra todas las sesiones abiertas de un usuario (C-04, D7); la implementacion usa el SessionRegistry. */
public interface CierreDeSesiones {

    void cerrarTodas(UUID usuarioId);
}
