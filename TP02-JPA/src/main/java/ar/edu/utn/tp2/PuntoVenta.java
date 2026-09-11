package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table ----------------------------------------------------
    @Entity
    @Table(name = "punto_venta")
public class PuntoVenta extends AuditoriaApp { 

// TODO: Configurar @Column(nullable = false) ----------------------------------------------
    @Column(nullable = false)
private int numero; 

private String descripcion; 
private String tipoEmision; 
private String domicilioComercial; 

//Constructores  -----------------------------------------------------------------------------------------
    public PuntoVenta(int numero, String descripcion, String tipoEmision, String domicilioComercial) {
        this.numero = numero;
        this.descripcion = descripcion;
        this.tipoEmision = tipoEmision;
        this.domicilioComercial = domicilioComercial;
    }

    public PuntoVenta() {
    }

    //getters y setters -----------------------------------------------------------------------------------------

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipoEmision() {
        return tipoEmision;
    }

    public void setTipoEmision(String tipoEmision) {
        this.tipoEmision = tipoEmision;
    }

    public String getDomicilioComercial() {
        return domicilioComercial;
    }

    public void setDomicilioComercial(String domicilioComercial) {
        this.domicilioComercial = domicilioComercial;
    }


} 
