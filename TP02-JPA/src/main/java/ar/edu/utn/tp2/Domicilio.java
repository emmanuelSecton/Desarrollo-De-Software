package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "domicilio")
public class Domicilio extends EntityId {
    private String nombreCalle;

    private String numeroCalle;

    protected Domicilio() {
    }

    public Domicilio(String nombreCalle, String numeroCalle) {
        this.nombreCalle = nombreCalle;
        this.numeroCalle = numeroCalle;
    }
}