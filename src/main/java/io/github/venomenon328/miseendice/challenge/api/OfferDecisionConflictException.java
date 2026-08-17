package io.github.venomenon328.miseendice.challenge.api;

/** A requested presentation/confirmation/reroll contradicts the persisted offer-decision lifecycle. */
public final class OfferDecisionConflictException extends RuntimeException {
    public OfferDecisionConflictException(String message) {
        super(message);
    }
}
