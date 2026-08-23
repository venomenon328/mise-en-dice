package io.github.venomenon328.miseendice.challenge.api;

public class ChallengeResultNotFoundException extends RuntimeException {
    public ChallengeResultNotFoundException(long challengeNumber, long participantId) {
        super("Challenge result does not exist for challenge " + challengeNumber + " and participant " + participantId);
    }
}
