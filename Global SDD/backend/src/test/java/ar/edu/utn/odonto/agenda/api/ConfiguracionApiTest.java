package ar.edu.utn.odonto.agenda.api;

import static ar.edu.utn.odonto.support.AutenticacionDePrueba.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.agenda.application.ConfiguracionDeAgenda;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

/**
 * API de configuracion con la cadena de seguridad real (US-001, C-05, D7, D8). Restaura el consultorio y usa
 * el domingo (7) para las franjas, para no alterar los datos iniciales que leen otros tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ConfiguracionApiTest {

    private static final int DOMINGO = 7;
    private static final String CONSULTORIO_VALIDO = "{\"nombre\":\"Consultorio Odontológico Sur\","
            + "\"profesionalNombre\":\"Dra. Ana Ficticia\",\"matricula\":\"MP 12345\",\"direccion\":\"Calle Falsa 123\","
            + "\"telefono\":\"0261 4123456\",\"email\":\"contacto@consultorio.test\",\"slugReserva\":\"consultorio-sur\","
            + "\"recordatorioHorasAntes\":72,\"confirmacionLimiteHoras\":12,\"cancelacionMinimaHoras\":6,"
            + "\"ofertaVencimientoMinutos\":45}";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    DataSource dataSource;

    @Autowired
    ConfiguracionDeAgenda configuracion;

    JdbcTemplate jdbc;

    @BeforeEach
    void preparar() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void restaurar() {
        jdbc.update("update consultorio set nombre = 'Mi consultorio', profesional_nombre = 'Profesional (completar)', "
                + "matricula = null, direccion = null, telefono = null, email = null, slug_reserva = 'mi-consultorio', "
                + "recordatorio_horas_antes = 48, confirmacion_limite_horas = 24, cancelacion_minima_horas = 24, "
                + "oferta_vencimiento_minutos = 30");
        jdbc.update("delete from horario_atencion where dia_semana = ?", DOMINGO);
    }

    private ResultActions enviar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido,
            String rol, String cuerpo) throws Exception {
        pedido.with(como(rol)).with(csrf());
        if (cuerpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        }
        return mvc.perform(pedido);
    }

    private int eventos(String accion, String entidad, String id) {
        return jdbc.queryForObject("select count(*) from evento_auditoria where accion = ? and entidad = ? "
                + "and entidad_id = ?", Integer.class, accion, entidad, id);
    }

    @Nested
    class Consultorio {

        @Test
        void elPersonalLeeLaConfiguracionInicial() throws Exception {
            mvc.perform(get("/api/v1/consultorio").with(como("SECRETARIA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.slugReserva").value("mi-consultorio"))
                    .andExpect(jsonPath("$.recordatorioHorasAntes").value(48))
                    .andExpect(jsonPath("$.confirmacionLimiteHoras").value(24))
                    .andExpect(jsonPath("$.cancelacionMinimaHoras").value(24))
                    .andExpect(jsonPath("$.ofertaVencimientoMinutos").value(30));
        }

        @Test
        void elOdontologoCompletaLosDatosYQuedaAuditado() throws Exception {
            UUID odontologo = UUID.randomUUID();
            String id = jdbc.queryForObject("select id::text from consultorio", String.class);

            mvc.perform(put("/api/v1/consultorio").with(como("ODONTOLOGO", odontologo)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(CONSULTORIO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Consultorio Odontológico Sur"))
                    .andExpect(jsonPath("$.recordatorioHorasAntes").value(72));

            assertThat(configuracion.parametros().confirmacionLimiteHoras()).isEqualTo(12);
            assertThat(jdbc.queryForObject("select count(*) from evento_auditoria where accion = 'MODIFICAR_ENTIDAD' "
                    + "and entidad = 'CONSULTORIO' and entidad_id = ? and actor_id = ?", Integer.class, id,
                    odontologo.toString())).isGreaterThanOrEqualTo(1);
        }

        @Test
        void unSlugInvalidoOConfirmacionPosteriorAlRecordatorioDan400SinCambios() throws Exception {
            enviar(put("/api/v1/consultorio"), "ODONTOLOGO", CONSULTORIO_VALIDO.replace("consultorio-sur", "Sur"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[*].campo", hasItem("slugReserva")));
            enviar(put("/api/v1/consultorio"), "ODONTOLOGO",
                    CONSULTORIO_VALIDO.replace("\"recordatorioHorasAntes\":72", "\"recordatorioHorasAntes\":24")
                            .replace("\"confirmacionLimiteHoras\":12", "\"confirmacionLimiteHoras\":36"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("confirmacionLimiteHoras"));

            assertThat(configuracion.parametros().recordatorioHorasAntes()).isEqualTo(48);
        }
    }

    @Nested
    class Horarios {

        private String crear(String desde, String hasta) throws Exception {
            return enviar(post("/api/v1/horarios"), "ODONTOLOGO",
                            "{\"diaSemana\":7,\"desde\":\"" + desde + "\",\"hasta\":\"" + hasta + "\"}")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
        }

        @Test
        void listaLasFranjasInicialesOrdenadasConHorasHHmm() throws Exception {
            mvc.perform(get("/api/v1/horarios").with(como("SECRETARIA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].diaSemana").value(1))
                    .andExpect(jsonPath("$[0].desde").value("09:00"))
                    .andExpect(jsonPath("$[0].hasta").value("13:00"))
                    .andExpect(jsonPath("$[1].desde").value("15:00"));
        }

        @Test
        void unaFranjaSuperpuestaDa409YUnaContiguaSeAcepta() throws Exception {
            crear("09:00", "13:00");

            enviar(post("/api/v1/horarios"), "ODONTOLOGO", "{\"diaSemana\":7,\"desde\":\"12:00\",\"hasta\":\"14:00\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/problemas/horario-superpuesto"));
            crear("13:00", "14:00");
            assertThat(configuracion.franjas()).filteredOn(f -> f.diaSemana() == DOMINGO).hasSize(2);
        }

        @Test
        void modificarYBorrarUnaFranjaQuedaAuditado() throws Exception {
            JsonNode creada = mapper.readTree(crear("08:00", "10:00"));
            String id = creada.get("id").asText();

            enviar(put("/api/v1/horarios/" + id), "ODONTOLOGO", "{\"diaSemana\":7,\"desde\":\"08:30\",\"hasta\":\"10:00\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value("08:30"));
            enviar(delete("/api/v1/horarios/" + id), "ODONTOLOGO", null).andExpect(status().isNoContent());
            enviar(delete("/api/v1/horarios/" + id), "ODONTOLOGO", null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("/problemas/no-encontrado"));

            assertThat(eventos("CREAR_ENTIDAD", "HORARIO_ATENCION", id)).isEqualTo(1);
            assertThat(eventos("MODIFICAR_ENTIDAD", "HORARIO_ATENCION", id)).isEqualTo(1);
            assertThat(eventos("ANULAR_ENTIDAD", "HORARIO_ATENCION", id)).isEqualTo(1);
        }

        @Test
        void unaFranjaInvalidaDa400() throws Exception {
            enviar(post("/api/v1/horarios"), "ODONTOLOGO", "{\"diaSemana\":7,\"desde\":\"13:00\",\"hasta\":\"09:00\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("hasta"));
            enviar(post("/api/v1/horarios"), "ODONTOLOGO", "{\"diaSemana\":7,\"desde\":\"09:07\",\"hasta\":\"10:00\"}")
                    .andExpect(status().isBadRequest());
            enviar(post("/api/v1/horarios"), "ODONTOLOGO", "{\"diaSemana\":8,\"desde\":\"09:00\",\"hasta\":\"10:00\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("diaSemana"));
        }
    }

    @Nested
    class Prestaciones {

        private String nombre() {
            return "Blanqueamiento " + UUID.randomUUID().toString().substring(0, 8);
        }

        private JsonNode crear(String nombre, int duracion) throws Exception {
            return mapper.readTree(enviar(post("/api/v1/prestaciones"), "ODONTOLOGO",
                            "{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":" + duracion
                                    + ",\"precioReferencia\":25000.50}")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString());
        }

        @Test
        void listaLasPrestacionesIniciales() throws Exception {
            mvc.perform(get("/api/v1/prestaciones").with(como("SECRETARIA")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nombre", hasItem("Endodoncia")))
                    .andExpect(jsonPath("$[?(@.nombre=='Control')].duracionMinutos", hasItem(15)));
        }

        @Test
        void altaModificacionBajaYReactivacion() throws Exception {
            String nombre = nombre();
            JsonNode creada = crear(nombre, 60);
            String id = creada.get("id").asText();
            assertThat(creada.get("reservableOnline").asBoolean()).isTrue();

            enviar(put("/api/v1/prestaciones/" + id), "ODONTOLOGO",
                            "{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":75,\"reservableOnline\":false}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.duracionMinutos").value(75))
                    .andExpect(jsonPath("$.reservableOnline").value(false));

            enviar(delete("/api/v1/prestaciones/" + id), "ODONTOLOGO", null).andExpect(status().isNoContent());
            mvc.perform(get("/api/v1/prestaciones").with(como("ODONTOLOGO")))
                    .andExpect(jsonPath("$[*].id", not(hasItem(id))));
            mvc.perform(get("/api/v1/prestaciones").param("incluirInactivas", "true").with(como("ODONTOLOGO")))
                    .andExpect(jsonPath("$[*].id", hasItem(id)));
            assertThat(configuracion.prestacion(UUID.fromString(id))).hasValueSatisfying(p -> assertThat(p.activa()).isFalse());

            enviar(post("/api/v1/prestaciones/" + id + "/reactivar"), "ODONTOLOGO", null).andExpect(status().isNoContent());
            assertThat(eventos("ANULAR_ENTIDAD", "PRESTACION", id)).isEqualTo(1);
            assertThat(eventos("MODIFICAR_ENTIDAD", "PRESTACION", id)).isEqualTo(2);
        }

        @Test
        void duracionInvalidaDa400YNombreRepetidoDa409() throws Exception {
            String nombre = nombre();
            crear(nombre, 30);

            enviar(post("/api/v1/prestaciones"), "ODONTOLOGO", "{\"nombre\":\"Otra\",\"duracionMinutos\":32}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores[0].campo").value("duracionMinutos"));
            enviar(post("/api/v1/prestaciones"), "ODONTOLOGO", "{\"nombre\":\"Otra\",\"duracionMinutos\":0}")
                    .andExpect(status().isBadRequest());
            enviar(post("/api/v1/prestaciones"), "ODONTOLOGO",
                            "{\"nombre\":\"" + nombre.toUpperCase() + "\",\"duracionMinutos\":30}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.type").value("/problemas/prestacion-duplicada"));
        }

        @Test
        void unaPrestacionInexistenteDa404() throws Exception {
            enviar(delete("/api/v1/prestaciones/" + UUID.randomUUID()), "ODONTOLOGO", null)
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Permisos {

        @Test
        void laSecretariaLeeTodoPeroNoModificaNada() throws Exception {
            for (String ruta : new String[] {"/api/v1/consultorio", "/api/v1/horarios", "/api/v1/prestaciones"}) {
                mvc.perform(get(ruta).with(como("SECRETARIA"))).andExpect(status().isOk());
            }
            enviar(put("/api/v1/consultorio"), "SECRETARIA", CONSULTORIO_VALIDO).andExpect(status().isForbidden());
            enviar(post("/api/v1/horarios"), "SECRETARIA", "{\"diaSemana\":7,\"desde\":\"09:00\",\"hasta\":\"10:00\"}")
                    .andExpect(status().isForbidden());
            enviar(post("/api/v1/prestaciones"), "SECRETARIA", "{\"nombre\":\"X\",\"duracionMinutos\":30}")
                    .andExpect(status().isForbidden());
            String prestacion = jdbc.queryForObject("select id::text from prestacion where nombre = 'Control'", String.class);
            enviar(delete("/api/v1/prestaciones/" + prestacion), "SECRETARIA", null).andExpect(status().isForbidden());

            assertThat(configuracion.parametros().slugReserva()).isEqualTo("mi-consultorio");
            assertThat(configuracion.franjas()).noneMatch(f -> f.diaSemana() == DOMINGO);
            assertThat(configuracion.prestacion(UUID.fromString(prestacion))).hasValueSatisfying(p -> assertThat(p.activa()).isTrue());
        }

        @Test
        void sinSesionTodoDa401() throws Exception {
            for (String ruta : new String[] {"/api/v1/consultorio", "/api/v1/horarios", "/api/v1/prestaciones"}) {
                mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
            }
        }
    }
}
