package ar.edu.utn.odonto.identidad.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.RelojDePrueba;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Login contra base real: contadores, bloqueo y auditoria (RN-SEG-03, D5, D11). Cada rechazo confirma
 * la transaccion, asi que el contador y el evento LOGIN_FALLIDO persisten juntos.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, RelojDePrueba.class})
class ServicioDeLoginTest {

    private static final String IP = "200.24.18.7";

    @Autowired
    ServicioDeLogin servicio;

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
    void unLoginCorrectoAuditaLoginYReiniciaElContador() {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(jdbc);
        servicio.autenticar(secretaria.email(), "incorrecta-1", IP);

        ResultadoLogin resultado = servicio.autenticar(secretaria.email().toUpperCase(), secretaria.clave(), IP);

        assertThat(resultado).isInstanceOf(ResultadoLogin.Exitoso.class);
        UsuarioAutenticado usuario = ((ResultadoLogin.Exitoso) resultado).usuario();
        assertThat(usuario.id()).isEqualTo(secretaria.id());
        Map<String, Object> fila = fila(secretaria);
        assertThat(fila.get("intentos_fallidos")).isEqualTo(0);
        assertThat(((Timestamp) fila.get("ultimo_login_at")).toInstant()).isEqualTo(RelojDePrueba.INICIO);
        assertThat(eventos(secretaria.id().toString(), "LOGIN")).singleElement().satisfies(evento -> {
            assertThat(evento.get("actor_tipo")).isEqualTo("USUARIO");
            assertThat(evento.get("actor_id")).isEqualTo(secretaria.id().toString());
            assertThat(evento.get("ip").toString()).isEqualTo(IP);
        });
    }

    @Test
    void unaClaveIncorrectaCuentaElFalloYAuditaLoginFallido() {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(jdbc);

        ResultadoLogin resultado = servicio.autenticar(secretaria.email(), "incorrecta-1", IP);

        assertThat(resultado).isEqualTo(new ResultadoLogin.Rechazado(MotivoRechazo.CREDENCIALES));
        assertThat(fila(secretaria).get("intentos_fallidos")).isEqualTo(1);
        assertThat(eventos(secretaria.id().toString(), "LOGIN_FALLIDO")).singleElement().satisfies(evento -> {
            assertThat(evento.get("actor_tipo")).isEqualTo("SISTEMA");
            assertThat(evento.get("actor_id")).isNull();
            assertThat(evento.get("detalle").toString()).contains("CREDENCIALES");
        });
    }

    @Test
    void unEmailInexistenteAuditaConIdDesconocidoYSinElEmail() {
        String email = UsuariosDePrueba.emailUnico("fantasma");
        Integer antes = cantidad("desconocido");

        ResultadoLogin resultado = servicio.autenticar(email, "cualquier-clave", IP);

        assertThat(resultado).isEqualTo(new ResultadoLogin.Rechazado(MotivoRechazo.CREDENCIALES));
        assertThat(cantidad("desconocido")).isEqualTo(antes + 1);
        assertThat(jdbc.queryForObject("select count(*) from evento_auditoria where entidad_id like '%@%' "
                        + "or actor_id like '%@%' or detalle::text like '%@%' or detalle::text like ?",
                Integer.class, "%" + email + "%"))
                .isZero();
    }

    @Test
    void unUsuarioInactivoSeRechazaAunConLaClaveCorrecta() {
        UsuariosDePrueba.Creado inactivo = UsuariosDePrueba.crear(jdbc, "SECRETARIA", false, false);

        assertThat(servicio.autenticar(inactivo.email(), inactivo.clave(), IP))
                .isEqualTo(new ResultadoLogin.Rechazado(MotivoRechazo.INACTIVA));
        assertThat(eventos(inactivo.id().toString(), "LOGIN_FALLIDO")).singleElement()
                .satisfies(evento -> assertThat(evento.get("detalle").toString()).contains("INACTIVA"));
    }

    @Test
    void alQuintoFalloLaCuentaSeBloqueaYRechazaLaClaveCorrecta() {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(jdbc);
        for (int i = 0; i < 5; i++) {
            servicio.autenticar(secretaria.email(), "incorrecta-" + i, IP);
            reloj.adelantar(Duration.ofSeconds(30));
        }

        reloj.adelantar(Duration.ofMinutes(10));
        assertThat(servicio.autenticar(secretaria.email(), secretaria.clave(), IP))
                .isEqualTo(new ResultadoLogin.Rechazado(MotivoRechazo.BLOQUEADA));
        assertThat(eventos(secretaria.id().toString(), "LOGIN_FALLIDO")).hasSize(6);

        reloj.adelantar(Duration.ofMinutes(5));
        assertThat(servicio.autenticar(secretaria.email(), secretaria.clave(), IP))
                .isInstanceOf(ResultadoLogin.Exitoso.class);
        assertThat(fila(secretaria).get("bloqueado_hasta")).isNull();
    }

    private Map<String, Object> fila(UsuariosDePrueba.Creado usuario) {
        return jdbc.queryForMap("select * from usuario where id = ?", usuario.id());
    }

    private List<Map<String, Object>> eventos(String entidadId, String accion) {
        return jdbc.queryForList(
                "select * from evento_auditoria where entidad = 'USUARIO' and entidad_id = ? and accion = ?",
                entidadId, accion);
    }

    private Integer cantidad(String entidadId) {
        return jdbc.queryForObject(
                "select count(*) from evento_auditoria where accion = 'LOGIN_FALLIDO' and entidad_id = ?",
                Integer.class, entidadId);
    }
}
