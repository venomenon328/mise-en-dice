package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.InactiveParticipantException;
import io.github.venomenon328.miseendice.challenge.api.ParticipantCommands;
import io.github.venomenon328.miseendice.challenge.api.ParticipantQueries;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordParticipantAdministrationWorkflowTest {

    @Test
    void createsANewDiscordParticipantWithoutAddingItToTheElectorate() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.empty());
        when(commands.resolveOrCreateParticipant(any())).thenReturn(participant(6, "PARTICIPANT-1", "Neue Person", true, false));
        TestDelivery delivery = new TestDelivery();

        workflow(commands, queries).create("10001", "Neue Person", delivery);

        verify(commands).resolveOrCreateParticipant(new ParticipantCommands.ResolveOrCreateParticipant(
                "discord", "10001", "Neue Person"));
        assertThat(delivery.success).singleElement().satisfies(message ->
                assertThat(message).contains("angelegt", "nicht im Standard-Elektorat"));
    }

    @Test
    void reportsRepeatedCreationIdempotently() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        ParticipantQueries.ParticipantView existing = participant(6, "PARTICIPANT-1", "Neue Person", true, false);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(existing));
        when(commands.resolveOrCreateParticipant(any())).thenReturn(existing);
        TestDelivery delivery = new TestDelivery();

        workflow(commands, queries).create("10001", "Neue Person", delivery);

        assertThat(delivery.success).singleElement().satisfies(message -> assertThat(message).contains("besteht bereits"));
    }

    @Test
    void activatesAndDeactivatesKnownParticipantsWithoutChangingRunningSnapshots() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        ParticipantQueries.ParticipantView inactive = participant(6, "PARTICIPANT-1", "Neue Person", false, false);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(inactive));
        when(commands.activateParticipant(any())).thenReturn(participant(6, "PARTICIPANT-1", "Neue Person", true, false));
        when(commands.deactivateParticipant(any())).thenReturn(inactive);
        TestDelivery delivery = new TestDelivery();

        workflow(commands, queries).activate("10001", delivery);
        workflow(commands, queries).deactivate("10001", delivery);

        verify(commands).activateParticipant(new ParticipantCommands.ActivateParticipant(6));
        verify(commands).deactivateParticipant(new ParticipantCommands.DeactivateParticipant(6));
        assertThat(delivery.success).anySatisfy(message -> assertThat(message).contains("aktiv"))
                .anySatisfy(message -> assertThat(message).contains("künftige", "laufende Snapshots"));
    }

    @Test
    void rejectsUnknownAndInactivePeopleForElectorateAdministration() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        ParticipantQueries.ParticipantView inactive = participant(6, "PARTICIPANT-1", "Neue Person", false, false);
        when(queries.findParticipantByExternalIdentity("discord", "unknown")).thenReturn(java.util.Optional.empty());
        when(queries.findParticipantByExternalIdentity("discord", "inactive")).thenReturn(java.util.Optional.of(inactive));
        when(commands.addDefaultElectorateMember(any())).thenThrow(new InactiveParticipantException(6));
        TestDelivery delivery = new TestDelivery();

        workflow(commands, queries).addToDefaultElectorate("unknown", delivery);
        workflow(commands, queries).addToDefaultElectorate("inactive", delivery);

        assertThat(delivery.rejected).anySatisfy(message -> assertThat(message).contains("kein Teilnehmer"))
                .anySatisfy(message -> assertThat(message).contains("Deaktivierte Teilnehmer"));
    }

    @Test
    void removesAnElectorAndListsMixedAdministrativeStates() {
        ParticipantCommands commands = mock(ParticipantCommands.class);
        ParticipantQueries queries = mock(ParticipantQueries.class);
        ParticipantQueries.ParticipantView activeElector = participant(6, "PARTICIPANT-1", "Aktiv", true, true);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(activeElector));
        when(queries.listParticipants()).thenReturn(List.of(activeElector, participant(7, "PARTICIPANT-2", "Inaktiv", false, false)));
        TestDelivery delivery = new TestDelivery();

        workflow(commands, queries).removeFromDefaultElectorate("10001", delivery);
        workflow(commands, queries).list(delivery);

        verify(commands).removeDefaultElectorateMember(new ParticipantCommands.RemoveDefaultElectorateMember(6));
        assertThat(delivery.success).anySatisfy(message -> assertThat(message).contains("Aktiv", "deaktiviert", "Standard-Elektorat: ja"));
    }

    private static DiscordParticipantAdministrationWorkflow workflow(ParticipantCommands commands, ParticipantQueries queries) {
        return new DiscordParticipantAdministrationWorkflow(commands, queries);
    }

    private static ParticipantQueries.ParticipantView participant(long id, String code, String name, boolean active,
                                                                   boolean electorateMember) {
        return new ParticipantQueries.ParticipantView(id, code, name, active, electorateMember);
    }

    private static final class TestDelivery implements DiscordParticipantAdministrationWorkflow.Delivery {
        private final List<String> success = new ArrayList<>();
        private final List<String> rejected = new ArrayList<>();

        @Override public void success(String message) { success.add(message); }
        @Override public void rejected(String message) { rejected.add(message); }
        @Override public void technicalFailure(Throwable exception) { throw new AssertionError(exception); }
    }
}
