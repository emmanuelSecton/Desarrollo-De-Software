package ar.edu.utn.tp2;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table ------------------------------------------------------------------------
    @Entity
    @Table(name = "cliente")

public class Cliente extends AuditoriaApp { 

// TODO: Configurar @Column(nullable = false) en cuitCuil y denominacion ---------------------------------
    @Column(nullable = false)
private String cuitCuil; 

    @Column(nullable = false)
private String denominacion; 

// TODO: Configurar @OneToOne y @JoinColumn(nullable = false) ---------------------------------------------
    @OneToOne
    @JoinColumn(nullable = false)
private Contacto contacto; 

// TODO: Configurar @OneToOne y @JoinColumn(nullable = false) ---------------------------------------------
    @OneToOne
    @JoinColumn(nullable = false)
private Domicilio domicilio;

//constructores, getters y setters

    public Cliente() {
    }

    public Cliente(Date fechaAlta, Date fechaBaja, Date fechaModificacion, Usuario usuarioCarga, Usuario usuarioBaja,
            Usuario usuarioModificac, String cuitCuil, String denominacion, Contacto contacto, Domicilio domicilio) {
        super(fechaAlta, fechaBaja, fechaModificacion, usuarioCarga, usuarioBaja, usuarioModificac);
        this.cuitCuil = cuitCuil;
        this.denominacion = denominacion;
        this.contacto = contacto;
        this.domicilio = domicilio;
    }

    public Cliente(String cuitCuil, String denominacion, Contacto contacto, Domicilio domicilio) {
        this.cuitCuil = cuitCuil;
        this.denominacion = denominacion;
        this.contacto = contacto;
        this.domicilio = domicilio;
    }

    //getters y setters ---------------------------------------------------------------------------------
    public String getCuitCuil() {
        return cuitCuil;
    }

    public void setCuitCuil(String cuitCuil) {
        this.cuitCuil = cuitCuil;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public Contacto getContacto() {
        return contacto;
    }

    public void setContacto(Contacto contacto) {
        this.contacto = contacto;
    }

    public Domicilio getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(Domicilio domicilio) {
        this.domicilio = domicilio;
    } 








} 

