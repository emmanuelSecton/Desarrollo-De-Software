package ar.edu.utn.tp2.arca;

/**
 * Tipos de comprobante de ARCA (tabla de tipos de comprobante de WSFE) que puede emitir la app.
 * El código es el que se envía en CbteTipo y el que se imprime como "COD. 0XX" debajo de la letra.
 */
public enum TipoComprobanteArca {
    FACTURA_A(1, "A"),
    FACTURA_B(6, "B"),
    FACTURA_C(11, "C"),
    FACTURA_M(51, "M");

    private final int codigo;
    private final String letra;

    TipoComprobanteArca(int codigo, String letra) {
        this.codigo = codigo;
        this.letra = letra;
    }

    public int getCodigo() {
        return codigo;
    }

    public String getLetra() {
        return letra;
    }

    /** Código con 3 dígitos, como se imprime en el comprobante (ej. 011). */
    public String getCodigoImpreso() {
        return String.format("%03d", codigo);
    }

    public static TipoComprobanteArca desdeCodigo(int codigo) {
        for (TipoComprobanteArca t : values()) {
            if (t.codigo == codigo) return t;
        }
        throw new IllegalArgumentException("Tipo de comprobante ARCA no soportado: " + codigo);
    }
}
