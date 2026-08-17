package io.github.venomenon328.miseendice.challenge.api;

import java.time.LocalDate;

/**
 * Small transport-neutral entry point for the normal initial generation and curation path.
 * Adapters must not assemble Phase-9 and Phase-10 commands themselves.
 */
public interface ChallengeOfferPreparationCommands {

    PreparationOutcome prepareInitial(PrepareInitialOfferSet command);

    PreparationOutcome continueInitial(ContinueInitialOfferSet command);

    record PrepareInitialOfferSet(LocalDate effectiveDate, int requestedOfferCount) {
        public PrepareInitialOfferSet {
            if (effectiveDate == null || requestedOfferCount < 1 || requestedOfferCount > 3) {
                throw new IllegalArgumentException("An effective date and an offer count between 1 and 3 are required");
            }
        }
    }

    /** Stateless restart entry point for an already persisted INITIAL attempt. */
    record ContinueInitialOfferSet(long sessionId, long attemptId) {
        public ContinueInitialOfferSet {
            if (sessionId <= 0 || attemptId <= 0) {
                throw new IllegalArgumentException("Positive session and attempt IDs are required");
            }
        }
    }

    sealed interface PreparationOutcome permits OfferReady, InProgress, Exhausted, Failed {
        long sessionId();
        long attemptId();
    }

    record OfferReady(long sessionId, long attemptId, long offerSetId, int offerCount) implements PreparationOutcome {
    }

    record InProgress(long sessionId, long attemptId, String phase, String reasonCode) implements PreparationOutcome {
    }

    record Exhausted(long sessionId, long attemptId, String reasonCode, String detail) implements PreparationOutcome {
    }

    /** Includes disabled/failed curator and unexpected persisted terminal generation states. */
    record Failed(long sessionId, long attemptId, String reasonCode, String detail) implements PreparationOutcome {
    }
}
