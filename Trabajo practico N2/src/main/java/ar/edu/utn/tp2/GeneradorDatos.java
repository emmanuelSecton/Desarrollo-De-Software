package ar.edu.utn.tp2;

import jakarta.persistence.EntityManager;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

public class GeneradorDatos {

    // Método auxiliar para no repetir código de auditoría en cada objeto
    private static void setAuditoria(AuditoriaApp entidad, Usuario u) {
        entidad.setFechaAlta(new java.util.Date());
        entidad.setFechaModificacion(new java.util.Date());
        entidad.setUsuarioCarga(u);
        entidad.setUsuarioModificac(u);
    }

    public static void cargarDatos(EntityManager em, Usuario u, CondicionIva cIva, TipoMoneda tMoneda) {
        
        // 1. Crear Puntos de Venta (necesitamos los números 2 y 5 para la consulta 9)
        PuntoVenta pv2 = new PuntoVenta(2, "Sucursal Centro", "Tipo A", "Centro 100");
        PuntoVenta pv5 = new PuntoVenta(5, "Sucursal Norte", "Tipo B", "Norte 200");
        setAuditoria(pv2, u); setAuditoria(pv5, u);
        em.persist(pv2); em.persist(pv5);

        // 2. Crear Rubros y Marcas (Rubro "Electrónica" para consulta 3)
        Rubro rubroElec = new Rubro("Electrónica", 2);
        Rubro rubroHogar = new Rubro("Hogar", 3);
        setAuditoria(rubroElec, u); setAuditoria(rubroHogar, u);
        em.persist(rubroElec); em.persist(rubroHogar);

        Marca marcaSamsung = new Marca("Samsung", 2);
        setAuditoria(marcaSamsung, u);
        em.persist(marcaSamsung);

        // 3. Crear Artículos (Uno sin marca para la consulta 12 y otro que no se venda para la 19)
        Articulo artTv = new Articulo(rubroElec, "TV001", "Smart TV 50", marcaSamsung);
        Articulo artGenerico = new Articulo(rubroHogar, "GEN01", "Silla Generica", null); // Sin marca
        Articulo artSinVentas = new Articulo(rubroElec, "SIN01", "Auriculares", marcaSamsung); // Nunca se venderá
        setAuditoria(artTv, u); setAuditoria(artGenerico, u); setAuditoria(artSinVentas, u);
        em.persist(artTv); em.persist(artGenerico); em.persist(artSinVentas);

        // 4. Crear Lista de Precios
        ListaPrecio lp = new ListaPrecio("LP002", "Lista General");
        setAuditoria(lp, u);
        em.persist(lp);

        ListaPrecioArticulo precioTv = new ListaPrecioArticulo(lp, 150000.0, artTv);
        ListaPrecioArticulo precioSilla = new ListaPrecioArticulo(lp, 8000.0, artGenerico);
        setAuditoria(precioTv, u); setAuditoria(precioSilla, u);
        em.persist(precioTv); em.persist(precioSilla);

        // 5. Crear Clientes (Cuit "20-" y nombre "Mundo" para consulta 6)
        Contacto c1 = new Contacto("mail@mundo.com", "111", "222");
        Domicilio d1 = new Domicilio("Mundo Calle", "123");
        Cliente clienteMundo = new Cliente("20-99999999-9", "Mundo Informático", c1, d1);
        setAuditoria(clienteMundo, u);
        em.persist(c1); em.persist(d1); em.persist(clienteMundo);

        // 6. Crear Facturas (Distintos estados y valores para consultas 5 y 20)
        
        // Factura 1: Alto Valor (Mayor a 50k), EMITIDA, sin anular
        FacturaVenta f1 = new FacturaVenta(
            2L, Date.valueOf(LocalDate.now().minusDays(10)), clienteMundo, cIva, tMoneda, pv2,
            0.0, 0.0, 150000.0, null, null, null, null, "EMITIDA", null, null, new ArrayList<>()
        );
        setAuditoria(f1, u);
        f1.getDetalles().add(new FacturaVentaDetalle(f1, precioTv, "Smart TV", 1.0, 150000.0, 0.0, 150000.0, 0.0, 150000.0));
        em.persist(f1);

        // Factura 2: Bajo Valor (Menor a 10k), ANULADA
        FacturaVenta f2 = new FacturaVenta(
            3L, Date.valueOf(LocalDate.now()), clienteMundo, cIva, tMoneda, pv5,
            0.0, 0.0, 8000.0, null, null, null, null, "ANULADA", Date.valueOf(LocalDate.now()), null, new ArrayList<>()
        );
        setAuditoria(f2, u);
        f2.getDetalles().add(new FacturaVentaDetalle(f2, precioSilla, "Silla", 1.0, 8000.0, 0.0, 8000.0, 0.0, 8000.0));
        em.persist(f2);
    }
}
