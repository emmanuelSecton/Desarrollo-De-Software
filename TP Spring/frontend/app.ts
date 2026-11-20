import {
    ClienteDTO,
    ClienteRequestDTO,
    FacturaDetalleRequestDTO,
    FacturaFiltrosDTO,
    FacturaReporteDTO,
    FacturaRequestDTO,
    ListaPrecioArticuloDTO,
    OpcionDTO
} from "./models/factura.dto.js";
import { FacturaService } from "./services/FacturaService.js";

const facturaService = new FacturaService();
let detallesEnMemoria: FacturaDetalleRequestDTO[] = [];
let facturaEditandoId: number | null = null;
let paginaActual = 0;
let totalPaginas = 1;
const TAMANIO_PAGINA = 5;

// ---------- Referencias DOM ----------
const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

const divMensaje = $<HTMLDivElement>("mensaje");
const formFiltros = $<HTMLFormElement>("formFiltros");
const btnLimpiarFiltros = $<HTMLButtonElement>("btnLimpiarFiltros");
const formFactura = $<HTMLFormElement>("formFactura");
const btnAgregarItem = $<HTMLButtonElement>("btnAgregarItem");
const tablaDetallesBody = $<HTMLTableSectionElement>("tablaDetallesBody");
const tablaFacturasBody = $<HTMLTableSectionElement>("tablaFacturasBody");
const totalEstimado = $<HTMLElement>("totalEstimado");
const infoTotal = $<HTMLElement>("infoTotal");

const btnPaginaAnterior = $<HTMLButtonElement>("btnPaginaAnterior");
const btnPaginaSiguiente = $<HTMLButtonElement>("btnPaginaSiguiente");
const infoPaginacion = $<HTMLSpanElement>("infoPaginacion");

const modalFactura = $<HTMLDivElement>("modalFactura");
const modalCliente = $<HTMLDivElement>("modalCliente");
const inputBuscarCliente = $<HTMLInputElement>("inputBuscarCliente");
const tablaModalClientes = $<HTMLTableSectionElement>("tablaModalClientes");
const modalArticulo = $<HTMLDivElement>("modalArticulo");
const inputBuscarArticulo = $<HTMLInputElement>("inputBuscarArticulo");
const tablaModalArticulos = $<HTMLTableSectionElement>("tablaModalArticulos");
const modalConfirmar = $<HTMLDivElement>("modalConfirmar");

// ---------- Utilidades ----------
const formatoMoneda = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" });
const moneda = (n: number) => formatoMoneda.format(n);

function fecha(iso?: string | null): string {
    if (!iso) return "-";
    const [a, m, d] = iso.substring(0, 10).split("-");
    return d && m && a ? `${d}/${m}/${a}` : iso;
}

function esc(valor: unknown): string {
    return String(valor ?? "").replace(/[&<>"']/g, (c) =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c] as string
    );
}

let timerMensaje: number | undefined;
/** Muestra un aviso. Con `fijo = true` queda visible hasta que otro mensaje lo reemplace. */
function mostrarMensaje(texto: string, tipo: "ok" | "err" | "info" = "ok", fijo = false): void {
    divMensaje.textContent = texto;
    divMensaje.className = tipo;
    window.clearTimeout(timerMensaje);
    if (!fijo) {
        timerMensaje = window.setTimeout(() => (divMensaje.className = ""), tipo === "err" ? 7000 : 4000);
    }
}
const mostrarError = (error: unknown) => mostrarMensaje((error as Error).message, "err");

// ---------- Modales ----------
function abrirModal(modal: HTMLElement, foco?: HTMLElement): void {
    modal.classList.add("abierto");
    (foco ?? modal.querySelector<HTMLElement>("input:not([type=hidden]):not([readonly]), button"))?.focus();
}

function cerrarModal(modal: HTMLElement): void {
    modal.classList.remove("abierto");
}

/** Modal de confirmación propio (reemplaza window.confirm). */
function confirmar(texto: string, etiquetaOk = "Confirmar", peligro = false): Promise<boolean> {
    const btnSi = $<HTMLButtonElement>("btnConfirmarSi");
    const btnNo = $<HTMLButtonElement>("btnConfirmarNo");
    $<HTMLElement>("textoConfirmar").textContent = texto;
    btnSi.textContent = etiquetaOk;
    btnSi.className = peligro ? "danger-solid" : "";
    abrirModal(modalConfirmar, btnNo);

    return new Promise((resolve) => {
        const terminar = (resultado: boolean) => {
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
document.querySelectorAll<HTMLButtonElement>("[data-cerrar]").forEach((btn) =>
    btn.addEventListener("click", () => cerrarModal($(btn.dataset.cerrar!)))
);
document.querySelectorAll<HTMLElement>(".modal-overlay").forEach((overlay) =>
    overlay.addEventListener("mousedown", (e) => {
        if (e.target !== overlay) return;
        if (overlay === modalConfirmar) overlay.dispatchEvent(new Event("cancelar"));
        else cerrarModal(overlay);
    })
);
document.addEventListener("keydown", (e) => {
    if (e.key !== "Escape") return;
    const abiertos = [...document.querySelectorAll<HTMLElement>(".modal-overlay.abierto")];
    const superior = abiertos.sort((a, b) => Number(getComputedStyle(b).zIndex) - Number(getComputedStyle(a).zIndex))[0];
    if (!superior) return;
    if (superior === modalConfirmar) superior.dispatchEvent(new Event("cancelar"));
    else cerrarModal(superior);
});

// ---------- Ítems de la factura ----------
function subtotalItem(item: FacturaDetalleRequestDTO): number | null {
    if (item.precioUnitario === undefined) return null;
    return item.cantidad * item.precioUnitario * (1 - item.porcentajeBonificacion / 100);
}

function renderizarDetalles(): void {
    tablaDetallesBody.innerHTML = "";

    if (detallesEnMemoria.length === 0) {
        tablaDetallesBody.innerHTML = `<tr><td colspan="6" class="vacio">Todavía no agregaste ítems.</td></tr>`;
        totalEstimado.textContent = moneda(0);
        return;
    }

    let total = 0;
    detallesEnMemoria.forEach((item, index) => {
        const subtotal = subtotalItem(item);
        if (subtotal !== null) total += subtotal;

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

function limpiarItemActual(): void {
    $<HTMLInputElement>("itemArticuloId").value = "";
    $<HTMLInputElement>("itemDescripcion").value = "";
    $<HTMLInputElement>("itemCantidad").value = "1";
    $<HTMLInputElement>("itemPrecio").value = "";
    $<HTMLInputElement>("itemBonificacion").value = "0";
}

function resetearFormularioFactura(): void {
    formFactura.reset();
    $<HTMLInputElement>("clienteId").value = "";
    $<HTMLInputElement>("clienteNombre").value = "Consumidor Final";
    $<HTMLInputElement>("fechaEmision").value = new Date().toISOString().substring(0, 10);
    limpiarItemActual();
    detallesEnMemoria = [];
    renderizarDetalles();
}

// ---------- Puntos de venta (solo los existentes en la base) ----------
const selectPuntoVenta = $<HTMLSelectElement>("puntoVentaId");

async function cargarPuntosVenta(): Promise<void> {
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
    } catch (error) {
        selectPuntoVenta.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}

// ---------- Condiciones de IVA (solo las existentes en la base) ----------
const selectCondicionIva = $<HTMLSelectElement>("condicionIvaId");

async function cargarCondicionesIva(): Promise<void> {
    try {
        const condiciones = await facturaService.obtenerCondicionesIva();
        selectCondicionIva.innerHTML = "";

        if (condiciones.length === 0) {
            selectCondicionIva.innerHTML = `<option value="" disabled selected>No hay condiciones cargadas</option>`;
            return;
        }

        // Por defecto: Responsable Inscripto (código AFIP 1) o, si no existe, la primera
        const porDefecto = condiciones.find((c) => c.codigoAfip === 1) ?? condiciones[0];
        condiciones.forEach((c) => {
            const esDefecto = c.id === porDefecto.id;
            // defaultSelected hace que formFactura.reset() vuelva a esta opción
            selectCondicionIva.add(new Option(c.denominacion, String(c.id), esDefecto, esDefecto));
        });
    } catch (error) {
        selectCondicionIva.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}

// ---------- Monedas (solo las existentes en la base) ----------
const selectMoneda = $<HTMLSelectElement>("tipoMonedaId");

async function cargarMonedas(): Promise<void> {
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
        });    } catch (error) {
        selectMoneda.innerHTML = `<option value="" disabled selected>No se pudieron cargar</option>`;
        mostrarError(error);
    }
}

// ---------- Listado de facturas ----------
function badgeEstado(estado: string): string {
    const clase = estado === "ANULADA" ? "badge-anulada" : estado === "EMITIDA" ? "badge-ok" : "badge-neutral";
    return `<span class="badge ${clase}">${esc(estado)}</span>`;
}

function renderizarFacturas(facturas: FacturaReporteDTO[]): void {
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

        tr.querySelector(".btn-arca")?.addEventListener("click", async () => {
            if (!(await confirmar(`¿Informar la factura Nº ${f.numeroFactura} a ARCA para obtener el CAE?`, "Informar"))) return;
            try {
                mostrarMensaje(`Informando factura Nº ${f.numeroFactura} a ARCA...`, "info");
                const resp = await facturaService.informarArca(f.id);
                mostrarMensaje(`Autorizada por ARCA. CAE ${resp.cae} (vence ${fecha(resp.caeFechaVencimiento)})`);
                await cargarFacturas();
            } catch (error) {
                mostrarError(error);
            }
        });
        tr.querySelector(".btn-editar")?.addEventListener("click", async () => {
            if (informadaArca || yaAnulada) return;
            try {
                const detalle = await facturaService.obtenerFacturaPorId(f.id);
                facturaEditandoId = detalle.id;
                resetearFormularioFactura();

                $<HTMLElement>("tituloModalFactura").textContent = `Editar factura Nº ${detalle.numero}`;
                (formFactura.querySelector('button[type="submit"]') as HTMLButtonElement).textContent = "Guardar cambios";

                $<HTMLInputElement>("numero").value = String(detalle.numero ?? "");
                $<HTMLInputElement>("fechaEmision").value = detalle.fechaEmision ? detalle.fechaEmision.substring(0, 10) : "";
                selectPuntoVenta.value = String(detalle.puntoVentaId ?? "");
                selectCondicionIva.value = String(detalle.condicionIvaId ?? "");
                selectMoneda.value = String(detalle.tipoMonedaId ?? "");
                $<HTMLInputElement>("observaciones").value = detalle.observaciones ?? "";

                if (detalle.clienteId) {
                    $<HTMLInputElement>("clienteId").value = String(detalle.clienteId);
                    $<HTMLInputElement>("clienteNombre").value = detalle.clienteCuitCuil
                        ? `${detalle.clienteDenominacion} — ${detalle.clienteCuitCuil}`
                        : detalle.clienteDenominacion;
                }

                detallesEnMemoria = detalle.detalles.map((d) => ({ ...d }));
                renderizarDetalles();
                abrirModal(modalFactura, $("numero"));
            } catch (error) {
                mostrarError(error);
            }
        });

        tr.querySelector(".btn-pdf")?.addEventListener("click", () => {
            window.open(facturaService.obtenerUrlPdf(f.id), "_blank");
        });

        tr.querySelector(".btn-anular")?.addEventListener("click", async () => {
            if (!(await confirmar(`¿Anular la factura Nº ${f.numeroFactura}? Quedará registrada como ANULADA.`, "Anular", true))) return;
            try {
                await facturaService.actualizarEstado(f.id, "ANULADA", "Anulada desde la web");
                mostrarMensaje(`Factura Nº ${f.numeroFactura} anulada.`);
                await cargarFacturas();
            } catch (error) {
                mostrarError(error);
            }
        });

        tr.querySelector(".btn-eliminar")?.addEventListener("click", async () => {
            if (informadaArca) return;
            if (!(await confirmar(`¿Eliminar definitivamente la factura Nº ${f.numeroFactura}? Esta acción no se puede deshacer.`, "Eliminar", true))) return;
            try {
                await facturaService.eliminarFactura(f.id);
                mostrarMensaje(`Factura Nº ${f.numeroFactura} eliminada.`);
                await cargarFacturas();
            } catch (error) {
                mostrarError(error);
            }
        });

        tablaFacturasBody.appendChild(tr);
    });
}

async function cargarFacturas(): Promise<void> {
    const nroStr = $<HTMLInputElement>("filtroNumero").value;
    const fechaDesde = $<HTMLInputElement>("filtroFechaDesde").value;
    const fechaHasta = $<HTMLInputElement>("filtroFechaHasta").value;
    const cliente = $<HTMLInputElement>("filtroCliente").value;
    const informadaArca = $<HTMLSelectElement>("filtroInformadaArca").value;

    const filtros: FacturaFiltrosDTO = { page: paginaActual, size: TAMANIO_PAGINA };
    if (nroStr) filtros.numeroFactura = Number(nroStr);
    if (fechaDesde) filtros.fechaDesde = fechaDesde;
    if (fechaHasta) filtros.fechaHasta = fechaHasta;
    if (cliente) filtros.cliente = cliente;
    if (informadaArca) filtros.informadaArca = informadaArca;

    try {
        const pageResp = await facturaService.obtenerFacturas(filtros);
        renderizarFacturas(pageResp.content);
        totalPaginas = Math.max(pageResp.totalPages, 1);
        infoPaginacion.textContent = `Página ${pageResp.page + 1} de ${totalPaginas}`;
        infoTotal.textContent = `${pageResp.totalElements} factura${pageResp.totalElements === 1 ? "" : "s"}`;
        btnPaginaAnterior.disabled = pageResp.page <= 0;
        btnPaginaSiguiente.disabled = pageResp.page + 1 >= totalPaginas;
    } catch (error) {
        tablaFacturasBody.innerHTML = `<tr><td colspan="8" class="vacio">No se pudieron cargar las facturas.</td></tr>`;
        mostrarError(error);
    }
}

// ---------- Explorer: Clientes ----------
let destinoModalCliente: "factura" | "filtro" = "factura";

function seleccionarCliente(c: ClienteDTO): void {
    if (destinoModalCliente === "filtro") {
        $<HTMLInputElement>("filtroCliente").value = c.denominacion;
    } else {
        $<HTMLInputElement>("clienteId").value = String(c.id);
        $<HTMLInputElement>("clienteNombre").value = c.cuitCuil ? `${c.denominacion} — ${c.cuitCuil}` : c.denominacion;
    }
    cerrarModal(modalCliente);
}

async function cargarModalClientes(busqueda = ""): Promise<void> {
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
    } catch (error) {
        tablaModalClientes.innerHTML = "";
        mostrarError(error);
    }
}

// ---------- Explorer: Artículos ----------
function seleccionarArticulo(a: ListaPrecioArticuloDTO): void {
    $<HTMLInputElement>("itemArticuloId").value = String(a.articuloId);
    $<HTMLInputElement>("itemDescripcion").value = a.articuloDenominacion;
    $<HTMLInputElement>("itemPrecio").value = String(a.precioVenta);
    cerrarModal(modalArticulo);
    $<HTMLInputElement>("itemCantidad").focus();
}

async function cargarModalArticulos(busqueda = ""): Promise<void> {
    tablaModalArticulos.innerHTML = `<tr><td colspan="3" class="vacio">Buscando...</td></tr>`;
    try {
        const articulos = await facturaService.buscarArticulosPrecio(busqueda);
        tablaModalArticulos.innerHTML = "";
        if (articulos.length === 0) {
            tablaModalArticulos.innerHTML = `<tr><td colspan="3" class="vacio">No se encontraron artículos.</td></tr>`;
            return;
        }
        articulos.forEach((a) => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${esc(a.articuloDenominacion)}<span class="sub">Art. ID ${a.articuloId}</span></td>
                <td class="num">${moneda(a.precioVenta)}</td>
                <td><button type="button" class="ok-btn sm">Elegir</button></td>
            `;
            tr.querySelector("button")?.addEventListener("click", () => seleccionarArticulo(a));
            tablaModalArticulos.appendChild(tr);
        });
    } catch (error) {
        tablaModalArticulos.innerHTML = "";
        mostrarError(error);
    }
}

// ---------- Eventos de modales ----------
$("btnAbrirModalFactura").addEventListener("click", () => {
    facturaEditandoId = null;
    $<HTMLElement>("tituloModalFactura").textContent = "Nueva factura";
    (formFactura.querySelector('button[type="submit"]') as HTMLButtonElement).textContent = "Guardar factura";
    resetearFormularioFactura();
    abrirModal(modalFactura, $("numero"));
});

$("btnAbrirModalCliente").addEventListener("click", () => {
    destinoModalCliente = "factura";
    $("btnAbrirModalNuevoCliente").style.display = "";
    abrirModal(modalCliente, inputBuscarCliente);
    cargarModalClientes(inputBuscarCliente.value);
});

$("btnAbrirModalClienteFiltro").addEventListener("click", () => {
    destinoModalCliente = "filtro";
    $("btnAbrirModalNuevoCliente").style.display = "none";
    abrirModal(modalCliente, inputBuscarCliente);
    cargarModalClientes(inputBuscarCliente.value);
});
$("btnCerrarModalCliente").addEventListener("click", () => cerrarModal(modalCliente));
$("btnEjecutarBuscarCliente").addEventListener("click", () => cargarModalClientes(inputBuscarCliente.value));
inputBuscarCliente.addEventListener("keydown", (e) => {
    if (e.key === "Enter") { e.preventDefault(); cargarModalClientes(inputBuscarCliente.value); }
});
$("btnLimpiarCliente").addEventListener("click", () => {
    $<HTMLInputElement>("clienteId").value = "";
    $<HTMLInputElement>("clienteNombre").value = "Consumidor Final";
});

// ---------- Alta de cliente ----------
const modalNuevoCliente = $<HTMLDivElement>("modalNuevoCliente");
const formNuevoCliente = $<HTMLFormElement>("formNuevoCliente");
const inputCuit = $<HTMLInputElement>("ncCuitCuil");

/** Formatea mientras se escribe: 20123456789 -> 20-12345678-9 */
function formatearCuit(valor: string): string {
    const d = valor.replace(/\D/g, "").substring(0, 11);
    if (d.length <= 2) return d;
    if (d.length <= 10) return `${d.substring(0, 2)}-${d.substring(2)}`;
    return `${d.substring(0, 2)}-${d.substring(2, 10)}-${d.substring(10)}`;
}

inputCuit.addEventListener("input", () => {
    inputCuit.value = formatearCuit(inputCuit.value);
    inputCuit.classList.remove("invalido");
});
formNuevoCliente.querySelectorAll("input").forEach((i) =>
    i.addEventListener("input", () => i.classList.remove("invalido"))
);

$("btnAbrirModalNuevoCliente").addEventListener("click", () => {
    formNuevoCliente.reset();
    formNuevoCliente.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    // Reaprovecha lo que se buscó: si son números va al CUIT, si no a la denominación
    const busqueda = inputBuscarCliente.value.trim();
    if (/^[\d-]+$/.test(busqueda)) inputCuit.value = formatearCuit(busqueda);
    else $<HTMLInputElement>("ncDenominacion").value = busqueda;
    abrirModal(modalNuevoCliente, $("ncDenominacion"));
});

formNuevoCliente.addEventListener("submit", async (e) => {
    e.preventDefault();
    const valor = (id: string) => $<HTMLInputElement>(id).value.trim();

    const errores: [string, string][] = [];
    if (!valor("ncDenominacion")) errores.push(["ncDenominacion", "Completá la denominación o razón social."]);
    if (valor("ncCuitCuil").replace(/\D/g, "").length !== 11) errores.push(["ncCuitCuil", "El CUIT/CUIL debe tener 11 dígitos."]);
    if (!valor("ncNombreCalle")) errores.push(["ncNombreCalle", "Completá la calle del domicilio."]);
    if (!valor("ncNumeroCalle")) errores.push(["ncNumeroCalle", "Completá el número del domicilio."]);
    if (valor("ncEmail") && !$<HTMLInputElement>("ncEmail").checkValidity()) errores.push(["ncEmail", "El email no tiene un formato válido."]);

    formNuevoCliente.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    if (errores.length > 0) {
        errores.forEach(([id]) => $(id).classList.add("invalido"));
        $(errores[0][0]).focus();
        mostrarMensaje(errores[0][1], "err");
        return;
    }

    const nuevo: ClienteRequestDTO = {
        denominacion: valor("ncDenominacion"),
        cuitCuil: valor("ncCuitCuil"),
        nombreCalle: valor("ncNombreCalle"),
        numeroCalle: valor("ncNumeroCalle")
    };
    if (valor("ncEmail")) nuevo.email = valor("ncEmail");
    if (valor("ncTelefono")) nuevo.telefono = valor("ncTelefono");
    if (valor("ncCelular")) nuevo.celular = valor("ncCelular");

    const btnGuardar = $<HTMLButtonElement>("btnGuardarCliente");
    btnGuardar.disabled = true;
    try {
        const creado = await facturaService.crearCliente(nuevo);
        cerrarModal(modalNuevoCliente);
        seleccionarCliente(creado);
        inputBuscarCliente.value = "";
        mostrarMensaje(`Cliente "${creado.denominacion}" creado y seleccionado.`);
    } catch (error) {
        mostrarError(error);
    } finally {
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
    if (e.key === "Enter") { e.preventDefault(); cargarModalArticulos(inputBuscarArticulo.value); }
});

// ---------- Alta de artículo ----------
const modalNuevoArticulo = $<HTMLDivElement>("modalNuevoArticulo");
const formNuevoArticulo = $<HTMLFormElement>("formNuevoArticulo");
const selectRubro = $<HTMLSelectElement>("naRubroId");
const selectMarca = $<HTMLSelectElement>("naMarcaId");
const selectListaPrecio = $<HTMLSelectElement>("naListaPrecioId");

function llenarSelect(select: HTMLSelectElement, opciones: OpcionDTO[], textoVacio: string | null): void {
    select.innerHTML = "";
    // textoVacio != null => campo opcional, se agrega la opción "sin valor"
    if (textoVacio !== null) select.add(new Option(textoVacio, "", true, true));
    opciones.forEach((o, i) => {
        const esDefecto = textoVacio === null && i === 0;
        select.add(new Option(o.denominacion, String(o.id), esDefecto, esDefecto));
    });
    if (textoVacio === null && opciones.length === 0) {
        select.add(new Option("No hay opciones cargadas", "", true, true));
    }
}

let opcionesArticuloCargadas = false;
async function cargarOpcionesArticulo(): Promise<void> {
    if (opcionesArticuloCargadas) return;
    const [rubros, marcas, listas] = await Promise.all([
        facturaService.obtenerRubros(),
        facturaService.obtenerMarcas(),
        facturaService.obtenerListasPrecio()
    ]);
    llenarSelect(selectRubro, rubros, "(Sin rubro)");
    llenarSelect(selectMarca, marcas, "(Sin marca)");
    llenarSelect(selectListaPrecio, listas, null);
    opcionesArticuloCargadas = true;
}

const inputCodigoArticulo = $<HTMLInputElement>("naCodigo");
inputCodigoArticulo.addEventListener("input", () => {
    inputCodigoArticulo.value = inputCodigoArticulo.value.toUpperCase();
});
formNuevoArticulo.querySelectorAll("input, select").forEach((i) =>
    i.addEventListener("input", () => i.classList.remove("invalido"))
);

$("btnAbrirModalNuevoArticulo").addEventListener("click", async () => {
    try {
        await cargarOpcionesArticulo();
    } catch (error) {
        mostrarError(error);
        return;
    }
    formNuevoArticulo.reset();
    formNuevoArticulo.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    // Reaprovecha lo que se buscó como denominación
    $<HTMLInputElement>("naDenominacion").value = inputBuscarArticulo.value.trim();
    abrirModal(modalNuevoArticulo, $("naDenominacion"));
});

formNuevoArticulo.addEventListener("submit", async (e) => {
    e.preventDefault();
    const valor = (id: string) => $<HTMLInputElement>(id).value.trim();
    const precio = Number(valor("naPrecioVenta"));

    const errores: [string, string][] = [];
    if (!valor("naDenominacion")) errores.push(["naDenominacion", "Completá la denominación del artículo."]);
    if (!valor("naCodigo")) errores.push(["naCodigo", "Completá el código del artículo."]);
    if (!selectListaPrecio.value) errores.push(["naListaPrecioId", "Elegí una lista de precios."]);
    if (!(precio > 0)) errores.push(["naPrecioVenta", "El precio de venta debe ser mayor a 0."]);

    formNuevoArticulo.querySelectorAll(".invalido").forEach((i) => i.classList.remove("invalido"));
    if (errores.length > 0) {
        errores.forEach(([id]) => $(id).classList.add("invalido"));
        $(errores[0][0]).focus();
        mostrarMensaje(errores[0][1], "err");
        return;
    }

    const btnGuardar = $<HTMLButtonElement>("btnGuardarArticulo");
    btnGuardar.disabled = true;
    try {
        const creado = await facturaService.crearArticulo({
            denominacion: valor("naDenominacion"),
            codigo: valor("naCodigo"),
            rubroId: selectRubro.value ? Number(selectRubro.value) : undefined,
            marcaId: selectMarca.value ? Number(selectMarca.value) : undefined,
            listaPrecioId: Number(selectListaPrecio.value),
            precioVenta: precio
        });
        cerrarModal(modalNuevoArticulo);
        seleccionarArticulo(creado);
        inputBuscarArticulo.value = "";
        mostrarMensaje(`Artículo "${creado.articuloDenominacion}" creado y seleccionado.`);
    } catch (error) {
        mostrarError(error);
        if ((error as Error).message.startsWith("Ya existe un artículo con el código")) {
            inputCodigoArticulo.classList.add("invalido");
            inputCodigoArticulo.focus();
        }
    } finally {
        btnGuardar.disabled = false;
    }
});

// ---------- Formulario de factura ----------
btnAgregarItem.addEventListener("click", () => {
    const articuloId = Number($<HTMLInputElement>("itemArticuloId").value);
    const descripcion = $<HTMLInputElement>("itemDescripcion").value.trim();
    const cantidad = Number($<HTMLInputElement>("itemCantidad").value);
    const precioStr = $<HTMLInputElement>("itemPrecio").value;
    const bonificacion = Number($<HTMLInputElement>("itemBonificacion").value) || 0;

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

    const nuevoItem: FacturaDetalleRequestDTO = { articuloId, cantidad, porcentajeBonificacion: bonificacion };
    if (descripcion) nuevoItem.descripcion = descripcion;
    if (precioStr) nuevoItem.precioUnitario = Number(precioStr);

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
    if (!selectMoneda.value) {
        mostrarMensaje("Elegí una moneda de la lista.", "err");
        selectMoneda.focus();
        return;
    }

    const clienteIdStr = $<HTMLInputElement>("clienteId").value;
    const nuevaFactura: FacturaRequestDTO = {
        numero: Number($<HTMLInputElement>("numero").value),
        fechaEmision: $<HTMLInputElement>("fechaEmision").value,
        clienteId: clienteIdStr ? Number(clienteIdStr) : null,
        condicionIvaId: Number(selectCondicionIva.value),
        puntoVentaId: Number(selectPuntoVenta.value),
        tipoMonedaId: Number(selectMoneda.value),
        estado: "EMITIDA",
        observaciones: $<HTMLInputElement>("observaciones").value.trim(),
        detalles: detallesEnMemoria
    };

    // Bloquear el botón mientras se guarda para evitar envíos duplicados
    const btnGuardarFactura = formFactura.querySelector('button[type="submit"]') as HTMLButtonElement;
    const textoBoton = btnGuardarFactura.textContent;
    btnGuardarFactura.disabled = true;
    btnGuardarFactura.textContent = "Guardando...";
    mostrarMensaje("Guardando factura...", "info", true);

    try {
        if (facturaEditandoId !== null) {
            const actualizada = await facturaService.actualizarFactura(facturaEditandoId, nuevaFactura);
            mostrarMensaje(`Factura Nº ${actualizada.numeroFactura} actualizada. Total: ${moneda(actualizada.importeTotal)}`);
            facturaEditandoId = null;
        } else {
            const creada = await facturaService.crearFactura(nuevaFactura);
            mostrarMensaje(`Factura Nº ${creada.numeroFactura} creada. Total: ${moneda(creada.importeTotal)}`);
            paginaActual = 0;
        }
        resetearFormularioFactura();
        cerrarModal(modalFactura);
        await cargarFacturas();
    } catch (error) {
        btnGuardarFactura.textContent = textoBoton;
        mostrarError(error);
        // Número repetido: marcar el campo para que se corrija
        if ((error as Error).message.startsWith("Ya existe una factura con el número")) {
            const inputNumero = $<HTMLInputElement>("numero");
            inputNumero.classList.add("invalido");
            inputNumero.focus();
            inputNumero.addEventListener("input", () => inputNumero.classList.remove("invalido"), { once: true });
        }
    } finally {
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
cargarPuntosVenta();
cargarCondicionesIva();
cargarMonedas();
cargarFacturas();
