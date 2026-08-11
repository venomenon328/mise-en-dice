package io.github.venomenon328.miseendice.catalog.api;

/** Indicates that an aggregate changed after the client loaded its expected version. */
public final class CatalogVersionConflictException extends RuntimeException {

    private final long conceptId;
    private final long expectedVersion;

    public CatalogVersionConflictException(long conceptId, long expectedVersion) {
        super("Ingredient concept %d no longer has version %d".formatted(conceptId, expectedVersion));
        this.conceptId = conceptId;
        this.expectedVersion = expectedVersion;
    }

    public long conceptId() {
        return conceptId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }
}
