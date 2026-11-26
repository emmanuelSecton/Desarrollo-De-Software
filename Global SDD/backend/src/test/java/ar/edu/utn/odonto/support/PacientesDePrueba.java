package ar.edu.utn.odonto.support;

import ar.edu.utn.odonto.pacientes.application.DatosDePaciente;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pacientes ficticios para tests (SU-07). DNI, celular y apellido unicos por llamada, para no chocar
 * en la base compartida por toda la suite.
 */
public final class PacientesDePrueba {

    private PacientesDePrueba() {}

    public static String dniUnico() {
        return Integer.toString(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    /** Celular de CABA ficticio en E.164: +54 9 11 3xxx-xxxx. */
    public static String celularUnico() {
        return "+54911" + ThreadLocalRandom.current().nextInt(30_000_000, 39_999_999);
    }

    /** Sufijo de letras para que la busqueda por texto devuelva solo lo creado en el test. */
    public static String sufijo() {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            texto.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return texto.toString();
    }

    public static DatosDePaciente datos(String apellido) {
        return new DatosDePaciente(dniUnico(), "Ana", apellido, LocalDate.of(1990, 5, 17), celularUnico(),
                "ana." + sufijo() + "@consultorio.test", false);
    }

    public static DatosDePaciente datos() {
        return datos("Pérez " + sufijo());
    }
}
