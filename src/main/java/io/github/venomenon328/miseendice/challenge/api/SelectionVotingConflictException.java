package io.github.venomenon328.miseendice.challenge.api;

/** A recoverable domain conflict for a stale or incompatible Phase-11B selection interaction. */
public class SelectionVotingConflictException extends RuntimeException {

    public SelectionVotingConflictException(String message) {
        super(message);
    }
}
