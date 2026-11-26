package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ImagenVersionadaTest {

    @Test
    void aceptaTagsSemanticosYDigestPeroRechazaLatest() {
        assertThat(ReferenciaImagen.esValida("odonto:1.2.3")).isTrue();
        assertThat(ReferenciaImagen.esValida("odonto:1.2.3@sha256:"
                + "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
                .isTrue();
        assertThat(ReferenciaImagen.esValida("odonto:latest")).isFalse();
        assertThat(ReferenciaImagen.esValida("odonto:dev")).isFalse();
        assertThat(ReferenciaImagen.esValida("")).isFalse();
    }

    @Test
    void todasLasInstanciasInformanLaMismaImagenFijada() throws Exception {
        JsonNode servicios = configuracion().get("services");
        Set<String> imagenes = new LinkedHashSet<>();
        for (String nombre : new String[] {"app", "app_demo_uno", "app_demo_dos"}) {
            String imagen = servicios.get(nombre).get("image").asText();
            assertThat(ReferenciaImagen.esValida(imagen)).as(nombre).isTrue();
            imagenes.add(imagen);
        }
        assertThat(imagenes).hasSize(1);
        assertThat(imagenes.iterator().next()).doesNotContain("latest");
    }

    @Test
    void elAltaUsaLaMismaReferenciaPorDefecto() throws Exception {
        AltaConsultorio.PlanAlta plan = AltaConsultorio.planificar(
                "demo_tres", "demo.example.com", Path.of("."), java.util.Map.of());
        assertThat(ReferenciaImagen.esValida(plan.variables().get("ODONTO_IMAGEN"))).isTrue();
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
