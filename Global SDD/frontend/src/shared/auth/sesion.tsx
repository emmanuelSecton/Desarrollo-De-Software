import {
  createContext,
  use,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { api, eventosDeSesion } from '../api-client/cliente'

// Contexto de sesion (C-07, D4). La UI oculta lo que el rol no puede usar, pero el control
// real lo hace la API (C-03).

export type Rol = 'ODONTOLOGO' | 'SECRETARIA'

export interface Usuario {
  id: string
  nombre: string
  email: string
  rol: Rol
  debeCambiarPassword: boolean
}

/** Por que no hay sesion: define si el login conserva la ruta pedida y si avisa que vencio. */
export type MotivoSinSesion = 'inicial' | 'vencida' | 'salida'

export type EstadoDeSesion =
  | { tipo: 'cargando' }
  | { tipo: 'sin-sesion'; motivo: MotivoSinSesion }
  | { tipo: 'con-sesion'; usuario: Usuario }

export interface ValorDeSesion {
  estado: EstadoDeSesion
  usuario: Usuario | null
  /** Aviso de una sola vez para la siguiente pantalla (p. ej. "Contraseña actualizada"). */
  aviso: string | null
  descartarAviso: () => void
  ingresar: (email: string, password: string) => Promise<Usuario>
  salir: () => Promise<void>
  /** Cierra la sesion porque vencio la inactividad: el login muestra el aviso de sesion vencida. */
  expirar: () => Promise<void>
  cambiarContrasena: (actual: string, nueva: string) => Promise<void>
  /** Llama a /auth/me, lo que renueva la sesion del servidor. */
  renovar: () => Promise<void>
}

const ContextoDeSesion = createContext<ValorDeSesion | null>(null)

const RUTA_YO = '/auth/me'

export function ProveedorDeSesion({ children }: { children: ReactNode }) {
  const [estado, setEstado] = useState<EstadoDeSesion>({ tipo: 'cargando' })
  const [aviso, setAviso] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    api<Usuario>(RUTA_YO, { avisarSesionVencida: false })
      .then((usuario) => vigente && setEstado({ tipo: 'con-sesion', usuario }))
      .catch(() => vigente && setEstado({ tipo: 'sin-sesion', motivo: 'inicial' }))
    return () => {
      vigente = false
    }
  }, [])

  useEffect(() => {
    const alVencer = () =>
      setEstado((previo) =>
        previo.tipo === 'con-sesion' ? { tipo: 'sin-sesion', motivo: 'vencida' } : previo,
      )
    const alPedirCambio = () =>
      setEstado((previo) =>
        previo.tipo === 'con-sesion'
          ? { tipo: 'con-sesion', usuario: { ...previo.usuario, debeCambiarPassword: true } }
          : previo,
      )
    eventosDeSesion.addEventListener('sesion-vencida', alVencer)
    eventosDeSesion.addEventListener('cambio-pendiente', alPedirCambio)
    return () => {
      eventosDeSesion.removeEventListener('sesion-vencida', alVencer)
      eventosDeSesion.removeEventListener('cambio-pendiente', alPedirCambio)
    }
  }, [])

  const ingresar = useCallback(async (email: string, password: string) => {
    const usuario = await api<Usuario>('/auth/login', {
      metodo: 'POST',
      cuerpo: { email, password },
    })
    setEstado({ tipo: 'con-sesion', usuario })
    return usuario
  }, [])

  const cerrar = useCallback(async (motivo: MotivoSinSesion) => {
    try {
      await api('/auth/logout', { metodo: 'POST', avisarSesionVencida: false })
    } catch {
      // Aunque falle el logout, en este navegador la sesion se da por cerrada.
    } finally {
      setAviso(null)
      setEstado({ tipo: 'sin-sesion', motivo })
    }
  }, [])

  const salir = useCallback(() => cerrar('salida'), [cerrar])
  const expirar = useCallback(() => cerrar('vencida'), [cerrar])

  const renovar = useCallback(async () => {
    const usuario = await api<Usuario>(RUTA_YO)
    setEstado({ tipo: 'con-sesion', usuario })
  }, [])

  const cambiarContrasena = useCallback(
    async (actual: string, nueva: string) => {
      await api('/auth/password', { metodo: 'POST', cuerpo: { actual, nueva } })
      setAviso('Contraseña actualizada')
      await renovar()
    },
    [renovar],
  )

  const valor = useMemo<ValorDeSesion>(
    () => ({
      estado,
      usuario: estado.tipo === 'con-sesion' ? estado.usuario : null,
      aviso,
      descartarAviso: () => setAviso(null),
      ingresar,
      salir,
      expirar,
      cambiarContrasena,
      renovar,
    }),
    [estado, aviso, ingresar, salir, expirar, cambiarContrasena, renovar],
  )

  return <ContextoDeSesion value={valor}>{children}</ContextoDeSesion>
}

export function useSesion(): ValorDeSesion {
  const valor = use(ContextoDeSesion)
  if (!valor) throw new Error('useSesion necesita un ProveedorDeSesion')
  return valor
}
