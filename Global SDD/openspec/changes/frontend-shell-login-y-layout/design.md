# Design — frontend-shell-login-y-layout (C-07)

## Context

- La SPA (React 19, Vite 8, TypeScript 5.9, Vitest) es un placeholder; ya existen las carpetas `src/features`, `src/pages`, `src/shared/{ui,api-client,formatos}` y un test que impide valores visuales fijos fuera de los tokens.
- Spring Boot sirve la SPA desde el mismo origen y reenvía a `index.html` las rutas que no son de la API (`SpaForwardController`), así que el ruteo del cliente funciona al recargar.
- C-03 dejó: `GET /auth/csrf`, `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/password`; cookie `XSRF-TOKEN` legible y cabecera `X-XSRF-TOKEN`; `401 /problemas/no-autenticado`, `401 /problemas/credenciales-invalidas`, `403 /problemas/cambio-password-requerido` y `403 /problemas/acceso-denegado`; sesión de 30 minutos.
- La CSP de C-03 es estricta (`script-src 'self'; style-src 'self'`): no se pueden usar estilos ni scripts en línea.
- El sistema de diseño (revisión 1, aprobada) deja abierta la librería de componentes.

## Goals / Non-Goals

**Goals:**
- Que C-13 y las pantallas siguientes solo tengan que agregar una ruta y usar el cliente HTTP y los componentes base.
- Que ninguna pantalla interprete a mano un `ProblemDetail`, la cookie CSRF o un `401`.

**Non-Goals:**
- Librería de componentes con tema (MUI, Mantine): ver D1.
- Estado global más allá de la sesión (no hace falta Redux ni similares todavía).
- Modo oscuro (fuera de la revisión 1).

## Decisions

### D1 — Componentes propios con tokens y CSS Modules
Se resuelve la decisión abierta del sistema de diseño: componentes propios en `src/shared/ui` (`Boton`, `Campo`, `Alerta`, `Aviso`, `Dialogo`, `EstadoVacio`, `Cargando`), con estilos en CSS Modules que solo usan variables de `tokens.css`. Los estilos van en archivos `.css`, así que cumplen la CSP (`style-src 'self'`).
- *Alternativa descartada*: MUI o Mantine. Traen su propio sistema de temas, que habría que mantener alineado con `tokens.css`, y suman peso al bundle.
- El diálogo usa el elemento nativo `<dialog>` con `showModal()`, que ya resuelve el foco atrapado y la tecla Escape.

### D2 — Rutas con `react-router`
`react-router` 7 en modo declarativo (`BrowserRouter` + `Routes`):

| Ruta | Pantalla | Guard |
|---|---|---|
| `/login` | Login | solo sin sesión |
| `/cambiar-contrasena` | Cambio obligatorio | sesión con cambio pendiente |
| `/agenda` | Agenda (estado vacío hasta C-13) | sesión |
| `/pacientes` | Pacientes (estado vacío hasta C-26) | sesión |
| `/configuracion` | Configuración (estado vacío hasta C-08) | rol `ODONTOLOGO` |
| `/` | redirige a `/agenda` | sesión |
| `*` | No encontrada | — |

### D3 — Cliente HTTP
`src/shared/api-client/cliente.ts` envuelve `fetch` con `credentials: 'same-origin'`:
- En `POST`, `PUT`, `PATCH` y `DELETE` agrega `X-XSRF-TOKEN` con la cookie `XSRF-TOKEN`; si no está, llama primero a `GET /auth/csrf`.
- Convierte `application/problem+json` en `ErrorDeApi { status, tipo, titulo, detalle, errores[] }`.
- `401` con `type` `/problemas/no-autenticado` dispara un evento de "sesión vencida" que el contexto de sesión escucha. El `401` del login no lo dispara, porque es un rechazo de credenciales.
- `403` con `type` `/problemas/cambio-password-requerido` lleva a `/cambiar-contrasena`.

### D4 — Contexto de sesión y guards
`src/shared/auth`: `ProveedorDeSesion` carga `/auth/me` al iniciar y expone `usuario`, `ingresar`, `salir` y `cambiarContrasena`. `RequiereSesion` guarda la ruta pedida (`?volver=`) antes de mandar al login; `RequiereRol` muestra la página de acceso denegado. La UI oculta lo que el rol no puede usar, pero el control real sigue en la API (C-03).

### D5 — Aviso de inactividad
Un temporizador cuenta 28 minutos sin eventos de teclado, puntero o navegación. Al cumplirse abre un diálogo "Tu sesión está por vencer" con "Seguir conectado" (llama a `/auth/me`, que renueva la sesión del servidor) y "Salir". Si pasan los 30 minutos, el próximo `401` lleva al login con "Tu sesión venció. Volvé a ingresar." El temporizador recibe el reloj por parámetro para testearlo sin esperas.

### D6 — Íconos
`lucide-react` (trazo lineal, como pide `brand.md`), importando solo los íconos usados.

### D7 — Tests
- Vitest y Testing Library: cliente HTTP (CSRF, conversión de errores, `401`), guards, temporizador de inactividad y formularios (etiquetas, `aria-invalid`, `role="alert"`).
- Se mantiene el test que prohíbe colores, espaciados y tipografías fijos fuera de `tokens.css`.

## Risks / Trade-offs

- [Componentes propios: más código que con una librería] → Son pocos y simples; el `<dialog>` nativo cubre lo más difícil (foco y Escape).
- [El aviso de inactividad cuenta en el navegador, no en el servidor] → Es solo un aviso; la fuente de verdad es el `401` del servidor.
- [Secciones vacías en la demo] → Muestran "Disponible próximamente" hasta que lleguen C-13, C-26 y C-08.

## Migration Plan

Solo frontend: entra con la imagen única. Sin cambios de base ni de variables.

## UI Mockups

- Design system revision: 1
- Decision: yes — 2026-11-26
- Mockups: `openspec/changes/frontend-shell-login-y-layout/mockups/`
- Approval: approved (2026-11-26, by user)
- Preview: rendered (2026-11-26) — `mockups/previews/` (9 pantallas a 390x844 y 1440x900)
- Visual verify: passed con diferencias menores (2026-11-26) — `mockups/verify/` (9 pantallas a 390x844 y 1440x900)
  - Recorrido con `docker compose` (imagen única, CSP real) y base desechable: login, login rechazado, cambio obligatorio con error y exitoso, app del odontólogo con el aviso "Contraseña actualizada", 404, salir, app de la secretaria, 403 en `/configuracion`. El aviso de inactividad se verificó con Vite (`VITE_AVISO_INACTIVIDAD_SEG=15`) sobre el mismo backend: diálogo, foco en "Seguir conectado" y, con la sesión cerrada en el servidor, vuelta al login con "Tu sesión venció.".
  - Consola: solo los `401` esperados (carga sin sesión y login rechazado); sin errores de CSP.
  - Menor — login: no muestra "¿Olvidaste tu contraseña?"; las pantallas de recuperación son de C-08, que agrega el enlace.
  - Menor — barra superior: el rol se muestra como área ("Odontología", "Secretaría") en lugar de "Odontóloga"/"Secretaria", porque el usuario no tiene género en los datos.
  - Menor — el círculo del ícono de las páginas de error casi no contrasta con el fondo (igual que en el mockup: `--color-brand-50` sobre `--color-bg`).
