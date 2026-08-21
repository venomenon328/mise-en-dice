package io.github.venomenon328.miseendice.challenge.api;

/** Removing a missing Card is a typed no-mutation outcome. */
public final class ChallengeCardNotFoundException extends RuntimeException {

    public ChallengeCardNotFoundException(long challengeNumber) {
        super("Challenge #%d has no Card".formatted(challengeNumber));
    }
}
