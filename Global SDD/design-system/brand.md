# Marca

| Elemento | Valor | Fuente / estado |
|---|---|---|
| Nombre del producto | **Odontogestión** | Definido por el usuario el 2026-11-26 (PA-11 resuelta). Vive en `brand.js`; la app lo toma de `frontend/src/shared/marca/Marca.tsx` (un test controla que coincidan). |
| Logo | Isotipo de diente blanco sobre cuadrado verde azulado (`--color-brand-600`) en `assets/logo.svg`; el nombre se escribe al lado como texto | Definido por el usuario el 2026-11-26: el ícono del placeholder pasa a ser el logo definitivo y se quita la caja "LOGO PROVISORIO". |
| Tono visual | Clínico, sereno y confiable; poco ornamento, alta legibilidad | Derivado de la KB: datos de salud, usuarios profesionales y pacientes de todas las edades. |
| Voz | Español rioplatense con voseo ("Reservá tu turno", "¿Confirmás tu asistencia?"), cercano y profesional, sin tecnicismos para el paciente | Público argentino (Discovery). |
| Color de marca | Verde azulado `--color-brand-600` (#0f766e) | Propuesto: asociado a salud, se diferencia del azul genérico de la mayoría de los competidores relevados. **Suposición:** a validar con el usuario. |
| Tipografía | Fuentes del sistema (`system-ui`, Segoe UI, Roboto) | Sin dependencia de fuentes remotas; buen rendimiento en celulares. |
| Imágenes | Sin fotografía de stock en la v1. Si se usa: personas reales en consultorio, nada de sonrisas genéricas de banco de imágenes | **(TBD)** |
| Iconografía | Trazo lineal de 1,5–2 px, esquinas redondeadas (por ejemplo, la librería Lucide) | Propuesto |

## Reglas de uso

- El color de marca es para **acciones primarias y navegación**. Los colores de estado (verde, ámbar, rojo, azul) se reservan para el ciclo de vida del turno y los mensajes de sistema; no se usan como decoración.
- El estado nunca se comunica solo con color: siempre lleva texto o ícono (accesibilidad).
- El verde de WhatsApp (`--color-whatsapp`) solo se usa en acciones que abren o refieren a WhatsApp.
- En la página pública del paciente se muestra el **nombre del consultorio** como protagonista; la marca del producto aparece discreta (por ejemplo "con tecnología de …").
- Los colores del odontograma siguen la convención clínica (rojo a realizar, azul realizado), pendiente de confirmar con un odontólogo (PA-08).
