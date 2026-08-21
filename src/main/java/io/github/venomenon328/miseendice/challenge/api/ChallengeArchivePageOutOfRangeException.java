package io.github.venomenon328.miseendice.challenge.api;

/** A non-empty archive has no requested page. */
public final class ChallengeArchivePageOutOfRangeException extends RuntimeException {

    public ChallengeArchivePageOutOfRangeException(int page, int totalPages) {
        super("Challenge archive page %d is outside 1..%d".formatted(page, totalPages));
    }
}
