package ar.edu.utn.odonto.operacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@FunctionalInterface
public interface ProcesadorComandos {

    String ejecutar(ProcessBuilder constructor, Path salida);

    static ProcesadorComandos real() {
        return (constructor, salida) -> {
            try {
                if (salida != null) {
                    Files.createDirectories(salida.getParent());
                    constructor.redirectOutput(salida.toFile());
                } else {
                    constructor.inheritIO();
                }
                constructor.redirectErrorStream(salida == null);
                Process proceso = constructor.start();
                String resultado = salida == null ? ""
                        : new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                if (proceso.waitFor() != 0) {
                    throw new IllegalStateException("fallo al ejecutar comando de operacion");
                }
                return resultado;
            } catch (InterruptedException fallo) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(fallo);
            } catch (IOException fallo) {
                throw new IllegalStateException(fallo);
            }
        };
    }
}
