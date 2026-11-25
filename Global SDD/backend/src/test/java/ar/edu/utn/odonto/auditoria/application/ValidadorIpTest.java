package ar.edu.utn.odonto.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** IP nullable y validada sintáticamente, sin resolver hostnames (C-02, D5). */
class ValidadorIpTest {

    private final ValidadorIp validador = new ValidadorIp();

    @Test
    void unaIpV4ValidaSeAcepta() {
        assertThat(validador.validar("200.24.18.7")).isEqualTo("200.24.18.7");
    }

    @Test
    void unaIpV6ValidaSeAcepta() {
        assertThat(validador.validar("2001:db8::1")).isEqualTo("2001:db8::1");
    }

    @Test
    void unaIpNulaSeAcepta() {
        assertThat(validador.validar(null)).isNull();
    }

    @Test
    void unHostnameOUnaIpInvalidaSeRechazan() {
        assertThatThrownBy(() -> validador.validar("consultorio.ejemplo.com"))
                .isInstanceOf(AuditoriaInvalidaException.class)
                .hasMessageContaining("ip");
        assertThatThrownBy(() -> validador.validar("999.24.18.7"))
                .isInstanceOf(AuditoriaInvalidaException.class)
                .hasMessageContaining("ip");
    }
}
