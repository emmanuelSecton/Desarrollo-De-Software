package ar.edu.utn.odonto.pacientes.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.PacientesDePrueba;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** API de pacientes con la cadena de seguridad real (D4, D5, D8). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class PacientesControllerTest {

    private static final String RUTA = "/api/v1/pacientes";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    DataSource dataSource;

    /** Misma forma de autenticacion que deja el login real de C-03: principal UUID y un rol. */
    private static RequestPostProcessor como(String rol) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private static String alta(String dni, String apellido, String telefono, String extra) {
        return "{\"dni\":\"" + dni + "\",\"nombre\":\"Ana\",\"apellido\":\"" + apellido + "\",\"telefono\":\""
                + telefono + "\",\"consentimientoDatos\":true" + extra + "}";
    }

    private ResultActions crear(String cuerpo, String rol) throws Exception {
        return mvc.perform(post(RUTA).with(como(rol)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private JsonNode crearOk(String apellido) throws Exception {
        String cuerpo = crear(alta(PacientesDePrueba.dniUnico(), apellido, PacientesDePrueba.celularUnico(), ""),
                        "SECRETARIA")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(cuerpo);
    }

    @Test
    void unAltaCorrectaDevuelve201ConLocationYDatosNormalizados() throws Exception {
        crear(alta("30." + PacientesDePrueba.dniUnico().substring(2), "Pérez", "011 15-3412-5566",
                        ",\"email\":\"ana@consultorio.test\",\"fechaNacimiento\":\"1990-05-17\",\"aceptaWhatsapp\":true"),
                        "SECRETARIA")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/pacientes/")))
                .andExpect(jsonPath("$.dni").value(org.hamcrest.Matchers.matchesPattern("30[0-9]{6}")))
                .andExpect(jsonPath("$.telefono").value("+5491134125566"))
                .andExpect(jsonPath("$.aceptaWhatsapp").value(true))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.ausenciasCount").value(0))
                .andExpect(jsonPath("$.consentimientoDatosAt").exists());
    }

    @Test
    void unDniExistenteDevuelve409ConElIdDelPaciente() throws Exception {
        JsonNode existente = crearOk("Gómez");

        crear(alta(existente.get("dni").asText(), "Otro", PacientesDePrueba.celularUnico(), ""), "SECRETARIA")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problemas/paciente-duplicado"))
                .andExpect(jsonPath("$.pacienteId").value(existente.get("id").asText()));
    }

    @Test
    void sinConsentimientoDeDatosDevuelve400() throws Exception {
        String sinConsentimiento = "{\"dni\":\"" + PacientesDePrueba.dniUnico() + "\",\"nombre\":\"Ana\","
                + "\"apellido\":\"Pérez\",\"telefono\":\"" + PacientesDePrueba.celularUnico()
                + "\",\"consentimientoDatos\":false}";

        crear(sinConsentimiento, "SECRETARIA")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("consentimientoDatos")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "30A23456|+5491134125566||dni",
        "30123456|12345||telefono",
        "30123456|+5491134125566|,\"email\":\"no-es-email\"|email",
        "30123456|+5491134125566|,\"fechaNacimiento\":\"2999-01-01\"|fechaNacimiento",
    })
    void losDatosInvalidosDevuelven400PorCampo(String dni, String telefono, String extra, String campo)
            throws Exception {
        crear(alta(dni, "Pérez", telefono, extra == null ? "" : extra), "SECRETARIA")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errores[*].campo", hasItem(campo)));
    }

    @Test
    void consultaYModificacion() throws Exception {
        JsonNode creado = crearOk("Ruiz");
        String id = creado.get("id").asText();

        mvc.perform(get(RUTA + "/" + id).with(como("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apellido").value("Ruiz"));

        mvc.perform(put(RUTA + "/" + id).with(como("SECRETARIA")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + creado.get("dni").asText() + "\",\"nombre\":\"Ana\","
                                + "\"apellido\":\"Ruiz\",\"telefono\":\"+54 9 11 3999-0001\","
                                + "\"email\":\"nuevo@consultorio.test\",\"aceptaWhatsapp\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("+5491139990001"))
                .andExpect(jsonPath("$.email").value("nuevo@consultorio.test"));
    }

    @Test
    void unIdInexistenteDevuelve404() throws Exception {
        mvc.perform(get(RUTA + "/" + UUID.randomUUID()).with(como("SECRETARIA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problemas/paciente-inexistente"));
    }

    @Test
    void cambiarElDniPorUnoExistenteDevuelve409() throws Exception {
        JsonNode uno = crearOk("Uno");
        JsonNode otro = crearOk("Otro");

        mvc.perform(put(RUTA + "/" + otro.get("id").asText()).with(como("SECRETARIA")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dni\":\"" + uno.get("dni").asText() + "\",\"nombre\":\"Ana\","
                                + "\"apellido\":\"Otro\",\"telefono\":\"" + otro.get("telefono").asText() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void laBusquedaEncuentraSinAcentosPorDniYPorTelefonoYPagina() throws Exception {
        String sufijo = PacientesDePrueba.sufijo();
        JsonNode paciente = crearOk("Núñez " + sufijo);

        mvc.perform(get(RUTA).param("q", "nunez " + sufijo).with(como("SECRETARIA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(paciente.get("id").asText()))
                .andExpect(jsonPath("$.tamano").value(20));
        mvc.perform(get(RUTA).param("q", paciente.get("dni").asText()).with(como("SECRETARIA")))
                .andExpect(jsonPath("$.contenido[*].id", hasItem(paciente.get("id").asText())));
        mvc.perform(get(RUTA).param("q", paciente.get("telefono").asText()).with(como("SECRETARIA")))
                .andExpect(jsonPath("$.contenido[*].id", hasItem(paciente.get("id").asText())));
        mvc.perform(get(RUTA).param("tamano", "101").with(como("SECRETARIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void laSecretariaNoDaDeBajaNiReactiva() throws Exception {
        String id = crearOk("Baja").get("id").asText();

        mvc.perform(delete(RUTA + "/" + id).with(como("SECRETARIA")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post(RUTA + "/" + id + "/reactivar").with(como("SECRETARIA")).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(activo(id)).isTrue();
    }

    @Test
    void elOdontologoDaDeBajaYReactivaSinBorrar() throws Exception {
        String sufijo = PacientesDePrueba.sufijo();
        String id = crearOk("Baja " + sufijo).get("id").asText();

        mvc.perform(delete(RUTA + "/" + id).with(como("ODONTOLOGO")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(activo(id)).isFalse();
        mvc.perform(get(RUTA).param("q", sufijo).with(como("ODONTOLOGO")))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get(RUTA).param("q", sufijo).param("incluirInactivos", "true").with(como("ODONTOLOGO")))
                .andExpect(jsonPath("$.total").value(1));

        mvc.perform(post(RUTA + "/" + id + "/reactivar").with(como("ODONTOLOGO")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(activo(id)).isTrue();
    }

    @Test
    void sinSesionTodoDevuelve401() throws Exception {
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private Boolean activo(String id) {
        return new JdbcTemplate(dataSource).queryForObject(
                "select activo from paciente where id = ?", Boolean.class, UUID.fromString(id));
    }
}
