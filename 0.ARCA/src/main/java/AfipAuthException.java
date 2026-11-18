/**
 * Error al autenticarse contra WSAA.
 *
 * El código es el que informa AFIP en el faultcode (sin prefijo de namespace),
 * por ejemplo "coe.alreadyAuthenticated" o "cms.cert.expired", o uno local:
 *   - "local.certificado" / "local.clave": no se pudo leer el archivo.
 *   - "red": no se pudo conectar o la conexión se cortó.
 *   - "respuesta.invalida": AFIP respondió algo que no se puede interpretar.
 *   - "http.NNN": error HTTP sin SOAP Fault.
 */
public class AfipAuthException extends Exception {

    private final String codigo;

    public AfipAuthException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public AfipAuthException(String codigo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
