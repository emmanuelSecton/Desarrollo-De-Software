# Global SDD — Gestión odontológica: Base de Conocimiento

Base de conocimiento construida en modo interactivo a partir del Discovery (`discovery/discovery.md`) y del estudio de mercado de 20 sistemas (`discovery/informe-discovery.md`).

## Índice de Archivos

| Archivo | Contenido |
|---------|-----------|
| [01_vision_y_objetivos.md](01_vision_y_objetivos.md) | Propósito, objetivos por actor, alcance del MVP, fuera de alcance, métricas |
| [02_descripcion_general.md](02_descripcion_general.md) | Stack, arquitectura general, integraciones, resumen de la API REST |
| [03_actores_y_roles.md](03_actores_y_roles.md) | Actores, matriz RBAC, rutas públicas |
| [04_modelo_de_datos.md](04_modelo_de_datos.md) | Dominios, ERD, entidades con constraints, seed inicial |
| [05_reglas_de_negocio.md](05_reglas_de_negocio.md) | Reglas RN-SEG, AG, TU, NO, LE, HC, CA, DP y GL |
| [06_funcionalidades.md](06_funcionalidades.md) | 7 épicas, historias US-001 a US-062 (incluye US-033) con criterios de aceptación |
| [07_flujos_principales.md](07_flujos_principales.md) | Reserva online, recordatorio WhatsApp, sin confirmar, lista de espera, atención, auth, exportación |
| [08_arquitectura_propuesta.md](08_arquitectura_propuesta.md) | Patrones, estructura de directorios, seguridad, variables de entorno, despliegue por instancia |
| [09_decisiones_y_supuestos.md](09_decisiones_y_supuestos.md) | Decisiones DD-01 a DD-10, supuestos SU-01 a SU-06, riesgos R-01 a R-06 |
| [10_preguntas_abiertas.md](10_preguntas_abiertas.md) | Inconsistencias IN-01/02 y preguntas PA-01 a PA-12 priorizadas |
| [11_integracion_whatsapp.md](11_integracion_whatsapp.md) | Alta en Meta, plantillas, ventana de 24 h, webhooks, costos, degradación |
| [12_cumplimiento_legal.md](12_cumplimiento_legal.md) | Leyes 25.326, 26.529, 25.506 y 27.706: cómo responde el diseño y entregables legales |

## Quick Start para Desarrolladores

1. Entender el dominio → [01](01_vision_y_objetivos.md), [03](03_actores_y_roles.md)
2. Entender los datos → [04](04_modelo_de_datos.md)
3. Entender las reglas → [05](05_reglas_de_negocio.md), [12](12_cumplimiento_legal.md)
4. Entender la arquitectura → [02](02_descripcion_general.md), [08](08_arquitectura_propuesta.md), [11](11_integracion_whatsapp.md)
5. Implementar → [07](07_flujos_principales.md), [06](06_funcionalidades.md)
6. Antes de codificar → [10](10_preguntas_abiertas.md), [09](09_decisiones_y_supuestos.md)

## Resumen Ejecutivo

Es una web app para que un odontólogo independiente gestione agenda, pacientes, historia clínica y cobros. Pone el foco en reducir ausencias con recordatorios y confirmaciones por WhatsApp oficial. Está construida como monolito modular en Spring Boot con una SPA en React y TypeScript sobre PostgreSQL gestionado, con una instancia por consultorio. La historia clínica es inalterable y auditada para cumplir las Leyes 26.529 y 25.326. Cada consultorio tiene su propia instancia, pero sobre host y clúster PostgreSQL compartidos para mantener bajo el costo (DD-01). El diferencial del producto es la **oferta de huecos por WhatsApp con un clic** a la lista de espera (DD-10).
