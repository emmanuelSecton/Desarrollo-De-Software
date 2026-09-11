package ar.edu.utn.tp2;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table 
    @Entity 
    @Table(name = "condicion_iva")

public class CondicionIva extends AuditoriaApp { 
// TODO: Configurar @Column(nullable = false) en codigoAfip y denominacion 

    @Column (nullable = false)
private int codigoAfip; 

    @Column (nullable = false)
private String denominacion;

    //constructores
    public CondicionIva() {
    }

    public CondicionIva(Date fechaAlta, Date fechaBaja, Date fechaModificacion, Usuario usuarioCarga,
            Usuario usuarioBaja, Usuario usuarioModificac, int codigoAfip, String denominacion) {
        super(fechaAlta, fechaBaja, fechaModificacion, usuarioCarga, usuarioBaja, usuarioModificac);
        this.codigoAfip = codigoAfip;
        this.denominacion = denominacion;
    }

    public CondicionIva(int codigoAfip, String denominacion) {
        this.codigoAfip = codigoAfip;
        this.denominacion = denominacion;
    } 
    //getters y setters

    public int getCodigoAfip() {
        return codigoAfip;
    }

    public void setCodigoAfip(int codigoAfip) {
        this.codigoAfip = codigoAfip;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }
    


} 
