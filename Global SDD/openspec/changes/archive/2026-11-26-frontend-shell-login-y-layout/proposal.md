# Proposal — frontend-shell-login-y-layout (C-07)

> Governance: **BAJO**. Es un cambio visual: por la política `system-and-mockups`, se implementa recién cuando el usuario apruebe los mockups.

## Why

La demo tiene que ser en pantalla (decisión del 2026-11-26). Hoy la SPA es un placeholder técnico: no hay login, ni navegación, ni forma de llamar a la API con la sesión y el CSRF de C-03. C-07 arma esa base, y C-13 (agenda recortada) se monta encima.

## What Changes

- **SPA base**: rutas del cliente, cliente HTTP con CSRF y manejo de `401`/`403`, contexto de sesión, y guards por sesión y por rol.
- **Pantallas**:
  - **Login** con email y contraseña, y el error genérico de C-03.
  - **Cambio de contraseña obligatorio** del primer ingreso.
  - **Estructura de la app**: barra superior con el nombre del producto y del usuario, y navegación por rol (el odontólogo ve Configuración; la secretaria no).
  - **Aviso de sesión por vencer** y vuelta al login con el motivo "sesión expirada".
  - Páginas de **acceso denegado (403)** y **no encontrada (404)**.
- **Componentes base** con los tokens del sistema de diseño: botón, campo de texto, alerta, aviso emergente, diálogo y estados vacío, de carga y de error.
- Las secciones que todavía no existen (Agenda llega con C-13, Pacientes con C-26 y Configuración con C-08) muestran un estado vacío "Disponible próximamente".

Fuera de alcance: recuperación de contraseña en pantalla (C-08; el login solo lleva el enlace), contenido de la agenda (C-13), modo oscuro.

## Capabilities

### New Capabilities
- `frontend-shell`: acceso del personal a la SPA (login, cambio obligatorio, sesión y su vencimiento), navegación por rol y manejo uniforme de errores de la API en pantalla.

### Modified Capabilities
_(ninguna)_

## Impact

- **Frontend**: `src/shared/api-client`, `src/shared/auth`, `src/shared/ui`, `src/pages` y `src/app` (rutas y layout). Nuevas dependencias: `react-router` (rutas) y `lucide-react` (íconos de trazo, como pide `brand.md`).
- **Backend**: ninguno; usa los endpoints de C-03.
- **Mockups**: `openspec/changes/frontend-shell-login-y-layout/mockups/`, a 390x844 y 1440x900.
- **Desbloquea**: C-13 (agenda para la demo) y, con C-04 a C-06, C-08.
