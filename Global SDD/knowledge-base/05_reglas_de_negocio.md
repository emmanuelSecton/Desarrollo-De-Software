# Reglas de Negocio

Cada regla tiene un código único `RN-{DOMINIO}-{NN}`.

## Dominio: Autenticación y seguridad (RN-SEG)

- **RN-SEG-01**: Solo odontólogo y secretaria inician sesión; el paciente no tiene cuenta (DD-05).
- **RN-SEG-02**: La secretaria no accede a la historia clínica. Son datos sensibles de salud y se aplica mínimo privilegio (Ley 25.326, art. 7 y 9).
- **RN-SEG-03**: Tras 5 intentos de login fallidos en 15 minutos, la cuenta se bloquea 15 minutos.
- **RN-SEG-04**: La sesión interna expira tras 30 minutos de inactividad.
- **RN-SEG-05**: Todo acceso de lectura a una historia clínica genera un evento de auditoría `VER_HISTORIA`.

## Dominio: Agenda (RN-AG)

- **RN-AG-01**: No puede haber dos turnos activos (`RESERVADO` o `CONFIRMADO`) superpuestos. Se garantiza en la base con una exclusion constraint, no solo en el código.
- **RN-AG-02**: La duración del turno es la de la prestación; la secretaria y el odontólogo pueden extenderla en bloques de 5 minutos.
- **RN-AG-03**: No se puede crear un bloqueo sobre turnos activos: primero hay que cancelar o mover esos turnos.
- **RN-AG-04**: La reserva online solo ofrece horarios dentro del `horario_atencion`, sin bloqueos ni turnos activos, de prestaciones `reservable_online`, y con al menos 2 h de anticipación.
- **RN-AG-05**: La reserva online permite hasta 60 días hacia adelante.
- **RN-AG-06**: Un paciente puede tener como máximo 2 turnos futuros activos reservados online. El consultorio no tiene ese límite.
- **RN-AG-07**: Los sobreturnos solo los crea el personal interno, que debe confirmar explícitamente que superponen. Quedan marcados y fuera de la constraint (estado activo, flag `sobreturno = true`). **Suposición:** el MVP los necesita; validar con usuarios.

## Dominio: Turnos y ciclo de vida (RN-TU)

- **RN-TU-01**: Estados y transiciones válidas:
  - `RESERVADO → CONFIRMADO | CANCELADO | ATENDIDO | AUSENTE`
  - `CONFIRMADO → CANCELADO | ATENDIDO | AUSENTE`
  - `CANCELADO`, `ATENDIDO` y `AUSENTE` son finales. Corregir `ATENDIDO` o `AUSENTE` requiere un rol interno y queda auditado.
- **RN-TU-02**: El paciente puede cancelar o reprogramar por enlace o WhatsApp hasta `cancelacion_minima_horas` (24 h por defecto) antes del turno. Pasado ese plazo debe comunicarse con el consultorio.
- **RN-TU-03**: Reprogramar equivale a cancelar el turno original y crear uno nuevo vinculado; la reprogramación no deja dos turnos activos a la vez.
- **RN-TU-04**: Si el paciente no asiste, el personal marca `AUSENTE` y se incrementa `paciente.ausencias_count`.
- **RN-TU-05**: Las ausencias **solo se registran** y se muestran en la ficha y en la agenda; no limitan la reserva online en v1.

## Dominio: Notificaciones y confirmación (RN-NO)

- **RN-NO-01**: Se envía un recordatorio por WhatsApp `recordatorio_horas_antes` (48 h por defecto) antes de cada turno `RESERVADO`, con botones "Confirmo" y "Cancelar".
- **RN-NO-02**: Si el turno se crea con menos de 48 h de anticipación, el recordatorio se envía inmediatamente, salvo que falten menos de 2 h.
- **RN-NO-03**: Si el paciente no tiene WhatsApp, no dio consentimiento o el envío falla, se usa email como respaldo.
- **RN-NO-04**: Cada combinación (turno, tipo, canal) se envía como máximo una vez (idempotencia). Los reintentos técnicos no duplican el mensaje.
- **RN-NO-05**: Si a `confirmacion_limite_horas` (24 h) del turno sigue `RESERVADO`, el sistema **avisa al consultorio**: marca el turno como "sin confirmar", lo destaca en la agenda y lo agrega a la bandeja de pendientes. **No lo cancela ni lo libera.**
- **RN-NO-06**: La respuesta "Confirmo" pasa el turno a `CONFIRMADO`. "Cancelar" lo pasa a `CANCELADO` si se respeta RN-TU-02; si no, se avisa al consultorio.
- **RN-NO-07**: Los mensajes fuera de la ventana de 24 h de WhatsApp solo se envían con plantillas aprobadas por Meta (ver `11_integracion_whatsapp.md`).
- **RN-NO-08**: No se envían mensajes de marketing ni campañas en v1; solo mensajes de utilidad vinculados a un turno o a una oferta de hueco pedida por el paciente al anotarse en la lista de espera (RN-LE-04).

## Dominio: Lista de espera (RN-LE)

- **RN-LE-01**: Cuando se libera un turno, el sistema muestra la lista activa ordenada por antigüedad y filtrable por prestación. **La secretaria elige** a quién ofrecerle el hueco: puede asignarlo directamente o enviar una oferta por WhatsApp (RN-LE-04).
- **RN-LE-02**: Al asignar un turno a un paciente de la lista, directamente o porque aceptó una oferta, su entrada pasa a `ASIGNADA`.
- **RN-LE-03**: El sistema **nunca envía ofertas sin una acción humana** ni elige destinatarios por su cuenta en v1. La oferta en cascada automática queda para v2 (DD-10).
- **RN-LE-04**: Una oferta se envía con un clic a entre 1 y 5 pacientes seleccionados, con la plantilla `oferta_hueco` y el botón "Lo tomo". Solo se puede ofrecer a pacientes con `acepta_whatsapp = true` y un hueco que empiece dentro de más de 2 h.
- **RN-LE-05**: **El primero que acepta se queda con el turno.** La aceptación crea el turno `CONFIRMADO` en una transacción; la exclusion constraint (RN-AG-01) garantiza que, ante aceptaciones simultáneas, solo una prospere. A los que aceptan después se les responde "ese turno ya fue tomado" y siguen `ACTIVA` en la lista.
- **RN-LE-06**: La oferta vence a los `oferta_vencimiento_minutos` (30 por defecto) o 2 h antes del inicio del hueco, lo que ocurra primero. Una oferta vencida o tomada no acepta más respuestas. Si vence sin aceptación, el hueco sigue libre y se puede ofrecer de nuevo a otros pacientes.
- **RN-LE-07**: Sobre un mismo hueco solo puede haber una oferta `ABIERTA` a la vez. Mientras está abierta, el hueco no se ofrece en la reserva online, para que no lo tome otra persona. La secretaria puede cancelar la oferta y liberar el hueco.

## Dominio: Historia clínica (RN-HC)

- **RN-HC-01**: Las entradas de historia clínica son **inalterables**: no se editan ni borran. Una corrección se hace con una entrada `ENMIENDA` que referencia la original, con autor y fecha (Ley 26.529, art. 18).
- **RN-HC-02**: Cada entrada guarda el hash SHA-256 de su contenido y el de la entrada anterior del paciente, para detectar alteraciones (integridad).
- **RN-HC-03**: Los pacientes con historia clínica no se eliminan físicamente. La historia se conserva como mínimo 10 años desde la última actuación (Ley 26.529, art. 18).
- **RN-HC-04**: Solo el odontólogo crea entradas de historia clínica.
- **RN-HC-05**: El paciente es titular de su historia clínica. El consultorio debe poder entregarle una copia completa (exportación PDF) dentro de las 48 h de solicitada (Ley 26.529, art. 14).
- **RN-HC-06**: Cada entrada queda asociada al profesional autor (nombre y matrícula).

## Dominio: Caja (RN-CA)

- **RN-CA-01**: Los cobros no se borran; se anulan con motivo, y solo el odontólogo puede anularlos.
- **RN-CA-02**: Saldo del paciente = suma de presupuestos `ACEPTADO` − suma de cobros no anulados.
- **RN-CA-03**: El sistema registra cobros pero **no emite comprobantes fiscales** en v1 (sin ARCA).
- **RN-CA-04**: Los montos se expresan en pesos argentinos (ARS) con 2 decimales.

## Dominio: Datos personales (RN-DP)

- **RN-DP-01**: Se registra el consentimiento informado del paciente para el tratamiento de sus datos y para recibir WhatsApp, con fecha (Ley 25.326, art. 5 y 7).
- **RN-DP-02**: El paciente puede ejercer acceso, rectificación y supresión. La supresión no alcanza a la historia clínica mientras dure la obligación de conservación (RN-HC-03).
- **RN-DP-03**: El odontólogo puede exportar todos los datos de su instancia en formato abierto (CSV/JSON + PDF de historias).

## Dominio: Excepciones globales

- **RN-GL-01**: Toda acción que crea, modifica o anula datos de turnos, pacientes, historia clínica o caja genera un evento de auditoría.
- **RN-GL-02**: Las fechas se almacenan en UTC y se muestran en la zona horaria del consultorio.
- **RN-GL-03**: Los enlaces públicos de turno usan un token aleatorio de 256 bits, no un ID secuencial, y vencen cuando el turno termina.
