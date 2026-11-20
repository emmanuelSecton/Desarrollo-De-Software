package ar.edu.utn.tp2.config;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve la carpeta frontend/ (index.html + dist/) desde el mismo servidor de Spring
 * y abre el navegador automáticamente cuando la aplicación termina de arrancar.
 */
@Configuration
public class FrontendConfig implements WebMvcConfigurer {

    @Value("${server.port:8080}")
    private int port;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontend = localizarFrontend();
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
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            System.err.println("[Frontend] No se pudo abrir el navegador: " + e.getMessage());
        }
    }

    /** Busca frontend/ tanto si se ejecuta desde "TP Spring" como desde la raíz del repo. */
    private Path localizarFrontend() {
        Path[] candidatos = {
                Paths.get("frontend"),
                Paths.get("TP Spring", "frontend")
        };
        for (Path p : candidatos) {
            if (Files.exists(p.resolve("index.html"))) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
