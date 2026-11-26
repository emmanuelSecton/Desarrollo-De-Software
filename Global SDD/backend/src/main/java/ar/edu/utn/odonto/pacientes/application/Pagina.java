package ar.edu.utn.odonto.pacientes.application;

import java.util.List;
import java.util.function.Function;

/** Una pagina de resultados con el total de coincidencias. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long total) {

    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(contenido.stream().map(conversion).toList(), pagina, tamano, total);
    }
}
