export interface FacturaReporteDTO {
    id: number;
    numeroFactura: number;
    fechaEmision: string;
    clienteDenominacion: string;
    condicionIva: string;
    puntoVentaDescripcion: string;
    importeTotal: number;
    cantidadItems: number;
    estado: string;
}

export interface FacturaDetalleRequestDTO {
    articuloId: number;
    descripcion?: string;
    cantidad: number;
    precioUnitario?: number;
    porcentajeBonificacion: number;
}

export interface FacturaRequestDTO {
    numero?: number;
    fechaEmision?: string;
    clienteId?: number | null;
    condicionIvaId?: number;
    puntoVentaId?: number;
    tipoMonedaId?: number;
    estado?: string;
    observaciones?: string;
    detalles?: FacturaDetalleRequestDTO[];
}

export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: string;
    montoMinimo?: number;
}