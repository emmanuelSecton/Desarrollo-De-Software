# Discovery — SaaS de gestión odontológica (Global SDD)

**Fecha**: 2026-11-23
**Fuentes investigadas**: 20 sistemas (ver `discovery/sources/` y el informe comparativo `discovery/estudio-de-mercado.md`)
**Naturaleza**: producto real a lanzar (no solo un trabajo práctico)

## 1. Problema que resuelve

El odontólogo independiente pierde horas de sillón por pacientes que faltan
sin avisar, y maneja agenda, historia clínica y cobros en herramientas
separadas (papel, planillas, WhatsApp personal). El producto unifica la gestión
clínica y administrativa del consultorio y ataca el ausentismo con
recordatorios y confirmaciones por WhatsApp.

## 2. Usuarios / roles

- **Odontólogo**: atiende y registra la parte clínica (ficha, anamnesis, odontograma, evolución, presupuestos).
- **Secretaria**: gestiona la agenda, los turnos sin confirmar, la lista de espera y los cobros.
- **Paciente**: reserva, confirma y cancela sus turnos online o por WhatsApp.

## 3. Casos de uso

1. Como paciente, quiero reservar un turno online a cualquier hora para no tener que llamar al consultorio.
2. Como paciente, quiero confirmar o cancelar mi turno respondiendo un WhatsApp para avisar sin esfuerzo.
3. Como secretaria, quiero ver los turnos sin confirmar y reasignar los huecos desde la lista de espera para no perder horas de sillón.
4. Como odontólogo, quiero registrar cada consulta en la ficha y el odontograma del paciente para tener su historia clínica completa.
5. Como secretaria, quiero registrar los cobros y presupuestos para saber qué debe cada paciente.

## 4. Competidores / soluciones existentes

| Competidor | Problema que resuelve | Pricing | Diferenciadores |
|---|---|---|---|
| Órbita (AR) | Gestión odontológica con asistente de WhatsApp | A cotizar | ARCA y liquidación de obras sociales documentadas en ayuda |
| Bilog (AR) | Gestión odontológica para consultorios, clínicas y obras sociales | Freemium; planes pagos sin precio | Liquidación y auditoría de obras sociales; el más establecido |
| AgendaPro (CL) | Agenda y turnos multirubro con vertical dental | Desde ARS 9.900/mes | Precios en ARS, app móvil, Mercado Pago, WhatsApp oficial |
| Odonto.ar (AR) | Gestión simple para el independiente | USD 29/mes, 30 días gratis | Autogestión de turnos del paciente, precio bajo |
| Dentia (AR) | Gestión con IA y WhatsApp | $20.000/mes | WhatsApp Cloud API oficial en términos; documentación legal transparente |

**Notas**: el informe completo compara 20 sistemas con una matriz ponderada.
Ningún sistema supera 2,8/5 con evidencia pública: el mercado argentino declara
mucho y documenta poco. El vacío más claro es la **lista de espera** (ningún
producto argentino la evidencia) y el circuito completo contra ausencias.
Demos prioritarias: Órbita, Bilog, AgendaPro, Dentalink y Dentia. Referencias de
UX: Dentally, Doctoralia y AgendaPro.

## 5. Funcionalidades necesarias

- Agenda por profesional: vista diaria y semanal, duración por prestación, bloqueos y prevención de solapamientos.
- Reserva online del paciente 24/7 por enlace o web, con cancelación y reprogramación.
- Recordatorio automático por WhatsApp (Cloud API oficial) 48 h antes, con confirmación o cancelación por respuesta; respaldo por email.
- Aviso al consultorio de los turnos sin confirmar a las 24 h.
- Lista de espera (gestión manual).
- Contador de ausencias visible en la ficha del paciente.
- Ficha clínica: datos, anamnesis, odontograma y evolución.
- Presupuestos y caja básica (registro de cobros).
- Roles y permisos: odontólogo, secretaria y paciente.
- Registro de auditoría y exportación de datos.
- Sincronización con Google Calendar.

## 6. Funcionalidades opcionales

- Mercado Pago y señas online al reservar.
- Facturación electrónica ARCA.
- Obras sociales y prepagas (coberturas y liquidaciones).
- Periodontograma, imágenes y radiografías.
- Liberación automática del turno no confirmado y lista de espera automática en cascada.
- Restricciones a pacientes con ausencias reiteradas.
- Multisede y planes para clínicas.
- Asistente conversacional con IA.

## 7. Reglas de negocio

- El recordatorio se envía 48 h antes del turno.
- Si el paciente no confirma 24 h antes, el sistema **avisa al consultorio**, que decide qué hacer; el turno no se libera automáticamente.
- La lista de espera es **manual**: la secretaria elige a quién ofrecer un turno liberado.
- Las ausencias **solo se registran** en la ficha; no restringen al paciente.
- Un profesional no puede tener dos turnos superpuestos.
- La historia clínica no se borra: las correcciones quedan registradas (inalterabilidad, Ley 26.529).

## 8. Integraciones

- **WhatsApp Business Platform (Cloud API oficial de Meta)**: recordatorios y confirmaciones; requiere plantillas aprobadas y tiene costo por conversación.
- **Google Calendar**: sincronización de la agenda del profesional.
- **Email**: recordatorios de respaldo y notificaciones.

## 9. Restricciones

- Producto real a lanzar, con **plazo acotado** (duración exacta sin definir).
- **Cumplimiento legal obligatorio**: Ley 25.326 (datos personales sensibles de salud) y Ley 26.529 (derechos del paciente e historia clínica).
- **Stack**: Java con Spring Boot en el backend, y TypeScript.
- MVP para un solo consultorio de un odontólogo independiente.

## 10. Riesgos

- **Diferencial débil**: el anti-ausentismo es el diferencial elegido, pero las reglas de la v1 son conservadoras (solo aviso, lista de espera manual, sin seña). Funcionalmente queda cerca de lo que ya ofrecen Bilog o Dentalink. El diferencial de la v1 depende de la ejecución (WhatsApp oficial confiable, simplicidad, precio claro) más que de funciones únicas.
- **Supuesto sin probar**: que los pacientes respondan a recordatorios por WhatsApp y que eso reduzca las ausencias de forma medible.
- **Dependencia de Meta**: costo por conversación, aprobación de plantillas y verificación del negocio.
- **Lanzar sin ARCA**: la facturación electrónica es un estándar en crecimiento en el mercado local; no tenerla puede frenar la adopción.
- **Costo del cumplimiento**: historia clínica electrónica (integridad, inalterabilidad, conservación, acceso del paciente) y datos sensibles.
- **Mercado con muchos competidores jóvenes** (2023–2026) y precios bajos.

## 11. Preguntas abiertas

- ¿Cuál es el plazo concreto del MVP: 3 meses o 6?
- ¿TypeScript se usa para el frontend web (por ejemplo React o Angular), para una app del paciente o para ambos?
- ¿Cuál va a ser el precio y el modelo de cobro (mensual en ARS, prueba gratuita)?
- ¿Hace falta app móvil nativa o alcanza con web responsive?
- ¿El paciente necesita cuenta propia o alcanza con validar su identidad por DNI y teléfono al reservar?

**Supuestos registrados** (a confirmar en la Knowledge Base):
- TypeScript es para el frontend web; el backend es Spring Boot.
- La reserva online del paciente entra en la v1.
- El MVP es para un solo consultorio, sin sucursales.
