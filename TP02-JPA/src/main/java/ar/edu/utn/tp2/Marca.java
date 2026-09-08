package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "marca")
public class Marca extends AuditoriaApp {

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private Integer codigo;

    protected Marca() {
    }

    public Marca(String denominacion, Integer codigo) {
        this.denominacion = denominacion;
        this.codigo = codigo;
    }
}