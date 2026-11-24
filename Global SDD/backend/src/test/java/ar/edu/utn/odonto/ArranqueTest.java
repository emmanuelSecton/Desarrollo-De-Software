package ar.edu.utn.odonto;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ArranqueTest {

    @Autowired
    DataSource dataSource;

    @Test
    void elContextoArrancaYSeConectaAPostgreSql16() {
        String version = new JdbcTemplate(dataSource).queryForObject("select version()", String.class);

        assertThat(version).startsWith("PostgreSQL 16.");
    }
}
