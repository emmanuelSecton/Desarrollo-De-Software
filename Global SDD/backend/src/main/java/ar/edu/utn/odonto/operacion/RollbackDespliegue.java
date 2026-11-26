package ar.edu.utn.odonto.operacion;

import java.nio.file.Path;
import java.util.List;

public final class RollbackDespliegue {

    private RollbackDespliegue() {}

    public interface Port {
        void dump(String base, String archivo);

        void imagen(String slug, String tag);
    }

    public record Resultado(boolean correcto, String mensaje) {}

    public static Resultado ejecutar(String slug, String dumpAnterior, String imagenAnterior,
            Port port, RestauracionConsultorio.Restaurador restaurador) {
        if (imagenAnterior == null || !ReferenciaImagen.esValida(imagenAnterior)) {
            return new Resultado(false, "falta ODONTO_IMAGEN_ANTERIOR valida");
        }
        String base = "consultorio_" + slug;
        String copiaActual = "/tmp/rollback-actual.dump";
        try {
            port.dump(base, copiaActual);
            RestauracionConsultorio.Resultado restauracion =
                    RestauracionConsultorio.ejecutar(slug, dumpAnterior, restaurador);
            if (!restauracion.correcto()) {
                return new Resultado(false, restauracion.mensaje());
            }
            port.imagen(slug, imagenAnterior);
            return new Resultado(true, "rollback de " + slug + " a " + imagenAnterior
                    + "; backup previo en " + copiaActual);
        } catch (RuntimeException fallo) {
            return new Resultado(false, "fallo de rollback: " + fallo.getMessage());
        }
    }

    public static void main(String[] args) {
        String directorio = System.getenv().getOrDefault("ODONTO_BACKUP_DIR", "/var/backups/odontologia");
        String fecha = java.time.Instant.now(java.time.Clock.systemUTC()).toString().replace(":", "-");
        String slug = args.length == 1 ? args[0] : "desconocido";
        String backupActual = directorio + "/consultorio_" + slug + "-pre-rollback-" + fecha + ".dump";
        String usuario = System.getenv().getOrDefault("POSTGRES_USER", "odonto");
        System.exit(ejecutar(args, dockerPort(backupActual, usuario, ProcesadorComandos.real()),
                dockerRestaurador(usuario, ProcesadorComandos.real())));
    }

    public static int ejecutar(String[] args, Port port, RestauracionConsultorio.Restaurador restaurador) {
        if (args.length != 1) {
            System.err.println("uso: RollbackDespliegue <slug>");
            return 1;
        }
        String slug = args[0];
        String imagen = System.getenv("ODONTO_IMAGEN_ANTERIOR");
        String dumpAnterior = System.getenv("ODONTO_DUMP_ANTERIOR");
        if (dumpAnterior == null || dumpAnterior.isBlank()) {
            System.err.println("falta ODONTO_DUMP_ANTERIOR (pg_dump -Fc anterior)");
            return 1;
        }
        Resultado resultado = ejecutar(slug, dumpAnterior, imagen, port, restaurador);
        System.out.println(resultado.mensaje());
        return resultado.correcto() ? 0 : 1;
    }

    static Port dockerPort(String backupActual, String usuario, ProcesadorComandos procesador) {
        return new Port() {
            @Override
            public void dump(String base, String archivo) {
                ejecutar(List.of("docker", "compose", "-f", "deploy/docker-compose.yml",
                        "exec", "-T", "postgres", "pg_dump", "-U", usuario, "-d", base, "-Fc"),
                        Path.of(backupActual), procesador);
            }

            @Override
            public void imagen(String slug, String tag) {
                ProcessBuilder constructor = new ProcessBuilder("docker", "compose", "-f",
                        "deploy/docker-compose.yml", "up", "-d", "--no-build", "--force-recreate",
                        "app_" + slug);
                constructor.environment().put("ODONTO_IMAGEN", tag);
                procesador.ejecutar(constructor, null);
            }
        };
    }

    static RestauracionConsultorio.Restaurador dockerRestaurador(String usuario, ProcesadorComandos procesador) {
        return new RestauracionConsultorio.Restaurador() {
            @Override
            public void restaurar(String base, String archivo) {
                ejecutar(List.of("docker", "compose", "-f", "deploy/docker-compose.yml",
                        "exec", "-T", "postgres", "pg_restore", "--clean", "--if-exists",
                        "--exit-on-error", "-U", usuario, "-d", base, archivo), null, procesador);
            }

            @Override
            public void reaplicarGrants(String base, String slug) {
                String rol = "consultorio_" + slug + "_runtime";
                ejecutar(List.of("docker", "compose", "-f", "deploy/docker-compose.yml",
                        "exec", "-T", "postgres", "psql", "-v", "ON_ERROR_STOP=1", "-U", usuario,
                        "-d", base, "-c", "REVOKE CONNECT ON DATABASE \"" + base + "\" FROM PUBLIC",
                        "-c", "GRANT CONNECT ON DATABASE \"" + base + "\" TO " + rol,
                        "-c", "GRANT USAGE ON SCHEMA public TO " + rol,
                        "-c", "GRANT SELECT, INSERT ON ALL TABLES IN SCHEMA public TO " + rol,
                        "-c", "GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + rol), null, procesador);
            }
        };
    }

    static void ejecutar(ProcessBuilder constructor, Path salida, ProcesadorComandos procesador) {
        procesador.ejecutar(constructor, salida);
    }

    static void ejecutar(List<String> comando, Path salida, ProcesadorComandos procesador) {
        ejecutar(new ProcessBuilder(comando), salida, procesador);
    }
}
