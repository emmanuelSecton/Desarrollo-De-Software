package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Opción genérica (id + denominación) para desplegables: rubros, marcas, listas de precio. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionDTO {
    private Long id;
    private String denominacion;
}
