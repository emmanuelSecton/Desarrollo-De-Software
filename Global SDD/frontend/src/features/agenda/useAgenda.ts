import { useCallback, useEffect, useState } from 'react'
import type { Rango } from '../../shared/formatos/fechas'
import {
  listarFranjas,
  listarPrestaciones,
  listarTurnos,
  obtenerConsultorio,
  type Franja,
  type Prestacion,
  type Turno,
} from './api'

/** Lo que la agenda necesita del consultorio; se carga una vez. */
export interface ConfiguracionDeAgenda {
  zona: string
  limiteConfirmacionHoras: number
  franjas: Franja[]
  prestaciones: Prestacion[]
}

/** Configuracion del consultorio para la agenda (D6). */
export function useConfiguracion() {
  const [configuracion, setConfiguracion] = useState<ConfiguracionDeAgenda | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    let vigente = true
    Promise.all([obtenerConsultorio(), listarFranjas(), listarPrestaciones()])
      .then(([consultorio, franjas, prestaciones]) => {
        if (!vigente) return
        setConfiguracion({
          zona: consultorio.zonaHoraria,
          limiteConfirmacionHoras: consultorio.confirmacionLimiteHoras,
          franjas,
          prestaciones: prestaciones.filter((p) => p.activa),
        })
      })
      .catch(() => vigente && setError(true))
    return () => {
      vigente = false
    }
  }, [])

  return { configuracion, error }
}

/** Turnos de un rango; recarga al pedirlo y al volver a la pestaña (D6). */
export function useTurnos(rango: Rango | null) {
  const [turnos, setTurnos] = useState<Turno[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(false)
  const [version, setVersion] = useState(0)
  const desde = rango?.desde
  const hasta = rango?.hasta

  useEffect(() => {
    if (!desde || !hasta) return
    let vigente = true
    listarTurnos({ desde, hasta })
      .then((lista) => {
        if (!vigente) return
        setTurnos(lista)
        setError(false)
      })
      .catch(() => vigente && setError(true))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [desde, hasta, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])

  useEffect(() => {
    const alVolver = () => {
      if (document.visibilityState === 'visible') recargar()
    }
    document.addEventListener('visibilitychange', alVolver)
    return () => document.removeEventListener('visibilitychange', alVolver)
  }, [recargar])

  return { turnos, cargando, error, recargar }
}
