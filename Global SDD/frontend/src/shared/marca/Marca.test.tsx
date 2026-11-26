import { render, screen } from '@testing-library/react'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { Marca, NOMBRE_PRODUCTO } from './Marca'

describe('Marca', () => {
  it('el nombre coincide con BRAND_NAME de design-system/brand.js (un solo lugar para renombrar)', () => {
    const brand = readFileSync(join(process.cwd(), '..', 'design-system', 'brand.js'), 'utf8')
    const nombre = /var BRAND_NAME = "([^"]+)"/.exec(brand)?.[1]

    expect(NOMBRE_PRODUCTO).toBe(nombre)
  })

  it('muestra el logo decorativo y el nombre', () => {
    const { container } = render(<Marca />)

    expect(screen.getByText(NOMBRE_PRODUCTO)).toBeVisible()
    expect(container.querySelector('img')).toHaveAttribute('alt', '')
  })
})
