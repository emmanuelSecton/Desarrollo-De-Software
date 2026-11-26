package ar.edu.utn.odonto.pacientes.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Paciente del consultorio (C-09). Nunca se borra: la baja es logica (RN-HC-03). El consentimiento de
 * datos se fija en el alta y no cambia; el de WhatsApp se puede dar y revocar con su fecha (RN-DP-01, D7).
 * Recibe siempre el instante actual: nunca consulta el reloj por su cuenta.
 */
public class Paciente {

    private final UUID id;
    private DatosPersonales datos;
    private final Instant consentimientoDatosAt;
    private final String politicaPrivacidadVersion;
    private Instant whatsappConsentimientoAt;
    private final int ausenciasCount;
    private boolean activo;
    private final Instant creadoAt;
    private Instant actualizadoAt;

    public Paciente(
            UUID id,
            DatosPersonales datos,
            Instant consentimientoDatosAt,
            String politicaPrivacidadVersion,
            Instant whatsappConsentimientoAt,
            int ausenciasCount,
            boolean activo,
            Instant creadoAt,
            Instant actualizadoAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.datos = Objects.requireNonNull(datos, "datos");
        this.consentimientoDatosAt = Objects.requireNonNull(consentimientoDatosAt, "consentimientoDatosAt");
        this.politicaPrivacidadVersion = Objects.requireNonNull(politicaPrivacidadVersion, "politicaPrivacidadVersion");
        this.whatsappConsentimientoAt = whatsappConsentimientoAt;
        this.ausenciasCount = ausenciasCount;
        this.activo = activo;
        this.creadoAt = Objects.requireNonNull(creadoAt, "creadoAt");
        this.actualizadoAt = Objects.requireNonNull(actualizadoAt, "actualizadoAt");
    }

    /** Alta con consentimiento de datos registrado en {@code ahora} (RN-DP-01). */
    public static Paciente nuevo(DatosPersonales datos, String politicaPrivacidadVersion, Instant ahora) {
        return new Paciente(UUID.randomUUID(), datos, ahora, politicaPrivacidadVersion,
                datos.aceptaWhatsapp() ? ahora : null, 0, true, ahora, ahora);
    }

    /**
     * Cambia datos personales y de contacto. El consentimiento de WhatsApp toma fecha al darse, la pierde
     * al revocarse y la conserva si no cambia. No toca el consentimiento de datos ni las ausencias.
     */
    public void modificar(DatosPersonales nuevos, Instant ahora) {
        if (nuevos.aceptaWhatsapp() && !datos.aceptaWhatsapp()) {
            whatsappConsentimientoAt = ahora;
        } else if (!nuevos.aceptaWhatsapp()) {
            whatsappConsentimientoAt = null;
        }
        this.datos = nuevos;
        this.actualizadoAt = ahora;
    }

    public void darDeBaja(Instant ahora) {
        this.activo = false;
        this.actualizadoAt = ahora;
    }

    public void reactivar(Instant ahora) {
        this.activo = true;
        this.actualizadoAt = ahora;
    }

    public UUID id() {
        return id;
    }

    public DatosPersonales datos() {
        return datos;
    }

    public Instant consentimientoDatosAt() {
        return consentimientoDatosAt;
    }

    public String politicaPrivacidadVersion() {
        return politicaPrivacidadVersion;
    }

    public Instant whatsappConsentimientoAt() {
        return whatsappConsentimientoAt;
    }

    public int ausenciasCount() {
        return ausenciasCount;
    }

    public boolean activo() {
        return activo;
    }

    public Instant creadoAt() {
        return creadoAt;
    }

    public Instant actualizadoAt() {
        return actualizadoAt;
    }

    /** Datos que carga el personal o el propio paciente; ya normalizados (DNI solo digitos, telefono E.164). */
    public record DatosPersonales(
            String dni,
            String nombre,
            String apellido,
            LocalDate fechaNacimiento,
            String telefonoE164,
            String email,
            boolean aceptaWhatsapp) {

        public DatosPersonales {
            Objects.requireNonNull(dni, "dni");
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(apellido, "apellido");
            Objects.requireNonNull(telefonoE164, "telefonoE164");
        }

        /** Nunca imprime datos personales (Ley 25.326). */
        @Override
        public String toString() {
            return "DatosPersonales[***]";
        }
    }
}
