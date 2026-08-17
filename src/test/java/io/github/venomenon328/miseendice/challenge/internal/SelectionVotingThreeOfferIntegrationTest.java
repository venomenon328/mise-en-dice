package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class,
        SelectionVotingIntegrationTest.SelectionVotingTestConfiguration.class
})
@Testcontainers
class SelectionVotingThreeOfferIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_selection_voting_three_offers")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("mise-en-dice.curation.openai.request-timeout", () -> "PT1S");
        registry.add("mise-en-dice.curation.openai.recovery-window", () -> "PT1S");
    }

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
        assertThat(completed.confirmedChallenge().participants()).hasSize(2);
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
        assertThat(completed.confirmedChallenge().participants()).hasSize(2);
        assertThat(offerDecisionQueries.findOfferSet(rerolledOfferSetId).orElseThrow()
                .confirmedChallenge().offerId()).isEqualTo(winner);
    }

    private OfferDecisionQueries.OfferSetView offered(int count, long seed) {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));
        Generated generated = (Generated) generationCommands.startNewSession(
                new StartNewSession(DATE, List.of(), seed, count));
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(CurationOrchestrationCommands.OfferReady.class);
        return offerDecisionQueries.findOfferSet(curationQueries.findOfferSet(generated.attemptId())
                .orElseThrow().offerSetId()).orElseThrow();
    }

    private Map<String, Long> participants() {
        return jdbcTemplate.query("select code, id from participant where code in ('GEORGIA', 'TOBIAS')",
                (result, row) -> Map.entry(result.getString("code"), result.getLong("id"))).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
