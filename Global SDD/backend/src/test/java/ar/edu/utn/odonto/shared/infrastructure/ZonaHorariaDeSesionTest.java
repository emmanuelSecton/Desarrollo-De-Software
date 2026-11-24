package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ZonaHorariaDeSesionTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    Environment entorno;

    @Test
    void laSesionDeBaseDeLaAplicacionUsaUtc() {
        String zona = new JdbcTemplate(dataSource).queryForObject("select current_setting('TimeZone')", String.class);

        assertThat(zona).isEqualTo("UTC");
    }

    @Test
    void hibernateEscribeYLeeInstantesEnUtc() {
        assertThat(entorno.getProperty("spring.jpa.properties.hibernate.jdbc.time_zone"))
                .isEqualTo("UTC");
    }
}
