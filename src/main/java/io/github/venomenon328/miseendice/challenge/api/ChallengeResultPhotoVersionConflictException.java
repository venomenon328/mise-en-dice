package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultPhotoVersionConflictException extends RuntimeException {
    public ChallengeResultPhotoVersionConflictException(long challengeNumber, long participantId, Long expectedVersion,
                                                       long actualVersion) {
        super("Challenge result photo version conflict for challenge " + challengeNumber + " and participant "
                + participantId + ": expected " + expectedVersion + " but was " + actualVersion);
    }
}
