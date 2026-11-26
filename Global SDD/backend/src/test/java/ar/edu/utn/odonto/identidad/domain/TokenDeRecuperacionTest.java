package ar.edu.utn.odonto.identidad.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.identidad.application.GeneradorDeTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Token de recuperacion: aleatorio de 256 bits, hash SHA-256, vigencia de 1 hora y un solo uso (C-04, D3). */
class TokenDeRecuperacionTest {

    private static final Instant T0 = Instant.parse("2026-11-26T12:00:00Z");

    @Test
    void losTokensSonDe43CaracteresUrlSafeYNoSeRepiten() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String token = GeneradorDeTokens.nuevo();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(1000);
    }

    @Test
    void elHashEsSha256EnHexadecimalYDeterministico() {
        assertThat(GeneradorDeTokens.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(GeneradorDeTokens.hash("abc")).isEqualTo(GeneradorDeTokens.hash("abc"));
    }

    @Test
    void venceALaHora() {
        TokenDeRecuperacion token = TokenDeRecuperacion.nuevo(UUID.randomUUID(), "h", T0);

        assertThat(token.venceAt()).isEqualTo(T0.plus(Duration.ofHours(1)));
        assertThat(token.vigente(T0.plus(Duration.ofMinutes(59)))).isTrue();
        assertThat(token.vigente(T0.plus(Duration.ofHours(1)))).isFalse();
    }

    @Test
    void usadoDejaDeEstarVigente() {
        TokenDeRecuperacion token = TokenDeRecuperacion.nuevo(UUID.randomUUID(), "h", T0);

        token.usar(T0.plusSeconds(10));

        assertThat(token.vigente(T0.plusSeconds(20))).isFalse();
    }

    @Test
    void restablecerLaContrasenaLimpiaElBloqueoYElCambioObligatorio() {
        Usuario usuario = Usuario.nuevo("u@consultorio.test", "hash", "Usuario", Rol.SECRETARIA, T0);
        for (int i = 0; i < Usuario.MAX_INTENTOS; i++) {
            usuario.registrarFallo(T0.plusSeconds(i));
        }
        assertThat(usuario.estaBloqueado(T0.plusSeconds(30))).isTrue();

        usuario.restablecerPassword("nuevo-hash");

        assertThat(usuario.estaBloqueado(T0.plusSeconds(30))).isFalse();
        assertThat(usuario.intentosFallidos()).isZero();
        assertThat(usuario.debeCambiarPassword()).isFalse();
        assertThat(usuario.passwordHash()).isEqualTo("nuevo-hash");
    }
}
