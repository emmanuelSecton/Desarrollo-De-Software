package ar.edu.utn.odonto.identidad.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Token de recuperacion de contraseña: solo su hash, vence a la hora y sirve una vez (C-04, D3). */
public class TokenDeRecuperacion {

    public static final Duration VIGENCIA = Duration.ofHours(1);

    private final UUID id;
    private final UUID usuarioId;
    private final String hash;
    private final Instant creadoAt;
    private final Instant venceAt;
    private Instant usadoAt;

    public TokenDeRecuperacion(UUID id, UUID usuarioId, String hash, Instant creadoAt, Instant venceAt, Instant usadoAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.hash = Objects.requireNonNull(hash, "hash");
        this.creadoAt = Objects.requireNonNull(creadoAt, "creadoAt");
        this.venceAt = Objects.requireNonNull(venceAt, "venceAt");
        this.usadoAt = usadoAt;
    }

    public static TokenDeRecuperacion nuevo(UUID usuarioId, String hash, Instant ahora) {
        return new TokenDeRecuperacion(UUID.randomUUID(), usuarioId, hash, ahora, ahora.plus(VIGENCIA), null);
    }

    /** Vigente si no se uso ni invalido y todavia no llego al vencimiento. */
    public boolean vigente(Instant ahora) {
        return usadoAt == null && ahora.isBefore(venceAt);
    }

    public void usar(Instant ahora) {
        this.usadoAt = ahora;
    }

    public UUID id() {
        return id;
    }

    public UUID usuarioId() {
        return usuarioId;
    }

    public String hash() {
        return hash;
    }

    public Instant creadoAt() {
        return creadoAt;
    }

    public Instant venceAt() {
        return venceAt;
    }

    public Instant usadoAt() {
        return usadoAt;
    }
}
