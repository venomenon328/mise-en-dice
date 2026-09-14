package io.github.venomenon328.miseendice.catalog.api;

import java.util.List;

/** Requires an explicit, current-save acknowledgement for newly affected cross-concept names. */
public final class CatalogNameCollisionWarningException extends RuntimeException {

    private final List<CatalogCommands.NameCollision> collisions;

    public CatalogNameCollisionWarningException(List<CatalogCommands.NameCollision> collisions) {
        super("Ingredient name collisions require explicit acknowledgement");
        this.collisions = List.copyOf(collisions);
    }

    public List<CatalogCommands.NameCollision> collisions() {
        return collisions;
    }
}
