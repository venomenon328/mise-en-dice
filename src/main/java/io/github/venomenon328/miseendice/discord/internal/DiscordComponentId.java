package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands.VoteOptionType;

/** Versioned, stateless Discord component identifiers. They carry opaque database IDs only. */
final class DiscordComponentId {
    private static final String PREFIX = "med:v1:";

    private DiscordComponentId() {
    }

    static String vote(long sessionId, long roundId, VoteOptionType type, Long offerId) {
        return PREFIX + "vote:" + sessionId + ":" + roundId + ":" + type + ":" + (offerId == null ? "-" : offerId);
    }

    static String initialContinue(long sessionId, long attemptId) {
        return PREFIX + "initial:" + sessionId + ":" + attemptId;
    }

    static String resume(long sessionId) {
        return PREFIX + "resume:" + sessionId;
    }

    static String presentation(long sessionId, long offerSetId) {
        return PREFIX + "present:" + sessionId + ":" + offerSetId;
    }

    static Parsed parse(String value) {
        if (value == null || !value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Unknown component version");
        }
        String[] parts = value.substring(PREFIX.length()).split(":", -1);
        try {
            if (parts.length == 3 && "initial".equals(parts[0])) {
                return new Initial(id(parts[1]), id(parts[2]));
            }
            if (parts.length == 2 && "resume".equals(parts[0])) {
                return new Resume(id(parts[1]));
            }
            if (parts.length == 3 && "present".equals(parts[0])) {
                return new Presentation(id(parts[1]), id(parts[2]));
            }
            if (parts.length == 5 && "vote".equals(parts[0])) {
                VoteOptionType type = VoteOptionType.valueOf(parts[3]);
                Long offerId = "-".equals(parts[4]) ? null : id(parts[4]);
                if ((type == VoteOptionType.OFFER) != (offerId != null)) {
                    throw new IllegalArgumentException("Malformed vote component");
                }
                return new Vote(id(parts[1]), id(parts[2]), type, offerId);
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Malformed component", exception);
        }
        throw new IllegalArgumentException("Malformed component");
    }

    private static long id(String text) {
        long value = Long.parseLong(text);
        if (value <= 0) {
            throw new IllegalArgumentException("IDs must be positive");
        }
        return value;
    }

    sealed interface Parsed permits Vote, Initial, Resume, Presentation {
        long sessionId();
    }

    record Vote(long sessionId, long roundId, VoteOptionType type, Long offerId) implements Parsed {
    }

    record Initial(long sessionId, long attemptId) implements Parsed {
    }

    record Resume(long sessionId) implements Parsed {
    }

    record Presentation(long sessionId, long offerSetId) implements Parsed {
    }
}
