package ar.edu.utn.odonto.pacientes.application;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Que buscar segun la forma del texto ingresado (D5). */
public sealed interface CriterioDeBusqueda {

    /** Lista sin filtro de texto. */
    record Todos() implements CriterioDeBusqueda {}

    /** 7 a 9 digitos: DNI exacto o telefono que termina en esos digitos. */
    record DniOTelefono(String digitos) implements CriterioDeBusqueda {}

    /** Telefono normalizado a E.164. */
    record Telefono(String e164) implements CriterioDeBusqueda {}

    /** Cada palabra debe aparecer en nombre o apellido, sin distinguir mayusculas ni acentos. */
    record Texto(List<String> palabras) implements CriterioDeBusqueda {}

    static CriterioDeBusqueda de(String q) {
        if (q == null || q.isBlank()) {
            return new Todos();
        }
        String compacto = q.replaceAll("[\\s.\\-()]", "");
        if (compacto.matches("[0-9]{7,9}")) {
            return new DniOTelefono(compacto);
        }
        if (compacto.matches("\\+?[0-9]{8,}")) {
            return NormalizadorDeTelefono.intentar(q).<CriterioDeBusqueda>map(Telefono::new)
                    .orElseGet(() -> new DniOTelefono(compacto.replace("+", "")));
        }
        List<String> palabras = Arrays.stream(q.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(p -> !p.isBlank())
                .limit(5)
                .toList();
        return new Texto(palabras);
    }
}
