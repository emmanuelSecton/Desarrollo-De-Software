package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario extends EntityId {
    @Column(nullable = false) // en los 4 atributos
    private String usuario;

    @Column(nullable = false)
    private String clave;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    protected Usuario() {
    }

    public Usuario(String usuario, String clave, String nombre, String apellido) {
        this.usuario = usuario;
        this.clave = clave;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
