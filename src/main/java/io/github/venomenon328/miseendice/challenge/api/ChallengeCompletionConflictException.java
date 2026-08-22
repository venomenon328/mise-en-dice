package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeCompletionConflictException extends RuntimeException {
    public ChallengeCompletionConflictException(long challengeNumber, ChallengeArchiveQueries.ChallengeStatus status) {
        super("Challenge " + challengeNumber + " cannot be completed from status " + status);
    }
}
