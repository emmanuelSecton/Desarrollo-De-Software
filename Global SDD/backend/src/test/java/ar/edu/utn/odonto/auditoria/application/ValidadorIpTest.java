package ar.edu.utn.odonto.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** IP nullable y validada sintáticamente, sin resolver hostnames (C-02, D5). */
class ValidadorIpTest {

    private final ValidadorIp validador = new ValidadorIp();

    @Test
    void elValidadorNoResuelveNombresConInetAddress() throws Exception {
        Path fuente = Path.of("src", "main", "java", "ar", "edu", "utn", "odonto", "auditoria", "application",
                "ValidadorIp.java").toAbsolutePath().normalize();

        assertThat(Files.readString(fuente)).doesNotContain("InetAddress").doesNotContain("getByName");
    }

    @Test
    void unaIpV4ValidaSeAcepta() {
        assertThat(validador.validar("200.24.18.7")).isEqualTo("200.24.18.7");
    }

    @Test
    void unaIpV6ValidaSeAcepta() {
        assertThat(validador.validar("2001:db8::1")).isEqualTo("2001:db8::1");
    }

    @Test
    void unaIpV6CompletaSeAcepta() {
        String completa = "2001:0DB8:0000:0000:0000:0000:0000:0001";

        assertThat(validador.validar(completa)).isEqualTo(completa);
    }

    @Test
    void ipv6ComprimidoConIPv4IncrustadoSeAcepta() {
        assertThat(ValidadorIp.esIpv6Literal("::ffff:192.0.2.1")).isTrue();
        assertThat(ValidadorIp.esIpv6Literal("2001:db8:0:0:0:0:192.0.2.1")).isTrue();
    }

    @Test
    void formasMalformadasYHostnamesSeRechazanSinResolucion() {
        for (String ip : List.of("1:2", ":::1", "2001:db8::1::2", "1:2:3:4:5:6:7:8:9", "2001:db8::gg",
                "consultorio.ejemplo.com")) {
            assertThatThrownBy(() -> validador.validar(ip)).isInstanceOf(AuditoriaInvalidaException.class);
        }
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
