package ar.edu.utn.odonto.operacion;

import java.util.regex.Pattern;

public final class ReferenciaImagen {

    private static final Pattern SEMANTICA = Pattern.compile(
            "^[a-z0-9][a-z0-9._/-]*:(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(-[0-9A-Za-z.-]+)?(@sha256:[a-f0-9]{64})?$");

    private ReferenciaImagen() {}

    public static boolean esValida(String referencia) {
        return referencia != null && SEMANTICA.matcher(referencia).matches();
    }
}
