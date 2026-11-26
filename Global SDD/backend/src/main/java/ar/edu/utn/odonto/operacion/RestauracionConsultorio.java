package ar.edu.utn.odonto.operacion;

import java.util.regex.Pattern;

public final class RestauracionConsultorio {

    private static final Pattern SLUG = Pattern.compile("^[a-z][a-z0-9_]{1,30}$");

    private RestauracionConsultorio() {}

    public interface Restaurador {
        void restaurar(String base, String archivo);

        void reaplicarGrants(String base, String slug);
    }

    public record Resultado(boolean correcto, String mensaje) {}

    public static Resultado ejecutar(String slug, String archivo, Restaurador restaurador) {
        if (slug == null || !SLUG.matcher(slug).matches()) {
            return new Resultado(false, "slug invalido: " + slug);
        }
        String base = "consultorio_" + slug;
        try {
            restaurador.restaurar(base, archivo);
            restaurador.reaplicarGrants(base, slug);
            return new Resultado(true, "restaurada " + base + " desde " + archivo);
        } catch (RuntimeException fallo) {
            return new Resultado(false, "fallo al restaurar " + base + ": " + fallo.getMessage());
        }
    }
}
