package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_moneda")
public class TipoMoneda extends AuditoriaApp {
    @Column(nullable = false)
    private String codigoAfip;

    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private String simbolo;

    protected TipoMoneda() {
    }

    public TipoMoneda(String codigoAfip, String denominacion, String simbolo) {
        this.codigoAfip = codigoAfip;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
    }
}