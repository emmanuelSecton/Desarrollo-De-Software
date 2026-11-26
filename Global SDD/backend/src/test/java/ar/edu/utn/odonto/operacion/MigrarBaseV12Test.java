package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrarBaseV12Test {

    @TempDir
    Path temporal;

    @Test
    void elUsoEstaticoMantieneElComposeBase() throws Exception {
        List<String> lineas = ejecutar("migra_uno", temporal.resolve("estatico.log"), null);

        assertThat(lineas).hasSize(3);
        assertThat(lineas.get(0)).contains("compose -f deploy/docker-compose.yml config --format json");
        assertThat(lineas.get(1)).contains("compose -f deploy/docker-compose.yml run --rm migrate_migra_uno");
        assertThat(lineas.get(2)).contains("compose -f deploy/docker-compose.yml up -d --no-build app_migra_uno");
    }

    @Test
    void elComposeGeneradoSeUsaEnConfiguracionMigracionYArranque() throws Exception {
        Path compose = temporal.resolve("clinica_22.compose.yml");
        Files.writeString(compose, "services: {}\n");
        List<String> lineas = ejecutar("clinica_22", temporal.resolve("dinamico.log"), compose);

        assertThat(lineas).hasSize(3);
        for (String linea : lineas) {
            assertThat(linea).contains("-f deploy/docker-compose.yml").contains("-f " + rutaBash(compose));
        }
        assertThat(lineas.get(1)).contains("run --rm migrate_clinica_22");
        assertThat(lineas.get(2)).contains("up -d --no-build app_clinica_22");
    }

    private List<String> ejecutar(String slug, Path registro, Path compose) throws Exception {
        Path envoltorio = temporal.resolve("envoltorio_" + registro.getFileName() + ".sh");
        Files.writeString(envoltorio, "#!/bin/bash\n"
                + "registro=\"$1\"\n"
                + "guion=\"$2\"\n"
                + "shift 2\n"
                + "docker() {\n"
                + "  printf '%s\\n' \"$*\" >> \"$registro\"\n"
                + "  case \"$*\" in\n"
                + "    *config*) echo 'flyway/flyway:11.7.2 FLYWAY_PLACEHOLDERS_RUNTIME_ROLE APP_DB_USER service_completed_successfully';;\n"
                + "  esac\n"
                + "}\n"
                + "source \"$guion\" \"$@\"\n");

        List<String> argumentos = new ArrayList<>();
        argumentos.add("bash");
        argumentos.add(rutaBash(envoltorio));
        argumentos.add(rutaBash(registro));
        argumentos.add(rutaBash(Path.of("..", "deploy", "bin", "migrar-base.sh")));
        argumentos.add(slug);
        if (compose != null) {
            argumentos.add(rutaBash(compose));
        }
        ProcessBuilder proceso = new ProcessBuilder(argumentos);
        proceso.directory(Path.of("..").toAbsolutePath().normalize().toFile());

        Process resultado = proceso.start();
        StringBuilder error = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(resultado.getErrorStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                error.append(linea);
            }
        }
        assertThat(resultado.waitFor()).as(error.toString()).isZero();
        return Files.readAllLines(registro);
    }

    private static String rutaBash(Path ruta) {
        String texto = ruta.toAbsolutePath().normalize().toString().replace('\\', '/');
        if (texto.matches("^[A-Za-z]:/.*")) {
            return "/mnt/" + Character.toLowerCase(texto.charAt(0)) + texto.substring(2);
        }
        return texto;
    }
}
