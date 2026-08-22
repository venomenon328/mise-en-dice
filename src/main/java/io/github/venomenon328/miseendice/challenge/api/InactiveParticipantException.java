package io.github.venomenon328.miseendice.challenge.api;

/** An inactive participant cannot be newly added to the mutable default electorate. */
public final class InactiveParticipantException extends RuntimeException {
    public InactiveParticipantException(long participantId) {
        super("Inactive participant %d cannot be added to the default electorate".formatted(participantId));
    }
}
