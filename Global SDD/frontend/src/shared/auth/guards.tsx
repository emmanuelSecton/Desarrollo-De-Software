import type { ReactNode } from 'react'
import { Navigate, Outlet, useLocation, useSearchParams } from 'react-router'
import { Cargando } from '../ui/Cargando'
import { useSesion, type Rol } from './sesion'

export const RUTA_INICIAL = '/agenda'
export const RUTA_CAMBIO = '/cambiar-contrasena'

/** Solo acepta rutas internas: evita que ?volver= lleve a otro sitio. */
export function rutaDeVuelta(volver: string | null): string {
  if (!volver || !volver.startsWith('/') || volver.startsWith('//') || volver.startsWith('/\\')) {
    return RUTA_INICIAL
  }
  if (volver.startsWith('/login') || volver.startsWith(RUTA_CAMBIO)) return RUTA_INICIAL
  return volver
}

/** Rutas de la app: exige sesion y, si hay cambio de contraseña pendiente, manda a hacerlo. */
export function RequiereSesion() {
  const { estado } = useSesion()
  const ubicacion = useLocation()

  if (estado.tipo === 'cargando') return <Cargando pantallaCompleta />
  if (estado.tipo === 'sin-sesion') {
    const destino =
      estado.motivo === 'salida'
        ? '/login'
        : `/login?volver=${encodeURIComponent(ubicacion.pathname + ubicacion.search)}`
    return <Navigate to={destino} replace />
  }
  if (estado.usuario.debeCambiarPassword) return <Navigate to={RUTA_CAMBIO} replace />
  return <Outlet />
}

/** El login: con sesion lleva a la ruta pedida (o al cambio de contraseña pendiente). */
export function SoloSinSesion() {
  const { estado } = useSesion()
  const [parametros] = useSearchParams()

  if (estado.tipo === 'cargando') return <Cargando pantallaCompleta />
  if (estado.tipo === 'con-sesion') {
    const destino = estado.usuario.debeCambiarPassword
      ? RUTA_CAMBIO
      : rutaDeVuelta(parametros.get('volver'))
    return <Navigate to={destino} replace />
  }
  return <Outlet />
}

/** La pantalla de cambio obligatorio: solo con sesion y con el cambio pendiente. */
export function RequiereCambioPendiente() {
  const { estado } = useSesion()

  if (estado.tipo === 'cargando') return <Cargando pantallaCompleta />
  if (estado.tipo === 'sin-sesion') return <Navigate to="/login" replace />
  if (!estado.usuario.debeCambiarPassword) return <Navigate to={RUTA_INICIAL} replace />
  return <Outlet />
}

/** Una seccion restringida a un rol: a los demas les muestra el acceso denegado. */
export function RequiereRol({ rol, denegado }: { rol: Rol; denegado: ReactNode }) {
  const { usuario } = useSesion()
  return usuario?.rol === rol ? <Outlet /> : denegado
}
