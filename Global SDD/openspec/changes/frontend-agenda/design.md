# Design — frontend-agenda (C-13)

## Context

- C-07 dejó la SPA con rutas, sesión, el cliente HTTP (`api()` con CSRF y `ErrorDeApi`) y componentes base (`Boton`, `Campo`, `Alerta`, `Aviso`, `Dialogo`, `EstadoVacio`, `Cargando`). `/agenda` muestra hoy un estado vacío.
- C-10 expone `GET /turnos?desde&hasta` (hasta 31 días; cada fila trae `version`, paciente y prestación), `POST /turnos`, `PUT /turnos/{id}` (reprogramar: `version`, `inicio`, `duracionMinutos`; **sin** sobreturno), `POST /turnos/{id}/cancelar` y `/confirmar`. Errores: `409 turno-superpuesto`, `409 turno-modificado`, `409 transicion-invalida`, `400` con errores por campo.
- C-09 expone `GET /pacientes?q=` (DNI, nombre o teléfono, paginado) y `POST /pacientes` (consentimiento obligatorio). C-05 expone `GET /horarios`, `GET /prestaciones` y `GET /consultorio` (con `confirmacionLimiteHoras`).
- La zona del consultorio vive en `APP_TIMEZONE` (`ZonaHorariaConsultorio` en el backend) y la API no la expone.
- La CSP es `style-src 'self'`: no se pueden usar atributos `style` en el HTML. Sí se puede asignar estilos desde JavaScript (CSSOM), que es lo que hace React con la prop `style`.

## Goals / Non-Goals

**Goals:**
- Que la demo muestre una agenda creíble: grilla del día con franjas, semana y el ciclo dar → confirmar → reprogramar → cancelar.
- Que la lógica de fechas quede aislada y testeada, porque es donde aparecen los errores de zona horaria.

**Non-Goals:**
- Caché de datos o actualización en tiempo real entre computadoras: se recarga al volver a la pestaña y tras cada acción; los conflictos los resuelve el `409`.
- Arrastrar turnos, bloqueos y asistencia (C-38).

## Decisions

### D1 — Fechas con `Intl` y la zona IANA del consultorio
Un módulo `src/shared/formatos/fechas.ts` resuelve:
- el rango `[inicio, fin)` de un día y de una semana (lunes a domingo) en la zona del consultorio, como instantes ISO para `GET /turnos`;
- convertir "día + hora local" del consultorio en un instante ISO con su desplazamiento (`2026-11-30T10:00:00-03:00`), calculando el offset con `Intl.DateTimeFormat(..., { timeZone, timeZoneName: 'longOffset' })`;
- formatear horas y fechas (`es-AR`) en esa zona.
- *Alternativa descartada*: `date-fns-tz` o `Temporal`. La primera suma una dependencia para pocas funciones; `Temporal` todavía no está en todos los navegadores objetivo. Las funciones propias se testean con zonas fijas.

### D2 — Zona horaria desde `GET /consultorio`
El backend agrega `zonaHoraria` a la respuesta de `GET /api/v1/consultorio` (de `ZonaHorariaConsultorio`) y la ignora en el `PUT`. Es el único cambio de backend.
- *Alternativa descartada*: usar la zona del navegador. Rompe la regla de mostrar en la zona del consultorio y falla con una computadora mal configurada.
- *Alternativa descartada*: un endpoint nuevo solo para la zona. `GET /consultorio` ya es la configuración que la agenda necesita (límite de confirmación).

### D3 — Estado de la vista en la URL
`/agenda?vista=dia|semana&fecha=AAAA-MM-DD` (valores por defecto: `dia` y hoy en la zona del consultorio). Recargar o compartir el enlace muestra lo mismo, y el botón Atrás del navegador vuelve a la fecha anterior.

### D4 — Vista día: grilla de tiempo en escritorio, lista en celular
- **Escritorio (≥ 761 px)**: una columna de horas de 15 minutos desde el primer horario relevante (inicio de franja o primer turno, redondeado a la hora) hasta el último. Cada turno se posiciona con CSS Grid (`grid-row`) usando variables CSS (`--inicio`, `--filas`) asignadas desde React (CSSOM, compatible con la CSP). Los sobreturnos que se superponen se dibujan en una segunda columna de la misma fila.
- **Celular**: lista ordenada por hora con tarjetas; las franjas se muestran como encabezados ("Atención 9:00 a 13:00").
- *Alternativa descartada*: una librería de calendario (FullCalendar, react-big-calendar). Traen su propio estilo, que habría que pisar con los tokens, y estilos en línea que choca con la CSP.

### D5 — Vista semana
Siete columnas (lunes a domingo) con las tarjetas de cada día ordenadas por hora, sin grilla de tiempo; el día de hoy se resalta. En celular se elige el día con una tira de siete botones y se ve la lista de ese día. Elegir un día lleva a su vista día.

### D6 — Carga de datos con un hook propio
`useAgenda(rango)` pide en paralelo turnos, franjas, prestaciones activas y consultorio (estos tres una sola vez por montaje), expone `cargando`, `error` y `recargar()`, y recarga al volver a la pestaña (`visibilitychange`).
- *Alternativa descartada*: TanStack Query. Sería útil con más pantallas; hoy alcanza un hook y se evita otra dependencia. Si C-26 o C-38 lo necesitan, se reevalúa.

### D7 — Diálogos y panel
- **Dar turno**: diálogo modal con secciones Paciente (buscador con resultados navegables por teclado y "Nuevo paciente", que despliega el alta en el mismo diálogo), Prestación (selector con duración), Día, Hora (`input type="time"` con `step=300`) y Duración (selector de la duración de la prestación a 480 en pasos de 5). Abrir el diálogo desde un hueco de la grilla precarga día y hora.
- **Detalle del turno**: panel lateral modal (sobre `<dialog>`) en escritorio y hoja inferior en celular, con las acciones permitidas por el estado (RN-TU-01).
- **Reprogramar** y **Cancelar** son diálogos propios abiertos desde el detalle; cancelar pide quién cancela (radios Paciente / Consultorio) y motivo (área de texto, 200 caracteres, con contador).
- Nuevos componentes base en `src/shared/ui`: `Selector`, `AreaDeTexto`, `Panel` y `GrupoDeOpciones` (radios). La etiqueta de estado (`EtiquetaDeTurno`) vive en `features/agenda`.

### D8 — Rechazos de la API
- `409 turno-superpuesto`: alerta dentro del diálogo que dice con qué turno choca (calculado con los turnos ya cargados del día) con "Elegir otro horario" (vuelve el foco a la hora) y, solo al dar turno, "Dar como sobreturno". Ese botón es la confirmación explícita de RN-AG-07: reenvía con `sobreturno: true`, sin un segundo diálogo.
- `409 turno-modificado` y `409 transicion-invalida`: se cierra el diálogo, se muestra un aviso y se recarga la agenda.
- `400` con `errores`: cada mensaje junto a su campo (los campos de la API se mapean a los del formulario).

### D9 — "Sin confirmar"
Un turno `RESERVADO` se muestra "Sin confirmar" si `inicio - ahora ≤ confirmacionLimiteHoras` (con el reloj inyectable, para testear). Tokens: `--turno-sinconf-*` para ese caso y `--turno-<estado>-*` para el resto, siempre con ícono y texto.

### D10 — Tests
- Unitarios de `fechas.ts` con zonas fijas (Buenos Aires y una con horario de verano, para probar el cálculo de offset).
- Componentes y flujos con Testing Library y la API falsa de C-07: vista día y semana, navegación y URL, dar turno (existente, nuevo, superpuesto y sobreturno), confirmar, reprogramar (incluido el superpuesto), cancelar (con y sin motivo) y `409 turno-modificado`.
- Backend: test del controlador para `zonaHoraria` en `GET` y que el `PUT` la ignore.

## Risks / Trade-offs

- [Grilla con estilos asignados por JavaScript bajo la CSP] → Es CSSOM, no atributos `style`; se verifica en `docker compose` que la consola no muestre violaciones de CSP.
- [Agenda desactualizada si otra persona da un turno] → Recarga al volver a la pestaña y tras cada acción; un alta en un horario tomado responde `409` igual.
- [Cálculo de offset propio] → Tests con zonas con y sin horario de verano; la zona del consultorio argentino no tiene horario de verano hoy.
- [Reprogramar no ofrece sobreturno] → Limitación de la API de C-10; queda anotada para C-38 si los usuarios lo piden.

## Migration Plan

Entra con la imagen única. El campo nuevo de `GET /consultorio` es aditivo; no hay migraciones ni variables nuevas.

## UI Mockups

- Design system revision: 1
- Decision: yes — 2026-11-26
- Mockups: `openspec/changes/frontend-agenda/mockups/`
- Approval: approved (2026-11-26, by user)
- Preview: rendered (2026-11-26) — `mockups/previews/` (10 pantallas a 390x844 y 1440x900)
- Visual verify: passed con diferencias menores (2026-11-26) — `mockups/verify/` (vista día y semana, día vacío, dar turno, horario ocupado, paciente nuevo, detalle, cancelar, reprogramar y turno modificado, a 390x844 y 1440x900)
  - Recorrido con `docker compose` (imagen única, CSP real) y base desechable con datos ficticios: alta con búsqueda, `409` superpuesto con el turno que choca y sobreturno creado, error real de teléfono al dar de alta un paciente, confirmar, reprogramar (el original queda cancelado), cancelar con motivo, "Cancelados visibles" y `409 turno-modificado` provocado desde otra sesión (aviso y agenda actualizada).
  - Consola: solo los `409` y `400` provocados a propósito; sin errores de CSP (las posiciones de la grilla con variables CSS asignadas desde JavaScript funcionan con `style-src 'self'`).
  - Menor — el buscador de pacientes no lleva el ícono de lupa y los resultados son una lista de botones en lugar de un listbox.
  - Menor — en un grupo de turnos superpuestos, cada tarjeta ocupa el alto de todo el grupo (en el mockup también).
  - Menor — los teléfonos se muestran en E.164 (`+5491155550122`), como los guarda la API; el formato local queda para C-26.
  - Menor — el aviso de turno modificado no separa título y texto.
