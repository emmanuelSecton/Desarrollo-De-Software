# Proposal — agenda-turnos-core (C-10)

> Governance: **CRÍTICO** según `CHANGES.md` (la garantía anti-solapamiento es el corazón del producto). Solo planificación hasta que el usuario lo apruebe.

## Why

Hoy el sistema tiene pacientes (C-09) y prestaciones y horarios (C-05), pero no puede dar un turno. C-10 es el siguiente del camino crítico y la base de todo lo que diferencia al producto: recordatorios (C-14), confirmación por WhatsApp (C-15/C-16), lista de espera y oferta de huecos (C-18, C-37) y reserva online (C-20). Además, es lo mínimo para una demo con pacientes y turnos.

## What Changes

- **Migración `V7`**: tabla `turno` con paciente, prestación, inicio y fin en UTC, estado, origen, marca de sobreturno, vínculo al turno reprogramado, token público de 256 bits, datos de cancelación y versión para el bloqueo optimista.
- **Garantía anti-solapamiento en la base** (RN-AG-01): una exclusion constraint impide dos turnos activos (`RESERVADO` o `CONFIRMADO`) superpuestos, salvo los marcados como sobreturno (RN-AG-07). La violación se traduce a `409`, nunca se valida solo en Java.
- **Máquina de estados** de RN-TU-01: `RESERVADO → CONFIRMADO | CANCELADO | ATENDIDO | AUSENTE`, `CONFIRMADO → CANCELADO | ATENDIDO | AUSENTE`; los demás son finales.
- **Endpoints internos** bajo `/api/v1/turnos` (odontólogo y secretaria):
  - `GET /turnos?desde&hasta`: turnos de un rango (hasta 31 días), con paciente y prestación.
  - `GET /turnos/{id}`.
  - `POST /turnos`: crea un turno para un paciente activo y una prestación activa; duración = la de la prestación, extensible en bloques de 5 minutos (RN-AG-02). Con `sobreturno = true` se permite la superposición.
  - `PUT /turnos/{id}`: reprograma (RN-TU-03): cancela el original y crea uno nuevo vinculado, en la misma transacción, sin dejar dos turnos activos.
  - `POST /turnos/{id}/cancelar`: pide motivo y quién cancela (paciente o consultorio).
  - `POST /turnos/{id}/confirmar`: el personal confirma, por ejemplo, después de una llamada.
- **Bloqueo optimista**: las modificaciones envían la `version` que leyeron; si otro la cambió antes, `409`.
- **Eventos de dominio** `TurnoCreado`, `TurnoCancelado`, `TurnoReprogramado` y `TurnoEstadoCambiado`, publicados para que C-14, C-15 y C-18 reaccionen después del commit.
- **Auditoría** `CREAR_TURNO`, `MODIFICAR_ENTIDAD` y `ANULAR_ENTIDAD`, sin datos personales.

Fuera de alcance:
- Marcar atendido o ausente y el contador de ausencias (C-12).
- Bloqueos y cálculo de disponibilidad (C-11).
- Notificar al paciente por WhatsApp o email (C-14 a C-16, que escuchan los eventos de este change).
- Reserva y gestión pública por enlace (C-20 y C-21).
- Pantallas de agenda (C-13, con mockup).
- Datos de demo (ver Impact).

## Capabilities

### New Capabilities
- `agenda-turnos`: creación, consulta, reprogramación, cancelación y confirmación de turnos internos, con garantía de no superposición en la base, estados válidos, bloqueo optimista, eventos y auditoría.

### Modified Capabilities
_(ninguna)_

## Impact

- **Backend**: módulo `agenda`. Usa `RegistroDePacientes` (`pacientes.application`) para validar al paciente y `ConfiguracionDeAgenda` para la prestación. La consulta del listado junta `turno`, `paciente` y `prestacion` en SQL (ver design D8).
- **Base**: migración `V7__turnos.sql`; el rol runtime recibe `SELECT, INSERT, UPDATE` sobre `turno`, sin `DELETE`.
- **Desbloquea**: C-11, C-12, C-14 y, con otros, C-13, C-18 y C-20.
- **Demo**: con este change la demo se puede hacer por API. Los pacientes y turnos ficticios para mostrar se proponen como un paso aparte (un script de datos de demo), no dentro de C-10.
