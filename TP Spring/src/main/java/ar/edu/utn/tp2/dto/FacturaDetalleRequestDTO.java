package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaDetalleRequestDTO {
    private Long articuloId;
    private String descripcion;
    private double cantidad;
    private Double precioUnitario; // Opcional: si es null, se toma el de ListaPrecioArticulo
    private double porcentajeBonificacion;
    private Long listaPrecioArticuloId; // Precio (de la lista elegida) del que sale el ítem; si es null, se usa el primero del artículo
}