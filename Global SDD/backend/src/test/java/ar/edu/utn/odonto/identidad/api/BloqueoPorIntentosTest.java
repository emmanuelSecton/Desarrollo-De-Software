package ar.edu.utn.odonto.identidad.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import ar.edu.utn.odonto.support.RelojDePrueba;
import ar.edu.utn.odonto.support.UsuariosDePrueba;
import java.time.Duration;
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

/** Bloqueo de punta a punta por HTTP (RN-SEG-03), con el reloj adelantado en vez de esperar. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, RelojDePrueba.class})
class BloqueoPorIntentosTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RelojDePrueba.RelojMutable reloj;

    @Autowired
    DataSource dataSource;

    @Test
    void cincoFallosBloqueanQuinceMinutosAunConLaClaveCorrecta() throws Exception {
        reloj.fijar(RelojDePrueba.INICIO);
        UsuariosDePrueba.Creado secretaria = UsuariosDePrueba.secretaria(new JdbcTemplate(dataSource));
        for (int i = 0; i < 5; i++) {
            login(secretaria.email(), "incorrecta-" + i).andExpect(status().isUnauthorized());
        }

        reloj.adelantar(Duration.ofMinutes(10));
        login(secretaria.email(), secretaria.clave())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problemas/credenciales-invalidas"));

        reloj.adelantar(Duration.ofMinutes(5));
        login(secretaria.email(), secretaria.clave()).andExpect(status().isOk());
    }

    private ResultActions login(String email, String clave) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(UsuariosDePrueba.cuerpoLogin(email, clave)));
    }
}
