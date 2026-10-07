package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table 
    @Entity 
    @Table(name = "usuario")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)

public class Usuario extends EntityId { 

// TODO: Configurar @Column(nullable = false) en los 4 atributos 

    @Column(nullable = false)
private String usuario; 

    @Column(nullable = false)
private String clave; 

    @Column(nullable = false)
private String nombre; 

    @Column(nullable = false)
private String apellido; 

} 
