package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;

/** Resolves a current guild member display name from an opaque Discord user ID. */
@FunctionalInterface
interface DiscordMemberNameResolver {
    String resolve(String discordUserId, String storedFallback);

    default String resolveOrFallback(String discordUserId, String storedFallback) {
        try {
            String resolved = resolve(discordUserId, storedFallback);
            return resolved == null || resolved.isBlank() ? storedFallback : resolved;
        } catch (RuntimeException ignored) {
            return storedFallback;
        }
    }

    static String resolveParticipant(ParticipantQueries participants, long participantId, String storedFallback,
                                     DiscordMemberNameResolver memberNames) {
        return participants.findExternalIdentity(participantId, DiscordProperties.PROVIDER)
                .map(identity -> memberNames.resolveOrFallback(identity.externalSubject(), storedFallback))
                .orElse(storedFallback);
    }

    static DiscordMemberNameResolver storedFallback() {
        return (discordUserId, storedFallback) -> storedFallback;
    }
}
