package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.Prestacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto del catalogo de prestaciones. Sin borrado: la baja es logica (D5). */
public interface RepositorioPrestaciones {

    /** Ordenadas por nombre. */
    List<Prestacion> listar(boolean incluirInactivas);

    Optional<Prestacion> buscarPrestacion(UUID id);

    void insertar(Prestacion prestacion);

    void actualizar(Prestacion prestacion);
}
