package ar.edu.utn.odonto.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tablas y datos iniciales de V6 (C-05, D3-D6) contra PostgreSQL 16 real. Usa el sabado (6) para no tocar el seed. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaConfiguracionTest {

    private static final int SABADO = 6;

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("delete from horario_atencion where dia_semana = ?", SABADO);
    }

    private void franja(int dia, String desde, String hasta) {
        jdbc.update("insert into horario_atencion (dia_semana, hora_desde, hora_hasta) values (?, ?, ?)",
                dia, Time.valueOf(LocalTime.parse(desde)), Time.valueOf(LocalTime.parse(hasta)));
    }

    private void prestacion(String nombre, int duracion, boolean activa) {
        jdbc.update("insert into prestacion (nombre, duracion_minutos, activa) values (?, ?, ?)", nombre, duracion, activa);
    }

    private static String nombreUnico() {
        return "Prestación " + UUID.randomUUID();
    }

    @Test
    void losDatosInicialesEstanCargados() {
        Map<String, Object> consultorio = jdbc.queryForMap("select * from consultorio");
        assertThat(consultorio)
                .containsEntry("recordatorio_horas_antes", 48)
                .containsEntry("confirmacion_limite_horas", 24)
                .containsEntry("cancelacion_minima_horas", 24)
                .containsEntry("oferta_vencimiento_minutos", 30);
        assertThat(jdbc.queryForObject("select count(*) from consultorio", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("select distinct dia_semana from horario_atencion where dia_semana <= 5 "
                + "order by dia_semana", Integer.class)).containsExactly(1, 2, 3, 4, 5);
        assertThat(jdbc.queryForObject("select count(*) from horario_atencion where dia_semana <= 5 "
                + "and ((hora_desde = '09:00' and hora_hasta = '13:00') or (hora_desde = '15:00' and hora_hasta = '19:00'))",
                Integer.class)).isEqualTo(10);
        assertThat(jdbc.queryForList("select nombre || ' ' || duracion_minutos from prestacion where nombre in "
                        + "('Consulta/diagnóstico','Limpieza','Obturación','Endodoncia','Extracción','Control')",
                String.class))
                .containsExactlyInAnyOrder("Consulta/diagnóstico 30", "Limpieza 45", "Obturación 45", "Endodoncia 90",
                        "Extracción 45", "Control 15");
    }

    @Test
    void elConsultorioEsUnicoYSusParametrosCoherentes() {
        assertThatThrownBy(() -> jdbc.update("insert into consultorio (nombre, profesional_nombre, slug_reserva, "
                + "actualizado_at) values ('Otro', 'Otro', 'otro-consultorio', now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
        for (String cambio : List.of("confirmacion_limite_horas = 48", "confirmacion_limite_horas = 0",
                "recordatorio_horas_antes = 200", "cancelacion_minima_horas = -1", "oferta_vencimiento_minutos = 2",
                "slug_reserva = 'Mi Consultorio'")) {
            assertThatThrownBy(() -> jdbc.update("update consultorio set " + cambio))
                    .as(cambio)
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    void lasFranjasDelMismoDiaNoSeSuperponen() {
        franja(SABADO, "09:00", "13:00");
        franja(SABADO, "13:00", "14:00");

        assertThatThrownBy(() -> franja(SABADO, "12:00", "14:30")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> franja(SABADO, "10:00", "11:00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasFranjasInvalidasSeRechazan() {
        assertThatThrownBy(() -> franja(SABADO, "13:00", "09:00")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> franja(SABADO, "09:07", "10:00")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> franja(8, "09:00", "10:00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void lasPrestacionesValidanDuracionYNombreUnicoEntreActivas() {
        String nombre = nombreUnico();
        prestacion(nombre, 30, true);

        assertThatThrownBy(() -> prestacion(nombre.toUpperCase(), 30, true))
                .isInstanceOf(DataIntegrityViolationException.class);
        prestacion(nombre, 30, false);
        assertThatThrownBy(() -> prestacion(nombreUnico(), 0, true)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> prestacion(nombreUnico(), 32, true)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> prestacion(nombreUnico(), 485, true)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elRolRuntimeTieneSoloLosPermisosNecesarios() {
        JdbcTemplate runtime = new JdbcTemplate(ConexionRuntimeTest.dataSourceRuntime());

        assertThat(runtime.update("update consultorio set telefono = telefono")).isEqualTo(1);
        sinPermiso(() -> runtime.update("insert into consultorio (nombre, profesional_nombre, slug_reserva, "
                + "actualizado_at) values ('X', 'X', 'xx-x', now())"));
        sinPermiso(() -> runtime.update("delete from consultorio"));
        sinPermiso(() -> runtime.update("delete from prestacion where nombre = 'no-existe'"));

        runtime.update("insert into horario_atencion (dia_semana, hora_desde, hora_hasta) values (?, '08:00', '09:00')",
                SABADO);
        assertThat(runtime.update("delete from horario_atencion where dia_semana = ?", SABADO)).isEqualTo(1);
    }

    private static void sinPermiso(Runnable accion) {
        assertThatThrownBy(accion::run)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause().getMessage())
                        .contains("permission denied"));
    }
}
