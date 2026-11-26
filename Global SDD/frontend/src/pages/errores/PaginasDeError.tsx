import { Compass, Lock } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { clasesDeBoton } from '../../shared/ui/Boton'
import { IconoGrande } from '../../shared/ui/EstadoVacio'
import estilos from './PaginasDeError.module.css'

interface PropsDePaginaDeError {
  codigo: number
  icono: ReactNode
  titulo: string
  texto: string
}

function PaginaDeError({ codigo, icono, titulo, texto }: PropsDePaginaDeError) {
  return (
    <main className={estilos.pagina}>
      <div className={estilos.caja}>
        <IconoGrande>{icono}</IconoGrande>
        <span className={estilos.codigo}>ERROR {codigo}</span>
        <h1>{titulo}</h1>
        <p className={estilos.texto}>{texto}</p>
        <Link className={clasesDeBoton('primario')} to="/agenda">
          Ir a la agenda
        </Link>
      </div>
    </main>
  )
}

export function PaginaAccesoDenegado() {
  return (
    <PaginaDeError
      codigo={403}
      icono={<Lock aria-hidden="true" />}
      titulo="No tenés acceso a esta sección"
      texto="Si creés que deberías verla, pedile acceso al odontólogo del consultorio."
    />
  )
}

export function PaginaNoEncontrada() {
  return (
    <PaginaDeError
      codigo={404}
      icono={<Compass aria-hidden="true" />}
      titulo="No encontramos esa página"
      texto="Puede que el enlace esté mal escrito o que la página ya no exista."
    />
  )
}
