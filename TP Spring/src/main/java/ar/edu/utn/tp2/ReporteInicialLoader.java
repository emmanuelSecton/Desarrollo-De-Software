package ar.edu.utn.tp2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ReporteInicialLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        Long cantidadFacturas = entityManager
                .createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class)
                .getSingleResult();

        if (cantidadFacturas > 0) {
            ConsultasJPQL.generarReporteDTO(entityManager);
        }
    }
}
