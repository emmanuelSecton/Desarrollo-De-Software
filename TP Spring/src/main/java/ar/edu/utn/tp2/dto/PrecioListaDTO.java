package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Precio de un artículo en una lista, para el alta de artículos. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrecioListaDTO {
    private Long listaPrecioId;
    private Double precioVenta;
}
