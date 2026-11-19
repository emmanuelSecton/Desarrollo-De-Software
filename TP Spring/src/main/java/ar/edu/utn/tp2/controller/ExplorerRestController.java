package ar.edu.utn.tp2.controller;

import ar.edu.utn.tp2.dto.ClienteDTO;
import ar.edu.utn.tp2.dto.ListaPrecioArticuloDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ExplorerRestController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteDTO>> buscarClientes(
            @RequestParam(required = false) String busqueda
    ) {
        String jpql = "SELECT new ar.edu.utn.tp2.dto.ClienteDTO(c.id, c.denominacion, c.cuitCuil) " +
                      "FROM Cliente c " +
                      "WHERE (:busqueda IS NULL OR LOWER(c.denominacion) LIKE LOWER(:busqueda) " +
                      "OR LOWER(COALESCE(c.cuitCuil, '')) LIKE LOWER(:busqueda)) " +
                      "ORDER BY c.denominacion ASC";

        TypedQuery<ClienteDTO> query = em.createQuery(jpql, ClienteDTO.class);
        String filtro = (busqueda != null && !busqueda.trim().isEmpty()) ? "%" + busqueda.trim() + "%" : null;
        query.setParameter("busqueda", filtro);

        return ResponseEntity.ok(query.getResultList());
    }

    @GetMapping("/articulos-precio")
    public ResponseEntity<List<ListaPrecioArticuloDTO>> buscarArticulosPrecio(
            @RequestParam(required = false) String busqueda
    ) {
        String jpql = "SELECT new ar.edu.utn.tp2.dto.ListaPrecioArticuloDTO(" +
                      "lpa.id, a.id, a.denominacion, lpa.precioVenta) " +
                      "FROM ListaPrecioArticulo lpa " +
                      "JOIN lpa.articulo a " +
                      "WHERE (:busqueda IS NULL OR LOWER(a.denominacion) LIKE LOWER(:busqueda)) " +
                      "ORDER BY a.denominacion ASC";

        TypedQuery<ListaPrecioArticuloDTO> query = em.createQuery(jpql, ListaPrecioArticuloDTO.class);
        String filtro = (busqueda != null && !busqueda.trim().isEmpty()) ? "%" + busqueda.trim() + "%" : null;
        query.setParameter("busqueda", filtro);

        return ResponseEntity.ok(query.getResultList());
    }
}