package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.identidad.domain.TokenDeRecuperacion;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion de contraseña por email (US-003, C-04). El pedido nunca revela si la cuenta existe: no
 * lanza, y el email sale despues del commit en segundo plano (D4, D5). El restablecimiento usa un token
 * de un solo uso y cierra las sesiones abiertas del usuario (D6, D7).
 */
@Service
public class ServicioDeRecuperacion {

    static final String RUTA_RESTABLECER = "/restablecer?token=";

    private final RepositorioUsuarios usuarios;
    private final RepositorioTokensRecuperacion tokens;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;
    private final String urlBase;

    public ServicioDeRecuperacion(
            RepositorioUsuarios usuarios,
            RepositorioTokensRecuperacion tokens,
            PasswordEncoder codificador,
            Clock reloj,
            ApplicationEventPublisher eventos,
            @Value("${app.base-url}") String urlBase) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.codificador = codificador;
        this.reloj = reloj;
        this.eventos = eventos;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
    }

    /** Si la cuenta existe y esta activa, invalida los tokens anteriores, crea uno y pide el envio del email. */
    @Transactional
    public void solicitar(String email, String ip) {
        Optional<Usuario> encontrado =
                email == null || email.isBlank() ? Optional.empty() : usuarios.buscarPorEmailParaActualizar(email);
        if (encontrado.isEmpty() || !encontrado.get().activo()) {
            return;
        }
        Usuario usuario = encontrado.get();
        Instant ahora = ahora();
        String token = GeneradorDeTokens.nuevo();
        tokens.invalidarVigentes(usuario.id(), ahora);
        tokens.insertar(TokenDeRecuperacion.nuevo(usuario.id(), GeneradorDeTokens.hash(token), ahora));

        String id = usuario.id().toString();
        eventos.publishEvent(new EventoAuditoria(AccionAuditoria.SOLICITAR_RECUPERACION, TipoActor.SISTEMA, null,
                ServicioDeLogin.ENTIDAD, id, Map.of(), ip));
        eventos.publishEvent(new EventosDeRecuperacion.RecuperacionSolicitada(
                usuario.email(), usuario.nombre(), urlBase + RUTA_RESTABLECER + token));
    }

    /**
     * Restablece la contraseña con un token vigente. Si la contraseña nueva no cumple la politica, la
     * transaccion se revierte y el token sigue sirviendo.
     */
    @Transactional
    public void restablecer(String token, String nueva, String ip) {
        Instant ahora = ahora();
        TokenDeRecuperacion encontrado = (token == null || token.isBlank()
                        ? Optional.<TokenDeRecuperacion>empty()
                        : tokens.buscarPorHashParaActualizar(GeneradorDeTokens.hash(token)))
                .filter(t -> t.vigente(ahora))
                .orElseThrow(TokenInvalidoException::new);
        Usuario usuario = usuarios.buscarPorIdParaActualizar(encontrado.usuarioId())
                .filter(Usuario::activo)
                .orElseThrow(TokenInvalidoException::new);

        PoliticaPassword.validarNueva(nueva);
        usuario.restablecerPassword(codificador.encode(nueva));
        usuarios.actualizar(usuario);
        encontrado.usar(ahora);
        tokens.marcarUsado(encontrado);

        String id = usuario.id().toString();
        eventos.publishEvent(new EventoAuditoria(AccionAuditoria.MODIFICAR_ENTIDAD, TipoActor.USUARIO, id,
                ServicioDeLogin.ENTIDAD, id, Map.of("referencia", "recuperacion-password"), ip));
        eventos.publishEvent(new EventosDeRecuperacion.PasswordRestablecida(usuario.id()));
    }

    /** Precision de TIMESTAMPTZ: lo comparado coincide con lo guardado. */
    private Instant ahora() {
        return reloj.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
