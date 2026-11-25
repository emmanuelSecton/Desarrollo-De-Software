package ar.edu.utn.odonto.auditoria.application;

/** Tipos de actor aceptados por el CHECK condicional de la tabla (D1). */
public enum TipoActor {
    SISTEMA("SISTEMA"),
    USUARIO("USUARIO"),
    PACIENTE("PACIENTE");

    private final String valor;

    TipoActor(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
