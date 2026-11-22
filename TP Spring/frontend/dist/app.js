import { FacturaService } from "./services/FacturaService.js";
const facturaService = new FacturaService();
let detallesEnMemoria = [];
let facturaEditandoId = null;
let paginaActual = 0;
let totalPaginas = 1;
const TAMANIO_PAGINA = 5;
// ---------- Referencias DOM ----------
const $ = (id) => document.getElementById(id);
const divMensaje = $("mensaje");
const formFiltros = $("formFiltros");
const btnLimpiarFiltros = $("btnLimpiarFiltros");
const formFactura = $("formFactura");
const btnAgregarItem = $("btnAgregarItem");
const tablaDetallesBody = $("tablaDetallesBody");
const tablaFacturasBody = $("tablaFacturasBody");
const totalEstimado = $("totalEstimado");
const infoTotal = $("infoTotal");
const btnPaginaAnterior = $("btnPaginaAnterior");
const btnPaginaSiguiente = $("btnPaginaSiguiente");
const infoPaginacion = $("infoPaginacion");
const modalFactura = $("modalFactura");
const modalCliente = $("modalCliente");
const inputBuscarCliente = $("inputBuscarCliente");
const tablaModalClientes = $("tablaModalClientes");
const modalArticulo = $("modalArticulo");
const inputBuscarArticulo = $("inputBuscarArticulo");
const tablaModalArticulos = $("tablaModalArticulos");
const modalConfirmar = $("modalConfirmar");
// ---------- Utilidades ----------
const formatoMoneda = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const moneda = (n) => formatoMoneda.format(n);
function fecha(iso) {
    if (!iso)
        return "-";
    const [a, m, d] = iso.substring(0, 10).split("-");
    return d && m && a ? `${d}/${m}/${a}` : iso;
}
function esc(valor) {
    return String(valor ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);
}
let timerMensaje;
/** Muestra un aviso. Con `fijo = true` queda visible hasta que otro mensaje lo reemplace. */
function mostrarMensaje(texto, tipo = "ok", fijo = false) {
    divMensaje.textContent = texto;
    divMensaje.className = tipo;
    window.clearTimeout(timerMensaje);
    if (!fijo) {
        timerMensaje = window.setTimeout(() => (divMensaje.className = ""), tipo === "err" ? 7000 : 4000);
    }
}
const mostrarError = (error) => mostrarMensaje(error.message, "err");
// ---------- Modales ----------
function abrirModal(modal, foco) {
    modal.classList.add("abierto");
    (foco ?? modal.querySelector("input:not([type=hidden]):not([readonly]), button"))?.focus();
}
function cerrarModal(modal) {
    modal.classList.remove("abierto");
}
/** Modal de confirmación propio (reemplaza window.confirm). */
function confirmar(texto, etiquetaOk = "Confirmar", peligro = false) {
    const btnSi = $("btnConfirmarSi");
    const btnNo = $("btnConfirmarNo");
    $("textoConfirmar").textContent = texto;
    btnSi.textContent = etiquetaOk;
    btnSi.className = peligro ? "danger-solid" : "";
    abrirModal(modalConfirmar, btnNo);
    return new Promise((resolve) => {
        const terminar = (resultado) => {
            cerrarModal(modalConfirmar);
            btnSi.removeEventListener("click", si);
            btnNo.removeEventListener("click", no);
            modalConfirmar.removeEventListener("cancelar", no);
            resolve(resultado);
        };
        const si = () => terminar(true);
        const no = () => terminar(false);
        btnSi.addEventListener("click", si);
        btnNo.addEventListener("click", no);
        modalConfirmar.addEventListener("cancelar", no);
    });
}
// Cerrar con botones [data-cerrar], clic en el fondo o Escape (cierra el modal de más arriba)
document.querySelectorAll("[data-cerrar]").forEach((btn) => btn.addEventListener("click", () => cerrarModal($(btn.dataset.cerrar))));
document.querySelectorAll(".modal-overlay").forEach((overlay) => overlay.addEventListener("mousedown", (e) => {
    if (e.target !== overlay)
        return;
    if (overlay === modalConfirmar)
        overlay.dispatchEvent(new Event("cancelar"));
    else
        cerrarModal(overlay);
}));
document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape")
        return;
    const abiertos = [...document.querySelectorAll(".modal-overlay.abierto")];
    const superior = abiertos.sort((a, b) => Number(getComputedStyle(b).zIndex) - Number(getComputedStyle(a).zIndex))[0];
    if (!superior)
        return;
    if (superior === modalConfirmar)
        superior.dispatchEvent(new Event("cancelar"));
    else
        cerrarModal(superior);
});
// ---------- Ítems de la factura ----------
function subtotalItem(item) {
    if (item.precioUnitario === undefined)
        return null;
    return item.cantidad * item.precioUnitario * (1 - item.porcentajeBonificacion / 100);
}
function renderizarDetalles() {
    tablaDetallesBody.innerHTML = "";
    if (detallesEnMemoria.length === 0) {
        tablaDetallesBody.innerHTML = `<tr><td colspan="6" class="vacio">Todavía no agregaste ítems.</td></tr>`;
        totalEstimado.textContent = moneda(0);
        return;
    }
    let total = 0;
    detallesEnMemoria.forEach((item, index) => {
        const subtotal = subtotalItem(item);
        if (subtotal !== null)
            total += subtotal;
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${esc(item.descripcion || "(Por defecto)")}<span class="sub">Art. ID ${item.articuloId}</span></td>
            <td class="num">${item.cantidad}</td>
            <td class="num">${item.precioUnitario !== undefined ? moneda(item.precioUnitario) : "De lista"}</td>
            <td class="num">${item.porcentajeBonificacion}%</td>
            <td class="num">${subtotal !== null ? moneda(subtotal) : "-"}</td>
            <td><button type="button" class="danger sm">Quitar</button></td>
        `;
        tr.querySelector("button")?.addEventListener("click", () => {
            detallesEnMemoria.splice(index, 1);
            renderizarDetalles();
        });
        tablaDetallesBody.appendChild(tr);
    });
    totalEstimado.textContent = moneda(total);
}
function limpiarItemActual() {
    $("itemArticuloId").value = "";
    $("itemListaPrecioArticuloId").value = "";
    $("itemDescripcion").value = "";
    btnAgregarItem.disabled = true; // se habilita al elegir un artículo
    $("itemCantidad").value = "1";
    $("itemPrecio").value = "";
    $("itemBonificacion").value = "0";
}
function resetearFormularioFactura() {
    formFactura.reset();
    $("clienteId").value = "";
    $("clienteNombre").value = "Consumidor Final";
    $("fechaEmision").value = new Date().toISOString().substring(0, 10);
    limpiarItemActual();
    detallesEnMemoria = [];
    renderizarDetalles();
    aplicarReglaClienteCondicion();
}
// ---------- Regla: sin Consumidor Final si la condición de IVA no es Consumidor Final ----------
const CODIGO_AFIP_CONSUMIDOR_FINAL = "5";
function condicionEsConsumidorFinal() {
    const opcion = selectCondicionIva.selectedOptions[0];
    // Mientras no cargan las condiciones no se bloquea nada
    return !opcion?.dataset.codigoAfip || opcion.dataset.codigoAfip === CODIGO_AFIP_CONSUMIDOR_FINAL;
}
/** Ajusta el campo Cliente según la condición de IVA elegida. */
function aplicarReglaClienteCondicion() {
    const esCF = condicionEsConsumidorFinal();
    const inputNombre = $("clienteNombre");
    const tieneCliente = $("clienteId").value !== "";
    const btnQuitar = $("btnLimpiarCliente");
    // "Quitar" vuelve a Consumidor Final: solo se permite si la condición es Consumidor Final
    btnQuitar.disabled = !esCF;
    btnQuitar.title = esCF ? "Volver a Consumidor Final" : "Con esta condición de IVA hay que elegir un cliente";
    if (!tieneCliente) {
        inputNombre.value = esCF ? "Consumidor Final" : "";
        inputNombre.placeholder = esCF ? "" : "Elegí un cliente con Buscar…";
    }
    if (esCF || tieneCliente)
        inputNombre.classList.remove("invalido");
}
// ---------- Puntos de venta (solo los existentes en la base) ----------
const selectPuntoVenta = $("puntoVentaId");
/** Nueva factura: muestra el número que se le asignará (el backend lo confirma al guardar). */
async function mostrarProximoNumero() {
    const inputNumero = $("numero");
    inputNumero.value = "";
    if (facturaEditandoId !== null || !selectPuntoVenta.value)
        return;
    try {
        inputNumero.value = String(await facturaService.obtenerProximoNumero(Number(selectPuntoVenta.value)));
    }
    catch (error) {
        mostrarError(error);
    }
}
selectPuntoVenta.addEventListener("change", () => {
    if (facturaEditandoId === null)
        mostrarProximoNumero();
});
async function cargarPuntosVenta() {
    try {
        const puntos = await facturaService.obtenerPuntosVenta();
        selectPuntoVenta.innerHTML = "";
        if (puntos.length === 0) {
            selectPuntoVenta.innerHTML = `<option value="" disabled selected>No hay puntos de venta</option>`;
            return;
        }
        // Por defecto el primero (menor número); defaultSelected hace que formFactura.reset() vuelva a él
        puntos.forEach((p, i) => {
            const opcion = new Option(p.descripcion, String(p.id), i === 0, i === 0);
            opcion.title = `Punto de venta Nº ${p.numero}`;
            selectPuntoVenta.add(opcion);
        });
    }
    catch (error) {
        selectPuntoVenta.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}
// ---------- Condiciones de IVA (solo las existentes en la base) ----------
const selectCondicionIva = $("condicionIvaId");
async function cargarCondicionesIva() {
    try {
        const condiciones = await facturaService.obtenerCondicionesIva();
        selectCondicionIva.innerHTML = "";
        if (condiciones.length === 0) {
            selectCondicionIva.innerHTML = `<option value="" disabled selected>No hay condiciones cargadas</option>`;
            return;
        }
        // Por defecto: Consumidor Final (código AFIP 5) o, si no existe, la primera
        const porDefecto = condiciones.find((c) => c.codigoAfip === 5) ?? condiciones[0];
        condiciones.forEach((c) => {
            const esDefecto = c.id === porDefecto.id;
            // defaultSelected hace que formFactura.reset() vuelva a esta opción
            const opcion = new Option(c.denominacion, String(c.id), esDefecto, esDefecto);
            opcion.dataset.codigoAfip = String(c.codigoAfip);
            selectCondicionIva.add(opcion);
        });
        aplicarReglaClienteCondicion();
    }
    catch (error) {
        selectCondicionIva.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}
// ---------- Monedas (solo las existentes en la base) ----------
const selectMoneda = $("tipoMonedaId");
async function cargarMonedas() {
    try {
        const monedas = await facturaService.obtenerMonedas();
        selectMoneda.innerHTML = "";
        if (monedas.length === 0) {
            selectMoneda.innerHTML = `<option value="" disabled selected>No hay monedas cargadas</option>`;
            return;
        }
        // Por defecto: Pesos (código AFIP "PES"/"011") o, si no existe, la primera de la lista
        const porDefecto = monedas.find((m) => ["PES", "011"].includes(m.codigoAfip)) ?? monedas[0];
        monedas.forEach((m) => {
            const esDefecto = m.id === porDefecto.id;
            // defaultSelected hace que formFactura.reset() vuelva a esta opción
            const opcion = new Option(m.simbolo, String(m.id), esDefecto, esDefecto);
            opcion.title = m.denominacion; // la denominación queda como ayuda al pasar el mouse
            selectMoneda.add(opcion);
        });
    }
    catch (error) {
        selectMoneda.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}
// ---------- Listado de facturas ----------
function badgeEstado(estado) {
    const clase = estado === "ANULADA" ? "badge-anulada" : estado === "EMITIDA" ? "badge-ok" : "badge-neutral";
    return `<span class="badge ${clase}">${esc(estado)}</span>`;
}
function renderizarFacturas(facturas) {
    tablaFacturasBody.innerHTML = "";
    if (facturas.length === 0) {
        tablaFacturasBody.innerHTML = `<tr><td colspan="8" class="vacio">No se encontraron facturas con esos filtros.</td></tr>`;
        return;
    }
    facturas.forEach((f) => {
        const tr = document.createElement("tr");
        const yaAnulada = f.estado === "ANULADA";
        const informadaArca = Boolean(f.cae && f.cae.trim() !== "");
        const columnaArca = informadaArca
            ? `<span class="badge badge-ok">Autorizada</span><span class="sub">CAE ${esc(f.cae)}</span><span class="sub">Vence ${fecha(f.caeFechaVencimiento)}</span>`
            : yaAnulada
                ? `<span class="badge badge-neutral">No presentada</span>`
                : `<span class="badge badge-pend">Pendiente</span>`;
        tr.innerHTML = `
            <td><strong>${esc(f.numeroFactura ?? "-")}</strong><span class="sub">ID ${f.id}</span></td>
            <td>${fecha(f.fechaEmision)}</td>
            <td>${esc(f.clienteDenominacion)}</td>
            <td>${esc(f.puntoVentaDescripcion)}</td>
            <td class="num"><strong>${moneda(f.importeTotal)}</strong></td>
            <td>${badgeEstado(f.estado)}</td>
            <td>${columnaArca}</td>
            <td>
                <div class="acciones">
                    <button type="button" class="ok-btn sm btn-arca" ${informadaArca || yaAnulada ? "disabled" : ""}>Informar a ARCA</button>
                    <button type="button" class="sec sm btn-editar" ${informadaArca || yaAnulada ? "disabled title='Solo se pueden editar facturas pendientes'" : ""}>Editar</button>
                    <button type="button" class="sec sm btn-pdf">PDF</button>
                    <button type="button" class="warn sm btn-anular" ${yaAnulada ? "disabled" : ""}>Anular</button>
                    <button type="button" class="danger sm btn-eliminar" ${informadaArca ? "disabled title='No se puede eliminar: ya fue informada a ARCA'" : ""}>Eliminar</button>
                </div>
            </td>
        `;
        const btnArca = tr.querySelector(".btn-arca");
        btnArca.addEventListener("click", async () => {
            if (!(await confirmar(`¿Informar la factura Nº ${f.numeroFactura} a ARCA para obtener el CAE?`, "Informar")))
                return;
            // Se deshabilita apenas se envía; solo vuelve a habilitarse si la autorización falla
            btnArca.disabled = true;
            btnArca.textContent = "Informando...";
            mostrarMensaje(`Informando factura Nº ${f.numeroFactura} a ARCA...`, "info", true);
            let autorizada = false;
            try {
                const resp = await facturaService.informarArca(f.id);
                autorizada = true;
                mostrarMensaje(`Autorizada por ARCA. CAE ${resp.cae} (vence ${fecha(resp.caeFechaVencimiento)})`);
            }
            catch (error) {
                btnArca.disabled = false;
                btnArca.textContent = "Informar a ARCA";
                mostrarError(error);
            }
            // Con la autorización, la grilla se recarga y el botón queda deshabilitado (ya tiene CAE)
            if (autorizada)
                await cargarFacturas();
        });
        tr.querySelector(".btn-editar")?.addEventListener("click", async () => {
            if (informadaArca || yaAnulada)
                return;
            try {
                const detalle = await facturaService.obtenerFacturaPorId(f.id);
                facturaEditandoId = detalle.id;
                resetearFormularioFactura();
                $("tituloModalFactura").textContent = `Editar factura Nº ${detalle.numero}`;
                formFactura.querySelector('button[type="submit"]').textContent = "Guardar cambios";
                $("numero").value = String(detalle.numero ?? "");
                $("fechaEmision").value = detalle.fechaEmision ? detalle.fechaEmision.substring(0, 10) : "";
                selectPuntoVenta.value = String(detalle.puntoVentaId ?? "");
                selectCondicionIva.value = String(detalle.condicionIvaId ?? "");
                selectMoneda.value = String(detalle.tipoMonedaId ?? "");
                $("observaciones").value = detalle.observaciones ?? "";
                if (detalle.clienteId) {
                    $("clienteId").value = String(detalle.clienteId);
                    $("clienteNombre").value = detalle.clienteCuitCuil
                        ? `${detalle.clienteDenominacion} — ${detalle.clienteCuitCuil}`
                        : detalle.clienteDenominacion;
                }
                aplicarReglaClienteCondicion();
                detallesEnMemoria = detalle.detalles.map((d) => ({ ...d }));
                renderizarDetalles();
                abrirModal(modalFactura, $("fechaEmision"));
            }
            catch (error) {
                mostrarError(error);
            }
        });
        tr.querySelector(".btn-pdf")?.addEventListener("click", () => {
            window.open(facturaService.obtenerUrlPdf(f.id), "_blank");
        });
        tr.querySelector(".btn-anular")?.addEventListener("click", async () => {
            if (!(await confirmar(`¿Anular la factura Nº ${f.numeroFactura}? Quedará registrada como ANULADA.`, "Anular", true)))
                return;
            try {
                await facturaService.actualizarEstado(f.id, "ANULADA", "Anulada desde la web");
                mostrarMensaje(`Factura Nº ${f.numeroFactura} anulada.`);
                await cargarFacturas();
            }
            catch (error) {
                mostrarError(error);
            }
        });
        tr.querySelector(".btn-eliminar")?.addEventListener("click", async () => {
            if (informadaArca)
                return;
            if (!(await confirmar(`¿Eliminar definitivamente la factura Nº ${f.numeroFactura}? Esta acción no se puede deshacer.`, "Eliminar", true)))
                return;
            try {
                await facturaService.eliminarFactura(f.id);
                mostrarMensaje(`Factura Nº ${f.numeroFactura} eliminada.`);
                await cargarFacturas();
            }
            catch (error) {
                mostrarError(error);
            }
        });
        tablaFacturasBody.appendChild(tr);
    });
}
/** Lee los filtros del formulario de búsqueda (los mismos para la grilla y el reporte PDF). */
function leerFiltros() {
    const nroStr = $("filtroNumero").value;
    const fechaDesde = $("filtroFechaDesde").value;
    const fechaHasta = $("filtroFechaHasta").value;
    const cliente = $("filtroCliente").value;
    const informadaArca = $("filtroInformadaArca").value;
    const filtros = {};
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
    return filtros;
}
async function cargarFacturas() {
    const filtros = { ...leerFiltros(), page: paginaActual, size: TAMANIO_PAGINA };
    try {
        const pageResp = await facturaService.obtenerFacturas(filtros);
        renderizarFacturas(pageResp.content);
        totalPaginas = Math.max(pageResp.totalPages, 1);
        infoPaginacion.textContent = `Página ${pageResp.page + 1} de ${totalPaginas}`;
        infoTotal.textContent = `${pageResp.totalElements} factura${pageResp.totalElements === 1 ? "" : "s"}`;
        btnPaginaAnterior.disabled = pageResp.page <= 0;
        btnPaginaSiguiente.disabled = pageResp.page + 1 >= totalPaginas;
    }
    catch (error) {
        tablaFacturasBody.innerHTML = `<tr><td colspan="8" class="vacio">No se pudieron cargar las facturas.</td></tr>`;
        mostrarError(error);
    }
}
// ---------- Explorer: Clientes ----------
let destinoModalCliente = "factura";
function seleccionarCliente(c) {
    if (destinoModalCliente === "filtro") {
        $("filtroCliente").value = c.denominacion;
    }
    else {
        $("clienteId").value = String(c.id);
        $("clienteNombre").value = c.cuitCuil ? `${c.denominacion} — ${c.cuitCuil}` : c.denominacion;
        aplicarReglaClienteCondicion();
    }
    cerrarModal(modalCliente);
}
async function cargarModalClientes(busqueda = "") {
    tablaModalClientes.innerHTML = `<tr><td colspan="3" class="vacio">Buscando...</td></tr>`;
    try {
        const clientes = await facturaService.buscarClientes(busqueda);
        tablaModalClientes.innerHTML = "";
        if (clientes.length === 0) {
            tablaModalClientes.innerHTML = `<tr><td colspan="3" class="vacio">No se encontraron clientes.</td></tr>`;
            return;
        }
        clientes.forEach((c) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${esc(c.denominacion)}<span class="sub">ID ${c.id}</span></td>
                <td>${esc(c.cuitCuil ?? "-")}</td>
                <td><button type="button" class="ok-btn sm">Elegir</button></td>
            `;
            tr.querySelector("button")?.addEventListener("click", () => seleccionarCliente(c));
            tablaModalClientes.appendChild(tr);
        });
    }
    catch (error) {
        tablaModalClientes.innerHTML = "";
        mostrarError(error);
    }
}
// ---------- Explorer: Artículos ----------
function seleccionarArticulo(a) {
    $("itemArticuloId").value = String(a.articuloId);
    $("itemListaPrecioArticuloId").value = String(a.id);
    btnAgregarItem.disabled = false;
    $("itemDescripcion").value = a.articuloDenominacion;
    $("itemPrecio").value = String(a.precioVenta);
    cerrarModal(modalArticulo);
    $("itemCantidad").focus();
}
// Lista de precios del explorer de artículos: los precios de la grilla son los de la lista elegida
const selectListaArticulo = $("listaPrecioArticulo");
let listasArticuloCargadas = false;
async function cargarListasArticulo() {
    if (listasArticuloCargadas)
        return;
    const listas = await facturaService.obtenerListasPrecio();
    selectListaArticulo.innerHTML = "";
    if (listas.length === 0) {
        selectListaArticulo.add(new Option("No hay listas de precios", "", true, true));
        return;
    }
    // Por defecto la primera lista creada (menor id), que es la de venta general
    const idDefecto = Math.min(...listas.map((l) => l.id));
    listas.forEach((l) => selectListaArticulo.add(new Option(l.denominacion, String(l.id), l.id === idDefecto, l.id === idDefecto)));
    listasArticuloCargadas = true;
}
let filasArticulos = [];
/** Muestra en cada renglón el precio de la lista elegida, sin volver a consultar al servidor. */
function mostrarPreciosLista() {
    const listaId = Number(selectListaArticulo.value);
    filasArticulos.forEach((fila) => {
        const precio = fila.precios.get(listaId);
        fila.celdaPrecio.innerHTML = precio ? moneda(precio.precioVenta) : `<span class="sub">Sin precio en esta lista</span>`;
        fila.boton.disabled = !precio;
    });
}
selectListaArticulo.addEventListener("change", mostrarPreciosLista);
async function cargarModalArticulos(busqueda = "") {
    tablaModalArticulos.innerHTML = `<tr><td colspan="3" class="vacio">Buscando...</td></tr>`;
    filasArticulos = [];
    try {
        await cargarListasArticulo();
        // Trae los precios de todas las listas; al cambiar de lista solo se cambia cuál se muestra
        const precios = await facturaService.buscarArticulosPrecio(busqueda);
        tablaModalArticulos.innerHTML = "";
        if (precios.length === 0) {
            tablaModalArticulos.innerHTML = `<tr><td colspan="3" class="vacio">No se encontraron artículos.</td></tr>`;
            return;
        }
        // Agrupar por artículo (vienen ordenados por denominación)
        const porArticulo = new Map();
        precios.forEach((p) => {
            if (!porArticulo.has(p.articuloId))
                porArticulo.set(p.articuloId, new Map());
            porArticulo.get(p.articuloId).set(p.listaPrecioId, p);
        });
        porArticulo.forEach((preciosArticulo) => {
            const a = preciosArticulo.values().next().value;
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${esc(a.articuloDenominacion)}<span class="sub">Art. ID ${a.articuloId}</span></td>
                <td class="num"></td>
                <td><button type="button" class="ok-btn sm">Elegir</button></td>
            `;
            const fila = {
                precios: preciosArticulo,
                celdaPrecio: tr.querySelector("td.num"),
                boton: tr.querySelector("button")
            };
            fila.boton.addEventListener("click", () => {
                const precio = fila.precios.get(Number(selectListaArticulo.value));
                if (precio)
                    seleccionarArticulo(precio);
            });
            filasArticulos.push(fila);
            tablaModalArticulos.appendChild(tr);
        });
        mostrarPreciosLista();
    }
    catch (error) {
        tablaModalArticulos.innerHTML = "";
        mostrarError(error);
    }
}
// ---------- Eventos de modales ----------
$("btnAbrirModalFactura").addEventListener("click", () => {
    facturaEditandoId = null;
    $("tituloModalFactura").textContent = "Nueva factura";
    formFactura.querySelector('button[type="submit"]').textContent = "Guardar factura";
    resetearFormularioFactura();
    abrirModal(modalFactura, $("fechaEmision"));
    mostrarProximoNumero();
});
$("btnAbrirModalCliente").addEventListener("click", () => {
    destinoModalCliente = "factura";
    abrirModal(modalCliente, inputBuscarCliente);
    cargarModalClientes(inputBuscarCliente.value);
});
$("btnAbrirModalClienteFiltro").addEventListener("click", () => {
    destinoModalCliente = "filtro";
    abrirModal(modalCliente, inputBuscarCliente);
    cargarModalClientes(inputBuscarCliente.value);
});
$("btnCerrarModalCliente").addEventListener("click", () => cerrarModal(modalCliente));
$("btnEjecutarBuscarCliente").addEventListener("click", () => cargarModalClientes(inputBuscarCliente.value));
inputBuscarCliente.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        cargarModalClientes(inputBuscarCliente.value);
    }
});
$("btnLimpiarCliente").addEventListener("click", () => {
    $("clienteId").value = "";
    aplicarReglaClienteCondicion();
});
selectCondicionIva.addEventListener("change", aplicarReglaClienteCondicion);
// ---------- Alta de cliente ----------
const modalNuevoCliente = $("modalNuevoCliente");
const formNuevoCliente = $("formNuevoCliente");
const inputCuit = $("ncCuitCuil");
/** Formatea mientras se escribe: 20123456789 -> 20-12345678-9 */
function formatearCuit(valor) {
    const d = valor.replace(/\D/g, "").substring(0, 11);
    if (d.length <= 2)
        return d;
    if (d.length <= 10)
        return `${d.substring(0, 2)}-${d.substring(2)}`;
    return `${d.substring(0, 2)}-${d.substring(2, 10)}-${d.substring(10)}`;
}
inputCuit.addEventListener("input", () => {
    inputCuit.value = formatearCuit(inputCuit.value);
    inputCuit.classList.remove("invalido");
});
formNuevoCliente.querySelectorAll("input").forEach((i) => i.addEventListener("input", () => i.classList.remove("invalido")));
$("btnAbrirModalNuevoCliente").addEventListener("click", () => {
    formNuevoCliente.reset();
    formNuevoCliente.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    // Reaprovecha lo que se buscó: si son números va al CUIT, si no a la denominación
    const busqueda = inputBuscarCliente.value.trim();
    if (/^[\d-]+$/.test(busqueda))
        inputCuit.value = formatearCuit(busqueda);
    else
        $("ncDenominacion").value = busqueda;
    abrirModal(modalNuevoCliente, $("ncDenominacion"));
});
formNuevoCliente.addEventListener("submit", async (e) => {
    e.preventDefault();
    const valor = (id) => $(id).value.trim();
    const errores = [];
    if (!valor("ncDenominacion"))
        errores.push(["ncDenominacion", "Completá la denominación o razón social."]);
    if (valor("ncCuitCuil").replace(/\D/g, "").length !== 11)
        errores.push(["ncCuitCuil", "El CUIT/CUIL debe tener 11 dígitos."]);
    if (!valor("ncNombreCalle"))
        errores.push(["ncNombreCalle", "Completá la calle del domicilio."]);
    if (!valor("ncNumeroCalle"))
        errores.push(["ncNumeroCalle", "Completá el número del domicilio."]);
    if (valor("ncEmail") && !$("ncEmail").checkValidity())
        errores.push(["ncEmail", "El email no tiene un formato válido."]);
    formNuevoCliente.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    if (errores.length > 0) {
        errores.forEach(([id]) => $(id).classList.add("invalido"));
        $(errores[0][0]).focus();
        mostrarMensaje(errores[0][1], "err");
        return;
    }
    const nuevo = {
        denominacion: valor("ncDenominacion"),
        cuitCuil: valor("ncCuitCuil"),
        nombreCalle: valor("ncNombreCalle"),
        numeroCalle: valor("ncNumeroCalle")
    };
    if (valor("ncEmail"))
        nuevo.email = valor("ncEmail");
    if (valor("ncTelefono"))
        nuevo.telefono = valor("ncTelefono");
    if (valor("ncCelular"))
        nuevo.celular = valor("ncCelular");
    const btnGuardar = $("btnGuardarCliente");
    btnGuardar.disabled = true;
    try {
        const creado = await facturaService.crearCliente(nuevo);
        cerrarModal(modalNuevoCliente);
        seleccionarCliente(creado);
        inputBuscarCliente.value = "";
        mostrarMensaje(`Cliente "${creado.denominacion}" creado y seleccionado.`);
    }
    catch (error) {
        mostrarError(error);
    }
    finally {
        btnGuardar.disabled = false;
    }
});
$("btnAbrirModalArticulo").addEventListener("click", () => {
    abrirModal(modalArticulo, inputBuscarArticulo);
    cargarModalArticulos(inputBuscarArticulo.value);
});
$("btnCerrarModalArticulo").addEventListener("click", () => cerrarModal(modalArticulo));
$("btnEjecutarBuscarArticulo").addEventListener("click", () => cargarModalArticulos(inputBuscarArticulo.value));
inputBuscarArticulo.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        cargarModalArticulos(inputBuscarArticulo.value);
    }
});
// ---------- Alta de artículo ----------
const modalNuevoArticulo = $("modalNuevoArticulo");
const formNuevoArticulo = $("formNuevoArticulo");
const selectRubro = $("naRubroId");
const selectMarca = $("naMarcaId");
const tablaPreciosNuevoArticulo = $("naTablaPrecios");
function llenarSelect(select, opciones, textoVacio) {
    select.innerHTML = "";
    // textoVacio != null => campo opcional, se agrega la opción "sin valor"
    if (textoVacio !== null)
        select.add(new Option(textoVacio, "", true, true));
    opciones.forEach((o, i) => {
        const esDefecto = textoVacio === null && i === 0;
        select.add(new Option(o.denominacion, String(o.id), esDefecto, esDefecto));
    });
    if (textoVacio === null && opciones.length === 0) {
        select.add(new Option("No hay opciones cargadas", "", true, true));
    }
}
/** Grilla de precios del alta: un renglón por lista con un campo de precio (en blanco = sin precio). */
function llenarTablaPrecios(listas) {
    tablaPreciosNuevoArticulo.innerHTML = "";
    if (listas.length === 0) {
        tablaPreciosNuevoArticulo.innerHTML = `<tr><td colspan="2" class="vacio">No hay listas de precios cargadas.</td></tr>`;
        return;
    }
    // Primero la lista creada primero (la de venta general), igual que el selector de artículos
    [...listas].sort((a, b) => a.id - b.id).forEach((l) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><label for="naPrecio-${l.id}">${esc(l.denominacion)}</label></td>
            <td class="num"><input type="number" id="naPrecio-${l.id}" class="input-precio" data-lista-id="${l.id}"
                step="0.01" min="0.01" placeholder="Sin precio" /></td>
        `;
        const input = tr.querySelector("input");
        input.addEventListener("input", () => {
            tablaPreciosNuevoArticulo.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
        });
        tablaPreciosNuevoArticulo.appendChild(tr);
    });
}
const inputsPrecioArticulo = () => [...tablaPreciosNuevoArticulo.querySelectorAll("input.input-precio")];
let opcionesArticuloCargadas = false;
async function cargarOpcionesArticulo() {
    if (opcionesArticuloCargadas)
        return;
    const [rubros, marcas, listas] = await Promise.all([
        facturaService.obtenerRubros(),
        facturaService.obtenerMarcas(),
        facturaService.obtenerListasPrecio()
    ]);
    llenarSelect(selectRubro, rubros, "(Sin rubro)");
    llenarSelect(selectMarca, marcas, "(Sin marca)");
    llenarTablaPrecios(listas);
    opcionesArticuloCargadas = true;
}
const inputCodigoArticulo = $("naCodigo");
inputCodigoArticulo.addEventListener("input", () => {
    inputCodigoArticulo.value = inputCodigoArticulo.value.toUpperCase();
});
formNuevoArticulo.querySelectorAll("input, select").forEach((i) => i.addEventListener("input", () => i.classList.remove("invalido")));
$("btnAbrirModalNuevoArticulo").addEventListener("click", async () => {
    try {
        await cargarOpcionesArticulo();
    }
    catch (error) {
        mostrarError(error);
        return;
    }
    formNuevoArticulo.reset();
    formNuevoArticulo.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    // Reaprovecha lo que se buscó como denominación
    $("naDenominacion").value = inputBuscarArticulo.value.trim();
    abrirModal(modalNuevoArticulo, $("naDenominacion"));
});
formNuevoArticulo.addEventListener("submit", async (e) => {
    e.preventDefault();
    const valor = (id) => $(id).value.trim();
    const errores = [];
    if (!valor("naDenominacion"))
        errores.push([[$("naDenominacion")], "Completá la denominación del artículo."]);
    if (!valor("naCodigo"))
        errores.push([[$("naCodigo")], "Completá el código del artículo."]);
    // Precios: solo las listas con un valor cargado; los cargados deben ser > 0
    const inputs = inputsPrecioArticulo();
    const cargados = inputs.filter((i) => i.value.trim() !== "");
    const invalidos = cargados.filter((i) => !(Number(i.value) > 0));
    if (inputs.length === 0) {
        errores.push([[], "No hay listas de precios cargadas para asignar el precio."]);
    }
    else if (cargados.length === 0) {
        errores.push([inputs, "Cargá el precio en al menos una lista."]);
    }
    else if (invalidos.length > 0) {
        errores.push([invalidos, "Los precios deben ser mayores a 0."]);
    }
    formNuevoArticulo.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    if (errores.length > 0) {
        errores.forEach(([campos]) => campos.forEach((c) => c.classList.add("invalido")));
        errores[0][0][0]?.focus();
        mostrarMensaje(errores[0][1], "err");
        return;
    }
    const btnGuardar = $("btnGuardarArticulo");
    btnGuardar.disabled = true;
    try {
        const creados = await facturaService.crearArticulo({
            denominacion: valor("naDenominacion"),
            codigo: valor("naCodigo"),
            rubroId: selectRubro.value ? Number(selectRubro.value) : undefined,
            marcaId: selectMarca.value ? Number(selectMarca.value) : undefined,
            precios: cargados.map((i) => ({ listaPrecioId: Number(i.dataset.listaId), precioVenta: Number(i.value) }))
        });
        // Se selecciona con el precio de la lista que se está mirando en "Seleccionar artículo"
        const listaActual = Number(selectListaArticulo.value);
        const elegido = creados.find((p) => p.listaPrecioId === listaActual) ?? creados[0];
        // Si no tiene precio en la lista que se estaba mirando, el selector pasa a la lista usada
        if (elegido.listaPrecioId !== listaActual) {
            selectListaArticulo.value = String(elegido.listaPrecioId);
            mostrarPreciosLista();
        }
        cerrarModal(modalNuevoArticulo);
        seleccionarArticulo(elegido);
        inputBuscarArticulo.value = "";
        mostrarMensaje(`Artículo "${elegido.articuloDenominacion}" creado y seleccionado.`);
    }
    catch (error) {
        mostrarError(error);
        if (error.message.startsWith("Ya existe un artículo con el código")) {
            inputCodigoArticulo.classList.add("invalido");
            inputCodigoArticulo.focus();
        }
    }
    finally {
        btnGuardar.disabled = false;
    }
});
// ---------- Formulario de factura ----------
btnAgregarItem.addEventListener("click", () => {
    const articuloId = Number($("itemArticuloId").value);
    const descripcion = $("itemDescripcion").value.trim();
    const cantidad = Number($("itemCantidad").value);
    const precioStr = $("itemPrecio").value;
    const bonificacion = Number($("itemBonificacion").value) || 0;
    if (!articuloId) {
        mostrarMensaje("Primero elegí un artículo con el botón Buscar.", "err");
        return;
    }
    if (!(cantidad > 0)) {
        mostrarMensaje("La cantidad tiene que ser mayor a 0.", "err");
        return;
    }
    if (bonificacion < 0 || bonificacion > 100) {
        mostrarMensaje("La bonificación tiene que estar entre 0 y 100%.", "err");
        return;
    }
    const nuevoItem = { articuloId, cantidad, porcentajeBonificacion: bonificacion };
    if (descripcion)
        nuevoItem.descripcion = descripcion;
    if (precioStr)
        nuevoItem.precioUnitario = Number(precioStr);
    const listaPrecioArticuloId = Number($("itemListaPrecioArticuloId").value);
    if (listaPrecioArticuloId)
        nuevoItem.listaPrecioArticuloId = listaPrecioArticuloId;
    detallesEnMemoria.push(nuevoItem);
    renderizarDetalles();
    limpiarItemActual();
});
formFactura.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (detallesEnMemoria.length === 0) {
        mostrarMensaje("Agregá al menos un ítem antes de guardar la factura.", "err");
        return;
    }
    if (!selectPuntoVenta.value) {
        mostrarMensaje("Elegí un punto de venta de la lista.", "err");
        selectPuntoVenta.focus();
        return;
    }
    if (!selectCondicionIva.value) {
        mostrarMensaje("Elegí una condición de IVA de la lista.", "err");
        selectCondicionIva.focus();
        return;
    }
    if (!condicionEsConsumidorFinal() && !$("clienteId").value) {
        const condicion = selectCondicionIva.selectedOptions[0]?.text ?? "";
        mostrarMensaje(`Con la condición de IVA "${condicion}" hay que elegir un cliente; no puede ser Consumidor Final.`, "err");
        $("clienteNombre").classList.add("invalido");
        $("btnAbrirModalCliente").focus();
        return;
    }
    if (!selectMoneda.value) {
        mostrarMensaje("Elegí una moneda de la lista.", "err");
        selectMoneda.focus();
        return;
    }
    const clienteIdStr = $("clienteId").value;
    const nuevaFactura = {
        // Al crear, el backend asigna el número; al editar se conserva el que ya tiene
        numero: facturaEditandoId !== null ? Number($("numero").value) : undefined,
        fechaEmision: $("fechaEmision").value,
        clienteId: clienteIdStr ? Number(clienteIdStr) : null,
        condicionIvaId: Number(selectCondicionIva.value),
        puntoVentaId: Number(selectPuntoVenta.value),
        tipoMonedaId: Number(selectMoneda.value),
        estado: "EMITIDA",
        observaciones: $("observaciones").value.trim(),
        detalles: detallesEnMemoria
    };
    // Bloquear el botón mientras se guarda para evitar envíos duplicados
    const btnGuardarFactura = formFactura.querySelector('button[type="submit"]');
    const textoBoton = btnGuardarFactura.textContent;
    btnGuardarFactura.disabled = true;
    btnGuardarFactura.textContent = "Guardando...";
    mostrarMensaje("Guardando factura...", "info", true);
    try {
        if (facturaEditandoId !== null) {
            const actualizada = await facturaService.actualizarFactura(facturaEditandoId, nuevaFactura);
            mostrarMensaje(`Factura Nº ${actualizada.numeroFactura} actualizada. Total: ${moneda(actualizada.importeTotal)}`);
            facturaEditandoId = null;
        }
        else {
            const creada = await facturaService.crearFactura(nuevaFactura);
            mostrarMensaje(`Factura Nº ${creada.numeroFactura} creada. Total: ${moneda(creada.importeTotal)}`);
            paginaActual = 0;
        }
        resetearFormularioFactura();
        cerrarModal(modalFactura);
        await cargarFacturas();
    }
    catch (error) {
        btnGuardarFactura.textContent = textoBoton;
        mostrarError(error);
        // Número repetido: marcar el campo para que se corrija
        if (error.message.startsWith("Ya existe una factura con el número")) {
            const inputNumero = $("numero");
            inputNumero.classList.add("invalido");
            inputNumero.focus();
            inputNumero.addEventListener("input", () => inputNumero.classList.remove("invalido"), { once: true });
        }
    }
    finally {
        btnGuardarFactura.disabled = false;
    }
});
// ---------- Filtros y paginación ----------
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
// Reporte PDF con todas las facturas filtradas (todas las páginas); sin filtros, todas las facturas
$("btnReportePdf").addEventListener("click", () => {
    window.open(facturaService.obtenerUrlReportePdf(leerFiltros()), "_blank");
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
// ---------- Carga inicial ----------
resetearFormularioFactura();
// La pantalla principal se muestra recién cuando terminan todas las cargas (bien o con error)
Promise.allSettled([
    cargarPuntosVenta(),
    cargarCondicionesIva(),
    cargarMonedas(),
    cargarFacturas()
]).then(() => {
    const pantallaCarga = $("pantallaCarga");
    pantallaCarga.classList.add("oculta");
    pantallaCarga.addEventListener("transitionend", () => pantallaCarga.remove(), { once: true });
});
