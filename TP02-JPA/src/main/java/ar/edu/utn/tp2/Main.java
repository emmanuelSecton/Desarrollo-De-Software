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

            // SETTERS DE AUDITORÍA PARA EL CLIENTE -------------------------------------------------------
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

            // SETTERS DE AUDITORÍA Y CREACIÓN PARA CONDICION IVA ----------------------------------
            CondicionIva condicionIva = new CondicionIva(1, "Responsable Inscripto"); 
            condicionIva.setFechaAlta(new java.util.Date());
            condicionIva.setFechaModificacion(new java.util.Date());
            condicionIva.setUsuarioCarga(usuario);
            condicionIva.setUsuarioModificac(usuario);

            // SETTERS DE AUDITORÍA Y CREACIÓN PARA TIPO MONEDA ------------------------------------
            TipoMoneda tipoMoneda = new TipoMoneda("011", "Pesos Argentinos", "$"); 
            tipoMoneda.setFechaAlta(new java.util.Date());
            tipoMoneda.setFechaModificacion(new java.util.Date());
            tipoMoneda.setUsuarioCarga(usuario);
            tipoMoneda.setUsuarioModificac(usuario);

          FacturaVenta facturaVenta = new FacturaVenta(
                1L,                                        // numero
                Date.valueOf(LocalDate.now()),                            // fechaEmision
                cliente,                                                  // cliente 
                condicionIva,                                             // condicionIva 
                tipoMoneda,                                               // tipoMoneda 
                puntoVenta,                                               // puntoVenta 
                0.0,                       // importeCobrado
                0.0,                           // importeSaldo
                121.0,                         // importeTotal 
                null,                                            // cae
                null,            // caeFechaVencimiento
                null,                        // resultadoAfip
                null,                        // motivoRechazo
                "PENDIENTE",                               // estado
                null,                      // fechaAnulacion
                null,                        // observaciones
                new java.util.ArrayList<>()                               // detalles
            );
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
            entityManager.persist(condicionIva);
            entityManager.persist(tipoMoneda);
            entityManager.persist(facturaVenta);

        
                GeneradorDatos.cargarDatos(entityManager, usuario, condicionIva, tipoMoneda);

            entityManager.getTransaction().commit();

            // Consultas JPQL   ----------------------------------------------------------------------------------
                //Nivel 1
            //1  ConsultasJPQL.obtenerTodasLasFacturas(entityManager);
            //2  ConsultasJPQL.proyeccionFacturas(entityManager);
            //3  ConsultasJPQL.articulosPorRubro(entityManager, "Electrónica");
            /*4  java.sql.Date inicio = java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(15));
                 java.sql.Date fin = java.sql.Date.valueOf(java.time.LocalDate.now());
                 ConsultasJPQL.facturasPorRangoDeFechas(entityManager, inicio, fin); */
        
                 //Nivel 2
            //5  ConsultasJPQL.facturasEmitidasConImporteMinimo(entityManager, "EMITIDA", 10000.0);
            //6  ConsultasJPQL.buscarClientesPorTexto(entityManager, "mundo", "20-");
            //7  ConsultasJPQL.estadosFacturasSinDuplicados(entityManager);
            //8  ConsultasJPQL.estadisticasFacturasPorEstado(entityManager, "EMITIDA");
            /*9  java.util.List<Integer> numeros = java.util.Arrays.asList(1, 2, 5);
                 ConsultasJPQL.puntosDeVentaPorNumeros(entityManager, numeros);*/

                 //Nivel 3
            //10 ConsultasJPQL.facturasPorUsuarioCarga(entityManager, "usuario1");
            //11 ConsultasJPQL.detallesPorPuntoDeVenta(entityManager, 2);
            //12 ConsultasJPQL.articulosConSusMarcas(entityManager);
            //13 ConsultasJPQL.facturasPorMarcaDeArticulo(entityManager, "Samsung");
            //14 ConsultasJPQL.facturasMayorAlPromedio(entityManager);

                //Nivel 4
            //15 ConsultasJPQL.totalesPorPuntoDeVenta(entityManager);
            //16 ConsultasJPQL.usuariosConMasDeCincoFacturas(entityManager);
            //17 ConsultasJPQL.totalesVendidosPorMarca(entityManager);

                //Nivel 5
            //18 ConsultasJPQL.articulosConVentasRegistradas(entityManager);
            //19 ConsultasJPQL.articulosSinVentasRegistradas(entityManager);
            //20 ConsultasJPQL.clasificarFacturasPorValor(entityManager);


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
