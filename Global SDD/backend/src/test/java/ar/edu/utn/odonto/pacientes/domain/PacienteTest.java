package ar.edu.utn.odonto.pacientes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Consentimientos y baja logica del paciente (RN-DP-01, RN-HC-03, D7). */
class PacienteTest {

    private static final Instant T0 = Instant.parse("2026-11-26T12:00:00Z");

    private static Paciente.DatosPersonales datos(boolean whatsapp) {
        return new Paciente.DatosPersonales("30123456", "Ana", "Pérez", LocalDate.of(1990, 5, 17),
                "+5491123456789", "ana@consultorio.test", whatsapp);
    }

    @Test
    void elAltaRegistraElConsentimientoDeDatosConFechaYVersion() {
        Paciente paciente = Paciente.nuevo(datos(false), "v1", T0);

        assertThat(paciente.consentimientoDatosAt()).isEqualTo(T0);
        assertThat(paciente.politicaPrivacidadVersion()).isEqualTo("v1");
        assertThat(paciente.whatsappConsentimientoAt()).isNull();
        assertThat(paciente.activo()).isTrue();
        assertThat(paciente.ausenciasCount()).isZero();
    }

    @Test
    void elAltaConWhatsappRegistraSuFecha() {
        assertThat(Paciente.nuevo(datos(true), "v1", T0).whatsappConsentimientoAt()).isEqualTo(T0);
    }

    @Test
    void darWhatsappDespuesFijaLaFechaDelCambioYRevocarloLaLimpia() {
        Paciente paciente = Paciente.nuevo(datos(false), "v1", T0);
        Instant despues = T0.plus(Duration.ofDays(3));

        paciente.modificar(datos(true), despues);
        assertThat(paciente.whatsappConsentimientoAt()).isEqualTo(despues);

        paciente.modificar(datos(false), despues.plusSeconds(60));
        assertThat(paciente.whatsappConsentimientoAt()).isNull();
        assertThat(paciente.consentimientoDatosAt()).isEqualTo(T0);
    }

    @Test
    void mantenerWhatsappConservaLaFechaOriginal() {
        Paciente paciente = Paciente.nuevo(datos(true), "v1", T0);

        paciente.modificar(datos(true), T0.plus(Duration.ofDays(1)));

        assertThat(paciente.whatsappConsentimientoAt()).isEqualTo(T0);
        assertThat(paciente.actualizadoAt()).isEqualTo(T0.plus(Duration.ofDays(1)));
    }

    @Test
    void laBajaYLaReactivacionSonLogicas() {
        Paciente paciente = Paciente.nuevo(datos(false), "v1", T0);

        paciente.darDeBaja(T0.plusSeconds(10));
        assertThat(paciente.activo()).isFalse();

        paciente.reactivar(T0.plusSeconds(20));
        assertThat(paciente.activo()).isTrue();
        assertThat(paciente.actualizadoAt()).isEqualTo(T0.plusSeconds(20));
    }

    @Test
    void losDatosNoSeImprimenEnLogs() {
        assertThat(datos(false).toString()).doesNotContain("30123456").doesNotContain("Pérez");
    }
}
