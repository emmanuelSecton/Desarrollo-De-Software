package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table ----------------------------------------------------
    @Entity
    @Table(name = "punto_venta")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)
public class PuntoVenta extends AuditoriaApp { 

// TODO: Configurar @Column(nullable = false) ----------------------------------------------
    @Column(nullable = false)
private int numero; 

private String descripcion; 
private String tipoEmision; 
private String domicilioComercial; 



} 
