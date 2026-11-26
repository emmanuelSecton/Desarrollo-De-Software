import { vi } from 'vitest'

type Manejador = (init: RequestInit | undefined) => Response | Promise<Response>

export function json(status: number, cuerpo: unknown) {
  return new Response(JSON.stringify(cuerpo), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function problema(status: number, tipo: string, detalle = 'Detalle', extra = {}) {
  return new Response(
    JSON.stringify({ type: tipo, title: 'Título', status, detail: detalle, ...extra }),
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export const sinContenido = () => new Response(null, { status: 204 })

export const odontologo = {
  id: '00000000-0000-0000-0000-000000000001',
  nombre: 'Dra. Ana Ficticia',
  email: 'dra.ficticia@consultorio.test',
  rol: 'ODONTOLOGO',
  debeCambiarPassword: false,
} as const

export const secretaria = {
  id: '00000000-0000-0000-0000-000000000002',
  nombre: 'Lucía Martínez',
  email: 'lucia@consultorio.test',
  rol: 'SECRETARIA',
  debeCambiarPassword: false,
} as const

/**
 * Reemplaza fetch por un mapa "METODO /ruta" → respuesta. Lo no declarado responde 404.
 * Siempre deja una cookie XSRF-TOKEN, asi los POST no piden /auth/csrf.
 */
export function instalarApiFalsa(rutas: Record<string, Manejador>) {
  document.cookie = 'XSRF-TOKEN=token-de-prueba; path=/'
  const falso = vi.fn<typeof fetch>(async (entrada, init) => {
    const ruta = String(entrada).replace('/api/v1', '')
    const clave = `${init?.method ?? 'GET'} ${ruta}`
    const manejador = rutas[clave]
    return manejador ? manejador(init) : problema(404, '/problemas/no-encontrado')
  })
  vi.stubGlobal('fetch', falso)
  return falso
}

export function cuerpoDe(llamada: Parameters<typeof fetch>) {
  return JSON.parse(String(llamada[1]?.body))
}
