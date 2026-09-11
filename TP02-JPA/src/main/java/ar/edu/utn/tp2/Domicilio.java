package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table ---------------------------------------------------------------------------------
    @Entity
    @Table (name = "domicilio")
public class Domicilio extends EntityId { 

private String nombreCalle; 
private String numeroCalle;

    //constructores ----------------------------------------------------------------------------------------
public Domicilio() {
}

public Domicilio(String nombreCalle, String numeroCalle) {
    this.nombreCalle = nombreCalle;
    this.numeroCalle = numeroCalle;
}

    //getters y setters ---------------------------------------------------------------------------------
public String getNombreCalle() {
    return nombreCalle;
}

public void setNombreCalle(String nombreCalle) {
    this.nombreCalle = nombreCalle;
}

public String getNumeroCalle() {
    return numeroCalle;
}

public void setNumeroCalle(String numeroCalle) {
    this.numeroCalle = numeroCalle;
} 


} 
