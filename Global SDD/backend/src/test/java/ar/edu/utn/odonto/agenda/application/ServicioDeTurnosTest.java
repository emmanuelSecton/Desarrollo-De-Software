package ar.edu.utn.odonto.agenda.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.agenda.application.evento.EventosDeTurno;
import ar.edu.utn.odonto.agenda.domain.CanceladoPor;
import ar.edu.utn.odonto.agenda.domain.EstadoTurno;
import ar.edu.utn.odonto.agenda.domain.TransicionInvalidaException;
import ar.edu.utn.odonto.agenda.domain.Turno;
import ar.edu.utn.odonto.agenda.domain.TurnoInvalidoException;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.TurnosDePrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Servicio de turnos con base real (C-10): alta, superposicion decidida por la base, sobreturno, estados,
 * bloqueo optimista, reprogramacion, concurrencia, eventos AFTER_COMMIT y auditoria.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, ServicioDeTurnosTest.OyenteDePrueba.class})
class ServicioDeTurnosTest {

    private static final String ACTOR = "8b6f2c1e-0000-4000-8000-0000000000aa";

    /** Recibe los eventos de turnos solo despues del commit, como lo haran C-14, C-15 y C-18. */
    @TestConfiguration(proxyBeanMethods = false)
    static class OyenteDePrueba {

        static final List<Object> RECIBIDOS = new CopyOnWriteArrayList<>();

        @Bean
        Oyente oyenteDeTurnos() {
            return new Oyente();
        }

        static class Oyente {
            @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
            public void creado(EventosDeTurno.TurnoCreado evento) {
                RECIBIDOS.add(evento);
            }

            @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
            public void cancelado(EventosDeTurno.TurnoCancelado evento) {
                RECIBIDOS.add(evento);
            }

            @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
            public void reprogramado(EventosDeTurno.TurnoReprogramado evento) {
                RECIBIDOS.add(evento);
            }

            @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
            public void estado(EventosDeTurno.TurnoEstadoCambiado evento) {
                RECIBIDOS.add(evento);
            }
        }
    }

    @Autowired
    ServicioDeTurnos servicio;

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;
    UUID paciente;
    UUID limpieza;
    Instant dia;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(dataSource);
        paciente = TurnosDePrueba.paciente(jdbc);
        limpieza = TurnosDePrueba.limpieza(jdbc);
        dia = TurnosDePrueba.diaLibre();
    }

    private Instant a(int minutos) {
        return dia.plus(Duration.ofMinutes(minutos));
    }

    private Turno crear(int minutos, Integer duracion, boolean sobreturno) {
        return servicio.crear(paciente, limpieza, a(minutos), duracion, sobreturno, ACTOR);
    }

    private int eventosAuditoria(String accion, UUID turno) {
        return jdbc.queryForObject("select count(*) from evento_auditoria where entidad = 'TURNO' and accion = ? "
                + "and entidad_id = ?", Integer.class, accion, turno.toString());
    }

    @Test
    void unAltaTomaLaDuracionDeLaPrestacionYQuedaAuditada() {
        Turno turno = crear(0, null, false);

        assertThat(turno.fin()).isEqualTo(a(45));
        assertThat(servicio.obtener(turno.id()).estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(eventosAuditoria("CREAR_TURNO", turno.id())).isEqualTo(1);
        assertThat(OyenteDePrueba.RECIBIDOS).anyMatch(e -> e instanceof EventosDeTurno.TurnoCreado c
                && c.turnoId().equals(turno.id()));
    }

    @Test
    void unTurnoSuperpuestoDa409PeroContiguoYSobreturnoSeCrean() {
        crear(0, null, false);

        assertThatThrownBy(() -> crear(30, null, false)).isInstanceOf(TurnoSuperpuestoException.class)
                .hasMessageContaining("sobreturno");
        assertThat(crear(45, null, false).inicio()).isEqualTo(a(45));
        assertThat(crear(15, null, true).sobreturno()).isTrue();
    }

    @Test
    void unAltaFallidaNoPublicaEventos() {
        crear(0, null, false);
        int antes = OyenteDePrueba.RECIBIDOS.size();

        assertThatThrownBy(() -> crear(10, null, false)).isInstanceOf(TurnoSuperpuestoException.class);

        assertThat(OyenteDePrueba.RECIBIDOS).hasSize(antes);
    }

    @Test
    void pacienteOPrestacionNoDisponiblesDan400() {
        UUID inactivo = TurnosDePrueba.paciente(jdbc);
        jdbc.update("update paciente set activo = false where id = ?", inactivo);
        UUID prestacionInactiva = UUID.randomUUID();
        jdbc.update("insert into prestacion (id, nombre, duracion_minutos, activa) values (?, ?, 30, false)",
                prestacionInactiva, "Inactiva " + UUID.randomUUID());

        assertThat(campo(() -> servicio.crear(UUID.randomUUID(), limpieza, a(0), null, false, ACTOR))).isEqualTo("pacienteId");
        assertThat(campo(() -> servicio.crear(inactivo, limpieza, a(0), null, false, ACTOR))).isEqualTo("pacienteId");
        assertThat(campo(() -> servicio.crear(paciente, prestacionInactiva, a(0), null, false, ACTOR)))
                .isEqualTo("prestacionId");
        assertThat(campo(() -> servicio.crear(paciente, limpieza, Instant.parse("2020-01-01T13:00:00Z"), null, false,
                ACTOR))).isEqualTo("inicio");
    }

    @Test
    void confirmarCancelarYVersionVieja() {
        Turno turno = crear(0, null, false);

        Turno confirmado = servicio.confirmar(turno.id(), 0, ACTOR);
        assertThat(confirmado.estado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(confirmado.version()).isEqualTo(1);

        assertThatThrownBy(() -> servicio.cancelar(turno.id(), 0, CanceladoPor.PACIENTE, "Avisó", ACTOR))
                .isInstanceOf(TurnoModificadoException.class);
        assertThat(servicio.obtener(turno.id()).estado()).isEqualTo(EstadoTurno.CONFIRMADO);

        Turno cancelado = servicio.cancelar(turno.id(), 1, CanceladoPor.PACIENTE, "Avisó por teléfono", ACTOR);
        assertThat(cancelado.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThatThrownBy(() -> servicio.confirmar(turno.id(), 2, ACTOR)).isInstanceOf(TransicionInvalidaException.class);

        assertThat(jdbc.queryForObject("select detalle::text from evento_auditoria where accion = 'ANULAR_ENTIDAD' "
                + "and entidad_id = ?", String.class, turno.id().toString())).doesNotContain("teléfono");
        assertThat(crear(0, null, false).inicio()).as("el horario cancelado queda libre").isEqualTo(a(0));
    }

    @Test
    void reprogramar15MinutosSobreElMismoTurno() {
        Turno original = crear(0, null, false);

        Turno nuevo = servicio.reprogramar(original.id(), 0, a(15), null, ACTOR);

        assertThat(nuevo.inicio()).isEqualTo(a(15));
        assertThat(nuevo.fin()).isEqualTo(a(60));
        assertThat(nuevo.reprogramadoDe()).isEqualTo(original.id());
        Turno viejo = servicio.obtener(original.id());
        assertThat(viejo.estado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(viejo.motivoCancelacion()).isEqualTo("Reprogramado");
        assertThat(OyenteDePrueba.RECIBIDOS).anyMatch(e -> e instanceof EventosDeTurno.TurnoReprogramado r
                && r.originalId().equals(original.id()) && r.nuevoId().equals(nuevo.id()));
        assertThat(jdbc.queryForObject("select count(*) from turno where reprogramado_de = ? or id = ? "
                + "and estado in ('RESERVADO','CONFIRMADO')", Integer.class, original.id(), original.id())).isEqualTo(1);
    }

    @Test
    void reprogramarAUnHorarioOcupadoDejaElOriginalIntacto() {
        Turno original = crear(0, null, false);
        crear(120, null, false);

        assertThatThrownBy(() -> servicio.reprogramar(original.id(), 0, a(100), null, ACTOR))
                .isInstanceOf(TurnoSuperpuestoException.class);

        Turno igual = servicio.obtener(original.id());
        assertThat(igual.estado()).isEqualTo(EstadoTurno.RESERVADO);
        assertThat(igual.version()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from turno where reprogramado_de = ?", Integer.class,
                original.id())).isZero();
    }

    @Test
    void listarDevuelveElDiaOrdenadoConPacienteYPrestacion() {
        Turno segundo = crear(60, null, false);
        Turno primero = crear(0, null, false);

        List<TurnoListado> listado = servicio.listar(a(-60), a(600));

        assertThat(listado).extracting(TurnoListado::id).containsExactly(primero.id(), segundo.id());
        assertThat(listado.get(0).prestacionNombre()).isEqualTo("Limpieza");
        assertThat(listado.get(0).pacienteNombre()).isEqualTo("Ana");
        assertThatThrownBy(() -> servicio.listar(a(0), a(0).plus(Duration.ofDays(32))))
                .isInstanceOf(TurnoInvalidoException.class);
    }

    @Test
    void dosAltasSimultaneasSuperpuestasDejanUnSoloTurno() throws Exception {
        CountDownLatch largada = new CountDownLatch(1);
        Callable<UUID> tarea = () -> {
            largada.await();
            return crear(0, null, false).id();
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<UUID> uno = hilos.submit(tarea);
            Future<UUID> dos = hilos.submit(tarea);
            largada.countDown();

            int creados = 0;
            int rechazados = 0;
            for (Future<UUID> resultado : List.of(uno, dos)) {
                try {
                    resultado.get(30, TimeUnit.SECONDS);
                    creados++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(TurnoSuperpuestoException.class);
                    rechazados++;
                }
            }
            assertThat(creados).isEqualTo(1);
            assertThat(rechazados).isEqualTo(1);
        } finally {
            hilos.shutdownNow();
        }
    }

    private static String campo(Runnable accion) {
        try {
            accion.run();
        } catch (TurnoInvalidoException e) {
            return e.campo();
        }
        throw new AssertionError("Se esperaba TurnoInvalidoException");
    }
}
