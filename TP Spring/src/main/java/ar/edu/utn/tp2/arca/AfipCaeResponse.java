package ar.edu.utn.tp2.arca;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AfipCaeResponse {
    private String resultado;           // "A" (Aprobado) o "R" (Rechazado)
    private String cae;
    private String caeFechaVencimiento; // Formato yyyyMMdd
    private long numeroComprobante;
    private String observaciones;
}