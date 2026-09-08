package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "articulo")
public class Articulo extends AuditoriaApp {

    @ManyToOne
    private Rubro rubro;

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    @ManyToOne
    private Marca marca;

    protected Articulo() {
    }

    public Articulo(Rubro rubro, String codigo, String denominacion, Marca marca) {
        this.rubro = rubro;
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.marca = marca;
    }
}