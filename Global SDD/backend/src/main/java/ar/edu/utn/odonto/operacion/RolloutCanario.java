package ar.edu.utn.odonto.operacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RolloutCanario {

    private static final List<String> CANARIOS = List.of("demo_uno", "demo_dos");

    private RolloutCanario() {}

    public interface Port {
        void desplegar(String slug);

        boolean salud(String slug);

        void migrar(String slug);
    }

    public record Resultado(boolean correcto, String motivo) {}

    public static Resultado ejecutar(List<String> slugs, Port port) {
        for (String canario : CANARIOS) {
            port.desplegar(canario);
        }
        Map<String, Boolean> salud = new LinkedHashMap<>();
        for (String canario : CANARIOS) {
            salud.put(canario, port.salud(canario));
        }
        List<String> destinos = new ArrayList<>();
        for (String slug : slugs) {
            if (!CANARIOS.contains(slug)) {
                destinos.add(slug);
            }
        }
        PlanRollout.Resultado evaluado = PlanRollout.evaluar(salud, destinos);
        if (!evaluado.continuar()) {
            RegistroAlertas.alertar(TipoAlerta.APP_DOWN);
            return new Resultado(false, evaluado.motivo());
        }
        evaluado.destinos().forEach(port::migrar);
        return new Resultado(true, "rollout completado tras validar los dos canarios");
    }

    public static void main(String[] args) {
        System.exit(ejecutar(new String[] {System.getenv("CONSULTORIO_SLUGS")},
                dockerPort(System.getenv().getOrDefault("ODONTO_URL_BASE", "http://localhost:8080"),
                        ProcesadorComandos.real())));
    }

    public static int ejecutar(String[] args, Port port) {
        String slugs = args.length == 1 ? args[0] : null;
        if (slugs == null || slugs.isBlank()) {
            System.err.println("falta CONSULTORIO_SLUGS");
            return 1;
        }
        String imagen = System.getenv("ODONTO_IMAGEN");
        if (imagen != null && !imagen.isBlank() && !ReferenciaImagen.esValida(imagen)) {
            System.err.println("ODONTO_IMAGEN no es una referencia versionada");
            return 1;
        }
        Resultado resultado = ejecutar(List.of(slugs.trim().split("\\s+")), port);
        System.out.println(resultado.motivo());
        return resultado.correcto() ? 0 : 1;
    }

    static Port dockerPort(String baseUrl, ProcesadorComandos procesador) {
        return new Port() {
            @Override
            public void desplegar(String slug) {
                ProcessBuilder constructor = new ProcessBuilder("docker", "compose", "-f",
                        "deploy/docker-compose.yml", "up", "-d", "--no-build", "app_" + slug);
                String imagen = System.getenv("ODONTO_IMAGEN");
                if (imagen != null && !imagen.isBlank()) {
                    constructor.environment().put("ODONTO_IMAGEN", imagen);
                }
                procesador.ejecutar(constructor, null);
            }

            @Override
            public boolean salud(String slug) {
                try {
                    HttpRequest peticion = HttpRequest.newBuilder(URI.create(baseUrl
                                    + "/consultorio/" + slug + "/actuator/health"))
                            .GET()
                            .build();
                    String cuerpo = HttpClient.newHttpClient()
                            .send(peticion, BodyHandlers.ofString())
                            .body();
                    return "UP".equals(new ObjectMapper().readTree(cuerpo).path("status").asText());
                } catch (Exception fallo) {
                    return false;
                }
            }

            @Override
            public void migrar(String slug) {
                procesador.ejecutar(new ProcessBuilder("deploy/bin/migrar-base.sh", slug), null);
            }
        };
    }

    static void ejecutar(ProcessBuilder constructor, ProcesadorComandos procesador) {
        procesador.ejecutar(constructor, null);
    }
}
