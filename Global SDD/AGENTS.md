<!-- active-stack:agent-instruction -->
# Global SDD — Gestión odontológica — Instrucciones para Agentes

> Este archivo (y su copia `CLAUDE.md`) es lo PRIMERO que todo agente lee al entrar al proyecto.
> Generado a partir de `knowledge-base/` y `CHANGES.md`. No editar a mano sin re-sincronizar ambos archivos.

Web app para que un odontólogo independiente gestione agenda, pacientes, historia clínica y cobros. El foco está en reducir ausencias con recordatorios y confirmaciones por WhatsApp oficial. Cada consultorio corre su **propia instancia** (single-tenant) con su propia base, sobre host y clúster PostgreSQL compartidos (DD-01).

---

## Stack Tecnológico

| Capa | Tecnología | Versión mínima |
|------|------------|----------------|
| Backend | Java + Spring Boot (Web, Security, Data JPA, Validation), monolito modular | Java 21 LTS, Spring Boot 3.x |
| Persistencia | PostgreSQL + Flyway | PostgreSQL 16 |
| Frontend | React + Vite + TypeScript (SPA servida por Spring Boot) | React 18, TypeScript 5 |
| Tareas programadas | Spring Scheduling dentro del monolito | — |
| Mensajería | WhatsApp Business Platform (Cloud API de Meta) | Graph API vigente |
| Email | SMTP transaccional (proveedor a definir) | — |
| Calendario | Google Calendar API (OAuth 2.0, unidireccional) — post-MVP | v3 |
| Empaquetado / hosting | Docker (imagen única). Demo: VPS con Docker Compose + PostgreSQL en contenedor (DD-11); producción: PostgreSQL gestionado (PA-13) | — |

Detalle completo: [knowledge-base/02_descripcion_general.md](knowledge-base/02_descripcion_general.md)

---

## Base de Conocimiento

La fuente de verdad del dominio vive en `knowledge-base/`. **Leé el archivo relevante ANTES de implementar.**

| Archivo | Cuándo leerlo |
|---------|---------------|
| [01_vision_y_objetivos.md](knowledge-base/01_vision_y_objetivos.md) | Propósito, alcance del MVP y fuera de alcance |
| [02_descripcion_general.md](knowledge-base/02_descripcion_general.md) | Stack, arquitectura general, API REST |
| [03_actores_y_roles.md](knowledge-base/03_actores_y_roles.md) | Auth, RBAC, rutas públicas |
| [04_modelo_de_datos.md](knowledge-base/04_modelo_de_datos.md) | Entidades, constraints, migraciones |
| [05_reglas_de_negocio.md](knowledge-base/05_reglas_de_negocio.md) | Reglas RN-XX |
| [06_funcionalidades.md](knowledge-base/06_funcionalidades.md) | Historias US-NNN y criterios de aceptación |
| [07_flujos_principales.md](knowledge-base/07_flujos_principales.md) | Flujos de punta a punta |
| [08_arquitectura_propuesta.md](knowledge-base/08_arquitectura_propuesta.md) | Patrones, estructura, seguridad, variables de entorno |
| [09_decisiones_y_supuestos.md](knowledge-base/09_decisiones_y_supuestos.md) | Decisiones DD-XX, supuestos y riesgos |
| [10_preguntas_abiertas.md](knowledge-base/10_preguntas_abiertas.md) | ⚠️ Inconsistencias y preguntas a resolver ANTES de codear |
| [11_integracion_whatsapp.md](knowledge-base/11_integracion_whatsapp.md) | Cloud API, plantillas, webhooks |
| [12_cumplimiento_legal.md](knowledge-base/12_cumplimiento_legal.md) | Leyes 25.326 y 26.529: requisitos y cómo responde el diseño |

> ⚠️ Revisar `10_preguntas_abiertas.md` antes del change que cada pregunta condiciona (ver `CHANGES.md`). PA-01 a PA-04 ya están resueltas; la demo usa **solo datos ficticios** (SU-07).

Sistema de diseño aprobado (revisión 1, política `system-and-mockups`): [design-system/README.md](design-system/README.md).

---

## Skills Disponibles

| Agente | Rol | Skills que carga |
|--------|-----|------------------|
| **Backend Core** | Spring Boot, dominio, JPA, migraciones, TDD | `java-springboot`, `springboot-tdd`, `java-testcontainers`, `postgresql-table-design` |
| **Backend Seguridad e Integraciones** | Auth/RBAC, cumplimiento, WhatsApp, email, Google | `springboot-security`, `java-springboot`, `hipaa-compliance` (solo referencia; adaptar a las Leyes 25.326 y 26.529), `integrate-whatsapp` (solo referencia; el proyecto usa la Cloud API directa, DD-04) |
| **Frontend** | React + Vite + TypeScript, tests, accesibilidad | `vite`, `vitest`, `typescript-react-reviewer`, `accessibility`, `web-design-guidelines` |
| **Diseño / UI** | Sistema de diseño y mockups por change | `design-system`, `emil-design-eng`, `design-taste-frontend-v1` |
| **DevOps** | Imagen única, compose, despliegue por instancia | `multi-stage-dockerfile`, `docker-patterns` |
| **Fundación** | Contexto e instrucciones | `kb-creator`, `roadmap-generator`, `agent-instruction`, `skill-registry` |

Cargá la skill correspondiente al contexto ANTES de escribir código.

> Los compact rules de cada skill los resuelve el orquestador desde [`.atl/skill-registry.md`](.atl/skill-registry.md) (generado por `skill-registry`; no versionado — no está en el repo). Esta tabla solo mapea skill→rol.

---

## Roadmap de Changes

El plan de implementación completo está en [CHANGES.md](CHANGES.md). Resumen:

- **Total**: 38 changes (C-01 a C-38) en 9 fases (0 a 8); **35 en el MVP** — C-30, C-31 y C-34 postergados a post-MVP (DD-13). Equipo de 1-2 personas, ~3 meses, objetivo: demo con datos ficticios. C-37 es el diferencial: oferta de huecos por WhatsApp con un clic (DD-10). C-13 se recortó para la demo en pantalla y sus bloqueos y asistencia pasaron a C-38.
- **Camino crítico** (11): `C-01 → C-02 → C-03 → C-09 → C-10 → C-14 → C-15 → C-16 → C-17 → C-19 → C-36`.
- **Primer change**: `C-01` (`scaffolding-monorepo-y-base-tecnica`). C-35 (despliegue) solo depende de C-01 y puede ir en paralelo.
- Los changes con UI requieren un **mockup HTML aprobado** antes de implementarse.

**Antes de iniciar un item**: leé [CHANGES.md](CHANGES.md), identificá las dependencias del change y los archivos de "Leer antes".

---

## Reglas Duras (específicas del proyecto)

> Reglas **globales** ya definidas en el archivo global de instrucciones del cliente activo (orquestador OPSX, governance por dominio, Engram, gate de diseño): el proyecto las **hereda**, no se repiten acá. Acá viven solo las reglas específicas de este proyecto y las universales que el global no cubre. Son contrato; romperlas es un defecto.

**Datos y cumplimiento**
- NUNCA hacer `UPDATE` ni `DELETE` sobre `entrada_historia` ni `evento_auditoria` → corregir con una entrada `ENMIENDA` o un evento nuevo (RN-HC-01, Ley 26.529).
- NUNCA cambiar el esquema a mano ni editar una migración Flyway ya aplicada → crear una migración `V{n}__descripcion.sql` nueva.
- NUNCA poner datos clínicos en logs, eventos de Google Calendar ni mensajes de WhatsApp → minimizar datos (Ley 25.326).
- NUNCA commitear tokens, claves ni contraseñas → variables de entorno, con `.env.example` sin valores reales.

**Backend**
- NUNCA validar el solapamiento de turnos solo en Java → la exclusion constraint de PostgreSQL es la garantía; traducir la violación a HTTP 409.
- NUNCA exponer entidades JPA en la API → DTOs con Bean Validation y errores como `ProblemDetail`.
- NUNCA testear repositorios con H2 ni con mocks de la base → Testcontainers con PostgreSQL real.
- NUNCA usar librerías no oficiales de WhatsApp → WhatsApp Cloud API de Meta detrás de `MensajeriaPort` (DD-04).

**Frontend y UI**
- NUNCA hardcodear colores, espaciados ni tipografías → usar los tokens de `design-system/tokens.css`.
- NUNCA implementar un cambio visual sin su mockup HTML aprobado → política `system-and-mockups`.
- NUNCA comunicar un estado solo con color → acompañarlo siempre con texto o ícono (accesibilidad).

**Proceso y arquitectura**
- NUNCA commitear ni pushear sin pedido explícito del usuario → cuando se pida, usar Conventional Commits.
- NUNCA usar clases internas de otro módulo → solo su API pública o eventos de dominio (monolito modular).
- NUNCA guardar fechas en hora local → `TIMESTAMPTZ` en UTC; mostrarlas en la zona horaria del consultorio.
- NUNCA dar por terminado un change con tests en rojo o sin tests de sus reglas RN → tests verdes que cubran las reglas del change.

---

## Flujo de Trabajo

```
1. Leer la KB relevante (knowledge-base/)        → entender el dominio
2. Identificar el change en CHANGES.md           → respetar dependencias
3. Acordar alcance y criterios de aceptación (OPSX: propose; mockup si hay UI)
4. Implementar y verificar respetando las reglas del proyecto
5. Registrar la evidencia y actualizar el estado en CHANGES.md
```

Aplicar TODAS las reglas duras en cada paso. Ante conflicto entre la KB y este archivo, las reglas duras prevalecen.
<!-- /active-stack:agent-instruction -->
