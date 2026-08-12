package io.github.venomenon328.miseendice.catalog.api;

/** Indicates that an exclusion rule changed after the client loaded it. */
public final class CatalogExclusionVersionConflictException extends RuntimeException {

    private final long exclusionRuleId;
    private final long expectedVersion;

    public CatalogExclusionVersionConflictException(long exclusionRuleId, long expectedVersion) {
        super("Exclusion rule %d no longer has version %d".formatted(exclusionRuleId, expectedVersion));
        this.exclusionRuleId = exclusionRuleId;
        this.expectedVersion = expectedVersion;
    }

    public long exclusionRuleId() {
        return exclusionRuleId;
    }

    public long expectedVersion() {
        return expectedVersion;
    }
}
