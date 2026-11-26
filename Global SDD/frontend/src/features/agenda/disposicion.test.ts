import { describe, expect, it } from 'vitest'
import type { Turno } from './api'
import { agruparSuperpuestos, disponerDia, huecosLibres, type TurnoEnElDia } from './disposicion'
import { estadoVisible } from './estado'

const turno = (id: string, estado: Turno['estado'] = 'RESERVADO') =>
  ({ id, estado }) as unknown as Turno
const en = (id: string, inicio: number, fin: number, estado?: Turno['estado']): TurnoEnElDia => ({
  turno: turno(id, estado),
  inicio,
  fin,
})
const h = (horas: number, minutos = 0) => horas * 60 + minutos
const FRANJAS = [
  { inicio: h(9), fin: h(13) },
  { inicio: h(15), fin: h(19) },
]

describe('estado visible', () => {
  const ahora = Date.parse('2026-11-26T12:00:00Z')
  it('un RESERVADO dentro del limite de confirmacion se ve "Sin confirmar"', () => {
    expect(estadoVisible({ estado: 'RESERVADO', inicio: '2026-11-27T08:00:00Z' }, ahora, 24)).toBe(
      'sinconf',
    )
    expect(estadoVisible({ estado: 'RESERVADO', inicio: '2026-11-28T08:00:00Z' }, ahora, 24)).toBe(
      'reservado',
    )
    expect(estadoVisible({ estado: 'CONFIRMADO', inicio: '2026-11-27T08:00:00Z' }, ahora, 24)).toBe(
      'confirmado',
    )
  })
})

describe('disposicion del dia', () => {
  it('agrupa los turnos que se superponen y deja aparte los contiguos', () => {
    const grupos = agruparSuperpuestos([
      en('a', h(10), h(10, 30)),
      en('b', h(10, 15), h(10, 45)),
      en('c', h(10, 45), h(11, 15)),
    ])
    expect(grupos.map((g) => g.map((t) => t.turno.id))).toEqual([['a', 'b'], ['c']])
  })

  it('encuentra huecos de 30 minutos o mas dentro de las franjas', () => {
    const huecos = huecosLibres(
      FRANJAS,
      [
        { inicio: h(9), fin: h(12) },
        { inicio: h(15), fin: h(18, 45) },
      ],
      0,
    )
    expect(huecos).toEqual([{ inicio: h(12), fin: h(13) }])
  })

  it('no ofrece huecos que ya pasaron', () => {
    expect(huecosLibres([{ inicio: h(9), fin: h(13) }], [], h(12, 20))).toEqual([
      { inicio: h(12, 30), fin: h(13) },
    ])
  })

  it('ubica horas, tramos fuera de atencion, turnos y huecos en filas de 15 minutos', () => {
    const disposicion = disponerDia(
      [en('a', h(9), h(9, 45), 'CONFIRMADO'), en('x', h(9, 45), h(12), 'CANCELADO')],
      FRANJAS,
      0,
    )!
    expect(disposicion.filas).toBe(40)
    expect(disposicion.horas[0]).toEqual({ fila: 1, filas: 4, texto: '9:00' })
    expect(disposicion.fuera).toEqual([
      { fila: 17, filas: 8, texto: 'Fuera del horario de atención · 13:00 a 15:00' },
    ])
    expect(disposicion.grupos[0]).toMatchObject({ fila: 1, filas: 3 })
    // El cancelado no ocupa el horario.
    expect(disposicion.huecos[0]).toEqual({ fila: 4, filas: 13, hora: '09:45' })
  })

  it('un dia sin franjas ni turnos no tiene grilla', () => {
    expect(disponerDia([], [], 0)).toBeNull()
  })
})
