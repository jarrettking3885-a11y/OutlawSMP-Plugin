package com.outlawsmp.player;

import java.util.UUID;

/**
 * A single Wish.
 *
 * A Wish permanently owns exactly one {@link Blessing}. Wishes cannot be
 * bought or destroyed - they only ever change owners (via PvP kills). The
 * blessing a Wish owns travels with it whenever it changes hands.
 */
public class Wish {

    private final UUID id;
    private final Blessing blessing;
    private UUID ownerId;
    private final long createdAt;

    public Wish(UUID id, Blessing blessing, UUID ownerId, long createdAt) {
        this.id = id;
        this.blessing = blessing;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    /** Convenience constructor for freshly rolled Wishes. */
    public static Wish create(Blessing blessing, UUID ownerId) {
        return new Wish(UUID.randomUUID(), blessing, ownerId, System.currentTimeMillis());
    }

    public UUID getId() {
        return id;
    }

    public Blessing getBlessing() {
        return blessing;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
