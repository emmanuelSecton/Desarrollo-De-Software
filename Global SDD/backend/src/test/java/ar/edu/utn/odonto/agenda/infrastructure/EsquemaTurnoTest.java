package ar.edu.utn.odonto.agenda.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.TurnosDePrueba;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tabla turno de V7 y su exclusion constraint (RN-AG-01, C-10 D1) contra PostgreSQL 16 real. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaTurnoTest {

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;
    UUID paciente;
    UUID prestacion;
    Instant dia;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(dataSource);
        paciente = TurnosDePrueba.paciente(jdbc);
        prestacion = TurnosDePrueba.limpieza(jdbc);
        dia = TurnosDePrueba.diaLibre();
    }

    private UUID insertar(JdbcTemplate destino, int desdeMin, int hastaMin, String estado, boolean sobreturno) {
        UUID id = UUID.randomUUID();
        boolean cancelado = "CANCELADO".equals(estado);
        destino.update("insert into turno (id, paciente_id, prestacion_id, inicio, fin, estado, origen, sobreturno, "
                        + "token_publico, cancelado_por, motivo_cancelacion, creado_at, actualizado_at) "
                        + "values (?, ?, ?, ?, ?, ?, 'INTERNO', ?, ?, ?, ?, ?, ?)",
                id, paciente, prestacion, ts(desdeMin), ts(hastaMin), estado, sobreturno,
                (UUID.randomUUID().toString() + "0000000").substring(0, 43), cancelado ? "CONSULTORIO" : null,
                cancelado ? "Prueba" : null, ts(0), ts(0));
        return id;
    }

    private Timestamp ts(int minutos) {
        return Timestamp.from(dia.plus(Duration.ofMinutes(minutos)));
    }

    @Test
    void dosTurnosActivosSuperpuestosSeRechazan() {
        insertar(jdbc, 0, 45, "RESERVADO", false);

        assertThatThrownBy(() -> insertar(jdbc, 30, 60, "CONFIRMADO", false))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, 0, 45, "RESERVADO", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void contiguosCanceladosYSobreturnosSeAceptan() {
        insertar(jdbc, 0, 45, "RESERVADO", false);

        insertar(jdbc, 45, 75, "RESERVADO", false);
        insertar(jdbc, 10, 40, "CANCELADO", false);
        insertar(jdbc, 15, 45, "RESERVADO", true);
        insertar(jdbc, 20, 50, "ATENDIDO", false);

        assertThat(jdbc.queryForObject("select count(*) from turno where inicio >= ? and inicio < ?", Integer.class,
                ts(0), ts(120))).isEqualTo(5);
    }

    @Test
    void lasReglasDeEstadoYCancelacionLasCuidaLaBase() {
        assertThatThrownBy(() -> jdbc.update("insert into turno (id, paciente_id, prestacion_id, inicio, fin, estado, "
                        + "origen, token_publico, creado_at, actualizado_at) values (?, ?, ?, ?, ?, 'CANCELADO', 'INTERNO', "
                        + "?, ?, ?)", UUID.randomUUID(), paciente, prestacion, ts(200), ts(230), "x".repeat(43), ts(0), ts(0)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, 300, 330, "EN_CURSO", false))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, 400, 400, "RESERVADO", false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elRolRuntimeLeeInsertaYActualizaPeroNoBorra() {
        JdbcTemplate runtime = new JdbcTemplate(ConexionRuntimeTest.dataSourceRuntime());
        UUID id = insertar(runtime, 500, 530, "RESERVADO", false);

        assertThat(runtime.update("update turno set estado = 'CONFIRMADO', version = version + 1 where id = ?", id))
                .isEqualTo(1);
        assertThatThrownBy(() -> runtime.update("delete from turno where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause().getMessage())
                        .contains("permission denied"));
    }
}
