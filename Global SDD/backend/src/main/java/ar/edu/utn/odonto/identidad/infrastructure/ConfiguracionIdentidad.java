package ar.edu.utn.odonto.identidad.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/** Beans de identidad que existen tambien fuera del contexto web (por ejemplo, el alta inicial). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesIdentidad.class)
public class ConfiguracionIdentidad {

    /** BCrypt cost 12 (08_arquitectura_propuesta.md §Seguridad). */
    public static final int COSTO_BCRYPT = 12;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(COSTO_BCRYPT);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
