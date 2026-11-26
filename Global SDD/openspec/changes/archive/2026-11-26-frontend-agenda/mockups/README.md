# Mockups — C-13 `frontend-agenda`

- **Design system revision:** 1
- **Approval:** approved (2026-11-26, by user)
- **Preview:** rendered (2026-11-26) a 390x844 y 1440x900 → `previews/`

## Pantallas y estados

| Archivo | Pantalla / estado |
|---|---|
| `agenda-dia.html` | Vista día: grilla con franjas de atención, horario fuera de atención atenuado, hueco libre, estados Confirmado y "Sin confirmar", sobreturno al lado del turno que pisa. En celular, lista por franja |
| `agenda-semana.html` | Vista semana: siete columnas con hoy resaltado y fin de semana sin atención. En celular, tira de días y lista del día elegido |
| `agenda-dia-vacio.html` | Día sin turnos, con la acción de dar turno |
| `dar-turno.html` | Dar turno: búsqueda de paciente con resultados navegables y "Nuevo paciente"; prestación, día, hora y duración |
| `dar-turno-paciente-nuevo.html` | Alta de paciente en el momento, con error en el teléfono, consentimiento obligatorio y aviso de horario fuera de atención |
| `dar-turno-superpuesto.html` | `409` por superposición: con qué turno choca, "Elegir otro horario" y "Dar como sobreturno" |
| `detalle-turno.html` | Detalle del turno (panel lateral; hoja inferior en celular) con las acciones que permite su estado |
| `cancelar-turno.html` | Cancelar: quién cancela y motivo obligatorio con error y contador |
| `reprogramar-turno.html` | Reprogramar a otro día y hora, con la explicación de que el turno actual queda cancelado |
| `aviso-turno-modificado.html` | `409` por edición simultánea: aviso y agenda actualizada (el turno pasó a Confirmado) |

Estados: el color siempre va con ícono y texto ("Confirmado", "Sin confirmar", "Reservado", "Cancelado") y el sobreturno lleva su propia marca. Los cancelados se ocultan salvo con "Cancelados visibles".

## Fuentes

- `design-system/tokens.css` (incluidos los tokens `--turno-*`), `brand.js` y `assets/logo.svg` (revisión 1, aprobada). Base de estilos de los mockups de C-07.
- Endpoints y errores: `openspec/specs/agenda-turnos`, `pacientes` y `configuracion-consultorio`.
- Datos de ejemplo ficticios (SU-07): nombres, DNI y teléfonos `5555` inventados.
- Las posiciones de la grilla usan atributos `style` solo en estos HTML de revisión; en la app se asignan desde JavaScript (CSSOM) por la CSP (`design.md`, D4).

## Provisorios (TBD)

- Nombre del producto y logo: siguen provisorios (PA-11).

## Cómo verlos

Desde `Global SDD/`:

```
python -m http.server 8765 --bind 127.0.0.1
```

y abrir `http://127.0.0.1:8765/openspec/changes/archive/2026-11-26-frontend-agenda/mockups/index.html`.
