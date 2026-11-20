package ar.edu.utn.tp2.service;

import ar.edu.utn.tp2.*;
import ar.edu.utn.tp2.arca.AfipCaeResponse;
import ar.edu.utn.tp2.arca.AfipWsaaClient;
import ar.edu.utn.tp2.arca.AfipWsfeClient;
import ar.edu.utn.tp2.dto.FacturaDetalleRequestDTO;
import ar.edu.utn.tp2.dto.FacturaEdicionDTO;
import ar.edu.utn.tp2.dto.FacturaReporteDTO;
import ar.edu.utn.tp2.dto.FacturaRequestDTO;
import ar.edu.utn.tp2.dto.PageResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FacturaService {

    private static final double ALICUOTA_IVA = 0.21;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AfipWsaaClient wsaaClient;

    @Autowired
    private AfipWsfeClient wsfeClient;

    public FacturaEdicionDTO obtenerPorId(Long id) {
        FacturaVenta f = em.find(FacturaVenta.class, id);
        if (f == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }

        List<FacturaDetalleRequestDTO> detalles = new ArrayList<>();
        if (f.getDetalles() != null) {
            for (FacturaVentaDetalle d : f.getDetalles()) {
                detalles.add(new FacturaDetalleRequestDTO(
                        d.getListaPrecioArticulo().getArticulo().getId(),
                        d.getDescripcion(),
                        d.getCantidad(),
                        d.getPrecioUnitario(),
                        d.getPorcentajeBonificacion()
                ));
            }
        }

        return new FacturaEdicionDTO(
                f.getId(),
                f.getNumero(),
                f.getFechaEmision(),
                f.getCliente() != null ? f.getCliente().getId() : null,
                f.getCliente() != null ? f.getCliente().getDenominacion() : "Consumidor Final",
                f.getCliente() != null ? f.getCliente().getCuitCuil() : null,
                f.getCondicionIva() != null ? f.getCondicionIva().getId() : null,
                f.getPuntoVenta() != null ? f.getPuntoVenta().getId() : null,
                f.getTipoMoneda() != null ? f.getTipoMoneda().getId() : null,
                f.getEstado(),
                f.getObservaciones(),
                detalles
        );
    }
    public byte[] generarPdfFactura(Long id) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            // Cabecera
            Paragraph titulo = new Paragraph("FACTURA DE VENTA N° " + (factura.getNumero() != null ? factura.getNumero() : factura.getId()), tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(15);
            document.add(titulo);

            String fechaStr = factura.getFechaEmision() != null ? sdf.format(factura.getFechaEmision()) : "-";
            String clienteStr = factura.getCliente() != null ? factura.getCliente().getDenominacion() : "Consumidor Final";
            String cuitStr = (factura.getCliente() != null && factura.getCliente().getCuitCuil() != null)
                    ? factura.getCliente().getCuitCuil() : "-";
            String condIvaStr = factura.getCondicionIva() != null ? factura.getCondicionIva().getDenominacion() : "-";
            String ptoVtaStr = factura.getPuntoVenta() != null ? factura.getPuntoVenta().getDescripcion() : "-";

            document.add(new Paragraph("Fecha de Emisión: " + fechaStr, normalFont));
            document.add(new Paragraph("Punto de Venta: " + ptoVtaStr, normalFont));
            document.add(new Paragraph("Cliente: " + clienteStr + "  |  CUIT/CUIL: " + cuitStr, normalFont));
            document.add(new Paragraph("Condición IVA: " + condIvaStr, normalFont));
            document.add(new Paragraph("Estado: " + factura.getEstado(), subtituloFont));
            document.add(new Paragraph(" "));

            // Tabla de detalles
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1.2f, 1.6f, 1.2f, 1.8f});

            String[] headers = {"Descripción", "Cant.", "Precio Unit.", "% Bonif.", "Subtotal"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, subtituloFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(6);
                table.addCell(cell);
            }

            if (factura.getDetalles() != null) {
                for (FacturaVentaDetalle d : factura.getDetalles()) {
                    table.addCell(new Phrase(d.getDescripcion() != null ? d.getDescripcion() : "-", normalFont));
                    table.addCell(new Phrase(String.format(Locale.US, "%.2f", d.getCantidad()), normalFont));
                    table.addCell(new Phrase(String.format(Locale.US, "$%.2f", d.getPrecioUnitario()), normalFont));
                    table.addCell(new Phrase(String.format(Locale.US, "%.2f%%", d.getPorcentajeBonificacion()), normalFont));
                    table.addCell(new Phrase(String.format(Locale.US, "$%.2f", d.getImporteSubtotal()), normalFont));
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Total
            Paragraph total = new Paragraph(
                    String.format(Locale.US, "IMPORTE TOTAL: $%.2f", factura.getImporteTotal()),
                    tituloFont
            );
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingAfter(20);
            document.add(total);

            // Sección ARCA / AFIP (CAE y Fecha de Vencimiento)
            PdfPTable arcaBox = new PdfPTable(1);
            arcaBox.setWidthPercentage(100);
            String caeTexto = (factura.getCae() != null && !factura.getCae().isBlank())
                    ? factura.getCae()
                    : "Comprobante aún no informado a ARCA";
            String vtoTexto = factura.getCaeFechaVencimiento() != null
                    ? sdf.format(factura.getCaeFechaVencimiento())
                    : "-";

            PdfPCell arcaCell = new PdfPCell();
            arcaCell.setPadding(10);
            arcaCell.addElement(new Paragraph("DATOS DE AUTORIZACIÓN ARCA / AFIP", subtituloFont));
            arcaCell.addElement(new Paragraph("CAE: " + caeTexto, normalFont));
            arcaCell.addElement(new Paragraph("Fecha Vto. ARCA: " + vtoTexto, normalFont));
            arcaBox.addCell(arcaCell);
            document.add(arcaBox);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar PDF: " + e.getMessage());
        }
    }

    public PageResponseDTO<FacturaReporteDTO> buscarFacturasPaginadas(
            Long numeroFactura,
            Date fechaDesde,
            Date fechaHasta,
            String cliente,
            Boolean informadaArca,
            int page,
            int size
    ) {
        Map<String, Object> params = new HashMap<>();
        String whereClause = armarFiltros(numeroFactura, fechaDesde, fechaHasta, cliente, informadaArca, params);

        // 1. Contar total de facturas que cumplen los filtros
        String countJpql = "SELECT COUNT(DISTINCT f.id) FROM FacturaVenta f LEFT JOIN f.cliente c " + whereClause;
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);
        params.forEach(countQuery::setParameter);
        long totalElements = countQuery.getSingleResult();

        // 2. Traer solo la página solicitada
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 5;

        TypedQuery<FacturaReporteDTO> dataQuery = em.createQuery(consultaReporte(whereClause), FacturaReporteDTO.class);
        params.forEach(dataQuery::setParameter);
        dataQuery.setFirstResult(safePage * safeSize);
        dataQuery.setMaxResults(safeSize);

        List<FacturaReporteDTO> content = dataQuery.getResultList();
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return new PageResponseDTO<>(content, safePage, safeSize, totalElements, totalPages);
    }

    /** Todas las facturas que cumplen los filtros (sin paginar), en el mismo orden que la grilla. */
    public List<FacturaReporteDTO> listarFacturasFiltradas(
            Long numeroFactura, Date fechaDesde, Date fechaHasta, String cliente, Boolean informadaArca) {
        Map<String, Object> params = new HashMap<>();
        String whereClause = armarFiltros(numeroFactura, fechaDesde, fechaHasta, cliente, informadaArca, params);
        TypedQuery<FacturaReporteDTO> query = em.createQuery(consultaReporte(whereClause), FacturaReporteDTO.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }

    /** Reporte PDF con las facturas filtradas (o todas si no hay filtros), con las columnas de la grilla. */
    public byte[] generarPdfReporte(
            Long numeroFactura, Date fechaDesde, Date fechaHasta, String cliente, Boolean informadaArca) {
        List<FacturaReporteDTO> facturas =
                listarFacturasFiltradas(numeroFactura, fechaDesde, fechaHasta, cliente, informadaArca);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        java.text.NumberFormat formatoMoneda = java.text.NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font encabezadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font chicoFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.DARK_GRAY);
            Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            // Cabecera del reporte
            Paragraph titulo = new Paragraph("REPORTE DE FACTURAS DE VENTA", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(6);
            document.add(titulo);

            Paragraph generado = new Paragraph(
                    "Generado el " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()), chicoFont);
            generado.setAlignment(Element.ALIGN_CENTER);
            document.add(generado);

            // Filtros aplicados
            List<String> filtros = new ArrayList<>();
            if (numeroFactura != null) filtros.add("Nº " + numeroFactura);
            if (fechaDesde != null) filtros.add("Desde " + sdf.format(fechaDesde));
            if (fechaHasta != null) filtros.add("Hasta " + sdf.format(fechaHasta));
            if (cliente != null && !cliente.isBlank()) filtros.add("Cliente contiene \"" + cliente.trim() + "\"");
            if (informadaArca != null) filtros.add(informadaArca ? "Autorizadas por ARCA" : "Pendientes de ARCA");
            Paragraph filtrosP = new Paragraph(
                    "Filtros: " + (filtros.isEmpty() ? "ninguno (todas las facturas)" : String.join(" · ", filtros)),
                    normalFont);
            filtrosP.setSpacingBefore(10);
            filtrosP.setSpacingAfter(10);
            document.add(filtrosP);

            // Tabla con las columnas de la grilla (sin Acciones)
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.1f, 1.2f, 3f, 2f, 1.6f, 1.3f, 2.6f});
            table.setHeaderRows(1);

            BaseColor colorEncabezado = new BaseColor(37, 99, 235);
            String[] headers = {"Nº", "Fecha", "Cliente", "Punto de venta", "Total", "Estado", "ARCA"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, encabezadoFont));
                cell.setBackgroundColor(colorEncabezado);
                cell.setPadding(6);
                if (h.equals("Total")) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(cell);
            }

            double sumaTotal = 0;
            for (FacturaReporteDTO f : facturas) {
                table.addCell(celda(f.getNumeroFactura() != null ? String.valueOf(f.getNumeroFactura()) : "-", normalFont));
                table.addCell(celda(f.getFechaEmision() != null ? sdf.format(f.getFechaEmision()) : "-", normalFont));
                table.addCell(celda(f.getClienteDenominacion() != null ? f.getClienteDenominacion() : "-", normalFont));
                table.addCell(celda(f.getPuntoVentaDescripcion() != null ? f.getPuntoVentaDescripcion() : "-", normalFont));
                PdfPCell total = celda(formatoMoneda.format(f.getImporteTotal()), normalFont);
                total.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(total);
                table.addCell(celda(f.getEstado() != null ? f.getEstado() : "-", normalFont));

                boolean autorizada = f.getCae() != null && !f.getCae().isBlank();
                String arca = autorizada
                        ? "Autorizada\nCAE " + f.getCae()
                          + (f.getCaeFechaVencimiento() != null ? "\nVence " + sdf.format(f.getCaeFechaVencimiento()) : "")
                        : "ANULADA".equals(f.getEstado()) ? "No presentada" : "Pendiente";
                table.addCell(celda(arca, normalFont));
                sumaTotal += f.getImporteTotal();
            }

            if (facturas.isEmpty()) {
                PdfPCell vacio = celda("No se encontraron facturas con esos filtros.", normalFont);
                vacio.setColspan(7);
                vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(vacio);
            }
            document.add(table);

            // Resumen
            Paragraph resumen = new Paragraph(
                    facturas.size() + " factura" + (facturas.size() == 1 ? "" : "s")
                            + "  ·  Importe total: " + formatoMoneda.format(sumaTotal),
                    totalFont);
            resumen.setAlignment(Element.ALIGN_RIGHT);
            resumen.setSpacingBefore(10);
            document.add(resumen);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el reporte PDF: " + e.getMessage());
        }
    }

    private static PdfPCell celda(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setPadding(5);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        return cell;
    }

    /** SELECT de las facturas para la grilla y el reporte, con el WHERE recibido. */
    private static String consultaReporte(String whereClause) {
        return "SELECT new ar.edu.utn.tp2.dto.FacturaReporteDTO(" +
            "f.id, f.numero, f.fechaEmision, COALESCE(c.denominacion, 'Consumidor Final'), " +
            "ci.denominacion, pv.descripcion, f.importeTotal, COUNT(d), f.estado, " +
            "f.cae, f.caeFechaVencimiento, f.resultadoAfip" +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "LEFT JOIN f.condicionIva ci " +
            "LEFT JOIN f.puntoVenta pv " +
            "LEFT JOIN f.detalles d " +
            whereClause +
            "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, ci.denominacion, pv.descripcion, f.importeTotal, f.estado, f.cae, f.caeFechaVencimiento, f.resultadoAfip " +
            "ORDER BY f.id DESC";
    }

    /** Arma el WHERE de los filtros de facturas y carga sus parámetros en {@code params}. */
    private static String armarFiltros(
            Long numeroFactura, Date fechaDesde, Date fechaHasta, String cliente, Boolean informadaArca,
            Map<String, Object> params) {
        StringBuilder whereClause = new StringBuilder("WHERE 1=1 ");

        if (numeroFactura != null) {
            whereClause.append("AND f.numero = :numeroFactura ");
            params.put("numeroFactura", numeroFactura);
        }
        if (fechaDesde != null) {
            whereClause.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            whereClause.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (cliente != null && !cliente.trim().isEmpty()) {
            whereClause.append("AND LOWER(COALESCE(c.denominacion, 'Consumidor Final')) LIKE LOWER(:cliente) ");
            params.put("cliente", "%" + cliente.trim() + "%");
        }
        if (informadaArca != null) {
            if (informadaArca) {
                whereClause.append("AND f.cae IS NOT NULL AND f.cae <> '' ");
            } else {
                // Pendientes: sin CAE y no anuladas (a las anuladas no les aplica informar a ARCA)
                whereClause.append("AND (f.cae IS NULL OR f.cae = '') AND (f.estado IS NULL OR f.estado <> 'ANULADA') ");
            }
        }

        return whereClause.toString();
    }

    @Transactional
    public FacturaReporteDTO crear(FacturaRequestDTO dto) {
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La factura debe contener al menos un detalle.");
        }

        PuntoVenta puntoVenta = buscarEntidadObligatoria(PuntoVenta.class, dto.getPuntoVentaId(), "Punto de venta");

        // El número de factura es único dentro de cada punto de venta (como en ARCA)
        if (dto.getNumero() != null) {
            Long existentes = em.createQuery(
                            "SELECT COUNT(f) FROM FacturaVenta f WHERE f.numero = :numero AND f.puntoVenta = :puntoVenta",
                            Long.class)
                    .setParameter("numero", dto.getNumero())
                    .setParameter("puntoVenta", puntoVenta)
                    .getSingleResult();
            if (existentes > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe una factura con el número " + dto.getNumero()
                                + " en el punto de venta " + puntoVenta.getNumero() + ".");
            }
        }
        CondicionIva condicionIva = buscarEntidadObligatoria(CondicionIva.class, dto.getCondicionIvaId(), "Condición de IVA");
        TipoMoneda tipoMoneda = buscarEntidadObligatoria(TipoMoneda.class, dto.getTipoMonedaId(), "Tipo de moneda");

        Cliente cliente = null;
        if (dto.getClienteId() != null) {
            cliente = buscarEntidadObligatoria(Cliente.class, dto.getClienteId(), "Cliente");
        }

        Usuario usuario = obtenerUsuarioPorDefecto();

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

        Date ahora = new Date();
        factura.setFechaAlta(ahora);
        factura.setFechaModificacion(ahora);
        factura.setUsuarioCarga(usuario);
        factura.setUsuarioModificac(usuario);

        double totalFactura = cargarDetallesYCalcularTotal(factura, dto.getDetalles());
        factura.setImporteTotal(totalFactura);
        factura.setImporteCobrado(0.0);
        factura.setImporteSaldo(totalFactura);

        em.persist(factura);
        em.flush();

        return mapearAReporteDTO(factura);
    }

    @Transactional
    public FacturaReporteDTO informarArca(Long id) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }
        if (estaInformadaArca(factura)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La factura ya fue informada a ARCA (CAE: " + factura.getCae() + ").");
        }
        if ("ANULADA".equalsIgnoreCase(factura.getEstado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede informar a ARCA una factura ANULADA.");
        }

        try {
            String[] auth = wsaaClient.obtenerTokenYSign();
            String cuitCliente = factura.getCliente() != null ? factura.getCliente().getCuitCuil() : null;
            int ptoVta = factura.getPuntoVenta().getNumero();

            AfipCaeResponse resp = wsfeClient.solicitarCae(
                    auth[0], auth[1], ptoVta, factura.getFechaEmision(), factura.getImporteTotal(), cuitCliente
            );

            factura.setResultadoAfip(resp.getResultado());

            if ("A".equalsIgnoreCase(resp.getResultado()) && resp.getCae() != null && !resp.getCae().isBlank()) {
                factura.setCae(resp.getCae());
                if (resp.getCaeFechaVencimiento() != null && !resp.getCaeFechaVencimiento().isBlank()) {
                    Date vto = new SimpleDateFormat("yyyyMMdd").parse(resp.getCaeFechaVencimiento());
                    factura.setCaeFechaVencimiento(vto);
                }
                factura.setNumero(resp.getNumeroComprobante());
                factura.setMotivoRechazo(null);
                factura.setFechaModificacion(new Date());
                em.merge(factura);
                em.flush();
                return mapearAReporteDTO(factura);
            } else {
                String motivo = resp.getObservaciones() != null ? resp.getObservaciones() : "Comprobante rechazado por ARCA.";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ARCA rechazó la factura: " + motivo);
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al comunicarse con ARCA: " + e.getMessage());
        }
    }

    @Transactional
    public FacturaReporteDTO actualizar(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = em.find(FacturaVenta.class, id);
        if (factura == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada con ID: " + id);
        }

        // Regla de negocio: si ya está informada a ARCA, bloquear modificaciones (solo permitir Anular)
        if (estaInformadaArca(factura)) {
            boolean intentaModificarDatos = dto.getNumero() != null
                    || dto.getFechaEmision() != null
                    || dto.getClienteId() != null
                    || dto.getCondicionIvaId() != null
                    || dto.getPuntoVentaId() != null
                    || dto.getTipoMonedaId() != null
                    || (dto.getDetalles() != null && !dto.getDetalles().isEmpty());

            boolean esAnulacion = "ANULADA".equalsIgnoreCase(dto.getEstado());

            if (intentaModificarDatos || !esAnulacion) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La factura ya fue informada a ARCA (CAE: " + factura.getCae() + "). No se puede modificar, solo Anular."
                );
            }

            factura.setEstado("ANULADA");
            if (factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(new Date());
            }
            if (dto.getObservaciones() != null) {
                factura.setObservaciones(dto.getObservaciones());
            }
            factura.setFechaModificacion(new Date());
            em.merge(factura);
            em.flush();
            return mapearAReporteDTO(factura);
        }

        
        // Si NO está informada a ARCA, permite modificar cabecera e ítems normalmente
        boolean esEdicionCompleta = dto.getDetalles() != null && !dto.getDetalles().isEmpty();

        PuntoVenta nuevoPtoVta = dto.getPuntoVentaId() != null
                ? buscarEntidadObligatoria(PuntoVenta.class, dto.getPuntoVentaId(), "Punto de venta")
                : factura.getPuntoVenta();
        Long nuevoNumero = dto.getNumero() != null ? dto.getNumero() : factura.getNumero();

        if (nuevoNumero != null && nuevoPtoVta != null) {
            Long existentes = em.createQuery(
                            "SELECT COUNT(f) FROM FacturaVenta f WHERE f.numero = :numero AND f.puntoVenta = :puntoVenta AND f.id <> :id",
                            Long.class)
                    .setParameter("numero", nuevoNumero)
                    .setParameter("puntoVenta", nuevoPtoVta)
                    .setParameter("id", id)
                    .getSingleResult();
            if (existentes > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe una factura con el número " + nuevoNumero
                                + " en el punto de venta " + nuevoPtoVta.getNumero() + ".");
            }
        }

        if (dto.getNumero() != null) factura.setNumero(dto.getNumero());
        if (dto.getFechaEmision() != null) factura.setFechaEmision(dto.getFechaEmision());
        if (dto.getClienteId() != null) {
            factura.setCliente(buscarEntidadObligatoria(Cliente.class, dto.getClienteId(), "Cliente"));
        } else if (esEdicionCompleta) {
            factura.setCliente(null); // Volver a Consumidor Final
        }
        if (dto.getCondicionIvaId() != null) factura.setCondicionIva(buscarEntidadObligatoria(CondicionIva.class, dto.getCondicionIvaId(), "Condición de IVA"));
        if (dto.getPuntoVentaId() != null) factura.setPuntoVenta(nuevoPtoVta);
        if (dto.getTipoMonedaId() != null) factura.setTipoMoneda(buscarEntidadObligatoria(TipoMoneda.class, dto.getTipoMonedaId(), "Tipo de moneda"));
        if (dto.getObservaciones() != null) factura.setObservaciones(dto.getObservaciones());

        if (dto.getEstado() != null && !dto.getEstado().isBlank()) {
            factura.setEstado(dto.getEstado());
            if ("ANULADA".equalsIgnoreCase(dto.getEstado()) && factura.getFechaAnulacion() == null) {
                factura.setFechaAnulacion(new Date());
            }
        }

        if (esEdicionCompleta) {
            factura.getDetalles().clear();
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
        // Regla de negocio: si ya está informada a ARCA, no se puede eliminar
        if (estaInformadaArca(factura)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar una factura informada a ARCA (CAE: " + factura.getCae() + "). Solo se puede Anular."
            );
        }
        em.remove(factura);
    }

    // --- Métodos auxiliares ---

    private boolean estaInformadaArca(FacturaVenta f) {
        return f.getCae() != null && !f.getCae().isBlank();
    }

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
                f.getEstado(),
                f.getCae(),
                f.getCaeFechaVencimiento(),
                f.getResultadoAfip()
        );
    }
}