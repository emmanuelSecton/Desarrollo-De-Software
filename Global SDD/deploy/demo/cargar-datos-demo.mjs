#!/usr/bin/env node
// Carga pacientes y turnos FICTICIOS (SU-07) para la demo en pantalla, usando la API real.
// Se puede correr varias veces: no duplica pacientes (DNI) ni turnos (paciente + inicio).
//
// Uso (Node 24):
//   DEMO_EMAIL=... DEMO_PASSWORD=... node deploy/demo/cargar-datos-demo.mjs [--url URL] [--desde AAAA-MM-DD] [--si]
//
// La contrasena se lee solo de la variable de entorno y nunca se imprime.

import { MOTIVO_CANCELACION, PACIENTES, planDeTurnos, sumarDias } from './plan.mjs'

function argumento(nombre) {
  const i = process.argv.indexOf(nombre)
  return i >= 0 ? process.argv[i + 1] : undefined
}

const URL_BASE = (argumento('--url') ?? process.env.DEMO_URL ?? 'http://localhost:8080').replace(/\/$/, '')
const CONFIRMADO = process.argv.includes('--si')
const DESDE = argumento('--desde')

function salir(mensaje) {
  console.error(`✗ ${mensaje}`)
  process.exit(1)
}

/** Cliente minimo con cookie de sesion y token CSRF, como el navegador. */
class Cliente {
  cookies = new Map()

  guardarCookies(respuesta) {
    for (const linea of respuesta.headers.getSetCookie()) {
      const [par] = linea.split(';')
      const i = par.indexOf('=')
      this.cookies.set(par.slice(0, i).trim(), par.slice(i + 1).trim())
    }
  }

  async pedir(metodo, ruta, cuerpo) {
    const cabeceras = { Accept: 'application/json' }
    if (this.cookies.size > 0) {
      cabeceras.Cookie = [...this.cookies].map(([k, v]) => `${k}=${v}`).join('; ')
    }
    if (metodo !== 'GET') {
      if (!this.cookies.has('XSRF-TOKEN')) await this.pedir('GET', '/auth/csrf')
      cabeceras['X-XSRF-TOKEN'] = decodeURIComponent(this.cookies.get('XSRF-TOKEN') ?? '')
      cabeceras.Cookie = [...this.cookies].map(([k, v]) => `${k}=${v}`).join('; ')
    }
    if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json'
    const respuesta = await fetch(`${URL_BASE}/api/v1${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      redirect: 'manual',
    })
    this.guardarCookies(respuesta)
    const texto = await respuesta.text()
    let datos = null
    try {
      datos = texto ? JSON.parse(texto) : null
    } catch {
      datos = texto
    }
    return { status: respuesta.status, datos }
  }
}

const esLocal = (url) => ['localhost', '127.0.0.1', '[::1]'].includes(new URL(url).hostname)

async function principal() {
  const email = process.env.DEMO_EMAIL
  const password = process.env.DEMO_PASSWORD
  if (!email || !password) {
    salir('Faltan DEMO_EMAIL y DEMO_PASSWORD (las credenciales del personal de esa instancia).')
  }
  if (DESDE && !/^\d{4}-\d{2}-\d{2}$/.test(DESDE)) salir('--desde tiene que ser una fecha AAAA-MM-DD.')
  if (!esLocal(URL_BASE) && !CONFIRMADO) {
    salir(`${URL_BASE} no es local. Si es la instancia de demo, volvé a correrlo con --si.`)
  }

  const cliente = new Cliente()
  console.log(`→ Instancia: ${URL_BASE}`)

  const login = await cliente.pedir('POST', '/auth/login', { email, password })
  if (login.status !== 200) salir(`No se pudo iniciar sesión (HTTP ${login.status}).`)
  if (login.datos.debeCambiarPassword) {
    salir('La cuenta tiene que cambiar la contraseña: entrá una vez desde la app y volvé a correr el script.')
  }

  const [consultorio, franjas, prestaciones] = await Promise.all([
    cliente.pedir('GET', '/consultorio'),
    cliente.pedir('GET', '/horarios'),
    cliente.pedir('GET', '/prestaciones'),
  ])
  if ([consultorio, franjas, prestaciones].some((r) => r.status !== 200)) {
    salir('No se pudo leer la configuración del consultorio.')
  }
  const zona = consultorio.datos.zonaHoraria
  if (!zona) salir('La API no devuelve zonaHoraria: la instancia necesita C-13 o posterior.')

  // 1. Pacientes: alta o, si el DNI ya existe, se reutiliza.
  const pacientes = []
  let pacientesNuevos = 0
  for (const p of PACIENTES) {
    const alta = await cliente.pedir('POST', '/pacientes', {
      ...p,
      email: null,
      aceptaWhatsapp: true,
      consentimientoDatos: true,
    })
    if (alta.status === 201) {
      pacientesNuevos++
      pacientes.push(alta.datos.id)
      continue
    }
    const existente = await cliente.pedir('GET', `/pacientes?q=${p.dni}&incluirInactivos=true`)
    const encontrado = existente.datos?.contenido?.find((x) => x.dni === p.dni)
    if (!encontrado) salir(`No se pudo dar de alta ni encontrar a ${p.nombre} ${p.apellido} (HTTP ${alta.status}).`)
    pacientes.push(encontrado.id)
  }

  // 2. Turnos: solo los que todavia no existen (mismo paciente y mismo inicio).
  const { plan, omitidos } = planDeTurnos({
    zona,
    ahoraMs: Date.now(),
    franjas: franjas.datos,
    prestaciones: prestaciones.datos,
    desde: DESDE,
  })
  let existentes = []
  if (plan.length > 0) {
    // Rango holgado en UTC: del dia del primer turno al dia siguiente del ultimo.
    const desde = encodeURIComponent(`${plan[0].fecha}T00:00:00Z`)
    const hasta = encodeURIComponent(`${sumarDias(plan.at(-1).fecha, 1)}T12:00:00Z`)
    const lista = await cliente.pedir('GET', `/turnos?desde=${desde}&hasta=${hasta}`)
    if (lista.status !== 200) salir(`No se pudieron leer los turnos existentes (HTTP ${lista.status}).`)
    existentes = lista.datos
  }
  const yaEsta = (pacienteId, inicio) =>
    existentes.some((t) => t.pacienteId === pacienteId && Date.parse(t.inicio) === Date.parse(inicio))

  const resumen = { creados: 0, yaEstaban: 0, ocupados: [], confirmados: 0, cancelados: 0, rechazados: [] }
  for (const turno of plan) {
    const pacienteId = pacientes[turno.paciente]
    if (yaEsta(pacienteId, turno.inicio)) {
      resumen.yaEstaban++
      continue
    }
    const alta = await cliente.pedir('POST', '/turnos', {
      pacienteId,
      prestacionId: turno.prestacionId,
      inicio: turno.inicio,
      sobreturno: turno.sobreturno,
    })
    if (alta.status === 409 && alta.datos?.type === '/problemas/turno-superpuesto') {
      // Ese horario ya lo tiene otro turno (p. ej. de una carga anterior en otra semana): se deja como esta.
      resumen.ocupados.push(`${turno.fecha} ${turno.hora}`)
      continue
    }
    if (alta.status !== 201) {
      resumen.rechazados.push(`${turno.fecha} ${turno.hora} (HTTP ${alta.status}${alta.datos?.title ? `: ${alta.datos.title}` : ''})`)
      continue
    }
    resumen.creados++
    const { id, version } = alta.datos
    if (turno.accion === 'confirmar') {
      const r = await cliente.pedir('POST', `/turnos/${id}/confirmar`, { version })
      if (r.status === 200) resumen.confirmados++
    } else if (turno.accion === 'cancelar') {
      const r = await cliente.pedir('POST', `/turnos/${id}/cancelar`, {
        version,
        canceladoPor: 'PACIENTE',
        motivo: MOTIVO_CANCELACION,
      })
      if (r.status === 200) resumen.cancelados++
    }
  }

  await cliente.pedir('POST', '/auth/logout')

  console.log(`✓ Pacientes: ${pacientesNuevos} nuevos, ${PACIENTES.length - pacientesNuevos} ya estaban.`)
  console.log(
    `✓ Turnos: ${resumen.creados} nuevos (${resumen.confirmados} confirmados, ${resumen.cancelados} cancelado), ${resumen.yaEstaban} ya estaban.`,
  )
  if (resumen.ocupados.length > 0) {
    console.log(`ℹ Horario ocupado por otro turno, no se cargaron: ${resumen.ocupados.join('; ')}.`)
  }
  if (omitidos.length > 0) {
    console.log(`ℹ Omitidos: ${omitidos.map((t) => `${t.hora} día ${t.dia} (${t.motivo})`).join('; ')}.`)
  }
  if (resumen.rechazados.length > 0) {
    console.log(`⚠ La API rechazó: ${resumen.rechazados.join('; ')}.`)
    process.exitCode = 2
  }
}

principal().catch((error) => salir(`Error inesperado: ${error.message}`))
