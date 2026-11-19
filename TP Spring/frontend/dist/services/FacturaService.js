export class FacturaService {
    constructor() {
        this.baseUrl = "http://localhost:8080/api/facturas";
    }
    async obtenerFacturas(filtros = {}) {
        const params = new URLSearchParams();
        if (filtros.fechaDesde) {
            params.append("fechaDesde", filtros.fechaDesde);
        }
        if (filtros.fechaHasta) {
            params.append("fechaHasta", filtros.fechaHasta);
        }
        if (filtros.estado && filtros.estado.trim() !== "") {
            params.append("estado", filtros.estado.trim());
        }
        if (filtros.montoMinimo !== undefined && !Number.isNaN(filtros.montoMinimo)) {
            params.append("montoMinimo", String(filtros.montoMinimo));
        }
        const queryString = params.toString();
        const url = queryString ? `${this.baseUrl}?${queryString}` : this.baseUrl;
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(`Error al obtener facturas (HTTP ${response.status})`);
        }
        return await response.json();
    }
    async crearFactura(factura) {
        const response = await fetch(this.baseUrl, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
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
        if (observaciones !== undefined) {
            body.observaciones = observaciones;
        }
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(body)
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Error al actualizar factura (HTTP ${response.status}): ${errorText}`);
        }
        return await response.json();
    }
    async eliminarFactura(id) {
        const response = await fetch(`${this.baseUrl}/${id}`, {
            method: "DELETE"
        });
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Error al eliminar factura (HTTP ${response.status}): ${errorText}`);
        }
    }
}
