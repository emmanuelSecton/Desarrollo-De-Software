# Oclusar

- **URL**: https://www.oclusar.com.ar/
- **Empresa proveedora**: OCLUSAR (razón social no evidenciada: ni términos ni privacidad la indican)
- **País de origen**: Argentina (referencias a Córdoba, ARCA, Ley 26.529 y pesos)
- **Fecha de investigación**: 2026-11-23

<!-- Las 8 secciones siguientes son el esquema fijo de web-scraper: van siempre y en este orden. -->

## Problema que resuelve

Sistema de gestión odontológica con agenda, historia clínica con odontograma, cobros y facturación electrónica; además una red B2B (Red Oclusar) para casas de insumos y laboratorios.

## Usuarios objetivo

Consultorio individual, clínica con varios profesionales (incluye alquiler de sillón a otros odontólogos); proveedores de insumos y laboratorios mediante Red Oclusar.

## Features detectadas

- Agenda con alerta de solapamientos, sincronización con Google Calendar, recordatorios por WhatsApp (declarado)
- Página/enlace de reserva online con seña opcional por Mercado Pago (declarado)
- Historia clínica con registros firmados no editables, odontograma, consentimientos firmados desde el celular, recetas
- Cobros (Mercado Pago, efectivo, transferencia), presupuestos con membrete, factura electrónica ARCA, seguimiento de obras sociales
- Multiusuario con roles, asignación de sillón, alquiler de sillón
- OCLU IA (adicional), importación desde Excel o foto de cuaderno y exportación completa

## Pricing

- Planes Individual, Consultorio y Clínica (según cantidad de profesionales): (no publicado); precios se informan al contratar. Moneda declarada: pesos (ARS) sin montos.
- Plan para consultorio "gratis para empezar", sin tarjeta (home).
- Adicionales: profesionales extra, OCLU IA (por tokens), almacenamiento adicional sobre 2 GB incluidos por espacio de trabajo.
- Pago mensual o anual con renovación automática; baja efectiva al fin del período, sin reembolso.
- Red Oclusar: plan mensual fijo sin comisión; monto no indicado.

## Posicionamiento

Gestión odontológica pensada para el consultorio argentino: "precio claro en pesos", cumplimiento local y facturación ARCA integrada (afirmación comercial).

## Diferenciadores

(Afirmaciones comerciales) Integración con ARCA, Mercado Pago y Google Calendar; registros inmutables con huella SHA-256; registro en el sistema nacional de interoperabilidad en salud y acceso a REFEPS; alquiler de sillón; importación desde foto de cuaderno; Red Oclusar.

## Notas del investigador

- El home no tiene página de precios ni de funcionalidades separadas; los precios se mencionan en una sección del home y en los términos, sin importes.
- Los términos indican que la prueba/promociones pueden cambiar; la existencia exacta del plan gratuito y sus límites no se verificó fuera del home.
- WhatsApp: la guía oficial indica que se usa click-to-chat desde el número propio, manual, sin API; la guía lo presenta como ventaja frente al riesgo de bloqueo por automatizar. Por tanto no hay recordatorios automáticos ni chatbot comprobados.
- Las guías (/guias) se leyeron solo como listado con resúmenes; el detalle de ARCA, obras sociales, exportación y permisos no se pudo comprobar en el contenido de cada guía.
- Declara "AAIP: registro como responsable completo; base de datos en trámite"; no se verificó ante la AAIP.
- No hay reseñas en marketplaces ni cifras de clientes.
- Páginas leídas mediante WebFetch (resumen automático).

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | SaaS, diseñado para uso móvil (web); sin app nativa evidenciada | Declarado | https://www.oclusar.com.ar/ |
| Agenda: vista diaria/semanal | No detallado; agenda con colores por estado de turno | Comprobado (estados por color, guía de WhatsApp); vistas no evidenciadas | https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp |
| Agenda: múltiples profesionales | Multiusuario con roles, invitaciones y distribución de turnos | Declarado | https://www.oclusar.com.ar/ |
| Agenda: múltiples sillones/boxes | Asignación de sillón y reportes por sillón | Declarado | https://www.oclusar.com.ar/ |
| Agenda: duración variable por prestación | No mencionado | No evidenciado | https://www.oclusar.com.ar/ |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | Alerta de solapamientos | Declarado | https://www.oclusar.com.ar/ |
| Turnos: reserva online 24/7 / enlace / web / redes | Enlace público donde el paciente elige horario libre; la solicitud llega a la agenda y se aprueba; enlace por mensajes de Instagram | Comprobado (guía de agenda/WhatsApp) | https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp |
| Turnos: chatbot / WhatsApp | WhatsApp por click-to-chat manual; OCLU IA (adicional) agenda y cobra, sin evidencia de canal WhatsApp | Comprobado (WhatsApp manual); chatbot Declarado | https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp |
| Turnos: confirmación, cancelación, reprogramación por el paciente | El paciente responde por WhatsApp y puede avisar que no asiste; no hay flujo formal registrado; "confirmación y aviso de llegada" declarado | Declarado | https://www.oclusar.com.ar/ |
| Turnos: lista de espera | No mencionado | No evidenciado | https://www.oclusar.com.ar/ |
| Automatización: recordatorios y confirmaciones (canal) | WhatsApp desde el número propio, envío manual con un toque; no automático | Comprobado | https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp |
| Automatización: ausentes, recuperación, controles periódicos, campañas | No mencionado | No evidenciado | https://www.oclusar.com.ar/ |
| Clínica: ficha clínica / anamnesis | Historia clínica digital con evolución; registros firmados no editables | Declarado (guía "Historia clínica digital y Ley 26.529" listada) | https://www.oclusar.com.ar/guias |
| Clínica: odontograma | Odontograma digital (guía dedicada listada) | Declarado | https://www.oclusar.com.ar/guias |
| Clínica: periodontograma | No mencionado | No evidenciado | https://www.oclusar.com.ar/ |
| Clínica: imágenes / radiografías | No mencionado (OCLU IA lee registros desde foto) | No evidenciado | https://www.oclusar.com.ar/ |
| Clínica: presupuestos y planes de tratamiento | Presupuestos con membrete; guía "de plan de tratamiento a cobro" | Declarado | https://www.oclusar.com.ar/guias |
| Admin: caja, cobros, señas | Cobros por Mercado Pago, efectivo, transferencia; seña opcional; tablero de cobrado/pendiente | Declarado | https://www.oclusar.com.ar/ |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | Factura desde el flujo de cobro; guía "Factura electrónica (CAE, ARCA y monotributo)" listada | Declarado | https://www.oclusar.com.ar/guias |
| Admin: Mercado Pago u otra pasarela | Mercado Pago para cobros y señas; política de privacidad lo cita como integración activada por el cliente | Declarado (home); mención en privacidad | https://www.oclusar.com.ar/privacidad |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | Seguimiento de lo presentado a cada obra social y su estado; no hay liquidación detallada | Declarado | https://www.oclusar.com.ar/ |
| Admin: reportes y métricas | Tablero de cobrado/pendiente; reportes de uso por sillón | Declarado | https://www.oclusar.com.ar/ |
| Integraciones: WhatsApp Business API (oficial o no oficial) | Click-to-chat oficial de WhatsApp, no API Business ni automatización | Comprobado | https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp |
| Integraciones: Google Calendar / sitio web / API pública | Google Calendar (citado también en privacidad); enlace público de reserva; API pública no mencionada | Declarado (Calendar); mención en privacidad | https://www.oclusar.com.ar/privacidad |
| Integraciones: radiología, firma digital, marketing | Firma electrónica de consentimientos desde el celular; ARCA; Instagram | Declarado | https://www.oclusar.com.ar/ |
| Operación: sucursales, roles y permisos, auditoría | Roles e invitaciones, asignación de sillón; sucursales no mencionadas; registros con profesional y marca de tiempo | Declarado | https://www.oclusar.com.ar/ |
| Operación: exportación de datos | Exportación completa; ventana de 90 días tras baja o falta de pago (3 meses si se discontinúa el servicio) | Comprobado (términos) | https://www.oclusar.com.ar/terminos |
| Operación: soporte y capacitación | Guías públicas; página de soporte referida en privacidad | Declarado | https://www.oclusar.com.ar/guias |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | Cifrado en tránsito, controles de acceso, autenticación, backups (frecuencia no indicada en privacidad; home declara diarios); medidas reforzadas para datos de salud | Comprobado (política de privacidad) | https://www.oclusar.com.ar/privacidad |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | Ley 25.326 (con rol de encargado de tratamiento), Ley 26.529, Ley 25.506; registro en interoperabilidad en salud; REFEPS; AAIP en trámite | Comprobado (privacidad cita 25.326 y 26.529); resto Declarado | https://www.oclusar.com.ar/privacidad |
| Comercial: prueba gratuita / plan gratuito | "Empezás gratis", sin tarjeta | Declarado | https://www.oclusar.com.ar/ |
| Comercial: modalidad de cobro y costos adicionales visibles | Mensual o anual, renovación automática; adicionales (profesionales extra, OCLU IA, almacenamiento); sin importes | Comprobado (términos) | https://www.oclusar.com.ar/terminos |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | Ninguno publicado | No evidenciado | https://www.oclusar.com.ar/ |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): marco legal y de datos muy explícito (https://www.oclusar.com.ar/privacidad y /terminos); política clara de exportación y retención; WhatsApp sin riesgo de bloqueo; enfoque local (ARCA, Mercado Pago, REFEPS) declarado.
- **Limitaciones** (con evidencia o por ausencia de evidencia): recordatorios manuales, sin automatización; sin lista de espera, periodontograma, imágenes ni campañas; sin precios; sin clientes ni reseñas verificables; razón social no identificada.
- **Diferencial real** (si lo hay): alineamiento explícito con el marco regulatorio argentino y decisión deliberada de usar WhatsApp oficial click-to-chat; alquiler de sillón.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 2 | Enlace público y recordatorio WhatsApp manual comprobados; el resto declarado y sin automatización. |
| Funcionalidad odontológica clínica | 20 % | 2 | HC y odontograma solo declarados o como títulos de guías; sin periodontograma ni imágenes. |
| Integraciones locales y WhatsApp | 15 % | 3 | WhatsApp click-to-chat oficial comprobado; ARCA, Mercado Pago y Calendar declarados y citados en privacidad. |
| Administración, cobros y facturación | 15 % | 2 | Cobros, ARCA y obras sociales declarados; sin detalle comprobado. |
| Experiencia del paciente | 10 % | 2 | Enlace de reserva comprobado; seña y confirmación declaradas. |
| Seguridad, exportación y trazabilidad | 10 % | 4 | Política de privacidad y términos concretos, exportación con plazos y marcos legales argentinos citados. |
| Precio y facilidad de adopción | 5 % | 3 | Plan inicial gratuito declarado y precios en pesos, pero sin montos. |

**Total ponderado: 2,40 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://www.oclusar.com.ar/ | oficial | 2026-11-23 |
| https://www.oclusar.com.ar/guias | ayuda | 2026-11-23 |
| https://www.oclusar.com.ar/guias/agenda-y-recordatorios-por-whatsapp | ayuda | 2026-11-23 |
| https://www.oclusar.com.ar/terminos | oficial (términos) | 2026-11-23 |
| https://www.oclusar.com.ar/privacidad | oficial (privacidad) | 2026-11-23 |
