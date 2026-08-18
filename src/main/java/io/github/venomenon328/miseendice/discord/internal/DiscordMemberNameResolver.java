package io.github.venomenon328.miseendice.discord.internal;

/** Resolves a current guild member display name from an opaque Discord user ID. */
@FunctionalInterface
interface DiscordMemberNameResolver {
    String resolve(String discordUserId, String storedFallback);

    static DiscordMemberNameResolver storedFallback() {
        return (discordUserId, storedFallback) -> storedFallback;
    }
}
