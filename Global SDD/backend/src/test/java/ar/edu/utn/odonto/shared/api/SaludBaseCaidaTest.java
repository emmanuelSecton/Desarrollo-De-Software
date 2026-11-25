package ar.edu.utn.odonto.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Usa un contenedor PROPIO (no el singleton) porque lo detiene durante el test. */
@SpringBootTest(properties = "spring.datasource.hikari.connection-timeout=1000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SaludBaseCaidaTest {

    static final PostgreSQLContainer<?> BASE_DEDICADA =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.16-alpine"))
                    .withInitScript("db/init/auditoria-roles.sql");

    static {
        BASE_DEDICADA.start();
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BASE_DEDICADA::getJdbcUrl);
        registro.add("spring.datasource.username", BASE_DEDICADA::getUsername);
        registro.add("spring.datasource.password", BASE_DEDICADA::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @Test
    void healthDevuelve503DownCuandoLaBaseDejaDeResponder() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());

        BASE_DEDICADA.stop();

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\"}", true));
    }
}
