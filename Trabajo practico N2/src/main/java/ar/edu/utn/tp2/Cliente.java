package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//  Agregar @Entity y @Table ------------------------------------------------------------------------
    @Entity
    @Table(name = "cliente")
    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @EqualsAndHashCode(callSuper = true)

public class Cliente extends AuditoriaApp { 

//  Configurar @Column(nullable = false) en cuitCuil y denominacion ---------------------------------
    @Column(nullable = false)
private String cuitCuil; 

    @Column(nullable = false)
private String denominacion; 

//  Configurar @OneToOne y @JoinColumn(nullable = false) ---------------------------------------------
    @OneToOne
    @JoinColumn(nullable = false)
private Contacto contacto; 

//  Configurar @OneToOne y @JoinColumn(nullable = false) ---------------------------------------------
    @OneToOne
    @JoinColumn(nullable = false)
private Domicilio domicilio;


} 

