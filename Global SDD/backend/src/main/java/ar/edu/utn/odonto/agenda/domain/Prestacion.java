package ar.edu.utn.odonto.agenda.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Prestacion del catalogo: duracion multiplo de 5 (RN-AG-02) y baja logica, nunca se borra (C-05, D5). */
public class Prestacion {

    public static final int DURACION_MAXIMA = 480;

    private final UUID id;
    private Datos datos;
    private boolean activa;

    public Prestacion(UUID id, Datos datos, boolean activa) {
        this.id = Objects.requireNonNull(id, "id");
        this.datos = Objects.requireNonNull(datos, "datos");
        this.activa = activa;
    }

    public static Prestacion nueva(Datos datos) {
        return new Prestacion(UUID.randomUUID(), datos, true);
    }

    public void modificar(Datos nuevos) {
        this.datos = Objects.requireNonNull(nuevos, "nuevos");
    }

    public void darDeBaja() {
        this.activa = false;
    }

    public void reactivar() {
        this.activa = true;
    }

    public UUID id() {
        return id;
    }

    public Datos datos() {
        return datos;
    }

    public boolean activa() {
        return activa;
    }

    public record Datos(String nombre, int duracionMinutos, BigDecimal precioReferencia, boolean reservableOnline) {

        public Datos {
            if (nombre == null || nombre.isBlank()) {
                throw new ConfiguracionInvalidaException("nombre", "es obligatorio");
            }
            nombre = nombre.trim();
            if (duracionMinutos <= 0 || duracionMinutos % 5 != 0 || duracionMinutos > DURACION_MAXIMA) {
                throw new ConfiguracionInvalidaException("duracionMinutos",
                        "debe ser múltiplo de 5, mayor que 0 y de hasta " + DURACION_MAXIMA + " minutos");
            }
            if (precioReferencia != null && precioReferencia.signum() < 0) {
                throw new ConfiguracionInvalidaException("precioReferencia", "no puede ser negativo");
            }
        }
    }
}
