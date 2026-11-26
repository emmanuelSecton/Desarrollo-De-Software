# Datos de la demo

`cargar-datos-demo.mjs` carga **pacientes y turnos ficticios** (SU-07) para mostrar la agenda en pantalla, usando la API real de una instancia: no toca la base a mano.

- 12 pacientes inventados (DNI y teléfonos `11 5555 01xx` de prueba).
- 17 turnos repartidos en los próximos 5 días con atención: confirmados, sin confirmar, un sobreturno y uno cancelado.
- Se puede correr varias veces: no duplica pacientes (los busca por DNI) ni turnos (mismo paciente y mismo inicio). Un horario que ya ocupa otro turno se informa y se saltea.
- Omite los turnos que ya pasaron o que caen fuera del horario de atención del consultorio.

## Requisitos

- Node 24 (el mismo del frontend), sin dependencias.
- Una instancia con C-13 o posterior (usa `zonaHoraria` de `GET /api/v1/consultorio`).
- Una cuenta del personal que ya haya cambiado la contraseña temporal (entrá una vez desde la app).

## Uso

Desde `Global SDD/`, en PowerShell:

```powershell
$env:DEMO_EMAIL = 'odontologo@consultorio.test'
$env:DEMO_PASSWORD = Read-Host 'Contraseña' -MaskInput
node deploy/demo/cargar-datos-demo.mjs
```

Opciones:

| Opción | Qué hace |
|---|---|
| `--url URL` | Instancia (por defecto `http://localhost:8080`; también `DEMO_URL`). |
| `--desde AAAA-MM-DD` | Primer día del plan. Útil para cargar la víspera y que todo empiece el día de la demo. |
| `--si` | Obligatorio si la URL no es local (por ejemplo la instancia de demo del VPS). |

La contraseña se lee solo de `DEMO_PASSWORD` y nunca se imprime. Al terminar, el script cierra la sesión.

## Tests

```powershell
node --test deploy/demo/plan.test.mjs
```

Cubren el armado del plan: días con atención, horarios en la zona del consultorio, turnos pasados y prestaciones faltantes.
