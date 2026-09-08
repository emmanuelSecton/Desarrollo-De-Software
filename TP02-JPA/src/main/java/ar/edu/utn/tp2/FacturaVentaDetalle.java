package ar.edu.utn.tp2;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "factura_venta_detalle")
public class FacturaVentaDetalle extends EntityId {
    @ManyToOne
    @JoinColumn(nullable = false)
    private FacturaVenta factura;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ListaPrecioArticulo listaPrecioArticulo;

    private String descripcion;

    @Column(nullable = false)
    private double cantidad;

    @Column(nullable = false)
    private double precioUnitario;

    private double porcentajeBonificacion;

    private double importeNeto;

    private double importeIva;

    @Column(nullable = false)
    private double importeSubtotal;

    protected FacturaVentaDetalle() {
    }

    public FacturaVentaDetalle(FacturaVenta factura, ListaPrecioArticulo listaPrecioArticulo, String descripcion,
            double cantidad, double precioUnitario, double porcentajeBonificacion, double importeNeto,
            double importeIva, double importeSubtotal) {
        this.factura = factura;
        this.listaPrecioArticulo = listaPrecioArticulo;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.porcentajeBonificacion = porcentajeBonificacion;
        this.importeNeto = importeNeto;
        this.importeIva = importeIva;
        this.importeSubtotal = importeSubtotal;
    }
}
