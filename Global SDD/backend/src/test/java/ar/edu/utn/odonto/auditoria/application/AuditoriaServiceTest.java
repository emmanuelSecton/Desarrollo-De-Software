package ar.edu.utn.odonto.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Registro con reloj fijo y transacción independiente (C-02, D3/D4). */
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgresTestConfig.class, AuditoriaServiceTest.RelojFijo.class})
class AuditoriaServiceTest {

    static final Instant INSTANTE = Instant.parse("2026-11-24T13:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class RelojFijo {
        @Bean
        @Primary
        Clock clockFijo() {
            return Clock.fixed(INSTANTE, ZoneOffset.UTC);
        }
    }

    @Autowired
    AuditoriaService servicio;

    @Autowired
    DataSource dataSource;

    @Test
    void elEventoSePersisteConElInstanteDelRelojInyectado() {
        servicio.registrar(new RegistroAuditoria(AccionAuditoria.CREAR_TURNO, TipoActor.USUARIO, "u-1",
                "TURNO", "t-1", Map.of("turno_id", "t-1"), "200.24.18.7"));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> fila = jdbc.queryForMap(
                "select ocurrido_at, actor_tipo, accion, entidad_id, detalle::text, host(ip)::text as ip "
                        + "from evento_auditoria where entidad_id = 't-1'");

        assertThat(fila.get("OCURRIDO_AT")).isEqualTo(Timestamp.from(INSTANTE));
        assertThat(fila.get("ACTOR_TIPO")).isEqualTo("USUARIO");
        assertThat(fila.get("ACCION")).isEqualTo("CREAR_TURNO");
        assertThat(fila.get("DETALLE").toString()).contains("turno_id");
        assertThat(fila.get("IP")).isEqualTo("200.24.18.7");
    }

    @Test
    void unEventoDelSistemaSinIpSePersisteIgualmente() {
        servicio.registrar(new RegistroAuditoria(AccionAuditoria.LOGIN_FALLIDO, TipoActor.SISTEMA, null,
                "USUARIO", "u-2", Map.of(), null));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> fila = jdbc.queryForMap(
                "select actor_id, ip, detalle::text from evento_auditoria where entidad_id = 'u-2'");

        assertThat(fila.get("ACTOR_ID")).isNull();
        assertThat(fila.get("IP")).isNull();
        assertThat(fila.get("DETALLE").toString()).isEqualTo("{}");
    }

    @Test
    void unRegistroInvalidoNoPersisteNada() {
        assertThatThrownBy(() -> servicio.registrar(new RegistroAuditoria(AccionAuditoria.EXPORTAR,
                TipoActor.USUARIO, "u-3", "CAJA", "c-3", Map.of("diagnostico", "x"), null)))
                .isInstanceOf(AuditoriaInvalidaException.class);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject(
                "select count(*) from evento_auditoria where entidad_id = 'c-3'", Integer.class))
                .isZero();
    }
}
