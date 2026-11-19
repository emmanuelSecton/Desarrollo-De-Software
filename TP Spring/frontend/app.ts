import {
    FacturaDetalleRequestDTO,
    FacturaFiltrosDTO,
    FacturaReporteDTO,
    FacturaRequestDTO
} from "./models/factura.dto.js";
import { FacturaService } from "./services/FacturaService.js";

const facturaService = new FacturaService();
let detallesEnMemoria: FacturaDetalleRequestDTO[] = [];

// Referencias al DOM
const divMensaje = document.getElementById("mensaje") as HTMLDivElement;
const formFiltros = document.getElementById("formFiltros") as HTMLFormElement;
const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros") as HTMLButtonElement;
const formFactura = document.getElementById("formFactura") as HTMLFormElement;
const btnAgregarItem = document.getElementById("btnAgregarItem") as HTMLButtonElement;
const tablaDetallesBody = document.getElementById("tablaDetallesBody") as HTMLTableSectionElement;
const tablaFacturasBody = document.getElementById("tablaFacturasBody") as HTMLTableSectionElement;

function mostrarMensaje(texto: string, esError = false): void {
    divMensaje.textContent = texto;
    divMensaje.className = esError ? "err" : "ok";
}

function renderizarDetalles(): void {
    tablaDetallesBody.innerHTML = "";
    detallesEnMemoria.forEach((item, index) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${item.articuloId}</td>
            <td>${item.descripcion || "(Por defecto)"}</td>
            <td>${item.cantidad}</td>
            <td>${item.precioUnitario !== undefined ? `$${item.precioUnitario}` : "(De lista)"}</td>
            <td>${item.porcentajeBonificacion}%</td>
            <td><button type="button" class="danger" data-index="${index}">Quitar</button></td>
        `;
        tr.querySelector("button")?.addEventListener("click", () => {
            detallesEnMemoria.splice(index, 1);
            renderizarDetalles();
        });
        tablaDetallesBody.appendChild(tr);
    });
}

function renderizarFacturas(facturas: FacturaReporteDTO[]): void {
    tablaFacturasBody.innerHTML = "";

    if (facturas.length === 0) {
        tablaFacturasBody.innerHTML = `<tr><td colspan="10">No se encontraron facturas.</td></tr>`;
        return;
    }

    facturas.forEach((f) => {
        const tr = document.createElement("tr");
        const yaAnulada = f.estado === "ANULADA";

        tr.innerHTML = `
            <td>${f.id}</td>
            <td>${f.numeroFactura ?? "-"}</td>
            <td>${f.fechaEmision}</td>
            <td>${f.clienteDenominacion}</td>
            <td>${f.condicionIva}</td>
            <td>${f.puntoVentaDescripcion}</td>
            <td>${f.cantidadItems}</td>
            <td>$${f.importeTotal.toFixed(2)}</td>
            <td><strong>${f.estado}</strong></td>
            <td class="acciones">
                <button type="button" class="warn btn-anular" ${yaAnulada ? "disabled" : ""}>Anular</button>
                <button type="button" class="danger btn-eliminar">Eliminar</button>
            </td>
        `;

        tr.querySelector(".btn-anular")?.addEventListener("click", async () => {
            if (!confirm(`¿Seguro que deseás anular la factura #${f.numeroFactura}?`)) return;
            try {
                await facturaService.actualizarEstado(f.id, "ANULADA", "Anulada desde la web");
                mostrarMensaje(`Factura ID ${f.id} anulada correctamente.`);
                await cargarFacturas();
            } catch (error) {
                mostrarMensaje((error as Error).message, true);
            }
        });

        tr.querySelector(".btn-eliminar")?.addEventListener("click", async () => {
            if (!confirm(`¿Seguro que deseás eliminar físicamente la factura ID ${f.id}?`)) return;
            try {
                await facturaService.eliminarFactura(f.id);
                mostrarMensaje(`Factura ID ${f.id} eliminada correctamente.`);
                await cargarFacturas();
            } catch (error) {
                mostrarMensaje((error as Error).message, true);
            }
        });

        tablaFacturasBody.appendChild(tr);
    });
}

async function cargarFacturas(): Promise<void> {
    const fechaDesde = (document.getElementById("filtroFechaDesde") as HTMLInputElement).value;
    const fechaHasta = (document.getElementById("filtroFechaHasta") as HTMLInputElement).value;
    const estado = (document.getElementById("filtroEstado") as HTMLSelectElement).value;
    const montoStr = (document.getElementById("filtroMontoMinimo") as HTMLInputElement).value;

    const filtros: FacturaFiltrosDTO = {};
    if (fechaDesde) filtros.fechaDesde = fechaDesde;
    if (fechaHasta) filtros.fechaHasta = fechaHasta;
    if (estado) filtros.estado = estado;
    if (montoStr) filtros.montoMinimo = Number(montoStr);

    try {
        const facturas = await facturaService.obtenerFacturas(filtros);
        renderizarFacturas(facturas);
    } catch (error) {
        mostrarMensaje((error as Error).message, true);
    }
}

// Eventos
btnAgregarItem.addEventListener("click", () => {
    const articuloId = Number((document.getElementById("itemArticuloId") as HTMLInputElement).value);
    const descripcion = (document.getElementById("itemDescripcion") as HTMLInputElement).value.trim();
    const cantidad = Number((document.getElementById("itemCantidad") as HTMLInputElement).value);
    const precioStr = (document.getElementById("itemPrecio") as HTMLInputElement).value;
    const bonificacion = Number((document.getElementById("itemBonificacion") as HTMLInputElement).value) || 0;

    if (!articuloId || cantidad <= 0) {
        mostrarMensaje("Indicá un Artículo ID válido y una cantidad mayor a 0.", true);
        return;
    }

    const nuevoItem: FacturaDetalleRequestDTO = {
        articuloId,
        cantidad,
        porcentajeBonificacion: bonificacion
    };
    if (descripcion) nuevoItem.descripcion = descripcion;
    if (precioStr) nuevoItem.precioUnitario = Number(precioStr);

    detallesEnMemoria.push(nuevoItem);
    renderizarDetalles();
    mostrarMensaje("Ítem agregado a la factura.");
});

formFactura.addEventListener("submit", async (e) => {
    e.preventDefault();

    if (detallesEnMemoria.length === 0) {
        mostrarMensaje("Agregá al menos un ítem antes de guardar la factura.", true);
        return;
    }

    const clienteIdStr = (document.getElementById("clienteId") as HTMLInputElement).value;

    const nuevaFactura: FacturaRequestDTO = {
        numero: Number((document.getElementById("numero") as HTMLInputElement).value),
        fechaEmision: (document.getElementById("fechaEmision") as HTMLInputElement).value,
        clienteId: clienteIdStr ? Number(clienteIdStr) : null,
        condicionIvaId: Number((document.getElementById("condicionIvaId") as HTMLInputElement).value),
        puntoVentaId: Number((document.getElementById("puntoVentaId") as HTMLInputElement).value),
        tipoMonedaId: Number((document.getElementById("tipoMonedaId") as HTMLInputElement).value),
        estado: "EMITIDA",
        observaciones: (document.getElementById("observaciones") as HTMLInputElement).value.trim(),
        detalles: detallesEnMemoria
    };

    try {
        const creada = await facturaService.crearFactura(nuevaFactura);
        mostrarMensaje(`Factura #${creada.numeroFactura} creada (ID ${creada.id}) - Total: $${creada.importeTotal.toFixed(2)}`);
        formFactura.reset();
        detallesEnMemoria = [];
        renderizarDetalles();
        await cargarFacturas();
    } catch (error) {
        mostrarMensaje((error as Error).message, true);
    }
});

formFiltros.addEventListener("submit", async (e) => {
    e.preventDefault();
    await cargarFacturas();
});

btnLimpiarFiltros.addEventListener("click", async () => {
    formFiltros.reset();
    await cargarFacturas();
});

// Carga inicial al abrir la página
cargarFacturas();