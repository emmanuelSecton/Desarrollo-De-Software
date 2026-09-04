package ar.edu.utn.tp2;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

public abstract class AuditoriaApp extends EntityId {
    @Column(nullable = false) // y formato de fecha
    protected Date fechaAlta;

    protected Date fechaBaja;

    @Column(nullable = false) // y formato de fecha
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
