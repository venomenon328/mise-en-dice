package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DiscordChallengeRestrictionWorkflowTest {

    @Test
    void forwardsAllExplicitRestrictionModesToInitialPreparation() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        when(preparation.prepareInitial(any())).thenReturn(
                new ChallengeOfferPreparationCommands.Failed(1, 2, "EXPECTED_TEST_FAILURE", null));
        var workflow = workflow(preparation, mock(OfferDecisionQueries.class), mock(SelectionVotingCommands.class),
                mock(SelectionVotingQueries.class));
        DiscordChallengeWorkflow.Delivery delivery = (message, delivered, failed) -> { };
        DiscordChallengeWorkflow.Feedback feedback = mock(DiscordChallengeWorkflow.Feedback.class);

        for (RestrictionMode mode : RestrictionMode.values()) {
            workflow.start(2, mode, delivery, feedback);
        }

        var commands = ArgumentCaptor.forClass(ChallengeOfferPreparationCommands.PrepareInitialOfferSet.class);
        verify(preparation, org.mockito.Mockito.times(3)).prepareInitial(commands.capture());
        assertThat(commands.getAllValues())
                .extracting(ChallengeOfferPreparationCommands.PrepareInitialOfferSet::restrictionMode)
                .containsExactly(RestrictionMode.AUTO, RestrictionMode.NONE, RestrictionMode.REQUIRED);
        assertThat(commands.getAllValues())
                .extracting(ChallengeOfferPreparationCommands.PrepareInitialOfferSet::requestedOfferCount)
                .containsOnly(2);
    }

    @Test
    void resumesRerollFromTheAuthoritativeRestrictionSnapshotWithoutPreparingAgain() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offerQueries = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var votingQueries = mock(SelectionVotingQueries.class);
        var offerSet = restrictedOfferSet();
        var electorate = List.of(new SelectionVotingQueries.ElectorateMemberView(6, "GEORGIA", "Georgia", true));
        var waiting = new SelectionVotingQueries.SelectionView(1, electorate, offerSet, null, List.of(),
                new SelectionVotingQueries.WaitingForPresentationView(offerSet.offerSetId(), 2), null);
        var presented = new SelectionVotingQueries.SelectionView(1, electorate, offerSet, null, List.of(), null, null);
        when(votingQueries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true,
                        "discord", "10001")));
        when(voting.resume(new SelectionVotingCommands.ResumeSelection(1))).thenReturn(waiting);
        when(offerQueries.findOfferSet(offerSet.offerSetId())).thenReturn(java.util.Optional.of(offerSet));
        when(voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(1, offerSet.offerSetId())))
                .thenReturn(presented);
        var messages = new ArrayList<DiscordChallengeRenderer.RenderedMessage>();
        DiscordChallengeWorkflow.Delivery delivery = (message, delivered, failed) -> {
            messages.add(message);
            delivered.run();
        };

        workflow(preparation, offerQueries, voting, votingQueries).component(
                DiscordComponentId.resume(1), "10001", delivery, mock(DiscordChallengeWorkflow.Feedback.class));

        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().content()).contains("Einschränkung: Kein Kochalkohol");
        assertThat(messages.getLast().content()).contains("Einschränkung: Kein Kochalkohol");
        verify(offerQueries).findOfferSet(offerSet.offerSetId());
        verifyNoInteractions(preparation);
    }

    private static DiscordChallengeWorkflow workflow(ChallengeOfferPreparationCommands preparation,
                                                       OfferDecisionQueries offers,
                                                       SelectionVotingCommands voting,
                                                       SelectionVotingQueries queries) {
        DiscordProperties properties = new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002"));
        return new DiscordChallengeWorkflow(properties, preparation, offers, voting, queries,
                new DiscordTestParticipantQueries(properties, queries), new DiscordChallengeRenderer());
    }

    private static OfferDecisionQueries.OfferSetView restrictedOfferSet() {
        var requirements = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(position -> new CurationRequest.RequirementSnapshot(position, "RANDOM", 1L, null, "CODE",
                        "Snapshot " + position, "SPECIFIC", 1, "{}", "{}", "[]"))
                .toList();
        var restriction = new CandidateProposalEngine.CandidateRestriction(
                101L, "NO_COOKING_ALCOHOL", "Kein Kochalkohol");
        var offer = new OfferDecisionQueries.OfferView(21, 1, 31, requirements, restriction);
        return new OfferDecisionQueries.OfferSetView(1, 2, 3, 1, CurationModel.OfferSetStatus.CURATED_UNPRESENTED,
                Instant.now(), null, null, null, List.of(offer));
    }
}
