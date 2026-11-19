import { FacturaService } from "./services/FacturaService.js";
const facturaService = new FacturaService();
let detallesEnMemoria = [];
// Referencias al DOM
const divMensaje = document.getElementById("mensaje");
const formFiltros = document.getElementById("formFiltros");
const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");
const formFactura = document.getElementById("formFactura");
const btnAgregarItem = document.getElementById("btnAgregarItem");
const tablaDetallesBody = document.getElementById("tablaDetallesBody");
const tablaFacturasBody = document.getElementById("tablaFacturasBody");
function mostrarMensaje(texto, esError = false) {
    divMensaje.textContent = texto;
    divMensaje.className = esError ? "err" : "ok";
}
function renderizarDetalles() {
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
function renderizarFacturas(facturas) {
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
            if (!confirm(`¿Seguro que deseás anular la factura #${f.numeroFactura}?`))
                return;
            try {
                await facturaService.actualizarEstado(f.id, "ANULADA", "Anulada desde la web");
                mostrarMensaje(`Factura ID ${f.id} anulada correctamente.`);
                await cargarFacturas();
            }
            catch (error) {
                mostrarMensaje(error.message, true);
            }
        });
        tr.querySelector(".btn-eliminar")?.addEventListener("click", async () => {
            if (!confirm(`¿Seguro que deseás eliminar físicamente la factura ID ${f.id}?`))
                return;
            try {
                await facturaService.eliminarFactura(f.id);
                mostrarMensaje(`Factura ID ${f.id} eliminada correctamente.`);
                await cargarFacturas();
            }
            catch (error) {
                mostrarMensaje(error.message, true);
            }
        });
        tablaFacturasBody.appendChild(tr);
    });
}
async function cargarFacturas() {
    const fechaDesde = document.getElementById("filtroFechaDesde").value;
    const fechaHasta = document.getElementById("filtroFechaHasta").value;
    const estado = document.getElementById("filtroEstado").value;
    const montoStr = document.getElementById("filtroMontoMinimo").value;
    const filtros = {};
    if (fechaDesde)
        filtros.fechaDesde = fechaDesde;
    if (fechaHasta)
        filtros.fechaHasta = fechaHasta;
    if (estado)
        filtros.estado = estado;
    if (montoStr)
        filtros.montoMinimo = Number(montoStr);
    try {
        const facturas = await facturaService.obtenerFacturas(filtros);
        renderizarFacturas(facturas);
    }
    catch (error) {
        mostrarMensaje(error.message, true);
    }
}
// Eventos
btnAgregarItem.addEventListener("click", () => {
    const articuloId = Number(document.getElementById("itemArticuloId").value);
    const descripcion = document.getElementById("itemDescripcion").value.trim();
    const cantidad = Number(document.getElementById("itemCantidad").value);
    const precioStr = document.getElementById("itemPrecio").value;
    const bonificacion = Number(document.getElementById("itemBonificacion").value) || 0;
    if (!articuloId || cantidad <= 0) {
        mostrarMensaje("Indicá un Artículo ID válido y una cantidad mayor a 0.", true);
        return;
    }
    const nuevoItem = {
        articuloId,
        cantidad,
        porcentajeBonificacion: bonificacion
    };
    if (descripcion)
        nuevoItem.descripcion = descripcion;
    if (precioStr)
        nuevoItem.precioUnitario = Number(precioStr);
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
    const clienteIdStr = document.getElementById("clienteId").value;
    const nuevaFactura = {
        numero: Number(document.getElementById("numero").value),
        fechaEmision: document.getElementById("fechaEmision").value,
        clienteId: clienteIdStr ? Number(clienteIdStr) : null,
        condicionIvaId: Number(document.getElementById("condicionIvaId").value),
        puntoVentaId: Number(document.getElementById("puntoVentaId").value),
        tipoMonedaId: Number(document.getElementById("tipoMonedaId").value),
        estado: "EMITIDA",
        observaciones: document.getElementById("observaciones").value.trim(),
        detalles: detallesEnMemoria
    };
    try {
        const creada = await facturaService.crearFactura(nuevaFactura);
        mostrarMensaje(`Factura #${creada.numeroFactura} creada (ID ${creada.id}) - Total: $${creada.importeTotal.toFixed(2)}`);
        formFactura.reset();
        detallesEnMemoria = [];
        renderizarDetalles();
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(error.message, true);
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
