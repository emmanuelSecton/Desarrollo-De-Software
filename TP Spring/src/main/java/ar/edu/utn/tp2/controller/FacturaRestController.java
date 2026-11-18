package ar.edu.utn.tp2.controller;

import ar.edu.utn.tp2.dto.FacturaReporteDTO;
import ar.edu.utn.tp2.service.FacturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas") 
public class FacturaRestController {

    @Autowired
    private FacturaService facturaService;

    @GetMapping 
    public List<FacturaReporteDTO> obtenerFacturas(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }
}