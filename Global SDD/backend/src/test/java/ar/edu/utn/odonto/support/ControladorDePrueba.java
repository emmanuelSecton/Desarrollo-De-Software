package ar.edu.utn.odonto.support;

import ar.edu.utn.odonto.shared.api.Api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador SOLO DE TEST: permite probar validacion y errores sin endpoints de negocio (D5).
 * Vive en el classpath de test y nunca se empaqueta.
 */
@RestController
@RequestMapping(Api.V1 + "/__test__")
public class ControladorDePrueba {

    public record Entrada(@NotBlank String nombre, @Min(1) Integer edad) {}

    @PostMapping("/validar")
    public Entrada validar(@Valid @RequestBody Entrada entrada) {
        return entrada;
    }

    @GetMapping("/parametro")
    public String parametro(@RequestParam @Min(1) int cantidad) {
        return "ok " + cantidad;
    }

    @PostMapping("/explota")
    public void explota(@RequestBody String cuerpo) {
        throw new IllegalStateException("detalle interno SELECT * FROM paciente WHERE dni=12345678");
    }
}
