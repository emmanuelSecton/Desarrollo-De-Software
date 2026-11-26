package ar.edu.utn.odonto.support;

import ar.edu.utn.odonto.identidad.infrastructure.ConfiguracionIdentidad;
import ar.edu.utn.odonto.identidad.infrastructure.ConfiguracionSeguridad;
import ar.edu.utn.odonto.identidad.infrastructure.EscritorDeProblemas;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Para los {@code @WebMvcTest}: aplica la cadena de seguridad real de la aplicacion (C-03) en vez de
 * la configuracion por defecto de Spring Boot, para que el slice se comporte como produccion.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ConfiguracionSeguridad.class, ConfiguracionIdentidad.class, EscritorDeProblemas.class})
public @interface SeguridadReal {}
