package io.github.venomenon328.miseendice.discord.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiscordChallengeRendererTest {

    @Test
    void rendersOneTwoAndThreeOffersFromStoredRequirementAndRestrictionSnapshots() {
        var renderer = new DiscordChallengeRenderer();
        for (int count = 1; count <= 3; count++) {
            var rendered = renderer.unpresentedOffers(offerSet(count));
            assertThat(rendered.content()).contains("Vorschlag " + count, "1. Snapshot " + count + ".1");
            assertThat(rendered.components()).hasSize(1);
        }

        assertThat(renderer.offers(offerSet(3)).content())
                .contains("1️⃣ **Vorschlag 1**", "2️⃣ **Vorschlag 2**", "3️⃣ **Vorschlag 3**",
                        "Einschränkung: Keine", "Einschränkung: Kein Kochalkohol", "Einschränkung: Keine Rohkost");
    }

    @Test
    void keepsVotesSecretWhileTheRoundIsOpen() {
        var renderer = new DiscordChallengeRenderer();
        var offerSet = offerSet(1);
        var selection = new SelectionVotingQueries.SelectionView(1, electorate(), offerSet,
                new SelectionVotingQueries.VotingRoundView(7, 1, offerSet.offerSetId(),
                        SelectionVotingQueries.VotingRoundStatus.OPEN,
                        List.of(new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.ACCEPT, null),
                                new SelectionVotingQueries.AllowedOptionView(SelectionVotingCommands.VoteOptionType.REROLL, null)),
                        List.of(new SelectionVotingQueries.VoteStatusView(8, "GEORGIA", "Georgia", true,
                                        SelectionVotingCommands.VoteChoice.accept()),
                                new SelectionVotingQueries.VoteStatusView(9, "TOBIAS", "Tobias", false, null)), null),
                List.of(), null, null);

        var rendered = renderer.selection(selection);

        assertThat(rendered.content()).contains("🗳️ **Abstimmung – Runde 1**", "Georgia: abgestimmt", "Tobias: noch offen")
                .doesNotContain("Georgia: Annehmen", "Georgia: Neu würfeln");
        assertThat(rendered.components()).extracting(DiscordChallengeRenderer.Component::label)
                .containsExactly("Annehmen", "Neu würfeln");
    }

    @Test
    void rendersCompletedResultTieBreakAndConfirmedSnapshotChallenge() {
        var renderer = new DiscordChallengeRenderer();
        var offerSet = offerSet(2, 2);
        var result = new SelectionVotingQueries.RoundResultView(SelectionVotingCommands.VoteChoice.offer(offerSet.offers().get(1).offerId()),
                true, Instant.now(), SelectionVotingQueries.ApplyState.CONFIRMED, null, null);
        var completed = new SelectionVotingQueries.VotingRoundView(7, 1, offerSet.offerSetId(),
                SelectionVotingQueries.VotingRoundStatus.COMPLETED, List.of(),
                List.of(new SelectionVotingQueries.VoteStatusView(8, "GEORGIA", "Georgia", true,
                                SelectionVotingCommands.VoteChoice.offer(offerSet.offers().get(1).offerId())),
                        new SelectionVotingQueries.VoteStatusView(9, "TOBIAS", "Tobias", true,
                                SelectionVotingCommands.VoteChoice.offer(offerSet.offers().get(0).offerId()))), result);
        var challenge = offerSet.confirmedChallenge();

        var rendered = renderer.selection(new SelectionVotingQueries.SelectionView(1, electorate(), offerSet, null,
                List.of(completed), null, challenge));

        assertThat(rendered.content()).contains("🗳️ **Abstimmung abgeschlossen**", "🏆 **Gewinner: Vorschlag 2**",
                "**Einzelstimmen**", "- Georgia: Vorschlag 2", "Tobias: Vorschlag 1",
                "✅ **Challenge bestätigt: Vorschlag 2**");
        String confirmedChallenge = rendered.content().substring(rendered.content().indexOf("**Challenge bestätigt"));
        assertThat(confirmedChallenge).contains("Snapshot 2.1", "Snapshot 2.4", "Einschränkung: Kein Kochalkohol")
                .doesNotContain("Snapshot 1.1", "Einschränkung: Keine\n");
    }

    @Test
    void rendersTerminalRerollStatesWithoutInventingAFallback() {
        var renderer = new DiscordChallengeRenderer();
        var offerSet = offerSet(2);
        var result = new SelectionVotingQueries.RoundResultView(SelectionVotingCommands.VoteChoice.reroll(), false,
                Instant.now(), SelectionVotingQueries.ApplyState.REROLL_EXHAUSTED, null, null);
        var completed = new SelectionVotingQueries.VotingRoundView(7, 1, offerSet.offerSetId(),
                SelectionVotingQueries.VotingRoundStatus.COMPLETED, List.of(), List.of(), result);

        var rendered = renderer.selection(new SelectionVotingQueries.SelectionView(1, electorate(), offerSet, null,
                List.of(completed), null, null));

        assertThat(rendered.content()).contains("🏆 **Gewinner: Neu würfeln**", "keine neuen Angebote");
        assertThat(rendered.components()).isEmpty();
    }

    @Test
    void usesResolvedDiscordMemberNamesOnlyForVisibleVotePresentation() {
        var renderer = new DiscordChallengeRenderer();
        var offerSet = offerSet(1);
        var selection = new SelectionVotingQueries.SelectionView(1, electorate(), offerSet,
                new SelectionVotingQueries.VotingRoundView(7, 1, offerSet.offerSetId(),
                        SelectionVotingQueries.VotingRoundStatus.OPEN, List.of(),
                        List.of(new SelectionVotingQueries.VoteStatusView(8, "GEORGIA", "Georgia", true, null),
                                new SelectionVotingQueries.VoteStatusView(9, "TOBIAS", "Tobias", false, null)), null),
                List.of(), null, null);

        var rendered = renderer.selection(selection, (participantId, storedFallback) ->
                participantId == 8 ? "Georgia Jetzt" : "Tobias Jetzt");

        assertThat(rendered.content()).contains("Georgia Jetzt: abgestimmt", "Tobias Jetzt: noch offen")
                .doesNotContain("Georgia: abgestimmt", "Tobias: noch offen");
    }

    @Test
    void rendersCuratorUnavailabilityAsAnInitialContinuationInsteadOfATerminalFailure() {
        var renderer = new DiscordChallengeRenderer();

        var rendered = renderer.preparation(new io.github.venomenon328.miseendice.challenge.api.ChallengeOfferPreparationCommands.InProgress(
                1, 2, "CURATION", "CURATOR_ADAPTER_DISABLED"));

        assertThat(rendered.content()).contains("Kuration gerade nicht erreichbar", "fortsetzen");
        assertThat(rendered.components()).containsExactly(new DiscordChallengeRenderer.Component("Fortsetzen",
                DiscordComponentId.initialContinue(1, 2)));
    }

    private static List<SelectionVotingQueries.ElectorateMemberView> electorate() {
        return List.of(new SelectionVotingQueries.ElectorateMemberView(8, "GEORGIA", "Georgia", true),
                new SelectionVotingQueries.ElectorateMemberView(9, "TOBIAS", "Tobias", true));
    }

    private static OfferDecisionQueries.OfferSetView offerSet(int count) {
        return offerSet(count, null);
    }

    private static OfferDecisionQueries.OfferSetView offerSet(int count, Integer confirmedPosition) {
        List<OfferDecisionQueries.OfferView> offers = java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(offer -> new OfferDecisionQueries.OfferView(offer, offer, offer + 20,
                        java.util.stream.IntStream.rangeClosed(1, 4).mapToObj(position ->
                                new CurationRequest.RequirementSnapshot(position, "RANDOM", 1L, null, "CODE",
                                        "Snapshot " + offer + "." + position, "SPECIFIC", 1, "{}", "{}", "[]"))
                                .toList(), restriction(offer))).toList();
        OfferDecisionQueries.ChallengeView confirmed = confirmedPosition == null ? null
                : new OfferDecisionQueries.ChallengeView(12, offers.get(confirmedPosition - 1).offerId(),
                offers.get(confirmedPosition - 1).candidateId(), Instant.now(), "CONFIRMED");
        return new OfferDecisionQueries.OfferSetView(1, 2, 3, count,
                confirmed == null ? CurationModel.OfferSetStatus.CURATED_UNPRESENTED : CurationModel.OfferSetStatus.CONFIRMED,
                Instant.now(), null, null, confirmed, offers);
    }

    private static CandidateProposalEngine.CandidateRestriction restriction(int offerPosition) {
        return switch (offerPosition) {
            case 1 -> CandidateProposalEngine.CandidateRestriction.none();
            case 2 -> new CandidateProposalEngine.CandidateRestriction(101L, "NO_COOKING_ALCOHOL", "Kein Kochalkohol");
            case 3 -> new CandidateProposalEngine.CandidateRestriction(102L, "NO_RAW_FOOD", "Keine Rohkost");
            default -> throw new IllegalArgumentException("Unsupported test offer position");
        };
    }
}
