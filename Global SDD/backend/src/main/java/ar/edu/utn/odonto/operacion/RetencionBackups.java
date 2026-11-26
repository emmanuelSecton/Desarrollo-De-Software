package ar.edu.utn.odonto.operacion;

import java.nio.file.Path;
import java.time.Instant;

public final class RetencionBackups {

    private RetencionBackups() {}

    public static void main(String[] args) {
        System.exit(ejecutar(args));
    }

    public static int ejecutar(String[] args) {
        if (args.length != 2) {
            System.err.println("uso: RetencionBackups <directorio> <ISO-8601-instant>");
            return 1;
        }
        int eliminados = PoliticaBackups.aplicarRetencion(Path.of(args[0]), Instant.parse(args[1]));
        System.out.println("retencion aplicada: " + eliminados + " backups eliminados");
        return 0;
    }
}
