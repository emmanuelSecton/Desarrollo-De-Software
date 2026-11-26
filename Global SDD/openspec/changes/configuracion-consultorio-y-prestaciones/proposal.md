# Proposal — configuracion-consultorio-y-prestaciones (C-05)

> Governance: **MEDIO** (lógica de negocio). Se implementa con puntos de control y las decisiones se muestran al usuario; igual que en los changes anteriores, se espera su aprobación antes de escribir código.

## Why

La agenda (C-10) necesita saber cuánto dura cada prestación y en qué franjas atiende el consultorio, y los recordatorios (C-14) necesitan las horas de recordatorio, confirmación y cancelación. Hoy nada de eso existe. C-05 es requisito de C-10, el siguiente del camino crítico, y cubre la parte de configuración de US-001.

## What Changes

- **Migración `V6`** con tres tablas:
  - `consultorio`: una sola fila por instancia, con datos del consultorio, `slug_reserva` y los parámetros de recordatorio (48 h), límite de confirmación (24 h), cancelación mínima (24 h) y vencimiento de ofertas (30 min).
  - `horario_atencion`: franjas por día de la semana, sin superposiciones dentro de un mismo día y en múltiplos de 5 minutos.
  - `prestacion`: nombre, duración en múltiplos de 5 (RN-AG-02), precio de referencia, `reservable_online` y baja lógica.
- **Datos iniciales en la misma migración**: el consultorio con valores provisorios que el odontólogo completa, horarios de lunes a viernes 9 a 13 y 15 a 19, y las seis prestaciones de ejemplo de la KB.
- **Endpoints** bajo `/api/v1` (todo el personal lee, solo el odontólogo modifica):
  - `GET` y `PUT /consultorio`.
  - `GET`, `POST`, `PUT` y `DELETE /horarios`.
  - `GET`, `POST` y `PUT /prestaciones`, más `DELETE /prestaciones/{id}` como baja lógica y `POST /prestaciones/{id}/reactivar`.
- **Validaciones** de los parámetros (por ejemplo, el límite de confirmación tiene que ser menor que el horario del recordatorio), en Java y en la base.
- **Auditoría** de cada cambio de configuración.
- El módulo `agenda` expone una consulta de configuración para C-10, C-14 y C-20.

Fuera de alcance: pantallas del asistente y del ABM (C-08, con mockup), endpoints públicos de prestaciones y disponibilidad (C-20), bloqueos de agenda (C-11), cálculo de disponibilidad (C-10).

## Capabilities

### New Capabilities
- `configuracion-consultorio`: datos del consultorio y parámetros de notificación, horarios de atención y catálogo de prestaciones, con sus reglas y permisos.

### Modified Capabilities
_(ninguna)_

## Impact

- **Backend**: módulo `agenda` (api, application, domain, infrastructure). No hay un módulo `configuracion`: horarios y prestaciones son la base de la agenda.
- **Base**: migración `V6__configuracion_consultorio.sql`. El rol runtime recibe `SELECT, UPDATE` sobre `consultorio`, `SELECT, INSERT, UPDATE, DELETE` sobre `horario_atencion` y `SELECT, INSERT, UPDATE` sobre `prestacion`, sin `DELETE`.
- **Frontend**: ninguno; C-08 consume los endpoints.
- **Desbloquea**: C-10 (junto con C-09, ya hecho) y, con C-04, C-06 y C-07, el C-08.
