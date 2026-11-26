package ar.edu.utn.odonto.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import ar.edu.utn.odonto.support.ControladorDePrueba;
import ar.edu.utn.odonto.support.SeguridadReal;
import ar.edu.utn.odonto.support.ControladorDePrueba.Entrada;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = ControladorDePrueba.class)
@SeguridadReal
@TestPropertySource(properties = "spring.web.locale=es-AR")
@ExtendWith(OutputCaptureExtension.class)
class ManejadorGlobalDeErroresTest {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    WebApplicationContext contexto;

    /** Todas las peticiones con sesion de personal y token CSRF: este test verifica errores, no seguridad (C-03). */
    MockMvc mvc;

    @BeforeEach
    void configurarMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto)
                .apply(springSecurity())
                .defaultRequest(get("/").with(csrf()).with(user("personal").roles("ODONTOLOGO")))
                .build();
    }

    @Autowired
    ManejadorGlobalDeErrores manejador;

    @Test
    void cuerpoInvalidoDevuelve400ConErroresPorCampoEnEspanol() throws Exception {
        mvc.perform(post("/api/v1/__test__/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"edad\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/v1/__test__/validar"))
                .andExpect(jsonPath("$.errores.length()").value(1))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").value("no debe estar vacío"));
    }

    @Test
    void variosCamposInvalidosSeListanTodos() throws Exception {
        mvc.perform(post("/api/v1/__test__/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"edad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.length()").value(2))
                .andExpect(jsonPath("$.errores[?(@.campo=='nombre')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo=='edad')]").exists());
    }

    @Test
    void parametroInvalidoDevuelve400ConErrores() throws Exception {
        mvc.perform(get("/api/v1/__test__/parametro").param("cantidad", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.length()").value(1))
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
    }

    @Test
    void jsonMalFormadoDevuelve400SinMensajeDelParser() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/__test__/validar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("El cuerpo de la petición no es válido"))
                .andReturn();
        assertThat(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("Unexpected")
                .doesNotContain("JSON parse")
                .doesNotContain("jackson");
    }

    @Test
    void excepcionNoPrevistaDevuelve500GenericoSinStackTrace() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/__test__/explota")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("cuerpo"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.instance").value("/api/v1/__test__/explota"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado"))
                .andReturn();
        assertThat(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain("IllegalStateException")
                .doesNotContain("SELECT")
                .doesNotContain("ar.edu.utn")
                .doesNotContain("12345678");
    }

    @Test
    void elLogDelErrorInesperadoNoIncluyeCuerpoNiMensajeDeLaExcepcion(CapturedOutput salida) throws Exception {
        mvc.perform(post("/api/v1/__test__/explota?dni=87654321")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("diagnostico-reservado"))
                .andExpect(status().isInternalServerError());

        assertThat(salida.getAll())
                .contains("IllegalStateException")
                .contains("/api/v1/__test__/explota")
                .doesNotContain("diagnostico-reservado")
                .doesNotContain("87654321")
                .doesNotContain("12345678");
    }

    @Test
    void rutaDeApiInexistenteDevuelve404ProblemJson() throws Exception {
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.instance").value("/api/v1/no-existe"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void metodoNoPermitidoDevuelve405ProblemJson() throws Exception {
        mvc.perform(delete("/api/v1/__test__/validar"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value("/api/v1/__test__/validar"));
    }

    @Test
    void constraintViolationExceptionSeTraduceA400ConErrores() {
        Set<ConstraintViolation<Entrada>> violaciones =
                Validation.buildDefaultValidatorFactory().getValidator().validate(new Entrada("", 5));

        var respuesta = manejador.handleConstraintViolation(new ConstraintViolationException(violaciones));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problema = respuesta.getBody();
        assertThat(problema).isNotNull();
        assertThat(problema.getStatus()).isEqualTo(400);
        assertThat(problema.getProperties()).containsKey("errores");
        assertThat(problema.getProperties().get("errores").toString()).contains("nombre");
    }
}
