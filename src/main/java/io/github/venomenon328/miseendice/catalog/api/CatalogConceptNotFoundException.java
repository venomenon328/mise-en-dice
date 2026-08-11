package io.github.venomenon328.miseendice.catalog.api;

/** Indicates that a requested ingredient concept does not exist anymore. */
public final class CatalogConceptNotFoundException extends RuntimeException {

    private final long conceptId;

    public CatalogConceptNotFoundException(long conceptId) {
        super("Ingredient concept %d does not exist".formatted(conceptId));
        this.conceptId = conceptId;
    }

    public long conceptId() {
        return conceptId;
    }
}
