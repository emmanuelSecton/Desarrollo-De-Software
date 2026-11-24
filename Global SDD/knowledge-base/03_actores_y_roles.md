# Actores y Roles

## Actores del sistema

| Actor | Descripción | Cómo interactúa |
|---|---|---|
| Odontólogo (`ODONTOLOGO`) | Profesional dueño del consultorio y de la instancia. Es el responsable de la historia clínica. | Panel interno con usuario y contraseña. Único profesional de la agenda en v1. |
| Secretaria (`SECRETARIA`) | Personal administrativo del consultorio (0 o más). | Panel interno con usuario y contraseña. |
| Paciente | Persona atendida. **No tiene cuenta** en v1 (ver DD-05). | Página pública de reserva, enlaces firmados en los mensajes y respuestas por WhatsApp. |
| Sistema (jobs) | Tareas programadas internas. | Envía recordatorios, detecta turnos sin confirmar y procesa webhooks. |
| Meta / Google | Servicios externos. | Webhooks entrantes (Meta) y OAuth (Google). |

## RBAC — Matriz de permisos

C = crear · R = leer · U = actualizar · D = eliminar/anular · — = sin acceso

| Recurso | Odontólogo | Secretaria | Paciente (enlace firmado / público) |
|---|---|---|---|
| Configuración del consultorio y prestaciones | CRUD | R | R (solo prestaciones reservables y horarios) |
| Usuarios internos | CRUD | — | — |
| Turnos | CRUD | CRUD | C (reserva), R/U/D solo **su** turno |
| Bloqueos de agenda | CRUD | CRUD | — |
| Lista de espera | CRUD | CRUD | — |
| Ofertas de hueco (DD-10) | CRUD (crear, cancelar) | CRUD (crear, cancelar) | Aceptar con el botón "Lo tomo" de WhatsApp (solo destinatarios de la oferta) |
| Pacientes: datos personales y contacto | CRUD | CRU | — |
| Historia clínica (anamnesis, evolución, odontograma) | CR + enmienda | — | — |
| Ausencias (marcar asistencia) | CRU | CRU | — |
| Presupuestos | CRUD | R | — |
| Cobros y saldo | CRUD | CRU (anular: no) | — |
| Auditoría | R | — | — |
| Exportación de datos | C | — | — |
| Integraciones (WhatsApp, Google) | CRUD | — | — |

Notas:
- La historia clínica **no se modifica ni se borra**: se corrige con una enmienda que referencia la entrada original (RN-HC-01).
- La secretaria **no** accede a la historia clínica. Datos sensibles de salud, mínimo privilegio (RN-SEG-02).
- La anulación de un cobro la hace solo el odontólogo y queda auditada.

## Rutas públicas

Accesibles sin sesión de usuario interno:

- `/` y `/reservar`: página pública de reserva (SPA).
- `/turno/{token}`: ver, cancelar o reprogramar un turno mediante el enlace firmado del mensaje.
- `/api/v1/public/**`: disponibilidad, reservas y gestión por token.
- `/api/v1/webhooks/whatsapp`: protegido por firma `X-Hub-Signature-256` de Meta, no por sesión.
- `/api/v1/auth/login` y `/api/v1/auth/password/recover`.
- `/privacidad` y `/terminos`: textos legales (obligatorios por Ley 25.326).
