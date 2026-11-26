package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.identidad.domain.TokenDeRecuperacion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de tokens de recuperacion; el adaptador vive en infrastructure. */
public interface RepositorioTokensRecuperacion {

    void insertar(TokenDeRecuperacion token);

    /** Busca por hash y bloquea la fila hasta el fin de la transaccion. */
    Optional<TokenDeRecuperacion> buscarPorHashParaActualizar(String hash);

    /** Marca como usados (invalidados) todos los tokens sin usar del usuario. */
    void invalidarVigentes(UUID usuarioId, Instant ahora);

    void marcarUsado(TokenDeRecuperacion token);
}
