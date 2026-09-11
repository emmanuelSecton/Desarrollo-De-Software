package ar.edu.utn.tp2;

import java.sql.Date;
import java.time.LocalDate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {

    public static void main(String[] args) {

        EntityManager entityManager = null;
        try (EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("FacturacionPU")) {
            entityManager = entityManagerFactory.createEntityManager();

            entityManager.getTransaction().begin();

            Usuario usuario = new Usuario("usuario1", "clave1", "Juan", "Perez");
            Domicilio domicilio = new Domicilio("Calle Falsa", "123");
            Contacto contacto = new Contacto("juanperez@email.com", "123456789", "987654321");
            Cliente cliente = new Cliente("20-12345678-9", "Cliente S.A.", contacto, domicilio);

            
            cliente.setFechaAlta(new java.util.Date());
            cliente.setFechaModificacion(new java.util.Date());
            cliente.setUsuarioCarga(usuario);
            cliente.setUsuarioModificac(usuario);
            

            PuntoVenta puntoVenta = new PuntoVenta(1, "Punto de Venta 1", "Tipo A", "Calle Falsa 123");
            Rubro rubro = new Rubro("Rubro 1", 1);
            Marca marca = new Marca("Marca 1", 1);
            Articulo articulo = new Articulo(rubro, "ART001", "Articulo 1", marca);
            ListaPrecio listaPrecio = new ListaPrecio("LP001", "Lista de Precios 1");
            ListaPrecioArticulo listaPrecioArticulo = new ListaPrecioArticulo(listaPrecio, 100.0, articulo);

            //  SETTERS DE AUDITORÍA PARA PUNTO DE VENTA ---------------------------------------------
            puntoVenta.setFechaAlta(new java.util.Date());
            puntoVenta.setFechaModificacion(new java.util.Date());
            puntoVenta.setUsuarioCarga(usuario);
            puntoVenta.setUsuarioModificac(usuario);

            //  SETTERS DE AUDITORÍA PARA RUBRO -------------------------------------------------------------
            rubro.setFechaAlta(new java.util.Date());
            rubro.setFechaModificacion(new java.util.Date());
            rubro.setUsuarioCarga(usuario);
            rubro.setUsuarioModificac(usuario);

            //  SETTERS DE AUDITORÍA PARA MARCA ----------------------------------------------------------------
            marca.setFechaAlta(new java.util.Date());
            marca.setFechaModificacion(new java.util.Date());
            marca.setUsuarioCarga(usuario);
            marca.setUsuarioModificac(usuario);

            //  SETTERS DE AUDITORÍA PARA ARTÍCULO -------------------------------------------------------------
            articulo.setFechaAlta(new java.util.Date());
            articulo.setFechaModificacion(new java.util.Date());
            articulo.setUsuarioCarga(usuario);
            articulo.setUsuarioModificac(usuario);

            //  SETTERS DE AUDITORÍA PARA LISTA DE PRECIO -----------------------------------------------------
            listaPrecio.setFechaAlta(new java.util.Date());
            listaPrecio.setFechaModificacion(new java.util.Date());
            listaPrecio.setUsuarioCarga(usuario);
            listaPrecio.setUsuarioModificac(usuario);

            //  SETTERS DE AUDITORÍA PARA LISTA PRECIO ARTÍCULO -------------------------------------------------------------
            listaPrecioArticulo.setFechaAlta(new java.util.Date());
            listaPrecioArticulo.setFechaModificacion(new java.util.Date());
            listaPrecioArticulo.setUsuarioCarga(usuario);
            listaPrecioArticulo.setUsuarioModificac(usuario);

           FacturaVenta facturaVenta = new FacturaVenta(Date.valueOf(LocalDate.now()), puntoVenta, 0.0, 0.0, 0.0, null, null, null, null, "PENDIENTE", null, null, new java.util.ArrayList<>());

                // Seteamos las fechas requeridas con la fecha y hora actual --------------------------------------------------
            facturaVenta.setFechaAlta(new java.util.Date());
            facturaVenta.setFechaModificacion(new java.util.Date());

                // Seteamos los usuarios de carga y modificación (usamos el que creaste arriba)
            facturaVenta.setUsuarioCarga(usuario);
            facturaVenta.setUsuarioModificac(usuario);
                // --------------------------------------------------------------

            FacturaVentaDetalle facturaVentaDetalle = new FacturaVentaDetalle(facturaVenta, listaPrecioArticulo,
                    "Articulo 1", 2.0, 50.0, 0.0, 100.0, 21.0, 121.0);
            
            facturaVenta.getDetalles().add(facturaVentaDetalle);

            entityManager.persist(usuario);
            entityManager.persist(contacto);
            entityManager.persist(domicilio);
            entityManager.persist(cliente);
            entityManager.persist(puntoVenta);
            entityManager.persist(rubro);
            entityManager.persist(marca);
            entityManager.persist(articulo);
            entityManager.persist(listaPrecio);
            entityManager.persist(listaPrecioArticulo);

// ACÁ ESTÁ LA MAGIA DEL TP:
// Persistimos solo la factura. Gracias al CascadeType.ALL configurado en FacturaVenta,
// Hibernate va a guardar automáticamente también el FacturaVentaDetalle.
            entityManager.persist(facturaVenta);

            entityManager.getTransaction().commit();

        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}
