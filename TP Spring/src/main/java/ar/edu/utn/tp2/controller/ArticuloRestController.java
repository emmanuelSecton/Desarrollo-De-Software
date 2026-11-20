package ar.edu.utn.tp2.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.tp2.dto.ArticuloRequestDTO;
import ar.edu.utn.tp2.dto.ListaPrecioArticuloDTO;
import ar.edu.utn.tp2.service.ArticuloService;

/** Alta de artículos con su precio. La búsqueda sigue en ExplorerRestController (/api/articulos-precio). */
@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloRestController {

    @Autowired
    private ArticuloService articuloService;

    @PostMapping
    public ResponseEntity<ListaPrecioArticuloDTO> crear(@RequestBody ArticuloRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(articuloService.crear(dto));
    }

    /** Devuelve el motivo como texto plano para que el frontend lo muestre tal cual. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> manejarError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.TEXT_PLAIN)
                .body(ex.getReason());
    }
}
