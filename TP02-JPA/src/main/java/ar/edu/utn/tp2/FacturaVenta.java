package ar.edu.utn.tp2;

import java.sql.Date;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "factura_venta")
public class FacturaVenta extends AuditoriaApp {
    private Long numero;

    @Column(nullable = false)
    private Date fechaEmision;

    @ManyToOne
    @JoinColumn(nullable = false)
    private PuntoVenta puntoVenta;

    private double importeCobrado;

    private double importeSaldo;

    @Column(nullable = false)
    private double importeTotal;

    private String cae;

    private Date caeFechaVencimiento;

    private String resultadoAfip;

    private String motivoRechazo;

    @Column(nullable = false)
    private String estado;

    private Date fechaAnulacion;

    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL)
    private List<FacturaVentaDetalle> detalles;

    protected FacturaVenta() {
    }

    public FacturaVenta(Date fechaEmision, PuntoVenta puntoVenta, double importeCobrado, double importeSaldo,
            double importeTotal, String cae, Date caeFechaVencimiento, String resultadoAfip, String motivoRechazo,
            String estado, Date fechaAnulacion, String observaciones, List<FacturaVentaDetalle> detalles) {
        this.fechaEmision = fechaEmision;
        this.puntoVenta = puntoVenta;
        this.importeCobrado = importeCobrado;
        this.importeSaldo = importeSaldo;
        this.importeTotal = importeTotal;
        this.cae = cae;
        this.caeFechaVencimiento = caeFechaVencimiento;
        this.resultadoAfip = resultadoAfip;
        this.motivoRechazo = motivoRechazo;
        this.estado = estado;
        this.fechaAnulacion = fechaAnulacion;
        this.observaciones = observaciones;
        this.detalles = detalles;
    }

}
