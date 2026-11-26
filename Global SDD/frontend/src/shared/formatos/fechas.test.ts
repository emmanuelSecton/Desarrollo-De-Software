import { describe, expect, it } from 'vitest'
import {
  diaDeLaSemana,
  esFechaValida,
  fechaDe,
  formatearDia,
  horaDe,
  hoyEn,
  instanteDe,
  lunesDe,
  minutosDelDia,
  rangoDeDia,
  rangoDeSemana,
  sumarDias,
} from './fechas'

const BSAS = 'America/Argentina/Buenos_Aires'
// Zona con horario de verano: el 2026-03-08 a las 2:00 pasa a las 3:00 (UTC-5 → UTC-4).
const NUEVA_YORK = 'America/New_York'

describe('fechas en la zona del consultorio', () => {
  it('convierte dia y hora locales en un instante con el desplazamiento de la zona', () => {
    expect(instanteDe('2026-11-30', '10:00', BSAS)).toBe('2026-11-30T10:00:00-03:00')
    expect(instanteDe('2026-11-30', '10:00', 'UTC')).toBe('2026-11-30T10:00:00+00:00')
    expect(instanteDe('2026-11-30', '10:00', 'Asia/Kolkata')).toBe('2026-11-30T10:00:00+05:30')
  })

  it('respeta el cambio de horario de verano', () => {
    expect(instanteDe('2026-03-07', '10:00', NUEVA_YORK)).toBe('2026-03-07T10:00:00-05:00')
    expect(instanteDe('2026-03-08', '10:00', NUEVA_YORK)).toBe('2026-03-08T10:00:00-04:00')
    // Medianoche del dia del cambio: todavia en horario de invierno.
    expect(instanteDe('2026-03-08', '00:00', NUEVA_YORK)).toBe('2026-03-08T00:00:00-05:00')
  })

  it('lee el dia y la hora de un instante en la zona, no en la del navegador', () => {
    const instante = '2026-11-30T02:30:00Z'
    expect(fechaDe(instante, BSAS)).toBe('2026-11-29')
    expect(horaDe(instante, BSAS)).toBe('23:30')
    expect(minutosDelDia(instante, BSAS)).toBe(23 * 60 + 30)
    expect(fechaDe(instante, 'UTC')).toBe('2026-11-30')
  })

  it('hoy depende de la zona', () => {
    const ahora = Date.parse('2026-11-27T01:00:00Z')
    expect(hoyEn(BSAS, ahora)).toBe('2026-11-26')
    expect(hoyEn('UTC', ahora)).toBe('2026-11-27')
  })

  it('aritmetica de dias y semana de lunes a domingo', () => {
    expect(sumarDias('2026-11-30', 1)).toBe('2026-12-01')
    expect(sumarDias('2026-01-01', -1)).toBe('2025-12-31')
    expect(lunesDe('2026-11-26')).toBe('2026-11-23')
    expect(lunesDe('2026-11-29')).toBe('2026-11-23')
    expect(lunesDe('2026-11-23')).toBe('2026-11-23')
    expect(diaDeLaSemana('2026-11-23')).toBe(1)
    expect(diaDeLaSemana('2026-11-29')).toBe(7)
  })

  it('rangos de dia y semana para la API, con fin exclusivo', () => {
    expect(rangoDeDia('2026-11-26', BSAS)).toEqual({
      desde: '2026-11-26T00:00:00-03:00',
      hasta: '2026-11-27T00:00:00-03:00',
    })
    expect(rangoDeSemana('2026-11-26', BSAS)).toEqual({
      desde: '2026-11-23T00:00:00-03:00',
      hasta: '2026-11-30T00:00:00-03:00',
    })
  })

  it('formatea el dia en castellano', () => {
    expect(formatearDia('2026-11-26')).toBe('Jueves 26 de noviembre')
  })

  it('valida fechas del parametro de la URL', () => {
    expect(esFechaValida('2026-11-26')).toBe(true)
    expect(esFechaValida('2026-02-30')).toBe(false)
    expect(esFechaValida('hoy')).toBe(false)
    expect(esFechaValida(null)).toBe(false)
  })
})
