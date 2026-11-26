package ar.edu.utn.odonto.identidad.application;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.auditoria.application.evento.EventoAuditoria;
import ar.edu.utn.odonto.identidad.domain.Rol;
import ar.edu.utn.odonto.identidad.domain.Usuario;
import java.time.Clock;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el odontologo inicial de una instancia vacia (D2). La contraseña es temporal: el usuario
 * queda obligado a cambiarla en su primer login. Nunca se loguean el email ni la contraseña.
 */
@Service
public class AltaOdontologoInicial {

    public static final String VARIABLE_EMAIL = "ADMIN_INITIAL_EMAIL";
    public static final String VARIABLE_PASSWORD = "ADMIN_INITIAL_PASSWORD";
    static final String NOMBRE_PROVISORIO = "Odontólogo";

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder codificador;
    private final Clock reloj;
    private final ApplicationEventPublisher eventos;

    public AltaOdontologoInicial(
            RepositorioUsuarios usuarios,
            PasswordEncoder codificador,
            Clock reloj,
            ApplicationEventPublisher eventos) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.reloj = reloj;
        this.eventos = eventos;
    }

    /** @return true si creo el usuario; false si ya existia un odontologo (las variables se ignoran). */
    @Transactional
    public boolean crearSiFalta(String email, String password) {
        if (usuarios.existeOdontologo()) {
            return false;
        }
        exigir(email, VARIABLE_EMAIL);
        exigir(password, VARIABLE_PASSWORD);

        Usuario odontologo = Usuario.nuevo(
                email.trim(), codificador.encode(password), NOMBRE_PROVISORIO, Rol.ODONTOLOGO, reloj.instant());
        usuarios.insertar(odontologo);
        String id = odontologo.id().toString();
        eventos.publishEvent(new EventoAuditoria(
                AccionAuditoria.CREAR_ENTIDAD, TipoActor.SISTEMA, null, ServicioDeLogin.ENTIDAD, id, Map.of(), null));
        return true;
    }

    private static void exigir(String valor, String variable) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno " + variable + " para crear el odontólogo inicial");
        }
    }
}
