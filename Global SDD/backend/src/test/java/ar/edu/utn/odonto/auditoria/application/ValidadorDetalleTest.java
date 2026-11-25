package ar.edu.utn.odonto.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** Whitelist de detalle y rechazo sin loguear valores sensibles (C-02, D5). */
class ValidadorDetalleTest {

    private final ValidadorDetalle validador = new ValidadorDetalle();
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void capturarLogs() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(ValidadorDetalle.class)).addAppender(appender);
    }

    @AfterEach
    void soltarLogs() {
        ((Logger) LoggerFactory.getLogger(ValidadorDetalle.class)).detachAppender(appender);
    }

    @Test
    void unaClaveDeReferenciaSeAcepta() {
        Map<String, Object> resultado = validador.validar(Map.of("turno_id", 42));

        assertThat(resultado).containsExactly(Map.entry("turno_id", 42));
    }

    @Test
    void unaClaveSensibleSeRechazaSinLoguearSuValor() {
        String valorSensible = "diagnostico-confidencial-xyz";

        assertThatThrownBy(() -> validador.validar(Map.of("diagnostico", valorSensible)))
                .isInstanceOf(AuditoriaInvalidaException.class)
                .hasMessageContaining("detalle");

        assertThat(appender.list).allSatisfy(evento -> assertThat(evento.getFormattedMessage())
                .doesNotContain(valorSensible));
    }

    @Test
    void valoresAnidadosYDemandadosSeRechazan() {
        assertThatThrownBy(() -> validador.validar(Map.of("referencia", Map.of("anidado", "x"))))
                .isInstanceOf(AuditoriaInvalidaException.class);
        assertThatThrownBy(() -> validador.validar(Map.of("referencia", "x".repeat(201))))
                .isInstanceOf(AuditoriaInvalidaException.class);
    }

    @Test
    void unDetalleNuloSeNormalizaAVacio() {
        assertThat(validador.validar(null)).isEmpty();
    }
}
