export class FacturaService {
    constructor() {
        this.apiBase = "http://localhost:8080/api";
        this.baseUrl = `${this.apiBase}/facturas`;
    }
    async obtenerFacturas(filtros = {}) {
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
            throw new Error(`Error al crear factura (HTTP ${response.status}): ${errorText}`);
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
    async buscarArticulosPrecio(busqueda = "") {
        const query = busqueda.trim() ? `?busqueda=${encodeURIComponent(busqueda.trim())}` : "";
        const response = await fetch(`${this.apiBase}/articulos-precio${query}`);
        if (!response.ok)
            throw new Error(`Error al buscar artículos (HTTP ${response.status})`);
        return await response.json();
    }
}
