package ar.edu.utn.odonto.auditoria.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Acepta IPv4/IPv6 literales por sintaxis pura; nunca resuelve hostnames (D5, C-35 difiere el proxy). */
@Component
public class ValidadorIp {

    private static final Logger LOG = LoggerFactory.getLogger(ValidadorIp.class);
    private static final String HEX_GRUPO = "[0-9a-fA-F]{1,4}";

    public String validar(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        if (esIpv4(ip) || esIpv6Literal(ip)) {
            return ip;
        }
        LOG.warn("ip rechazada: formato no valido");
        throw new AuditoriaInvalidaException("ip: formato invalido");
    }

    private static boolean esIpv4(String ip) {
        String[] octetos = ip.split("\\.", -1);
        if (octetos.length != 4) {
            return false;
        }
        for (String octeto : octetos) {
            if (octeto.isEmpty() || octeto.length() > 3) {
                return false;
            }
            try {
                int valor = Integer.parseInt(octeto);
                if (valor < 0 || valor > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    static boolean esIpv6Literal(String ip) {
        if (ip.indexOf(':') < 0 || !ip.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        int doble = ip.indexOf("::");
        if (doble >= 0 && ip.indexOf("::", doble + 2) >= 0) {
            return false;
        }
        String izquierda = doble < 0 ? ip : ip.substring(0, doble);
        String derecha = doble < 0 ? "" : ip.substring(doble + 2);
        List<String> grupos = new ArrayList<>();
        Collections.addAll(grupos, izquierda.isEmpty() ? new String[0] : izquierda.split(":", -1));
        Collections.addAll(grupos, derecha.isEmpty() ? new String[0] : derecha.split(":", -1));

        int contador = 0;
        for (int i = 0; i < grupos.size(); i++) {
            String grupo = grupos.get(i);
            if (grupo.contains(".")) {
                if (i != grupos.size() - 1 || !esIpv4(grupo)) {
                    return false;
                }
                contador += 2;
            } else if (grupo.matches(HEX_GRUPO)) {
                contador++;
            } else {
                return false;
            }
        }
        return doble >= 0 ? contador <= 7 : contador == 8;
    }
}
