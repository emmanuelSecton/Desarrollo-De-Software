package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table
public class Domicilio extends EntityId {
    private String nombreCalle;
    private String numeroCalle;
}