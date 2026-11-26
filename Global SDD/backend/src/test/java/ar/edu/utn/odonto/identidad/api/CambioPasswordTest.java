package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Cambio de contraseña obligatorio y su politica (D8), con base real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class CambioPasswordTest {

    private static final String NUEVA = "una-clave-nueva-larga";
    private static final String RUTA_NEGOCIO = "/api/v1/__test__/rbac/personal";

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    JdbcTemplate jdbc;
    UsuariosDePrueba.Creado pendiente;
    MockHttpSession sesion;

    @BeforeEach
    void iniciarSesionConCambioPendiente() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        pendiente = UsuariosDePrueba.crear(jdbc, "SECRETARIA", true, true);
        sesion = UsuariosDePrueba.login(mvc, pendiente.email(), pendiente.clave());
    }

    @Test
    void conElCambioPendienteLaApiDeNegocioDevuelve403ConTypePropio() throws Exception {
        mvc.perform(get(RUTA_NEGOCIO).session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problemas/cambio-password-requerido"));
    }

    @Test
    void conElCambioPendienteMeYLogoutSiguenDisponibles() throws Exception {
        mvc.perform(get("/api/v1/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debeCambiarPassword").value(true));
        mvc.perform(post("/api/v1/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void unCambioExitosoLimpiaLaMarcaGuardaOtroHashYHabilitaLaApi() throws Exception {
        String hashAnterior = hash();
        String idAnterior = sesion.getId();

        cambiar(pendiente.clave(), NUEVA).andExpect(status().isNoContent());

        Map<String, Object> fila = jdbc.queryForMap(
                "select debe_cambiar_password, password_hash from usuario where id = ?", pendiente.id());
        assertThat(fila.get("debe_cambiar_password")).isEqualTo(false);
        assertThat((String) fila.get("password_hash")).isNotEqualTo(hashAnterior).startsWith("$2a$12$");
        assertThat(sesion.getId()).isNotEqualTo(idAnterior);
        mvc.perform(get(RUTA_NEGOCIO).session(sesion)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from evento_auditoria where accion = 'MODIFICAR_ENTIDAD' "
                        + "and entidad_id = ? and detalle ->> 'referencia' = 'cambio-password'",
                Integer.class, pendiente.id().toString()))
                .isEqualTo(1);
    }

    @Test
    void conLaClaveActualIncorrectaDevuelve400YNoCambiaNada() throws Exception {
        String hashAnterior = hash();

        cambiar("no-es-la-actual", NUEVA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problemas/password-invalida"));

        assertThat(hash()).isEqualTo(hashAnterior);
    }

    @Test
    void unaNuevaIgualALaActualDevuelve400() throws Exception {
        cambiar(pendiente.clave(), pendiente.clave()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"corta-123", "ñññññññññññññññññññññññññññññññññññññ"})
    void unaNuevaDemasiadoCortaOConMasDe72BytesDevuelve400(String nueva) throws Exception {
        String hashAnterior = hash();

        cambiar(pendiente.clave(), nueva)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());

        assertThat(hash()).isEqualTo(hashAnterior);
        assertThat(jdbc.queryForObject("select debe_cambiar_password from usuario where id = ?", Boolean.class,
                        pendiente.id()))
                .isTrue();
    }

    @Test
    void sinSesionElCambioDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actual\":\"x\",\"nueva\":\"y\"}"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions cambiar(String actual, String nueva) throws Exception {
        return mvc.perform(post("/api/v1/auth/password")
                .session(sesion)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"actual\":\"" + actual + "\",\"nueva\":\"" + nueva + "\"}"));
    }

    private String hash() {
        return jdbc.queryForObject("select password_hash from usuario where id = ?", String.class, pendiente.id());
    }
}
