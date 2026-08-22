package io.github.venomenon328.miseendice.challenge.api;

/** A new challenge selection cannot start while the default electorate is intentionally empty. */
public final class EmptyDefaultElectorateException extends RuntimeException {
    public EmptyDefaultElectorateException() {
        super("A new challenge selection requires at least one default electorate member");
    }
}
