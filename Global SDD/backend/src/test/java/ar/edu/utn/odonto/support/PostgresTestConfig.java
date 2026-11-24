package ar.edu.utn.odonto.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Contenedor PostgreSQL singleton compartido por toda la suite (mismo major que produccion: 16).
 * Nunca H2 ni mocks de la base (regla dura).
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestConfig {

    public static final String IMAGEN = "postgres:16.16-alpine";

    public static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse(IMAGEN));

    static {
        POSTGRES.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return POSTGRES;
    }
}
