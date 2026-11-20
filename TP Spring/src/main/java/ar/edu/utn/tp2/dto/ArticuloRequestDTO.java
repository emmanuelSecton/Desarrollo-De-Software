package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos para dar de alta un Articulo junto con su precio en una lista (ListaPrecioArticulo). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloRequestDTO {
    // Articulo
    private String codigo;
    private String denominacion;
    private Long rubroId;   // opcional
    private Long marcaId;   // opcional

    // ListaPrecioArticulo
    private Long listaPrecioId;
    private Double precioVenta;
}
