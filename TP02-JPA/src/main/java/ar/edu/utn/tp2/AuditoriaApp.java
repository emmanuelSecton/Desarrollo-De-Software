package ar.edu.utn.tp2;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AuditoriaApp extends EntityId {
    @Column(nullable = false)
    protected Date fechaAlta;

    protected Date fechaBaja;

    @Column(nullable = false)
    protected Date fechaModificacion;

    @ManyToOne
    @JoinColumn(nullable = false)
    protected Usuario usuarioCarga;

    @ManyToOne
    protected Usuario usuarioBaja;

    @ManyToOne
    @JoinColumn(nullable = false)
    protected Usuario usuarioModificacion;
}
