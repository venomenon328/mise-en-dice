package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultVersionConflictException extends RuntimeException {
    public ChallengeResultVersionConflictException(long challengeNumber, long participantId, long expectedVersion, long actualVersion) {
        super("Challenge result version conflict for challenge " + challengeNumber + " and participant " + participantId
                + ": expected " + expectedVersion + " but was " + actualVersion);
    }
}
