package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantIdentityConflictException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DiscordLegacyParticipantBootstrapTest {

    @Test
    void importsMatchingGeorgiaAndTobiasMappingsIdempotentlyBeforeTheGatewayPhase() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        when(queries.findParticipantByCode("GEORGIA")).thenReturn(java.util.Optional.of(participant(1, "GEORGIA")));
        when(queries.findParticipantByCode("TOBIAS")).thenReturn(java.util.Optional.of(participant(2, "TOBIAS")));
        when(commands.linkExternalIdentity(any())).thenAnswer(invocation -> {
            ParticipantCommands.LinkExternalIdentity command = invocation.getArgument(0);
            return new ParticipantQueries.ExternalIdentityView(command.participantId(), command.provider(),
                    command.externalSubject());
        });

        DiscordLegacyParticipantBootstrap first = bootstrap(commands, queries, Map.of("GEORGIA", "10001", "TOBIAS", "10002"));
        first.start();
        DiscordLegacyParticipantBootstrap restarted = bootstrap(commands, queries, Map.of("GEORGIA", "10001", "TOBIAS", "10002"));
        restarted.start();

        verify(commands, org.mockito.Mockito.times(2)).linkExternalIdentity(
                new ParticipantCommands.LinkExternalIdentity(1, "discord", "10001"));
        verify(commands, org.mockito.Mockito.times(2)).linkExternalIdentity(
                new ParticipantCommands.LinkExternalIdentity(2, "discord", "10002"));
        assertThat(first.getPhase()).isLessThan(Integer.MAX_VALUE - 100);
    }

    @Test
    void failsClearlyWithoutOverwritingWhenTheDatabaseMappingConflicts() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        when(queries.findParticipantByCode("GEORGIA")).thenReturn(java.util.Optional.of(participant(1, "GEORGIA")));
        when(commands.linkExternalIdentity(any())).thenThrow(
                new ParticipantIdentityConflictException("External identity is already linked to another participant"));

        assertThatThrownBy(() -> bootstrap(commands, queries, Map.of("GEORGIA", "10001")).start())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEORGIA", "was not overwritten")
                .hasCauseInstanceOf(ParticipantIdentityConflictException.class);
        verify(commands).linkExternalIdentity(new ParticipantCommands.LinkExternalIdentity(1, "discord", "10001"));
        verify(queries, never()).findParticipantByCode("TOBIAS");
    }

    @Test
    void startsWithoutLegacyPropertiesForLaterDatabaseManagedParticipants() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);

        DiscordLegacyParticipantBootstrap bootstrap = bootstrap(commands, queries, Map.of());
        bootstrap.start();

        assertThat(bootstrap.isRunning()).isTrue();
        org.mockito.Mockito.verifyNoInteractions(commands, queries);
    }

    private static DiscordLegacyParticipantBootstrap bootstrap(ParticipantCommands commands, ParticipantQueries queries,
                                                                Map<String, String> legacyMappings) {
        return new DiscordLegacyParticipantBootstrap(new DiscordProperties(true, "token", 99, 777, ZoneId.of("Europe/Berlin"),
                legacyMappings), commands, queries);
    }

    private static ParticipantQueries.ParticipantView participant(long id, String code) {
        return new ParticipantQueries.ParticipantView(id, code, code, true, true);
    }
}
