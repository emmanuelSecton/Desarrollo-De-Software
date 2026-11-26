package ar.edu.utn.odonto.operacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import ar.edu.utn.odonto.support.PostgresTestConfig;

class AltaEfectivaTest {

    @TempDir
    Path temporal;

    @Test
    void creaBaseRolesGrantsComposeYLanzaElServicioReal() throws Exception {
        try (PostgreSQLContainer<?> contenedor = new PostgreSQLContainer<>(
                DockerImageName.parse(PostgresTestConfig.IMAGEN))) {
            contenedor.start();
            Path caddy = caddyInicial();
            AltaConsultorio.PlanAlta plan = plan("nuevo_uno");
            List<String> comandos = new ArrayList<>();
            Map<String, String> variables = new LinkedHashMap<>();
            ProcesadorComandos procesador = (constructor, salida) -> {
                comandos.add(String.join(" ", constructor.command()));
                variables.put("ODONTO_IMAGEN", constructor.environment().getOrDefault("ODONTO_IMAGEN", ""));
                return "";
            };

            AltaConsultorio.Resultado resultado = AltaConsultorio.aplicar(plan,
                    "https://nuevo-uno.demo.example.com/actuator/health", puerto(contenedor), caddy,
                    procesador, url -> "{\"status\":\"UP\"}");

            assertThat(resultado.correcto()).as(resultado.mensaje()).isTrue();
            assertThat(bases(contenedor)).contains("consultorio_nuevo_uno");
            assertThat(roles(contenedor)).contains("consultorio_nuevo_uno_migrator", "consultorio_nuevo_uno_runtime");
            assertThat(permisos(contenedor, "consultorio_nuevo_uno", "consultorio_nuevo_uno_runtime"))
                    .contains("CONNECT=true", "USAGE=true");
            assertThat(noConnectPublico(contenedor, "consultorio_nuevo_uno")).isFalse();
            assertThat(Files.readString(caddy)).contains("nuevo-uno.{$ODONTO_DOMINIO} {")
                    .contains("reverse_proxy app_nuevo_uno:8080");
            assertThat(Files.readString(plan.envFile())).contains("APP_DB_NAME=consultorio_nuevo_uno");
            assertThat(plan.composeFile()).exists();
            assertThat(Files.readString(plan.composeFile()))
                    .contains("app_nuevo_uno:")
                    .contains("image: odonto:1.0.0")
                    .contains("app_nuevo_uno:8080");
            assertThat(comandos).anyMatch(comando -> comando.contains("up -d --no-build app_nuevo_uno")
                    && comando.contains(plan.composeFile().toString()));
            List<Integer> migraciones = indices(comandos, "run --rm migrate_nuevo_uno");
            List<Integer> levantamientos = indices(comandos, "up -d --no-build app_nuevo_uno");
            assertThat(migraciones).hasSize(1);
            assertThat(levantamientos).hasSize(1);
            assertThat(migraciones.get(0)).isLessThan(levantamientos.get(0));
            assertThat(comandos.get(migraciones.get(0))).contains(plan.composeFile().toString());
            assertThat(variables).containsEntry("ODONTO_IMAGEN", "odonto:1.0.0");
        }
    }

    @Test
    void noDuplicaBloquesYReportaHealthDownTrasLanzarElServicio() throws Exception {
        try (PostgreSQLContainer<?> contenedor = new PostgreSQLContainer<>(
                DockerImageName.parse(PostgresTestConfig.IMAGEN))) {
            contenedor.start();
            Path caddy = caddyInicial();
            AltaConsultorio.PlanAlta plan = plan("clinica_9");
            List<String> comandos = new ArrayList<>();
            ProcesadorComandos procesador = (constructor, salida) -> {
                comandos.add(String.join(" ", constructor.command()));
                return "";
            };

            AltaConsultorio.Resultado primera = AltaConsultorio.aplicar(plan,
                    "https://clinica-9.demo.example.com/actuator/health", puerto(contenedor), caddy,
                    procesador, url -> "{\"status\":\"UP\"}");
            assertThat(primera.correcto()).isTrue();

            AltaConsultorio.Resultado segunda = AltaConsultorio.aplicar(plan,
                    "https://clinica-9.demo.example.com/actuator/health", puerto(contenedor), caddy,
                    procesador, url -> "{\"status\":\"DOWN\"}");

            assertThat(segunda.correcto()).isFalse();
            assertThat(segunda.mensaje()).contains("health");
            assertThat(Files.readString(caddy).split("clinica-9\\.\\{\\$ODONTO_DOMINIO\\} \\{", -1)).hasSize(2);
            assertThat(bases(contenedor)).contains("consultorio_clinica_9");
            assertThat(comandos).filteredOn(comando -> comando.contains("app_clinica_9")).hasSize(2);
            List<Integer> migraciones = indices(comandos, "run --rm migrate_clinica_9");
            List<Integer> levantamientos = indices(comandos, "up -d --no-build app_clinica_9");
            assertThat(migraciones).hasSize(2);
            assertThat(levantamientos).hasSize(2);
            assertThat(migraciones.get(0)).isLessThan(levantamientos.get(0));
            assertThat(migraciones.get(1)).isLessThan(levantamientos.get(1));
        }
    }

    private static List<Integer> indices(List<String> comandos, String texto) {
        List<Integer> encontrados = new ArrayList<>();
        for (int indice = 0; indice < comandos.size(); indice++) {
            if (comandos.get(indice).contains(texto)) {
                encontrados.add(indice);
            }
        }
        return encontrados;
    }

    private AltaConsultorio.PlanAlta plan(String slug) {
        Map<String, String> secretos = new LinkedHashMap<>();
        secretos.put("ADMIN_INITIAL_EMAIL", "odontologo@ejemplo.invalid");
        secretos.put("APP_DB_PASSWORD", "clave-ficticia-runtime");
        secretos.put("APP_DB_MIGRATOR_PASSWORD", "clave-ficticia-migrator");
        secretos.put("POSTGRES_PASSWORD", "clave-ficticia-postgres");
        secretos.put("ODONTO_IMAGEN", "odonto:1.0.0");
        return AltaConsultorio.planificar(slug, "demo.example.com", temporal, secretos);
    }

    private Path caddyInicial() throws Exception {
        Path caddy = Path.of("..", "deploy", "caddy", "Caddyfile").toAbsolutePath().normalize();
        return Files.writeString(temporal.resolve("Caddyfile"), Files.readString(caddy));
    }

    private static AltaConsultorio.Port puerto(PostgreSQLContainer<?> contenedor) {
        return new AltaConsultorio.Port() {
            @Override
            public void sql(String sentencia) {
                ejecutar(contenedor, "psql -v ON_ERROR_STOP=1 -U " + contenedor.getUsername()
                        + " -d postgres -c \"" + sentencia.replace("\"", "\\\"") + "\"");
            }

            @Override
            public void sqlEnBase(String base, String sentencia) {
                ejecutar(contenedor, "psql -v ON_ERROR_STOP=1 -U " + contenedor.getUsername()
                        + " -d " + base + " -c \"" + sentencia.replace("\"", "\\\"") + "\"");
            }

            @Override
            public boolean consultaBooleana(String consulta) {
                try (Connection conexion = DriverManager.getConnection(
                                contenedor.getJdbcUrl(), contenedor.getUsername(), contenedor.getPassword());
                        Statement sentencia = conexion.createStatement()) {
                    return Boolean.parseBoolean(MigracionesC35.consultaUnica(sentencia, consulta));
                } catch (Exception fallo) {
                    throw new IllegalStateException(fallo);
                }
            }
        };
    }

    private static void ejecutar(PostgreSQLContainer<?> contenedor, String comando) {
        try {
            var respuesta = contenedor.execInContainer("/bin/sh", "-c", comando);
            assertThat(respuesta.getExitCode()).as(respuesta.getStderr()).isEqualTo(0);
        } catch (Exception fallo) {
            throw new IllegalStateException(fallo);
        }
    }

    private static List<String> bases(PostgreSQLContainer<?> contenedor) throws Exception {
        try (Connection conexion = MigracionesC35.administrador(contenedor);
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaLista(sentencia,
                    "select datname from pg_database where datname like 'consultorio%' order by datname");
        }
    }

    private static List<String> roles(PostgreSQLContainer<?> contenedor) throws Exception {
        try (Connection conexion = MigracionesC35.administrador(contenedor);
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaLista(sentencia,
                    "select rolname from pg_roles where rolname like 'consultorio%' order by rolname");
        }
    }

    private static boolean noConnectPublico(PostgreSQLContainer<?> contenedor, String base) throws Exception {
        try (Connection conexion = MigracionesC35.administrador(contenedor);
                Statement sentencia = conexion.createStatement()) {
            return MigracionesC35.consultaUnica(sentencia,
                    "select has_database_privilege('public', '" + base + "', 'CONNECT')::text")
                    .equals("true");
        }
    }

    private static List<String> permisos(PostgreSQLContainer<?> contenedor, String base, String rol) throws Exception {
        try (Connection conexion = DriverManager.getConnection(
                        MigracionesC35.url(contenedor, base), contenedor.getUsername(), contenedor.getPassword());
                Statement sentencia = conexion.createStatement()) {
            return List.of(
                    "CONNECT=" + MigracionesC35.consultaUnica(sentencia,
                            "select has_database_privilege('" + rol + "', '" + base + "', 'CONNECT')::text"),
                    "USAGE=" + MigracionesC35.consultaUnica(sentencia,
                            "select has_schema_privilege('" + rol + "', 'public', 'USAGE')::text"));
        }
    }
}
