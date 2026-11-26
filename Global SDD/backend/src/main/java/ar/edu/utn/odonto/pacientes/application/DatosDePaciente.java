package ar.edu.utn.odonto.pacientes.application;

import java.time.LocalDate;

/**
 * Datos de alta o modificacion tal como llegan (DNI con puntos, telefono en cualquier formato).
 * La normalizacion la hace {@link RegistroDePacientes}.
 */
public record DatosDePaciente(
        String dni,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        boolean aceptaWhatsapp) {

    /** Nunca imprime datos personales (Ley 25.326). */
    @Override
    public String toString() {
        return "DatosDePaciente[***]";
    }
}
