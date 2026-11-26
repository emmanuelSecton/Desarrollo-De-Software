package ar.edu.utn.odonto.identidad.infrastructure;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion del modulo identidad, tomada de variables de entorno (D2, D3, D10).
 *
 * @param emailInicial {@code ADMIN_INITIAL_EMAIL}: solo se usa si la instancia no tiene odontologo
 * @param claveInicial {@code ADMIN_INITIAL_PASSWORD}: contraseña temporal, idem
 * @param inactividadSesion expiracion de la sesion por inactividad (RN-SEG-04)
 * @param loginPorMinuto {@code RATE_LIMIT_LOGIN_POR_MINUTO}: cupo por IP de {@code POST /auth/login}
 * @param publicoPorMinuto {@code RATE_LIMIT_PUBLICO_POR_MINUTO}: cupo por IP de {@code /public/**}
 */
@Validated
@ConfigurationProperties("identidad")
public record PropiedadesIdentidad(
        String emailInicial,
        String claveInicial,
        @DefaultValue("30m") Duration inactividadSesion,
        @DefaultValue("10") @Min(1) long loginPorMinuto,
        @DefaultValue("60") @Min(1) long publicoPorMinuto) {

    /** Nunca expone la contraseña ni el email en logs o mensajes de error de binding. */
    @Override
    public String toString() {
        return "PropiedadesIdentidad[inactividadSesion=" + inactividadSesion + ", loginPorMinuto=" + loginPorMinuto
                + ", publicoPorMinuto=" + publicoPorMinuto + "]";
    }
}
