package ar.edu.utn.tp2.controller;

import ar.edu.utn.tp2.dto.ClienteDTO;
import ar.edu.utn.tp2.dto.CondicionIvaDTO;
import ar.edu.utn.tp2.dto.ListaPrecioArticuloDTO;
import ar.edu.utn.tp2.dto.OpcionDTO;
import ar.edu.utn.tp2.dto.TipoMonedaDTO;
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

    /** Monedas vigentes (sin fecha de baja) para el desplegable de la factura. */
    @GetMapping("/monedas")
    public ResponseEntity<List<TipoMonedaDTO>> listarMonedas() {
        String jpql = "SELECT new ar.edu.utn.tp2.dto.TipoMonedaDTO(m.id, m.simbolo, m.denominacion, m.codigoAfip) " +
                      "FROM TipoMoneda m " +
                      "WHERE m.fechaBaja IS NULL " +
                      "ORDER BY m.denominacion ASC";
        return ResponseEntity.ok(em.createQuery(jpql, TipoMonedaDTO.class).getResultList());
    }

    /** Condiciones de IVA vigentes (sin fecha de baja) para el desplegable de la factura. */
    @GetMapping("/condiciones-iva")
    public ResponseEntity<List<CondicionIvaDTO>> listarCondicionesIva() {
        String jpql = "SELECT new ar.edu.utn.tp2.dto.CondicionIvaDTO(c.id, c.denominacion, c.codigoAfip) " +
                      "FROM CondicionIva c " +
                      "WHERE c.fechaBaja IS NULL " +
                      "ORDER BY c.denominacion ASC";
        return ResponseEntity.ok(em.createQuery(jpql, CondicionIvaDTO.class).getResultList());
    }

    /** Rubros, marcas y listas de precio vigentes, para los desplegables del alta de artículo. */
    @GetMapping("/rubros")
    public ResponseEntity<List<OpcionDTO>> listarRubros() {
        return ResponseEntity.ok(listarOpciones("Rubro"));
    }

    @GetMapping("/marcas")
    public ResponseEntity<List<OpcionDTO>> listarMarcas() {
        return ResponseEntity.ok(listarOpciones("Marca"));
    }

    @GetMapping("/listas-precio")
    public ResponseEntity<List<OpcionDTO>> listarListasPrecio() {
        return ResponseEntity.ok(listarOpciones("ListaPrecio"));
    }

    /** entidad es un nombre fijo del código (nunca viene del usuario). */
    private List<OpcionDTO> listarOpciones(String entidad) {
        String jpql = "SELECT new ar.edu.utn.tp2.dto.OpcionDTO(e.id, e.denominacion) FROM " + entidad + " e " +
                      "WHERE e.fechaBaja IS NULL ORDER BY e.denominacion ASC";
        return em.createQuery(jpql, OpcionDTO.class).getResultList();
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