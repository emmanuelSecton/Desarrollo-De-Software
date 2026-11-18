package ar.edu.utn.tp2;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table -----------------------------------------------------------
    @Entity 
    @Table(name = "contacto")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)

public class Contacto extends EntityId { 
private String email; 
private String telefono; 
private String celular;



}
