import { CalendarDays, LogOut, SlidersHorizontal, Users, type LucideIcon } from 'lucide-react'
import { NavLink, Outlet } from 'react-router'
import { useSesion, type Rol } from '../shared/auth/sesion'
import { EtiquetaProvisoria, Marca } from '../shared/marca/Marca'
import { Aviso } from '../shared/ui/Aviso'
import { Boton } from '../shared/ui/Boton'
import { AvisoDeInactividad } from './AvisoDeInactividad'
import estilos from './EstructuraApp.module.css'

interface Seccion {
  ruta: string
  texto: string
  Icono: LucideIcon
  soloOdontologo?: boolean
}

export const SECCIONES: Seccion[] = [
  { ruta: '/agenda', texto: 'Agenda', Icono: CalendarDays },
  { ruta: '/pacientes', texto: 'Pacientes', Icono: Users },
  {
    ruta: '/configuracion',
    texto: 'Configuración',
    Icono: SlidersHorizontal,
    soloOdontologo: true,
  },
]

const NOMBRE_DEL_ROL: Record<Rol, string> = {
  ODONTOLOGO: 'Odontología',
  SECRETARIA: 'Secretaría',
}

/** "Dra. Ana Ficticia" → "AF": ignora titulos abreviados. */
export function iniciales(nombre: string): string {
  return nombre
    .split(/\s+/)
    .filter((palabra) => palabra && !palabra.endsWith('.'))
    .slice(0, 2)
    .map((palabra) => palabra[0].toLocaleUpperCase('es-AR'))
    .join('')
}

/** Barra superior, navegacion por rol (lateral en escritorio, inferior en celular) y contenido. */
export function EstructuraApp() {
  const { usuario, salir, aviso, descartarAviso } = useSesion()
  if (!usuario) return null

  const secciones = SECCIONES.filter((s) => !s.soloOdontologo || usuario.rol === 'ODONTOLOGO')
  const enlace = (s: Seccion) => (
    <NavLink to={s.ruta}>
      <s.Icono aria-hidden="true" />
      <span>{s.texto}</span>
    </NavLink>
  )

  return (
    <div className={estilos.app}>
      <header className={estilos.barra}>
        <Marca />
        <span className={estilos.provisorio}>
          <EtiquetaProvisoria />
        </span>
        <span className={estilos.espacio} />
        <div className={estilos.usuario}>
          <span className={estilos.avatar} aria-hidden="true">
            {iniciales(usuario.nombre)}
          </span>
          <span className={estilos.datos}>
            <span>{usuario.nombre}</span>
            <span className={estilos.rol}>{NOMBRE_DEL_ROL[usuario.rol]}</span>
          </span>
          <Boton variante="fantasma" aria-label="Salir" onClick={() => void salir()}>
            <LogOut aria-hidden="true" />
          </Boton>
        </div>
      </header>
      <div className={estilos.cuerpo}>
        <nav className={estilos.nav} aria-label="Secciones">
          <ul>
            {secciones.map((s) => (
              <li key={s.ruta}>{enlace(s)}</li>
            ))}
          </ul>
        </nav>
        <main className={estilos.contenido}>
          {aviso && <Aviso mensaje={aviso} alCerrar={descartarAviso} />}
          <Outlet />
        </main>
      </div>
      <nav className={estilos.navInferior} aria-label="Secciones (celular)">
        {secciones.map((s) => (
          <span key={s.ruta}>{enlace(s)}</span>
        ))}
      </nav>
      <AvisoDeInactividad />
    </div>
  )
}
