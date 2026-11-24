import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

const designSystem = fileURLToPath(new URL('../design-system', import.meta.url))

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@design-system': designSystem },
  },
  server: {
    // Permite importar design-system/tokens.css, que vive fuera de frontend/.
    fs: { allow: ['..'] },
    // En desarrollo no hay CORS: Vite reenvia al backend (en produccion todo es mismo origen).
    proxy: {
      '/api': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
})
