# Visión y Objetivos

> Nombre comercial del producto: **(TBD)**. En esta KB se lo llama "el sistema".

## Propósito del sistema

**El sistema le permite a un odontólogo independiente gestionar su consultorio (agenda, pacientes, historia clínica y cobros) y reducir los turnos perdidos por ausencias mediante recordatorios y confirmaciones por WhatsApp.**

Hoy el odontólogo independiente argentino maneja turnos por teléfono o WhatsApp personal, la historia clínica en papel o en planillas, y los cobros por separado. Cada ausencia sin aviso es una hora de sillón perdida. El estudio de mercado (`discovery/informe-discovery.md`) mostró que la gestión clínica es un estándar que todos los competidores ofrecen, y que el vacío local está en el **circuito contra ausencias**: confirmación, aviso y reasignación del turno. El sistema cubre el estándar y pone el foco en ese circuito.

## Objetivos por actor

| Actor | Objetivo principal | Objetivos secundarios |
|---|---|---|
| Odontólogo | Tener la historia clínica completa de cada paciente en un solo lugar | Ver su agenda del día; reducir ausencias; saber qué le deben |
| Secretaria | Mantener la agenda llena y al día sin llamar uno por uno | Ver turnos sin confirmar; reasignar huecos desde la lista de espera; registrar cobros |
| Paciente | Sacar, confirmar o cancelar un turno sin llamar | Recibir recordatorios; cancelar con anticipación de forma simple |

## Alcance v1.0 (MVP)

- Agenda del profesional: vista diaria y semanal, duración por prestación, bloqueos de horario y prevención de solapamientos.
- Reserva online del paciente 24/7 mediante enlace público, con cancelación y reprogramación.
- Recordatorio automático por WhatsApp (Cloud API oficial) 48 h antes, con confirmación o cancelación por respuesta; respaldo por email.
- Aviso al consultorio de los turnos sin confirmar a las 24 h.
- Lista de espera con **oferta de huecos por WhatsApp con un clic**: la secretaria elige a quiénes ofrecer y gana el primero que toca "Lo tomo" (DD-10).
- Registro y contador de ausencias por paciente.
- Ficha del paciente: datos personales, anamnesis, odontograma y evolución clínica, con historia clínica inalterable.
- Presupuestos y caja básica (registro de cobros y saldo del paciente).
- Usuarios internos con roles (odontólogo, secretaria) y acceso del paciente a sus turnos.
- Registro de auditoría y exportación de datos.

## Fuera de alcance (v1.0)

- Multi-tenant: cada consultorio tiene **su propia instancia** de la aplicación y su propia base, sobre infraestructura compartida (ver DD-01).
- Varios profesionales por consultorio, multisede y agenda por sillón/box.
- Mercado Pago, señas y cobros online.
- Facturación electrónica ARCA.
- Obras sociales, prepagas y liquidaciones.
- Periodontograma, imágenes y radiografías.
- Liberación automática de turnos no confirmados y oferta automática en cascada sin intervención humana.
- Restricciones a pacientes con ausencias reiteradas.
- Asistente conversacional con IA.
- App móvil nativa (la SPA es responsive).
- Recetas y firma digital.
- **Postergados por el recorte de alcance (DD-13)**: sincronización con Google Calendar (US-014), resumen de caja (US-052) e indicadores de ausentismo en el panel (US-062).

## Métricas de éxito

| Métrica | Cómo se mide | Meta inicial (supuesto a validar) |
|---|---|---|
| Tasa de ausentismo | Turnos con estado `AUSENTE` / turnos totales del mes | Bajar respecto de la línea base que reporte el consultorio |
| Tasa de confirmación | Turnos confirmados por el paciente / recordatorios enviados | > 60 % |
| Huecos recuperados | Huecos liberados que se ocuparon desde la lista de espera (directo u oferta) / huecos liberados con más de 2 h de anticipación | > 40 % (supuesto a validar en el piloto) |
| Reservas online | Turnos creados por el paciente / turnos totales | > 30 % a los 3 meses |
| Costo de operación por cliente | Parte del host y del clúster compartidos + mensajes de Meta por consultorio/mes | Ver `09_decisiones_y_supuestos.md` DD-01 |
