package ar.edu.utn.tp2.config;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Utilidades para abrir el frontend en el navegador del sistema. */
public final class Navegador {

    private Navegador() {
    }

    /** Abre la URI en el navegador por defecto. Devuelve true si pudo. */
    public static boolean abrir(URI uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
            } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString()).start();
            } else {
                new ProcessBuilder("xdg-open", uri.toString()).start();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[Frontend] No se pudo abrir el navegador: " + e.getMessage());
            return false;
        }
    }

    /** Busca frontend/ tanto si se ejecuta desde "TP Spring" como desde la raíz del repo. */
    public static Path localizarFrontend() {
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
