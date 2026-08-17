package io.github.venomenon328.miseendice.challenge.api;

/** Public, transport-neutral Phase-10B use case for a previously generated attempt. */
public interface CurationOrchestrationCommands {

    CurationOutcome curate(long attemptId);

    sealed interface CurationOutcome permits OfferReady, CurationExhausted, InProgress,
            CuratorUnavailable, CuratorFailed, GeneratorExhausted, GeneratorFailed {
        long attemptId();
    }

    record OfferReady(long attemptId, long offerSetId, int offerCount) implements CurationOutcome {
    }

    record CurationExhausted(long attemptId, String reasonCode, String detail) implements CurationOutcome {
    }

    record InProgress(long attemptId, long roundId, String reasonCode) implements CurationOutcome {
    }

    /** Non-terminal result: no request was sent because no productive curator adapter is currently available. */
    record CuratorUnavailable(long attemptId, String reasonCode, String detail) implements CurationOutcome {
    }

    /** Terminal provider or structured-output failure. No further request is permitted for this result. */
    record CuratorFailed(long attemptId, long roundId, String reasonCode, String detail)
            implements CurationOutcome {
    }

    record GeneratorExhausted(long attemptId, long batchId, String reasonCode) implements CurationOutcome {
    }

    record GeneratorFailed(long attemptId, String reasonCode, String detail) implements CurationOutcome {
    }
}
