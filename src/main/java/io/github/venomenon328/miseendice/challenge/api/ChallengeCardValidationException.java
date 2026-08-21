package io.github.venomenon328.miseendice.challenge.api;

/** The supplied bytes are not a permitted Challenge Card PNG. */
public final class ChallengeCardValidationException extends RuntimeException {

    public ChallengeCardValidationException(String message) {
        super(message);
    }
}
