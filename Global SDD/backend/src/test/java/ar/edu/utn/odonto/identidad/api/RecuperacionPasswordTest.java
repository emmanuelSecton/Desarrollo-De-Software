package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.identidad.application.GeneradorDeTokens;
import ar.edu.utn.odonto.notificaciones.application.MensajeEmail;
import ar.edu.utn.odonto.support.MensajeriaDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.RelojDePrueba;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Recuperacion de contraseña de punta a punta por HTTP (US-003, C-04): pedido, email, restablecimiento. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, RelojDePrueba.class, MensajeriaDePrueba.class})
class RecuperacionPasswordTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);
    private static final String NUEVA = "una-clave-nueva-segura";

    @Autowired
    MockMvc mvc;

    @Autowired
    MensajeriaDePrueba.Capturados correo;

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

    private ResultActions pedir(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/password/recover").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions restablecer(String token, String nueva) throws Exception {
        return mvc.perform(post("/api/v1/auth/password/reset").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"nueva\":\"" + nueva + "\"}"));
    }

    private String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString();
    }

    private MensajeEmail emailPara(String destino) throws Exception {
        return correo.esperar(destino, ESPERA).orElseThrow(() -> new AssertionError("No llegó el email"));
    }

    @Test
    void elPedidoRespondeIgualParaCuentaActivaInexistenteEInactiva() throws Exception {
        UsuariosDePrueba.Creado activa = UsuariosDePrueba.secretaria(jdbc);
        UsuariosDePrueba.Creado inactiva = UsuariosDePrueba.crear(jdbc, "SECRETARIA", false, false);
        String inexistente = UsuariosDePrueba.emailUnico("nadie");

        String respuestaActiva = cuerpo(pedir(activa.email()).andExpect(status().isAccepted()));
        String respuestaInexistente = cuerpo(pedir(inexistente).andExpect(status().isAccepted()));
        String respuestaInactiva = cuerpo(pedir(inactiva.email()).andExpect(status().isAccepted()));

        assertThat(respuestaActiva).isEqualTo(respuestaInexistente).isEqualTo(respuestaInactiva);
        assertThat(emailPara(activa.email()).texto()).contains("http://localhost:8080/restablecer?token=");
        assertThat(correo.esperar(inexistente, Duration.ofMillis(500))).isEmpty();
        assertThat(correo.esperar(inactiva.email(), Duration.ofMillis(500))).isEmpty();
    }

    @Test
    void elEmailTraeUnTokenCuyoHashEsElGuardadoYElPedidoSeAuditaSinDatos() throws Exception {
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.secretaria(jdbc);

        pedir(usuario.email()).andExpect(status().isAccepted());
        MensajeEmail email = emailPara(usuario.email());
        String token = MensajeriaDePrueba.Capturados.token(email);

        assertThat(email.html()).contains("restablecer?token=");
        Map<String, Object> fila = jdbc.queryForMap(
                "select token_hash, creado_at, vence_at from token_recuperacion where usuario_id = ?", usuario.id());
        assertThat(fila.get("token_hash")).isEqualTo(GeneradorDeTokens.hash(token));
        assertThat(((Timestamp) fila.get("vence_at")).toInstant())
                .isEqualTo(RelojDePrueba.INICIO.plus(Duration.ofHours(1)));
        assertThat(jdbc.queryForList("select * from evento_auditoria where accion = 'SOLICITAR_RECUPERACION' "
                        + "and entidad_id = ?", usuario.id().toString()))
                .singleElement()
                .satisfies(evento -> assertThat(evento.toString()).doesNotContain(usuario.email()).doesNotContain(token));
    }

    @Test
    void conElTokenSeRestableceSeDesbloqueaYSePuedeEntrarConLaNueva() throws Exception {
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.crear(jdbc, "SECRETARIA", true, true);
        jdbc.update("update usuario set bloqueado_hasta = ?, intentos_fallidos = 3 where id = ?",
                Timestamp.from(RelojDePrueba.INICIO.plus(Duration.ofMinutes(10))), usuario.id());
        pedir(usuario.email());
        String token = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));

        restablecer(token, NUEVA).andExpect(status().isNoContent());

        Map<String, Object> fila = jdbc.queryForMap("select password_hash, debe_cambiar_password, bloqueado_hasta, "
                + "intentos_fallidos from usuario where id = ?", usuario.id());
        assertThat((String) fila.get("password_hash")).startsWith("$2a$12$");
        assertThat(fila.get("debe_cambiar_password")).isEqualTo(false);
        assertThat(fila.get("bloqueado_hasta")).isNull();
        assertThat(fila.get("intentos_fallidos")).isEqualTo(0);
        UsuariosDePrueba.login(mvc, usuario.email(), NUEVA);
        assertThat(jdbc.queryForObject("select count(*) from evento_auditoria where accion = 'MODIFICAR_ENTIDAD' "
                        + "and entidad_id = ? and detalle ->> 'referencia' = 'recuperacion-password'",
                Integer.class, usuario.id().toString()))
                .isEqualTo(1);
    }

    @Test
    void unTokenUsadoVencidoOInexistenteDanElMismo400() throws Exception {
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.secretaria(jdbc);
        pedir(usuario.email());
        String usado = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));
        restablecer(usado, NUEVA).andExpect(status().isNoContent());

        String reutilizado = cuerpo(restablecer(usado, "otra-clave-nueva-1").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problemas/token-invalido")));
        String inexistente = cuerpo(restablecer(GeneradorDeTokens.nuevo(), NUEVA).andExpect(status().isBadRequest()));

        pedir(usuario.email());
        String porVencer = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));
        reloj.adelantar(Duration.ofMinutes(61));
        String vencido = cuerpo(restablecer(porVencer, NUEVA).andExpect(status().isBadRequest()));

        assertThat(reutilizado.replace("/password/reset", ""))
                .isEqualTo(inexistente.replace("/password/reset", ""))
                .isEqualTo(vencido.replace("/password/reset", ""));
    }

    @Test
    void unPedidoNuevoInvalidaElEnlaceAnterior() throws Exception {
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.secretaria(jdbc);
        pedir(usuario.email());
        String primero = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));
        pedir(usuario.email());
        String segundo = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));

        restablecer(primero, NUEVA).andExpect(status().isBadRequest());
        restablecer(segundo, NUEVA).andExpect(status().isNoContent());
    }

    @Test
    void unaContrasenaInvalidaNoConsumeElToken() throws Exception {
        UsuariosDePrueba.Creado usuario = UsuariosDePrueba.secretaria(jdbc);
        pedir(usuario.email());
        String token = MensajeriaDePrueba.Capturados.token(emailPara(usuario.email()));

        restablecer(token, "corta").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problemas/password-invalida"));
        restablecer(token, NUEVA).andExpect(status().isNoContent());
    }

    @Test
    void sinTokenCsrfAmbosResponden403() throws Exception {
        mvc.perform(post("/api/v1/auth/password/recover").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@consultorio.test\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"x\",\"nueva\":\"" + NUEVA + "\"}"))
                .andExpect(status().isForbidden());
    }
}
