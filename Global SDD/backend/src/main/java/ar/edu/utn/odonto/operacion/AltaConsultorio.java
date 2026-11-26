package ar.edu.utn.odonto.operacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class AltaConsultorio {

    private static final Pattern SLUG = Pattern.compile("^[a-z][a-z0-9_]{1,30}$");

    private AltaConsultorio() {}

    public record PlanAlta(
            String slug,
            String base,
            String migrador,
            String runtime,
            String subdominio,
            Path envFile,
            List<String> comandos,
            Path composeFile,
            Map<String, String> variables) {}

    public static PlanAlta planificar(String slug, String dominio, Path directorioEnv, Map<String, String> secretos) {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            throw new IllegalArgumentException("slug invalido: " + slug);
        }
        if (dominio == null || dominio.isBlank()) {
            throw new IllegalArgumentException("dominio obligatorio");
        }
        String base = "consultorio_" + slug;
        String migrador = base + "_migrator";
        String runtime = base + "_runtime";
        String subdominio = slug.replace('_', '-') + "." + dominio;
        Path envFile = directorioEnv.resolve(slug + ".env");
        Path composeFile = directorioEnv.resolve(slug + ".compose.yml");
        String upper = slug.toUpperCase(java.util.Locale.ROOT);

        Map<String, String> variables = new TreeMap<>();
        variables.put("ADMIN_INITIAL_EMAIL", valor(secretos, "ADMIN_INITIAL_EMAIL"));
        variables.put("APP_BASE_URL", "https://" + subdominio);
        variables.put("APP_DB_MIGRATOR_USER", migrador);
        variables.put("APP_DB_NAME", base);
        variables.put("APP_DB_PASSWORD", valor(secretos, "APP_DB_PASSWORD"));
        variables.put("APP_DB_USER", runtime);
        variables.put("CONSULTORIO_" + upper + "_MIGRATOR_PASSWORD", valor(secretos, "APP_DB_MIGRATOR_PASSWORD"));
        variables.put("CONSULTORIO_" + upper + "_RUNTIME_PASSWORD", valor(secretos, "APP_DB_PASSWORD"));
        variables.put("DB_PASSWORD", valor(secretos, "APP_DB_PASSWORD"));
        variables.put("DB_POOL_MAX_SIZE", "5");
        variables.put("DB_URL", "jdbc:postgresql://postgres:5432/" + base);
        variables.put("DB_USER", runtime);
        variables.put("ODONTO_IMAGEN", valor(secretos, "ODONTO_IMAGEN", "odonto:1.0.0"));
        variables.put("POSTGRES_PASSWORD", valor(secretos, "POSTGRES_PASSWORD"));
        variables.put("WA_VERIFY_TOKEN", "");

        List<String> comandos = new ArrayList<>();
        comandos.add(String.join(" ", comandoCompose(envFile, composeFile,
                "run", "--rm", "migrate_" + slug)));
        comandos.add(String.join(" ", comandoCompose(envFile, composeFile,
                "up", "-d", "--no-build", "app_" + slug)));
        comandos.add("curl -fsS https://" + subdominio + "/actuator/health");

        return new PlanAlta(slug, base, migrador, runtime, subdominio, envFile,
                List.copyOf(comandos), composeFile, Map.copyOf(variables));
    }

    public static Path escribirEnv(PlanAlta plan) throws IOException {
        Path destino = plan.envFile();
        Files.createDirectories(destino.getParent());
        StringBuilder contenido = new StringBuilder();
        for (Map.Entry<String, String> entrada : new TreeMap<>(plan.variables()).entrySet()) {
            contenido.append(entrada.getKey()).append('=').append(entrada.getValue()).append('\n');
        }
        Files.writeString(destino, contenido.toString(), StandardCharsets.UTF_8);
        return destino;
    }

    public static Path escribirCompose(PlanAlta plan) throws IOException {
        Path destino = plan.composeFile();
        Files.createDirectories(destino.getParent());
        String slug = plan.slug();
        String upper = slug.toUpperCase(Locale.ROOT);
        String envFile = plan.envFile().toAbsolutePath().toString().replace('\\', '/');
        String contenido = "services:\n"
                + "  migrate_" + slug + ":\n"
                + "    image: flyway/flyway:11.7.2\n"
                + "    environment:\n"
                + "      FLYWAY_URL: jdbc:postgresql://postgres:5432/" + plan.base() + "\n"
                + "      FLYWAY_USER: " + plan.migrador() + "\n"
                + "      FLYWAY_PASSWORD: ${CONSULTORIO_" + upper + "_MIGRATOR_PASSWORD:-}\n"
                + "      FLYWAY_LOCATIONS: filesystem:/flyway/sql\n"
                + "      FLYWAY_PLACEHOLDERS_RUNTIME_ROLE: " + plan.runtime() + "\n"
                + "    command: migrate\n"
                + "    volumes:\n"
                + "      - ../backend/src/main/resources/db/migration:/flyway/sql:ro\n"
                + "    restart: \"no\"\n"
                + "    depends_on:\n"
                + "      postgres:\n"
                + "        condition: service_healthy\n"
                + "    security_opt:\n"
                + "      - no-new-privileges:true\n"
                + "    cpus: 0.25\n"
                + "    mem_limit: 256m\n"
                + "    networks:\n"
                + "      - backend\n"
                + "  app_" + slug + ":\n"
                + "    image: " + plan.variables().get("ODONTO_IMAGEN") + "\n"
                + "    env_file:\n"
                + "      - \"" + envFile + "\"\n"
                + "    environment:\n"
                + "      SPRING_FLYWAY_ENABLED: \"false\"\n"
                + "    labels:\n"
                + "      odonto.upstream: \"app_" + slug + ":8080\"\n"
                + "    depends_on:\n"
                + "      postgres:\n"
                + "        condition: service_healthy\n"
                + "      migrate_" + slug + ":\n"
                + "        condition: service_completed_successfully\n"
                + "    security_opt:\n"
                + "      - no-new-privileges:true\n"
                + "    mem_limit: 768m\n"
                + "    cpus: 1.0\n"
                + "    networks:\n"
                + "      - backend\n";
        Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        return destino;
    }

    @FunctionalInterface
    public interface ConsultaSalud {
        String consultar(String url) throws Exception;
    }

    public interface Port {
        void sql(String sentencia);

        void sqlEnBase(String base, String sentencia);

        boolean consultaBooleana(String consulta);
    }

    public record Resultado(boolean correcto, String mensaje) {}

    public static Resultado aplicar(PlanAlta plan, String healthUrl, Port port, Path caddyFile) throws Exception {
        return aplicar(plan, healthUrl, port, caddyFile, ProcesadorComandos.real(), AltaConsultorio::verificarSalud);
    }

    public static Resultado aplicar(PlanAlta plan, String healthUrl, Port port, Path caddyFile,
            ProcesadorComandos procesador, ConsultaSalud consultaSalud) throws Exception {
        escribirEnv(plan);
        if (!port.consultaBooleana("select exists(select from pg_roles where rolname='"
                + plan.migrador() + "')::text")) {
            port.sql("CREATE ROLE " + plan.migrador() + " LOGIN PASSWORD "
                    + literal(plan.variables().get("CONSULTORIO_"
                            + plan.slug().toUpperCase(Locale.ROOT) + "_MIGRATOR_PASSWORD")) + ";");
        }
        if (!port.consultaBooleana("select exists(select from pg_roles where rolname='"
                + plan.runtime() + "')::text")) {
            port.sql("CREATE ROLE " + plan.runtime() + " LOGIN PASSWORD "
                    + literal(plan.variables().get("APP_DB_PASSWORD")) + ";");
        }
        if (!port.consultaBooleana("select exists(select from pg_database where datname='"
                + plan.base() + "')::text")) {
            port.sql("CREATE DATABASE " + plan.base() + " OWNER " + plan.migrador() + ";");
        }
        port.sql("REVOKE CONNECT ON DATABASE " + plan.base() + " FROM PUBLIC;");
        port.sql("GRANT CONNECT ON DATABASE " + plan.base() + " TO " + plan.runtime() + ";");
        port.sqlEnBase(plan.base(), "GRANT USAGE ON SCHEMA public TO " + plan.runtime() + ";");
        port.sqlEnBase(plan.base(), "ALTER DEFAULT PRIVILEGES FOR ROLE " + plan.migrador()
                + " IN SCHEMA public GRANT SELECT, INSERT ON TABLES TO " + plan.runtime() + ";");
        port.sqlEnBase(plan.base(), "ALTER DEFAULT PRIVILEGES FOR ROLE " + plan.migrador()
                + " IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO " + plan.runtime() + ";");

        agregarBloqueCaddy(caddyFile, plan);
        Path compose = escribirCompose(plan);
        ProcessBuilder migracion = new ProcessBuilder(comandoCompose(plan.envFile(), compose,
                "run", "--rm", "migrate_" + plan.slug()));
        migracion.environment().put("ODONTO_IMAGEN", plan.variables().get("ODONTO_IMAGEN"));
        procesador.ejecutar(migracion, null);
        ProcessBuilder levantamiento = new ProcessBuilder(comandoCompose(plan.envFile(), compose,
                "up", "-d", "--no-build", "app_" + plan.slug()));
        levantamiento.environment().put("ODONTO_IMAGEN", plan.variables().get("ODONTO_IMAGEN"));
        procesador.ejecutar(levantamiento, null);

        JsonNode salud = new ObjectMapper().readTree(consultaSalud.consultar(healthUrl));
        String estado = salud.path("status").asText("");
        return "UP".equals(estado)
                ? new Resultado(true, "alta de " + plan.slug() + " aplicada; health UP")
                : new Resultado(false, "alta de " + plan.slug() + " aplicada; health " + estado);
    }

    static void agregarBloqueCaddy(Path caddyFile, PlanAlta plan) throws IOException {
        Files.createDirectories(caddyFile.getParent());
        String actual = Files.exists(caddyFile) ? Files.readString(caddyFile, StandardCharsets.UTF_8) : "";
        String host = plan.slug().replace('_', '-') + ".{$ODONTO_DOMINIO}";
        if (actual.contains(host + " {")) {
            return;
        }
        String bloque = host + " {\n  reverse_proxy app_" + plan.slug() + ":8080\n}\n";
        String separador = actual.isEmpty() || actual.endsWith("\n") ? "" : "\n";
        Files.writeString(caddyFile, actual + separador + bloque + "\n", StandardCharsets.UTF_8);
    }

    private static List<String> comandoCompose(Path envFile, Path compose, String... argumentos) {
        List<String> comando = new ArrayList<>();
        comando.add("docker");
        comando.add("compose");
        comando.add("--env-file");
        comando.add(envFile.toString());
        comando.add("-f");
        comando.add("deploy/docker-compose.yml");
        comando.add("-f");
        comando.add(compose.toString());
        comando.addAll(List.of(argumentos));
        return comando;
    }

    static String literal(String valor) {
        return "'" + valor.replace("'", "''") + "'";
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("uso: alta-consultorio <slug> <dominio> <directorio-env>");
        }
        Map<String, String> secretos = new TreeMap<>();
        for (String clave : List.of("ADMIN_INITIAL_EMAIL", "APP_DB_PASSWORD", "APP_DB_MIGRATOR_PASSWORD",
                "POSTGRES_PASSWORD", "ODONTO_IMAGEN")) {
            String valor = System.getenv(clave);
            if (valor != null) {
                secretos.put(clave, valor);
            }
        }
        String imagen = secretos.get("ODONTO_IMAGEN");
        if (imagen != null && !imagen.isBlank() && !ReferenciaImagen.esValida(imagen)) {
            throw new IllegalArgumentException("ODONTO_IMAGEN no es una referencia versionada");
        }
        PlanAlta plan = planificar(args[0], args[1], Path.of(args[2]), secretos);
        Path escrito = escribirEnv(plan);
        System.out.println("env_file=" + escrito);
        if ("1".equals(System.getenv("ODONTO_APPLY"))) {
            Path caddyFile = Path.of(System.getenv().getOrDefault("ODONTO_CADDYFILE",
                    "deploy/caddy/Caddyfile"));
            String usuario = System.getenv().getOrDefault("POSTGRES_USER", "odonto");
            Resultado resultado = aplicar(plan, "https://" + plan.subdominio() + "/actuator/health",
                    dockerPort(usuario, ProcesadorComandos.real()), caddyFile);
            System.out.println(resultado.mensaje());
            if (!resultado.correcto()) {
                System.exit(1);
            }
        } else {
            plan.comandos().forEach(System.out::println);
        }
    }

    static Port dockerPort(String usuario, ProcesadorComandos procesador) {
        return new Port() {
            @Override
            public void sql(String sentencia) {
                ejecutarEnPostgres(usuario, "postgres", sentencia, procesador);
            }

            @Override
            public void sqlEnBase(String base, String sentencia) {
                ejecutarEnPostgres(usuario, base, sentencia, procesador);
            }

            @Override
            public boolean consultaBooleana(String consulta) {
                List<String> comando = List.of("docker", "compose", "-f", "deploy/docker-compose.yml",
                        "exec", "-T", "postgres", "psql", "-tAc", "-U", usuario, "-d", "postgres",
                        "-c", consulta);
                String salida = procesador.ejecutar(new ProcessBuilder(comando).redirectErrorStream(true), null);
                return Boolean.parseBoolean(salida.trim());
            }
        };
    }

    static void ejecutarEnPostgres(String usuario, String base, String sentencia, ProcesadorComandos procesador) {
        List<String> comando = List.of("docker", "compose", "-f", "deploy/docker-compose.yml",
                "exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1", "-U", usuario,
                "-d", base, "-c", sentencia);
        procesador.ejecutar(new ProcessBuilder(comando), null);
    }

    public static String verificarSalud(String url) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return HttpClient.newHttpClient().send(peticion, BodyHandlers.ofString()).body();
    }

    private static String valor(Map<String, String> secretos, String clave) {
        return valor(secretos, clave, "");
    }

    private static String valor(Map<String, String> secretos, String clave, String porDefecto) {
        String valor = secretos.get(clave);
        return valor == null ? porDefecto : valor;
    }
}
