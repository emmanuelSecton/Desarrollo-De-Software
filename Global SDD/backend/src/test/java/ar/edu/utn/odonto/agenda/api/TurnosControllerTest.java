package ar.edu.utn.odonto.agenda.api;

import static ar.edu.utn.odonto.support.AutenticacionDePrueba.como;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.TurnosDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
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

/** API de turnos con la cadena de seguridad real (US-011, C-10, D11). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class TurnosControllerTest {

    private static final String RUTA = "/api/v1/turnos";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    DataSource dataSource;

    UUID paciente;
    UUID limpieza;
    Instant dia;

    @BeforeEach
    void preparar() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        paciente = TurnosDePrueba.paciente(jdbc);
        limpieza = TurnosDePrueba.limpieza(jdbc);
        dia = TurnosDePrueba.diaLibre();
    }

    /** Hora en Buenos Aires (-03:00), como la mandaria la SPA. */
    private String hora(int minutos) {
        return dia.plus(Duration.ofMinutes(minutos)).atOffset(ZoneOffset.ofHours(-3)).toString();
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido,
            String cuerpo) throws Exception {
        return mvc.perform(pedido.with(como("SECRETARIA")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions alta(int minutos, String extra) throws Exception {
        return enviar(post(RUTA), "{\"pacienteId\":\"" + paciente + "\",\"prestacionId\":\"" + limpieza
                + "\",\"inicio\":\"" + hora(minutos) + "\"" + extra + "}");
    }

    private JsonNode creado(int minutos) throws Exception {
        return mapper.readTree(alta(minutos, "").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void laSecretariaCreaUnTurnoConVersionYToken() throws Exception {
        alta(0, "")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/turnos/")))
                .andExpect(jsonPath("$.estado").value("RESERVADO"))
                .andExpect(jsonPath("$.origen").value("INTERNO"))
                .andExpect(jsonPath("$.duracionMinutos").value(45))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.tokenPublico").value(org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.inicio").value(dia.toString()));
    }

    @Test
    void unTurnoSuperpuestoDa409ConSugerenciaYElSobreturnoSeCrea() throws Exception {
        creado(0);

        alta(30, "")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problemas/turno-superpuesto"))
                .andExpect(jsonPath("$.detail").value(containsString("sobreturno")));
        alta(30, ",\"sobreturno\":true")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sobreturno").value(true));
    }

    @Test
    void losDatosInvalidosDan400PorCampo() throws Exception {
        alta(0, ",\"duracionMinutos\":40")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("duracionMinutos"));
        alta(7, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("inicio"));
        enviar(post(RUTA), "{\"prestacionId\":\"" + limpieza + "\",\"inicio\":\"" + hora(0) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("pacienteId")));
    }

    @Test
    void listarPorRangoYObtenerPorId() throws Exception {
        JsonNode turno = creado(0);
        String desde = dia.minus(Duration.ofHours(1)).toString();
        String hasta = dia.plus(Duration.ofHours(10)).toString();

        mvc.perform(get(RUTA).param("desde", desde).param("hasta", hasta).with(como("ODONTOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(turno.get("id").asText()))
                .andExpect(jsonPath("$[0].prestacionNombre").value("Limpieza"))
                .andExpect(jsonPath("$[0].pacienteNombre").value("Ana"));
        mvc.perform(get(RUTA).param("desde", desde).param("hasta", dia.plus(Duration.ofDays(40)).toString())
                        .with(como("ODONTOLOGO")))
                .andExpect(status().isBadRequest());
        mvc.perform(get(RUTA + "/" + turno.get("id").asText()).with(como("ODONTOLOGO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turno.get("id").asText()));
        mvc.perform(get(RUTA + "/" + UUID.randomUUID()).with(como("ODONTOLOGO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void reprogramarConfirmarYCancelarConVersion() throws Exception {
        String id = creado(0).get("id").asText();

        String nuevo = mapper.readTree(enviar(put(RUTA + "/" + id), "{\"version\":0,\"inicio\":\"" + hora(15) + "\"}")
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.reprogramadoDe").value(id))
                        .andReturn().getResponse().getContentAsString())
                .get("id").asText();

        enviar(post(RUTA + "/" + nuevo + "/confirmar"), "{\"version\":0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"))
                .andExpect(jsonPath("$.version").value(1));
        enviar(post(RUTA + "/" + nuevo + "/cancelar"),
                        "{\"version\":0,\"canceladoPor\":\"PACIENTE\",\"motivo\":\"Avisó\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problemas/turno-modificado"));
        enviar(post(RUTA + "/" + nuevo + "/cancelar"), "{\"version\":1,\"canceladoPor\":\"PACIENTE\",\"motivo\":\"\"}")
                .andExpect(status().isBadRequest());
        enviar(post(RUTA + "/" + nuevo + "/cancelar"),
                        "{\"version\":1,\"canceladoPor\":\"PACIENTE\",\"motivo\":\"Avisó por teléfono\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"))
                .andExpect(jsonPath("$.canceladoPor").value("PACIENTE"));
        enviar(post(RUTA + "/" + nuevo + "/confirmar"), "{\"version\":2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problemas/transicion-invalida"));
    }

    @Test
    void sinSesionTodoDa401() throws Exception {
        mvc.perform(get(RUTA + "/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(post(RUTA).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
