package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operaciones del usuario sobre su propia cuenta: consultar sus datos y cambiar su contraseña (D8). */
@Service
public class ServicioDeCuenta {

    static final String ACTUAL_INCORRECTA = "La contraseña actual no es correcta";
    static final String NUEVA_IGUAL = "La nueva contraseña debe ser distinta de la actual";

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final ApplicationEventPublisher eventos;

    public ServicioDeCuenta(
            RepositorioUsuarios usuarios, PasswordEncoder codificador, ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> buscar(UUID id) {
        return usuarios.buscarPorId(id).map(UsuarioAutenticado::de);
    }

    @Transactional
    public UsuarioAutenticado cambiarPassword(UUID id, String actual, String nueva, String ip) {
        Usuario usuario = usuarios.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new CambioPasswordInvalidoException(ACTUAL_INCORRECTA));
        if (actual == null || !codificador.matches(actual, usuario.passwordHash())) {
            throw new CambioPasswordInvalidoException(ACTUAL_INCORRECTA);
        }
        PoliticaPassword.validarNueva(nueva);
        if (codificador.matches(nueva, usuario.passwordHash())) {
            throw new CambioPasswordInvalidoException(NUEVA_IGUAL);
        }

        usuario.cambiarPassword(codificador.encode(nueva));
        usuarios.actualizar(usuario);
        String idTexto = usuario.id().toString();
        eventos.publishEvent(new EventoAuditoria(
                AccionAuditoria.MODIFICAR_ENTIDAD,
                TipoActor.USUARIO,
                idTexto,
                ServicioDeLogin.ENTIDAD,
                idTexto,
                Map.of("referencia", "cambio-password"),
                ip));
        return UsuarioAutenticado.de(usuario);
    }
}
