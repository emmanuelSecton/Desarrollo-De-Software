# Tasks: frontend-agenda (C-13)

Convenciones de apply:
- **Mockups aprobados primero** (`system-and-mockups`): no se implementa ninguna pantalla hasta que `design.md` diga `Approval: approved`. Los mockups aprobados son el criterio de aceptación visual.
- **TDD**: RED → GREEN → REFACTOR con Vitest y Testing Library (frontend) y JUnit con Testcontainers (backend).
- **Solo tokens**: ningún color, espaciado ni tipografía fijos; el test existente lo controla.
- Datos de ejemplo siempre ficticios (SU-07).

## 1. Mockups (antes de cualquier código)

- [x] 1.1 Crear los mockups HTML en `mockups/`: agenda día (con franjas, estados, "Sin confirmar" y sobreturno), agenda semana, día sin turnos, dar turno (búsqueda con resultados), dar turno con paciente nuevo, horario superpuesto con opción de sobreturno, detalle del turno, cancelar, reprogramar y aviso de turno modificado. Verificación: cada archivo importa `design-system/tokens.css` y `brand.js`, y `mockups/README.md` lista pantallas y estados.
- [x] 1.2 Renderizar cada mockup a 390x844 y 1440x900 y guardar las capturas en `mockups/previews/`. Verificación: capturas revisadas y `Preview: rendered` en `design.md`.
- [x] 1.3 Obtener la aprobación del usuario y registrar `Approval: approved` en `design.md` y en `mockups/README.md`. Verificación: aprobación explícita del usuario.

## 2. Backend: zona horaria del consultorio

- [x] 2.1 Agregar `zonaHoraria` a la respuesta de `GET /api/v1/consultorio` e ignorarla en el `PUT` (D2). Verificación: test del controlador con `GET` (devuelve la zona configurada) y `PUT` con otra zona (sigue la configurada); `./gradlew build` en verde.

## 3. Base del frontend

- [x] 3.1 `src/shared/formatos/fechas.ts` (D1): rangos de día y semana, día + hora local → instante con offset, formateo `es-AR`. Verificación: tests con Buenos Aires y con una zona con horario de verano (cambio de hora incluido).
- [x] 3.2 Tipos y llamadas de turnos, pacientes, prestaciones, horarios y consultorio sobre `api()`. Verificación: tests de los parámetros enviados (rango ISO, `sobreturno`, `version`) con la API falsa.
- [x] 3.3 Componentes base `Selector`, `AreaDeTexto`, `Panel` y `GrupoDeOpciones` (D7). Verificación: tests de etiquetas, `aria-invalid`, contador de caracteres y foco del panel.

## 4. Agenda

- [x] 4.1 `useAgenda` y estado en la URL (D3, D6). Verificación: tests de carga, navegación anterior / hoy / siguiente, cambio de vista que actualiza la URL y recarga al volver a la pestaña.
- [x] 4.2 Vista día (grilla en escritorio, lista en celular), franjas, estados con texto e ícono, "Sin confirmar" (D9), sobreturno, "Mostrar cancelados" y estado vacío, según los mockups. Verificación: tests de estados, marca sin confirmar con reloj fijo, cancelados ocultos y nombre accesible de cada turno.
- [x] 4.3 Vista semana (D5), según los mockups. Verificación: tests de los siete días, hoy resaltado y paso a la vista día.

## 5. Acciones

- [x] 5.1 Dar turno: búsqueda de paciente, alta en el momento, prestación, día, hora y duración; superposición y sobreturno confirmado (D8). Verificación: tests de turno para paciente existente, paciente nuevo, errores por campo, `409 turno-superpuesto` y sobreturno.
- [x] 5.2 Detalle del turno con acciones por estado; confirmar. Verificación: tests de acciones visibles por estado y confirmación.
- [x] 5.3 Reprogramar y cancelar (con quién cancela y motivo obligatorio). Verificación: tests de reprogramación exitosa y superpuesta (sin sobreturno), cancelación con y sin motivo.
- [x] 5.4 Manejo de `409 turno-modificado` y `409 transicion-invalida` (D8). Verificación: test con versión vieja que muestra el aviso y recarga.

## 6. Verificación

- [x] 6.1 `npm run lint`, `npm run build`, `npm test` y `./gradlew build` en verde. Verificación: totales en el resumen de apply.
- [x] 6.2 Verificación visual con `docker compose` y base desechable con datos ficticios: vista día y semana, dar turno (existente y nuevo), superposición y sobreturno, confirmar, reprogramar, cancelar y turno modificado desde otra sesión, capturando a 390x844 y 1440x900 en `mockups/verify/` y comparando con los mockups; sin errores de consola ni de CSP. Verificación: diferencias reportadas por prioridad y `Visual verify` registrado en `design.md`.

## Workflow follow-up

- Revisión del PR, merge, sincronizar `openspec/specs/frontend-agenda` y `configuracion-consultorio`, archivar y marcar C-13 en `CHANGES.md`.
- Siguiente: script de datos ficticios para la demo (pacientes y turnos de la semana).
