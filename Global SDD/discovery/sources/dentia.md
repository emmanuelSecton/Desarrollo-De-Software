# Dentia

- **URL**: https://dentia.tech/
- **Empresa proveedora**: Brezzo Matías Alejandro (persona humana, CUIT 20-38108051-0, según términos y condiciones)
- **País de origen**: Argentina (Villa María, Córdoba)
- **Fecha de investigación**: 2026-11-23

## Problema que resuelve

Software de gestión para clínicas odontológicas "hecho en Argentina" con asistente de IA que atiende por WhatsApp, agenda y cancela turnos, y gestión clínica y administrativa (odontograma, facturación ARCA, obras sociales).

## Usuarios objetivo

Consultorios y clínicas odontológicas, desde pequeños (2 usuarios) hasta multisede con usuarios ilimitados.

## Features detectadas

- Agenda, sala de espera, sobreturnos, multisede, sincronización con Google Calendar, app instalable
- Asistente IA por WhatsApp (notas de voz, disponibilidad real, seña a pacientes nuevos); CRM WhatsApp/Instagram/Messenger
- Odontograma y periodontograma con dictado, presupuestos, consentimiento informado, firma electrónica, órdenes de laboratorio, inventario
- Facturación ARCA (A, B y C, notas de crédito/débito, alerta de tope de monotributo), liquidaciones por obra social/prepaga, Mercado Pago
- Recordatorios por WhatsApp o email, cumpleaños, pedido de reseña, avisos de cuotas
- Herramientas de IA clínica (cefalometría, panorámica, diseño de sonrisa)

## Pricing

- Básico: $20.000 por mes o $200.000 por año (2 usuarios, 1 sede)
- Pro: $30.000 por mes o $300.000 por año (10 usuarios, 3 sedes; agrega CRM de mensajería)
- Elite: $40.000 por mes o $400.000 por año (usuarios y sedes ilimitados; asistente IA 24/7, seña, campañas de recall)
- Moneda: "pesos" con signo "$"; el código ISO no figura. Anual = dos meses gratis; sin contrato; baja en cualquier momento. Términos: impuestos incluidos.

## Posicionamiento

Sistema odontológico argentino con IA en WhatsApp, a precio de entrada bajo y sin contrato.

## Diferenciadores

Afirmaciones comerciales: asistente por WhatsApp que responde audios y pide seña; 17 perfiles de permisos; firma electrónica con hash SHA-256 y QR; herramientas de IA clínica; prueba de 30 días con todas las funciones.

## Notas del investigador

- Proveedor aparente: una persona humana (monotributista/autónomo), no una sociedad; riesgo de continuidad a considerar. Sin fecha de fundación.
- Evidencia de WhatsApp: los términos y condiciones declaran uso de la API WhatsApp Cloud de Meta, con Dentia como Tech Provider y Embedded Signup; es documento legal propio, pero no verificación de Meta. El listado de subprocesadores nombra solo a Meta Platforms.
- Los subprocesadores revelan que los datos de la plataforma residen en Hostinger (Brasil), imágenes en AWS (EE. UU.) y que la IA corre en Microsoft Azure, OpenAI y otros (global); el propio sitio admite que 16 de 18 proveedores están en EE. UU. o Brasil. Relevante para Ley 25.326 (transferencia internacional).
- Sin centro de ayuda dedicado (FAQ en la landing) ni videos; por eso las funciones son casi todas "Declarado". La reserva online pública para pacientes no está descripta (solo vía WhatsApp).
- Adopción: "+100 clínicas" es una afirmación sin lista, testimonios ni reseñas; la búsqueda en marketplaces no devolvió reseñas de Dentia. La prueba de 30 días figura en la landing, pero los términos no la mencionan.
- No se abrieron privacidad, DPA ni eliminación de datos.

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | SaaS web + app instalable; hosting en Hostinger y AWS | Comprobado (subprocesadores) | https://dentia.tech/subprocesadores |
| Agenda: vista diaria/semanal | Sin detalle | No evidenciado | https://dentia.tech/ |
| Agenda: múltiples profesionales | Planes con usuarios múltiples; agenda por profesional sin detalle | Declarado | https://dentia.tech/#precios |
| Agenda: múltiples sillones/boxes | Sin evidencia (solo multisede) | No evidenciado | https://dentia.tech/ |
| Agenda: duración variable por prestación | Sin evidencia | No evidenciado | https://dentia.tech/ |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | Sobreturnos y sala de espera declarados | Declarado | https://dentia.tech/ |
| Turnos: reserva online 24/7 / enlace / web / redes | Solo vía asistente de WhatsApp; enlace público no descripto | Declarado | https://dentia.tech/ |
| Turnos: chatbot / WhatsApp | Asistente IA 24/7 (plan Elite) con audios y voz | Declarado | https://dentia.tech/#precios |
| Turnos: confirmación, cancelación, reprogramación por el paciente | Reprograma y cancela por WhatsApp | Declarado | https://dentia.tech/ |
| Turnos: lista de espera | "Sala de espera" (en clínica); lista de espera de turnos sin evidencia | Declarado (parcial) | https://dentia.tech/ |
| Automatización: recordatorios y confirmaciones (canal) | WhatsApp o email | Declarado | https://dentia.tech/ |
| Automatización: ausentes, recuperación, controles periódicos, campañas | Campañas de recall (Elite), cumpleaños, reseñas Google, alertas de cuotas | Declarado | https://dentia.tech/#precios |
| Clínica: ficha clínica / anamnesis | Historia clínica, consentimiento informado, firma electrónica | Declarado | https://dentia.tech/ |
| Clínica: odontograma | Con dictado por voz | Declarado | https://dentia.tech/ |
| Clínica: periodontograma | Con dictado por voz | Declarado | https://dentia.tech/ |
| Clínica: imágenes / radiografías | Almacenamiento en AWS; IA sobre panorámica y cefalometría | Declarado | https://dentia.tech/subprocesadores |
| Clínica: presupuestos y planes de tratamiento | Planes de tratamiento y presupuestos | Declarado | https://dentia.tech/ |
| Admin: caja, cobros, señas | Seña a pacientes nuevos por asistente (Elite); caja sin detalle | Declarado | https://dentia.tech/ |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | Facturas A, B, C, notas; ARCA listada como proveedor | Declarado (ARCA en subprocesadores: Comprobado como proveedor) | https://dentia.tech/subprocesadores |
| Admin: Mercado Pago u otra pasarela | Cobro integrado; Mercado Pago listado como proveedor (pagos y suscripciones) | Declarado | https://dentia.tech/ |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | Liquidaciones por obra social y prepaga | Declarado | https://dentia.tech/ |
| Admin: reportes y métricas | Sin detalle | No evidenciado | https://dentia.tech/ |
| Integraciones: WhatsApp Business API (oficial o no oficial) | API WhatsApp Cloud de Meta; Tech Provider; Embedded Signup | Comprobado (términos legales) | https://dentia.tech/terminos-y-condiciones |
| Integraciones: Google Calendar / sitio web / API pública | Google Calendar declarado; API pública no | Declarado | https://dentia.tech/subprocesadores |
| Integraciones: radiología, firma digital, marketing | Firma con SHA-256 y QR; CRM Instagram/Messenger; audiencias Meta opcionales | Declarado | https://dentia.tech/ |
| Operación: sucursales, roles y permisos, auditoría | Multisede, 17 perfiles, registro de auditoría | Declarado | https://dentia.tech/ |
| Operación: exportación de datos | Exportación en formato común durante 30 días tras la baja; exportación en cualquier momento declarada | Comprobado (términos) | https://dentia.tech/terminos-y-condiciones |
| Operación: soporte y capacitación | Contacto por WhatsApp; sin centro de ayuda | Declarado | https://dentia.tech/ |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | 2FA y permisos declarados; backups no mencionados en términos; transferencia internacional reconocida | Declarado | https://dentia.tech/subprocesadores |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | Ley 25.326; documentos de acuerdo de tratamiento de datos, subprocesadores y eliminación | Declarado | https://dentia.tech/ |
| Comercial: prueba gratuita / plan gratuito | 30 días con todas las funciones (landing); no figura en términos | Declarado | https://dentia.tech/ |
| Comercial: modalidad de cobro y costos adicionales visibles | Mensual o anual; impuestos incluidos; sin reembolso por días no usados; arrepentimiento 10 días; costos de mensajes de Meta no aclarados | Comprobado | https://dentia.tech/terminos-y-condiciones |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | "+100 clínicas" sin lista; sin reseñas | Declarado | https://dentia.tech/ |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): documentación legal inusualmente transparente (subprocesadores, DPA, baja, arrepentimiento, exportación post-baja); WhatsApp Cloud API oficial declarada en términos; precios publicados bajos con moneda en pesos.
- **Limitaciones** (con evidencia o por ausencia de evidencia): titular persona humana; sin centro de ayuda, videos ni reseñas, por lo que casi todas las funciones solo están declaradas; datos alojados en Brasil/EE. UU. y procesados por IA global; asistente WhatsApp solo en plan Elite; sin reportes ni vistas de agenda descriptos.
- **Diferencial real** (si lo hay): combinación de precio de entrada publicado ($20.000) con facturación ARCA, Mercado Pago y WhatsApp oficial, aunque en su mayoría declarada.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 2 | Funciones amplias pero solo declaradas en landing; sin documentación de agenda. |
| Funcionalidad odontológica clínica | 20 % | 2 | Odontograma, periodontograma y presupuestos solo declarados. |
| Integraciones locales y WhatsApp | 15 % | 3 | WhatsApp Cloud API comprobada en términos; ARCA y Mercado Pago como proveedores listados. |
| Administración, cobros y facturación | 15 % | 2 | ARCA, obras sociales y cobros declarados sin documentación. |
| Experiencia del paciente | 10 % | 2 | Atención por WhatsApp con audios declarada; sin reserva web. |
| Seguridad, exportación y trazabilidad | 10 % | 3 | Exportación y titularidad en términos, subprocesadores y DPA; auditoría y 2FA declarados. |
| Precio y facilidad de adopción | 5 % | 4 | Precios publicados, sin contrato, prueba de 30 días declarada; sin plan gratuito. |

**Puntaje ponderado preliminar: 2,35 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://dentia.tech/ | oficial / precios | 2026-11-23 |
| https://dentia.tech/terminos-y-condiciones | oficial (legal) | 2026-11-23 |
| https://dentia.tech/subprocesadores | oficial (legal) | 2026-11-23 |
| Búsqueda web de reseñas Dentia (sin resultados) | marketplace / reseña | 2026-11-23 |
