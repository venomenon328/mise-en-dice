package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultPhotoAlreadyExistsException extends RuntimeException {
    public ChallengeResultPhotoAlreadyExistsException(long challengeNumber, long participantId) {
        super("Challenge result photo already exists for challenge " + challengeNumber + " and participant " + participantId);
    }
}
