package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogsJsonSegurosTest {

    @Test
    void laConfiguracionUsaFormatoJsonEstructurado() throws Exception {
        String configuracion = Files.readString(Path.of("src/main/resources/application.yml").toAbsolutePath());
        assertThat(configuracion).contains("structured:")
                .contains("console: logstash")
                .contains("file: logstash");
    }

    @Test
    void elFiltroConservaSoloMetadatosTecnicosPermitidos() {
        Map<String, String> filtrado = FiltroMdcLogs.conservar(Map.of(
                "request_id", "r-1",
                "auditoria.entidad", "USUARIO",
                "consultorio_slug", "demo_uno",
                "entidad_id", "paciente-9",
                "telefono", "123456789",
                "detalle_clinico", "diagnostico"));

        assertThat(filtrado).containsOnlyKeys(
                "request_id", "auditoria.entidad", "consultorio_slug");
    }

    @Test
    void elFiltroConservaCorrelacionYDescartaPiiIncrustada() {
        Map<String, String> filtrado = FiltroMdcLogs.conservar(Map.of(
                "auditoria.correlacion", "c-123",
                "paciente.nombre", "Ana",
                "paciente.dni", "12345678"));

        assertThat(filtrado).containsExactly(Map.entry("auditoria.correlacion", "c-123"));
    }
}
