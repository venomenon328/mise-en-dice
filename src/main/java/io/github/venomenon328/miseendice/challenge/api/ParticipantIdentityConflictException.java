package io.github.venomenon328.miseendice.challenge.api;

/** A provider identity must never be silently reassigned to another participant. */
public class ParticipantIdentityConflictException extends RuntimeException {
    public ParticipantIdentityConflictException(String message) {
        super(message);
    }
}
