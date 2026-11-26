import { Alerta } from './Alerta'

/** Confirmacion de una accion (p. ej. "Contraseña actualizada") que el usuario puede cerrar. */
export function Aviso({ mensaje, alCerrar }: { mensaje: string; alCerrar: () => void }) {
  return (
    <Alerta tipo="exito" alCerrar={alCerrar}>
      {mensaje}
    </Alerta>
  )
}
