package ar.edu.utn.tp2.service;

import ar.edu.utn.tp2.dto.FacturaReporteDTO;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {
        
        // Usamos StringBuilder para armar la consulta dinámicamente con el truco WHERE 1=1
        StringBuilder jpql = new StringBuilder(
            "SELECT new ar.edu.utn.tp2.dto.FacturaReporteDTO(" +
            "f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
            "ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d)" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "LEFT JOIN f.condicionIva ci " +
            "LEFT JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d " +
            //
            "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

        // Agregamos las condiciones solo si el usuario mandó los parámetros
        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado);
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }
}
