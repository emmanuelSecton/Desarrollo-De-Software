package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table ----------------------------------------------------------------
    @Entity 
    @Table (name = "articulo")

public class Articulo extends AuditoriaApp { 

// TODO: Configurar @ManyToOne ------------------------------------------------------------------
    @ManyToOne 
private Rubro rubro; 

// TODO: Configurar @Column(nullable = false) en codigo y denominacion -------------------------
    @Column (nullable = false)
private String codigo; 
    @Column (nullable = false)  
private String denominacion; 

// TODO: Configurar @ManyToOne -----------------------------------------------------------------
    @ManyToOne 
private Marca marca;

//Constructor ----------------------------------------------------------------------------------


    public Articulo(Rubro rubro, String codigo, String denominacion, Marca marca) {
        this.rubro = rubro;
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.marca = marca;
    } 

    public Articulo() {
    }

    //getters y setters -----------------------------------------------------------------------
    public Rubro getRubro() {
        return rubro;
    }

    public void setRubro(Rubro rubro) {
        this.rubro = rubro;
    }

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

    public Marca getMarca() {
        return marca;
    }

    public void setMarca(Marca marca) {
        this.marca = marca;
    }






} 
