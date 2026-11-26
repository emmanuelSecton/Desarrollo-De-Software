import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { instalarApiFalsa, json, odontologo, problema, sinContenido } from '../../test/apiFalsa'
import { renderizarApp } from '../../test/renderizarApp'

afterEach(() => vi.unstubAllGlobals())

async function abrirCambio(rutas = {}) {
  const api = instalarApiFalsa({
    'GET /auth/me': () => json(200, { ...odontologo, debeCambiarPassword: true }),
    ...rutas,
  })
  renderizarApp('/cambiar-contrasena')
  await screen.findByRole('heading', { name: 'Elegí una contraseña nueva' })
  return { api, usuario: userEvent.setup() }
}

const llamoAlCambio = (api: ReturnType<typeof instalarApiFalsa>) =>
  api.mock.calls.some(([url]) => String(url).endsWith('/auth/password'))

describe('CambiarContrasenaPage', () => {
  it('valida en el navegador: actual vacia y nueva corta, sin llamar a la API', async () => {
    const { api, usuario } = await abrirCambio()

    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'corta')
    await usuario.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(screen.getByLabelText('Contraseña actual')).toHaveAccessibleDescription(
      'Ingresá tu contraseña actual',
    )
    expect(screen.getByLabelText('Contraseña actual')).toHaveFocus()
    const nueva = screen.getByLabelText('Contraseña nueva')
    expect(nueva).toHaveAttribute('aria-invalid', 'true')
    expect(nueva).toHaveAccessibleDescription(
      expect.stringContaining('La nueva contraseña debe tener al menos 10 caracteres'),
    )
    expect(llamoAlCambio(api)).toBe(false)
  })

  it('la nueva igual a la actual se rechaza antes de llamar a la API', async () => {
    const { api, usuario } = await abrirCambio()

    await usuario.type(screen.getByLabelText('Contraseña actual'), 'misma-clave-123')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'misma-clave-123')
    await usuario.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(screen.getByRole('alert')).toHaveTextContent(
      'La nueva contraseña debe ser distinta de la actual',
    )
    expect(llamoAlCambio(api)).toBe(false)
  })

  it('los requisitos se marcan al cumplirse, tambien con texto para lectores de pantalla', async () => {
    const { usuario } = await abrirCambio()
    const requisitos = () => screen.getAllByRole('listitem').map((li) => li.textContent)

    expect(requisitos()).toEqual([
      'Falta:Al menos 10 caracteres',
      'Falta:Distinta de la contraseña actual',
    ])

    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una-clave-nueva')

    expect(requisitos()).toEqual([
      'Cumple:Al menos 10 caracteres',
      'Cumple:Distinta de la contraseña actual',
    ])
  })

  it('el error de la API sobre la contraseña actual va a ese campo', async () => {
    const { usuario } = await abrirCambio({
      'POST /auth/password': () =>
        problema(400, '/problemas/password-invalida', 'La contraseña actual no es correcta'),
    })

    await usuario.type(screen.getByLabelText('Contraseña actual'), 'equivocada-1')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una-clave-nueva')
    await usuario.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    const actual = screen.getByLabelText('Contraseña actual')
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'La contraseña actual no es correcta',
    )
    expect(actual).toHaveAttribute('aria-invalid', 'true')
    expect(actual).toHaveFocus()
  })

  it('los errores por campo de la API se muestran junto a su campo', async () => {
    const { usuario } = await abrirCambio({
      'POST /auth/password': () =>
        problema(400, '/problemas/validacion', 'Datos inválidos', {
          errores: [{ campo: 'nueva', mensaje: 'el tamaño debe estar entre 0 y 200' }],
        }),
    })

    await usuario.type(screen.getByLabelText('Contraseña actual'), 'temporal-123')
    await usuario.type(screen.getByLabelText('Contraseña nueva'), 'una-clave-nueva')
    await usuario.click(screen.getByRole('button', { name: 'Guardar y continuar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('el tamaño debe estar entre 0 y 200')
    expect(screen.getByLabelText('Contraseña nueva')).toHaveAttribute('aria-invalid', 'true')
  })

  it('Salir cierra la sesion', async () => {
    const { usuario } = await abrirCambio({ 'POST /auth/logout': sinContenido })

    await usuario.click(screen.getByRole('button', { name: 'Salir' }))

    expect(await screen.findByRole('heading', { name: 'Ingresá al consultorio' })).toBeVisible()
  })
})
