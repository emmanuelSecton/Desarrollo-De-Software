package ar.edu.utn.odonto.pacientes.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.support.PacientesDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.RelojDePrueba;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Registro de pacientes con base real: auditoria, consentimiento y obtener-o-crear (D6, D7, D9). */
@SpringBootTest(properties = "pacientes.politica-privacidad-version=v-test")
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, RelojDePrueba.class})
class RegistroDePacientesTest {

    private static final Actor SECRETARIA = Actor.usuario("8b6f2c1e-0000-4000-8000-000000000001");

    @Autowired
    RegistroDePacientes registro;

    @Autowired
    RelojDePrueba.RelojMutable reloj;

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(dataSource);
        reloj.fijar(RelojDePrueba.INICIO);
    }

    @Test
    void elAltaNormalizaRegistraElConsentimientoYAuditaSinDatosPersonales() {
        DatosDePaciente datos = new DatosDePaciente("30.123." + PacientesDePrueba.dniUnico().substring(0, 3),
                " Ana ", "Pérez", null, "011 15-3" + PacientesDePrueba.dniUnico().substring(0, 3) + "-1234",
                "Ana.Perez@Consultorio.test", true);

        PacienteVista creado = registro.alta(datos, SECRETARIA);

        assertThat(creado.dni()).matches("30123[0-9]{3}");
        assertThat(creado.nombre()).isEqualTo("Ana");
        assertThat(creado.telefono()).startsWith("+54911");
        assertThat(creado.consentimientoDatosAt()).isEqualTo(RelojDePrueba.INICIO);
        Map<String, Object> fila = jdbc.queryForMap(
                "select politica_privacidad_version, whatsapp_consentimiento_at from paciente where id = ?", creado.id());
        assertThat(fila.get("politica_privacidad_version")).isEqualTo("v-test");
        assertThat(fila.get("whatsapp_consentimiento_at")).isNotNull();

        assertThat(eventos(creado.id(), "CREAR_ENTIDAD")).singleElement().satisfies(evento -> {
            assertThat(evento.get("actor_tipo")).isEqualTo("USUARIO");
            assertThat(evento.get("actor_id")).isEqualTo(SECRETARIA.id());
            assertThat(evento.get("entidad")).isEqualTo("PACIENTE");
            assertThat(evento.toString())
                    .doesNotContain(creado.dni())
                    .doesNotContain("Pérez")
                    .doesNotContain(creado.telefono())
                    .doesNotContain("Consultorio.test");
        });
    }

    @Test
    void laFechaDevueltaEnElAltaEsLaMismaQueQuedaGuardada() {
        reloj.fijar(java.time.Instant.parse("2026-11-26T12:00:00.123456789Z"));

        PacienteVista creado = registro.alta(PacientesDePrueba.datos(), SECRETARIA);

        assertThat(creado.consentimientoDatosAt()).isEqualTo(java.time.Instant.parse("2026-11-26T12:00:00.123456Z"));
        assertThat(registro.obtener(creado.id()).consentimientoDatosAt()).isEqualTo(creado.consentimientoDatosAt());
    }

    @Test
    void unDniDuplicadoDevuelveElIdExistenteYNoAudita() {
        PacienteVista existente = registro.alta(PacientesDePrueba.datos(), SECRETARIA);
        DatosDePaciente repetido = new DatosDePaciente(existente.dni(), "Otro", "Nombre", null,
                PacientesDePrueba.celularUnico(), null, false);

        assertThatThrownBy(() -> registro.alta(repetido, SECRETARIA))
                .isInstanceOf(PacienteDuplicadoException.class)
                .satisfies(e -> assertThat(((PacienteDuplicadoException) e).existente()).isEqualTo(existente.id()));
        assertThat(eventos(existente.id(), "CREAR_ENTIDAD")).hasSize(1);
        assertThat(registro.obtener(existente.id()).nombre()).isEqualTo("Ana");
    }

    @Test
    void dniOTelefonoInvalidosSeRechazanConElCampo() {
        DatosDePaciente dniMalo = new DatosDePaciente("12AB", "Ana", "Pérez", null,
                PacientesDePrueba.celularUnico(), null, false);
        DatosDePaciente telefonoMalo = new DatosDePaciente(PacientesDePrueba.dniUnico(), "Ana", "Pérez", null,
                "12345", null, false);

        assertThatThrownBy(() -> registro.alta(dniMalo, SECRETARIA))
                .isInstanceOf(DatoDePacienteInvalidoException.class)
                .satisfies(e -> assertThat(((DatoDePacienteInvalidoException) e).campo()).isEqualTo("dni"));
        assertThatThrownBy(() -> registro.alta(telefonoMalo, SECRETARIA))
                .isInstanceOf(DatoDePacienteInvalidoException.class)
                .satisfies(e -> assertThat(((DatoDePacienteInvalidoException) e).campo()).isEqualTo("telefono"));
    }

    @Test
    void laModificacionConservaConsentimientoYAusenciasYAudita() {
        PacienteVista creado = registro.alta(PacientesDePrueba.datos(), SECRETARIA);
        jdbc.update("update paciente set ausencias_count = 2 where id = ?", creado.id());
        reloj.adelantar(Duration.ofDays(5));
        String nuevoCelular = PacientesDePrueba.celularUnico();

        PacienteVista modificado = registro.modificar(creado.id(), new DatosDePaciente(creado.dni(), "Ana María",
                creado.apellido(), null, nuevoCelular, null, true), SECRETARIA);

        assertThat(modificado.nombre()).isEqualTo("Ana María");
        assertThat(modificado.telefono()).isEqualTo(nuevoCelular);
        assertThat(modificado.email()).isNull();
        assertThat(modificado.ausenciasCount()).isEqualTo(2);
        assertThat(modificado.consentimientoDatosAt()).isEqualTo(RelojDePrueba.INICIO);
        assertThat(eventos(creado.id(), "MODIFICAR_ENTIDAD")).hasSize(1);
    }

    @Test
    void cambiarElDniPorUnoExistenteDa409SinCambios() {
        PacienteVista uno = registro.alta(PacientesDePrueba.datos(), SECRETARIA);
        PacienteVista otro = registro.alta(PacientesDePrueba.datos(), SECRETARIA);

        assertThatThrownBy(() -> registro.modificar(otro.id(), new DatosDePaciente(uno.dni(), otro.nombre(),
                        otro.apellido(), null, otro.telefono(), null, false), SECRETARIA))
                .isInstanceOf(PacienteDuplicadoException.class);
        assertThat(registro.obtener(otro.id()).dni()).isEqualTo(otro.dni());
    }

    @Test
    void laBajaYLaReactivacionSeAuditanYNuncaBorran() {
        PacienteVista creado = registro.alta(PacientesDePrueba.datos(), SECRETARIA);
        Actor odontologo = Actor.usuario(UUID.randomUUID().toString());

        registro.darDeBaja(creado.id(), odontologo);
        assertThat(registro.obtener(creado.id()).activo()).isFalse();
        registro.reactivar(creado.id(), odontologo);

        assertThat(registro.obtener(creado.id()).activo()).isTrue();
        assertThat(eventos(creado.id(), "ANULAR_ENTIDAD")).hasSize(1);
        assertThat(eventos(creado.id(), "MODIFICAR_ENTIDAD")).singleElement()
                .satisfies(e -> assertThat(e.get("detalle").toString()).contains("reactivacion"));
    }

    @Test
    void unIdInexistenteDa404() {
        assertThatThrownBy(() -> registro.obtener(UUID.randomUUID())).isInstanceOf(PacienteInexistenteException.class);
        assertThatThrownBy(() -> registro.darDeBaja(UUID.randomUUID(), SECRETARIA))
                .isInstanceOf(PacienteInexistenteException.class);
    }

    @Test
    void obtenerOCrearDevuelveElExistenteSinCambiarSusDatos() {
        PacienteVista existente = registro.alta(PacientesDePrueba.datos(), SECRETARIA);
        DatosDePaciente otrosDatos = new DatosDePaciente(existente.dni(), "Impostor", "X", null,
                PacientesDePrueba.celularUnico(), null, true);

        PacienteVista obtenido = registro.obtenerOCrear(otrosDatos, new Actor(TipoActor.SISTEMA, null));

        assertThat(obtenido.id()).isEqualTo(existente.id());
        assertThat(obtenido.telefono()).isEqualTo(existente.telefono());
        assertThat(registro.obtener(existente.id()).nombre()).isEqualTo("Ana");
        assertThat(eventos(existente.id(), "CREAR_ENTIDAD")).hasSize(1);
    }

    @Test
    void obtenerOCrearCreaUnPacienteNuevoConUnSoloEvento() {
        PacienteVista creado = registro.obtenerOCrear(PacientesDePrueba.datos(), new Actor(TipoActor.SISTEMA, null));

        assertThat(eventos(creado.id(), "CREAR_ENTIDAD")).singleElement()
                .satisfies(e -> assertThat(e.get("actor_tipo")).isEqualTo("SISTEMA"));
    }

    @Test
    void dosObtenerOCrearSimultaneosDejanUnSoloPaciente() throws Exception {
        DatosDePaciente datos = PacientesDePrueba.datos();
        CountDownLatch largada = new CountDownLatch(1);
        Callable<UUID> tarea = () -> {
            largada.await();
            return registro.obtenerOCrear(datos, new Actor(TipoActor.SISTEMA, null)).id();
        };
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<UUID> primero = hilos.submit(tarea);
            Future<UUID> segundo = hilos.submit(tarea);
            largada.countDown();

            UUID idPrimero = primero.get(30, TimeUnit.SECONDS);
            UUID idSegundo = segundo.get(30, TimeUnit.SECONDS);

            assertThat(idPrimero).isEqualTo(idSegundo);
            assertThat(jdbc.queryForObject("select count(*) from paciente where dni = ?", Integer.class, datos.dni()))
                    .isEqualTo(1);
            assertThat(eventos(idPrimero, "CREAR_ENTIDAD")).hasSize(1);
        } finally {
            hilos.shutdownNow();
        }
    }

    private List<Map<String, Object>> eventos(UUID id, String accion) {
        return jdbc.queryForList(
                "select * from evento_auditoria where entidad = 'PACIENTE' and entidad_id = ? and accion = ?",
                id.toString(), accion);
    }
}
