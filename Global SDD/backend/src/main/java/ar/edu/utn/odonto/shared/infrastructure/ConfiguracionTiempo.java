package ar.edu.utn.odonto.shared.infrastructure;

import ar.edu.utn.odonto.shared.application.ZonaHorariaConsultorio;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj unico de la plataforma (UTC) y zona horaria del consultorio (D7).
 * Nadie llama a {@code now()} sin Clock: un test fija el tiempo reemplazando este bean.
 */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionTiempo {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ZonaHorariaConsultorio zonaHorariaConsultorio(AppProperties propiedades) {
        return new ZonaHorariaConsultorio(propiedades.zonaHoraria());
    }
}
