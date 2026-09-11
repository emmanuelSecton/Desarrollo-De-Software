package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table -----------------------------------------------------------
    @Entity 
    @Table(name = "contacto")

public class Contacto extends EntityId { 
private String email; 
private String telefono; 
private String celular;

//Constructores--------------------------------------------------------------------------------

public Contacto(String email, String telefono, String celular) {
    this.email = email;
    this.telefono = telefono;
    this.celular = celular;
}

public Contacto() {
}

//getters y setters --------------------------------------------------------------------------

public String getEmail() {
    return email;
}

public void setEmail(String email) {
    this.email = email;
}

public String getTelefono() {
    return telefono;
}

public void setTelefono(String telefono) {
    this.telefono = telefono;
}

public String getCelular() {
    return celular;
}

public void setCelular(String celular) {
    this.celular = celular;
}







}
