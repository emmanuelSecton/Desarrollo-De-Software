import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ErrorDeApi, api, eventosDeSesion } from './cliente'

function respuestaJson(status: number, cuerpo: unknown, tipo = 'application/json') {
  return new Response(JSON.stringify(cuerpo), { status, headers: { 'Content-Type': tipo } })
}

function problema(status: number, type: string, extra: Record<string, unknown> = {}) {
  return respuestaJson(
    status,
    { type, title: 'Título', status, detail: 'Detalle del problema', ...extra },
    'application/problem+json',
  )
}

function borrarCookies() {
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
}

describe('cliente HTTP', () => {
  const fetchFalso = vi.fn<typeof fetch>()

  beforeEach(() => {
    fetchFalso.mockReset()
    vi.stubGlobal('fetch', fetchFalso)
    borrarCookies()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('hace GET sobre /api/v1 con la cookie de sesion del mismo origen y devuelve el JSON', async () => {
    fetchFalso.mockResolvedValueOnce(respuestaJson(200, { nombre: 'Ana' }))

    const datos = await api<{ nombre: string }>('/auth/me')

    expect(datos).toEqual({ nombre: 'Ana' })
    const [url, init] = fetchFalso.mock.calls[0]
    expect(url).toBe('/api/v1/auth/me')
    expect(init?.credentials).toBe('same-origin')
    expect(new Headers(init?.headers).has('X-XSRF-TOKEN')).toBe(false)
  })

  it('en POST agrega X-XSRF-TOKEN con la cookie y manda el cuerpo como JSON', async () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/'
    fetchFalso.mockResolvedValueOnce(new Response(null, { status: 204 }))

    const datos = await api('/auth/logout', { metodo: 'POST', cuerpo: { a: 1 } })

    expect(datos).toBeUndefined()
    const [, init] = fetchFalso.mock.calls[0]
    const cabeceras = new Headers(init?.headers)
    expect(init?.method).toBe('POST')
    expect(cabeceras.get('X-XSRF-TOKEN')).toBe('abc123')
    expect(cabeceras.get('Content-Type')).toBe('application/json')
    expect(init?.body).toBe('{"a":1}')
  })

  it('si falta la cookie CSRF la pide primero a /auth/csrf', async () => {
    fetchFalso.mockImplementationOnce(async () => {
      document.cookie = 'XSRF-TOKEN=nuevo; path=/'
      return respuestaJson(200, { token: 'nuevo', cabecera: 'X-XSRF-TOKEN' })
    })
    fetchFalso.mockResolvedValueOnce(new Response(null, { status: 204 }))

    await api('/auth/password', { metodo: 'POST', cuerpo: {} })

    expect(fetchFalso.mock.calls[0][0]).toBe('/api/v1/auth/csrf')
    const [url, init] = fetchFalso.mock.calls[1]
    expect(url).toBe('/api/v1/auth/password')
    expect(new Headers(init?.headers).get('X-XSRF-TOKEN')).toBe('nuevo')
  })

  it('convierte un problem+json en ErrorDeApi con los errores por campo', async () => {
    fetchFalso.mockResolvedValueOnce(
      problema(400, '/problemas/validacion', {
        errores: [{ campo: 'email', mensaje: 'no debe estar vacío' }],
      }),
    )

    const error = await api('/pacientes').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorDeApi)
    expect(error).toMatchObject({
      status: 400,
      tipo: '/problemas/validacion',
      titulo: 'Título',
      detalle: 'Detalle del problema',
      errores: [{ campo: 'email', mensaje: 'no debe estar vacío' }],
    })
  })

  it('una respuesta de error sin problem+json igual se convierte en ErrorDeApi', async () => {
    fetchFalso.mockResolvedValueOnce(new Response('falla', { status: 502 }))

    const error = await api('/pacientes').catch((e: unknown) => e)

    expect(error).toMatchObject({ status: 502, tipo: 'about:blank', errores: [] })
  })

  it('un 401 no-autenticado avisa que la sesion vencio', async () => {
    const escucha = vi.fn()
    eventosDeSesion.addEventListener('sesion-vencida', escucha)
    fetchFalso.mockResolvedValueOnce(problema(401, '/problemas/no-autenticado'))

    await expect(api('/pacientes')).rejects.toBeInstanceOf(ErrorDeApi)

    expect(escucha).toHaveBeenCalledTimes(1)
    eventosDeSesion.removeEventListener('sesion-vencida', escucha)
  })

  it('el 401 del login por credenciales no avisa sesion vencida', async () => {
    const escucha = vi.fn()
    eventosDeSesion.addEventListener('sesion-vencida', escucha)
    document.cookie = 'XSRF-TOKEN=abc; path=/'
    fetchFalso.mockResolvedValueOnce(problema(401, '/problemas/credenciales-invalidas'))

    await expect(api('/auth/login', { metodo: 'POST', cuerpo: {} })).rejects.toMatchObject({
      status: 401,
    })

    expect(escucha).not.toHaveBeenCalled()
    eventosDeSesion.removeEventListener('sesion-vencida', escucha)
  })

  it('se puede pedir que un 401 no avise (carga inicial sin sesion)', async () => {
    const escucha = vi.fn()
    eventosDeSesion.addEventListener('sesion-vencida', escucha)
    fetchFalso.mockResolvedValueOnce(problema(401, '/problemas/no-autenticado'))

    await expect(api('/auth/me', { avisarSesionVencida: false })).rejects.toBeInstanceOf(ErrorDeApi)

    expect(escucha).not.toHaveBeenCalled()
    eventosDeSesion.removeEventListener('sesion-vencida', escucha)
  })

  it('un 403 de cambio de contraseña pendiente avisa para ir a /cambiar-contrasena', async () => {
    const escucha = vi.fn()
    eventosDeSesion.addEventListener('cambio-pendiente', escucha)
    fetchFalso.mockResolvedValueOnce(problema(403, '/problemas/cambio-password-requerido'))

    await expect(api('/pacientes')).rejects.toMatchObject({ status: 403 })

    expect(escucha).toHaveBeenCalledTimes(1)
    eventosDeSesion.removeEventListener('cambio-pendiente', escucha)
  })

  it('si un POST es rechazado por CSRF renueva el token y reintenta una sola vez', async () => {
    document.cookie = 'XSRF-TOKEN=viejo; path=/'
    fetchFalso.mockResolvedValueOnce(problema(403, '/problemas/acceso-denegado'))
    fetchFalso.mockImplementationOnce(async () => {
      document.cookie = 'XSRF-TOKEN=fresco; path=/'
      return respuestaJson(200, { token: 'fresco', cabecera: 'X-XSRF-TOKEN' })
    })
    fetchFalso.mockResolvedValueOnce(respuestaJson(200, { ok: true }))

    const datos = await api('/auth/login', { metodo: 'POST', cuerpo: {} })

    expect(datos).toEqual({ ok: true })
    expect(fetchFalso).toHaveBeenCalledTimes(3)
    expect(new Headers(fetchFalso.mock.calls[2][1]?.headers).get('X-XSRF-TOKEN')).toBe('fresco')
  })
})
