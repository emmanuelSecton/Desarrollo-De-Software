package ar.edu.utn.odonto.identidad.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import javax.sql.DataSource;
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

/** Limite por IP de login y rutas publicas (D10), con cupo 3 por minuto en un contexto propio. */
@SpringBootTest(properties = {"identidad.login-por-minuto=3", "identidad.publico-por-minuto=3"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class LimiteDePeticionesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DataSource dataSource;

    @Test
    void alAgotarElCupoElLoginDevuelve429SinEvaluarCredenciales() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(jdbc);
        for (int i = 0; i < 3; i++) {
            login("10.0.0.1", secretaria.email(), "incorrecta").andExpect(status().isUnauthorized());
        }

        login("10.0.0.1", secretaria.email(), "incorrecta")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problemas/demasiadas-peticiones"));

        assertThat(jdbc.queryForObject("select count(*) from evento_auditoria where accion = 'LOGIN_FALLIDO' "
                        + "and entidad_id = ?", Integer.class, secretaria.id().toString()))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class,
                        secretaria.id()))
                .isEqualTo(3);
    }

    @Test
    void otraIpConservaSuPropioCupo() throws Exception {
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(new JdbcTemplate(dataSource));
        for (int i = 0; i < 4; i++) {
            login("10.0.0.2", secretaria.email(), "incorrecta");
        }

        login("10.0.0.3", secretaria.email(), secretaria.clave()).andExpect(status().isOk());
    }

    @Test
    void lasRutasPublicasTambienTienenLimite() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/api/v1/public/disponibilidad").with(ip("10.0.0.4")))
                    .andExpect(status().isNotFound());
        }

        mvc.perform(get("/api/v1/public/disponibilidad").with(ip("10.0.0.4")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void lasRutasAutenticadasNoConsumenCupo() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(get("/api/v1/auth/me").with(ip("10.0.0.5"))).andExpect(status().isUnauthorized());
        }
    }

    private ResultActions login(String direccion, String email, String clave) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(ip(direccion))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(UsuariosDePrueba.cuerpoLogin(email, clave)));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor ip(String direccion) {
        return peticion -> {
            peticion.setRemoteAddr(direccion);
            return peticion;
        };
    }
}
