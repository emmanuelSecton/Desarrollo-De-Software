package ar.edu.utn.tp2;

import java.sql.Date;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {
        try (EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("FacturacionPU")) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            entityManager.getTransaction().begin();

            Usuario usuario = new Usuario("usuario1", "clave1", "Juan", "Perez");
            Domicilio domicilio = new Domicilio("Calle Falsa", "123");
            Contacto contacto = new Contacto("juanperez@email.com", "123456789", "987654321");
            Cliente cliente = new Cliente("20-12345678-9", "Cliente S.A.", contacto, domicilio);

            PuntoVenta puntoVenta = new PuntoVenta(1, "Punto de Venta 1", "Tipo A", "Calle Falsa 123");
            Rubro rubro = new Rubro("Rubro 1", null);
            Marca marca = new Marca("Marca 1", null);
            Articulo articulo = new Articulo(rubro, "ART001", "Articulo 1", marca);
            ListaPrecio listaPrecio = new ListaPrecio("LP001", "Lista de Precios 1");

            ListaPrecioArticulo listaPrecioArticulo = new ListaPrecioArticulo(listaPrecio, 100.0, articulo);

            FacturaVenta facturaVenta1 = new FacturaVenta(Date.valueOf(LocalDate.now()), puntoVenta, 0.0, 0.0, 0.0,
                    null, null, null, null, "PENDIENTE", null, null, null);

            FacturaVentaDetalle ArticuloFacturado1 = new FacturaVentaDetalle(facturaVenta1, listaPrecioArticulo,
                    "Articulo 1", 2.0, 50.0, 0.0, 100.0, 21.0, 121.0);

            facturaVenta1.setDetalles(java.util.Arrays.asList(ArticuloFacturado1));

            // ?? Pregunta: Estos persist son necesarios para que se generen los IDs y se
            // puedan usar en las relaciones????
            // Dónde se colocan? Luego de cada instancia de cada objeto? O al final, como
            // está ahora?
            // entityManager.persist(usuario);
            // entityManager.persist(puntoVenta);
            // entityManager.persist(cliente);
            // entityManager.persist(rubro);
            // entityManager.persist(marca);
            // entityManager.persist(articulo);
            // entityManager.persist(listaPrecio);
            // entityManager.persist(listaPrecioArticulo);
            // entityManager.persist(ArticuloFacturado1);

            entityManager.persist(facturaVenta1);

            entityManager.getTransaction().commit();

            // System.out.printf("usuario persistido con id %d: %s %s%n",
            // usuario.getId(), usuario.getNombre(), usuario.getApellido());
            entityManager.close();
        }
    }
}
