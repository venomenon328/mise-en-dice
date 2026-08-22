package io.github.venomenon328.miseendice.discord.internal;

import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantIdentityConflictException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import org.springframework.context.SmartLifecycle;

/** Imports the former Georgia/Tobias property map once, before the Discord gateway starts. */
final class DiscordLegacyParticipantBootstrap implements SmartLifecycle {
    private static final int PHASE = Integer.MAX_VALUE - 200;

    private final DiscordProperties properties;
    private final ParticipantCommands commands;
    private final ParticipantQueries queries;
    private volatile boolean running;

    DiscordLegacyParticipantBootstrap(DiscordProperties properties, ParticipantCommands commands, ParticipantQueries queries) {
        this.properties = properties;
        this.commands = commands;
        this.queries = queries;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        if (properties.hasLegacyParticipantBootstrap()) {
            for (String participantCode : java.util.List.of("GEORGIA", "TOBIAS")) {
                String discordUserId = properties.participantUserIds().get(participantCode);
                if (discordUserId == null) {
                    continue;
                }
                ParticipantQueries.ParticipantView participant = queries.findParticipantByCode(participantCode)
                        .orElseThrow(() -> new IllegalStateException(
                                "Discord legacy bootstrap cannot find participant " + participantCode));
                try {
                    commands.linkExternalIdentity(new ParticipantCommands.LinkExternalIdentity(participant.participantId(),
                            DiscordProperties.PROVIDER, discordUserId));
                } catch (ParticipantIdentityConflictException exception) {
                    throw new IllegalStateException("Discord legacy bootstrap conflicts for " + participantCode
                            + "; the database identity was not overwritten", exception);
                }
            }
        }
        running = true;
    }

    @Override
    public synchronized void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return PHASE;
    }
}
