import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cuerpoDe, instalarApiFalsa, json, odontologo, problema } from '../../test/apiFalsa'
import { renderizarApp } from '../../test/renderizarApp'
import { MENSAJE_CREDENCIALES } from './LoginPage'

const sinSesion = () => problema(401, '/problemas/no-autenticado')

afterEach(() => vi.unstubAllGlobals())

async function abrirLogin(rutas = {}) {
  const api = instalarApiFalsa({ 'GET /auth/me': sinSesion, ...rutas })
  renderizarApp('/login')
  await screen.findByRole('heading', { name: 'Ingresá al consultorio' })
  return { api, usuario: userEvent.setup() }
}

describe('LoginPage', () => {
  it('ingreso correcto: manda las credenciales y muestra la agenda con el nombre en la barra', async () => {
    const { api, usuario } = await abrirLogin({ 'POST /auth/login': () => json(200, odontologo) })

    await usuario.type(screen.getByLabelText('Email'), '  dra.ficticia@consultorio.test ')
    await usuario.type(screen.getByLabelText('Contraseña'), 'una-clave-larga')
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('heading', { name: 'Agenda', level: 1 })).toBeVisible()
    expect(screen.getByRole('banner')).toHaveTextContent('Dra. Ana Ficticia')
    const llamada = api.mock.calls.find(([url]) => String(url).endsWith('/auth/login'))!
    expect(cuerpoDe(llamada)).toEqual({
      email: 'dra.ficticia@consultorio.test',
      password: 'una-clave-larga',
    })
    expect(new Headers(llamada[1]?.headers).get('X-XSRF-TOKEN')).toBe('token-de-prueba')
  })

  it('401: mensaje generico en role="alert", email invalido y con foco, contraseña vacia', async () => {
    const { usuario } = await abrirLogin({
      'POST /auth/login': () => problema(401, '/problemas/credenciales-invalidas'),
    })

    await usuario.type(screen.getByLabelText('Email'), 'alguien@consultorio.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'equivocada-123')
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }))

    const alerta = await screen.findByRole('alert')
    expect(alerta).toHaveTextContent(MENSAJE_CREDENCIALES)
    const email = screen.getByLabelText('Email')
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription(MENSAJE_CREDENCIALES)
    expect(email).toHaveFocus()
    expect(screen.getByLabelText('Contraseña')).toHaveValue('')
  })

  it('campos vacios: los marca invalidos con su mensaje sin llamar a la API', async () => {
    const { api, usuario } = await abrirLogin()

    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }))

    const email = screen.getByLabelText('Email')
    expect(email).toHaveAttribute('aria-invalid', 'true')
    expect(email).toHaveAccessibleDescription('Ingresá tu email')
    expect(screen.getByLabelText('Contraseña')).toHaveAccessibleDescription('Ingresá tu contraseña')
    expect(email).toHaveFocus()
    expect(api.mock.calls.some(([url]) => String(url).endsWith('/auth/login'))).toBe(false)
  })

  it('429: muestra el detalle de la API', async () => {
    const { usuario } = await abrirLogin({
      'POST /auth/login': () =>
        problema(429, '/problemas/demasiadas-peticiones', 'Demasiados intentos. Esperá un minuto.'),
    })

    await usuario.type(screen.getByLabelText('Email'), 'a@b.test')
    await usuario.type(screen.getByLabelText('Contraseña'), 'x')
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Demasiados intentos. Esperá un minuto.',
    )
  })

  it('el boton del ojo muestra y oculta la contraseña', async () => {
    const { usuario } = await abrirLogin()
    const password = screen.getByLabelText('Contraseña')

    await usuario.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(password).toHaveAttribute('type', 'text')

    await usuario.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(password).toHaveAttribute('type', 'password')
  })
})
