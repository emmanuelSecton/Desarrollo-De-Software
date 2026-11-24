# Bilog

- **URL**: https://bilog.com.ar/
- **Empresa proveedora**: Bilog (razón social no evidenciada)
- **País de origen**: Argentina (el sitio menciona alcance nacional y la Ley 25.506)
- **Fecha de investigación**: 2026-11-23

<!-- Las 8 secciones siguientes son el esquema fijo de web-scraper: van siempre y en este orden. -->

## Problema que resuelve

Software de gestión odontológica con agenda, historia clínica con odontograma, administración y caja, y facturación a obras sociales; incluye una solución de auditoría para obras sociales y gerenciadoras.

## Usuarios objetivo

Consultorio, clínica multiprofesional, y además obras sociales/gerenciadoras (auditoría) y universidades (licencias académicas).

## Features detectadas

- Agenda diaria, semanal y por profesional; presentismo; sobreturnos y arrastre de turnos (declarado)
- Recordatorios por WhatsApp con respuesta "Confirmo/Cancelo"; SMS y email (declarado)
- Reserva online por QR o enlace (declarado); app móvil iOS/Android
- Ficha de paciente, odontograma, tratamientos, evolución, adjuntos, consentimientos con firma por QR/link
- Presupuestos, pagos, caja diaria, liquidación de honorarios y de obras sociales, KPIs
- Recetas electrónicas, galería de imágenes, asistente iAngela (IA)

## Pricing

- Freemium: sin costo; 1 profesional, 1 usuario, hasta 50 pacientes.
- Lite: (no publicado); hasta 5 profesionales, pacientes ilimitados, administración limitada; por usuario.
- Premium: (no publicado); profesionales ilimitados, 2 usuarios, administración completa; por usuario.
- Reserva online, galería de imágenes, recetas y recordatorios por WhatsApp: servicios aparte, sin precio publicado.
- Alta, migración y capacitación inicial sin costo; cobro mensual; sin permanencia. Moneda no especificada.
- Recordatorios por WhatsApp: packs de 100 a 600 recordatorios, por cotización.

## Posicionamiento

Más de 20 años en el mercado argentino, con soluciones por perfil (consultorios, clínicas, auditoría).

## Diferenciadores

(Afirmaciones comerciales) Más de 1.500 clínicas, 5.000 usuarios, 10 millones de pacientes, 150.000 turnos por mes; módulo de auditoría para obras sociales; plan Freemium; asistente iAngela.

## Notas del investigador

- No hay prueba por tiempo limitado, pero sí plan Freemium permanente (https://bilog.com.ar/planes/).
- La página de recordatorios es inconsistente: el plan Evolución habla de "envíos automatizados" y la FAQ dice que es manual (un clic por paciente), usando enlaces de chat de WhatsApp; no se declara API oficial. Se clasifica como no evidenciado/probablemente no oficial.
- Facturación electrónica y recetas: el sitio enlaza a páginas dedicadas, pero no se abrieron; no hay evidencia sobre ARCA. Sucursales no mencionadas en la página de clínicas.
- El centro de ayuda docs.bilog.com.ar existe pero el resumen solo mostró títulos de secciones.
- Búsqueda de reseñas en Capterra/GetApp sin resultados para Bilog.
- No hay API pública del producto (indicado en llms.txt del propio sitio).
- Las páginas se leyeron mediante WebFetch (resumen automático).

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | Web, escritorio y app móvil iOS/Android | Declarado | https://bilog.com.ar/llms.txt |
| Agenda: vista diaria/semanal | Vistas diaria, semanal y por profesional | Declarado | https://bilog.com.ar/agenda |
| Agenda: múltiples profesionales | Vista por profesional; plan Premium con profesionales ilimitados | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Agenda: múltiples sillones/boxes | Ocupación de sillones en agenda sincronizada, sin superposiciones; configuración no descrita | Declarado | https://bilog.com.ar/soluciones/clinicas |
| Agenda: duración variable por prestación | No mencionado | No evidenciado | https://bilog.com.ar/agenda |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | Sobreturnos y arrastre (home); coordinación sin superposiciones (clínicas) | Declarado | https://bilog.com.ar/soluciones/clinicas |
| Turnos: reserva online 24/7 / enlace / web / redes | Reserva por QR o enlace, incluida para pacientes nuevos; contratable aparte | Declarado | https://bilog.com.ar/llms.txt |
| Turnos: chatbot / WhatsApp | iAngela (IA) para cargar turnos y consultas por voz/texto; WhatsApp solo para recordatorios | Declarado | https://bilog.com.ar/ |
| Turnos: confirmación, cancelación, reprogramación por el paciente | Respuestas "Confirmo/Cancelo" actualizan la agenda; reserva/cancelación por QR declarada | Comprobado (confirmar/cancelar por respuesta, página de recordatorios) | https://bilog.com.ar/reminders-whatsapp |
| Turnos: lista de espera | No mencionado | No evidenciado | https://bilog.com.ar/agenda |
| Automatización: recordatorios y confirmaciones (canal) | WhatsApp (manual por clic, packs por volumen); SMS y email declarados | Comprobado (WhatsApp); resto Declarado | https://bilog.com.ar/reminders-whatsapp |
| Automatización: ausentes, recuperación, controles periódicos, campañas | Guía sobre ausentismo; funcionalidad de recuperación/campañas no descrita | No evidenciado | https://bilog.com.ar/recursos/reducir-ausentismo-pacientes-consultorio |
| Clínica: ficha clínica / anamnesis | Ficha de paciente, evolución, adjuntos; Freemium incluye historia clínica | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Clínica: odontograma | Incluido hasta en Freemium | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Clínica: periodontograma | No mencionado | No evidenciado | https://bilog.com.ar/pacientes |
| Clínica: imágenes / radiografías | Galería de imágenes (servicio aparte); iAngela asiste lectura radiográfica (declarado) | Declarado | https://bilog.com.ar/gallery |
| Clínica: presupuestos y planes de tratamiento | Presupuestos incluidos desde Freemium | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Admin: caja, cobros, señas | Caja diaria en centro de ayuda; pagos en planes; señas no mencionadas | Comprobado (ayuda y planes) | https://docs.bilog.com.ar/ |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | Existe un enlace a una página de factura electrónica; sin detalle ni mención de ARCA | No evidenciado | https://bilog.com.ar/llms.txt |
| Admin: Mercado Pago u otra pasarela | No mencionado | No evidenciado | https://bilog.com.ar/planes/ |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | Liquidación a obras sociales, presentación por convenio, débitos; módulo de auditoría para aseguradoras | Comprobado (página de clínicas con detalle; no hay video) | https://bilog.com.ar/soluciones/clinicas |
| Admin: reportes y métricas | Reportes, KPIs; reportes completos en Premium | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Integraciones: WhatsApp Business API (oficial o no oficial) | Enlaces de chat de WhatsApp, envío manual; API oficial no declarada | No evidenciado (API oficial) | https://bilog.com.ar/reminders-whatsapp |
| Integraciones: Google Calendar / sitio web / API pública | Sin API pública del producto (lo indica el propio sitio); sin Google Calendar | No evidenciado | https://bilog.com.ar/llms.txt |
| Integraciones: radiología, firma digital, marketing | Firma de consentimientos por QR/link (Ley 25.506 declarada); recetas electrónicas; radiología no integrada | Declarado | https://bilog.com.ar/ |
| Operación: sucursales, roles y permisos, auditoría | Roles y permisos por perfil, trazabilidad de movimientos (FAQ); sucursales no mencionadas; auditoría sin detalle (solo para aseguradoras) | Declarado (roles); permisos mencionados en ayuda | https://docs.bilog.com.ar/ |
| Operación: exportación de datos | No aparece | No evidenciado | https://docs.bilog.com.ar/ |
| Operación: soporte y capacitación | Soporte telefónico local; capacitación por rol; migración sin costo | Declarado | https://bilog.com.ar/planes/ |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | Control por roles y trazabilidad; sin medidas técnicas descritas | Declarado | https://bilog.com.ar/soluciones/clinicas |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | Solo Ley 25.506 (firma electrónica) | Declarado | https://bilog.com.ar/ |
| Comercial: prueba gratuita / plan gratuito | Plan Freemium permanente (1 profesional, 50 pacientes); sin prueba por tiempo | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Comercial: modalidad de cobro y costos adicionales visibles | Mensual por usuario; adicionales (reserva online, galería, recetas, WhatsApp) sin precio | Comprobado (planes) | https://bilog.com.ar/planes/ |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | Más de 1.500 clínicas y 150.000 turnos/mes (declarado); casos de clientes mencionados; sin reseñas en marketplaces | Declarado | https://bilog.com.ar/ |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): plan Freemium con odontograma e historia clínica (https://bilog.com.ar/planes/); liquidación a obras sociales con detalle (https://bilog.com.ar/soluciones/clinicas); confirmación/cancelación por respuesta de WhatsApp.
- **Limitaciones** (con evidencia o por ausencia de evidencia): precios de Lite/Premium no publicados; recordatorios WhatsApp manuales y de pago aparte; sin evidencia de ARCA, Mercado Pago, lista de espera, exportación ni API; sin reseñas externas.
- **Diferencial real** (si lo hay): módulo de auditoría para obras sociales y orientación a liquidación de obras sociales, poco común entre los demás relevados.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 3 | Agenda multiprofesional y confirmación por WhatsApp comprobadas; reserva online y sobreturnos solo declarados; recordatorios manuales. |
| Funcionalidad odontológica clínica | 20 % | 3 | HC, odontograma y presupuestos comprobados en planes; sin periodontograma. |
| Integraciones locales y WhatsApp | 15 % | 2 | WhatsApp por enlace de chat; sin ARCA, Mercado Pago ni API. |
| Administración, cobros y facturación | 15 % | 3 | Caja y liquidación de obras sociales sólidas; sin facturación electrónica evidenciada. |
| Experiencia del paciente | 10 % | 2 | Reserva por QR declarada; confirmación por WhatsApp comprobada. |
| Seguridad, exportación y trazabilidad | 10 % | 2 | Roles y trazabilidad declarados; sin exportación ni medidas técnicas. |
| Precio y facilidad de adopción | 5 % | 3 | Freemium permanente y alta sin costo; planes pagos sin precio. |

**Total ponderado: 2,65 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://bilog.com.ar/ | oficial | 2026-11-23 |
| https://bilog.com.ar/planes/ | precios | 2026-11-23 |
| https://bilog.com.ar/agenda | oficial | 2026-11-23 |
| https://bilog.com.ar/reminders-whatsapp | oficial | 2026-11-23 |
| https://bilog.com.ar/soluciones/clinicas | oficial | 2026-11-23 |
| https://bilog.com.ar/llms.txt | oficial | 2026-11-23 |
| https://docs.bilog.com.ar/ | ayuda | 2026-11-23 |
