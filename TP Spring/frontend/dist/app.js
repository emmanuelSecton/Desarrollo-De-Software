import { FacturaService } from "./services/FacturaService.js";
const facturaService = new FacturaService();
let detallesEnMemoria = [];
let paginaActual = 0;
let totalPaginas = 1;
const TAMANIO_PAGINA = 5;
// Referencias DOM principales
const divMensaje = document.getElementById("mensaje");
const formFiltros = document.getElementById("formFiltros");
const btnLimpiarFiltros = document.getElementById("btnLimpiarFiltros");
const formFactura = document.getElementById("formFactura");
const btnAgregarItem = document.getElementById("btnAgregarItem");
const tablaDetallesBody = document.getElementById("tablaDetallesBody");
const tablaFacturasBody = document.getElementById("tablaFacturasBody");
// Paginación
const btnPaginaAnterior = document.getElementById("btnPaginaAnterior");
const btnPaginaSiguiente = document.getElementById("btnPaginaSiguiente");
const infoPaginacion = document.getElementById("infoPaginacion");
// Modales Explorer
const modalCliente = document.getElementById("modalCliente");
const inputBuscarCliente = document.getElementById("inputBuscarCliente");
const tablaModalClientes = document.getElementById("tablaModalClientes");
const modalArticulo = document.getElementById("modalArticulo");
const inputBuscarArticulo = document.getElementById("inputBuscarArticulo");
const tablaModalArticulos = document.getElementById("tablaModalArticulos");
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
            <td><button type="button" class="danger">Quitar</button></td>
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
        tablaFacturasBody.innerHTML = `<tr><td colspan="9">No se encontraron facturas.</td></tr>`;
        return;
    }
    facturas.forEach((f) => {
        const tr = document.createElement("tr");
        const yaAnulada = f.estado === "ANULADA";
        const informadaArca = Boolean(f.cae && f.cae.trim() !== "");
        const columnaArca = informadaArca
            ? `<span class="badge-ok">CAE: ${f.cae}</span><br/><small>Vto: ${f.caeFechaVencimiento ?? "-"}</small>`
            : `<span class="badge-pend">Pendiente</span>`;
        tr.innerHTML = `
            <td>${f.id}</td>
            <td>${f.numeroFactura ?? "-"}</td>
            <td>${f.fechaEmision}</td>
            <td>${f.clienteDenominacion}</td>
            <td>${f.puntoVentaDescripcion}</td>
            <td>$${f.importeTotal.toFixed(2)}</td>
            <td><strong>${f.estado}</strong></td>
            <td>${columnaArca}</td>
            <td class="acciones">
                <button type="button" class="ok-btn btn-arca" ${informadaArca || yaAnulada ? "disabled" : ""}>Informar ARCA</button>
                <button type="button" class="sec btn-pdf">PDF</button>
                <button type="button" class="warn btn-anular" ${yaAnulada ? "disabled" : ""}>Anular</button>
                <button type="button" class="danger btn-eliminar" ${informadaArca ? "disabled title='Bloqueada: ya informada a ARCA'" : ""}>Eliminar</button>
            </td>
        `;
        tr.querySelector(".btn-arca")?.addEventListener("click", async () => {
            if (!confirm(`¿Informar la factura ID ${f.id} a ARCA para obtener CAE?`))
                return;
            try {
                mostrarMensaje(`Informando factura ID ${f.id} a ARCA...`);
                const resp = await facturaService.informarArca(f.id);
                mostrarMensaje(`¡Autorizada por ARCA! CAE: ${resp.cae} (Vto: ${resp.caeFechaVencimiento})`);
                await cargarFacturas();
            }
            catch (error) {
                mostrarMensaje(error.message, true);
            }
        });
        tr.querySelector(".btn-pdf")?.addEventListener("click", () => {
            window.open(facturaService.obtenerUrlPdf(f.id), "_blank");
        });
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
            if (informadaArca)
                return;
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
    const nroStr = document.getElementById("filtroNumero").value;
    const fechaDesde = document.getElementById("filtroFechaDesde").value;
    const fechaHasta = document.getElementById("filtroFechaHasta").value;
    const cliente = document.getElementById("filtroCliente").value;
    const informadaArca = document.getElementById("filtroInformadaArca").value;
    const filtros = { page: paginaActual, size: TAMANIO_PAGINA };
    if (nroStr)
        filtros.numeroFactura = Number(nroStr);
    if (fechaDesde)
        filtros.fechaDesde = fechaDesde;
    if (fechaHasta)
        filtros.fechaHasta = fechaHasta;
    if (cliente)
        filtros.cliente = cliente;
    if (informadaArca)
        filtros.informadaArca = informadaArca;
    try {
        const pageResp = await facturaService.obtenerFacturas(filtros);
        renderizarFacturas(pageResp.content);
        totalPaginas = Math.max(pageResp.totalPages, 1);
        infoPaginacion.textContent = `Página ${pageResp.page + 1} de ${totalPaginas} (${pageResp.totalElements} facturas)`;
        btnPaginaAnterior.disabled = pageResp.page <= 0;
        btnPaginaSiguiente.disabled = pageResp.page + 1 >= totalPaginas;
    }
    catch (error) {
        mostrarMensaje(error.message, true);
    }
}
// --- Lógica Modal Explorer: Clientes ---
async function cargarModalClientes(busqueda = "") {
    try {
        const clientes = await facturaService.buscarClientes(busqueda);
        tablaModalClientes.innerHTML = "";
        if (clientes.length === 0) {
            tablaModalClientes.innerHTML = `<tr><td colspan="4">No se encontraron clientes.</td></tr>`;
            return;
        }
        clientes.forEach((c) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${c.id}</td>
                <td>${c.denominacion}</td>
                <td>${c.cuitCuil ?? "-"}</td>
                <td><button type="button" class="ok-btn">Seleccionar</button></td>
            `;
            tr.querySelector("button")?.addEventListener("click", () => {
                document.getElementById("clienteId").value = String(c.id);
                document.getElementById("clienteNombre").value = `${c.denominacion} (ID ${c.id})`;
                modalCliente.classList.remove("abierto");
            });
            tablaModalClientes.appendChild(tr);
        });
    }
    catch (error) {
        mostrarMensaje(error.message, true);
    }
}
// --- Lógica Modal Explorer: ListaPrecioArticulo ---
async function cargarModalArticulos(busqueda = "") {
    try {
        const articulos = await facturaService.buscarArticulosPrecio(busqueda);
        tablaModalArticulos.innerHTML = "";
        if (articulos.length === 0) {
            tablaModalArticulos.innerHTML = `<tr><td colspan="4">No se encontraron artículos.</td></tr>`;
            return;
        }
        articulos.forEach((a) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${a.articuloId}</td>
                <td>${a.articuloDenominacion}</td>
                <td>$${a.precioVenta.toFixed(2)}</td>
                <td><button type="button" class="ok-btn">Seleccionar</button></td>
            `;
            tr.querySelector("button")?.addEventListener("click", () => {
                document.getElementById("itemArticuloId").value = String(a.articuloId);
                document.getElementById("itemDescripcion").value = a.articuloDenominacion;
                document.getElementById("itemPrecio").value = String(a.precioVenta);
                modalArticulo.classList.remove("abierto");
            });
            tablaModalArticulos.appendChild(tr);
        });
    }
    catch (error) {
        mostrarMensaje(error.message, true);
    }
}
// Eventos Modales
document.getElementById("btnAbrirModalCliente")?.addEventListener("click", () => {
    modalCliente.classList.add("abierto");
    cargarModalClientes(inputBuscarCliente.value);
});
document.getElementById("btnCerrarModalCliente")?.addEventListener("click", () => modalCliente.classList.remove("abierto"));
document.getElementById("btnEjecutarBuscarCliente")?.addEventListener("click", () => cargarModalClientes(inputBuscarCliente.value));
document.getElementById("btnLimpiarCliente")?.addEventListener("click", () => {
    document.getElementById("clienteId").value = "";
    document.getElementById("clienteNombre").value = "Consumidor Final";
});
document.getElementById("btnAbrirModalArticulo")?.addEventListener("click", () => {
    modalArticulo.classList.add("abierto");
    cargarModalArticulos(inputBuscarArticulo.value);
});
document.getElementById("btnCerrarModalArticulo")?.addEventListener("click", () => modalArticulo.classList.remove("abierto"));
document.getElementById("btnEjecutarBuscarArticulo")?.addEventListener("click", () => cargarModalArticulos(inputBuscarArticulo.value));
// Eventos Formulario e Ítems
btnAgregarItem.addEventListener("click", () => {
    const articuloId = Number(document.getElementById("itemArticuloId").value);
    const descripcion = document.getElementById("itemDescripcion").value.trim();
    const cantidad = Number(document.getElementById("itemCantidad").value);
    const precioStr = document.getElementById("itemPrecio").value;
    const bonificacion = Number(document.getElementById("itemBonificacion").value) || 0;
    if (!articuloId || cantidad <= 0) {
        mostrarMensaje("Seleccioná un artículo desde el botón Buscar y poné una cantidad mayor a 0.", true);
        return;
    }
    const nuevoItem = { articuloId, cantidad, porcentajeBonificacion: bonificacion };
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
        document.getElementById("clienteId").value = "";
        document.getElementById("clienteNombre").value = "Consumidor Final";
        detallesEnMemoria = [];
        renderizarDetalles();
        paginaActual = 0;
        await cargarFacturas();
    }
    catch (error) {
        mostrarMensaje(error.message, true);
    }
});
// Eventos Filtros y Paginación
formFiltros.addEventListener("submit", async (e) => {
    e.preventDefault();
    paginaActual = 0;
    await cargarFacturas();
});
btnLimpiarFiltros.addEventListener("click", async () => {
    formFiltros.reset();
    paginaActual = 0;
    await cargarFacturas();
});
btnPaginaAnterior.addEventListener("click", async () => {
    if (paginaActual > 0) {
        paginaActual--;
        await cargarFacturas();
    }
});
btnPaginaSiguiente.addEventListener("click", async () => {
    if (paginaActual + 1 < totalPaginas) {
        paginaActual++;
        await cargarFacturas();
    }
});
// Carga inicial
cargarFacturas();
