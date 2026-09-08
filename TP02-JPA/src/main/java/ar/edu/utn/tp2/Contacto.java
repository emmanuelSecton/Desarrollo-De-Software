package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "contacto")
public class Contacto extends EntityId {
    private String email;

    private String telefono;

    private String celular;

    protected Contacto() {
    }

    public Contacto(String email, String telefono, String celular) {
        this.email = email;
        this.telefono = telefono;
        this.celular = celular;
    }
}