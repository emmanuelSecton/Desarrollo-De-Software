import { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router'
import {
  AVISO_MS,
  MARGEN_MS,
  TemporizadorDeInactividad,
  formatearCuentaRegresiva,
  relojDelNavegador,
  type Reloj,
} from '../shared/auth/inactividad'
import { useSesion } from '../shared/auth/sesion'
import { Boton } from '../shared/ui/Boton'
import { Dialogo } from '../shared/ui/Dialogo'
import estilos from './AvisoDeInactividad.module.css'

const EVENTOS_DE_ACTIVIDAD = ['keydown', 'pointerdown', 'wheel', 'touchstart'] as const

interface Props {
  avisoMs?: number
  margenMs?: number
  reloj?: Reloj
}

/**
 * A los 28 minutos sin actividad avisa que la sesion esta por vencer (D5). "Seguir conectado"
 * llama a /auth/me, que renueva la sesion del servidor; si la cuenta llega a cero, cierra la sesion.
 */
export function AvisoDeInactividad({
  avisoMs = AVISO_MS,
  margenMs = MARGEN_MS,
  reloj = relojDelNavegador,
}: Props) {
  const { renovar, salir, expirar } = useSesion()
  const ubicacion = useLocation()
  const [venceA, setVenceA] = useState<number | null>(null)
  const [ahora, setAhora] = useState(0)

  const temporizador = useMemo(
    () =>
      new TemporizadorDeInactividad(
        avisoMs,
        () => {
          const momento = reloj.ahora()
          setAhora(momento)
          setVenceA(momento + margenMs)
        },
        reloj,
      ),
    [avisoMs, margenMs, reloj],
  )

  // Cuenta la inactividad mientras el aviso no esta abierto; navegar tambien es actividad.
  useEffect(() => {
    if (venceA !== null) return
    temporizador.reiniciar()
    const alActuar = () => temporizador.reiniciar()
    EVENTOS_DE_ACTIVIDAD.forEach((tipo) =>
      window.addEventListener(tipo, alActuar, { passive: true }),
    )
    return () => {
      EVENTOS_DE_ACTIVIDAD.forEach((tipo) => window.removeEventListener(tipo, alActuar))
      temporizador.detener()
    }
  }, [temporizador, venceA, ubicacion.key])

  // Cuenta regresiva del aviso.
  useEffect(() => {
    if (venceA === null) return
    let id: unknown
    const tic = () => {
      const momento = reloj.ahora()
      setAhora(momento)
      if (momento >= venceA) {
        void expirar()
      } else {
        id = reloj.programar(tic, 1000)
      }
    }
    id = reloj.programar(tic, 1000)
    return () => reloj.cancelar(id)
  }, [venceA, reloj, expirar])

  async function seguirConectado() {
    try {
      await renovar()
      setVenceA(null)
    } catch {
      // Si la sesion ya vencio, el 401 de /auth/me lleva al login con el aviso.
    }
  }

  return (
    <Dialogo
      abierto={venceA !== null}
      rol="alertdialog"
      titulo="Tu sesión está por vencer"
      alCancelar={() => void seguirConectado()}
      acciones={
        <>
          <Boton variante="secundario" onClick={() => void salir()}>
            Salir
          </Boton>
          <Boton data-foco-inicial onClick={() => void seguirConectado()}>
            Seguir conectado
          </Boton>
        </>
      }
    >
      <p>
        Por seguridad, la sesión se cierra tras 30 minutos sin actividad. Se va a cerrar en{' '}
        <span className={estilos.cuentaRegresiva}>
          {formatearCuentaRegresiva((venceA ?? 0) - ahora)}
        </span>
        .
      </p>
    </Dialogo>
  )
}
