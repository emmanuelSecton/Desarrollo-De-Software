package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.ControladorRbacDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Rutas autenticadas por defecto, RBAC (RN-SEG-02, D9, D13) y cabeceras de seguridad (D12). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class AutorizacionYAccesoTest {

    private static final String HISTORIA = "/api/v1/__test__/rbac/historia";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    /** Odontologo sin tocar al usuario del alta inicial: misma forma de autenticacion que el login real. */
    private static RequestPostProcessor odontologo() {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ODONTOLOGO"))));
    }

    @Test
    void unaSecretariaRecibe403EnHistoriaClinicaSinEjecutarElMetodo() throws Exception {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(new JdbcTemplate(dataSource));
        MockHttpSession sesion = UsuariosDePrueba.login(mvc, secretaria.email(), secretaria.clave());
        int antes = ControladorRbacDePrueba.EJECUCIONES_HISTORIA.get();

        mvc.perform(get(HISTORIA).session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problemas/acceso-denegado"));

        assertThat(ControladorRbacDePrueba.EJECUCIONES_HISTORIA.get()).isEqualTo(antes);
        mvc.perform(get("/api/v1/__test__/rbac/personal").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void elOdontologoEjecutaLaRutaDeHistoriaClinica() throws Exception {
        mvc.perform(get(HISTORIA).with(odontologo()))
                .andExpect(status().isOk())
                .andExpect(content().string("historia"));
    }

    @ParameterizedTest
    @ValueSource(strings = {HISTORIA, "/api/v1/no-existe", "/api/v1/pacientes"})
    void sinSesionTodaRutaDeApiDevuelve401ProblemJson(String ruta) throws Exception {
        mvc.perform(get(ruta))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problemas/no-autenticado"));
    }

    @Test
    void conSesionUnaRutaDeApiInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/no-existe").with(odontologo()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator/health", "/", "/agenda/semana", "/api/v1/auth/csrf"})
    void losRecursosPublicosNoPidenSesion(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().is2xxSuccessful());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/api/v1/auth/csrf", "/api/v1/pacientes"})
    void todaRespuestaLlevaLasCabecerasDeSeguridad(String ruta) throws Exception {
        mvc.perform(get(ruta))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString(
                        "default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString(
                        "frame-ancestors 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void sobreHttpsSeAgregaHsts() throws Exception {
        mvc.perform(get("/").secure(true))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
    }
}
