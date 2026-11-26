# Mockups — C-07 `frontend-shell-login-y-layout`

- **Design system revision:** 1
- **Approval:** approved (2026-11-26, by user)
- **Preview:** rendered (2026-11-26) a 390x844 y 1440x900 → `previews/`

## Pantallas y estados

| Archivo | Pantalla / estado |
|---|---|
| `login.html` | Login sin datos |
| `login-error.html` | Login rechazado: mensaje genérico de C-03 (no dice si el email existe ni si la cuenta está bloqueada) |
| `login-sesion-vencida.html` | Vuelta al login después de un `401` por sesión vencida |
| `cambiar-contrasena.html` | Cambio de contraseña obligatorio del primer ingreso, con error en la contraseña nueva |
| `app-odontologo.html` | Estructura de la app del odontólogo: Agenda, Pacientes y Configuración; secciones con "Disponible próximamente" |
| `app-secretaria.html` | Estructura de la app de la secretaria: solo Agenda y Pacientes |
| `app-sesion-por-vencer.html` | Diálogo de sesión por vencer tras 28 minutos sin actividad |
| `error-403.html` | Acceso denegado |
| `error-404.html` | Página no encontrada |

En escritorio la navegación es una barra lateral; en celular pasa a una barra inferior con las mismas secciones.

## Fuentes

- `design-system/tokens.css`, `brand.js` y `assets/logo.svg` (revisión 1, aprobada). Todos los valores visuales salen de los tokens; `mockups.css` no tiene colores ni espaciados fijos.
- Textos de error y rutas: endpoints y `ProblemDetail` de C-03 (`openspec/specs/identidad`).
- Datos de ejemplo ficticios (SU-07): "Dra. Ana Ficticia" y "Lucía Martínez".

## Provisorios (TBD)

- Nombre del producto y logo: siguen provisorios (PA-11); se cambian en `brand.js` y `assets/logo.svg` sin tocar estos archivos.

## Cómo verlos

Desde `Global SDD/`:

```
python -m http.server 8765 --bind 127.0.0.1
```

y abrir `http://127.0.0.1:8765/openspec/changes/frontend-shell-login-y-layout/mockups/index.html`.
