package ar.edu.utn.tp2;

import java.util.Date;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

// TODO: Agregar @Entity y @Table -----------------------------------------------------------
    @Entity
    @Table(name = "factura_venta")


public class FacturaVenta extends AuditoriaApp { 

    private Long numero; 

// TODO: Configurar @Column(nullable = false) --------------------------------------------------
    @Column(nullable = false)
    private Date fechaEmision; 

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = true)
    @ManyToOne 
    @JoinColumn(nullable = true) 
    private Cliente cliente;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    @ManyToOne
    @JoinColumn(nullable = false)
    private CondicionIva condicionIva;

 // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    @ManyToOne
    @JoinColumn(nullable = false)
    private TipoMoneda tipoMoneda;

// TODO: Configurar @ManyToOne y @JoinColumn(nullable = false) -----------------------------------
    @ManyToOne 
    @JoinColumn(nullable = false)
    private PuntoVenta puntoVenta; 
    private double importeCobrado; 
    private double importeSaldo; 


// TODO: Configurar @Column(nullable = false) -------------------------------------------------------
    @Column (nullable = false)
    private double importeTotal; 
    private String cae; 
    private Date caeFechaVencimiento; 
    private String resultadoAfip; 
    private String motivoRechazo; 


// TODO: Configurar @Column(nullable = false) ---------------------------------------------------------
    @Column (nullable = false)
    private String estado; 
    private Date fechaAnulacion; 
    private String observaciones; 

//TODO: Configurar @OneToMany(mappedBy = "facturaVenta", cascade = CascadeType.ALL) --------------------
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL)
    private List<FacturaVentaDetalle> detalles = new java.util.ArrayList<>();

    //Constructores --------------------------------------------------------------------------------------

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

    //getters y setters ---------------------------------------------------------------------------------

    public Long getNumero() {
        return numero;
    }

    public void setNumero(Long numero) {
        this.numero = numero;
    }

    public Date getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(Date fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public PuntoVenta getPuntoVenta() {
        return puntoVenta;
    }

    public void setPuntoVenta(PuntoVenta puntoVenta) {
        this.puntoVenta = puntoVenta;
    }

    public double getImporteCobrado() {
        return importeCobrado;
    }

    public void setImporteCobrado(double importeCobrado) {
        this.importeCobrado = importeCobrado;
    }

    public double getImporteSaldo() {
        return importeSaldo;
    }

    public void setImporteSaldo(double importeSaldo) {
        this.importeSaldo = importeSaldo;
    }

    public double getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public String getCae() {
        return cae;
    }

    public void setCae(String cae) {
        this.cae = cae;
    }

    public Date getCaeFechaVencimiento() {
        return caeFechaVencimiento;
    }

    public void setCaeFechaVencimiento(Date caeFechaVencimiento) {
        this.caeFechaVencimiento = caeFechaVencimiento;
    }

    public String getResultadoAfip() {
        return resultadoAfip;
    }

    public void setResultadoAfip(String resultadoAfip) {
        this.resultadoAfip = resultadoAfip;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Date getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(Date fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<FacturaVentaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<FacturaVentaDetalle> detalles) {
        this.detalles = detalles;
    }
    


}
