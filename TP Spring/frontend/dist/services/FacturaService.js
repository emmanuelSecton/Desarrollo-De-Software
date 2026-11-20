export class FacturaService {
    constructor() {
        this.apiBase = "http://localhost:8080/api";
        this.baseUrl = `${this.apiBase}/facturas`;
    }
    /** Parámetros de búsqueda de facturas (sin paginación). */
    parametrosFiltros(filtros) {
        const params = new URLSearchParams();
        if (filtros.numeroFactura !== undefined && !Number.isNaN(filtros.numeroFactura)) {
            params.append("numeroFactura", String(filtros.numeroFactura));
        }
        if (filtros.fechaDesde)
            params.append("fechaDesde", filtros.fechaDesde);
        if (filtros.fechaHasta)
            params.append("fechaHasta", filtros.fechaHasta);
        if (filtros.cliente && filtros.cliente.trim() !== "") {
            params.append("cliente", filtros.cliente.trim());
        }
        if (filtros.informadaArca === "true" || filtros.informadaArca === "false") {
            params.append("informadaArca", filtros.informadaArca);
        }
        return params;
    }
    /** URL del reporte PDF con todas las facturas que cumplen los filtros. */
    obtenerUrlReportePdf(filtros = {}) {
        const query = this.parametrosFiltros(filtros).toString();
        return `${this.baseUrl}/reporte/pdf${query ? `?${query}` : ""}`;
    }
    async obtenerFacturas(filtros = {}) {
        const params = this.parametrosFiltros(filtros);
        params.append("page", String(filtros.page ?? 0));
        params.append("size", String(filtros.size ?? 5));
        const response = await fetch(`${this.baseUrl}?${params.toString()}`);
        if (!response.ok) {
            throw new Error(`Error al obtener facturas (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async crearFactura(factura) {
        const response = await fetch(this.baseUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(factura)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Error al crear factura (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async actualizarEstado(id, estado, observaciones) {
        const body = { estado };
        if (observaciones !== undefined)
            body.observaciones = observaciones;
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Error al actualizar factura (HTTP ${response.status}): ${errorText}`);
        }
        return await response.json();
    }
    async eliminarFactura(id) {
        const response = await fetch(`${this.baseUrl}/${id}`, { method: "DELETE" });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Error al eliminar factura (HTTP ${response.status}): ${errorText}`);
        }
    }
    async informarArca(id) {
        const response = await fetch(`${this.baseUrl}/${id}/informar-arca`, { method: "POST" });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Error al informar a ARCA (HTTP ${response.status}): ${errorText}`);
        }
        return await response.json();
    }
    obtenerUrlPdf(id) {
        return `${this.baseUrl}/${id}/pdf`;
    }
    async buscarClientes(busqueda = "") {
        const query = busqueda.trim() ? `?busqueda=${encodeURIComponent(busqueda.trim())}` : "";
        const response = await fetch(`${this.apiBase}/clientes${query}`);
        if (!response.ok)
            throw new Error(`Error al buscar clientes (HTTP ${response.status})`);
        return await response.json();
    }
    async crearCliente(cliente) {
        const response = await fetch(`${this.apiBase}/clientes`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(cliente)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Error al crear el cliente (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async obtenerPuntosVenta() {
        const response = await fetch(`${this.apiBase}/puntos-venta`);
        if (!response.ok)
            throw new Error(`Error al obtener los puntos de venta (HTTP ${response.status})`);
        return await response.json();
    }
    async obtenerCondicionesIva() {
        const response = await fetch(`${this.apiBase}/condiciones-iva`);
        if (!response.ok)
            throw new Error(`Error al obtener las condiciones de IVA (HTTP ${response.status})`);
        return await response.json();
    }
    async obtenerMonedas() {
        const response = await fetch(`${this.apiBase}/monedas`);
        if (!response.ok)
            throw new Error(`Error al obtener las monedas (HTTP ${response.status})`);
        return await response.json();
    }
    async obtenerRubros() {
        return this.obtenerOpciones("rubros", "los rubros");
    }
    async obtenerMarcas() {
        return this.obtenerOpciones("marcas", "las marcas");
    }
    async obtenerListasPrecio() {
        return this.obtenerOpciones("listas-precio", "las listas de precio");
    }
    async obtenerOpciones(ruta, nombre) {
        const response = await fetch(`${this.apiBase}/${ruta}`);
        if (!response.ok)
            throw new Error(`Error al obtener ${nombre} (HTTP ${response.status})`);
        return await response.json();
    }
    async crearArticulo(articulo) {
        const response = await fetch(`${this.apiBase}/articulos`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(articulo)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Error al crear el artículo (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async buscarArticulosPrecio(busqueda = "") {
        const query = busqueda.trim() ? `?busqueda=${encodeURIComponent(busqueda.trim())}` : "";
        const response = await fetch(`${this.apiBase}/articulos-precio${query}`);
        if (!response.ok)
            throw new Error(`Error al buscar artículos (HTTP ${response.status})`);
        return await response.json();
    }
    async obtenerFacturaPorId(id) {
        const response = await fetch(`${this.baseUrl}/${id}`);
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Error al obtener la factura (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async actualizarFactura(id, factura) {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(factura)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Error al actualizar factura (HTTP ${response.status})`);
        }
        return await response.json();
    }
}
