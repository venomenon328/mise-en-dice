package io.github.venomenon328.miseendice.catalog.api;

import java.util.List;

/** Requires a deliberate confirmation before a relation to an inactive concept is saved. */
public final class CatalogRelationWarningException extends RuntimeException {

    private final List<String> warnings;

    public CatalogRelationWarningException(List<String> warnings) {
        super("The pending ingredient refinements require confirmation");
        this.warnings = List.copyOf(warnings);
    }

    public List<String> warnings() {
        return warnings;
    }
}
