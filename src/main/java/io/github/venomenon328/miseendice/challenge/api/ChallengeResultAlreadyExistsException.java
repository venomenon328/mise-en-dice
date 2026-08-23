package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultAlreadyExistsException extends RuntimeException {
    public ChallengeResultAlreadyExistsException(long challengeNumber, long participantId) {
        super("Challenge result already exists for challenge " + challengeNumber + " and participant " + participantId);
    }
}
