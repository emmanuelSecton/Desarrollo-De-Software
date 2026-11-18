package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table ---------------------------------------------------------------------------------
    @Entity
    @Table (name = "domicilio")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)
public class Domicilio extends EntityId { 

private String nombreCalle; 
private String numeroCalle;


} 
