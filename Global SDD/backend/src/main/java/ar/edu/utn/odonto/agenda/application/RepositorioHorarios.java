package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.FranjaDeAtencion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de las franjas de atencion. La no superposicion la garantiza la base (D4). */
public interface RepositorioHorarios {

    /** Ordenadas por dia y hora de inicio. */
    List<FranjaDeAtencion> listar();

    Optional<FranjaDeAtencion> buscar(UUID id);

    void insertar(FranjaDeAtencion franja);

    void actualizar(FranjaDeAtencion franja);

    void borrar(UUID id);
}
