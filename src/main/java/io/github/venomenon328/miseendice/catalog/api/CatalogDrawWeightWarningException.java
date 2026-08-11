package io.github.venomenon328.miseendice.catalog.api;

import java.util.List;

/** Requires an explicit acknowledgement before intentionally exceeding a baseline draw-weight guideline. */
public final class CatalogDrawWeightWarningException extends RuntimeException {

    private final List<String> warnings;

    public CatalogDrawWeightWarningException(List<String> warnings) {
        super("The requested draw weight exceeds a baseline guideline");
        this.warnings = List.copyOf(warnings);
    }

    public List<String> warnings() {
        return warnings;
    }
}
