# Cumplimiento Legal

> Esto es una guía de diseño para el equipo técnico, **no asesoramiento legal**. Antes del lanzamiento, un abogado especializado en datos personales y salud debe revisarla.

## Marco normativo aplicable

| Norma | Qué exige al sistema (resumen) |
|---|---|
| **Ley 25.326** de Protección de Datos Personales (y su Decreto 1558/2001) | Los datos de salud son **datos sensibles**. Requieren consentimiento informado y finalidad determinada, seguridad y confidencialidad, y derechos de acceso, rectificación y supresión. Las bases de datos deben inscribirse ante la autoridad de control (AAIP). La transferencia internacional requiere un nivel de protección adecuado o garantías. |
| **Ley 26.529** de Derechos del Paciente, Historia Clínica y Consentimiento Informado (modificada por Ley 26.742) | El paciente es titular de su historia clínica y tiene derecho a una copia. La historia debe ser íntegra, completa, con fecha y autoría, inalterable e inviolable. El responsable debe custodiarla al menos 10 años. Una historia electrónica requiere mecanismos que garanticen su integridad, autenticidad, inalterabilidad, perdurabilidad y recuperabilidad. |
| **Ley 25.506** de Firma Digital | Marco de validez de los documentos y firmas electrónicas o digitales; relevante para la autoría de las entradas (PA-12). |
| **Ley 27.706** (Programa Federal Único de Informatización y Digitalización de Historias Clínicas) | Marco de digitalización e interoperabilidad de historias clínicas; seguir la reglamentación por si impone requisitos técnicos o de interoperabilidad. |
| **Resolución AAIP 47/2018** | Medidas de seguridad recomendadas para el tratamiento de datos personales. Se usa como checklist de buenas prácticas. |

## Cómo responde el diseño

| Requisito | Implementación | Referencia |
|---|---|---|
| Consentimiento informado para datos sensibles | Casilla obligatoria con fecha en la reserva o el alta; texto de la política de privacidad versionado | RN-DP-01, `paciente.consentimiento_datos_at` |
| Finalidad y minimización | Solo los datos necesarios; Google Calendar sin datos clínicos | US-014 |
| Confidencialidad / mínimo privilegio | La secretaria no accede a la historia clínica; RBAC | RN-SEG-02, `03_actores_y_roles.md` |
| Integridad e inalterabilidad de la HC | Entradas append-only, enmiendas y hash encadenado; sin UPDATE ni DELETE en la base | RN-HC-01, RN-HC-02, DD-07 |
| Autoría y fecha | Cada entrada guarda autor (nombre y matrícula) y timestamp | RN-HC-06 |
| Conservación mínima de 10 años | Sin borrado físico de pacientes con HC; backups con retención | RN-HC-03 |
| Derecho del paciente a una copia | Exportación PDF de la HC en menos de 48 h | RN-HC-05, US-044 |
| Acceso, rectificación y supresión | Proceso documentado; la supresión excluye la HC mientras dure la obligación de conservación | RN-DP-02 |
| Trazabilidad | Auditoría append-only de lecturas y modificaciones | RN-GL-01, RN-SEG-05 |
| Seguridad técnica | TLS, cifrado en reposo, BCrypt, bloqueo por intentos, expiración de sesión, backups, rate limiting | `08_arquitectura_propuesta.md` |
| Portabilidad / no dependencia del proveedor | Exportación completa en formatos abiertos | RN-DP-03, US-061 |
| Transferencia internacional | Elegir la región del hosting y evaluar nivel adecuado o cláusulas contractuales | PA-09 |
| Inscripción de la base | Cada consultorio es el **responsable** de su base y debe inscribirla en la AAIP; el proveedor del sistema actúa como **encargado del tratamiento** | Contrato de servicio |

## Roles legales

- **Responsable de la base de datos**: el odontólogo o consultorio. Decide la finalidad, es dueño de la relación con el paciente y custodia la historia clínica.
- **Encargado del tratamiento**: el proveedor del sistema. Trata los datos por cuenta del responsable, bajo contrato, y no los usa para fines propios.
- **Subencargados**: Meta (WhatsApp), Google (Calendar), el hosting y el proveedor de email. Deben listarse públicamente en la página de privacidad, como hacen Dentia y Dentiqa en el estudio de mercado.

## Entregables legales para el lanzamiento

- [ ] Política de privacidad (pacientes) y términos de servicio (consultorios).
- [ ] Contrato de encargado del tratamiento (DPA) entre el proveedor y cada consultorio.
- [ ] Listado público de subencargados con su ubicación.
- [ ] Texto del consentimiento informado para datos y WhatsApp.
- [ ] Procedimiento de respuesta a solicitudes de acceso, rectificación y supresión.
- [ ] Procedimiento de notificación de incidentes de seguridad.
- [ ] Guía para que el consultorio inscriba su base en la AAIP.
- [ ] Revisión del asesor legal de este documento y de las preguntas PA-09 y PA-12.
