// Temporizador de inactividad (C-07, D5). Recibe el reloj por parametro para testearlo sin esperas.
// Es solo un aviso: la fuente de verdad es el 401 del servidor.

export interface Reloj {
  programar: (accion: () => void, ms: number) => unknown
  cancelar: (id: unknown) => void
  ahora: () => number
}

export const relojDelNavegador: Reloj = {
  programar: (accion, ms) => window.setTimeout(accion, ms),
  cancelar: (id) => window.clearTimeout(id as number),
  ahora: () => Date.now(),
}

const MINUTO = 60_000

/** La sesion del servidor vence a los 30 minutos sin actividad (RN-SEG-04, DD-08). */
export const SESION_MS = 30 * MINUTO

function avisoConfigurado(): number {
  // Solo para la verificacion visual: permite acortar el aviso al compilar (VITE_AVISO_INACTIVIDAD_SEG).
  const segundos = Number(import.meta.env.VITE_AVISO_INACTIVIDAD_SEG)
  return Number.isFinite(segundos) && segundos > 0 ? segundos * 1000 : 28 * MINUTO
}

/** El aviso aparece a los 28 minutos: deja 2 minutos para seguir conectado. */
export const AVISO_MS = avisoConfigurado()
export const MARGEN_MS = 2 * MINUTO

export class TemporizadorDeInactividad {
  private id: unknown = null
  private readonly limiteMs: number
  private readonly alCumplirse: () => void
  private readonly reloj: Reloj

  constructor(limiteMs: number, alCumplirse: () => void, reloj: Reloj = relojDelNavegador) {
    this.limiteMs = limiteMs
    this.alCumplirse = alCumplirse
    this.reloj = reloj
  }

  /** Cada actividad del usuario vuelve a contar desde cero. */
  reiniciar(): void {
    this.detener()
    this.id = this.reloj.programar(() => {
      this.id = null
      this.alCumplirse()
    }, this.limiteMs)
  }

  detener(): void {
    if (this.id !== null) this.reloj.cancelar(this.id)
    this.id = null
  }
}

/** "1:58" a partir de milisegundos restantes. */
export function formatearCuentaRegresiva(ms: number): string {
  const total = Math.max(0, Math.ceil(ms / 1000))
  const minutos = Math.floor(total / 60)
  const segundos = String(total % 60).padStart(2, '0')
  return `${minutos}:${segundos}`
}
