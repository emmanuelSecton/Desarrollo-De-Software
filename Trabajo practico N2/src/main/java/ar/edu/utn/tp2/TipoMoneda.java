package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TODO: Agregar @Entity y @Table -------------------------------------------------

    @Entity 
    @Table(name = "tipo_moneda")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)
public class TipoMoneda extends AuditoriaApp { 

// TODO: Configurar @Column(nullable = false) en los 3 atributos ------------------

    @Column(nullable = false)
private String codigoAfip; 

    @Column(nullable = false)
private String denominacion; 

    @Column(nullable = false)
private String simbolo; 

} 

