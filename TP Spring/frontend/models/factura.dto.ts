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
    cae?: string | null;
    caeFechaVencimiento?: string | null;
    resultadoAfip?: string | null;
}

export interface PageResponseDTO<T> {
    content: T[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
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
    numeroFactura?: number;
    fechaDesde?: string;
    fechaHasta?: string;
    cliente?: string;
    informadaArca?: string; // "" | "true" | "false"
    page?: number;
    size?: number;
}

export interface ClienteDTO {
    id: number;
    denominacion: string;
    cuitCuil?: string | null;
}

export interface ListaPrecioArticuloDTO {
    id: number;
    articuloId: number;
    articuloDenominacion: string;
    precioVenta: number;
}
export interface ClienteRequestDTO {
    cuitCuil: string;
    denominacion: string;
    email?: string;
    telefono?: string;
    celular?: string;
    nombreCalle: string;
    numeroCalle: string;
}

export interface TipoMonedaDTO {
    id: number;
    simbolo: string;
    denominacion: string;
    codigoAfip: string;
}

export interface CondicionIvaDTO {
    id: number;
    denominacion: string;
    codigoAfip: number;
}

export interface OpcionDTO {
    id: number;
    denominacion: string;
}

export interface ArticuloRequestDTO {
    codigo: string;
    denominacion: string;
    rubroId?: number;
    marcaId?: number;
    listaPrecioId: number;
    precioVenta: number;
}

export interface PuntoVentaDTO {
    id: number;
    numero: number;
    descripcion: string;
}

export interface FacturaEdicionDTO {
    id: number;
    numero: number;
    fechaEmision: string;
    clienteId?: number | null;
    clienteDenominacion: string;
    clienteCuitCuil?: string | null;
    condicionIvaId: number;
    puntoVentaId: number;
    tipoMonedaId: number;
    estado: string;
    observaciones?: string | null;
    detalles: FacturaDetalleRequestDTO[];
}