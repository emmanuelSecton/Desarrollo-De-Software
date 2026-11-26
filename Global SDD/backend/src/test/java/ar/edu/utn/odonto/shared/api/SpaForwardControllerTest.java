package ar.edu.utn.odonto.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import ar.edu.utn.odonto.support.SeguridadReal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SpaForwardController.class)
@SeguridadReal
class SpaForwardControllerTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/agenda/semana", "/pacientes", "/pacientes/123/historia", "/a/b/c/d/e"})
    void rutasDelClienteDevuelvenIndexHtml(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void rutaDeApiInexistenteNoEsInterceptadaYDevuelve404ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/no-existe").with(user("personal").roles("ODONTOLOGO")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rutaDeApiInexistenteSinSesionDevuelve401ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(forwardedUrl(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api", "/api/v1/pacientes/1", "/actuator/health", "/actuator/env"})
    void apiYActuatorNuncaSeReenvianALaSpa(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(forwardedUrl(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/favicon.ico", "/assets/app.js", "/assets/img/logo.svg"})
    void archivosEstaticosInexistentesDevuelven404EnVezDeIndexHtml(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isNotFound()).andExpect(forwardedUrl(null));
    }

    @Test
    void soloSeReenvianPeticionesGet() throws Exception {
        mvc.perform(post("/agenda/semana")).andExpect(forwardedUrl(null));
    }
}
