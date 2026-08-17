package io.github.venomenon328.miseendice.challenge.api;

/** A deterministic lifecycle conflict; unknown persistence failures are never converted to this type. */
public final class CurationConflictException extends RuntimeException {
    public CurationConflictException(String message) {
        super(message);
    }
}
