package ar.edu.utn.odonto.auditoria.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.utn.odonto.auditoria.application.AccionAuditoria;
import ar.edu.utn.odonto.auditoria.application.AuditoriaService;
import ar.edu.utn.odonto.auditoria.application.RegistroAuditoria;
import ar.edu.utn.odonto.auditoria.application.TipoActor;
import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Adaptador JDBC y transacción REQUIRES_NEW sobre PostgreSQL real (C-02, D3/D4). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class RepositorioAuditoriaTest {

    @Autowired
    AuditoriaService servicio;

    @Autowired
    DataSource dataSource;

    @Autowired
    PlatformTransactionManager transacciones;

    @Test
    void laFilaSePersisteConLosTiposPostgresCorrectos() {
        servicio.registrar(new RegistroAuditoria(AccionAuditoria.EXPORTAR, TipoActor.PACIENTE, "p-9",
                "HISTORIA", "h-9", Map.of("entrada_id", "e-9"), "2001:db8::1"));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, Object> fila = jdbc.queryForMap(
                "select pg_typeof(detalle)::text as tipo_detalle, pg_typeof(ip)::text as tipo_ip, host(ip)::text as ip "
                        + "from evento_auditoria where entidad_id = 'h-9'");

        assertThat(fila.get("TIPO_DETALLE")).isEqualTo("jsonb");
        assertThat(fila.get("TIPO_IP")).isEqualTo("inet");
        assertThat(fila.get("IP")).isEqualTo("2001:db8::1");
    }

    @Test
    void laAuditoriaSobreviveAlRollbackDeLaOperacionQueLaEnvuelve() {
        TransactionTemplate plantilla = new TransactionTemplate(transacciones);

        assertThatThrownBy(() -> plantilla.executeWithoutResult(estado -> {
            servicio.registrar(new RegistroAuditoria(AccionAuditoria.ANULAR_ENTIDAD, TipoActor.USUARIO, "u-8",
                    "TURNO", "t-8", Map.of("turno_id", "t-8"), null));
            throw new IllegalStateException("rollback de la operacion de negocio");
        })).isInstanceOf(IllegalStateException.class);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject(
                "select count(*) from evento_auditoria where entidad_id = 't-8'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void unSegundoRegistroConOtraAccionSePersisteSinConflicto() {
        servicio.registrar(new RegistroAuditoria(AccionAuditoria.VER_HISTORIA, TipoActor.USUARIO, "u-5",
                "HISTORIA", "h-5", Map.of("historia_id", "h-5"), "200.24.18.7"));
        servicio.registrar(new RegistroAuditoria(AccionAuditoria.MODIFICAR_ENTIDAD, TipoActor.USUARIO, "u-5",
                "HISTORIA", "h-5", Map.of("historia_id", "h-5"), null));

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList(
                "select accion from evento_auditoria where entidad_id = 'h-5' order by id", String.class))
                .containsExactly("VER_HISTORIA", "MODIFICAR_ENTIDAD");
    }
}
