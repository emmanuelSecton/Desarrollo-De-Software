package ar.edu.utn.odonto.identidad.infrastructure;

import ar.edu.utn.odonto.identidad.application.Problemas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Unica configuracion de Spring Security de la aplicacion (D3). Todo {@code /api/**} exige sesion
 * salvo las rutas publicas explicitas; la SPA, sus archivos y {@code /actuator/health} quedan abiertos.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'";

    static final String TITULO_NO_AUTENTICADO = "No autenticado";
    static final String DETALLE_NO_AUTENTICADO = "Se requiere iniciar sesión";
    static final String TITULO_ACCESO_DENEGADO = "Acceso denegado";
    static final String DETALLE_ACCESO_DENEGADO = "No tiene permiso para realizar esta acción";

    /** Avisa al SessionRegistry cuando una sesion se destruye o cambia de id (C-04, D7). */
    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain cadenaDeSeguridad(
            HttpSecurity http,
            EscritorDeProblemas escritor,
            SecurityContextRepository contextos,
            SessionRegistry sesiones,
            PropiedadesIdentidad propiedades)
            throws Exception {
        CookieCsrfTokenRepository tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookieCustomizer(cookie -> cookie.sameSite("Strict").secure(true).path("/"));
        // Sin mascara XOR: la SPA reenvia en X-XSRF-TOKEN el valor crudo de la cookie (D7).
        CsrfTokenRequestAttributeHandler manejadorCsrf = new CsrfTokenRequestAttributeHandler();

        http.csrf(csrf -> csrf.csrfTokenRepository(tokens)
                        .csrfTokenRequestHandler(manejadorCsrf)
                        .ignoringRequestMatchers(RutasDeSeguridad.WEBHOOKS + "**"))
                .securityContext(contexto -> contexto.securityContextRepository(contextos))
                .sessionManagement(sesion -> sesion
                        .sessionFixation(fijacion -> fijacion.changeSessionId())
                        // Sin limite de sesiones: el registro sirve para cerrarlas al restablecer (C-04, D7).
                        .maximumSessions(-1)
                        .sessionRegistry(sesiones)
                        .expiredSessionStrategy(evento -> escritor.escribir(
                                evento.getRequest(), evento.getResponse(), HttpStatus.UNAUTHORIZED,
                                Problemas.NO_AUTENTICADO, TITULO_NO_AUTENTICADO, DETALLE_NO_AUTENTICADO)))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((peticion, respuesta, ex) -> escritor.escribir(
                                peticion, respuesta, HttpStatus.UNAUTHORIZED,
                                Problemas.NO_AUTENTICADO, TITULO_NO_AUTENTICADO, DETALLE_NO_AUTENTICADO))
                        .accessDeniedHandler((peticion, respuesta, ex) -> escritor.escribir(
                                peticion, respuesta, HttpStatus.FORBIDDEN,
                                Problemas.ACCESO_DENEGADO, TITULO_ACCESO_DENEGADO, DETALLE_ACCESO_DENEGADO)))
                .headers(cabeceras -> cabeceras
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .frameOptions(opciones -> opciones.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.POST, RutasDeSeguridad.LOGIN).permitAll()
                        .requestMatchers(HttpMethod.GET, RutasDeSeguridad.CSRF).permitAll()
                        .requestMatchers(RutasDeSeguridad.PASSWORD_RECOVER).permitAll()
                        .requestMatchers(RutasDeSeguridad.PASSWORD_RESET).permitAll()
                        .requestMatchers(RutasDeSeguridad.PUBLICO + "**").permitAll()
                        .requestMatchers(RutasDeSeguridad.WEBHOOKS + "**").permitAll()
                        .requestMatchers(RutasDeSeguridad.API + "**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(
                        new FiltroLimiteDePeticiones(
                                escritor, propiedades.loginPorMinuto(), propiedades.publicoPorMinuto()),
                        SecurityContextHolderFilter.class)
                .addFilterAfter(new FiltroCambioPasswordPendiente(escritor), AuthorizationFilter.class);
        return http.build();
    }
}
