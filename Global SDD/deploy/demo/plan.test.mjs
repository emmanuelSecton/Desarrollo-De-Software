import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { diasConAtencion, instanteDe, PACIENTES, planDeTurnos, TURNOS } from './plan.mjs'

const BSAS = 'America/Argentina/Buenos_Aires'
const FRANJAS = [1, 2, 3, 4, 5].flatMap((dia) => [
  { diaSemana: dia, desde: '09:00', hasta: '13:00' },
  { diaSemana: dia, desde: '15:00', hasta: '19:00' },
])
const PRESTACIONES = [
  ['Consulta/diagnóstico', 30],
  ['Control', 15],
  ['Endodoncia', 90],
  ['Extracción', 45],
  ['Limpieza', 45],
  ['Obturación', 45],
].map(([nombre, duracionMinutos], i) => ({ id: `p${i}`, nombre, duracionMinutos, activa: true }))

describe('plan de datos de la demo', () => {
  it('convierte la hora del consultorio en un instante con su desplazamiento', () => {
    assert.equal(instanteDe('2026-11-27', '09:00', BSAS), '2026-11-27T09:00:00-03:00')
    assert.equal(instanteDe('2026-03-08', '10:00', 'America/New_York'), '2026-03-08T10:00:00-04:00')
  })

  it('usa solo dias con atencion: un sabado salta al lunes', () => {
    assert.deepEqual(diasConAtencion('2026-11-28', FRANJAS, 2), ['2026-11-30', '2026-12-01'])
  })

  it('un lunes antes de abrir carga todas las plantillas, todas en el futuro y dentro de las franjas', () => {
    const ahora = Date.parse('2026-11-30T08:00:00-03:00')
    const { plan, omitidos } = planDeTurnos({ zona: BSAS, ahoraMs: ahora, franjas: FRANJAS, prestaciones: PRESTACIONES })

    assert.equal(omitidos.length, 0)
    assert.equal(plan.length, TURNOS.length)
    assert.ok(plan.every((t) => Date.parse(t.inicio) > ahora))
    assert.equal(plan[0].inicio, '2026-11-30T09:00:00-03:00')
    assert.equal(plan.filter((t) => t.sobreturno).length, 1)
  })

  it('a la tarde omite los turnos de hoy que ya pasaron y deja los que vienen', () => {
    const ahora = Date.parse('2026-11-30T15:00:00-03:00')
    const { plan, omitidos } = planDeTurnos({ zona: BSAS, ahoraMs: ahora, franjas: FRANJAS, prestaciones: PRESTACIONES })

    assert.deepEqual(
      omitidos.map((t) => t.hora),
      ['09:00', '10:00', '10:05', '11:00'],
    )
    assert.ok(omitidos.every((t) => t.motivo === 'ya pasó'))
    assert.ok(plan.some((t) => t.inicio === '2026-11-30T15:30:00-03:00'))
  })

  it('si una prestacion no existe, omite ese turno con el motivo', () => {
    const ahora = Date.parse('2026-11-30T08:00:00-03:00')
    const sinEndodoncia = PRESTACIONES.filter((p) => p.nombre !== 'Endodoncia')
    const { omitidos } = planDeTurnos({ zona: BSAS, ahoraMs: ahora, franjas: FRANJAS, prestaciones: sinEndodoncia })

    assert.equal(omitidos.length, 2)
    assert.ok(omitidos.every((t) => t.motivo.includes('Endodoncia')))
  })

  it('los pacientes son ficticios: DNI unicos y telefonos de prueba 5555', () => {
    assert.equal(new Set(PACIENTES.map((p) => p.dni)).size, PACIENTES.length)
    assert.ok(PACIENTES.every((p) => p.telefono.startsWith('115555')))
    assert.ok(TURNOS.every((t) => t.paciente < PACIENTES.length))
  })

  it('con --desde arma todo a partir del dia de la demo, aunque hoy sea mas tarde', () => {
    const ahora = Date.parse('2026-11-26T15:00:00-03:00')
    const { plan, omitidos } = planDeTurnos({
      zona: BSAS,
      ahoraMs: ahora,
      franjas: FRANJAS,
      prestaciones: PRESTACIONES,
      desde: '2026-11-30',
    })

    assert.equal(omitidos.length, 0)
    assert.equal(plan[0].inicio, '2026-11-30T09:00:00-03:00')
    assert.equal(plan.at(-1).fecha, '2026-12-04')
  })
})
