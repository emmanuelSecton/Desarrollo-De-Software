package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table
public class Contacto extends EntityId {
    private String email;
    private String telefono;
    private String celular;
}