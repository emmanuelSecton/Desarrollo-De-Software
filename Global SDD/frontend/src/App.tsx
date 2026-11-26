import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { EstructuraApp } from './app/EstructuraApp'
import { CambiarContrasenaPage } from './pages/acceso/CambiarContrasenaPage'
import { LoginPage } from './pages/acceso/LoginPage'
import { PaginaAccesoDenegado, PaginaNoEncontrada } from './pages/errores/PaginasDeError'
import { AgendaPage } from './features/agenda/AgendaPage'
import { ConfiguracionPage, PacientesPage } from './pages/secciones'
import {
  RequiereCambioPendiente,
  RequiereRol,
  RequiereSesion,
  RUTA_INICIAL,
  SoloSinSesion,
} from './shared/auth/guards'
import { ProveedorDeSesion } from './shared/auth/sesion'

/** Rutas de la SPA (C-07, D2). Spring Boot reenvia estas rutas a index.html. */
export function Rutas() {
  return (
    <Routes>
      <Route element={<SoloSinSesion />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>
      <Route element={<RequiereCambioPendiente />}>
        <Route path="/cambiar-contrasena" element={<CambiarContrasenaPage />} />
      </Route>
      <Route element={<RequiereSesion />}>
        <Route element={<RequiereRol rol="ODONTOLOGO" denegado={<PaginaAccesoDenegado />} />}>
          <Route element={<EstructuraApp />}>
            <Route path="/configuracion" element={<ConfiguracionPage />} />
          </Route>
        </Route>
        <Route element={<EstructuraApp />}>
          <Route path="/" element={<Navigate to={RUTA_INICIAL} replace />} />
          <Route path="/agenda" element={<AgendaPage />} />
          <Route path="/pacientes" element={<PacientesPage />} />
        </Route>
      </Route>
      <Route path="*" element={<PaginaNoEncontrada />} />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <ProveedorDeSesion>
        <Rutas />
      </ProveedorDeSesion>
    </BrowserRouter>
  )
}
