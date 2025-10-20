package de.bund.zrb.service;

import de.bund.zrb.model.Precondition;

import java.util.UUID;

/** Create new preconditions with UUID and optional name. */
public final class PreconditionFactory {

    private PreconditionFactory() {}

    public static Precondition newPrecondition(String name) {
        Precondition p = new Precondition();
        p.setId(UUID.randomUUID().toString());
        p.setName((name != null && name.trim().length() > 0) ? name.trim() : "Neue Vorbedingung");
        return p;
    }
}
