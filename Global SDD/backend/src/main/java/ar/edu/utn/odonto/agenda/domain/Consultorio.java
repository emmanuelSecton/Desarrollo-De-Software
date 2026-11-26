package ar.edu.utn.odonto.agenda.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** El consultorio de la instancia (uno solo) con sus datos y parametros de notificacion (C-05, D3). */
public class Consultorio {

    private final UUID id;
    private Datos datos;
    private Parametros parametros;
    private Instant actualizadoAt;

    public Consultorio(UUID id, Datos datos, Parametros parametros, Instant actualizadoAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.datos = Objects.requireNonNull(datos, "datos");
        this.parametros = Objects.requireNonNull(parametros, "parametros");
        this.actualizadoAt = Objects.requireNonNull(actualizadoAt, "actualizadoAt");
    }

    public void modificar(Datos nuevosDatos, Parametros nuevosParametros, Instant ahora) {
        this.datos = Objects.requireNonNull(nuevosDatos, "nuevosDatos");
        this.parametros = Objects.requireNonNull(nuevosParametros, "nuevosParametros");
        this.actualizadoAt = ahora;
    }

    public UUID id() {
        return id;
    }

    public Datos datos() {
        return datos;
    }

    public Parametros parametros() {
        return parametros;
    }

    public Instant actualizadoAt() {
        return actualizadoAt;
    }

    /** Datos de contacto y de la reserva publica. */
    public record Datos(
            String nombre,
            String profesionalNombre,
            String matricula,
            String direccion,
            String telefono,
            String email,
            String slugReserva) {

        public Datos {
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(profesionalNombre, "profesionalNombre");
            if (slugReserva == null || !slugReserva.matches("[a-z0-9-]{3,60}")) {
                throw new ConfiguracionInvalidaException(
                        "slugReserva", "debe tener entre 3 y 60 caracteres en minúscula, dígitos o guiones");
            }
        }
    }

    /**
     * Parametros de recordatorio, confirmacion, cancelacion y ofertas (RN-NO-01, RN-NO-05, RN-TU-02, DD-10).
     * La confirmacion tiene que vencer despues de enviado el recordatorio: limite menor que la anticipacion.
     */
    public record Parametros(
            int recordatorioHorasAntes,
            int confirmacionLimiteHoras,
            int cancelacionMinimaHoras,
            int ofertaVencimientoMinutos) {

        public Parametros {
            rango("recordatorioHorasAntes", recordatorioHorasAntes, 1, 168);
            rango("cancelacionMinimaHoras", cancelacionMinimaHoras, 0, 168);
            rango("ofertaVencimientoMinutos", ofertaVencimientoMinutos, 5, 1440);
            if (confirmacionLimiteHoras < 1 || confirmacionLimiteHoras >= recordatorioHorasAntes) {
                throw new ConfiguracionInvalidaException("confirmacionLimiteHoras",
                        "debe ser al menos 1 y menor que las horas de anticipación del recordatorio");
            }
        }

        private static void rango(String campo, int valor, int minimo, int maximo) {
            if (valor < minimo || valor > maximo) {
                throw new ConfiguracionInvalidaException(campo, "debe estar entre " + minimo + " y " + maximo);
            }
        }
    }
}
