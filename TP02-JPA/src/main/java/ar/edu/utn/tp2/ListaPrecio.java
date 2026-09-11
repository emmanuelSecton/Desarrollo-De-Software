package ar.edu.utn.tp2;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table  --------------------------------------------------------
    @Entity 
    @Table (name = "lista_precio")

public class ListaPrecio extends AuditoriaApp { 

// TODO: Configurar @Column(nullable = false) en codigo y denominacion ---------------------
    @Column (nullable = false)
private String codigo; 

    @Column (nullable = false)
private String denominacion; 

//constructores ---------------------------------------------------------------------------

    public ListaPrecio() {
    }

    public ListaPrecio(Date fechaAlta, Date fechaBaja, Date fechaModificacion, Usuario usuarioCarga,
            Usuario usuarioBaja, Usuario usuarioModificac, String codigo, String denominacion) {
        super(fechaAlta, fechaBaja, fechaModificacion, usuarioCarga, usuarioBaja, usuarioModificac);
        this.codigo = codigo;
        this.denominacion = denominacion;
    }

    public ListaPrecio(String codigo, String denominacion) {
        this.codigo = codigo;
        this.denominacion = denominacion;
    }

    //getters y setters -----------------------------------------------------------------------

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

} 
