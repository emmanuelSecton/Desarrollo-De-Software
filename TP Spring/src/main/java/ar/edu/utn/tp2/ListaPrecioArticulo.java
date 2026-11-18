package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// TODO: Agregar @Entity y @Table 
    @Entity 
    @Table(name = "lista_precio_articulo")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true, exclude = {"articulo"}) 
    @ToString(exclude = {"articulo"})

public class ListaPrecioArticulo extends AuditoriaApp { 

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) 
    @ManyToOne 
    @JoinColumn (nullable = false)
private ListaPrecio listaPrecio; 

// TODO: Configurar @Column(nullable = false) 
    @Column (nullable = false)
private double precioVenta; 

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) 
    @ManyToOne 
    @JoinColumn (nullable = false)  
private Articulo articulo; 


} 
