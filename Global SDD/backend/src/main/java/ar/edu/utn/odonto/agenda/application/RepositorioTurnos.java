package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.Turno;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de turnos. La no superposicion la garantiza la base (RN-AG-01); el bloqueo es optimista (D5). */
public interface RepositorioTurnos {

    void insertar(Turno turno);

    /**
     * Guarda los cambios de estado si la version en la base sigue siendo {@code turno.version()} y la incrementa.
     * @return false si otro lo modifico antes
     */
    boolean actualizarSiNoCambio(Turno turno);

    Optional<Turno> buscar(UUID id);

    /** Turnos que empiezan en [desde, hasta), ordenados por inicio, con datos para mostrar (D8). */
    List<TurnoListado> listar(Instant desde, Instant hasta);
}
