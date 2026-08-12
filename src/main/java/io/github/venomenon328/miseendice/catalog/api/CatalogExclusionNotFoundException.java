package io.github.venomenon328.miseendice.catalog.api;

/** Indicates that a requested exclusion rule does not exist anymore. */
public final class CatalogExclusionNotFoundException extends RuntimeException {

    private final long exclusionRuleId;

    public CatalogExclusionNotFoundException(long exclusionRuleId) {
        super("Exclusion rule %d does not exist".formatted(exclusionRuleId));
        this.exclusionRuleId = exclusionRuleId;
    }

    public long exclusionRuleId() {
        return exclusionRuleId;
    }
}
