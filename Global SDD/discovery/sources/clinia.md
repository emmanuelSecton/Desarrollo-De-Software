# ClinIA (módulo OdontoClinIA)

- **URL**: https://www.clinia.com.ar/odontologia
- **Empresa proveedora**: EMPRINET 4.0 (marca ClinIA registrada ante INPI según el propio sitio)
- **País de origen**: Argentina (Córdoba, según clientes municipales y contacto)
- **Fecha de investigación**: 2026-11-23

## Problema que resuelve

Plataforma digital sanitaria "diseñada para centros de salud públicos y privados". El módulo de odontología agrega turnero, historia clínica odontológica, odontograma, facturación a obras sociales/prepagas y cobros particulares dentro de una plataforma de gestión de salud más amplia.

## Usuarios objetivo

Centros de salud municipales, hospitales públicos, clínicas PyME y cadenas multisede. Es una plataforma de salud general con un módulo odontológico (se cotiza por separado), no un producto exclusivamente odontológico.

## Features detectadas

- Agenda por profesional, especialidad o sillón/consultorio; recordatorios por WhatsApp y email
- Asistente de IA por WhatsApp para reservar, confirmar, cancelar y reprogramar
- Turnero web y app para pacientes
- Odontograma FDI (52 piezas, por superficie, versionado, exporta PDF)
- Historia clínica con CIE-10 y adjuntos
- Planes de tratamiento con honorarios según nomenclador
- Verificación de cobertura (PUCO), facturación a obras sociales/prepagas, facturación electrónica ARCA
- Caja en pesos, dólares o euros; recibos PDF por WhatsApp
- Receta electrónica (ReNaPDiS ID 248, HL7 FHIR); módulos PAMI y SUMAR+; panel multisede

## Pricing

- Abono: **(no publicado)**; se cotiza por institución y módulos contratados. No hay costo por usuario ni de migración (declarado)
- La página muestra "$" sin indicar moneda para los costos en cero. Soporte 24/7, actualizaciones y backups cada 10 minutos incluidos en el abono (declarado)

## Posicionamiento

Plataforma sanitaria argentina para el ámbito público y privado, con foco en PAMI/SUMAR+ y obras sociales; la odontología es una vertical.

## Diferenciadores

Afirmaciones comerciales: único sistema de ocho comparados que informa facturación PAMI y SUMAR+; receta electrónica registrada en ReNaPDiS (ID 248); verificación de cobertura PUCO/RENAPER; migración sin costo en menos de 24 horas.

## Notas del investigador

- No se pudo consultar /catalogo-funcional.pdf ni /privacidad ni /terminos (no se abrieron en esta pasada): la verificación de funciones es solo desde landing, FAQ, seguridad y "sobre".
- La página /recursos/mejor-software-gestion-clinicas-argentina es un comparativo publicado por el propio proveedor; sus datos de competidores no son independientes.
- La FAQ no describe odontograma/periodontograma; solo la landing odontológica lo hace. Periodontograma, imágenes y exportación de datos: sin evidencia.
- El proveedor aclara que el registro ReNaPDiS cubre solo el componente de recetas, no todo el software.
- WhatsApp: ninguna página indica si es API oficial de Meta o no.
- Reseñas independientes en marketplaces: no se buscaron específicamente (límite de fetches); no hay testimonios publicados en el sitio.
- Cifras de adopción internas: más de 500 centros de atención, 1,2 millones de pacientes, 15 municipios con PAMI (octubre 2026); el sitio admite que las categorías no son equivalentes y no hay desglose de clínicas odontológicas. Lanzamiento al mercado: enero 2023.

## Relevamiento detallado

Estado de cada fila:
- **Comprobado**: se ve en documentación pública, centro de ayuda, capturas o videos oficiales que muestran el producto funcionando, o en la página de precios.
- **Declarado**: solo aparece como afirmación de marketing en la landing.
- **No evidenciado**: no se encontró evidencia pública.

| Dimensión | Detalle | Estado | Fuente (URL) |
|---|---|---|---|
| Modalidad (SaaS / nube / app móvil / local / híbrida) | Navegador, sin instalación; instancia propia con subdominio por cliente; app para pacientes | Declarado | https://www.clinia.com.ar/odontologia ; https://www.clinia.com.ar/faq |
| Agenda: vista diaria/semanal | Sin detalle de vistas | No evidenciado | https://www.clinia.com.ar/odontologia |
| Agenda: múltiples profesionales | Agenda por profesional/especialidad | Declarado | https://www.clinia.com.ar/odontologia |
| Agenda: múltiples sillones/boxes | Agenda por sillón/consultorio | Declarado | https://www.clinia.com.ar/odontologia |
| Agenda: duración variable por prestación | Sin evidencia | No evidenciado | https://www.clinia.com.ar/odontologia |
| Agenda: bloqueos, sobreturnos, prevención de solapamientos | El asistente valida contra la agenda real; bloqueos/sobreturnos no descriptos | Declarado (parcial) | https://www.clinia.com.ar/odontologia |
| Turnos: reserva online 24/7 / enlace / web / redes | "Turnero web y app para pacientes"; sin detalle del flujo | Declarado | https://www.clinia.com.ar/faq |
| Turnos: chatbot / WhatsApp | Asistente IA por WhatsApp; deriva a persona | Declarado | https://www.clinia.com.ar/odontologia |
| Turnos: confirmación, cancelación, reprogramación por el paciente | Vía asistente de WhatsApp | Declarado | https://www.clinia.com.ar/odontologia |
| Turnos: lista de espera | Sin evidencia | No evidenciado | https://www.clinia.com.ar/odontologia |
| Automatización: recordatorios y confirmaciones (canal) | WhatsApp y email | Declarado | https://www.clinia.com.ar/odontologia |
| Automatización: ausentes, recuperación, controles periódicos, campañas | Sin evidencia | No evidenciado | https://www.clinia.com.ar/odontologia |
| Clínica: ficha clínica / anamnesis | Historia clínica odontológica con CIE-10, adjuntos, resumen IA | Declarado | https://www.clinia.com.ar/odontologia |
| Clínica: odontograma | FDI, 52 piezas, por superficie, versionado, PDF, compatible con tablet | Declarado | https://www.clinia.com.ar/odontologia |
| Clínica: periodontograma | Sin evidencia | No evidenciado | https://www.clinia.com.ar/faq |
| Clínica: imágenes / radiografías | Adjuntos en historia; imágenes radiológicas sin evidencia | No evidenciado | https://www.clinia.com.ar/faq |
| Clínica: presupuestos y planes de tratamiento | Honorarios estimados según nomenclador | Declarado | https://www.clinia.com.ar/odontologia |
| Admin: caja, cobros, señas | Caja en ARS/USD/EUR, recibos PDF, cierre de caja; señas no descriptas | Declarado | https://www.clinia.com.ar/odontologia |
| Admin: facturación electrónica (AFIP/ARCA u otra local) | Integración ARCA "según configuración contratada" | Declarado | https://www.clinia.com.ar/faq |
| Admin: Mercado Pago u otra pasarela | No mencionado | No evidenciado | https://www.clinia.com.ar/faq |
| Admin: obras sociales / prepagas / aseguradoras y liquidaciones | Convenios, nomenclador, PUCO, PAMI, SUMAR+; no garantiza cobro | Declarado | https://www.clinia.com.ar/faq |
| Admin: reportes y métricas | Panel único multisede; sin detalle | Declarado | https://www.clinia.com.ar/odontologia |
| Integraciones: WhatsApp Business API (oficial o no oficial) | No se informa el tipo de integración | No evidenciado | https://www.clinia.com.ar/faq |
| Integraciones: Google Calendar / sitio web / API pública | Sin evidencia (HL7 FHIR solo para recetas) | No evidenciado | https://www.clinia.com.ar/odontologia |
| Integraciones: radiología, firma digital, marketing | Sin evidencia | No evidenciado | https://www.clinia.com.ar/odontologia |
| Operación: sucursales, roles y permisos, auditoría | Multisede declarado; acceso "limitado según función", sin detalle de roles ni auditoría | Declarado (parcial) | https://www.clinia.com.ar/seguridad |
| Operación: exportación de datos | No mencionada | No evidenciado | https://www.clinia.com.ar/faq |
| Operación: soporte y capacitación | Soporte 24/7 incluido; WhatsApp/email en horario L-V 8-16 h | Declarado | https://www.clinia.com.ar/odontologia |
| Seguridad: datos de salud, privacidad, respaldo, control de acceso | Backups cada 10 min (declarado); HTTPS en el sitio; sin hosting informado | Declarado | https://www.clinia.com.ar/seguridad |
| Cumplimiento declarado (Ley 25.326, Ley 26.529, HC electrónica, HIPAA/GDPR, etc.) | Insignia Ley 25.326 sin detalle; ReNaPDiS solo para recetas; Ley 26.529 no mencionada | Declarado | https://www.clinia.com.ar/seguridad |
| Comercial: prueba gratuita / plan gratuito | Sin prueba; demo gratuita de 45 min | Declarado | https://www.clinia.com.ar/odontologia |
| Comercial: modalidad de cobro y costos adicionales visibles | Abono por institución y módulos; sin precio; $0 por usuario, migración y servidores | Declarado | https://www.clinia.com.ar/faq |
| Adopción: casos de éxito, nº de clientes publicado, reseñas verificables | +500 centros, 1,2 M pacientes, 15 municipios PAMI (datos internos); sin testimonios ni reseñas | Declarado | https://www.clinia.com.ar/sobre-clinia |

## Fortalezas, limitaciones y diferenciales evidentes

- **Fortalezas** (con evidencia): trayectoria de producto desde 2021/2023 en el sector público y privado; registro ReNaPDiS verificable para recetas; amplitud de facturación a financiadores (PAMI, SUMAR+, obras sociales) declarada.
- **Limitaciones** (con evidencia o por ausencia de evidencia): precio no publicado y sin prueba gratuita; sin evidencia de periodontograma, imágenes, exportación ni Mercado Pago; tipo de API de WhatsApp no informado; el módulo odontológico es una vertical de una plataforma generalista.
- **Diferencial real** (si lo hay): integración con circuitos públicos argentinos (PUCO, PAMI, SUMAR+). Menos relevante para un consultorio odontológico privado.

## Puntuación preliminar (0-5)

Criterio: 0 = no evidenciado · 1-2 = básico o solo declarado · 3 = sólido y comprobado · 4-5 = superior y comprobado. Lo que solo esté "Declarado" no puede puntuar más de 2.

| Criterio | Peso | Puntaje | Justificación (1 línea) |
|---|---|---|---|
| Gestión de turnos y automatización | 25 % | 2 | Agenda y asistente WhatsApp declarados; sin lista de espera ni campañas ni documentación. |
| Funcionalidad odontológica clínica | 20 % | 2 | Odontograma FDI declarado; sin periodontograma ni imágenes. |
| Integraciones locales y WhatsApp | 15 % | 2 | ARCA, PUCO y ReNaPDiS declarados; WhatsApp sin tipo de API; sin Mercado Pago. |
| Administración, cobros y facturación | 15 % | 2 | Caja, ARCA y financiadores declarados, sin documentación. |
| Experiencia del paciente | 10 % | 2 | Turnero web/app y WhatsApp declarados sin detalle del flujo. |
| Seguridad, exportación y trazabilidad | 10 % | 1 | Backups declarados; sin exportación, auditoría ni roles descriptos. |
| Precio y facilidad de adopción | 5 % | 1 | Sin precio ni prueba; solo demo; migración gratuita declarada. |

**Puntaje ponderado preliminar: 1,85 / 5**

## Fuentes consultadas

| URL | Tipo (oficial / precios / ayuda / video / marketplace / reseña) | Fecha de consulta |
|---|---|---|
| https://www.clinia.com.ar/odontologia | oficial | 2026-11-23 |
| https://www.clinia.com.ar/faq | oficial / ayuda | 2026-11-23 |
| https://www.clinia.com.ar/seguridad | oficial | 2026-11-23 |
| https://www.clinia.com.ar/sobre-clinia | oficial | 2026-11-23 |
| https://www.clinia.com.ar/recursos/mejor-software-gestion-clinicas-argentina | oficial (comparativo propio) | 2026-11-23 |
