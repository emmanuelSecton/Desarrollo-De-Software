package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table --------------------------------------------------------------
    @Entity 
    @Table (name = "rubro") 
    
public class Rubro extends AuditoriaApp { 
// TODO: Configurar @Column(nullable = false) en denominacion y codigo --------------------------
    @Column(nullable = false)
private String denominacion; 

    @Column(nullable = false)
private Integer codigo; 
//Constructores ----------------------------------------------------------------------------------
    public Rubro(String denominacion, Integer codigo) {
        this.denominacion = denominacion;
        this.codigo = codigo;
    }

    public Rubro() {
    }

    //getters y setters -----------------------------------------------------------------------

    public String getDenominacion() {
        return denominacion;
    }

    public void setDenominacion(String denominacion) {
        this.denominacion = denominacion;
    }

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }



}
