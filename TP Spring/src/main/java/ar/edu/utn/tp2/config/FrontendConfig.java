package ar.edu.utn.tp2.config;

import java.net.URI;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve la carpeta frontend/ (index.html + dist/) desde el mismo servidor de Spring.
 * Al terminar de arrancar abre el navegador en el index, que muestra un modal de carga
 * hasta que termina de traer los datos iniciales.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    @Value("${server.port:8080}")
    private int port;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontend = Navegador.localizarFrontend();
        if (frontend != null) {
            registry.addResourceHandler("/**")
                    .addResourceLocations(frontend.toUri().toString())
                    .setCachePeriod(0);
        } else {
            System.err.println("[Frontend] No se encontró la carpeta 'frontend' con index.html");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void abrirNavegador() {
        String url = "http://localhost:" + port + "/";
        System.out.println("[Frontend] Disponible en " + url);
        // El index se abre con el modal de carga encima hasta que termina de traer los datos
        Navegador.abrir(URI.create(url));
    }
}
