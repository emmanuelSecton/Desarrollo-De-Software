package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "lista_precio")
public class ListaPrecio extends AuditoriaApp {

    @Column(nullable = false)
    private String codigo;

    @Column(nullable = false)
    private String denominacion;

    protected ListaPrecio() {
    }

    public ListaPrecio(String codigo, String denominacion) {
        this.codigo = codigo;
        this.denominacion = denominacion;
    }
}