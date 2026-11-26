package ar.edu.utn.odonto.pacientes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Celulares argentinos en sus formatos habituales a E.164 (D3). */
class NormalizadorDeTelefonoTest {

    @ParameterizedTest
    @CsvSource({
        "011 15-2345-6789, +5491123456789",
        "+54 9 11 2345-6789, +5491123456789",
        "1123456789, +5491123456789",
        "11 2345 6789, +5491123456789",
        "+54 11 2345-6789, +5491123456789",
        "(0351) 15 555-1234, +5493515551234",
        "+5493515551234, +5493515551234",
        "0261 15 412 3456, +5492614123456",
    })
    void normalizaLosFormatosArgentinosAlCelularE164(String texto, String esperado) {
        assertThat(NormalizadorDeTelefono.normalizar(texto)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "hola", "11 2345", "+54 9 11 2345 6789 1234"})
    void rechazaLoQueNoEsUnCelularValido(String texto) {
        assertThatThrownBy(() -> NormalizadorDeTelefono.normalizar(texto))
                .isInstanceOf(DatoDePacienteInvalidoException.class)
                .satisfies(e -> assertThat(((DatoDePacienteInvalidoException) e).campo()).isEqualTo("telefono"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unTelefonoVacioNoSeNormaliza(String texto) {
        assertThat(NormalizadorDeTelefono.intentar(texto)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"+1 650 253 0000, +16502530000"})
    void unNumeroExtranjeroConCodigoDePaisSeConserva(String texto, String esperado) {
        assertThat(NormalizadorDeTelefono.intentar(texto)).contains(esperado);
    }
}
