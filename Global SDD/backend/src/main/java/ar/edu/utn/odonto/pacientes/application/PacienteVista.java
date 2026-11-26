package ar.edu.utn.odonto.pacientes.application;

import ar.edu.utn.odonto.pacientes.domain.Paciente;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Lectura de un paciente para la API y para otros modulos; no expone la clase de dominio. */
public record PacienteVista(
        UUID id,
        String dni,
        String nombre,
        String apellido,
        LocalDate fechaNacimiento,
        String telefono,
        String email,
        boolean aceptaWhatsapp,
        int ausenciasCount,
        boolean activo,
        Instant consentimientoDatosAt) {

    public static PacienteVista de(Paciente paciente) {
        Paciente.DatosPersonales datos = paciente.datos();
        return new PacienteVista(paciente.id(), datos.dni(), datos.nombre(), datos.apellido(),
                datos.fechaNacimiento(), datos.telefonoE164(), datos.email(), datos.aceptaWhatsapp(),
                paciente.ausenciasCount(), paciente.activo(), paciente.consentimientoDatosAt());
    }

    /** Nunca imprime datos personales (Ley 25.326). */
    @Override
    public String toString() {
        return "PacienteVista[id=" + id + "]";
    }
}
