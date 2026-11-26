package ar.edu.utn.odonto.pacientes.application;

import ar.edu.utn.odonto.auditoria.application.TipoActor;
import java.util.Objects;

/** Quien ejecuta una operacion sobre pacientes, para la auditoria (D9). C-20 usara PACIENTE. */
public record Actor(TipoActor tipo, String id) {

    public Actor {
        Objects.requireNonNull(tipo, "tipo");
    }

    public static Actor usuario(String id) {
        return new Actor(TipoActor.USUARIO, Objects.requireNonNull(id, "id"));
    }
}
