package ar.edu.utn.odonto.auditoria.application;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Acepta IPv4/IPv6 literales; nunca resuelve hostnames (D5, C-35 difiere el proxy). */
@Component
public class ValidadorIp {

    private static final Logger LOG = LoggerFactory.getLogger(ValidadorIp.class);

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

    private static boolean esIpv6Literal(String ip) {
        if (!ip.contains(":") || !ip.matches("[0-9a-fA-F:.]+")) {
            return false;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
