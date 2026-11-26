import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { eventosDeSesion } from './shared/api-client/cliente'
import { rutaDeVuelta } from './shared/auth/guards'
import {
  cuerpoDe,
  instalarApiFalsa,
  json,
  odontologo,
  problema,
  secretaria,
  sinContenido,
} from './test/apiFalsa'
import { renderizarApp } from './test/renderizarApp'

const sinSesion = () => problema(401, '/problemas/no-autenticado')

afterEach(() => vi.unstubAllGlobals())

describe('sesion y guards', () => {
  it('sin sesion lleva al login conservando la ruta pedida', async () => {
    instalarApiFalsa({ 'GET /auth/me': sinSesion })

    renderizarApp('/pacientes?q=ana')

    expect(await screen.findByRole('heading', { name: 'Ingresá al consultorio' })).toBeVisible()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent(
      `/login?volver=${encodeURIComponent('/pacientes?q=ana')}`,
    )
    // La carga inicial sin sesion no es una sesion vencida.
    expect(screen.queryByText('Tu sesión venció.')).not.toBeInTheDocument()
  })

  it('con sesion vigente, al recargar sigue en la misma seccion', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })

    renderizarApp('/pacientes')

    expect(await screen.findByRole('heading', { name: 'Pacientes', level: 1 })).toBeVisible()
    expect(screen.getByText('Dra. Ana Ficticia')).toBeVisible()
  })

  it('la raiz lleva a la agenda', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })

    renderizarApp('/')

    expect(await screen.findByRole('heading', { name: 'Agenda', level: 1 })).toBeVisible()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/agenda')
  })

  it('al ingresar vuelve a la ruta pedida', async () => {
    instalarApiFalsa({
      'GET /auth/me': sinSesion,
      'POST /auth/login': () => json(200, odontologo),
    })
    renderizarApp('/pacientes')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Email'), 'dra.ficticia@consultorio.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'una-clave-larga')
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('heading', { name: 'Pacientes', level: 1 })).toBeVisible()
  })

  it('con el cambio de contraseña pendiente solo deja ver esa pantalla', async () => {
    instalarApiFalsa({
      'GET /auth/me': () => json(200, { ...odontologo, debeCambiarPassword: true }),
    })

    renderizarApp('/agenda')

    expect(await screen.findByRole('heading', { name: 'Elegí una contraseña nueva' })).toBeVisible()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent('/cambiar-contrasena')
  })

  it('sin cambio pendiente, /cambiar-contrasena lleva a la agenda', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, secretaria) })

    renderizarApp('/cambiar-contrasena')

    expect(await screen.findByRole('heading', { name: 'Agenda', level: 1 })).toBeVisible()
  })

  it('un 403 de cambio pendiente durante el uso lleva a /cambiar-contrasena', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })
    renderizarApp('/agenda')
    await screen.findByRole('heading', { name: 'Agenda', level: 1 })

    act(() => {
      eventosDeSesion.dispatchEvent(new Event('cambio-pendiente'))
    })

    expect(await screen.findByRole('heading', { name: 'Elegí una contraseña nueva' })).toBeVisible()
  })

  it('un 401 durante el uso vuelve al login con el aviso y conserva la seccion', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, secretaria) })
    renderizarApp('/pacientes')
    await screen.findByRole('heading', { name: 'Pacientes', level: 1 })

    act(() => {
      eventosDeSesion.dispatchEvent(new Event('sesion-vencida'))
    })

    expect(await screen.findByText('Tu sesión venció.')).toBeVisible()
    expect(screen.getByRole('status')).toHaveTextContent(
      'Volvé a ingresar para seguir donde estabas.',
    )
    expect(screen.getByTestId('ubicacion')).toHaveTextContent(
      `/login?volver=${encodeURIComponent('/pacientes')}`,
    )
  })

  it('salir cierra la sesion en la API y va al login sin ruta de vuelta', async () => {
    const api = instalarApiFalsa({
      'GET /auth/me': () => json(200, odontologo),
      'POST /auth/logout': sinContenido,
    })
    renderizarApp('/agenda')
    const usuario = userEvent.setup()

    const barra = within(await screen.findByRole('banner'))
    await usuario.click(barra.getByRole('button', { name: 'Salir' }))

    expect(await screen.findByRole('heading', { name: 'Ingresá al consultorio' })).toBeVisible()
    expect(screen.getByTestId('ubicacion')).toHaveTextContent(/^\/login$/)
    expect(
      api.mock.calls.some(
        ([url, init]) => String(url).endsWith('/auth/logout') && init?.method === 'POST',
      ),
    ).toBe(true)
  })

  it('?volver= solo acepta rutas internas', () => {
    expect(rutaDeVuelta('/pacientes?q=1')).toBe('/pacientes?q=1')
    expect(rutaDeVuelta('https://otro.sitio')).toBe('/agenda')
    expect(rutaDeVuelta('//otro.sitio')).toBe('/agenda')
    expect(rutaDeVuelta('/\\otro.sitio')).toBe('/agenda')
    expect(rutaDeVuelta('/login')).toBe('/agenda')
    expect(rutaDeVuelta(null)).toBe('/agenda')
  })
})

describe('navegacion segun el rol', () => {
  it('el odontologo ve Agenda, Pacientes y Configuracion', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })

    renderizarApp('/agenda')

    const nav = within(await screen.findByRole('navigation', { name: 'Secciones' }))
    expect(nav.getAllByRole('link').map((a) => a.textContent)).toEqual([
      'Agenda',
      'Pacientes',
      'Configuración',
    ])
    expect(nav.getByRole('link', { name: 'Agenda' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByText('Odontología')).toBeVisible()
  })

  it('la secretaria ve Agenda y Pacientes, y no Configuracion', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, secretaria) })

    renderizarApp('/agenda')

    const nav = within(await screen.findByRole('navigation', { name: 'Secciones' }))
    expect(nav.getAllByRole('link').map((a) => a.textContent)).toEqual(['Agenda', 'Pacientes'])
    expect(screen.queryByRole('link', { name: 'Configuración' })).not.toBeInTheDocument()
  })

  it('la secretaria que abre /configuracion ve el acceso denegado', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, secretaria) })

    renderizarApp('/configuracion')

    expect(
      await screen.findByRole('heading', { name: 'No tenés acceso a esta sección' }),
    ).toBeVisible()
    expect(screen.getByRole('link', { name: 'Ir a la agenda' })).toHaveAttribute('href', '/agenda')
  })

  it('el odontologo abre /configuracion', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })

    renderizarApp('/configuracion')

    expect(await screen.findByRole('heading', { name: 'Configuración', level: 1 })).toBeVisible()
    expect(screen.getByText('Disponible próximamente')).toBeVisible()
  })

  it('una ruta inexistente muestra la pagina no encontrada con enlace a la agenda', async () => {
    instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo) })

    renderizarApp('/no-existe')

    expect(await screen.findByRole('heading', { name: 'No encontramos esa página' })).toBeVisible()
    expect(screen.getByRole('link', { name: 'Ir a la agenda' })).toHaveAttribute('href', '/agenda')
  })
})

describe('cambio de contraseña exitoso', () => {
  it('lleva a la agenda con el aviso "Contraseña actualizada"', async () => {
    let pendiente = true
    const api = instalarApiFalsa({
      'GET /auth/me': () => json(200, { ...odontologo, debeCambiarPassword: pendiente }),
      'POST /auth/password': () => {
        pendiente = false
        return sinContenido()
      },
    })
    renderizarApp('/agenda')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Contraseña actual'), 'temporal-123')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una-clave-nueva')
    await usuario.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(await screen.findByRole('heading', { name: 'Agenda', level: 1 })).toBeVisible()
    expect(screen.getByRole('status')).toHaveTextContent('Contraseña actualizada')
    const llamada = api.mock.calls.find(([url]) => String(url).endsWith('/auth/password'))!
    expect(cuerpoDe(llamada)).toEqual({ actual: 'temporal-123', nueva: 'una-clave-nueva' })

    await usuario.click(screen.getByRole('button', { name: 'Cerrar aviso' }))
    expect(screen.queryByText('Contraseña actualizada')).not.toBeInTheDocument()
  })
})
