package ar.edu.utn.odonto.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.OdontoApplication;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class MigracionBaselineTest {

    private static final String INSTALADA_EN =
            "select max(extract(epoch from installed_on))::bigint from flyway_schema_history";

    @Autowired
    DataSource dataSource;

    @Autowired
    Environment entorno;

    @Test
    void laBaseVaciaQuedaConV1V2YLasTresExtensiones() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> extensiones = jdbc.queryForList("select extname from pg_extension", String.class);
        List<String> migraciones = jdbc.queryForList(
                "select version || ':' || description from flyway_schema_history where success order by installed_rank",
                String.class);

        assertThat(extensiones).contains("btree_gist", "citext", "pgcrypto");
        assertThat(migraciones).containsExactly("1:baseline", "2:auditoria", "3:identidad", "4:pacientes");
    }

    @Test
    void hibernateSoloValidaElEsquemaYNoLoModifica() {
        assertThat(entorno.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(entorno.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void unSegundoArranqueContraLaMismaBaseNoReaplicaMigraciones() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Long instalacionOriginal = jdbc.queryForObject(INSTALADA_EN, Long.class);

        try (ConfigurableApplicationContext segundo = new SpringApplicationBuilder(OdontoApplication.class)
                .web(WebApplicationType.NONE)
                .profiles("test")
                .run(
                        "--spring.datasource.url=" + PostgresTestConfig.POSTGRES.getJdbcUrl(),
                        "--spring.datasource.username=" + PostgresTestConfig.POSTGRES.getUsername(),
                        "--spring.datasource.password=" + PostgresTestConfig.POSTGRES.getPassword())) {
            Flyway flyway = segundo.getBean(Flyway.class);
            assertThat(flyway.info().pending()).isEmpty();
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        }

        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history", Integer.class))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject(INSTALADA_EN, Long.class)).isEqualTo(instalacionOriginal);
    }
}
