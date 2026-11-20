package ar.edu.utn.tp2.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FacturaEdicionDTO {
    private Long id;
    private Long numero;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "America/Argentina/Mendoza")
    private Date fechaEmision;

    private Long clienteId;
    private String clienteDenominacion;
    private String clienteCuitCuil;

    private Long condicionIvaId;
    private Long puntoVentaId;
    private Long tipoMonedaId;
    private String estado;
    private String observaciones;
    private List<FacturaDetalleRequestDTO> detalles;
}