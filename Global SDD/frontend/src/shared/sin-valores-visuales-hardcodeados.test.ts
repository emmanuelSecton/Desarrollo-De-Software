import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

// Regla dura del proyecto: nada de colores hardcodeados; todo sale de design-system/tokens.css.
const SRC = join(process.cwd(), 'src')
const COLOR = /#[0-9a-fA-F]{3,8}\b|\brgba?\(|\bhsla?\(/
const EXTENSIONES = /\.(css|ts|tsx)$/

function archivos(dir: string): string[] {
  return readdirSync(dir).flatMap((nombre) => {
    const ruta = join(dir, nombre)
    return statSync(ruta).isDirectory() ? archivos(ruta) : [ruta]
  })
}

describe('src/ sin valores visuales hardcodeados', () => {
  it('ningun archivo de codigo contiene colores literales', () => {
    const culpables = archivos(SRC)
      .filter(
        (ruta) =>
          EXTENSIONES.test(ruta) && !ruta.endsWith('sin-valores-visuales-hardcodeados.test.ts'),
      )
      .filter((ruta) => COLOR.test(readFileSync(ruta, 'utf8')))

    expect(culpables).toEqual([])
  })

  it('el punto de entrada importa los tokens del design system', () => {
    const main = readFileSync(join(SRC, 'main.tsx'), 'utf8')

    expect(main).toContain("import '@design-system/tokens.css'")
  })

  it('el detector reconoce colores literales (control del propio test)', () => {
    expect(COLOR.test('color: #0f766e;')).toBe(true)
    expect(COLOR.test('background: rgb(1, 2, 3)')).toBe(true)
    expect(COLOR.test('color: var(--color-text)')).toBe(false)
  })
})
