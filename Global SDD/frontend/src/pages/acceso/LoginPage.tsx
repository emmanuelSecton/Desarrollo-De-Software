import { useRef, useState, type FormEvent } from 'react'
import { ErrorDeApi } from '../../shared/api-client/cliente'
import { useSesion } from '../../shared/auth/sesion'
import { Alerta } from '../../shared/ui/Alerta'
import { Boton } from '../../shared/ui/Boton'
import { Campo } from '../../shared/ui/Campo'
import { PantallaDeAcceso, claseDeFormulario } from './PantallaDeAcceso'

/** Mismo texto que C-03: no revela si el email existe ni si la cuenta esta bloqueada. */
export const MENSAJE_CREDENCIALES =
  'El email o la contraseña no son válidos, o la cuenta está bloqueada temporalmente.'
const MENSAJE_SIN_CONEXION =
  'No pudimos conectar con el consultorio. Probá de nuevo en unos segundos.'

interface Errores {
  email?: string
  password?: string
}

function mensajeDeRechazo(error: unknown): string {
  if (!(error instanceof ErrorDeApi)) return MENSAJE_SIN_CONEXION
  if (error.status === 429) return error.detalle || error.titulo
  if (error.status === 401) return MENSAJE_CREDENCIALES
  return MENSAJE_SIN_CONEXION
}

export function LoginPage() {
  const { ingresar, estado } = useSesion()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errores, setErrores] = useState<Errores>({})
  const [rechazo, setRechazo] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)
  const refEmail = useRef<HTMLInputElement>(null)
  const refPassword = useRef<HTMLInputElement>(null)

  const sesionVencida = estado.tipo === 'sin-sesion' && estado.motivo === 'vencida'

  async function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const faltantes: Errores = {}
    if (!email.trim()) faltantes.email = 'Ingresá tu email'
    if (!password) faltantes.password = 'Ingresá tu contraseña'
    setErrores(faltantes)
    setRechazo(null)
    if (faltantes.email || faltantes.password) {
      ;(faltantes.email ? refEmail : refPassword).current?.focus()
      return
    }

    setEnviando(true)
    try {
      // Con la sesion iniciada, el guard del login lleva a la ruta pedida.
      await ingresar(email.trim(), password)
    } catch (error) {
      setEnviando(false)
      if (error instanceof ErrorDeApi && error.status === 400 && error.errores.length > 0) {
        setErrores(Object.fromEntries(error.errores.map((e) => [e.campo, e.mensaje])))
        return
      }
      setRechazo(mensajeDeRechazo(error))
      setPassword('')
      refEmail.current?.focus()
    }
  }

  return (
    <PantallaDeAcceso
      titulo="Ingresá al consultorio"
      bajada="Usá el email y la contraseña que te dio el consultorio."
    >
      <form className={claseDeFormulario} noValidate onSubmit={enviar}>
        {rechazo ? (
          <Alerta tipo="error" id="alerta-login">
            {rechazo}
          </Alerta>
        ) : (
          sesionVencida && (
            <Alerta tipo="sesion" titulo="Tu sesión venció.">
              Volvé a ingresar para seguir donde estabas.
            </Alerta>
          )
        )}
        <Campo
          ref={refEmail}
          etiqueta="Email"
          name="email"
          type="email"
          autoComplete="username"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errores.email}
          invalido={rechazo !== null}
          descritoPor={rechazo ? 'alerta-login' : undefined}
        />
        <Campo
          ref={refPassword}
          etiqueta="Contraseña"
          name="password"
          type="password"
          autoComplete="current-password"
          revelable
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errores.password}
        />
        <Boton type="submit" bloque disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </Boton>
      </form>
    </PantallaDeAcceso>
  )
}
