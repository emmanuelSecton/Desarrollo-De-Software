package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LimitesRecursosTest {

    @Test
    void losCuatroServiciosTienenCpuYMemoriaYElPoolEstaAcotado() throws Exception {
        JsonNode servicios = configuracion().get("services");
        for (String nombre : new String[] {"app_demo_uno", "app_demo_dos", "postgres", "migrate_demo_uno", "caddy"}) {
            JsonNode servicio = servicios.get(nombre);
            assertThat(servicio.has("cpus")).as(nombre).isTrue();
            assertThat(servicio.get("cpus").asDouble()).as(nombre).isGreaterThan(0.0);
            assertThat(servicio.has("mem_limit")).as(nombre).isTrue();
            assertThat(servicio.get("mem_limit").asLong()).as(nombre).isGreaterThan(0L);
        }
        assertThat(servicios.get("app_demo_uno").get("environment").get("DB_POOL_MAX_SIZE").asText()).isEqualTo("5");
        assertThat(servicios.get("app_demo_dos").get("environment").get("DB_POOL_MAX_SIZE").asText()).isEqualTo("5");
    }

    @Test
    void unConsumoExcesivoNoDesplazaALaOtraInstancia() throws Exception {
        JsonNode servicios = configuracion().get("services");
        long limiteUno = servicios.get("app_demo_uno").get("mem_limit").asLong();
        long limiteDos = servicios.get("app_demo_dos").get("mem_limit").asLong();
        assertThat(limiteUno).isEqualTo(limiteDos);
        assertThat(limiteUno).isLessThanOrEqualTo(805306368L);
        assertThat(servicios.get("postgres").get("cpus").asDouble()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void existeUnVerificadorDeLimites() throws Exception {
        Path verificador = Path.of("..", "deploy", "bin", "verificar-limites.sh")
                .toAbsolutePath()
                .normalize();
        assertThat(verificador).exists();
        String contenido = Files.readString(verificador);
        assertThat(contenido).contains("cpus")
                .contains("mem_limit")
                .contains("DB_POOL_MAX_SIZE")
                .contains("caddy")
                .contains("migrate_demo_uno");
    }

    private static JsonNode configuracion() throws Exception {
        ProcessBuilder proceso = new ProcessBuilder(
                "docker", "compose", "-f", "deploy/docker-compose.yml", "config", "--format", "json");
        proceso.directory(Path.of("..").toAbsolutePath().normalize().toFile());
        Process resultado = proceso.start();
        StringBuilder salida = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(resultado.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                salida.append(linea);
            }
        }
        assertThat(resultado.waitFor()).isZero();
        return new ObjectMapper().readTree(salida.toString());
    }
}
