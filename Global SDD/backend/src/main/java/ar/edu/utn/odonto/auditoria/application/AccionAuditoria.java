package ar.edu.utn.odonto.auditoria.application;

/** Catálogo extensible: agregar un valor no requiere migración de base (D1, D3). */
public enum AccionAuditoria {
    LOGIN("LOGIN"),
    LOGIN_FALLIDO("LOGIN_FALLIDO"),
    VER_HISTORIA("VER_HISTORIA"),
    CREAR_TURNO("CREAR_TURNO"),
    EXPORTAR("EXPORTAR"),
    CREAR_ENTIDAD("CREAR_ENTIDAD"),
    MODIFICAR_ENTIDAD("MODIFICAR_ENTIDAD"),
    ANULAR_ENTIDAD("ANULAR_ENTIDAD");

    private final String valor;

    AccionAuditoria(String valor) {
        this.valor = valor;
    }

    public String valor() {
        return valor;
    }
}
