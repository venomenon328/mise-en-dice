package io.github.venomenon328.miseendice.challenge.api;

import java.time.Instant;

/** Explicit, idempotent lifecycle transition for a confirmed Challenge. */
public interface ChallengeCompletionCommands {

    Completion completeChallenge(CompleteChallenge command);

    record CompleteChallenge(long challengeNumber) {
        public CompleteChallenge {
            if (challengeNumber <= 0) {
                throw new IllegalArgumentException("Challenge number must be positive");
            }
        }
    }

    record Completion(long challengeNumber, ChallengeArchiveQueries.ChallengeStatus status, Instant completedAt) {
        public Completion {
            if (challengeNumber <= 0 || status != ChallengeArchiveQueries.ChallengeStatus.COMPLETED || completedAt == null) {
                throw new IllegalArgumentException("A completion must describe a completed Challenge with a timestamp");
            }
        }
    }
}
