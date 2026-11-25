package ar.edu.utn.odonto.auditoria.infrastructure;

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

/** Esquema exacto de auditoria sobre PostgreSQL 16 real (C-02, D1). */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
class EsquemaAuditoriaTest {

    @Autowired
    DataSource dataSource;

    @Test
    void lasColumnasTienenLosTiposAcordadosYOCurridoAtSinDefault() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<Map<String, Object>> columnas = columnas();

        assertThat(columnas).extracting(c -> c.get("COLUMN_NAME").toString())
                .containsExactlyInAnyOrder("id", "ocurrido_at", "actor_tipo", "actor_id", "accion", "entidad",
                        "entidad_id", "detalle", "ip");

        assertThat(tipo(columnas, "id")).isEqualTo("bigint");
        assertThat(tipo(columnas, "ocurrido_at")).isEqualTo("timestamp with time zone");
        assertThat(tipo(columnas, "actor_tipo")).isEqualTo("character varying");
        assertThat(tipo(columnas, "actor_id")).isEqualTo("character varying");
        assertThat(tipo(columnas, "accion")).isEqualTo("character varying");
        assertThat(columnas).filteredOn(c -> "accion".equals(c.get("COLUMN_NAME")))
                .extracting(c -> c.get("CHARACTER_MAXIMUM_LENGTH"))
                .containsExactly(60);
        assertThat(tipo(columnas, "entidad")).isEqualTo("character varying");
        assertThat(tipo(columnas, "entidad_id")).isEqualTo("character varying");
        assertThat(tipo(columnas, "detalle")).isEqualTo("jsonb");
        assertThat(tipo(columnas, "ip")).isEqualTo("inet");

        assertThat(columnas).filteredOn(c -> "ocurrido_at".equals(c.get("COLUMN_NAME")))
                .extracting(c -> c.get("COLUMN_DEFAULT"))
                .containsOnlyNulls();
    }

    @Test
    void losCuatroIndicesDeConsultaEstanPresentes() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> definiciones = jdbc.queryForList(
                "select indexdef from pg_indexes where tablename = 'evento_auditoria'", String.class);

        assertThat(definiciones).anySatisfy(d -> assertThat(d).contains("(ocurrido_at DESC)"));
        assertThat(definiciones).anySatisfy(d -> assertThat(d).contains("(actor_tipo, actor_id)"));
        assertThat(definiciones).anySatisfy(d -> assertThat(d).contains("(accion)"));
        assertThat(definiciones).anySatisfy(d -> assertThat(d).contains("(entidad, entidad_id)"));
    }

    @Test
    void laAccionNoEsUnEnumDeBase() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<String> enums = jdbc.queryForList(
                "select t.typname from pg_type t join pg_enum e on e.enumtypid = t.oid "
                        + "where t.typname like '%accion%' or t.typname like '%auditoria%'",
                String.class);

        assertThat(enums).isEmpty();
    }

    private List<Map<String, Object>> columnas() {
        return new JdbcTemplate(dataSource).queryForList(
                "select column_name, data_type, character_maximum_length, column_default "
                        + "from information_schema.columns where table_name = 'evento_auditoria'");
    }

    private static String tipo(List<Map<String, Object>> columnas, String nombre) {
        return columnas.stream()
                .filter(c -> nombre.equals(c.get("COLUMN_NAME")))
                .map(c -> c.get("DATA_TYPE").toString())
                .findFirst()
                .orElseThrow();
    }
}
