package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class DiscordChallengeWorkflowTest {

    @Test
    void reportsPresentationOnlyAfterVisibleOfferDelivery() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var sequence = new ArrayList<String>();
        var offerSet = offerSet();
        when(preparation.prepareInitial(any())).thenReturn(new ChallengeOfferPreparationCommands.OfferReady(1, 2, 3, 1));
        when(offers.findOfferSet(3)).thenReturn(java.util.Optional.of(offerSet));
        doAnswer(invocation -> { sequence.add("handshake"); return selection(offerSet); })
                .when(voting).presentationSucceeded(any());

        workflow(preparation, offers, voting, queries).start(1, (message, delivered, failed) -> {
            sequence.add("delivered");
            delivered.run();
        }, feedback());

        assertThat(sequence).startsWith("delivered", "handshake");
    }

    @Test
    void failedDeliveryDoesNotReportPresentation() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        when(preparation.prepareInitial(any())).thenReturn(new ChallengeOfferPreparationCommands.OfferReady(1, 2, 3, 1));
        when(offers.findOfferSet(3)).thenReturn(java.util.Optional.of(offerSet()));

        workflow(preparation, offers, voting, queries).start(1, (message, delivered, failed) ->
                failed.accept(new IllegalStateException("Discord unavailable")), feedback());

        org.mockito.Mockito.verifyNoInteractions(voting);
    }

    @Test
    void startsTheTransportNeutralPreparationFacadeForOneTwoAndThreeOffers() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        when(preparation.prepareInitial(any())).thenReturn(new ChallengeOfferPreparationCommands.Failed(1, 2, "FAILED", null));
        var workflow = workflow(preparation, offers, voting, queries);

        workflow.start(1, delivered(), feedback());
        workflow.start(2, delivered(), feedback());
        workflow.start(3, delivered(), feedback());

        var commands = ArgumentCaptor.forClass(ChallengeOfferPreparationCommands.PrepareInitialOfferSet.class);
        verify(preparation, org.mockito.Mockito.times(3)).prepareInitial(commands.capture());
        assertThat(commands.getAllValues()).extracting(ChallengeOfferPreparationCommands.PrepareInitialOfferSet::requestedOfferCount)
                .containsExactly(1, 2, 3);
    }

    @Test
    void acceptsOnlyConfiguredGuildAndDiscordUsers() {
        var workflow = workflow(mock(ChallengeOfferPreparationCommands.class), mock(OfferDecisionQueries.class),
                mock(SelectionVotingCommands.class), mock(SelectionVotingQueries.class));

        assertThat(workflow.accepts(99, "10001")).isTrue();
        assertThat(workflow.accepts(98, "10001")).isFalse();
        assertThat(workflow.accepts(99, "not-a-participant")).isFalse();
    }

    @Test
    void resolvesIdentityValidatesTheCurrentRoundAndThenCastsTheVote() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var set = offerSet();
        var selection = selection(set);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(queries.findSelection(1)).thenReturn(java.util.Optional.of(selection));
        when(voting.castVote(any())).thenReturn(selection);
        var result = new TestFeedback();

        workflow(preparation, offers, voting, queries).component(
                DiscordComponentId.vote(1, 7, SelectionVotingCommands.VoteOptionType.ACCEPT, null), "10001", delivered(), result);

        var command = ArgumentCaptor.forClass(SelectionVotingCommands.CastVote.class);
        verify(voting).castVote(command.capture());
        assertThat(command.getValue().participantId()).isEqualTo(6);
        assertThat(command.getValue().choice()).isEqualTo(SelectionVotingCommands.VoteChoice.accept());
        assertThat(result.success).containsExactly("Deine Stimme wurde gespeichert.");
    }

    @Test
    void rejectsStaleVoteWithoutChangingDomainState() {
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var set = offerSet();
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(queries.findSelection(1)).thenReturn(java.util.Optional.of(selection(set)));
        var result = new TestFeedback();

        workflow(mock(ChallengeOfferPreparationCommands.class), mock(OfferDecisionQueries.class), voting, queries).component(
                DiscordComponentId.vote(1, 99, SelectionVotingCommands.VoteOptionType.ACCEPT, null), "10001", delivered(), result);

        verify(voting, never()).castVote(any());
        assertThat(result.stale).containsExactly("Diese Interaktion ist nicht mehr aktuell oder nicht erlaubt.");
    }

    @Test
    void resumesRerollOnlyThroughThePhaseElevenBCommandThenPresentsTheNewSet() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var set = offerSet();
        var waiting = new SelectionVotingQueries.SelectionView(1, selection(set).electorate(), set, null, List.of(),
                new SelectionVotingQueries.WaitingForPresentationView(3, 1), null);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(voting.resume(new SelectionVotingCommands.ResumeSelection(1))).thenReturn(waiting);
        when(offers.findOfferSet(3)).thenReturn(java.util.Optional.of(set));
        when(voting.presentationSucceeded(any())).thenReturn(selection(set));

        workflow(preparation, offers, voting, queries).component(DiscordComponentId.resume(1), "10001", delivered(), feedback());

        verify(voting).resume(new SelectionVotingCommands.ResumeSelection(1));
        verify(voting).presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(1, 3));
    }

    @Test
    void reportsTechnicalFailuresWithoutMaskingThemAsStaleInteractions() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        when(preparation.prepareInitial(any())).thenThrow(new IllegalStateException("database unavailable"));
        var result = new TestFeedback();

        workflow(preparation, mock(OfferDecisionQueries.class), mock(SelectionVotingCommands.class), mock(SelectionVotingQueries.class))
                .start(1, delivered(), result);

        assertThat(result.technical).hasSize(1);
        assertThat(result.stale).isEmpty();
    }

    @Test
    void permitsAnElectorWhoseParticipantIsNowInactiveBecauseTheElectorateIsFrozen() {
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var set = offerSet();
        var selection = selection(set);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", false, "discord", "10001")));
        when(queries.findSelection(1)).thenReturn(java.util.Optional.of(selection));
        when(voting.castVote(any())).thenReturn(selection);

        workflow(mock(ChallengeOfferPreparationCommands.class), offers, voting, queries).component(
                DiscordComponentId.vote(1, 7, SelectionVotingCommands.VoteOptionType.ACCEPT, null), "10001", delivered(), feedback());

        verify(voting).castVote(any());
    }

    @Test
    void derivesThreeOfferChoicesRerollAndVoteChangesFromTheCurrentRound() {
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var set = offerSet(3);
        var round = new SelectionVotingQueries.VotingRoundView(7, 1, set.offerSetId(),
                SelectionVotingQueries.VotingRoundStatus.OPEN,
                List.of(new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.OFFER, set.offers().get(0).offerId()),
                        new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.OFFER, set.offers().get(1).offerId()),
                        new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.OFFER, set.offers().get(2).offerId()),
                        new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.REROLL, null)),
                List.of(new SelectionVotingQueries.VoteStatusView(6, "GEORGIA", "Georgia", false, null)), null);
        var selection = new SelectionVotingQueries.SelectionView(1, List.of(
                new SelectionVotingQueries.ElectorateMemberView(6, "GEORGIA", "Georgia", true)), set, round, List.of(), null, null);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(queries.findSelection(1)).thenReturn(java.util.Optional.of(selection));
        when(voting.castVote(any())).thenReturn(selection);
        var workflow = workflow(mock(ChallengeOfferPreparationCommands.class), mock(OfferDecisionQueries.class), voting, queries);

        workflow.component(DiscordComponentId.vote(1, 7, SelectionVotingCommands.VoteOptionType.OFFER, set.offers().get(0).offerId()),
                "10001", delivered(), feedback());
        workflow.component(DiscordComponentId.vote(1, 7, SelectionVotingCommands.VoteOptionType.OFFER, set.offers().get(2).offerId()),
                "10001", delivered(), feedback());
        workflow.component(DiscordComponentId.vote(1, 7, SelectionVotingCommands.VoteOptionType.REROLL, null),
                "10001", delivered(), feedback());

        var commands = ArgumentCaptor.forClass(SelectionVotingCommands.CastVote.class);
        verify(voting, org.mockito.Mockito.times(3)).castVote(commands.capture());
        assertThat(commands.getAllValues()).extracting(SelectionVotingCommands.CastVote::choice).containsExactly(
                SelectionVotingCommands.VoteChoice.offer(set.offers().get(0).offerId()),
                SelectionVotingCommands.VoteChoice.offer(set.offers().get(2).offerId()),
                SelectionVotingCommands.VoteChoice.reroll());
    }

    @Test
    void continuesInitialCuratorUnavailabilityThroughTheFacade() {
        var preparation = mock(ChallengeOfferPreparationCommands.class);
        when(preparation.prepareInitial(any())).thenReturn(new ChallengeOfferPreparationCommands.InProgress(
                1, 2, "CURATION", "CURATOR_ADAPTER_DISABLED"));
        when(preparation.continueInitial(new ChallengeOfferPreparationCommands.ContinueInitialOfferSet(1, 2)))
                .thenReturn(new ChallengeOfferPreparationCommands.Exhausted(1, 2, "CURATION_EXHAUSTED", null));
        var messages = new ArrayList<DiscordChallengeRenderer.RenderedMessage>();
        var delivery = recordingDelivery(messages);
        var workflow = workflow(preparation, mock(OfferDecisionQueries.class), mock(SelectionVotingCommands.class),
                mock(SelectionVotingQueries.class));

        workflow.start(1, delivery, feedback());
        workflow.component(messages.getFirst().components().getFirst().customId(), "10001", delivery, feedback());

        verify(preparation).continueInitial(new ChallengeOfferPreparationCommands.ContinueInitialOfferSet(1, 2));
        assertThat(messages.getFirst().content()).contains("Kuration gerade nicht erreichbar");
        assertThat(messages.getLast().content()).contains("Keine Challenge verfügbar");
    }

    @Test
    void presentsRerollOffersThenRendersOnlySecondRoundChoices() {
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var rerollSet = offerSet(2);
        var electorate = List.of(new SelectionVotingQueries.ElectorateMemberView(6, "GEORGIA", "Georgia", true),
                new SelectionVotingQueries.ElectorateMemberView(7, "TOBIAS", "Tobias", true));
        var waiting = new SelectionVotingQueries.SelectionView(1, electorate, rerollSet, null, List.of(),
                new SelectionVotingQueries.WaitingForPresentationView(3, 2), null);
        var secondRound = new SelectionVotingQueries.SelectionView(1, electorate, rerollSet,
                new SelectionVotingQueries.VotingRoundView(8, 2, 3, SelectionVotingQueries.VotingRoundStatus.OPEN,
                        List.of(new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.OFFER,
                                rerollSet.offers().get(0).offerId()),
                                new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.OFFER,
                                rerollSet.offers().get(1).offerId())), List.of(), null), List.of(), null, null);
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(voting.resume(new SelectionVotingCommands.ResumeSelection(1))).thenReturn(waiting);
        when(offers.findOfferSet(3)).thenReturn(java.util.Optional.of(rerollSet));
        when(voting.presentationSucceeded(any())).thenReturn(secondRound);
        var messages = new ArrayList<DiscordChallengeRenderer.RenderedMessage>();

        workflow(mock(ChallengeOfferPreparationCommands.class), offers, voting, queries).component(
                DiscordComponentId.resume(1), "10001", recordingDelivery(messages), feedback());

        assertThat(messages.getLast().content()).contains("Runde 2");
        assertThat(messages.getLast().components()).extracting(DiscordChallengeRenderer.Component::label)
                .containsExactly("Vorschlag 1 wählen", "Vorschlag 2 wählen");
    }

    @Test
    void autoConfirmedSingleRerollOfferRendersTheConfirmedSnapshotWithoutACastVote() {
        var offers = mock(OfferDecisionQueries.class);
        var voting = mock(SelectionVotingCommands.class);
        var queries = mock(SelectionVotingQueries.class);
        var rerollSet = confirmedOfferSet();
        var electorate = List.of(new SelectionVotingQueries.ElectorateMemberView(6, "GEORGIA", "Georgia", true),
                new SelectionVotingQueries.ElectorateMemberView(7, "TOBIAS", "Tobias", true));
        var waiting = new SelectionVotingQueries.SelectionView(1, electorate, rerollSet, null, List.of(),
                new SelectionVotingQueries.WaitingForPresentationView(3, 1), null);
        var confirmed = new SelectionVotingQueries.SelectionView(1, electorate, rerollSet, null, List.of(), null,
                new SelectionVotingQueries.ChallengeParticipationView(12, List.of()));
        when(queries.findParticipantByExternalIdentity("discord", "10001")).thenReturn(java.util.Optional.of(
                new SelectionVotingQueries.ParticipantIdentityView(6, "GEORGIA", "Georgia", true, "discord", "10001")));
        when(voting.resume(new SelectionVotingCommands.ResumeSelection(1))).thenReturn(waiting);
        when(offers.findOfferSet(3)).thenReturn(java.util.Optional.of(rerollSet));
        when(voting.presentationSucceeded(any())).thenReturn(confirmed);
        var messages = new ArrayList<DiscordChallengeRenderer.RenderedMessage>();

        workflow(mock(ChallengeOfferPreparationCommands.class), offers, voting, queries).component(
                DiscordComponentId.resume(1), "10001", recordingDelivery(messages), feedback());

        verify(voting, never()).castVote(any());
        assertThat(messages.getLast().content()).contains("Challenge bestätigt: Vorschlag 1", "Zutat 1.1");
    }

    private static DiscordChallengeWorkflow workflow(ChallengeOfferPreparationCommands preparation,
                                                       OfferDecisionQueries offers, SelectionVotingCommands voting,
                                                       SelectionVotingQueries queries) {
        return new DiscordChallengeWorkflow(new DiscordProperties(true, "token", 99, ZoneId.of("Europe/Berlin"),
                Map.of("GEORGIA", "10001", "TOBIAS", "10002")), preparation, offers, voting, queries,
                new DiscordChallengeRenderer());
    }

    private static DiscordChallengeWorkflow.Feedback feedback() {
        return new DiscordChallengeWorkflow.Feedback() {
            @Override public void success(String message) { }
            @Override public void staleOrRejected(String message) { }
            @Override public void technicalFailure(Throwable exception) { }
        };
    }

    private static DiscordChallengeWorkflow.Delivery delivered() {
        return (message, delivered, failed) -> delivered.run();
    }

    private static DiscordChallengeWorkflow.Delivery recordingDelivery(List<DiscordChallengeRenderer.RenderedMessage> messages) {
        return (message, delivered, failed) -> {
            messages.add(message);
            delivered.run();
        };
    }

    private static final class TestFeedback implements DiscordChallengeWorkflow.Feedback {
        private final List<String> success = new ArrayList<>();
        private final List<String> stale = new ArrayList<>();
        private final List<Throwable> technical = new ArrayList<>();

        @Override public void success(String message) { success.add(message); }
        @Override public void staleOrRejected(String message) { stale.add(message); }
        @Override public void technicalFailure(Throwable exception) { technical.add(exception); }
    }

    private static OfferDecisionQueries.OfferSetView offerSet() {
        return offerSet(1);
    }

    private static OfferDecisionQueries.OfferSetView offerSet(int offerCount) {
        List<OfferDecisionQueries.OfferView> offers = java.util.stream.IntStream.rangeClosed(1, offerCount).mapToObj(offer ->
                new OfferDecisionQueries.OfferView(offer + 3, offer, offer + 4, java.util.stream.IntStream.rangeClosed(1, 4)
                        .mapToObj(position -> new CurationRequest.RequirementSnapshot(position, "RANDOM", 1L, null,
                                "CODE_" + offer + "_" + position, "Zutat " + offer + "." + position,
                                "SPECIFIC", 1, "{}", "{}", "[]")).toList())).toList();
        return new OfferDecisionQueries.OfferSetView(1, 2, 3, offerCount, CurationModel.OfferSetStatus.CURATED_UNPRESENTED,
                Instant.now(), null, null, null, offers);
    }

    private static OfferDecisionQueries.OfferSetView confirmedOfferSet() {
        OfferDecisionQueries.OfferSetView unpresented = offerSet();
        OfferDecisionQueries.OfferView offer = unpresented.offers().getFirst();
        return new OfferDecisionQueries.OfferSetView(1, 2, 3, 1, CurationModel.OfferSetStatus.CONFIRMED, Instant.now(),
                Instant.now(), Instant.now(), new OfferDecisionQueries.ChallengeView(12, offer.offerId(), offer.candidateId(),
                Instant.now(), "CONFIRMED"), unpresented.offers());
    }

    private static SelectionVotingQueries.SelectionView selection(OfferDecisionQueries.OfferSetView set) {
        var member = new SelectionVotingQueries.ElectorateMemberView(6, "GEORGIA", "Georgia", true);
        var round = new SelectionVotingQueries.VotingRoundView(7, 1, 3, SelectionVotingQueries.VotingRoundStatus.OPEN,
                List.of(new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.ACCEPT, null)),
                List.of(new SelectionVotingQueries.VoteStatusView(6, "GEORGIA", "Georgia", false, null)), null);
        return new SelectionVotingQueries.SelectionView(1, List.of(member), set, round, List.of(), null, null);
    }
}
