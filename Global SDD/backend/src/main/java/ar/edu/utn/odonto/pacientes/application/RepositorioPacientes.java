package ar.edu.utn.odonto.pacientes.application;

import ar.edu.utn.odonto.pacientes.domain.Paciente;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de pacientes; el adaptador vive en infrastructure. */
public interface RepositorioPacientes {

    /** Inserta si no existe otro paciente con el mismo DNI (D6). @return true si inserto. */
    boolean insertarSiNoExiste(Paciente paciente);

    void actualizar(Paciente paciente);

    Optional<Paciente> buscarPorId(UUID id);

    /** Busca y bloquea la fila hasta el fin de la transaccion. */
    Optional<Paciente> buscarPorIdParaActualizar(UUID id);

    Optional<Paciente> buscarPorDni(String dni);

    Pagina<Paciente> buscar(CriterioDeBusqueda criterio, boolean incluirInactivos, int pagina, int tamano);
}
