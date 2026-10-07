package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// TODO: Agregar @Entity y @Table ---------------------------------------------------------------------------
    @Entity 
    @Table(name = "factura_venta_detalle")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder 
    @EqualsAndHashCode(callSuper = true, exclude = {"factura"}) 
    @ToString(exclude = {"factura"})

public class FacturaVentaDetalle extends EntityId { 


// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) ----------------------------------------------
    @ManyToOne 
    @JoinColumn(nullable = false) 
    
private FacturaVenta factura; 


// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) ---------------------------------------------
    @ManyToOne 
    @JoinColumn(nullable = false) 
private ListaPrecioArticulo listaPrecioArticulo; 
private String descripcion; 


// TODO: Configurar @Column(nullable = false) en cantidad, precioUnitario e importeSubtotal ------------------
    @Column(nullable = false)
private double cantidad; 

    @Column(nullable = false)
private double precioUnitario; 


private double porcentajeBonificacion; 
private double importeNeto; 
private double importeIva; 

    @Column(nullable = false)
private double importeSubtotal; 
    
} 
