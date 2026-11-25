package ar.edu.utn.odonto.auditoria.application;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Whitelist estricta de metadatos mínimos; rechaza claves sensibles sin loguear valores (D5). */
@Component
public class ValidadorDetalle {

    private static final Logger LOG = LoggerFactory.getLogger(ValidadorDetalle.class);
    private static final Set<String> WHITELIST = Set.of(
            "turno_id", "paciente_id", "usuario_id", "historia_id", "entrada_id", "cobro_id", "referencia",
            "motivo_codigo");
    private static final List<String> DENYLIST = List.of(
            "diagnostico", "email", "telefono", "dni", "nombre", "apellido", "direccion", "password", "token",
            "clave", "nota", "observacion");
    private static final int LONGITUD_MAXIMA = 200;
    private static final Pattern REFERENCIA = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    public Map<String, Object> validar(Map<String, Object> detalle) {
        if (detalle == null || detalle.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> validado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : detalle.entrySet()) {
            validarClave(entrada.getKey());
            validarValor(entrada.getKey(), entrada.getValue());
            validado.put(entrada.getKey(), entrada.getValue());
        }
        return Map.copyOf(validado);
    }

    private static void validarClave(String clave) {
        if (clave == null || !WHITELIST.contains(clave) || coincideDenylist(clave)) {
            LOG.warn("detalle rechazado: clave fuera de la whitelist");
            throw new AuditoriaInvalidaException("detalle: clave no permitida");
        }
    }

    private static void validarValor(String clave, Object valor) {
        if (!(valor instanceof String || valor instanceof Number || valor instanceof Boolean)) {
            LOG.warn("detalle rechazado: valor no escalar");
            throw new AuditoriaInvalidaException("detalle: valor no escalar");
        }
        if ("referencia".equals(clave) && valor instanceof String texto && !REFERENCIA.matcher(texto).matches()) {
            LOG.warn("detalle rechazado: referencia fuera del formato permitido");
            throw new AuditoriaInvalidaException("detalle: referencia invalida");
        }
        if (valor instanceof String texto && texto.length() > LONGITUD_MAXIMA) {
            LOG.warn("detalle rechazado: valor excede la longitud maxima");
            throw new AuditoriaInvalidaException("detalle: valor demasiado largo");
        }
    }

    private static boolean coincideDenylist(String clave) {
        String normalizada = clave.toLowerCase(Locale.ROOT);
        return DENYLIST.stream().anyMatch(normalizada::contains);
    }
}
