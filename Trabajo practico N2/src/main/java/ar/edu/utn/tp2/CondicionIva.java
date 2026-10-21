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
    @Table(name = "condicion_iva")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)

public class CondicionIva extends AuditoriaApp { 
// TODO: Configurar @Column(nullable = false) en codigoAfip y denominacion 

    @Column (nullable = false)
private int codigoAfip; 

    @Column (nullable = false)
private String denominacion;


} 
