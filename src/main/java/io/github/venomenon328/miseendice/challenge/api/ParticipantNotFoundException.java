package io.github.venomenon328.miseendice.challenge.api;

/** The requested durable participant identity does not exist. */
public final class ParticipantNotFoundException extends RuntimeException {
    public ParticipantNotFoundException(long participantId) {
        super("Participant %d does not exist".formatted(participantId));
    }
}
