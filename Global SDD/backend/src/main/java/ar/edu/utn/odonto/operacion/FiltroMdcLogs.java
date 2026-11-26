package ar.edu.utn.odonto.operacion;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class FiltroMdcLogs {

    private static final Set<String> PERMITIDOS = Set.of(
            "request_id",
            "consultorio_slug",
            "auditoria.accion",
            "auditoria.entidad",
            "auditoria.motivo",
            "auditoria.correlacion");

    private FiltroMdcLogs() {}

    public static boolean permitido(String clave) {
        return clave != null && PERMITIDOS.contains(clave);
    }

    public static Map<String, String> conservar(Map<String, String> valores) {
        return valores.entrySet().stream()
                .filter(entrada -> PERMITIDOS.contains(entrada.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
