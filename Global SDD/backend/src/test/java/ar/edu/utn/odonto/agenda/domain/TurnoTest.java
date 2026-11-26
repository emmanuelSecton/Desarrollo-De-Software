package ar.edu.utn.odonto.agenda.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/** Reglas del turno: duracion, inicio, estados (RN-TU-01, RN-AG-02) y token (C-10). */
class TurnoTest {

    private static final Instant AHORA = Instant.parse("2026-11-26T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-12-01T13:00:00Z");

    private static Turno turno(Integer duracion) {
        return Turno.nuevo(UUID.randomUUID(), UUID.randomUUID(), 45, duracion, INICIO, false, OrigenTurno.INTERNO, null,
                AHORA);
    }

    private static String campo(Runnable accion) {
        try {
            accion.run();
        } catch (TurnoInvalidoException e) {
            return e.campo();
        }
        throw new AssertionError("Se esperaba TurnoInvalidoException");
    }

    @Test
    void unTurnoNuevoTieneLaDuracionDeLaPrestacionYQuedaReservado() {
        Turno turno = turno(null);

        assertThat(turno.fin()).isEqualTo(INICIO.plus(Duration.ofMinutes(45)));
        assertThat(turno.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(turno.version()).isZero();
        assertThat(turno.duracionMinutos()).isEqualTo(45);
    }

    @Test
    void laDuracionSePuedeExtenderEnBloquesDe5() {
        assertThat(turno(60).duracionMinutos()).isEqualTo(60);
        assertThat(campo(() -> turno(40))).isEqualTo("duracionMinutos");
        assertThat(campo(() -> turno(62))).isEqualTo("duracionMinutos");
        assertThat(campo(() -> turno(485))).isEqualTo("duracionMinutos");
    }

    @Test
    void elInicioEsFuturoYEnMultiplosDe5() {
        assertThat(campo(() -> Turno.nuevo(UUID.randomUUID(), UUID.randomUUID(), 45, null, AHORA.minusSeconds(60),
                false, OrigenTurno.INTERNO, null, AHORA))).isEqualTo("inicio");
        assertThat(campo(() -> Turno.nuevo(UUID.randomUUID(), UUID.randomUUID(), 45, null,
                INICIO.plus(Duration.ofMinutes(7)), false, OrigenTurno.INTERNO, null, AHORA))).isEqualTo("inicio");
        assertThat(campo(() -> Turno.nuevo(UUID.randomUUID(), UUID.randomUUID(), 45, null, INICIO.plusSeconds(30),
                false, OrigenTurno.INTERNO, null, AHORA))).isEqualTo("inicio");
    }

    @ParameterizedTest
    @CsvSource({
        "RESERVADO, CONFIRMADO, true", "RESERVADO, CANCELADO, true", "RESERVADO, ATENDIDO, true",
        "RESERVADO, AUSENTE, true", "CONFIRMADO, CANCELADO, true", "CONFIRMADO, ATENDIDO, true",
        "CONFIRMADO, AUSENTE, true", "CONFIRMADO, RESERVADO, false", "CONFIRMADO, CONFIRMADO, false",
        "RESERVADO, RESERVADO, false",
    })
    void lasTransicionesSiguenRnTu01(EstadoTurno desde, EstadoTurno hacia, boolean valida) {
        assertThat(desde.puedePasarA(hacia)).isEqualTo(valida);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ATENDIDO", "AUSENTE"})
    void losEstadosFinalesNoCambian(EstadoTurno finalizado) {
        for (EstadoTurno destino : EstadoTurno.values()) {
            assertThat(finalizado.puedePasarA(destino)).isFalse();
        }
        assertThat(finalizado.activo()).isFalse();
    }

    @Test
    void confirmarYCancelarUnTurnoCanceladoFalla() {
        Turno turno = turno(null);
        turno.confirmar(AHORA);
        assertThat(turno.estado()).isEqualTo(EstadoTurno.CONFIRMADO);

        turno.cancelar(CanceladoPor.PACIENTE, "  Avisó por teléfono  ", AHORA);
        assertThat(turno.motivoCancelacion()).isEqualTo("Avisó por teléfono");
        assertThat(turno.canceladoPor()).isEqualTo(CanceladoPor.PACIENTE);

        assertThatThrownBy(() -> turno.confirmar(AHORA)).isInstanceOf(TransicionInvalidaException.class);
        assertThatThrownBy(() -> turno.cancelar(CanceladoPor.CONSULTORIO, "Otra vez", AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void cancelarSinMotivoFalla() {
        assertThat(campo(() -> turno(null).cancelar(CanceladoPor.CONSULTORIO, " ", AHORA))).isEqualTo("motivo");
    }

    @Test
    void reprogramarCancelaElOriginalYCreaUnoVinculadoConLaMismaDuracion() {
        Turno original = turno(60);

        Turno nuevo = original.reprogramar(INICIO.plus(Duration.ofMinutes(15)), null, 45, AHORA);

        assertThat(original.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(original.canceladoPor()).isEqualTo(CanceladoPor.CONSULTORIO);
        assertThat(original.motivoCancelacion()).isEqualTo("Reprogramado");
        assertThat(nuevo.reprogramadoDe()).isEqualTo(original.id());
        assertThat(nuevo.pacienteId()).isEqualTo(original.pacienteId());
        assertThat(nuevo.duracionMinutos()).isEqualTo(60);
        assertThat(nuevo.estado()).isEqualTo(EstadoTurno.RESERVADO);
    }

    @Test
    void elTokenTiene43CaracteresUrlSafeYNoSeRepite() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String token = turno(null).tokenPublico();
            assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(500);
    }
}
