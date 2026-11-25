package ar.edu.utn.odonto.auditoria.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.odonto.support.PostgresTestConfig;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Contrato publico de auditoria y catálogo extensible sin migracion (C-02, D3). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class ContratoAuditoriaTest {

    @Autowired
    DataSource dataSource;

    @Test
    void elCatalogoContieneLasAccionesAcordadasYBajo60Caracteres() {
        assertThat(AccionAuditoria.values()).extracting(AccionAuditoria::valor)
                .contains("LOGIN", "LOGIN_FALLIDO", "VER_HISTORIA", "CREAR_TURNO", "EXPORTAR", "CREAR_ENTIDAD",
                        "MODIFICAR_ENTIDAD", "ANULAR_ENTIDAD");
        assertThat(AccionAuditoria.values())
                .allSatisfy(a -> assertThat(a.valor()).hasSizeLessThanOrEqualTo(60));
        assertThat(TipoActor.values()).extracting(Enum::name).containsExactlyInAnyOrder(
                "SISTEMA", "USUARIO", "PACIENTE");
    }

    @Test
    void unaAccionNuevaSePersisteSinAlterarElTipoDeLaColumna() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String accionNueva = "ACCION_NUEVA_SIN_MIGRACION";

        jdbc.update("insert into evento_auditoria "
                        + "(ocurrido_at, actor_tipo, actor_id, accion, entidad, entidad_id, detalle) "
                        + "values ('2026-11-24T13:00:00Z', 'SISTEMA', null, ?, 'USUARIO', 'u-1', '{}'::jsonb)",
                accionNueva);

        List<String> acciones = jdbc.queryForList(
                "select accion from evento_auditoria where accion = ?", String.class, accionNueva);
        assertThat(acciones).containsExactly(accionNueva);

        Integer longitud = jdbc.queryForObject(
                "select character_maximum_length from information_schema.columns "
                        + "where table_name = 'evento_auditoria' and column_name = 'accion'", Integer.class);
        assertThat(longitud).isEqualTo(60);
    }

    @Test
    void elRegistroPublicoLlevaLosCamposDelContrato() {
        RegistroAuditoria registro = new RegistroAuditoria(
                AccionAuditoria.VER_HISTORIA, TipoActor.USUARIO, "u-7", "HISTORIA", "h-7",
                Map.of("historia_id", "h-7"), null);

        assertThat(registro.accion().valor()).isEqualTo("VER_HISTORIA");
        assertThat(registro.tipoActor()).isEqualTo(TipoActor.USUARIO);
        assertThat(registro.actorId()).isEqualTo("u-7");
        assertThat(registro.entidad()).isEqualTo("HISTORIA");
        assertThat(registro.entidadId()).isEqualTo("h-7");
        assertThat(registro.detalle()).containsEntry("historia_id", "h-7");
        assertThat(registro.ip()).isNull();
    }
}
