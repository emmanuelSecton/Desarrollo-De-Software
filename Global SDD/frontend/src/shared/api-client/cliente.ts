// Cliente HTTP de la API REST (C-07, D3). Ninguna pantalla interpreta a mano la cookie CSRF,
// un ProblemDetail ni un 401: todo pasa por aca.

export const BASE_API = '/api/v1'

const COOKIE_CSRF = 'XSRF-TOKEN'
const CABECERA_CSRF = 'X-XSRF-TOKEN'
const METODOS_QUE_MODIFICAN = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export const PROBLEMAS = {
  noAutenticado: '/problemas/no-autenticado',
  credencialesInvalidas: '/problemas/credenciales-invalidas',
  cambioPasswordRequerido: '/problemas/cambio-password-requerido',
  accesoDenegado: '/problemas/acceso-denegado',
  passwordInvalida: '/problemas/password-invalida',
} as const

export interface ErrorDeCampo {
  campo: string
  mensaje: string
}

export class ErrorDeApi extends Error {
  readonly status: number
  readonly tipo: string
  readonly titulo: string
  readonly detalle: string
  readonly errores: ErrorDeCampo[]

  constructor(datos: {
    status: number
    tipo: string
    titulo: string
    detalle: string
    errores: ErrorDeCampo[]
  }) {
    super(datos.detalle || datos.titulo)
    this.name = 'ErrorDeApi'
    this.status = datos.status
    this.tipo = datos.tipo
    this.titulo = datos.titulo
    this.detalle = datos.detalle
    this.errores = datos.errores
  }
}

/** Avisos para el contexto de sesion: 'sesion-vencida' y 'cambio-pendiente'. */
export const eventosDeSesion = new EventTarget()

export interface OpcionesDePeticion {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  cuerpo?: unknown
  /** false en la carga inicial: sin sesion todavia no hay nada que haya vencido. */
  avisarSesionVencida?: boolean
}

function leerCookie(nombre: string): string | undefined {
  const prefijo = `${nombre}=`
  const valor = document.cookie
    .split(';')
    .map((parte) => parte.trim())
    .find((parte) => parte.startsWith(prefijo))
    ?.slice(prefijo.length)
  return valor ? decodeURIComponent(valor) : undefined
}

async function renovarTokenCsrf(): Promise<string | undefined> {
  await fetch(`${BASE_API}/auth/csrf`, { credentials: 'same-origin' })
  return leerCookie(COOKIE_CSRF)
}

async function aErrorDeApi(respuesta: Response): Promise<ErrorDeApi> {
  let cuerpo: Record<string, unknown> = {}
  if (respuesta.headers.get('Content-Type')?.includes('json')) {
    cuerpo = await respuesta.json().catch(() => ({}))
  }
  const errores = Array.isArray(cuerpo.errores) ? (cuerpo.errores as ErrorDeCampo[]) : []
  return new ErrorDeApi({
    status: respuesta.status,
    tipo: typeof cuerpo.type === 'string' ? cuerpo.type : 'about:blank',
    titulo: typeof cuerpo.title === 'string' ? cuerpo.title : respuesta.statusText,
    detalle: typeof cuerpo.detail === 'string' ? cuerpo.detail : '',
    errores,
  })
}

async function enviar(ruta: string, metodo: string, cuerpo: unknown, token?: string) {
  const cabeceras = new Headers({ Accept: 'application/json, application/problem+json' })
  if (cuerpo !== undefined) cabeceras.set('Content-Type', 'application/json')
  if (token) cabeceras.set(CABECERA_CSRF, token)
  return fetch(`${BASE_API}${ruta}`, {
    method: metodo,
    credentials: 'same-origin',
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  })
}

export async function api<T = void>(ruta: string, opciones: OpcionesDePeticion = {}): Promise<T> {
  const metodo = opciones.metodo ?? 'GET'
  const modifica = METODOS_QUE_MODIFICAN.has(metodo)

  let token = modifica ? (leerCookie(COOKIE_CSRF) ?? (await renovarTokenCsrf())) : undefined
  let respuesta = await enviar(ruta, metodo, opciones.cuerpo, token)

  if (modifica && respuesta.status === 403) {
    const error = await aErrorDeApi(respuesta.clone())
    // Un token CSRF viejo (p. ej. tras un nuevo login) se rechaza como acceso denegado: se renueva una vez.
    if (error.tipo === PROBLEMAS.accesoDenegado) {
      token = await renovarTokenCsrf()
      respuesta = await enviar(ruta, metodo, opciones.cuerpo, token)
    }
  }

  if (!respuesta.ok) {
    const error = await aErrorDeApi(respuesta)
    if (error.status === 401 && error.tipo === PROBLEMAS.noAutenticado) {
      if (opciones.avisarSesionVencida !== false) {
        eventosDeSesion.dispatchEvent(new Event('sesion-vencida'))
      }
    } else if (error.status === 403 && error.tipo === PROBLEMAS.cambioPasswordRequerido) {
      eventosDeSesion.dispatchEvent(new Event('cambio-pendiente'))
    }
    throw error
  }

  if (respuesta.status === 204 || !respuesta.headers.get('Content-Type')?.includes('json')) {
    return undefined as T
  }
  return (await respuesta.json()) as T
}
