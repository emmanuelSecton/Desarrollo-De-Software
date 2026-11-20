package ar.edu.utn.tp2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos para dar de alta un Cliente junto con su Contacto y Domicilio. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRequestDTO {
    // Cliente
    private String cuitCuil;
    private String denominacion;

    // Contacto
    private String email;
    private String telefono;
    private String celular;

    // Domicilio
    private String nombreCalle;
    private String numeroCalle;
}
