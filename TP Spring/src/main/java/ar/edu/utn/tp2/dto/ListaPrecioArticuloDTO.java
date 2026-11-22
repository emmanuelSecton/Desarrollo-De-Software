package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListaPrecioArticuloDTO {
    private Long id;
    private Long articuloId;
    private String articuloDenominacion;
    private double precioVenta;
    private Long listaPrecioId;
}