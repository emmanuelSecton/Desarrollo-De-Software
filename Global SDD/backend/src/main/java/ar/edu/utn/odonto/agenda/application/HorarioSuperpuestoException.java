package ar.edu.utn.odonto.agenda.application;

/** La franja se superpone con otra del mismo dia; lo detecta la exclusion constraint (D4). Se traduce a 409. */
public class HorarioSuperpuestoException extends RuntimeException {

    public HorarioSuperpuestoException() {
        super("La franja se superpone con otra del mismo día");
    }
}
