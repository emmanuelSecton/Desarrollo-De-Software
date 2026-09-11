package ar.edu.utn.tp2;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;


    @MappedSuperclass
public abstract class AuditoriaApp extends EntityId {

    // TODO: Configurar @Column(nullable = false) y formato de fecha -----------------------------------
    @Column(nullable = false)
 protected Date fechaAlta;
 protected Date fechaBaja;

 // TODO: Configurar @Column(nullable = false) y formato de fecha -------------------------------------
    @Column(nullable = false)
 protected Date fechaModificacion;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) ---------------------------------------
    @ManyToOne
    @JoinColumn(nullable = false)
 protected Usuario usuarioCarga;

 // TODO: Configurar @ManyToOne -------------------------------------------------------------------------
    @ManyToOne
 protected Usuario usuarioBaja;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) -----------------------------------------
    @ManyToOne
    @JoinColumn(nullable = false)
 protected Usuario usuarioModificac;

    //Constructor ----------------------------------------------------------------------------------------
    public AuditoriaApp() {
    }
    public AuditoriaApp(Date fechaAlta, Date fechaBaja, Date fechaModificacion, Usuario usuarioCarga,
            Usuario usuarioBaja, Usuario usuarioModificac) {
        this.fechaAlta = fechaAlta;
        this.fechaBaja = fechaBaja;
        this.fechaModificacion = fechaModificacion;
        this.usuarioCarga = usuarioCarga;
        this.usuarioBaja = usuarioBaja;
        this.usuarioModificac = usuarioModificac;
    }

    //getters y setters ---------------------------------------------------------------------------------
    public Date getFechaAlta() {
        return fechaAlta;
    }
    public void setFechaAlta(Date fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
    public Date getFechaBaja() {
        return fechaBaja;
    }
    public void setFechaBaja(Date fechaBaja) {
        this.fechaBaja = fechaBaja;
    }
    public Date getFechaModificacion() {
        return fechaModificacion;
    }
    public void setFechaModificacion(Date fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
    public Usuario getUsuarioCarga() {
        return usuarioCarga;
    }
    public void setUsuarioCarga(Usuario usuarioCarga) {
        this.usuarioCarga = usuarioCarga;
    }
    public Usuario getUsuarioBaja() {
        return usuarioBaja;
    }
    public void setUsuarioBaja(Usuario usuarioBaja) {
        this.usuarioBaja = usuarioBaja;
    }
    public Usuario getUsuarioModificac() {
        return usuarioModificac;
    }
    public void setUsuarioModificac(Usuario usuarioModificac) {
        this.usuarioModificac = usuarioModificac;
    }

   
}
