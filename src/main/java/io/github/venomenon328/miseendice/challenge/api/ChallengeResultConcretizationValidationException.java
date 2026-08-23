package io.github.venomenon328.miseendice.challenge.api;

/** Raised when a personal concretization targets no OPEN requirement or links an unrelated catalog concept. */
public final class ChallengeResultConcretizationValidationException extends RuntimeException {
    public ChallengeResultConcretizationValidationException(String message) {
        super(message);
    }
}
