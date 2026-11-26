package ar.edu.utn.odonto.agenda.application;

import ar.edu.utn.odonto.agenda.domain.Consultorio;

/** Puerto del consultorio unico de la instancia; el adaptador vive en infrastructure. */
public interface RepositorioConsultorio {

    /** El consultorio que crean las migraciones; siempre existe. */
    Consultorio obtener();

    /** Bloquea la fila hasta el fin de la transaccion. */
    Consultorio obtenerParaActualizar();

    void actualizar(Consultorio consultorio);
}
