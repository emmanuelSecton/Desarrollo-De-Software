import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { cuerpoDe, instalarApiFalsa, json, problema, sinContenido } from '../../test/apiFalsa'
import { AgendaPage, TEXTO_TURNO_MODIFICADO } from './AgendaPage'
import type { Turno } from './api'

// Jueves 26/11/2026 a las 12:00 en Buenos Aires. Datos ficticios (SU-07).
const AHORA = Date.parse('2026-11-26T15:00:00Z')
const ZONA = 'America/Argentina/Buenos_Aires'
const LIMPIEZA = { id: 'p-limpieza', nombre: 'Limpieza', duracionMinutos: 45, activa: true }
const CONTROL = { id: 'p-control', nombre: 'Control', duracionMinutos: 30, activa: true }

/** hora 'HH:mm' del 26/11 (o del dia indicado) en Buenos Aires → ISO UTC. */
const en = (hora: string, dia = '2026-11-26') => new Date(`${dia}T${hora}:00-03:00`).toISOString()

function turno(
  datos: Partial<Turno> & { id: string; desde: string; hasta: string; dia?: string },
): Turno {
  const { desde, hasta, dia, ...resto } = datos
  return {
    inicio: en(desde, dia),
    fin: en(hasta, dia),
    estado: 'RESERVADO',
    origen: 'INTERNO',
    sobreturno: false,
    version: 0,
    pacienteId: `pac-${datos.id}`,
    pacienteNombre: 'Lucía',
    pacienteApellido: 'Martínez',
    prestacionId: LIMPIEZA.id,
    prestacionNombre: 'Limpieza',
    ...resto,
  }
}

const TURNOS: Turno[] = [
  turno({ id: 't1', desde: '09:00', hasta: '09:45', estado: 'CONFIRMADO' }),
  turno({
    id: 't2',
    desde: '15:30',
    hasta: '16:00',
    pacienteNombre: 'Jorge',
    pacienteApellido: 'Pereyra',
    prestacionId: CONTROL.id,
    prestacionNombre: 'Control',
  }),
  turno({
    id: 't3',
    desde: '15:45',
    hasta: '16:15',
    sobreturno: true,
    pacienteNombre: 'Ana',
    pacienteApellido: 'Gómez',
  }),
  turno({
    id: 't4',
    desde: '11:00',
    hasta: '12:00',
    estado: 'CANCELADO',
    pacienteNombre: 'Carla',
    pacienteApellido: 'Ruiz',
  }),
  turno({
    id: 't5',
    desde: '10:00',
    hasta: '10:45',
    dia: '2026-11-28',
    pacienteNombre: 'Inés',
    pacienteApellido: 'Vega',
  }),
]

const LUCIA = {
  id: 'pac-lucia',
  dni: '30111222',
  nombre: 'Lucía',
  apellido: 'Martínez',
  telefono: '+5491155550101',
  email: null,
  activo: true,
}

function instalar(rutas = {}) {
  return instalarApiFalsa({
    'GET /consultorio': () => json(200, { confirmacionLimiteHoras: 24, zonaHoraria: ZONA }),
    'GET /horarios': () =>
      json(
        200,
        [1, 2, 3, 4, 5].flatMap((dia) => [
          { id: `m${dia}`, diaSemana: dia, desde: '09:00', hasta: '13:00' },
          { id: `t${dia}`, diaSemana: dia, desde: '15:00', hasta: '19:00' },
        ]),
      ),
    'GET /prestaciones': () => json(200, [LIMPIEZA, CONTROL]),
    'GET /turnos': (_: unknown, url: string) => {
      const desde = Date.parse(decodeURIComponent(url.split('desde=')[1].split('&')[0]))
      const hasta = Date.parse(decodeURIComponent(url.split('hasta=')[1]))
      return json(
        200,
        TURNOS.filter((t) => Date.parse(t.inicio) >= desde && Date.parse(t.inicio) < hasta),
      )
    },
    'GET /pacientes/pac-t2': () => json(200, { ...LUCIA, id: 'pac-t2', dni: '33777888' }),
    ...rutas,
  })
}

function Ubicacion() {
  const { search } = useLocation()
  return <div data-testid="ubicacion">{search}</div>
}

async function abrir(ruta = '/agenda') {
  render(
    <MemoryRouter initialEntries={[ruta]}>
      <Routes>
        <Route path="/agenda" element={<AgendaPage ahora={() => AHORA} />} />
      </Routes>
      <Ubicacion />
    </MemoryRouter>,
  )
  await screen.findByRole('group', { name: /Turnos del|Vista/ })
  await waitFor(() => expect(screen.queryByText('Cargando…')).not.toBeInTheDocument())
  return userEvent.setup()
}

const llamadas = (api: ReturnType<typeof instalarApiFalsa>, metodo: string, ruta: string) =>
  api.mock.calls.filter(
    ([url, init]) => (init?.method ?? 'GET') === metodo && String(url).includes(ruta),
  )

afterEach(() => vi.unstubAllGlobals())

describe('vista dia', () => {
  it('muestra los turnos de hoy en la zona del consultorio con estado por texto, sin los cancelados', async () => {
    instalar()
    await abrir()

    expect(screen.getByRole('heading', { name: 'Jueves 26 de noviembre' })).toBeVisible()
    expect(screen.getByText('Hoy · 3 turnos')).toBeVisible()
    expect(
      screen.getByRole('button', { name: '09:00, Lucía Martínez, Limpieza, Confirmado' }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: '15:30, Jorge Pereyra, Control, Sin confirmar' }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: '15:45, Ana Gómez, Limpieza, Sin confirmar, sobreturno' }),
    ).toBeVisible()
    expect(screen.queryByText(/Carla Ruiz/)).not.toBeInTheDocument()
    expect(screen.getByText(/Fuera del horario de atención · 13:00 a 15:00/)).toBeVisible()
  })

  it('"Cancelados visibles" muestra los cancelados con su etiqueta', async () => {
    instalar()
    const usuario = await abrir()

    await usuario.click(screen.getByRole('checkbox', { name: /Cancelados/ }))

    expect(
      screen.getByRole('button', { name: '11:00, Carla Ruiz, Limpieza, Cancelado' }),
    ).toBeVisible()
  })

  it('ofrece los huecos libres de lo que queda del dia y abre el alta con esa hora', async () => {
    instalar()
    const usuario = await abrir()

    expect(screen.queryByRole('button', { name: /Dar turno a las 9:45/ })).not.toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Libre · Dar turno a las 12:00' }))

    const dialogo = screen.getByRole('dialog', { name: 'Dar turno' })
    expect(within(dialogo).getByLabelText('Hora')).toHaveValue('12:00')
  })

  it('navega por dia y por semana, y lo deja en la URL', async () => {
    const api = instalar()
    const usuario = await abrir()

    await usuario.click(screen.getByRole('button', { name: 'Día siguiente' }))
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('fecha=2026-11-27')
    expect(await screen.findByRole('heading', { name: 'Viernes 27 de noviembre' })).toBeVisible()

    await usuario.click(screen.getByRole('button', { name: 'Semana' }))
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('vista=semana')
    const ultima = llamadas(api, 'GET', '/turnos').at(-1)![0] as string
    expect(decodeURIComponent(ultima)).toContain('desde=2026-11-23T00:00:00-03:00')
    expect(decodeURIComponent(ultima)).toContain('hasta=2026-11-30T00:00:00-03:00')
  })

  it('un dia sin turnos muestra el estado vacio con la accion de dar turno', async () => {
    instalar()
    await abrir('/agenda?fecha=2026-11-30')

    expect(screen.getByRole('region', { name: 'No hay turnos para este día' })).toBeVisible()
    expect(screen.getByRole('button', { name: 'Dar turno este día' })).toBeVisible()
  })
})

describe('vista semana', () => {
  it('muestra los siete dias con hoy marcado y lleva al dia elegido', async () => {
    instalar()
    const usuario = await abrir('/agenda?vista=semana&fecha=2026-11-26')

    expect(screen.getByRole('heading', { name: '23 al 29 de noviembre' })).toBeVisible()
    expect(screen.getAllByRole('region')).toHaveLength(7)
    expect(screen.getByRole('region', { name: 'Jue 26' })).toHaveAttribute('aria-current', 'date')
    expect(
      within(screen.getByRole('region', { name: 'Sáb 28' })).getByRole('button', {
        name: /Inés Vega/,
      }),
    ).toBeVisible()
    expect(
      within(screen.getByRole('region', { name: 'Dom 29' })).getByText('Sin horario de atención'),
    ).toBeVisible()

    await usuario.click(screen.getByRole('button', { name: 'Ver el Sáb 28' }))

    expect(screen.getByTestId('ubicacion')).toHaveTextContent('vista=dia')
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('fecha=2026-11-28')
  })
})

describe('dar turno', () => {
  async function abrirAlta(usuario: ReturnType<typeof userEvent.setup>) {
    await usuario.click(screen.getByRole('button', { name: 'Dar turno' }))
    return screen.getByRole('dialog', { name: 'Dar turno' })
  }

  async function elegirLucia(usuario: ReturnType<typeof userEvent.setup>, dialogo: HTMLElement) {
    await usuario.type(within(dialogo).getByLabelText('Buscar por DNI, nombre o teléfono'), 'Martí')
    await usuario.click(await within(dialogo).findByRole('button', { name: /Lucía Martínez/ }))
  }

  it('para un paciente existente: busca, elige y crea el turno en la zona del consultorio', async () => {
    const api = instalar({
      'GET /pacientes': () => json(200, { contenido: [LUCIA], total: 1 }),
      'POST /turnos': () => json(201, { id: 'nuevo' }),
    })
    const usuario = await abrir()
    const dialogo = await abrirAlta(usuario)
    expect(within(dialogo).getByLabelText('Buscar por DNI, nombre o teléfono')).toHaveFocus()

    await elegirLucia(usuario, dialogo)
    fireEvent.change(within(dialogo).getByLabelText('Hora'), { target: { value: '12:00' } })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Dar turno' }))

    expect(await screen.findByText('Turno dado')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'POST', '/turnos')[0])).toEqual({
      pacienteId: 'pac-lucia',
      prestacionId: 'p-limpieza',
      inicio: '2026-11-26T12:00:00-03:00',
      duracionMinutos: 45,
      sobreturno: false,
    })
    // Recarga la agenda despues de dar el turno.
    expect(llamadas(api, 'GET', '/turnos').length).toBeGreaterThan(1)
  })

  it('sin paciente ni hora no llama a la API y marca los campos', async () => {
    const api = instalar()
    const usuario = await abrir()
    const dialogo = await abrirAlta(usuario)

    await usuario.click(within(dialogo).getByRole('button', { name: 'Dar turno' }))

    expect(
      within(dialogo).getByLabelText('Buscar por DNI, nombre o teléfono'),
    ).toHaveAccessibleDescription('Elegí al paciente o dalo de alta')
    expect(within(dialogo).getByLabelText('Hora')).toHaveAttribute('aria-invalid', 'true')
    expect(llamadas(api, 'POST', '/turnos')).toHaveLength(0)
  })

  it('un horario ocupado dice con que turno choca y permite darlo como sobreturno', async () => {
    let intentos = 0
    const api = instalar({
      'GET /pacientes': () => json(200, { contenido: [LUCIA], total: 1 }),
      'POST /turnos': () =>
        ++intentos === 1
          ? problema(409, '/problemas/turno-superpuesto')
          : json(201, { id: 'nuevo' }),
    })
    const usuario = await abrir()
    const dialogo = await abrirAlta(usuario)
    await elegirLucia(usuario, dialogo)
    fireEvent.change(within(dialogo).getByLabelText('Hora'), { target: { value: '09:30' } })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Dar turno' }))

    const alerta = await within(dialogo).findByRole('alert')
    expect(alerta).toHaveTextContent('Ese horario se superpone con otro turno')
    expect(alerta).toHaveTextContent('09:00 a 09:45 · Lucía Martínez (Limpieza)')

    await usuario.click(within(alerta).getByRole('button', { name: 'Dar como sobreturno' }))

    expect(await screen.findByText('Sobreturno dado')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'POST', '/turnos')[1])).toMatchObject({ sobreturno: true })
  })

  it('da de alta un paciente en el momento, con el consentimiento obligatorio y errores por campo', async () => {
    let altas = 0
    const api = instalar({
      'GET /pacientes': () => json(200, { contenido: [], total: 0 }),
      'POST /pacientes': () =>
        ++altas === 1
          ? problema(400, '/problemas/validacion', 'Datos inválidos', {
              errores: [{ campo: 'telefono', mensaje: 'no es un teléfono válido para Argentina' }],
            })
          : json(201, { ...LUCIA, id: 'pac-nuevo', nombre: 'Valentina', apellido: 'Quiroga' }),
    })
    const usuario = await abrir()
    const dialogo = await abrirAlta(usuario)

    await usuario.click(within(dialogo).getByRole('button', { name: 'Nuevo paciente' }))
    await usuario.type(within(dialogo).getByLabelText('DNI'), '40555666')
    await usuario.type(within(dialogo).getByLabelText('Nombre'), 'Valentina')
    await usuario.type(within(dialogo).getByLabelText('Apellido'), 'Quiroga')
    await usuario.type(within(dialogo).getByLabelText('Teléfono'), '11 5555 01')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar paciente' }))

    expect(within(dialogo).getByRole('alert')).toHaveTextContent(
      'Sin la aceptación del tratamiento de datos',
    )
    expect(llamadas(api, 'POST', '/pacientes')).toHaveLength(0)

    await usuario.click(within(dialogo).getByRole('checkbox', { name: /política de privacidad/ }))
    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar paciente' }))
    expect(
      await within(dialogo).findByText('no es un teléfono válido para Argentina'),
    ).toBeVisible()
    expect(within(dialogo).getByLabelText('Teléfono')).toHaveAttribute('aria-invalid', 'true')

    await usuario.click(within(dialogo).getByRole('button', { name: 'Guardar paciente' }))
    expect(await within(dialogo).findByText('Valentina Quiroga')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'POST', '/pacientes')[0])).toMatchObject({
      dni: '40555666',
      consentimientoDatos: true,
      aceptaWhatsapp: true,
    })
  })
})

describe('acciones sobre un turno', () => {
  async function abrirTurno(usuario: ReturnType<typeof userEvent.setup>, nombre: RegExp) {
    await usuario.click(screen.getByRole('button', { name: nombre }))
    return screen.getByRole('dialog', { name: /Jorge Pereyra|Lucía Martínez|Carla Ruiz/ })
  }

  it('un RESERVADO se puede confirmar, reprogramar y cancelar; confirmar envia su version', async () => {
    const api = instalar({ 'POST /turnos/t2/confirmar': () => json(200, {}) })
    const usuario = await abrir()
    const panel = await abrirTurno(usuario, /Jorge Pereyra/)

    expect(
      within(panel).getByText('Faltan menos de 24 h y el paciente todavía no confirmó.'),
    ).toBeVisible()
    expect(await within(panel).findByText('33777888')).toBeVisible()
    expect(within(panel).getByRole('button', { name: 'Confirmar turno' })).toHaveFocus()
    expect(within(panel).getByRole('button', { name: 'Reprogramar' })).toBeVisible()

    await usuario.click(within(panel).getByRole('button', { name: 'Confirmar turno' }))

    expect(await screen.findByText('Turno confirmado')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'POST', '/turnos/t2/confirmar')[0])).toEqual({ version: 0 })
  })

  it('un CONFIRMADO no ofrece confirmar y un cancelado no ofrece acciones', async () => {
    instalar()
    const usuario = await abrir()

    const confirmado = await abrirTurno(usuario, /Lucía Martínez, Limpieza, Confirmado/)
    expect(
      within(confirmado).queryByRole('button', { name: 'Confirmar turno' }),
    ).not.toBeInTheDocument()
    await usuario.click(within(confirmado).getByRole('button', { name: 'Cerrar' }))

    await usuario.click(screen.getByRole('checkbox', { name: /Cancelados/ }))
    const cancelado = await abrirTurno(usuario, /Carla Ruiz/)
    expect(within(cancelado).getByText('Este turno ya no admite cambios.')).toBeVisible()
    expect(
      within(cancelado).queryByRole('button', { name: 'Cancelar turno' }),
    ).not.toBeInTheDocument()
  })

  it('cancelar exige motivo y envia quien cancela', async () => {
    const api = instalar({ 'POST /turnos/t2/cancelar': () => json(200, {}) })
    const usuario = await abrir()
    const panel = await abrirTurno(usuario, /Jorge Pereyra/)
    await usuario.click(within(panel).getByRole('button', { name: 'Cancelar turno' }))
    const dialogo = screen.getByRole('dialog', { name: 'Cancelar turno' })

    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar turno' }))
    expect(within(dialogo).getByLabelText('Motivo')).toHaveAttribute('aria-invalid', 'true')
    expect(llamadas(api, 'POST', '/cancelar')).toHaveLength(0)

    await usuario.click(within(dialogo).getByRole('radio', { name: 'El consultorio' }))
    await usuario.type(within(dialogo).getByLabelText('Motivo'), 'La odontóloga no atiende ese día')
    expect(within(dialogo).getByText('32/200')).toBeVisible()
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar turno' }))

    expect(await screen.findByText('Turno cancelado')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'POST', '/turnos/t2/cancelar')[0])).toEqual({
      version: 0,
      canceladoPor: 'CONSULTORIO',
      motivo: 'La odontóloga no atiende ese día',
    })
  })

  it('reprogramar envia el nuevo inicio; si choca, no ofrece sobreturno', async () => {
    let intentos = 0
    const api = instalar({
      'PUT /turnos/t2': () =>
        ++intentos === 1 ? problema(409, '/problemas/turno-superpuesto') : sinContenido(),
    })
    const usuario = await abrir()
    const panel = await abrirTurno(usuario, /Jorge Pereyra/)
    await usuario.click(within(panel).getByRole('button', { name: 'Reprogramar' }))
    const dialogo = screen.getByRole('dialog', { name: 'Reprogramar turno' })
    expect(within(dialogo).getByLabelText('Hora')).toHaveValue('15:30')

    fireEvent.change(within(dialogo).getByLabelText('Hora'), { target: { value: '09:15' } })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Reprogramar' }))

    const alerta = await within(dialogo).findByRole('alert')
    expect(alerta).toHaveTextContent('09:00 a 09:45 · Lucía Martínez (Limpieza)')
    expect(within(alerta).queryByRole('button', { name: /sobreturno/ })).not.toBeInTheDocument()

    fireEvent.change(within(dialogo).getByLabelText('Hora'), { target: { value: '17:00' } })
    await usuario.click(within(dialogo).getByRole('button', { name: 'Reprogramar' }))

    expect(await screen.findByText('Turno reprogramado')).toBeVisible()
    expect(cuerpoDe(llamadas(api, 'PUT', '/turnos/t2')[1])).toEqual({
      version: 0,
      inicio: '2026-11-26T17:00:00-03:00',
      duracionMinutos: 30,
    })
  })

  it('si otra persona modifico el turno, avisa y recarga la agenda', async () => {
    const api = instalar({
      'POST /turnos/t2/cancelar': () => problema(409, '/problemas/turno-modificado'),
    })
    const usuario = await abrir()
    const cargas = llamadas(api, 'GET', '/turnos?').length
    const panel = await abrirTurno(usuario, /Jorge Pereyra/)
    await usuario.click(within(panel).getByRole('button', { name: 'Cancelar turno' }))
    const dialogo = screen.getByRole('dialog', { name: 'Cancelar turno' })
    await usuario.type(within(dialogo).getByLabelText('Motivo'), 'Avisó por teléfono')
    await usuario.click(within(dialogo).getByRole('button', { name: 'Cancelar turno' }))

    expect(await screen.findByText(TEXTO_TURNO_MODIFICADO)).toBeVisible()
    expect(screen.queryByRole('dialog', { name: 'Cancelar turno' })).not.toBeInTheDocument()
    await waitFor(() => expect(llamadas(api, 'GET', '/turnos?').length).toBe(cargas + 1))
  })
})
