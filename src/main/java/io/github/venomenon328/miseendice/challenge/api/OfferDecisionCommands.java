package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;

/** Public transport-neutral Phase-11A commands for presenting, confirming, and rerolling curated offers. */
public interface OfferDecisionCommands {

    Presentation present(PresentOfferSet command);

    Confirmation confirm(ConfirmOffer command);

    RerollOutcome reroll(RerollOfferSet command);

    record PresentOfferSet(long offerSetId) {
        public PresentOfferSet {
            requireId(offerSetId, "Offer set");
        }
    }

    record ConfirmOffer(long offerSetId, long offerId) {
        public ConfirmOffer {
            requireId(offerSetId, "Offer set");
            requireId(offerId, "Offer");
        }
    }

    /** An explicit seed is reserved for deterministic tests and replay; transports do not choose it. */
    record RerollOfferSet(long offerSetId, Long explicitSeed) {
        public RerollOfferSet {
            requireId(offerSetId, "Offer set");
        }

        public RerollOfferSet(long offerSetId) {
            this(offerSetId, null);
        }
    }

    record Presentation(long sessionId, long attemptId, long offerSetId, CurationModel.OfferSetStatus status,
                        Instant presentedAt) {
    }

    record Confirmation(long sessionId, long attemptId, long offerSetId, long offerId, long candidateId,
                        long challengeId, Instant decidedAt) {
    }

    sealed interface RerollOutcome permits RerollOfferReady, RerollInProgress, RerollExhausted, RerollFailed {
        long sessionId();
        long sourceOfferSetId();
        long rerollAttemptId();
    }

    record RerollOfferReady(long sessionId, long sourceOfferSetId, long rerollAttemptId,
                            long offerSetId, int offerCount) implements RerollOutcome {
    }

    /** A durable generation, provider, or curation step is waiting/recoverable; repeat this command to continue it. */
    record RerollInProgress(long sessionId, long sourceOfferSetId, long rerollAttemptId,
                            String phase, String reasonCode) implements RerollOutcome {
    }

    record RerollExhausted(long sessionId, long sourceOfferSetId, long rerollAttemptId,
                           String reasonCode, String detail) implements RerollOutcome {
    }

    record RerollFailed(long sessionId, long sourceOfferSetId, long rerollAttemptId,
                        String reasonCode, String detail) implements RerollOutcome {
    }

    private static void requireId(long value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(label + " ID must be positive");
        }
    }
}
