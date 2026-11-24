package ar.edu.utn.odonto.shared.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita las propiedades de la aplicacion (los {@code @Enable*} no van en la clase de arranque). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
public class ConfiguracionApp {}
