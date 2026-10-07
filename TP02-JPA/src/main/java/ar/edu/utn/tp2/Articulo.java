package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table ----------------------------------------------------------------
    @Entity 
    @Table (name = "articulo")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)

public class Articulo extends AuditoriaApp { 

// TODO: Configurar @ManyToOne ------------------------------------------------------------------
    @ManyToOne 
private Rubro rubro; 

// TODO: Configurar @Column(nullable = false) en codigo y denominacion -------------------------
    @Column (nullable = false)
private String codigo; 
    @Column (nullable = false)  
private String denominacion; 

// TODO: Configurar @ManyToOne -----------------------------------------------------------------
    @ManyToOne 
private Marca marca;


} 
