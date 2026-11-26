package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RolloutCanarioEfectivoTest {

    @Test
    void despliegaLosDosCanariosAntesDeVerificarYSoloLuegoMigraElResto() throws Exception {
        Map<String, Integer> estados = Map.of("demo_uno", 200, "demo_dos", 200);
        try (ServidorSalud servidor = new ServidorSalud(estados)) {
            List<String> eventos = new ArrayList<>();
            RolloutCanario.Port port = port(servidor.url(), eventos);

            RolloutCanario.Resultado resultado = RolloutCanario.ejecutar(
                    List.of("demo_uno", "demo_dos", "consultorio_tres", "consultorio_cuatro"), port);

            assertThat(resultado.correcto()).isTrue();
            assertThat(eventos).containsExactly(
                    "desplegar:demo_uno", "desplegar:demo_dos",
                    "health:demo_uno", "health:demo_dos",
                    "migrar:consultorio_tres", "migrar:consultorio_cuatro");
        }
    }

    @Test
    void unaCanariaDownDetieneAntesDeMigrarPeroYaDesplegoAmbas() throws Exception {
        Map<String, Integer> estados = Map.of("demo_uno", 200, "demo_dos", 503);
        try (ServidorSalud servidor = new ServidorSalud(estados)) {
            List<String> eventos = new ArrayList<>();

            RolloutCanario.Resultado resultado = RolloutCanario.ejecutar(
                    List.of("demo_uno", "demo_dos", "consultorio_tres"), port(servidor.url(), eventos));

            assertThat(resultado.correcto()).isFalse();
            assertThat(resultado.motivo()).contains("demo_dos");
            assertThat(eventos).containsExactly(
                    "desplegar:demo_uno", "desplegar:demo_dos",
                    "health:demo_uno", "health:demo_dos");
        }
    }

    private static RolloutCanario.Port port(String baseUrl, List<String> eventos) {
        return new RolloutCanario.Port() {
            @Override
            public void desplegar(String slug) {
                eventos.add("desplegar:" + slug);
            }

            @Override
            public boolean salud(String slug) {
                eventos.add("health:" + slug);
                try {
                    HttpRequest peticion = HttpRequest.newBuilder(
                                    URI.create(baseUrl + "/consultorio/" + slug + "/actuator/health"))
                            .GET()
                            .build();
                    String cuerpo = HttpClient.newHttpClient()
                            .send(peticion, BodyHandlers.ofString())
                            .body();
                    return "UP".equals(new ObjectMapper().readTree(cuerpo).path("status").asText());
                } catch (Exception fallo) {
                    throw new IllegalStateException(fallo);
                }
            }

            @Override
            public void migrar(String slug) {
                eventos.add("migrar:" + slug);
            }
        };
    }

    private static final class ServidorSalud implements AutoCloseable {
        private final HttpServer servidor;

        private ServidorSalud(Map<String, Integer> estados) throws IOException {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/consultorio/", intercambio -> {
                String slug = intercambio.getRequestURI().getPath().split("/")[2];
                int estado = estados.getOrDefault(slug, 503);
                byte[] cuerpo = ("{\"status\":\"" + (estado == 200 ? "UP" : "DOWN") + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                intercambio.sendResponseHeaders(estado, cuerpo.length);
                intercambio.getResponseBody().write(cuerpo);
                intercambio.close();
            });
            servidor.start();
        }

        private String url() {
            return "http://127.0.0.1:" + servidor.getAddress().getPort();
        }

        @Override
        public void close() {
            servidor.stop(0);
        }
    }
}
