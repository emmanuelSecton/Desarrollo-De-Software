# Proposal — pacientes-core (C-09)

> Governance: **ALTO** (datos personales de pacientes, Ley 25.326). Se propone y se espera la revisión del usuario antes de escribir código.

## Why

La agenda (C-10), la historia clínica (C-23), la caja (C-28) y la reserva online (C-20) necesitan un paciente al cual asociar turnos, entradas y cobros. C-09 está en el camino crítico (C-03 → **C-09** → C-10 …) y es la primera vez que el sistema guarda datos personales de pacientes, así que tiene que nacer con consentimiento registrado, minimización y trazabilidad (RN-DP-01, RN-GL-01).

## What Changes

- Nueva tabla `paciente` (migración `V4`) con DNI único, nombre, apellido, fecha de nacimiento, celular en formato E.164, email, consentimiento de datos con fecha y versión de la política, consentimiento de WhatsApp con fecha, contador de ausencias y baja lógica. Nunca se borra físicamente (RN-HC-03).
- Endpoints bajo `/api/v1/pacientes` para el personal interno:
  - `GET /pacientes?q=` busca por DNI, nombre, apellido o teléfono, sin distinguir mayúsculas ni acentos, paginado.
  - `POST /pacientes` da de alta; si el DNI ya existe responde `409` con el id del paciente existente.
  - `GET /pacientes/{id}` y `PUT /pacientes/{id}` consultan y modifican datos personales y de contacto.
  - `DELETE /pacientes/{id}` hace la **baja lógica** (solo odontólogo) y `POST /pacientes/{id}/reactivar` la revierte (solo odontólogo).
- Operación interna "obtener o crear por DNI" para C-10 y C-20: si el DNI existe, devuelve ese paciente **sin modificar sus datos**; si no existe, lo crea.
- Normalización de teléfonos argentinos a E.164 (acepta `011 15-…`, `+54 9 …`, con o sin espacios y guiones).
- El consentimiento de datos es obligatorio en toda alta; el de WhatsApp es opcional y queda con su fecha.
- Auditoría de alta, modificación, baja y reactivación (RN-GL-01), sin datos personales en el detalle.
- RBAC: odontólogo y secretaria crean, leen y modifican; solo el odontólogo da de baja o reactiva (03_actores_y_roles.md).

Fuera de alcance: historia clínica (C-23), ficha completa con turnos y saldo (C-26), reserva pública (C-20, que usa la operación "obtener o crear"), derechos de acceso, rectificación y supresión del paciente (RN-DP-02, proceso posterior), exportación (C-33), pantallas (C-08 y C-26, con mockup).

## Capabilities

### New Capabilities
- `pacientes`: alta, búsqueda, consulta, modificación y baja lógica de pacientes, con DNI único, teléfono E.164, consentimiento registrado y auditoría.

### Modified Capabilities
_(ninguna)_

## Impact

- **Backend**: módulo `pacientes` (api, application, domain, infrastructure). Su capa `application` expone la operación "obtener o crear por DNI" para otros módulos.
- **Base**: migración `V4__pacientes.sql` (habilita la extensión `unaccent`); el rol runtime recibe `SELECT, INSERT, UPDATE` sobre `paciente`, sin `DELETE`.
- **Dependencias**: `com.googlecode.libphonenumber:libphonenumber` para normalizar teléfonos.
- **Frontend**: ninguno en este change.
- **Desbloquea**: C-10, C-23 y, junto con C-05, C-28.
