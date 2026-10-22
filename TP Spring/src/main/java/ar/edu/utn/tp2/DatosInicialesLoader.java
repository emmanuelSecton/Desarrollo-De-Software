package ar.edu.utn.tp2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Order(1)
public class DatosInicialesLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        Long cantidadFacturas = entityManager
                .createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class)
                .getSingleResult();

        if (cantidadFacturas > 0) {
            return;
        }

        Usuario usuario = new Usuario("usuario1", "clave1", "Juan", "Perez");
        entityManager.persist(usuario);

        CondicionIva condicionIva = new CondicionIva(1, "Responsable Inscripto");
        configurarAuditoria(condicionIva, usuario);
        entityManager.persist(condicionIva);

        TipoMoneda tipoMoneda = new TipoMoneda("011", "Pesos Argentinos", "$");
        configurarAuditoria(tipoMoneda, usuario);
        entityManager.persist(tipoMoneda);

        GeneradorDatos.cargarDatos(entityManager, usuario, condicionIva, tipoMoneda);
    }

    private void configurarAuditoria(AuditoriaApp entidad, Usuario usuario) {
        Date ahora = new Date();
        entidad.setFechaAlta(ahora);
        entidad.setFechaModificacion(ahora);
        entidad.setUsuarioCarga(usuario);
        entidad.setUsuarioModificac(usuario);
    }
}
