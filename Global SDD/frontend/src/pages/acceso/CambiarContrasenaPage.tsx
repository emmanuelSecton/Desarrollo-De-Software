import { Check, Dot, LogOut } from 'lucide-react'
import { useRef, useState, type FormEvent, type ReactNode } from 'react'
import { ErrorDeApi, PROBLEMAS } from '../../shared/api-client/cliente'
import { useSesion } from '../../shared/auth/sesion'
import { Alerta } from '../../shared/ui/Alerta'
import { Boton } from '../../shared/ui/Boton'
import { Campo } from '../../shared/ui/Campo'
import estilos from './Acceso.module.css'
import { PantallaDeAcceso, claseDeFormulario } from './PantallaDeAcceso'

/** Politica de C-03 (NIST SP 800-63B): al menos 10 caracteres y distinta de la actual. */
export const MINIMO_CARACTERES = 10
const MENSAJE_LARGO = `La nueva contraseña debe tener al menos ${MINIMO_CARACTERES} caracteres`
const MENSAJE_IGUAL = 'La nueva contraseña debe ser distinta de la actual'

interface Errores {
  actual?: string
  nueva?: string
}

const largo = (texto: string) => [...texto].length

export function CambiarContrasenaPage() {
  const { cambiarContrasena, salir } = useSesion()
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [errores, setErrores] = useState<Errores>({})
  const [falla, setFalla] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refActual = useRef<HTMLInputElement>(null)
  const refNueva = useRef<HTMLInputElement>(null)

  const cumpleLargo = largo(nueva) >= MINIMO_CARACTERES
  const esDistinta = nueva.length > 0 && nueva !== actual

  function enfocar(nuevos: Errores) {
    ;(nuevos.actual ? refActual : refNueva).current?.focus()
  }

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const nuevos: Errores = {}
    if (!actual) nuevos.actual = 'Ingresá tu contraseña actual'
    if (!cumpleLargo) nuevos.nueva = MENSAJE_LARGO
    else if (!esDistinta) nuevos.nueva = MENSAJE_IGUAL
    setErrores(nuevos)
    setFalla(null)
    if (nuevos.actual || nuevos.nueva) {
      enfocar(nuevos)
      return
    }

    setEnviando(true)
    try {
      // Al completarse, el guard lleva a la agenda con el aviso "Contraseña actualizada".
      await cambiarContrasena(actual, nueva)
    } catch (error) {
      setEnviando(false)
      if (!(error instanceof ErrorDeApi)) {
        setFalla('No pudimos conectar con el consultorio. Probá de nuevo en unos segundos.')
        return
      }
      let deLaApi: Errores = {}
      if (error.errores.length > 0) {
        deLaApi = Object.fromEntries(error.errores.map((e) => [e.campo, e.mensaje]))
      } else if (error.tipo === PROBLEMAS.passwordInvalida) {
        // La API devuelve un unico detalle: va al campo del que habla.
        deLaApi = /actual no es correcta/i.test(error.detalle)
          ? { actual: error.detalle }
          : { nueva: error.detalle }
      } else {
        setFalla(error.detalle || error.titulo)
        return
      }
      setErrores(deLaApi)
      enfocar(deLaApi)
    }
  }

  return (
    <PantallaDeAcceso
      titulo="Elegí una contraseña nueva"
      bajada="Es tu primer ingreso: reemplazá la contraseña temporal por una propia."
      pie={
        <Boton variante="fantasma" onClick={() => void salir()}>
          <LogOut aria-hidden="true" />
          Salir
        </Boton>
      }
    >
      <form className={claseDeFormulario} noValidate onSubmit={enviar}>
        {falla && <Alerta tipo="error">{falla}</Alerta>}
        <Campo
          ref={refActual}
          etiqueta="Contraseña actual"
          name="actual"
          type="password"
          autoComplete="current-password"
          value={actual}
          onChange={(e) => setActual(e.target.value)}
          error={errores.actual}
        />
        <Campo
          ref={refNueva}
          etiqueta="Contraseña nueva"
          name="nueva"
          type="password"
          autoComplete="new-password"
          revelable
          value={nueva}
          onChange={(e) => setNueva(e.target.value)}
          error={errores.nueva}
          descritoPor="requisitos-password"
        >
          <ul className={estilos.requisitos} id="requisitos-password">
            <Requisito cumple={cumpleLargo}>Al menos {MINIMO_CARACTERES} caracteres</Requisito>
            <Requisito cumple={esDistinta}>Distinta de la contraseña actual</Requisito>
          </ul>
        </Campo>
        <Boton type="submit" bloque disabled={enviando}>
          {enviando ? 'Guardando…' : 'Guardar y continuar'}
        </Boton>
      </form>
    </PantallaDeAcceso>
  )
}

function Requisito({ cumple, children }: { cumple: boolean; children: ReactNode }) {
  return (
    <li className={cumple ? estilos.cumple : undefined}>
      {cumple ? <Check aria-hidden="true" /> : <Dot aria-hidden="true" />}
      <span className="sr-only">{cumple ? 'Cumple:' : 'Falta:'}</span>
      {children}
    </li>
  )
}
