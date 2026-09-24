package ar.edu.utn.tp2;

import jakarta.persistence.EntityManager;
import java.util.List;

public class ConsultasJPQL {

        //Nivel 1-----------------------------------------------------------------------------------------------------------------
    // Consulta 1 ---------------------------------
    public static void obtenerTodasLasFacturas(EntityManager em) {
        String jpql = "SELECT f FROM FacturaVenta f"; 
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class).getResultList();
        
         System.out.println("\nConsulta 1. \nConsulta de Entidades Completas:");
        for (FacturaVenta f : facturas) {
            System.out.println("Factura Nro: " + f.getNumero() + " - Total: $" + f.getImporteTotal());
        } 
       
    }

    // Consulta 2 -------------------------
    public static void proyeccionFacturas(EntityManager em) {
        String jpql = "SELECT f.numero, f.fechaEmision, f.importeTotal FROM FacturaVenta f";
        List<Object[]> resultados = em.createQuery(jpql, Object[].class).getResultList();
        
        System.out.println("\nConsulta 2. \nProyección de Atributos Específicos:");
        for (Object[] fila : resultados) {
            Long numero = (Long) fila[0];
            java.util.Date fecha = (java.util.Date) fila[1];
            Double total = (Double) fila[2];
            
            System.out.println("Nro: " + numero + " | Fecha: " + fecha + " | Total: $" + total);
        }
    }

    // Consulta 3 -----------------------
    public static void articulosPorRubro(EntityManager em, String denominacionRubro) {
        
        String jpql = "SELECT a FROM Articulo a WHERE a.rubro.denominacion = :nombreRubro";
        
        List<Articulo> articulos = em.createQuery(jpql, Articulo.class)
                                     .setParameter("nombreRubro", denominacionRubro)
                                     .getResultList();
        
        System.out.println("\nConsulta 3. \nFiltrado por Igualdad: " + denominacionRubro.toUpperCase());
        
        if (articulos.isEmpty()) {
            System.out.println("No se encontraron artículos para este rubro.");
        } else {
            for (Articulo a : articulos) {
                System.out.println("Código: " + a.getCodigo() + " | Artículo: " + a.getDenominacion());
            }
        }
    }

    //Consulta 4 -----------------------
    public static void facturasPorRangoDeFechas(EntityManager em, java.sql.Date fechaInicio, java.sql.Date fechaFin) {
        
        String jpql = "SELECT f FROM FacturaVenta f WHERE f.fechaEmision BETWEEN :inicio AND :fin";
        
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                                        .setParameter("inicio", fechaInicio)
                                        .setParameter("fin", fechaFin)
                                        .getResultList();
        
        System.out.println("\nConsulta 4. \nFiltrado por Rango de Fechas " + fechaInicio + " y " + fechaFin );
        
        if (facturas.isEmpty()) {
            System.out.println("No se encontraron facturas en este rango de fechas.");
        } else {
            for (FacturaVenta f : facturas) {
                System.out.println("Factura Nro: " + f.getNumero() + " | Fecha: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal());
            }
        }
    }
        //Nivel 2 ----------------------------------------------------------------------------------------------------------------------------
    //Consulta 5 -----------------------------
    public static void facturasEmitidasConImporteMinimo(EntityManager em, String estadoReq, double importeMinimo) {
        
        String jpql = "SELECT f FROM FacturaVenta f WHERE f.estado = :estado AND f.importeTotal > :importe AND f.fechaAnulacion IS NULL";
        
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                                        .setParameter("estado", estadoReq)
                                        .setParameter("importe", importeMinimo)
                                        .getResultList();
        
        System.out.println("\nConsulta 5. \nCondicionales Complejos y Verificación de Nulos: '" + estadoReq + "' mayores a $" + importeMinimo + " sin anular");
        
        if (facturas.isEmpty()) {
            System.out.println("No se encontraron facturas con esos criterios.");
        } else {
            for (FacturaVenta f : facturas) {
                System.out.println("Factura Nro: " + f.getNumero() + " | Estado: " + f.getEstado() + " | Total: $" + f.getImporteTotal());
            }
        }
    }

    //Consulta 6--------------------------------
    public static void buscarClientesPorTexto(EntityManager em, String textoParcial, String prefijoCuit) {
        
        String jpql = "SELECT c FROM Cliente c WHERE LOWER(c.denominacion) LIKE LOWER(:texto) OR c.cuitCuil LIKE :prefijo";
        
        List<Cliente> clientes = em.createQuery(jpql, Cliente.class)
                                   .setParameter("texto", "%" + textoParcial + "%")
                                   .setParameter("prefijo", prefijoCuit + "%")
                                   .getResultList();
        
        System.out.println("\nConsulta 6. \nBúsqueda por Patrón de Texto:'" + textoParcial.toUpperCase() + "' o cuil '" + prefijoCuit );
        
        if (clientes.isEmpty()) {
            System.out.println("No se encontraron clientes que coincidan.");
        } else {
            for (Cliente c : clientes) {
                System.out.println("CUIT: " + c.getCuitCuil() + " | Cliente: " + c.getDenominacion());
            }
        }
    }

    //Consulta 7---------------------------------
    public static void estadosFacturasSinDuplicados(EntityManager em) {
        
        String jpql = "SELECT DISTINCT f.estado FROM FacturaVenta f ORDER BY f.estado ASC";
        
        List<String> estados = em.createQuery(jpql, String.class).getResultList();
        
        System.out.println("\nConsulta 7. \nValores Distintos y Ordenamiento:");
        
        if (estados.isEmpty()) {
            System.out.println("No hay estados registrados.");
        } else {
            for (String estado : estados) {
                System.out.println(estado);
            }
        }
    }

    //Consulta 8------------------------
    public static void estadisticasFacturasPorEstado(EntityManager em, String estadoReq) {
        
        String jpql = "SELECT COUNT(f), SUM(f.importeTotal), AVG(f.importeTotal) FROM FacturaVenta f WHERE f.estado = :estado";
        
        List<Object[]> resultados = em.createQuery(jpql, Object[].class)
                                      .setParameter("estado", estadoReq)
                                      .getResultList();
        
        System.out.println("\nConsulta 8. \nFunciones de Agrefacion simples'" + estadoReq );
        
        Object[] fila = resultados.get(0);
        Long cantidad = (Long) fila[0]; 
        Double suma = (Double) fila[1]; 
        Double promedio = (Double) fila[2]; 
        
        if (cantidad == 0) {
            System.out.println("No hay facturas registradas en estado " + estadoReq);
        } else {
            System.out.println("Cantidad total: " + cantidad);
            System.out.println("Suma acumulada: $" + suma);
            System.out.println("Importe promedio: $" + promedio);
        }
    }

    //Consulta 9--------------
    public static void puntosDeVentaPorNumeros(EntityManager em, java.util.List<Integer> numerosBuscados) {
        
        String jpql = "SELECT p FROM PuntoVenta p WHERE p.numero IN :listaNumeros";
        
        List<PuntoVenta> puntos = em.createQuery(jpql, PuntoVenta.class)
                                    .setParameter("listaNumeros", numerosBuscados)
                                    .getResultList();
        
        System.out.println("\nConsulta 9. \nOperador de Inclusión:" + numerosBuscados);
        
        if (puntos.isEmpty()) {
            System.out.println("No se encontraron puntos de venta con esos números.");
        } else {
            for (PuntoVenta p : puntos) {
                System.out.println("Punto de Venta Nro: " + p.getNumero() + " | Descripción: " + p.getDescripcion());
            }
        }
    }

        //Nivel 3 ---------------------------------------------------------------------------------------------------------------
    //Consulta 10--------------------------------
    public static void facturasPorUsuarioCarga(EntityManager em, String nombreUsuarioCarga) {
        
        String jpql = """
                        SELECT f 
                        FROM FacturaVenta f 
                        WHERE f.usuarioCarga.usuario = :nombreUsuario
                        """;
        
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                                        .setParameter("nombreUsuario", nombreUsuarioCarga)
                                        .getResultList();
        
        System.out.println("\nConsulta 10. \nNavegación Implícita por Relaciones: '" + nombreUsuarioCarga);
        
        if (facturas.isEmpty()) {
            System.out.println("No se encontraron facturas para este usuario.");
        } else {
            for (FacturaVenta f : facturas) {
                System.out.println("Factura Nro: " + f.getNumero() + " | Fecha: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal());
            }
        }
    }

    //Consulta 11-------------------------
    public static void detallesPorPuntoDeVenta(EntityManager em, int numeroPuntoVenta) {
        
        String jpql = """ 
                        SELECT d 
                        FROM FacturaVenta f 
                        INNER JOIN f.detalles d 
                        WHERE f.puntoVenta.numero = :numeroPV 
                """;
        
        List<FacturaVentaDetalle> detalles = em.createQuery(jpql, FacturaVentaDetalle.class)
                                               .setParameter("numeroPV", numeroPuntoVenta)
                                               .getResultList();
        
        System.out.println("\nConsulta 11. \nCláusula INNER JOIN Explícita: " + numeroPuntoVenta);
        
        if (detalles.isEmpty()) {
            System.out.println("No se encontraron detalles para facturas de este punto de venta.");
        } else {
            for (FacturaVentaDetalle d : detalles) {
                System.out.println("Descripción: " + d.getDescripcion() + " | Cantidad: " + d.getCantidad() + " | Subtotal: $" + d.getImporteSubtotal());
            }
        }
    }
    
    //Consulta 12----------------------
    public static void articulosConSusMarcas(EntityManager em) {
        
        String jpql = """
                        SELECT a.denominacion, m.denominacion 
                        FROM Articulo a 
                        LEFT JOIN a.marca m
                        """;
        
        List<Object[]> resultados = em.createQuery(jpql, Object[].class).getResultList();
        
        System.out.println("\nConsulta 12. \nCláusula LEFT JOIN: ");
        
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron artículos.");
        } else {
            for (Object[] fila : resultados) {
                String nombreArticulo = (String) fila[0];
                String nombreMarca = (String) fila[1];
                
                String textoMarca = (nombreMarca != null) ? nombreMarca : "SIN MARCA ASIGNADA";
                
                System.out.println("Artículo: " + nombreArticulo + " | Marca: " + textoMarca);
            }
        }
    }

    //Consulta 13-----------
    public static void facturasPorMarcaDeArticulo(EntityManager em, String nombreMarca) {
        
        String jpql = """
                        SELECT DISTINCT f 
                        FROM FacturaVenta f 
                        JOIN f.detalles d 
                        WHERE d.listaPrecioArticulo.articulo.marca.denominacion = :marca
                        """;
        
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class)
                                        .setParameter("marca", nombreMarca)
                                        .getResultList();
        
        System.out.println("\nConsulta 13. \nNavegación Multinivel con JOINs Combinados: '" + nombreMarca.toUpperCase());
        
        if (facturas.isEmpty()) {
            System.out.println("No se encontraron facturas que contengan artículos de esa marca.");
        } else {
            for (FacturaVenta f : facturas) {
                System.out.println("Factura Nro: " + f.getNumero() + " | Fecha: " + f.getFechaEmision() + " | Total: $" + f.getImporteTotal());
            }
        }
    }

    //Consulta 14 -----------------
    public static void facturasMayorAlPromedio(EntityManager em) {
        
        String jpql = """
                      SELECT f 
                      FROM FacturaVenta f 
                      WHERE f.importeTotal > (
                          SELECT AVG(f2.importeTotal) 
                          FROM FacturaVenta f2
                      )
                      """;
        
        List<FacturaVenta> facturas = em.createQuery(jpql, FacturaVenta.class).getResultList();
        
        System.out.println("\nConsulta 14. \n Subconsulta en Cláusula WHERE:");
        
        if (facturas.isEmpty()) {
            System.out.println("No hay facturas que superen el promedio.");
        } else {
            for (FacturaVenta f : facturas) {
                System.out.println("Factura Nro: " + f.getNumero() + " | Total: $" + f.getImporteTotal());
            }
        }
    }

        //Nivel 4 ------------------------------------------------------------------------------------------------------
    //Consulta 15-------------------------------
    public static void totalesPorPuntoDeVenta(EntityManager em) {
        
        String jpql = """
                      SELECT p.descripcion, COUNT(f), SUM(f.importeTotal) 
                      FROM FacturaVenta f 
                      JOIN f.puntoVenta p 
                      GROUP BY p.descripcion
                      """;
        
        List<Object[]> resultados = em.createQuery(jpql, Object[].class).getResultList();
        
        System.out.println("\nConsulta 15. \nAgrupamiento Básico (GROUP BY):");
        
        if (resultados.isEmpty()) {
            System.out.println("No hay datos para agrupar.");
        } else {
            for (Object[] fila : resultados) {
                String descripcion = (String) fila[0];
                Long cantidadFacturas = (Long) fila[1];
                Double sumaTotal = (Double) fila[2];
                
                System.out.println("Punto de Venta: " + descripcion + " | Cantidad: " + cantidadFacturas + " | Total Facturado: $" + sumaTotal);
            }
        }
    }

    //Consulta 16 --------------------
    public static void usuariosConMasDeCincoFacturas(EntityManager em) {
        
        String jpql = """
                      SELECT u.usuario 
                      FROM FacturaVenta f 
                      JOIN f.usuarioCarga u 
                      GROUP BY u.usuario 
                      HAVING COUNT(f) > 5
                      """;
        
        List<String> usuarios = em.createQuery(jpql, String.class).getResultList();
        
        System.out.println("\nConsulta 16. \nAgrupamiento con Condicional de Grupo (HAVING):");
        
        if (usuarios.isEmpty()) {
            System.out.println("Ningún usuario ha registrado más de 5 facturas.");
        } else {
            for (String usuario : usuarios) {
                System.out.println("- " + usuario);
            }
        }
    }

    //Consulta 17
    public static void totalesVendidosPorMarca(EntityManager em) {
        
        String jpql = """
                      SELECT m.denominacion, SUM(d.cantidad), SUM(d.importeSubtotal) 
                      FROM FacturaVentaDetalle d 
                      JOIN d.listaPrecioArticulo lpa 
                      JOIN lpa.articulo a 
                      JOIN a.marca m 
                      GROUP BY m.denominacion
                      """;
        
        List<Object[]> resultados = em.createQuery(jpql, Object[].class).getResultList();
        
        System.out.println("\nConsulta 17. \nAgrupamiento y Agregación sobre Entidades Relacionadas:");
        
        if (resultados.isEmpty()) {
            System.out.println("No hay detalles de venta registrados para agrupar.");
        } else {
            for (Object[] fila : resultados) {
                String marca = (String) fila[0];
                Double cantidadUnidades = (Double) fila[1];
                Double subtotalAcumulado = (Double) fila[2];
                
                System.out.println("Marca: " + marca + " | Unidades vendidas: " + cantidadUnidades + " | Subtotal: $" + subtotalAcumulado);
            }
        }
    }

        //Nivel 5 -----------------------------------------------------------------------------------------------------------------
    //Consulta 18 -----------
    public static void articulosConVentasRegistradas(EntityManager em) {
        
        String jpql = """
                      SELECT a 
                      FROM Articulo a 
                      WHERE EXISTS (
                          SELECT d 
                          FROM FacturaVentaDetalle d 
                          WHERE d.listaPrecioArticulo.articulo = a
                      )
                      """;
        
        List<Articulo> articulos = em.createQuery(jpql, Articulo.class).getResultList();
        
        System.out.println("\nConsulta 18. \nSubconsulta con Operador de Existencia:");
        
        if (articulos.isEmpty()) {
            System.out.println("Ningún artículo registra ventas hasta el momento.");
        } else {
            for (Articulo a : articulos) {
                System.out.println("Código: " + a.getCodigo() + " | Artículo: " + a.getDenominacion());
            }
        }
    }

    //Consulta 19 --------------------
    public static void articulosSinVentasRegistradas(EntityManager em) {
        
        String jpql = """
                      SELECT a 
                      FROM Articulo a 
                      WHERE NOT EXISTS (
                          SELECT d 
                          FROM FacturaVentaDetalle d 
                          WHERE d.listaPrecioArticulo.articulo = a
                      )
                      """;
        
        List<Articulo> articulos = em.createQuery(jpql, Articulo.class).getResultList();
        
        System.out.println("\nConsulta 19.\n Subconsulta Correlacionada con NOT EXISTS:");
        
        if (articulos.isEmpty()) {
            System.out.println("Todos los artículos han sido incluidos en al menos una factura.");
        } else {
            for (Articulo a : articulos) {
                System.out.println("Código: " + a.getCodigo() + " | Artículo: " + a.getDenominacion());
            }
        }
    }

    //Consulta 20 --------------------------
    public static void clasificarFacturasPorValor(EntityManager em) {
        
        String jpql = """
                      SELECT f.numero, f.importeTotal, 
                             CASE 
                                 WHEN f.importeTotal > 50000 THEN 'ALTO VALOR' 
                                 WHEN f.importeTotal BETWEEN 10000 AND 50000 THEN 'MEDIO VALOR' 
                                 WHEN f.importeTotal < 10000 THEN 'BAJO VALOR' 
                             END 
                      FROM FacturaVenta f 
                      ORDER BY f.importeTotal DESC
                      """;
        
        List<Object[]> resultados = em.createQuery(jpql, Object[].class).getResultList();
        
        System.out.println("\nConsulta 20.\nProyección Condicional (CASE WHEN):");
        
        if (resultados.isEmpty()) {
            System.out.println("No hay facturas registradas para clasificar.");
        } else {
            for (Object[] fila : resultados) {
                Integer numero = (Integer) fila[0]; 
                Double importe = (Double) fila[1];  
                String categoria = (String) fila[2];
                
                System.out.println("Factura Nro: " + numero + " | Total: $" + importe + " | Categoría: " + categoria);
            }
        }
    }

}
