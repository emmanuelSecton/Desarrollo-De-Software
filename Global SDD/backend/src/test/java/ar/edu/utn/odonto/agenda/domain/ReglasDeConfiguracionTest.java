package ar.edu.utn.odonto.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Reglas del dominio de configuracion (US-001, RN-AG-02, C-05): parametros, franjas y prestaciones. */
class ReglasDeConfiguracionTest {

    private static String campo(Runnable accion) {
        try {
            accion.run();
        } catch (ConfiguracionInvalidaException e) {
            return e.campo();
        }
        throw new AssertionError("Se esperaba ConfiguracionInvalidaException");
    }

    @Test
    void losParametrosPorDefectoSonValidos() {
        Consultorio.Parametros parametros = new Consultorio.Parametros(48, 24, 24, 30);

        assertThat(parametros.confirmacionLimiteHoras()).isLessThan(parametros.recordatorioHorasAntes());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, 24, 30, recordatorioHorasAntes",
        "169, 24, 24, 30, recordatorioHorasAntes",
        "24, 24, 24, 30, confirmacionLimiteHoras",
        "24, 36, 24, 30, confirmacionLimiteHoras",
        "48, 0, 24, 30, confirmacionLimiteHoras",
        "48, 24, -1, 30, cancelacionMinimaHoras",
        "48, 24, 169, 30, cancelacionMinimaHoras",
        "48, 24, 24, 4, ofertaVencimientoMinutos",
        "48, 24, 24, 1441, ofertaVencimientoMinutos",
    })
    void losParametrosFueraDeRangoSeRechazanConElCampo(int recordatorio, int confirmacion, int cancelacion, int oferta,
            String campoEsperado) {
        assertThat(campo(() -> new Consultorio.Parametros(recordatorio, confirmacion, cancelacion, oferta)))
                .isEqualTo(campoEsperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mi-Consultorio", "mi consultorio", "ab", "mi_consultorio"})
    void unSlugInvalidoSeRechaza(String slug) {
        assertThat(campo(() -> new Consultorio.Datos("C", "P", null, null, null, null, slug))).isEqualTo("slugReserva");
    }

    @Test
    void unaFranjaValidaYLasInvalidas() {
        assertThat(FranjaDeAtencion.nueva(1, LocalTime.of(9, 0), LocalTime.of(13, 0)).diaSemana()).isEqualTo(1);

        assertThat(campo(() -> FranjaDeAtencion.nueva(0, LocalTime.of(9, 0), LocalTime.of(10, 0)))).isEqualTo("diaSemana");
        assertThat(campo(() -> FranjaDeAtencion.nueva(8, LocalTime.of(9, 0), LocalTime.of(10, 0)))).isEqualTo("diaSemana");
        assertThat(campo(() -> FranjaDeAtencion.nueva(1, LocalTime.of(13, 0), LocalTime.of(9, 0)))).isEqualTo("hasta");
        assertThat(campo(() -> FranjaDeAtencion.nueva(1, LocalTime.of(9, 0), LocalTime.of(9, 0)))).isEqualTo("hasta");
        assertThat(campo(() -> FranjaDeAtencion.nueva(1, LocalTime.of(9, 7), LocalTime.of(10, 0)))).isEqualTo("desde");
        assertThat(campo(() -> FranjaDeAtencion.nueva(1, LocalTime.of(9, 0), LocalTime.of(10, 0, 30)))).isEqualTo("hasta");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 32, 485})
    void unaDuracionInvalidaSeRechaza(int duracion) {
        assertThat(campo(() -> new Prestacion.Datos("Consulta", duracion, null, true))).isEqualTo("duracionMinutos");
    }

    @Test
    void laPrestacionRecortaElNombreRechazaPrecioNegativoYTieneBajaLogica() {
        Prestacion prestacion = Prestacion.nueva(new Prestacion.Datos("  Limpieza  ", 45, new BigDecimal("1500.00"), true));

        assertThat(prestacion.datos().nombre()).isEqualTo("Limpieza");
        assertThat(campo(() -> new Prestacion.Datos("X", 30, new BigDecimal("-1"), true))).isEqualTo("precioReferencia");
        assertThat(campo(() -> new Prestacion.Datos(" ", 30, null, true))).isEqualTo("nombre");

        prestacion.darDeBaja();
        assertThat(prestacion.activa()).isFalse();
        prestacion.reactivar();
        assertThat(prestacion.activa()).isTrue();
    }

    @Test
    void laExcepcionLlevaUnMensajeLegible() {
        assertThatThrownBy(() -> new Prestacion.Datos("X", 32, null, true))
                .hasMessageContaining("múltiplo de 5");
    }
}
