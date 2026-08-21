package io.github.venomenon328.miseendice.challenge.api;

/** Replacing a Card must be explicit so an accidental upload never silently overwrites it. */
public final class ChallengeCardAlreadyExistsException extends RuntimeException {

    public ChallengeCardAlreadyExistsException(long challengeNumber) {
        super("Challenge #%d already has a Card; set replaceExisting to true to replace it".formatted(challengeNumber));
    }
}
