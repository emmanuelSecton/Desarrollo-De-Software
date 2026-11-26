import { aHora, aMinutos, horaLegible, minutosDelDia } from '../../shared/formatos/fechas'
import type { Franja, Turno } from './api'
import { esActivo } from './estado'

// Ubicacion de los turnos en la grilla del dia (D4): filas de 15 minutos.

export const MINUTOS_POR_FILA = 15
const HUECO_MINIMO = 30

export interface Intervalo {
  inicio: number
  fin: number
}

export interface TurnoEnElDia extends Intervalo {
  turno: Turno
}

export interface Ubicacion {
  fila: number
  filas: number
}

export interface Disposicion {
  filas: number
  horas: (Ubicacion & { texto: string })[]
  fuera: (Ubicacion & { texto: string })[]
  grupos: (Ubicacion & { turnos: Turno[] })[]
  huecos: (Ubicacion & { hora: string })[]
}

export function enElDia(turno: Turno, zona: string): TurnoEnElDia {
  const inicio = minutosDelDia(turno.inicio, zona)
  const duracion = Math.round((Date.parse(turno.fin) - Date.parse(turno.inicio)) / 60_000)
  return { turno, inicio, fin: inicio + duracion }
}

export const intervaloDeFranja = (franja: Franja): Intervalo => ({
  inicio: aMinutos(franja.desde),
  fin: aMinutos(franja.hasta),
})

/** Agrupa turnos que se superponen: cada grupo se dibuja en una fila con un carril por turno. */
export function agruparSuperpuestos(turnos: TurnoEnElDia[]): TurnoEnElDia[][] {
  const ordenados = [...turnos].sort((a, b) => a.inicio - b.inicio || a.fin - b.fin)
  const grupos: TurnoEnElDia[][] = []
  let finDelGrupo = -1
  for (const turno of ordenados) {
    if (grupos.length > 0 && turno.inicio < finDelGrupo) {
      grupos[grupos.length - 1].push(turno)
      finDelGrupo = Math.max(finDelGrupo, turno.fin)
    } else {
      grupos.push([turno])
      finDelGrupo = turno.fin
    }
  }
  return grupos
}

/** Huecos libres de al menos 30 minutos dentro de las franjas, sin contar lo que ya paso. */
export function huecosLibres(
  franjas: Intervalo[],
  ocupados: Intervalo[],
  desdeMinuto: number,
): Intervalo[] {
  const huecos: Intervalo[] = []
  const tomados = [...ocupados].sort((a, b) => a.inicio - b.inicio)
  for (const franja of franjas) {
    let cursor = Math.max(
      franja.inicio,
      Math.ceil(desdeMinuto / MINUTOS_POR_FILA) * MINUTOS_POR_FILA,
    )
    for (const ocupado of tomados) {
      if (ocupado.fin <= cursor || ocupado.inicio >= franja.fin) continue
      if (ocupado.inicio - cursor >= HUECO_MINIMO)
        huecos.push({ inicio: cursor, fin: ocupado.inicio })
      cursor = Math.max(cursor, ocupado.fin)
    }
    if (franja.fin - cursor >= HUECO_MINIMO) huecos.push({ inicio: cursor, fin: franja.fin })
  }
  return huecos
}

function tramosFuera(franjas: Intervalo[], desde: number, hasta: number): Intervalo[] {
  const tramos: Intervalo[] = []
  let cursor = desde
  for (const franja of [...franjas].sort((a, b) => a.inicio - b.inicio)) {
    if (franja.inicio > cursor) tramos.push({ inicio: cursor, fin: franja.inicio })
    cursor = Math.max(cursor, franja.fin)
  }
  if (cursor < hasta) tramos.push({ inicio: cursor, fin: hasta })
  return tramos
}

/**
 * Calcula la grilla de un dia. `desdeMinuto` es desde cuando se ofrecen huecos: 0 para un dia futuro,
 * el minuto actual para hoy y null para un dia pasado.
 */
export function disponerDia(
  turnos: TurnoEnElDia[],
  franjas: Intervalo[],
  desdeMinuto: number | null,
): Disposicion | null {
  const bordes = [...franjas, ...turnos]
  if (bordes.length === 0) return null
  const desde = Math.floor(Math.min(...bordes.map((b) => b.inicio)) / 60) * 60
  const hasta = Math.min(24 * 60, Math.ceil(Math.max(...bordes.map((b) => b.fin)) / 60) * 60)

  const ubicar = (intervalo: Intervalo): Ubicacion => {
    const fila = Math.floor((intervalo.inicio - desde) / MINUTOS_POR_FILA) + 1
    const ultima = Math.ceil((intervalo.fin - desde) / MINUTOS_POR_FILA)
    return { fila, filas: Math.max(1, ultima - fila + 1) }
  }

  const horas = []
  for (let minuto = desde; minuto < hasta; minuto += 60) {
    horas.push({
      ...ubicar({ inicio: minuto, fin: minuto + 60 }),
      texto: horaLegible(aHora(minuto)),
    })
  }

  const fuera = tramosFuera(franjas, desde, hasta).map((tramo) => ({
    ...ubicar(tramo),
    texto:
      franjas.length === 0
        ? 'Sin horario de atención'
        : `Fuera del horario de atención · ${horaLegible(aHora(tramo.inicio))} a ${horaLegible(aHora(tramo.fin))}`,
  }))

  const grupos = agruparSuperpuestos(turnos).map((grupo) => ({
    ...ubicar({
      inicio: Math.min(...grupo.map((t) => t.inicio)),
      fin: Math.max(...grupo.map((t) => t.fin)),
    }),
    turnos: grupo.map((t) => t.turno),
  }))

  const huecos =
    desdeMinuto === null
      ? []
      : huecosLibres(
          franjas,
          turnos.filter((t) => esActivo(t.turno)),
          desdeMinuto,
        ).map((hueco) => ({ ...ubicar(hueco), hora: aHora(hueco.inicio) }))

  return { filas: (hasta - desde) / MINUTOS_POR_FILA, horas, fuera, grupos, huecos }
}
