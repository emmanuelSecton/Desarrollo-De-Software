// Fechas y horas en la zona horaria del consultorio (C-13, D1). Nunca se usa la zona del navegador:
// las fechas son textos 'AAAA-MM-DD', las horas 'HH:mm' y los instantes ISO con desplazamiento.

export type Fecha = string
export type Hora = string

const MINUTO_MS = 60_000
const DIA_MS = 24 * 60 * MINUTO_MS

const NOMBRES_DIA = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']
const NOMBRES_DIA_CORTO = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const NOMBRES_MES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
]

const formateadores = new Map<string, Intl.DateTimeFormat>()

function partesEn(zona: string, instanteMs: number) {
  let formato = formateadores.get(zona)
  if (!formato) {
    formato = new Intl.DateTimeFormat('en-US', {
      timeZone: zona,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
    formateadores.set(zona, formato)
  }
  const partes: Record<string, number> = {}
  for (const parte of formato.formatToParts(new Date(instanteMs))) {
    if (parte.type !== 'literal') partes[parte.type] = Number(parte.value)
  }
  return partes
}

/** Minutos que la zona esta adelantada respecto de UTC en ese instante (Buenos Aires: -180). */
function desplazamiento(zona: string, instanteMs: number): number {
  const p = partesEn(zona, instanteMs)
  const comoUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second)
  return Math.round((comoUtc - Math.floor(instanteMs / 1000) * 1000) / MINUTO_MS)
}

function formatearDesplazamiento(minutos: number): string {
  const signo = minutos < 0 ? '-' : '+'
  const absoluto = Math.abs(minutos)
  const horas = String(Math.floor(absoluto / 60)).padStart(2, '0')
  return `${signo}${horas}:${String(absoluto % 60).padStart(2, '0')}`
}

function separar(fecha: Fecha): [number, number, number] {
  const [a, m, d] = fecha.split('-').map(Number)
  return [a, m, d]
}

/** "Dia + hora del consultorio" → instante ISO con su desplazamiento ('2026-11-30T10:00:00-03:00'). */
export function instanteDe(fecha: Fecha, hora: Hora, zona: string): string {
  const [a, m, d] = separar(fecha)
  const [h, mi] = hora.split(':').map(Number)
  const comoUtc = Date.UTC(a, m - 1, d, h, mi)
  let offset = desplazamiento(zona, comoUtc)
  const corregido = desplazamiento(zona, comoUtc - offset * MINUTO_MS)
  if (corregido !== offset) offset = corregido
  return `${fecha}T${hora}:00${formatearDesplazamiento(offset)}`
}

function aMs(instante: string | number): number {
  return typeof instante === 'number' ? instante : Date.parse(instante)
}

/** Dia del consultorio en el que cae un instante. */
export function fechaDe(instante: string | number, zona: string): Fecha {
  const p = partesEn(zona, aMs(instante))
  return `${p.year}-${String(p.month).padStart(2, '0')}-${String(p.day).padStart(2, '0')}`
}

/** Hora del consultorio de un instante ('09:05'). */
export function horaDe(instante: string | number, zona: string): Hora {
  const p = partesEn(zona, aMs(instante))
  return `${String(p.hour).padStart(2, '0')}:${String(p.minute).padStart(2, '0')}`
}

export function minutosDelDia(instante: string | number, zona: string): number {
  const p = partesEn(zona, aMs(instante))
  return p.hour * 60 + p.minute
}

export function hoyEn(zona: string, ahoraMs: number = Date.now()): Fecha {
  return fechaDe(ahoraMs, zona)
}

export function sumarDias(fecha: Fecha, dias: number): Fecha {
  const [a, m, d] = separar(fecha)
  return new Date(Date.UTC(a, m - 1, d) + dias * DIA_MS).toISOString().slice(0, 10)
}

/** 1 = lunes ... 7 = domingo, como `diaSemana` de los horarios. */
export function diaDeLaSemana(fecha: Fecha): number {
  const [a, m, d] = separar(fecha)
  const domingoCero = new Date(Date.UTC(a, m - 1, d)).getUTCDay()
  return domingoCero === 0 ? 7 : domingoCero
}

export function lunesDe(fecha: Fecha): Fecha {
  return sumarDias(fecha, 1 - diaDeLaSemana(fecha))
}

export interface Rango {
  desde: string
  hasta: string
}

/** Rango [desde, hasta) de un dia del consultorio, como lo espera GET /turnos. */
export function rangoDeDia(fecha: Fecha, zona: string): Rango {
  return {
    desde: instanteDe(fecha, '00:00', zona),
    hasta: instanteDe(sumarDias(fecha, 1), '00:00', zona),
  }
}

/** Rango [desde, hasta) de la semana (lunes a domingo) que contiene la fecha. */
export function rangoDeSemana(fecha: Fecha, zona: string): Rango {
  const lunes = lunesDe(fecha)
  return {
    desde: instanteDe(lunes, '00:00', zona),
    hasta: instanteDe(sumarDias(lunes, 7), '00:00', zona),
  }
}

/** "Jueves 26 de noviembre". */
export function formatearDia(fecha: Fecha): string {
  const [, m, d] = separar(fecha)
  return `${NOMBRES_DIA[diaDeLaSemana(fecha) - 1]} ${d} de ${NOMBRES_MES[m - 1]}`
}

/** "Jue". */
export function nombreCortoDelDia(fecha: Fecha): string {
  return NOMBRES_DIA_CORTO[diaDeLaSemana(fecha) - 1]
}

export function numeroDelDia(fecha: Fecha): number {
  return separar(fecha)[2]
}

/** "23 al 29 de noviembre" o "30 de noviembre al 6 de diciembre". */
export function formatearSemana(fecha: Fecha): string {
  const lunes = lunesDe(fecha)
  const domingo = sumarDias(lunes, 6)
  const [, m1, d1] = separar(lunes)
  const [, m2, d2] = separar(domingo)
  return m1 === m2
    ? `${d1} al ${d2} de ${NOMBRES_MES[m2 - 1]}`
    : `${d1} de ${NOMBRES_MES[m1 - 1]} al ${d2} de ${NOMBRES_MES[m2 - 1]}`
}

/** "26/11". */
export function formatearDiaCorto(fecha: Fecha): string {
  const [, m, d] = separar(fecha)
  return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}`
}

export function esFechaValida(texto: string | null): texto is Fecha {
  if (!texto || !/^\d{4}-\d{2}-\d{2}$/.test(texto)) return false
  const [a, m, d] = separar(texto)
  const fecha = new Date(Date.UTC(a, m - 1, d))
  return fecha.getUTCFullYear() === a && fecha.getUTCMonth() === m - 1 && fecha.getUTCDate() === d
}

/** 'HH:mm' a minutos desde la medianoche. */
export function aMinutos(hora: Hora): number {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

/** Minutos desde la medianoche a 'HH:mm'. */
export function aHora(minutos: number): Hora {
  return `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`
}

/** '09:00' → '9:00', como se lee en la agenda. */
export function horaLegible(hora: Hora): string {
  return hora.replace(/^0(\d)/, '$1')
}
