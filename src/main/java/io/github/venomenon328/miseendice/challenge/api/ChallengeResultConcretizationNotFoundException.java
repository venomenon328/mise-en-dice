package io.github.venomenon328.miseendice.challenge.api;

/** Raised when a targeted personal requirement concretization no longer exists. */
public final class ChallengeResultConcretizationNotFoundException extends RuntimeException {
    public ChallengeResultConcretizationNotFoundException(long resultId, int requirementPosition) {
        super("Challenge result " + resultId + " has no concretization at requirement position " + requirementPosition);
    }
}
