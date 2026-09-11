package ar.edu.utn.tp2;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

// ==========================================
// Superclases / Herencia
// ==========================================
    @MappedSuperclass
public abstract class EntityId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    //constructores --------------------------------------------------------------------------
    public EntityId() {
    }

    public EntityId(Long id) {
        this.id = id;
    }
    
    //getters y setters --------------------------------------------------------------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }





    
}
