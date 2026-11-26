# Sistema de diseño

- **Mode:** system-and-mockups
- **Revision:** 2
- **Approval:** approved (2026-11-26, by user) — revisión 2: nombre y logo definitivos
- **Decisión de política:** elegida por el usuario el 2026-11-23 durante la fundación (active-orchestrator, fase visual-design). Cada cambio visual tendrá un mockup HTML en `openspec/changes/<change>/mockups/` para aprobar antes de implementar.

## Propósito

Una única fuente de valores visuales (colores, tipografía, espaciado, estados) para la SPA en React y para los mockups de cada change. Los mockups importan `tokens.css` y `brand.js`; la app importa `tokens.css` (o sus valores convertidos a la configuración del framework de estilos que se elija).

## Archivos

| Archivo | Para qué |
|---|---|
| `tokens.css` | Variables CSS: color (marca, neutros, feedback, estados del turno, odontograma, WhatsApp), tipografía, espaciado, radios, sombras, foco, movimiento y `prefers-reduced-motion` |
| `brand.md` | Nombre, logo, tono, voz y reglas de uso |
| `brand.js` | Constante del nombre del producto y binding para `[data-brand-name]` |
| `components.html` | Referencia de componentes: botones, formularios, estados del turno, alertas, agenda, bandeja sin confirmar, ficha del paciente, odontograma, reserva pública y vista previa de WhatsApp |
| `assets/logo.svg` | Logo definitivo: isotipo de diente (40x40) |
| `previews/` | Capturas renderizadas: `components-desktop-1440.png`, `components-mobile-390.png` (página completa) y `components-mobile-390-viewport.png` |

## Fuentes

- `knowledge-base/` (actores, flujos, reglas RN-TU, RN-NO y RN-HC) y `discovery/estudio-de-mercado.md` (referencias de UX: Dentally, Doctoralia y AgendaPro).
- No había identidad visual previa: es un proyecto nuevo.

## Decisiones abiertas (TBD)

- Convención de colores del odontograma (PA-08).
- Modo oscuro: no incluido en la revisión 1.

## Historial de revisiones

| Revisión | Fecha | Cambio |
|---|---|---|
| 1 | 2026-11-23 | Fundación: tokens, marca provisoria, componentes de referencia. |
| 2 | 2026-11-26 | Nombre **Odontogestión** y logo definitivo (PA-11). Se quitan las etiquetas "provisorio" de la app y de `components.html`. Sin cambios de tokens. |

La librería de componentes de React quedó resuelta en C-07: componentes propios en `frontend/src/shared/ui` con CSS Modules y solo tokens.

## Cómo cambiar valores

1. **Nombre**: editar `BRAND_NAME` en `brand.js` y `NOMBRE_PRODUCTO` en `frontend/src/shared/marca/Marca.tsx` (un test controla que coincidan).
2. **Logo**: reemplazar `assets/logo.svg` **con el mismo nombre de archivo**.
3. **Colores u otros valores**: editar solo `tokens.css`. Cambiar un token compartido sube la `Revision`, vuelve `Approval` a `pending` y obliga a re-renderizar y revisar los mockups afectados.

## Cómo previsualizar

Servir la carpeta en local (`python -m http.server 8765` dentro de `design-system/`) y abrir `http://127.0.0.1:8765/components.html`. También se puede abrir el archivo directamente en el navegador.
