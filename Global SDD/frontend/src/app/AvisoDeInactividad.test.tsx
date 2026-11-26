import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  TemporizadorDeInactividad,
  formatearCuentaRegresiva,
  type Reloj,
} from '../shared/auth/inactividad'
import { ProveedorDeSesion } from '../shared/auth/sesion'
import { instalarApiFalsa, json, odontologo, sinContenido } from '../test/apiFalsa'
import { AvisoDeInactividad } from './AvisoDeInactividad'

const MINUTO = 60_000

/** Reloj manual: el tiempo solo avanza cuando el test lo pide. */
class RelojFalso implements Reloj {
  private momento = 0
  private siguienteId = 1
  private pendientes = new Map<number, { cuando: number; accion: () => void }>()

  ahora = () => this.momento
  programar = (accion: () => void, ms: number) => {
    const id = this.siguienteId++
    this.pendientes.set(id, { cuando: this.momento + ms, accion })
    return id
  }
  cancelar = (id: unknown) => {
    this.pendientes.delete(id as number)
  }

  avanzar(ms: number) {
    const fin = this.momento + ms
    for (;;) {
      const proximo = [...this.pendientes.entries()]
        .filter(([, p]) => p.cuando <= fin)
        .sort((a, b) => a[1].cuando - b[1].cuando)[0]
      if (!proximo) break
      const [id, { cuando, accion }] = proximo
      this.pendientes.delete(id)
      this.momento = cuando
      accion()
    }
    this.momento = fin
  }
}

afterEach(() => vi.unstubAllGlobals())

describe('TemporizadorDeInactividad', () => {
  it('se cumple al limite y cada actividad vuelve a contar desde cero', () => {
    const reloj = new RelojFalso()
    const alCumplirse = vi.fn()
    const temporizador = new TemporizadorDeInactividad(28 * MINUTO, alCumplirse, reloj)

    temporizador.reiniciar()
    reloj.avanzar(20 * MINUTO)
    temporizador.reiniciar()
    reloj.avanzar(27 * MINUTO)
    expect(alCumplirse).not.toHaveBeenCalled()

    reloj.avanzar(MINUTO)
    expect(alCumplirse).toHaveBeenCalledTimes(1)
  })

  it('formatea la cuenta regresiva', () => {
    expect(formatearCuentaRegresiva(118_000)).toBe('1:58')
    expect(formatearCuentaRegresiva(120_000)).toBe('2:00')
    expect(formatearCuentaRegresiva(-5)).toBe('0:00')
  })
})

describe('AvisoDeInactividad', () => {
  async function montar(rutas = {}) {
    const api = instalarApiFalsa({ 'GET /auth/me': () => json(200, odontologo), ...rutas })
    const reloj = new RelojFalso()
    render(
      <MemoryRouter>
        <ProveedorDeSesion>
          <AvisoDeInactividad reloj={reloj} />
        </ProveedorDeSesion>
      </MemoryRouter>,
    )
    // Espera a que la sesion cargue.
    await act(async () => {})
    return { api, reloj }
  }

  const llamadas = (api: ReturnType<typeof instalarApiFalsa>, ruta: string) =>
    api.mock.calls.filter(([url]) => String(url).endsWith(ruta)).length

  it('a los 28 minutos sin actividad avisa, con foco en "Seguir conectado"', async () => {
    const { reloj } = await montar()

    act(() => reloj.avanzar(28 * MINUTO - 1))
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()

    act(() => reloj.avanzar(1))
    const dialogo = screen.getByRole('alertdialog', { name: 'Tu sesión está por vencer' })
    expect(dialogo).toHaveTextContent('Se va a cerrar en 2:00')
    expect(screen.getByRole('button', { name: 'Seguir conectado' })).toHaveFocus()

    act(() => reloj.avanzar(2_000))
    expect(dialogo).toHaveTextContent('Se va a cerrar en 1:58')
  })

  it('la actividad del usuario posterga el aviso', async () => {
    const { reloj } = await montar()

    act(() => reloj.avanzar(20 * MINUTO))
    fireEvent.keyDown(window, { key: 'a' })
    act(() => reloj.avanzar(20 * MINUTO))

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('"Seguir conectado" renueva la sesion con /auth/me y cierra el aviso', async () => {
    const { api, reloj } = await montar()
    act(() => reloj.avanzar(28 * MINUTO))
    const antes = llamadas(api, '/auth/me')

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Seguir conectado' }))
    })

    expect(llamadas(api, '/auth/me')).toBe(antes + 1)
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('"Salir" cierra la sesion', async () => {
    const { api, reloj } = await montar({ 'POST /auth/logout': sinContenido })
    act(() => reloj.avanzar(28 * MINUTO))

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Salir' }))
    })

    expect(llamadas(api, '/auth/logout')).toBe(1)
  })

  it('si la cuenta regresiva llega a cero, cierra la sesion', async () => {
    const { api, reloj } = await montar({ 'POST /auth/logout': sinContenido })
    act(() => reloj.avanzar(28 * MINUTO))

    await act(async () => reloj.avanzar(2 * MINUTO))

    expect(llamadas(api, '/auth/logout')).toBe(1)
  })
})
