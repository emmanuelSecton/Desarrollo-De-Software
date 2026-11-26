package ar.edu.utn.odonto.operacion;

import java.util.Arrays;
import java.util.function.Predicate;

public final class MigradorTodas {

    private MigradorTodas() {}

    public static void main(String[] args) {
        System.exit(ejecutar(args, slug -> migrar(slug, ProcesadorComandos.real())));
    }

    public static int ejecutar(String[] args, Predicate<String> migrar) {
        if (args.length == 0) {
            System.err.println("uso: MigradorTodas <slug>...");
            return 1;
        }
        MigradorInstancias.Resultado resultado = MigradorInstancias.ejecutar(Arrays.asList(args), migrar);
        if (!resultado.correcto()) {
            System.err.println("fallo de migracion en " + resultado.fallida());
            return 1;
        }
        System.out.println("todas las instancias migraron: " + String.join(" ", resultado.migradas()));
        return 0;
    }

    static boolean migrar(String slug, ProcesadorComandos procesador) {
        try {
            procesador.ejecutar(new ProcessBuilder("deploy/bin/migrar-base.sh", slug), null);
            return true;
        } catch (RuntimeException fallo) {
            return false;
        }
    }
}
