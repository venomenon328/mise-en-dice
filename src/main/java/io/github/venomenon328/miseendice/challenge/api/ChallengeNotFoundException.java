package io.github.venomenon328.miseendice.challenge.api;

/** A Card command addressed a public challenge number that does not exist. */
public final class ChallengeNotFoundException extends RuntimeException {

    private final long challengeNumber;

    public ChallengeNotFoundException(long challengeNumber) {
        super("Challenge #%d does not exist".formatted(challengeNumber));
        this.challengeNumber = challengeNumber;
    }

    public long challengeNumber() {
        return challengeNumber;
    }
}
