package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultPhotoNotFoundException extends RuntimeException {
    public ChallengeResultPhotoNotFoundException(long challengeNumber, long participantId) {
        super("Challenge result photo does not exist for challenge " + challengeNumber + " and participant " + participantId);
    }
}
