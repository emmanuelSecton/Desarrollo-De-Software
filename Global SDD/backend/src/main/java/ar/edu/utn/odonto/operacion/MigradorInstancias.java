package ar.edu.utn.odonto.operacion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class MigradorInstancias {

    private MigradorInstancias() {}

    public record Resultado(boolean correcto, List<String> migradas, String fallida) {}

    public static Resultado ejecutar(List<String> slugs, Predicate<String> migrar) {
        List<String> migradas = new ArrayList<>();
        for (String slug : slugs) {
            boolean correcto;
            try {
                correcto = migrar.test(slug);
            } catch (RuntimeException fallo) {
                correcto = false;
            }
            if (!correcto) {
                RegistroAlertas.alertar(TipoAlerta.MIGRACION_FALLIDA);
                return new Resultado(false, List.copyOf(migradas), slug);
            }
            migradas.add(slug);
        }
        return new Resultado(true, List.copyOf(migradas), null);
    }
}
