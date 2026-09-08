package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "punto_venta")
public class PuntoVenta extends AuditoriaApp {

    @Column(nullable = false)
    private int numero;

    private String descripcion;

    private String tipoEmision;

    private String domicilioComercial;

    protected PuntoVenta() {
    }

    public PuntoVenta(int numero, String descripcion, String tipoEmision, String domicilioComercial) {
        this.numero = numero;
        this.descripcion = descripcion;
        this.tipoEmision = tipoEmision;
        this.domicilioComercial = domicilioComercial;
    }

}
