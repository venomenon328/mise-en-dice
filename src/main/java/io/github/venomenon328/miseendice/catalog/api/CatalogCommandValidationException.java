package io.github.venomenon328.miseendice.catalog.api;

import java.util.Map;

/** A known, presentation-safe validation failure for a catalog command. */
public final class CatalogCommandValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public CatalogCommandValidationException(Map<String, String> fieldErrors) {
        super("The catalog command contains invalid values");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
