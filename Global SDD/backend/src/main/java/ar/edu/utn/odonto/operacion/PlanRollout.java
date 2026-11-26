package ar.edu.utn.odonto.operacion;

import java.util.List;
import java.util.Map;

public final class PlanRollout {

    private static final List<String> CANARIOS = List.of("demo_uno", "demo_dos");

    private PlanRollout() {}

    public record Resultado(boolean continuar, List<String> destinos, String motivo) {}

    public static Resultado evaluar(Map<String, Boolean> salud, List<String> restantes) {
        if (!salud.keySet().containsAll(CANARIOS)) {
            return new Resultado(false, List.of(), "faltan canarios: " + CANARIOS);
        }
        for (String canario : CANARIOS) {
            if (!Boolean.TRUE.equals(salud.get(canario))) {
                return new Resultado(false, List.of(), "canario " + canario + " sin salud");
            }
        }
        return new Resultado(true, List.copyOf(restantes), "");
    }
}
