package ar.edu.utn.tp2.service;

import ar.edu.utn.tp2.*;
import ar.edu.utn.tp2.dto.FacturaDetalleRequestDTO;
import ar.edu.utn.tp2.dto.FacturaReporteDTO;
import ar.edu.utn.tp2.dto.FacturaRequestDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class FacturaService {

    private static final double ALICUOTA_IVA = 0.21;

    @PersistenceContext
    private EntityManager em;

    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta, String estado, Double montoMinimo) {

        StringBuilder jpql = new StringBuilder(
            "SELECT new ar.edu.utn.tp2.dto.FacturaReporteDTO(" +
            "f.id, f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
            "ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d), f.estado" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "LEFT JOIN f.condicionIva ci " +
            "LEFT JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d " +
            "WHERE 1=1 "
        );

        Map<String, Object> params = new HashMap<>();

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

        jpql.append("GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal, f.estado");

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);

        return query.getResultList();
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO dto) {
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura debe contener al menos un detalle.");
        }

        // 1. Buscar entidades asociadas por ID
        PuntoVenta puntoVenta = buscarEntidadObligatoria(PuntoVenta.class, dto.getPuntoVentaId(), "Punto de venta");
        CondicionIva condicionIva = buscarEntidadObligatoria(CondicionIva.class, dto.getCondicionIvaId(), "Condición de IVA");
        TipoMoneda tipoMoneda = buscarEntidadObligatoria(TipoMoneda.class, dto.getTipoMonedaId(), "Tipo de moneda");

        Cliente cliente = null;
        if (dto.getClienteId() != null) {
            cliente = buscarEntidadObligatoria(Cliente.class, dto.getClienteId(), "Cliente");
        }

        Usuario usuario = obtenerUsuarioPorDefecto();

        // 2. Mapear cabecera de FacturaVenta
        FacturaVenta factura = new FacturaVenta();
        factura.setNumero(dto.getNumero());
        factura.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : new Date());
        factura.setCliente(cliente);
        factura.setCondicionIva(condicionIva);
        factura.setTipoMoneda(tipoMoneda);
        factura.setPuntoVenta(puntoVenta);
        factura.setEstado(dto.getEstado() != null && !dto.getEstado().isBlank() ? dto.getEstado() : "EMITIDA");
        factura.setObservaciones(dto.getObservaciones());
        factura.setDetalles(new ArrayList<>());

        // Auditoría obligatoria
        Date ahora = new Date();
        factura.setFechaAlta(ahora);
        factura.setFechaModificacion(ahora);
        factura.setUsuarioCarga(usuario);
        factura.setUsuarioModificac(usuario);

        // 3. Procesar detalles y calcular importes
        double totalFactura = cargarDetallesYCalcularTotal(factura, dto.getDetalles());
        factura.setImporteTotal(totalFactura);
        factura.setImporteCobrado(0.0);
        factura.setImporteSaldo(totalFactura);

        // 4. Persistir en cascada (CascadeType.ALL en FacturaVenta.detalles)
        em.persist(factura);
        em.flush();

        return mapearAReporteDTO(factura);
    }

    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }

        // Actualizar cabecera (solo los campos enviados)
        if (dto.getNumero() != null) {
            factura.setNumero(dto.getNumero());
        }
        if (dto.getFechaEmision() != null) {
            factura.setFechaEmision(dto.getFechaEmision());
        }
        if (dto.getClienteId() != null) {
            factura.setCliente(buscarEntidadObligatoria(Cliente.class, dto.getClienteId(), "Cliente"));
        }
        if (dto.getCondicionIvaId() != null) {
            factura.setCondicionIva(buscarEntidadObligatoria(CondicionIva.class, dto.getCondicionIvaId(), "Condición de IVA"));
        }
        if (dto.getPuntoVentaId() != null) {
            factura.setPuntoVenta(buscarEntidadObligatoria(PuntoVenta.class, dto.getPuntoVentaId(), "Punto de venta"));
        }
        if (dto.getTipoMonedaId() != null) {
            factura.setTipoMoneda(buscarEntidadObligatoria(TipoMoneda.class, dto.getTipoMonedaId(), "Tipo de moneda"));
        }
        if (dto.getObservaciones() != null) {
            factura.setObservaciones(dto.getObservaciones());
        }
        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            factura.setEstado(dto.getEstado());
            if ("ANULADA".equalsIgnoreCase(dto.getEstado()) && factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(new Date());
            }
        }

        // Actualizar ítems si vienen en el request
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            factura.getDetalles().clear(); // orphanRemoval = true elimina los anteriores
            double nuevoTotal = cargarDetallesYCalcularTotal(factura, dto.getDetalles());
            factura.setImporteTotal(nuevoTotal);
            factura.setImporteSaldo(nuevoTotal - factura.getImporteCobrado());
        }

        factura.setFechaModificacion(new Date());
        em.merge(factura);
        em.flush();

        return mapearAReporteDTO(factura);
    }

    @Transactional
    public void eliminar(Long id) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }
        em.remove(factura);
    }

    // --- Métodos auxiliares ---

    private double cargarDetallesYCalcularTotal(FacturaVenta factura, List<FacturaDetalleRequestDTO> detallesDTO) {
        double totalFactura = 0.0;

        for (FacturaDetalleRequestDTO itemDto : detallesDTO) {
            ListaPrecioArticulo lpa = buscarListaPrecioPorArticuloId(itemDto.getArticuloId());

            double precioUnitario = (itemDto.getPrecioUnitario() != null && itemDto.getPrecioUnitario() > 0)
                    ? itemDto.getPrecioUnitario()
                    : lpa.getPrecioVenta();

            double bruto = itemDto.getCantidad() * precioUnitario;
            double descuento = bruto * (itemDto.getPorcentajeBonificacion() / 100.0);
            double importeNeto = bruto - descuento;
            double importeIva = importeNeto * ALICUOTA_IVA;
            double importeSubtotal = importeNeto + importeIva;

            FacturaVentaDetalle detalle = new FacturaVentaDetalle();
            detalle.setFactura(factura);
            detalle.setListaPrecioArticulo(lpa);
            detalle.setDescripcion(
                    itemDto.getDescripcion() != null && !itemDto.getDescripcion().isBlank()
                            ? itemDto.getDescripcion()
                            : lpa.getArticulo().getDenominacion()
            );
            detalle.setCantidad(itemDto.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setPorcentajeBonificacion(itemDto.getPorcentajeBonificacion());
            detalle.setImporteNeto(importeNeto);
            detalle.setImporteIva(importeIva);
            detalle.setImporteSubtotal(importeSubtotal);

            factura.getDetalles().add(detalle);
            totalFactura += importeSubtotal;
        }

        return totalFactura;
    }

    private <T> T buscarEntidadObligatoria(Class<T> clazz, Long id, String nombreEntidad) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, nombreEntidad + " ID es obligatorio.");
        }
        T entidad = em.find(clazz, id);
        if (entidad == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, nombreEntidad + " no encontrado con ID: " + id);
        }
        return entidad;
    }

    private ListaPrecioArticulo buscarListaPrecioPorArticuloId(Long articuloId) {
        if (articuloId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "articuloId es obligatorio en cada detalle.");
        }
        List<ListaPrecioArticulo> resultados = em.createQuery(
                "SELECT lpa FROM ListaPrecioArticulo lpa WHERE lpa.articulo.id = :articuloId",
                ListaPrecioArticulo.class
        ).setParameter("articuloId", articuloId).setMaxResults(1).getResultList();

        if (resultados.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No se encontró un precio registrado (ListaPrecioArticulo) para el artículo ID: " + articuloId
            );
        }
        return resultados.get(0);
    }

    private Usuario obtenerUsuarioPorDefecto() {
        List<Usuario> usuarios = em.createQuery("SELECT u FROM Usuario u", Usuario.class)
                .setMaxResults(1)
                .getResultList();
        if (usuarios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No hay usuarios registrados para la auditoría.");
        }
        return usuarios.get(0);
    }

    private FacturaReporteDTO mapearAReporteDTO(FacturaVenta f) {
        return new FacturaReporteDTO(
                f.getId(),
                f.getNumero(),
                f.getFechaEmision(),
                f.getCliente() != null ? f.getCliente().getDenominacion() : "Consumidor Final",
                f.getCondicionIva() != null ? f.getCondicionIva().getDenominacion() : null,
                f.getPuntoVenta() != null ? f.getPuntoVenta().getDescripcion() : null,
                f.getImporteTotal(),
                f.getDetalles() != null ? f.getDetalles().size() : 0L,
                f.getEstado()
        );
    }
}