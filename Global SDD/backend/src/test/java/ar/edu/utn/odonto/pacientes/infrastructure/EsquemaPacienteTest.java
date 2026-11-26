package ar.edu.utn.odonto.pacientes.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PacientesDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Tabla paciente de V4 (C-09, D1) contra PostgreSQL 16 real. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaPacienteTest {

    private static final Timestamp AHORA = Timestamp.from(Instant.parse("2026-11-26T12:00:00Z"));

    @Autowired
    DataSource dataSource;

    @Test
    void lasColumnasTienenLosTiposEsperadosYLasFechasNoTienenDefault() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> columnas = jdbc.queryForList("select column_name, udt_name, column_default "
                + "from information_schema.columns where table_name = 'paciente'");
        Map<String, String> tipos = columnas.stream()
                .collect(Collectors.toMap(f -> (String) f.get("column_name"), f -> (String) f.get("udt_name")));

        assertThat(tipos)
                .containsEntry("id", "uuid")
                .containsEntry("dni", "varchar")
                .containsEntry("fecha_nacimiento", "date")
                .containsEntry("telefono_e164", "varchar")
                .containsEntry("email", "citext")
                .containsEntry("consentimiento_datos_at", "timestamptz")
                .containsEntry("whatsapp_consentimiento_at", "timestamptz")
                .containsEntry("ausencias_count", "int4")
                .containsEntry("creado_at", "timestamptz")
                .containsEntry("actualizado_at", "timestamptz");
        assertThat(columnas)
                .filteredOn(f -> ((String) f.get("udt_name")).equals("timestamptz"))
                .allSatisfy(f -> assertThat(f.get("column_default")).isNull());
    }

    @Test
    void existenLosIndicesYLaExtensionUnaccent() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(jdbc.queryForList("select indexname from pg_indexes where tablename = 'paciente'", String.class))
                .contains("uq_paciente_dni", "idx_paciente_apellido_nombre", "idx_paciente_telefono");
        assertThat(jdbc.queryForObject("select unaccent('Pérez Núñez')", String.class)).isEqualTo("Perez Nunez");
    }

    @Test
    void unDniRepetidoSeRechaza() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String dni = PacientesDePrueba.dniUnico();
        insertar(jdbc, dni, PacientesDePrueba.celularUnico(), 0, false, null);

        assertThatThrownBy(() -> insertar(jdbc, dni, PacientesDePrueba.celularUnico(), 0, false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void losChecksRechazanDatosMalFormados() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String celular = PacientesDePrueba.celularUnico();

        assertThatThrownBy(() -> insertar(jdbc, "30A23456", celular, 0, false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, "123456", celular, 0, false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, PacientesDePrueba.dniUnico(), "1123456789", 0, false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, PacientesDePrueba.dniUnico(), celular, -1, false, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, PacientesDePrueba.dniUnico(), celular, 0, true, null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar(jdbc, PacientesDePrueba.dniUnico(), celular, 0, false, AHORA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elRolRuntimeLeeInsertaYActualizaPeroNoBorra() {
        JdbcTemplate runtime = new JdbcTemplate(ConexionRuntimeTest.dataSourceRuntime());
        UUID id = insertar(runtime, PacientesDePrueba.dniUnico(), PacientesDePrueba.celularUnico(), 0, false, null);

        assertThat(runtime.update("update paciente set activo = false where id = ?", id)).isEqualTo(1);
        assertThat(runtime.queryForObject("select activo from paciente where id = ?", Boolean.class, id)).isFalse();
        assertThatThrownBy(() -> runtime.update("delete from paciente where id = ?", id))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(((DataAccessException) e).getMostSpecificCause().getMessage())
                        .contains("permission denied"));
    }

    private static UUID insertar(
            JdbcTemplate jdbc, String dni, String telefono, int ausencias, boolean whatsapp, Timestamp whatsappAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into paciente (id, dni, nombre, apellido, telefono_e164, consentimiento_datos_at, "
                        + "politica_privacidad_version, acepta_whatsapp, whatsapp_consentimiento_at, ausencias_count, "
                        + "creado_at, actualizado_at) values (?, ?, 'Ana', 'Prueba', ?, ?, 'v1', ?, ?, ?, ?, ?)",
                id, dni, telefono, AHORA, whatsapp, whatsappAt, ausencias, AHORA, AHORA);
        return id;
    }
}
