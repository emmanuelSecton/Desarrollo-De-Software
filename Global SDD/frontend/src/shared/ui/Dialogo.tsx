import { X } from 'lucide-react'
import { useEffect, useId, useRef, type ReactNode } from 'react'
import estilos from './Dialogo.module.css'

interface PropsDeDialogo {
  abierto: boolean
  titulo: string
  children: ReactNode
  acciones: ReactNode
  /** Escape. */
  alCancelar: () => void
  /** alertdialog para avisos que piden una decision (p. ej. sesion por vencer). */
  rol?: 'dialog' | 'alertdialog'
  /** Formularios: mas ancho y en celular como hoja inferior. */
  ancho?: boolean
  /** false en formularios: el contenido no se lee como descripcion del dialogo. */
  describir?: boolean
  /** Muestra la X de cerrar, que llama a alCancelar. */
  conCerrar?: boolean
}

/**
 * Dialogo modal sobre el <dialog> nativo: showModal() atrapa el foco y maneja Escape.
 * El foco inicial va al elemento marcado con data-foco-inicial.
 */
export function Dialogo({
  abierto,
  titulo,
  children,
  acciones,
  alCancelar,
  rol = 'dialog',
  ancho = false,
  describir = true,
  conCerrar = false,
}: PropsDeDialogo) {
  const ref = useRef<HTMLDialogElement>(null)
  const id = useId()

  useEffect(() => {
    const dialogo = ref.current
    if (!dialogo) return
    if (abierto && !dialogo.open) {
      // Al cerrarse (o desmontarse) el foco vuelve a quien lo abrio.
      const anterior = document.activeElement as HTMLElement | null
      dialogo.showModal()
      dialogo.querySelector<HTMLElement>('[data-foco-inicial]')?.focus()
      return () => anterior?.focus()
    }
    if (!abierto && dialogo.open) dialogo.close()
  }, [abierto])

  return (
    <dialog
      ref={ref}
      className={ancho ? `${estilos.dialogo} ${estilos.ancho}` : estilos.dialogo}
      role={rol}
      aria-modal="true"
      aria-labelledby={`${id}-titulo`}
      aria-describedby={describir ? `${id}-texto` : undefined}
      onCancel={(evento) => {
        evento.preventDefault()
        alCancelar()
      }}
    >
      {abierto && (
        <>
          <div className={estilos.cabecera}>
            <h2 id={`${id}-titulo`}>{titulo}</h2>
            {conCerrar && (
              <button
                type="button"
                className={estilos.cerrar}
                aria-label="Cerrar"
                onClick={alCancelar}
              >
                <X aria-hidden="true" />
              </button>
            )}
          </div>
          <div id={`${id}-texto`} className={estilos.cuerpo}>
            {children}
          </div>
          <div className={estilos.acciones}>{acciones}</div>
        </>
      )}
    </dialog>
  )
}
