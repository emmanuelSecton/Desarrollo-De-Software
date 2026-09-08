package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lista_precio_articulo")
public class ListaPrecioArticulo extends AuditoriaApp {

    @ManyToOne
    @JoinColumn(nullable = false)
    private ListaPrecio listaPrecio;

    @Column(nullable = false)
    private double precioVenta;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Articulo articulo;

    protected ListaPrecioArticulo() {
    }

    public ListaPrecioArticulo(ListaPrecio listaPrecio, double precioVenta, Articulo articulo) {
        this.listaPrecio = listaPrecio;
        this.precioVenta = precioVenta;
        this.articulo = articulo;
    }
}