package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table
public class Rubro extends AuditoriaApp {
    @Column(nullable = false)
    private String denominacion;

    @Column(nullable = false)
    private Integer codigo;
}