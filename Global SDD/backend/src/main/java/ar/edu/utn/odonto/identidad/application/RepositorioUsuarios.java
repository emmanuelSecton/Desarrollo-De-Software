package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de usuarios internos; el adaptador vive en infrastructure. */
public interface RepositorioUsuarios {

    /** Busca por email sin distinguir mayusculas y bloquea la fila hasta el fin de la transaccion. */
    Optional<Usuario> buscarPorEmailParaActualizar(String email);

    Optional<Usuario> buscarPorId(UUID id);

    /** Busca por id y bloquea la fila hasta el fin de la transaccion. */
    Optional<Usuario> buscarPorIdParaActualizar(UUID id);

    /** true si existe algun ODONTOLOGO, activo o no. */
    boolean existeOdontologo();

    void insertar(Usuario usuario);

    void actualizar(Usuario usuario);
}
