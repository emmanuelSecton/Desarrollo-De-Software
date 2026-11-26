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
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Endpoints de sesion (Flujo 6, DD-08, D4, D7) con la cadena de seguridad real. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AuthControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    UsuariosDePrueba.Creado secretaria;

    @BeforeEach
    void crearUsuario() {
        secretaria = UsuariosDePrueba.secretaria(new JdbcTemplate(dataSource));
    }

    @Test
    void unLoginCorrectoDevuelveLosDatosYElRol() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UsuariosDePrueba.cuerpoLogin(secretaria.email(), secretaria.clave())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secretaria.id().toString()))
                .andExpect(jsonPath("$.email").value(secretaria.email()))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"))
                .andExpect(jsonPath("$.debeCambiarPassword").value(false))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void elLoginRotaElIdDeLaSesionAnonimaYFijaLaInactividadEnTreintaMinutos() throws Exception {
        MockHttpSession anonima = new MockHttpSession();
        String idAnterior = anonima.getId();

        MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
                        .session(anonima)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UsuariosDePrueba.cuerpoLogin(secretaria.email(), secretaria.clave())))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
        assertThat(sesion.getId()).isNotEqualTo(idAnterior);
        assertThat(sesion.getMaxInactiveInterval()).isEqualTo(1800);
    }

    @Test
    void emailInexistenteYClaveIncorrectaDevuelvenElMismo401() throws Exception {
        String inexistente = cuerpo401(UsuariosDePrueba.emailUnico("nadie"), "clave-cualquiera");
        String incorrecta = cuerpo401(secretaria.email(), "clave-incorrecta");

        assertThat(inexistente).isEqualTo(incorrecta);
        assertThat(inexistente).contains("/problemas/credenciales-invalidas").doesNotContain(secretaria.email());
    }

    @Test
    void unLoginSinTokenCsrfDevuelve403ProblemJson() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UsuariosDePrueba.cuerpoLogin(secretaria.email(), secretaria.clave())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problemas/acceso-denegado"));
    }

    @Test
    void meDevuelveElUsuarioDeLaSesion() throws Exception {
        MockHttpSession sesion = UsuariosDePrueba.login(mvc, secretaria.email(), secretaria.clave());

        mvc.perform(get("/api/v1/auth/me").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(secretaria.id().toString()))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"));
    }

    @Test
    void meSinSesionDevuelve401ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problemas/no-autenticado"));
    }

    @Test
    void elLogoutInvalidaLaSesion() throws Exception {
        MockHttpSession sesion = UsuariosDePrueba.login(mvc, secretaria.email(), secretaria.clave());

        mvc.perform(post("/api/v1/auth/logout").session(sesion).with(csrf())).andExpect(status().isNoContent());

        assertThat(sesion.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me").session(new MockHttpSession(null, sesion.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unCuerpoDeLoginInvalidoDevuelve400PorCampo() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2));
    }

    private String cuerpo401(String email, String clave) throws Exception {
        String cuerpo = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UsuariosDePrueba.cuerpoLogin(email, clave)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return cuerpo;
    }
}
