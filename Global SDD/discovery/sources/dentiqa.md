# Dentiqa

- **URL**: https://dentiqa.app/
- **Empresa proveedora**: Violet Wave (sin razón social ni forma jurídica indicada)
- **País de origen**: Argentina (términos: ley argentina, tribunales de la Ciudad Autónoma de Buenos Aires)
- **Fecha de investigación**: 2026-11-23

<!-- Las 8 secciones siguientes son el esquema fijo de web-scraper: van siempre y en este orden. -->

## Problema que resuelve

Plataforma de gestión para clínicas odontológicas en Latinoamérica: agenda multi-dentista, historia clínica odontológica, recordatorios por WhatsApp y un chatbot de IA 24/7 que agenda turnos y registra pacientes nuevos.

## Usuarios objetivo

Clínicas odontológicas (consultorio a clínica con varios profesionales) y, en el plan Enterprise, "clínicas grandes y grupos / cadenas". Roles: Dueño, Admin, Dentista y Recepción. Cupo de 10 clínicas fundadoras en Argentina y 10 en el resto de Latinoamérica.

## Features detectadas

- Agenda multi-dentista con bloqueos por profesional, vistas día y semana, reprogramación.
- Recordatorios por WhatsApp 24 h y 2 h antes; chatbot de IA por WhatsApp (nace apagado).
- Odontograma versionado, periodontograma (BOP/NIC), planes de tratamiento, evoluciones, consentimientos, galería de imágenes, indicaciones.
- CRM con pipeline de 7 etapas y campañas por WhatsApp (cumpleaños, reactivación, posttratamiento).
- Estadísticas de citas, ingresos, tratamientos y rendimiento por dentista.
- Firma electrónica, multiusuario y multiclínica (Enterprise); "Coach de IA".

## Pricing

- Starter: USD 89/mes (5 usuarios, 2.000 mensajes WhatsApp, 2.000 interacciones IA).
- Professional: USD 149/mes (10 usuarios, 5.000 mensajes, 5.000 interacciones IA).
- Enterprise: USD 249/mes (usuarios ilimitados, 10.000 mensajes, 10.000 interacciones IA).
- Cobro en moneda local vía Mercado Pago, convertido al tipo de cambio vigente al iniciar la suscripción; referencia en la landing: ≈ $140.000 / $235.000 / $390.000 (ARS, aproximado, "del momento").
- Mes a mes, sin permanencia; pagos no reembolsables (términos).

## Posicionamiento

"Dentiqa" se presenta como plataforma odontológica integral con IA y WhatsApp oficial para clínicas de Latinoamérica, con implementación incluida.

## Diferenciadores

(Afirmación comercial) Chatbot de IA que agenda por WhatsApp (reglas + modelos Claude Haiku/Sonnet), integración oficial con WhatsApp Business API (proveedor verificado por Meta), odontograma versionado y periodontograma, implementación con migración y capacitación incluidas.

## Notas del investigador

- **Contradicción**: la landing dice que no hay prueba gratuita de software (solo demo gratuita), pero los Términos de Servicio indican "14 días de prueba gratuita". Verificar con el proveedor.
- La landing no es consistente en la lista de países (precios, FAQ y pie de página difieren).
- No se publica cantidad de clientes; las cifras visibles son de una captura de dashboard de ejemplo.
- No hay centro de ayuda; el contacto es por WhatsApp. No se encontró canal de YouTube ni reseñas en marketplaces.
- La política de privacidad (actualizada 17-09-2026) indica que la plataforma "aún no ofrece exportación automática de datos" y que la portabilidad se gestiona manualmente.
- Los turnos online para pacientes no aparecen como función (solo un enlace "Agenda dental online" en el pie). Facturación, ARCA/AFIP, obras sociales y Mercado Pago (para cobros de la clínica) no se mencionan; Mercado Pago se usa solo para cobrar la suscripción a Dentiqa.
- Fuera de lo leído en la landing y los textos legales no se verificó el producto funcionando.

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | SaaS web ("100% responsive"); hospedaje en Vercel/Railway, base en Supabase (sa-east-1) | Comprobado (política de privacidad) | https://dentiqa.app/legal/politica-de-privacidad |
| Agenda: vista diaria/semanal | Vistas día y semana | Declarado | https://dentiqa.app/ |
| Agenda: múltiples profesionales | Multi-dentista con bloqueos por profesional | Declarado | https://dentiqa.app/ |
| Agenda: múltiples sillones/boxes | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Agenda: duración variable por prestación | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | Bloqueos por profesional y reprogramación; sobreturnos y solapamientos no evidenciados | Declarado | https://dentiqa.app/ |
| Turnos: reserva online 24/7 / enlace / web / redes | Solo vía chatbot de WhatsApp; portal/enlace web no evidenciado | Declarado | https://dentiqa.app/ |
| Turnos: chatbot / WhatsApp | Chatbot de IA 24/7 que agenda y registra pacientes; cupos y apagado por plan en los términos | Declarado (cupos en términos: Comprobado) | https://dentiqa.app/ ; https://dentiqa.app/legal/terminos-de-servicio |
| Turnos: confirmación, cancelación, reprogramación por el paciente | Reprogramación y recordatorios; acciones del paciente no detalladas | Declarado | https://dentiqa.app/ |
| Turnos: lista de espera | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Automatización: recordatorios y confirmaciones (canal) | WhatsApp 24 h y 2 h antes; los términos confirman que recordatorios y confirmaciones siguen enviándose al agotar el cupo | Declarado (términos: Comprobado) | https://dentiqa.app/ ; https://dentiqa.app/legal/terminos-de-servicio |
| Automatización: ausentes, recuperación, controles periódicos, campañas | Campañas WhatsApp (cumpleaños, reactivación, posttratamiento), pipeline CRM de 7 etapas; ausentes no evidenciado | Declarado | https://dentiqa.app/ |
| Clínica: ficha clínica / anamnesis | Alergias, medicación, antecedentes, grupo sanguíneo, notas clínicas, evoluciones, consentimientos | Comprobado (política de privacidad lista los datos) | https://dentiqa.app/legal/politica-de-privacidad |
| Clínica: odontograma | Odontograma versionado | Declarado | https://dentiqa.app/ |
| Clínica: periodontograma | Con métricas BOP/NIC | Declarado | https://dentiqa.app/ |
| Clínica: imágenes / radiografías | Galería de imágenes; validación de archivos por MIME y magic bytes | Declarado (validación: política) | https://dentiqa.app/ ; https://dentiqa.app/legal/politica-de-privacidad |
| Clínica: presupuestos y planes de tratamiento | Planes de tratamiento; presupuestos no mencionados | Declarado | https://dentiqa.app/ |
| Admin: caja, cobros, señas | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Admin: Mercado Pago u otra pasarela | Mercado Pago solo para cobrar la suscripción a Dentiqa; no para cobros de la clínica | No evidenciado (para la clínica) | https://dentiqa.app/legal/terminos-de-servicio |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Admin: reportes y métricas | Estadísticas de citas, ingresos, tratamientos, rendimiento por dentista | Declarado | https://dentiqa.app/ |
| Integraciones: WhatsApp Business API (oficial o no oficial) | Oficial: WhatsApp Cloud API de Meta (proveedor verificado por Meta, declarado); Meta figura como subprocesador | Declarado (subprocesador: política) | https://dentiqa.app/ ; https://dentiqa.app/legal/politica-de-privacidad |
| Integraciones: Google Calendar / sitio web / API pública | No evidenciado | No evidenciado | https://dentiqa.app/ |
| Integraciones: radiología, firma digital, marketing | Firma electrónica y campañas propias (declarado); radiología no evidenciada | Declarado | https://dentiqa.app/ |
| Operación: sucursales, roles y permisos, auditoría | Multiclínica (Enterprise), roles (Dueño/Admin/Dentista/Recepción), JWT con roles, audit logs de cambios clínicos (10 años) y de seguridad (90 días / 12 meses) | Comprobado (política de privacidad) | https://dentiqa.app/legal/politica-de-privacidad |
| Operación: exportación de datos | Sin exportación automática; portabilidad manual a pedido | Comprobado (política de privacidad) | https://dentiqa.app/legal/politica-de-privacidad |
| Operación: soporte y capacitación | Implementación incluida (onboarding, migración, capacitación, soporte prioritario el primer mes); contacto por WhatsApp; sin centro de ayuda | Declarado | https://dentiqa.app/ |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | TLS, AES-256-GCM para tokens de WhatsApp, aislamiento por clínica, bloqueo tras 5 intentos, rate limiting, respaldos automáticos PostgreSQL | Comprobado (política de privacidad; respaldo: Declarado) | https://dentiqa.app/legal/politica-de-privacidad |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | La política no cita la Ley 25.326 ni la 26.529; solo "regulaciones locales"; sin certificaciones; la clínica es responsable del consentimiento del paciente | Comprobado (ausencia de cita) | https://dentiqa.app/legal/politica-de-privacidad ; https://dentiqa.app/legal/terminos-de-servicio |
| Comercial: prueba gratuita / plan gratuito | Landing: sin prueba, solo demo gratuita; términos: 14 días de prueba (contradicción) | Comprobado (conflicto entre fuentes) | https://dentiqa.app/ ; https://dentiqa.app/legal/terminos-de-servicio |
| Comercial: modalidad de cobro y costos adicionales visibles | Mensual en USD cobrado en moneda local vía Mercado Pago; sin permanencia; cupos de WhatsApp e IA por plan (marketing suspendido al agotar cupo, sin cargos por excedente); sin reembolsos | Comprobado (precios y términos) | https://dentiqa.app/#precios ; https://dentiqa.app/legal/terminos-de-servicio |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | Ninguno; cupo de 10+10 clínicas fundadoras | No evidenciado | https://dentiqa.app/ |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): producto argentino con cobro vía Mercado Pago y términos bajo ley argentina; política de privacidad concreta (subprocesadores, ubicación de datos en sa-east-1, retención, auditoría con retención de 10 años); transparencia sobre cupos de WhatsApp/IA y comportamiento al agotarse.
- **Limitaciones** (con evidencia o por ausencia de evidencia): precio de entrada alto (USD 89/mes) frente a SMILE; sin facturación ARCA, caja, obras sociales ni Mercado Pago para la clínica; sin reserva online por enlace; sin exportación automática; no cita Ley 25.326; contradicción sobre prueba gratuita; sin ayuda pública ni clientes verificables; mucho de lo funcional es solo declarado.
- **Diferencial real** (si lo hay): chatbot de agenda por WhatsApp con IA, con reglas y cupos explícitos, y periodontograma/odontograma versionado; lo comprobado es la transparencia contractual, no el producto.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 2 | Agenda multi-dentista, recordatorios y chatbot declarados; solo los cupos están en términos. |
| Funcionalidad odontológica clínica | 20 % | 2 | Odontograma versionado y periodontograma declarados, sin documentación ni demo pública. |
| Integraciones locales y WhatsApp | 15 % | 2 | WhatsApp Cloud API oficial; sin ARCA, obras sociales ni Mercado Pago para la clínica. |
| Administración, cobros y facturación | 15 % | 1 | Solo estadísticas declaradas; sin caja ni facturación. |
| Experiencia del paciente | 10 % | 2 | Chatbot de WhatsApp para agendar; sin portal ni autogestión evidenciados. |
| Seguridad, exportación y trazabilidad | 10 % | 3 | Auditoría, aislamiento y bloqueo documentados en política; exportación solo manual. |
| Precio y facilidad de adopción | 5 % | 2 | USD 89/mes mínimo, demo e implementación incluida; prueba gratuita contradictoria. |

**Ponderado preliminar: 1,95 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://dentiqa.app/ | oficial / precios | 2026-11-23 |
| https://dentiqa.app/legal/politica-de-privacidad | oficial (privacidad) | 2026-11-23 |
| https://dentiqa.app/legal/terminos-de-servicio | oficial (términos) | 2026-11-23 |
