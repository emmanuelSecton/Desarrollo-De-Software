package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.RegistroAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.support.ConexionRuntimeTest;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class RepositorioAuditoriaRuntimeTest {

    private NamedParameterJdbcTemplate jdbcRuntime;
    private RepositorioAuditoriaJdbc repositorio;

    @BeforeEach
    void configurar() {
        DataSource fuente = ConexionRuntimeTest.dataSourceRuntime();
        jdbcRuntime = new NamedParameterJdbcTemplate(fuente);
        DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
        fabrica.registerSingleton("jdbcRuntime", jdbcRuntime);
        repositorio = new RepositorioAuditoriaJdbc(
                fabrica.getBeanProvider(NamedParameterJdbcTemplate.class),
                Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper());
    }

    @Test
    void elRepositorioGuardaYElRolRuntimePuedeConsultarElEvento() {
        repositorio.guardar(registro("runtime-jdbc-1"));

        assertThat(jdbcRuntime.queryForObject("select current_user", new MapSqlParameterSource(), String.class))
                .isEqualTo(ConexionRuntimeTest.USUARIO);
        assertThat(contar("runtime-jdbc-1")).isEqualTo(1);
    }

    @Test
    void elRepositorioGuardaElDetalleJsonBConsultablePorElRolRuntime() {
        repositorio.guardar(registro("runtime-jdbc-2"));

        String detalle = jdbcRuntime.queryForObject(
                "select detalle::text from evento_auditoria where entidad_id = :id",
                new MapSqlParameterSource("id", "runtime-jdbc-2"), String.class);
        assertThat(detalle).contains("\"origen\"").contains("integracion");
    }

    @Test
    void elRolRuntimeNoPuedeModificarLoQueInsertoElRepositorio() {
        repositorio.guardar(registro("runtime-jdbc-3"));

        assertThatThrownBy(() -> jdbcRuntime.update(
                "update evento_auditoria set accion = 'EXPORTAR' where entidad_id = :id",
                new MapSqlParameterSource("id", "runtime-jdbc-3")))
                .isInstanceOf(DataAccessException.class)
                .satisfies(t -> assertThat(t.getCause()).hasMessageContaining("permission denied"));
        assertThatThrownBy(() -> jdbcRuntime.update(
                "delete from evento_auditoria where entidad_id = :id",
                new MapSqlParameterSource("id", "runtime-jdbc-3")))
                .isInstanceOf(DataAccessException.class)
                .satisfies(t -> assertThat(t.getCause()).hasMessageContaining("permission denied"));
        assertThat(contar("runtime-jdbc-3")).isEqualTo(1);
    }

    private RegistroAuditoria registro(String id) {
        return new RegistroAuditoria(AccionAuditoria.LOGIN, TipoActor.SISTEMA, null, "SISTEMA", id,
                Map.of("origen", "integracion"), null);
    }

    private Integer contar(String id) {
        return jdbcRuntime.queryForObject("select count(*) from evento_auditoria where entidad_id = :id",
                new MapSqlParameterSource("id", id), Integer.class);
    }
}
