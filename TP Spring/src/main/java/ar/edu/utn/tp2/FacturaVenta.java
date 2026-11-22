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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// TODO: Agregar @Entity y @Table -----------------------------------------------------------
@Entity
@Table(name = "factura_venta", uniqueConstraints = @UniqueConstraint(
        name = FacturaVenta.UK_NUMERO_PUNTO_VENTA, columnNames = { "punto_venta_id", "numero" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = { "detalles" })
@ToString(exclude = { "detalles" })

public class FacturaVenta extends AuditoriaApp {

    /** Restricción única: el número de factura no se repite dentro de un punto de venta. */
    public static final String UK_NUMERO_PUNTO_VENTA = "uk_factura_venta_pto_numero";

    private Long numero;

    // TODO: Configurar @Column(nullable = false)
    // --------------------------------------------------
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

    // TODO: Configurar @ManyToOne y @JoinColumn(nullable = false)
    // -----------------------------------
    @ManyToOne
    @JoinColumn(nullable = false)
    private PuntoVenta puntoVenta;
    private double importeCobrado;
    private double importeSaldo;

    // TODO: Configurar @Column(nullable = false)
    // -------------------------------------------------------
    @Column(nullable = false)
    private double importeTotal;
    private String cae;
    private Date caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;

    // TODO: Configurar @Column(nullable = false)
    // ---------------------------------------------------------
    @Column(nullable = false)
    private String estado;
    private Date fechaAnulacion;
    private String observaciones;

    // TODO: Configurar @OneToMany(mappedBy = "facturaVenta", cascade =
    // CascadeType.ALL) --------------------
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaVentaDetalle> detalles;

}
