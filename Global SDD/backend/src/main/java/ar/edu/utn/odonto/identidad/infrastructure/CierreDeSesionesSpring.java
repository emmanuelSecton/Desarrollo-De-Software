package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.CierreDeSesiones;
import java.util.UUID;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Marca como vencidas todas las sesiones registradas del usuario (C-04, D7). El {@code ConcurrentSessionFilter}
 * responde 401 en la siguiente peticion de cada una.
 */
@Component
public class CierreDeSesionesSpring implements CierreDeSesiones {

    private final SessionRegistry registro;

    public CierreDeSesionesSpring(SessionRegistry registro) {
        this.registro = registro;
    }

    @Override
    public void cerrarTodas(UUID usuarioId) {
        registro.getAllSessions(usuarioId, false).forEach(SessionInformation::expireNow);
    }
}
