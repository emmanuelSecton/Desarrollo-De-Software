package ar.edu.utn.odonto.identidad.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Ventana de 5 fallos en 15 min y bloqueo de 15 min (RN-SEG-03, D5), con instantes fijos. */
class UsuarioTest {

    private static final Instant T0 = Instant.parse("2026-11-25T12:00:00Z");

    private static Usuario usuario() {
        return Usuario.nuevo("u@consultorio.test", "hash", "Usuario", Rol.SECRETARIA, T0);
    }

    private static void fallar(Usuario usuario, int veces, Instant desde) {
        for (int i = 0; i < veces; i++) {
            usuario.registrarFallo(desde.plusSeconds(i));
        }
    }

    @Test
    void unUsuarioNuevoEstaActivoSinFallosYDebeCambiarLaContrasena() {
        Usuario usuario = usuario();

        assertThat(usuario.activo()).isTrue();
        assertThat(usuario.debeCambiarPassword()).isTrue();
        assertThat(usuario.intentosFallidos()).isZero();
        assertThat(usuario.estaBloqueado(T0)).isFalse();
    }

    @Test
    void cuatroFallosDentroDeLaVentanaNoBloquean() {
        Usuario usuario = usuario();

        fallar(usuario, 4, T0);

        assertThat(usuario.intentosFallidos()).isEqualTo(4);
        assertThat(usuario.estaBloqueado(T0.plusSeconds(10))).isFalse();
    }

    @Test
    void elQuintoFalloDentroDeLaVentanaBloqueaQuinceMinutos() {
        Usuario usuario = usuario();
        Instant quinto = T0.plus(Duration.ofMinutes(14));

        fallar(usuario, 4, T0);
        usuario.registrarFallo(quinto);

        assertThat(usuario.bloqueadoHasta()).isEqualTo(quinto.plus(Duration.ofMinutes(15)));
        assertThat(usuario.estaBloqueado(quinto.plus(Duration.ofMinutes(10)))).isTrue();
        assertThat(usuario.estaBloqueado(quinto.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void losFallosFueraDeLaVentanaReinicianElConteo() {
        Usuario usuario = usuario();

        fallar(usuario, 4, T0);
        usuario.registrarFallo(T0.plus(Duration.ofMinutes(15)));

        assertThat(usuario.intentosFallidos()).isEqualTo(1);
        assertThat(usuario.primerFalloAt()).isEqualTo(T0.plus(Duration.ofMinutes(15)));
        assertThat(usuario.estaBloqueado(T0.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void unLoginExitosoReiniciaElContadorYRegistraElUltimoLogin() {
        Usuario usuario = usuario();
        Instant exito = T0.plusSeconds(30);

        fallar(usuario, 3, T0);
        usuario.registrarExito(exito);

        assertThat(usuario.intentosFallidos()).isZero();
        assertThat(usuario.primerFalloAt()).isNull();
        assertThat(usuario.bloqueadoHasta()).isNull();
        assertThat(usuario.ultimoLoginAt()).isEqualTo(exito);
    }

    @Test
    void cambiarLaContrasenaLimpiaLaMarcaDeCambioObligatorio() {
        Usuario usuario = usuario();

        usuario.cambiarPassword("nuevo-hash");

        assertThat(usuario.passwordHash()).isEqualTo("nuevo-hash");
        assertThat(usuario.debeCambiarPassword()).isFalse();
    }
}
