package ar.edu.utn.tp2.controller;

import ar.edu.utn.tp2.dto.FacturaReporteDTO;
import ar.edu.utn.tp2.dto.FacturaRequestDTO;
import ar.edu.utn.tp2.dto.PageResponseDTO;
import ar.edu.utn.tp2.service.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    @Autowired
    private FacturaService facturaService;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdfBytes = facturaService.generarPdfFactura(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=factura-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    @GetMapping
    public ResponseEntity<PageResponseDTO<FacturaReporteDTO>> obtenerFacturas(
            @RequestParam(required = false) Long numeroFactura,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) Boolean informadaArca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(
                facturaService.buscarFacturasPaginadas(
                        numeroFactura, fechaDesde, fechaHasta, cliente, informadaArca, page, size
                )
        );
    }

    @PostMapping("/{id}/informar-arca")
    public ResponseEntity<FacturaReporteDTO> informarArca(@PathVariable Long id) {
        FacturaReporteDTO autorizada = facturaService.informarArca(id);
        return ResponseEntity.ok(autorizada);
    }

    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crearFactura(@RequestBody FacturaRequestDTO request) {
        FacturaReporteDTO creada = facturaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizarFactura(
            @PathVariable Long id,
            @RequestBody FacturaRequestDTO request) {
        FacturaReporteDTO actualizada = facturaService.actualizar(id, request);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Devuelve el motivo del error como texto plano para que el frontend lo muestre. */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<String> manejarError(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.getReason());
    }
}