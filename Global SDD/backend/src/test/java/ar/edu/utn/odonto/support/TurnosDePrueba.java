package ar.edu.utn.odonto.support;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos de prueba para turnos (C-10). Cada test pide un dia futuro al azar para no chocar con los turnos
 * de otros tests en la base compartida: la exclusion constraint abarca a todos.
 */
public final class TurnosDePrueba {

    private static final Instant BASE = Instant.parse("2030-01-01T13:00:00Z");

    private TurnosDePrueba() {}

    /** Secuencia por ejecucion: cada llamada devuelve un dia distinto, asi dos tests nunca comparten agenda. */
    private static final AtomicInteger SIGUIENTE = new AtomicInteger();

    /** Un dia distinto en cada llamada, desde 2030, a las 13:00 UTC (10:00 en Buenos Aires). */
    public static Instant diaLibre() {
        return BASE.plus(Duration.ofDays(SIGUIENTE.getAndIncrement()));
    }

    /** Crea un paciente activo directamente en la base y devuelve su id. */
    public static UUID paciente(JdbcTemplate jdbc) {
        UUID id = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.parse("2026-11-26T12:00:00Z"));
        jdbc.update("insert into paciente (id, dni, nombre, apellido, telefono_e164, consentimiento_datos_at, "
                        + "politica_privacidad_version, creado_at, actualizado_at) values (?, ?, 'Ana', ?, ?, ?, 'v1', ?, ?)",
                id, PacientesDePrueba.dniUnico(), "Turno " + PacientesDePrueba.sufijo(), PacientesDePrueba.celularUnico(),
                ahora, ahora, ahora);
        return id;
    }

    /** Id de la prestacion inicial "Limpieza" (45 minutos). */
    public static UUID limpieza(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select id from prestacion where nombre = 'Limpieza' and activa", UUID.class);
    }
}
