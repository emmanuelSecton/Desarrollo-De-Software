package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica credenciales, aplica el bloqueo por intentos (RN-SEG-03) y audita LOGIN / LOGIN_FALLIDO.
 * Un rechazo NO lanza excepcion: la transaccion se confirma y el contador y el evento persisten (D5).
 */
@Service
public class ServicioDeLogin {

    static final String ENTIDAD = "USUARIO";
    static final String ID_DESCONOCIDO = "desconocido";

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;
    /** Hash de una contraseña aleatoria: igualar el costo cuando el email no existe evita enumerar cuentas. */
    private final String hashFicticio;

    public ServicioDeLogin(
            RepositorioUsuarios usuarios,
            PasswordEncoder codificador,
            Clock reloj,
            ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.reloj = reloj;
        this.eventos = eventos;
        this.hashFicticio = codificador.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public ResultadoLogin autenticar(String email, String password, String ip) {
        Instant ahora = reloj.instant();
        String clave = password == null ? "" : password;
        Optional<Usuario> encontrado =
                email == null ? Optional.empty() : usuarios.buscarPorEmailParaActualizar(email);

        if (encontrado.isEmpty()) {
            codificador.matches(clave, hashFicticio);
            return rechazar(ID_DESCONOCIDO, MotivoRechazo.CREDENCIALES, ip);
        }
        Usuario usuario = encontrado.get();
        String id = usuario.id().toString();
        if (!usuario.activo()) {
            codificador.matches(clave, hashFicticio);
            return rechazar(id, MotivoRechazo.INACTIVA, ip);
        }
        if (usuario.estaBloqueado(ahora)) {
            codificador.matches(clave, hashFicticio);
            return rechazar(id, MotivoRechazo.BLOQUEADA, ip);
        }
        if (!codificador.matches(clave, usuario.passwordHash())) {
            usuario.registrarFallo(ahora);
            usuarios.actualizar(usuario);
            return rechazar(id, MotivoRechazo.CREDENCIALES, ip);
        }

        usuario.registrarExito(ahora);
        usuarios.actualizar(usuario);
        eventos.publishEvent(new EventoAuditoria(
                AccionAuditoria.LOGIN, TipoActor.USUARIO, id, ENTIDAD, id, Map.of(), ip));
        return new ResultadoLogin.Exitoso(UsuarioAutenticado.de(usuario));
    }

    private ResultadoLogin rechazar(String entidadId, MotivoRechazo motivo, String ip) {
        eventos.publishEvent(new EventoAuditoria(
                AccionAuditoria.LOGIN_FALLIDO,
                TipoActor.SISTEMA,
                null,
                ENTIDAD,
                entidadId,
                Map.of("motivo_codigo", motivo.name()),
                ip));
        return new ResultadoLogin.Rechazado(motivo);
    }
}
