package ar.edu.utn.tp2;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


    @MappedSuperclass
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    
public abstract class AuditoriaApp extends EntityId {

    // TODO: Configurar @Column(nullable = false) y formato de fecha -----------------------------------
    @Column(nullable = false)
 protected Date fechaAlta;
 protected Date fechaBaja;

 // TODO: Configurar @Column(nullable = false) y formato de fecha -------------------------------------
    @Column(nullable = false)
 protected Date fechaModificacion;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) ---------------------------------------
    @ManyToOne
    @JoinColumn(nullable = false)
 protected Usuario usuarioCarga;

 // TODO: Configurar @ManyToOne -------------------------------------------------------------------------
    @ManyToOne
 protected Usuario usuarioBaja;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) -----------------------------------------
    @ManyToOne
    @JoinColumn(nullable = false)
 protected Usuario usuarioModificac;

      
}
