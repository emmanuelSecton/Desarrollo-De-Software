package ar.edu.utn.odonto.shared.infrastructure;

import jakarta.validation.constraints.NotBlank;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuracion de la instancia que viene de variables de entorno (D7, D13). Los mensajes nombran
 * la variable de entorno, no la propiedad de Spring, para que el error sea accionable.
 *
 * @param baseUrl URL publica de la instancia ({@code APP_BASE_URL}), obligatoria
 * @param timezone zona horaria del consultorio ({@code APP_TIMEZONE}), por defecto Buenos Aires
 * @param dbUrl copia de {@code DB_URL} usada solo para fallar con un mensaje claro si falta
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(
        @NotBlank(message = "APP_BASE_URL es obligatoria") String baseUrl,
        @DefaultValue("America/Argentina/Buenos_Aires") String timezone,
        @NotBlank(message = "DB_URL es obligatoria") String dbUrl) {

    public AppProperties {
        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("APP_TIMEZONE no es una zona horaria valida: '" + timezone + "'", e);
        }
    }

    public ZoneId zonaHoraria() {
        return ZoneId.of(timezone);
    }
}
