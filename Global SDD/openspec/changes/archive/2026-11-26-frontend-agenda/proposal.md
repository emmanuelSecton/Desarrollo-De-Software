# Proposal — frontend-agenda (C-13, recortado para la demo)

> Governance: **MEDIO** (lógica de negocio en pantalla). Es un cambio visual: por la política `system-and-mockups`, se implementa recién cuando el usuario apruebe los mockups.

## Why

La demo tiene que ser en pantalla: pacientes creados y un turno dado desde la agenda. C-10 ya garantiza en la API que dos turnos activos no se superponen, y C-07 dejó la SPA con login, sesión y la sección Agenda vacía. Falta la pantalla donde la secretaria y el odontólogo ven y manejan los turnos.

## What Changes

- **Agenda en `/agenda`** (reemplaza el estado vacío de C-07):
  - **Vista día** (por defecto) y **vista semana**, con navegación anterior / hoy / siguiente.
  - Cada turno muestra hora, paciente, prestación y estado con **color, ícono y texto**; los sobreturnos llevan su marca. Un turno `RESERVADO` cuyo inicio está dentro del límite de confirmación del consultorio se marca "Sin confirmar".
  - Las **franjas de atención** del día se ven sobre la grilla; lo que queda fuera se ve atenuado.
  - En celular, la vista día es una lista; la semana se recorre día por día.
- **Dar un turno** (diálogo):
  - Buscar el paciente por DNI, nombre o teléfono, o **darlo de alta en el momento** (con el consentimiento obligatorio de C-09).
  - Elegir prestación (duración por defecto la de la prestación, extensible en bloques de 5 minutos), día y hora.
  - Si el horario se superpone (`409 turno-superpuesto`), mostrar un mensaje claro y ofrecer **darlo como sobreturno** con confirmación explícita (RN-AG-07).
- **Detalle de un turno** (panel lateral en escritorio, hoja inferior en celular) con las acciones que su estado permite (RN-TU-01):
  - **Confirmar** manualmente.
  - **Reprogramar** a otro día y hora (con el mismo manejo de superposición).
  - **Cancelar** con quién cancela (paciente o consultorio) y motivo obligatorio.
- **Ediciones simultáneas**: ante `409 turno-modificado` la pantalla avisa que otra persona cambió el turno, recarga la agenda y no aplica el cambio.
- **Backend (mínimo)**: `GET /api/v1/consultorio` agrega `zonaHoraria` (solo lectura, la de `APP_TIMEZONE`) para que la agenda muestre y envíe horarios en la zona del consultorio y no en la del navegador.

Fuera de alcance (pasan a C-38): bloqueos, marcar atendido o ausente, marca "paciente con ausencias" y mover por arrastre. Las notificaciones al paciente al mover o cancelar llegan con C-14 a C-16. La ficha del paciente es C-26.

## Capabilities

### New Capabilities
- `frontend-agenda`: cómo el personal ve la agenda y da, confirma, reprograma y cancela turnos desde la pantalla, con los rechazos de la API mostrados de forma clara.

### Modified Capabilities
- `configuracion-consultorio`: la consulta de los datos del consultorio devuelve también su zona horaria (solo lectura).

## Impact

- **Frontend**: `src/features/agenda` (vistas, diálogos y su lógica de fechas), `src/shared/api-client` (tipos de turnos, pacientes, prestaciones y horarios), `src/shared/formatos` (fechas y horas en la zona del consultorio). Nuevos componentes base que la agenda necesita y que sirven a C-26 y C-38: selector, área de texto, panel lateral y etiqueta de estado.
- **Backend**: `ConsultorioController` agrega el campo `zonaHoraria` a la respuesta; sin migraciones ni variables nuevas.
- **Dependencias**: ninguna nueva prevista; las fechas se calculan con `Intl` y la zona IANA del consultorio.
- **Mockups**: `openspec/changes/frontend-agenda/mockups/`, a 390x844 y 1440x900.
- **Desbloquea**: la demo en pantalla (con un script de datos ficticios) y C-38.
