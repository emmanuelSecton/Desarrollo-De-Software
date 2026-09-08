package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente extends AuditoriaApp {
    @Column(nullable = false)
    private String cuitCuil;

    @Column(nullable = false)
    private String denominacion;

    @OneToOne
    @JoinColumn(nullable = false)
    private Contacto contacto;

    @OneToOne
    @JoinColumn(nullable = false)
    private Domicilio domicilio;

    protected Cliente() {
    }

    public Cliente(String cuitCuil, String denominacion, Contacto contacto, Domicilio domicilio) {
        this.cuitCuil = cuitCuil;
        this.denominacion = denominacion;
        this.contacto = contacto;
        this.domicilio = domicilio;
    }
}
