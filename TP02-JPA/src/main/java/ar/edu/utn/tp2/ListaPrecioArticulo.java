package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table 
    @Entity 
    @Table(name = "lista_precio_articulo")

public class ListaPrecioArticulo extends AuditoriaApp { 

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) 
    @ManyToOne 
    @JoinColumn (nullable = false)
private ListaPrecio listaPrecio; 

// TODO: Configurar @Column(nullable = false) 
    @Column (nullable = false)
private double precioVenta; 

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) 
    @ManyToOne 
    @JoinColumn (nullable = false)  
private Articulo articulo; 

//Constructor ----------------------------------------------------------------------------------

    public ListaPrecioArticulo(ListaPrecio listaPrecio, double precioVenta, Articulo articulo) {
        this.listaPrecio = listaPrecio;
        this.precioVenta = precioVenta;
        this.articulo = articulo;
    } 

    public ListaPrecioArticulo() {
    }

    //getters y setters -----------------------------------------------------------------------

    public ListaPrecio getListaPrecio() {
        return listaPrecio;
    }

    public void setListaPrecio(ListaPrecio listaPrecio) {
        this.listaPrecio = listaPrecio;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }

} 
