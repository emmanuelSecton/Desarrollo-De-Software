package ar.edu.utn.tp2;

import jakarta.persistence.Column;

public class Usuario extends EntityId {
    @Column(nullable = false) // en los 4 atributos
    private String usuario;
    @Column(nullable = false)
    private String clave;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String apellido;
}
