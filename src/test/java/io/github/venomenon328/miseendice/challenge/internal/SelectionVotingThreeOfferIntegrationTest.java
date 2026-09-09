package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class,
        SelectionVotingIntegrationTest.SelectionVotingTestConfiguration.class
})
class SelectionVotingThreeOfferIntegrationTest extends CurrentSchemaPostgresIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Autowired GenerationCommands generationCommands;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionQueries offerDecisionQueries;
    @Autowired SelectionVotingCommands voting;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanData() {
        curator.reset();
        jdbcTemplate.execute("truncate table challenge_participation, selection_vote, selection_voting_round, "
                + "selection_electorate, participant_external_identity, reroll_offer_exposure_requirement, "
                + "reroll_offer_exposure, challenge, curated_offer_set, curation_round, generation_batch, "
                + "generation_attempt, challenge_session cascade");
    }

    @Test
    void firstRoundWithThreeOffersExposesAllOffersPlusRerollAndConfirmsTheWinner() {
        OfferDecisionQueries.OfferSetView ready = offered(3, 81_000_018L);
        Map<String, Long> participants = participants();

        SelectionVotingQueries.SelectionView open = voting.presentationSucceeded(
                new SelectionVotingCommands.PresentationSucceeded(ready.sessionId(), ready.offerSetId()));

        assertThat(open.currentRound().roundNumber()).isEqualTo(1);
        assertThat(open.currentRound().allowedOptions()).hasSize(4);
        assertThat(open.currentRound().allowedOptions())
                .filteredOn(option -> option.type() == SelectionVotingCommands.VoteOptionType.OFFER)
                .extracting(SelectionVotingQueries.AllowedOptionView::offerId)
                .containsExactlyElementsOf(ready.offers().stream()
                        .map(OfferDecisionQueries.OfferView::offerId).toList());
        assertThat(open.currentRound().allowedOptions())
                .filteredOn(option -> option.type() == SelectionVotingCommands.VoteOptionType.REROLL)
                .singleElement()
                .satisfies(option -> assertThat(option.offerId()).isNull());

        long winner = ready.offers().get(2).offerId();
        voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.offer(winner)));
        SelectionVotingQueries.SelectionView completed = voting.castVote(new SelectionVotingCommands.CastVote(
                ready.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.offer(winner)));

        assertThat(completed.currentRound()).isNull();
        assertThat(completed.completedRounds()).singleElement().satisfies(round -> {
            assertThat(round.roundNumber()).isEqualTo(1);
            assertThat(round.result().winningChoice()).isEqualTo(SelectionVotingCommands.VoteChoice.offer(winner));
        });
        assertNoNewParticipation(completed.confirmedChallenge().challengeId());
        assertThat(offerDecisionQueries.findOfferSet(ready.offerSetId()).orElseThrow()
                .confirmedChallenge().offerId()).isEqualTo(winner);
    }

    @Test
    void rerolledThreeOfferSetOpensSecondRoundWithOnlyThreeOffersAndConfirmsTheWinner() {
        OfferDecisionQueries.OfferSetView initial = offered(3, 81_000_019L);
        Map<String, Long> participants = participants();
        curator.script(CurationOrchestrationIntegrationTest.Script.success(3));
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(
                initial.sessionId(), initial.offerSetId()));

        voting.castVote(new SelectionVotingCommands.CastVote(initial.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.reroll()));
        SelectionVotingQueries.SelectionView waiting = voting.castVote(new SelectionVotingCommands.CastVote(
                initial.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.reroll()));

        assertThat(waiting.waitingForPresentation()).isNotNull();
        assertThat(waiting.waitingForPresentation().offerCount()).isEqualTo(3);
        long rerolledOfferSetId = waiting.waitingForPresentation().offerSetId();
        OfferDecisionQueries.OfferSetView rerolled = offerDecisionQueries.findOfferSet(rerolledOfferSetId).orElseThrow();
        assertThat(rerolled.offers()).hasSize(3);

        SelectionVotingQueries.SelectionView secondRound = voting.presentationSucceeded(
                new SelectionVotingCommands.PresentationSucceeded(initial.sessionId(), rerolledOfferSetId));

        assertThat(secondRound.currentRound().roundNumber()).isEqualTo(2);
        assertThat(secondRound.currentRound().allowedOptions()).hasSize(3)
                .allSatisfy(option -> assertThat(option.type()).isEqualTo(SelectionVotingCommands.VoteOptionType.OFFER));
        assertThat(secondRound.currentRound().allowedOptions())
                .extracting(SelectionVotingQueries.AllowedOptionView::offerId)
                .containsExactlyElementsOf(rerolled.offers().stream()
                        .map(OfferDecisionQueries.OfferView::offerId).toList());

        long winner = rerolled.offers().get(2).offerId();
        voting.castVote(new SelectionVotingCommands.CastVote(initial.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.offer(winner)));
        SelectionVotingQueries.SelectionView completed = voting.castVote(new SelectionVotingCommands.CastVote(
                initial.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.offer(winner)));

        assertThat(completed.currentRound()).isNull();
        assertThat(completed.completedRounds())
                .filteredOn(round -> round.roundNumber() == 2)
                .singleElement()
                .satisfies(round -> assertThat(round.result().winningChoice())
                        .isEqualTo(SelectionVotingCommands.VoteChoice.offer(winner)));
        assertNoNewParticipation(completed.confirmedChallenge().challengeId());
        assertThat(offerDecisionQueries.findOfferSet(rerolledOfferSetId).orElseThrow()
                .confirmedChallenge().offerId()).isEqualTo(winner);
    }

    private OfferDecisionQueries.OfferSetView offered(int count, long seed) {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));
        Generated generated = (Generated) generationCommands.startNewSession(
                new StartNewSession(DATE, List.of(), seed, count, RestrictionMode.AUTO));
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(CurationOrchestrationCommands.OfferReady.class);
        return offerDecisionQueries.findOfferSet(curationQueries.findOfferSet(generated.attemptId())
                .orElseThrow().offerSetId()).orElseThrow();
    }

    private Map<String, Long> participants() {
        return jdbcTemplate.query("select code, id from participant where code in ('GEORGIA', 'TOBIAS')",
                (result, row) -> Map.entry(result.getString("code"), result.getLong("id"))).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void assertNoNewParticipation(long challengeId) {
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge_participation where challenge_id = ?",
                Integer.class, challengeId)).isZero();
    }
}
