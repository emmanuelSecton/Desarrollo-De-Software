# Funcionalidades

Organizadas por **épica** y **historia de usuario** (US-NNN). Prioridad: **M** = MVP imprescindible · **S** = MVP deseable (se recorta si el plazo aprieta).

## Épica 1: Configuración e identidad

### US-001 — Instalar y configurar el consultorio (M)
**Como** odontólogo **quiero** cargar los datos del consultorio, mis horarios de atención y mis prestaciones **para** que la agenda y la reserva online reflejen cómo trabajo.

**Criterios de aceptación**:
- [ ] En el primer login se fuerza el cambio de contraseña y se abre un asistente de configuración.
- [ ] Se pueden crear y editar prestaciones con duración (múltiplo de 5) y la marca "reservable online".
- [ ] Se definen franjas de atención por día de la semana.
- [ ] Se configuran las horas del recordatorio, del límite de confirmación y de la cancelación mínima (por defecto 48 / 24 / 24).

**Reglas relacionadas**: RN-AG-04, RN-NO-01, RN-NO-05, RN-TU-02

### US-002 — Gestionar usuarios internos (M)
**Como** odontólogo **quiero** dar de alta y de baja a mis secretarias **para** que gestionen la agenda sin ver la historia clínica.

**Criterios de aceptación**:
- [ ] Alta con email y contraseña temporal; baja lógica (`activo = false`).
- [ ] Una secretaria no ve el menú ni los endpoints de historia clínica (HTTP 403).

**Reglas relacionadas**: RN-SEG-01, RN-SEG-02

### US-003 — Iniciar sesión de forma segura (M)
**Como** usuario interno **quiero** iniciar sesión y recuperar mi contraseña **para** acceder al panel.

**Criterios de aceptación**:
- [ ] Bloqueo tras 5 intentos fallidos; expiración por inactividad a los 30 minutos.
- [ ] La recuperación de contraseña envía un enlace por email con vencimiento de 1 h.

**Reglas relacionadas**: RN-SEG-03, RN-SEG-04

## Épica 2: Agenda

### US-010 — Ver la agenda diaria y semanal (M)
**Como** odontólogo o secretaria **quiero** ver los turnos del día y de la semana con su estado **para** organizar la jornada.

**Criterios de aceptación**:
- [ ] Vista día y vista semana; color por estado; marca visible de "sin confirmar" y de "paciente con ausencias".
- [ ] Se muestran los bloqueos y las franjas fuera del horario de atención.

### US-011 — Crear, mover y cancelar turnos desde el consultorio (M)
**Como** secretaria **quiero** crear, mover y cancelar turnos **para** mantener la agenda al día.

**Criterios de aceptación**:
- [ ] Al crear un turno se busca el paciente por DNI, nombre o teléfono, o se lo da de alta en el momento.
- [ ] Un turno superpuesto se rechaza con un mensaje claro, salvo que se marque como sobreturno.
- [ ] Mover un turno reprograma (RN-TU-03) y notifica al paciente.
- [ ] Cancelar pide motivo y notifica al paciente.

**Reglas relacionadas**: RN-AG-01, RN-AG-02, RN-AG-07, RN-TU-03

### US-012 — Bloquear horarios (M)
**Como** odontólogo **quiero** bloquear franjas (vacaciones, congresos) **para** que no se reserven turnos.

**Reglas relacionadas**: RN-AG-03

### US-013 — Registrar asistencia y ausencias (M)
**Como** secretaria **quiero** marcar cada turno como atendido o ausente **para** llevar el registro de ausencias.

**Criterios de aceptación**:
- [ ] Marcar `AUSENTE` incrementa el contador del paciente.
- [ ] Corregir un estado final queda auditado.

**Reglas relacionadas**: RN-TU-01, RN-TU-04, RN-TU-05

### US-014 — Sincronizar con Google Calendar (S)
**Como** odontólogo **quiero** ver mis turnos en mi Google Calendar **para** consultarlos desde el celular.

**Criterios de aceptación**:
- [ ] Conexión por OAuth; los turnos se crean, actualizan y borran en el calendario elegido.
- [ ] El evento lleva solo nombre de pila e inicial del apellido y la prestación, sin datos clínicos (minimización de datos).
- [ ] Un fallo de Google no bloquea la operación en la agenda; se reintenta.

## Épica 3: Reserva online del paciente

### US-020 — Reservar un turno online (M)
**Como** paciente **quiero** elegir prestación, día y horario libre y reservar **para** no tener que llamar.

**Criterios de aceptación**:
- [ ] Muestra solo horarios válidos (RN-AG-04, RN-AG-05).
- [ ] Pide DNI, nombre, apellido, celular y email, con aceptación de la política de privacidad y consentimiento para WhatsApp.
- [ ] Si el DNI ya existe, asocia el turno a ese paciente sin sobrescribir sus datos clínicos.
- [ ] Si dos pacientes eligen el mismo horario, solo uno lo obtiene; el otro ve "ese horario se acaba de ocupar".
- [ ] Envía una confirmación de reserva por WhatsApp o email con el enlace del turno.

**Reglas relacionadas**: RN-AG-01, RN-AG-04, RN-AG-05, RN-AG-06, RN-DP-01

### US-021 — Cancelar o reprogramar mi turno (M)
**Como** paciente **quiero** cancelar o cambiar mi turno desde el enlace o WhatsApp **para** avisar sin llamar.

**Criterios de aceptación**:
- [ ] Permitido hasta 24 h antes; después se muestra el teléfono del consultorio.
- [ ] Al cancelar, el consultorio ve el hueco libre y la lista de espera sugerida.

**Reglas relacionadas**: RN-TU-02, RN-TU-03, RN-LE-01

## Épica 4: Anti-ausentismo

### US-030 — Recordatorio y confirmación por WhatsApp (M)
**Como** paciente **quiero** recibir un recordatorio 48 h antes y confirmar o cancelar con un botón **para** no olvidarme del turno.

**Criterios de aceptación**:
- [ ] El job envía la plantilla aprobada con los botones "Confirmo" y "Cancelar".
- [ ] La respuesta actualiza el estado del turno en menos de 1 minuto.
- [ ] Si WhatsApp falla, se envía por email (RN-NO-03).
- [ ] No hay envíos duplicados (RN-NO-04).

**Reglas relacionadas**: RN-NO-01 a RN-NO-07

### US-031 — Bandeja de turnos sin confirmar (M)
**Como** secretaria **quiero** ver los turnos que siguen sin confirmar a 24 h **para** decidir si llamo al paciente o libero el horario.

**Criterios de aceptación**:
- [ ] Lista con paciente, teléfono, turno, cantidad de ausencias previas y estado del recordatorio (enviado, entregado, leído).
- [ ] Desde la bandeja se puede abrir el chat de WhatsApp (click-to-chat), cancelar o marcar "contactado".

**Reglas relacionadas**: RN-NO-05

### US-032 — Lista de espera y oferta de huecos con un clic (M)
**Como** secretaria **quiero** anotar pacientes que quieren un turno antes y, cuando se libera un hueco, ofrecérselo por WhatsApp a varios a la vez con un clic **para** recuperar la hora de sillón sin llamar uno por uno.

**Criterios de aceptación**:
- [ ] Alta con paciente, prestación opcional y preferencia en texto libre.
- [ ] Al liberarse un turno se sugiere la lista filtrada por prestación y ordenada por antigüedad.
- [ ] Se puede asignar directamente (crea el turno y marca `ASIGNADA`) **o** seleccionar entre 1 y 5 pacientes y "Ofrecer por WhatsApp".
- [ ] Solo se puede ofrecer a pacientes con consentimiento de WhatsApp y un hueco que empiece dentro de más de 2 h.
- [ ] Mientras la oferta está abierta, el panel muestra a quién se envió, quién la leyó, el tiempo restante y un botón para cancelarla; el hueco no aparece en la reserva online.
- [ ] Cuando alguien la toma, el panel se actualiza solo y el hueco aparece ocupado en la agenda.

**Reglas relacionadas**: RN-LE-01 a RN-LE-07

### US-033 — Tomar un hueco ofrecido por WhatsApp (M)
**Como** paciente en lista de espera **quiero** recibir un WhatsApp cuando se libera un turno y tomarlo con un botón **para** atenderme antes sin tener que llamar.

**Criterios de aceptación**:
- [ ] El mensaje indica prestación, día y hora, y cuánto tiempo hay para responder.
- [ ] Tocar "Lo tomo" crea el turno `CONFIRMADO` a mi nombre y recibo la confirmación con el enlace del turno.
- [ ] Si otro paciente lo tomó antes o la oferta venció, recibo "ese turno ya fue tomado" o "la oferta venció" y sigo en la lista de espera.
- [ ] Ante dos aceptaciones simultáneas, solo una obtiene el turno; nunca queda un turno duplicado.

**Reglas relacionadas**: RN-LE-04 a RN-LE-07, RN-AG-01, RN-NO-07

## Épica 5: Pacientes e historia clínica

### US-040 — Ficha del paciente (M)
**Como** odontólogo **quiero** ver en una pantalla los datos, el contador de ausencias, los turnos, el saldo y la historia clínica **para** tener todo a mano.

### US-041 — Registrar anamnesis (M)
**Como** odontólogo **quiero** cargar la anamnesis (antecedentes, alergias, medicación) **para** atender con seguridad.

**Criterios de aceptación**:
- [ ] Formulario estructurado; las alergias y la medicación quedan destacadas en la ficha.
- [ ] Una actualización crea una entrada nueva; las anteriores quedan visibles como historial.

**Reglas relacionadas**: RN-HC-01, RN-HC-04

### US-042 — Odontograma (M)
**Como** odontólogo **quiero** marcar el estado de cada pieza y cara con la notación FDI **para** registrar el diagnóstico y lo realizado.

**Criterios de aceptación**:
- [ ] Dentición permanente y temporal; estados por cara; distinción entre "a realizar" y "realizado".
- [ ] Cada guardado crea una versión; se puede ver la evolución en el tiempo.

**Reglas relacionadas**: RN-HC-01, RN-HC-02

### US-043 — Evolución clínica y enmiendas (M)
**Como** odontólogo **quiero** registrar lo realizado en cada consulta y corregir con enmiendas **para** cumplir con la inalterabilidad de la historia clínica.

**Reglas relacionadas**: RN-HC-01, RN-HC-02, RN-HC-06

### US-044 — Entregar copia de la historia clínica (M)
**Como** odontólogo **quiero** exportar en PDF la historia clínica completa de un paciente **para** entregársela cuando la pida.

**Reglas relacionadas**: RN-HC-05

## Épica 6: Caja

### US-050 — Presupuestos (M)
**Como** odontólogo **quiero** armar un presupuesto con prestaciones y piezas y entregarlo en PDF **para** que el paciente decida el tratamiento.

### US-051 — Registrar cobros y ver saldo (M)
**Como** secretaria **quiero** registrar cobros con su medio de pago y ver el saldo del paciente **para** saber cuánto debe.

**Reglas relacionadas**: RN-CA-01 a RN-CA-04

### US-052 — Resumen de caja (S)
**Como** odontólogo **quiero** ver lo cobrado por día, semana y mes, discriminado por medio de pago **para** controlar ingresos.

## Épica 7: Cumplimiento, auditoría y datos

### US-060 — Auditoría (M)
**Como** odontólogo **quiero** consultar quién vio o modificó qué y cuándo **para** tener trazabilidad sobre los datos de salud.

**Reglas relacionadas**: RN-GL-01, RN-SEG-05

### US-061 — Exportar todos los datos (M)
**Como** odontólogo **quiero** exportar todos mis datos en formatos abiertos **para** no quedar atado al sistema y cumplir con la portabilidad.

**Reglas relacionadas**: RN-DP-03

### US-062 — Indicadores de ausentismo (S)
**Como** odontólogo **quiero** ver la tasa de ausencias, confirmaciones, reservas online y **huecos recuperados por oferta** del mes **para** medir si el sistema me sirve.
