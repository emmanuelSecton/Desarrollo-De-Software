# Design — agenda-turnos-core (C-10)

## Context

- El módulo `agenda` ya tiene consultorio, franjas y prestaciones (C-05), con `ConfiguracionDeAgenda` como consulta pública y el patrón de controladores con clase base de errores.
- `pacientes` (C-09) expone `RegistroDePacientes.obtener(id)`, que devuelve `PacienteVista` con `activo`.
- `btree_gist` está habilitada desde `V1`; C-05 ya usa una exclusion constraint y traduce el SQLState `23P01` a `409`.
- Regla dura del proyecto: el solapamiento de turnos nunca se valida solo en Java; la garantía es la constraint de PostgreSQL.
- La auditoría (C-02) ya tiene `CREAR_TURNO` en el catálogo y `turno_id` en la whitelist del detalle.
- No hay oyentes de eventos de turnos todavía: los traen C-14 (recordatorios), C-15/C-16 (WhatsApp) y C-18 (lista de espera).

## Goals / Non-Goals

**Goals:**
- Que dos turnos activos superpuestos sean imposibles aunque lleguen pedidos simultáneos, por cualquier camino (API interna hoy, reserva online y oferta de huecos después).
- Que la reprogramación nunca deje dos turnos activos ni pierda el original si el nuevo horario está ocupado.
- Que dos personas editando el mismo turno no se pisen.

**Non-Goals:**
- Validar que el turno caiga dentro del horario de atención: el personal puede dar turnos fuera de franja; la reserva online sí lo exigirá (RN-AG-04, C-20).
- Bloqueos de agenda (C-11): por ahora un turno puede caer sobre un horario que más adelante se bloquee.
- Marcar atendido o ausente (C-12), aunque la máquina de estados ya contempla esas transiciones.

## Decisions

### D1 — Tabla `turno`
`V7__turnos.sql`:

| Columna | Tipo | Notas |
|---|---|---|
| `id` | `UUID PK` | |
| `paciente_id` | `UUID NOT NULL REFERENCES paciente(id)` | índice |
| `prestacion_id` | `UUID NOT NULL REFERENCES prestacion(id)` | |
| `inicio`, `fin` | `TIMESTAMPTZ NOT NULL` | `CHECK (fin > inicio)`; UTC |
| `estado` | `VARCHAR(12) NOT NULL` | `CHECK IN ('RESERVADO','CONFIRMADO','CANCELADO','ATENDIDO','AUSENTE')` |
| `origen` | `VARCHAR(8) NOT NULL` | `CHECK IN ('INTERNO','ONLINE')` |
| `sobreturno` | `BOOLEAN NOT NULL DEFAULT FALSE` | |
| `reprogramado_de` | `UUID REFERENCES turno(id)` | índice único: un turno se reprograma una sola vez |
| `token_publico` | `CHAR(43) NOT NULL UNIQUE` | |
| `sin_confirmar_alertado` | `BOOLEAN NOT NULL DEFAULT FALSE` | lo usa C-14 |
| `google_event_id` | `VARCHAR(255)` | post-MVP |
| `cancelado_por` | `VARCHAR(12)` | `CHECK IN ('PACIENTE','CONSULTORIO')` |
| `motivo_cancelacion` | `VARCHAR(200)` | |
| `creado_at`, `actualizado_at` | `TIMESTAMPTZ NOT NULL` | del `Clock` |
| `version` | `INT NOT NULL DEFAULT 0` | bloqueo optimista |

- `CHECK ((estado = 'CANCELADO') = (cancelado_por IS NOT NULL AND motivo_cancelacion IS NOT NULL))`: un turno cancelado siempre dice quién y por qué.
- **Anti-solapamiento**: `EXCLUDE USING gist (tstzrange(inicio, fin) WITH &&) WHERE (estado IN ('RESERVADO','CONFIRMADO') AND NOT sobreturno)`, tal como en la KB. El rango `[)` acepta turnos contiguos.
- Índices `(inicio)`, `(estado, inicio)` y `(paciente_id)`.
- Grants: `SELECT, INSERT, UPDATE` al runtime, sin `DELETE`.
- `CHECK` en vez de `ENUM` de PostgreSQL, igual que en C-02, C-03 y C-05.

### D2 — Máquina de estados en el dominio
`EstadoTurno` (enum de `agenda.domain`) declara las transiciones de RN-TU-01 con `puedePasarA(destino)`. `Turno.confirmar()`, `cancelar(...)`, y en el futuro `marcarAtendido()` y `marcarAusente()` (C-12), lanzan `TransicionInvalidaException` (`409 /problemas/transicion-invalida`) si no corresponde. Corregir un estado final no se hace en este change: C-12 lo trae con su permiso y auditoría.

### D3 — Reglas del alta
- Paciente: `RegistroDePacientes.obtener(id)`; inexistente o con `activo = false` da `400` en el campo `pacienteId`.
- Prestación: `ConfiguracionDeAgenda.prestacion(id)`; inexistente o inactiva da `400` en `prestacionId`.
- Duración: si no viene, la de la prestación. Si viene, tiene que ser mayor o igual que la de la prestación, múltiplo de 5 y de hasta 480.
- Inicio: múltiplo de 5 minutos, sin segundos, y posterior a `Clock.instant()`.
- El cliente manda `inicio` como ISO-8601 con desplazamiento (por ejemplo `2026-12-01T10:00:00-03:00`); se guarda en UTC y se devuelve con `Z`.

### D4 — Superposición: la decide la base
El servicio inserta y, si PostgreSQL responde `23P01`, traduce a `TurnoSuperpuestoException` (`409 /problemas/turno-superpuesto`, con el detalle "Ese horario se superpone con otro turno. Si querés darlo igual, crealo como sobreturno."). No hay una consulta previa de disponibilidad: sería una carrera entre la consulta y el insert, y la constraint ya resuelve las altas simultáneas.

### D5 — Bloqueo optimista
Las operaciones de cambio reciben la `version` leída. El `UPDATE` es `... SET version = version + 1 WHERE id = :id AND version = :version`; si afecta 0 filas y el turno existe, es `TurnoModificadoException` (`409 /problemas/turno-modificado`). No se usa `SELECT ... FOR UPDATE`: con la versión alcanza y no se retienen bloqueos.

### D6 — Reprogramación en una transacción
1. Cancela el original con `cancelado_por = CONSULTORIO` y motivo "Reprogramado", con chequeo de versión.
2. Inserta el nuevo con `reprogramado_de = original`, el mismo paciente, la misma prestación, el mismo origen y la duración pedida (o la original).

Como la cancelación va primero, el nuevo turno puede pisar el horario del original (mover 15 minutos). Si el insert choca con otro turno, la excepción revierte la transacción y el original queda activo e intacto. El estado del nuevo es `RESERVADO`: se vuelve a pedir confirmación porque cambió el horario.

### D7 — Token público
32 bytes de `SecureRandom` en Base64 URL-safe (43 caracteres), generados por `agenda.domain.TokenPublico`. Se guarda en claro porque los mensajes de C-14 y C-15 tienen que incluir el enlace días después de creado el turno; queda anotado como riesgo. Vence al terminar el turno: lo valida C-21.

### D8 — Listado con un JOIN de solo lectura
`GET /turnos` lee con una sola consulta `turno JOIN paciente JOIN prestacion` para traer nombre, apellido y prestación sin N+1. Es una lectura que cruza tablas de otro módulo: las escrituras siempre pasan por las APIs (`RegistroDePacientes`, `ConfiguracionDeAgenda`).
- *Alternativa descartada*: armar el listado llamando a `RegistroDePacientes` por cada turno. Con 30 turnos por día son 30 consultas más.

### D9 — Eventos de dominio
Records en `agenda.application.evento`: `TurnoCreado(turnoId, pacienteId, inicio, origen)`, `TurnoCancelado(turnoId, pacienteId, canceladoPor)`, `TurnoReprogramado(originalId, nuevoId, pacienteId)` y `TurnoEstadoCambiado(turnoId, desde, hacia)`. Se publican con `ApplicationEventPublisher` dentro de la transacción; los oyentes futuros usan `@TransactionalEventListener(AFTER_COMMIT)`, así que no se enteran de un turno que no se guardó.

### D10 — Auditoría
| Operación | `accion` | `detalle` |
|---|---|---|
| Alta | `CREAR_TURNO` | `{"paciente_id": …}` |
| Confirmación | `MODIFICAR_ENTIDAD` | `{"referencia": "confirmacion"}` |
| Reprogramación | `MODIFICAR_ENTIDAD` sobre el original y `CREAR_TURNO` sobre el nuevo | `{"turno_id": <nuevo>, "referencia": "reprogramacion"}` en el original |
| Cancelación | `ANULAR_ENTIDAD` | `{"motivo_codigo": "PACIENTE" \| "CONSULTORIO"}` |

Siempre con `entidad = 'TURNO'`, el id del turno y el usuario de la sesión. El texto del motivo no se audita: puede tener datos de salud.

### D11 — API y permisos
`TurnosController` (`agenda.api`, extiende la clase base de errores de C-05) con `@PreAuthorize(Permisos.PERSONAL)` en todos los métodos: la matriz da CRUD de turnos a ambos roles. `version` viaja en el cuerpo de `PUT`, `cancelar` y `confirmar`.

### D12 — Tests
- Testcontainers para la constraint (superpuesto, contiguo, cancelado, sobreturno), las transiciones, el bloqueo optimista y la reprogramación.
- Concurrencia: dos hilos con un `CountDownLatch` insertan turnos superpuestos; uno responde `TurnoSuperpuestoException`.
- Un oyente de prueba `AFTER_COMMIT` verifica que los eventos llegan solo tras el commit.
- Los turnos de los tests usan fechas futuras distintas por test, para no chocar en la base compartida.

## Risks / Trade-offs

- [El token público se guarda en claro] → Los mensajes lo necesitan después; la base ya contiene datos personales más sensibles y su acceso está restringido. Si se quiere guardar solo el hash, habría que guardar el enlace en cada notificación (C-14).
- [El JOIN del listado acopla `agenda` a las tablas de `pacientes`] → Es solo lectura y está en un único repositorio de consulta; si `paciente` cambia, el test del listado falla.
- [Un turno puede quedar fuera del horario de atención] → Es intencional para el personal; C-13 lo muestra distinto y C-20 lo impide en la reserva online.
- [Sobreturnos: PA-06 sigue abierta] → Se implementan según RN-AG-07 y se pueden ocultar en la UI si las entrevistas dicen que no hacen falta.

## Migration Plan

1. `V7__turnos.sql` se aplica con el migrator. Es aditiva.
2. **Rollback**: revertir la imagen; la tabla puede quedar. Cualquier corrección va en una migración nueva.

## UI Mockups

- Design system revision: 1
- Decision: no (no UI) — 2026-11-26. La vista de agenda y el alta de turnos en pantalla son de C-13, con su propio mockup.
