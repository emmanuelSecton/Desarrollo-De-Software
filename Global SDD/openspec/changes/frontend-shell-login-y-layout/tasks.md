# Tasks: frontend-shell-login-y-layout (C-07)

Convenciones de apply:
- **Mockups aprobados primero** (`system-and-mockups`): no se implementa ninguna pantalla hasta que `design.md` diga `Approval: approved`. Los mockups aprobados son el criterio de aceptación visual.
- **TDD**: RED → GREEN → REFACTOR con Vitest y Testing Library.
- **Solo tokens**: ningún color, espaciado ni tipografía fijos; el test existente lo controla.

## 1. Mockups (antes de cualquier código)

- [x] 1.1 Crear los mockups HTML en `mockups/`: login (normal y con error), cambio de contraseña obligatorio, app del odontólogo, app de la secretaria, aviso de sesión por vencer, login con sesión vencida, 403 y 404. Verificación: cada archivo importa `design-system/tokens.css` y `brand.js`, y `mockups/README.md` lista pantallas y estados.
- [x] 1.2 Renderizar cada mockup a 390x844 y 1440x900 y guardar las capturas en `mockups/previews/`. Verificación: capturas revisadas y `Preview: rendered` en `design.md`.
- [x] 1.3 Obtener la aprobación del usuario y registrar `Approval: approved` en `design.md` y en `mockups/README.md`. Verificación: aprobación explícita del usuario.

## 2. Base de la SPA

- [x] 2.1 Agregar `react-router` y `lucide-react` con versión fija. Verificación: `npm run build` y `npm test` en verde.
- [x] 2.2 Implementar el cliente HTTP (D3). Verificación: tests de CSRF en métodos que modifican, conversión de `ProblemDetail` con errores por campo, evento de sesión vencida en `401` y redirección en `403` de cambio pendiente.
- [x] 2.3 Implementar el contexto de sesión y los guards (D4). Verificación: tests de carga con y sin sesión, ruta pedida conservada, guard de rol y cambio pendiente.

## 3. Pantallas

- [x] 3.1 Componentes base de `src/shared/ui` (D1). Verificación: tests de accesibilidad (etiquetas, `aria-invalid`, `role="alert"`, foco del diálogo).
- [x] 3.2 Login y cambio de contraseña obligatorio según los mockups. Verificación: tests de ingreso correcto, `401` genérico con foco en el email, campos vacíos, cambio exitoso y errores por campo.
- [x] 3.3 Estructura de la app con navegación por rol, estados vacíos de las secciones, 403 y 404 según los mockups. Verificación: tests de menú por rol y de rutas restringidas.
- [x] 3.4 Aviso de inactividad (D5). Verificación: test con reloj falso: aviso a los 28 minutos, "Seguir conectado" renueva y "Salir" cierra la sesión.

## 4. Verificación

- [x] 4.1 `npm run lint`, `npm run build`, `npm test` y `./gradlew build` en verde. Verificación: totales en el resumen de apply.
- [x] 4.2 Verificación visual con `docker compose`: login real, cambio obligatorio, navegación como odontólogo y como secretaria, 403, 404 y aviso de inactividad (acortado por configuración de prueba), capturando a 390x844 y 1440x900 en `mockups/verify/` y comparando con los mockups; sin errores de consola ni de CSP. Verificación: diferencias reportadas por prioridad y `Visual verify` registrado en `design.md`.

## Workflow follow-up

- Revisión del PR, merge, sincronizar `openspec/specs/frontend-shell`, archivar y marcar C-07 en `CHANGES.md`.
- Siguiente: C-13 recortado (agenda para la demo), con su propio mockup.
