package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingConflictException;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
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
class SelectionVotingIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_selection_voting")
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
    @Autowired SelectionVotingQueries votingQueries;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;
    @Autowired FixedTieBreakRandom tieBreakRandom;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanData() {
        curator.reset();
        tieBreakRandom.reset(0);
        jdbcTemplate.execute("truncate table challenge_participation, selection_vote, selection_voting_round, "
                + "selection_electorate, participant_external_identity, reroll_offer_exposure_requirement, "
                + "reroll_offer_exposure, challenge, curated_offer_set, curation_round, generation_batch, "
                + "generation_attempt, challenge_session cascade");
    }

    @Test
    void presentationHandshakeKeepsVotesSecretUntilThePersistedMajorityResult() {
        OfferDecisionQueries.OfferSetView ready = offered(2, 81_000_001L);
        Map<String, Long> participants = participants();

        SelectionVotingQueries.SelectionView initialized = voting.initialize(
                new SelectionVotingCommands.InitializeSelection(ready.sessionId()));
        assertThat(initialized.currentRound()).isNull();
        assertThat(offerDecisionQueries.findOfferSet(ready.offerSetId()).orElseThrow().status().name())
                .isEqualTo("CURATED_UNPRESENTED");

        SelectionVotingQueries.SelectionView open = voting.presentationSucceeded(
                new SelectionVotingCommands.PresentationSucceeded(ready.sessionId(), ready.offerSetId()));
        assertThat(open.currentRound().roundNumber()).isEqualTo(1);
        assertThat(open.currentRound().allowedOptions()).hasSize(3);

        voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.offer(ready.offers().getFirst().offerId())));
        SelectionVotingQueries.SelectionView hidden = votingQueries.findSelection(ready.sessionId()).orElseThrow();
        assertThat(hidden.currentRound().votes()).allSatisfy(vote -> assertThat(vote.vote()).isNull());
        assertThat(hidden.currentRound().votes()).filteredOn(SelectionVotingQueries.VoteStatusView::hasVoted).hasSize(1);

        long winner = ready.offers().get(1).offerId();
        voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.offer(winner)));
        SelectionVotingQueries.SelectionView completed = voting.castVote(new SelectionVotingCommands.CastVote(
                ready.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.offer(winner)));

        assertThat(completed.currentRound()).isNull();
        assertThat(completed.completedRounds()).singleElement().satisfies(round -> {
            assertThat(round.result().winningChoice()).isEqualTo(SelectionVotingCommands.VoteChoice.offer(winner));
            assertThat(round.result().tieBreakUsed()).isFalse();
            assertThat(round.votes()).allSatisfy(vote -> assertThat(vote.vote()).isEqualTo(
                    SelectionVotingCommands.VoteChoice.offer(winner)));
        });
        assertThat(completed.confirmedChallenge().participants()).hasSize(2);
        assertThat(tieBreakRandom.calls()).isZero();
        assertThatThrownBy(() -> voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(),
                participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.offer(winner))))
                .isInstanceOf(SelectionVotingConflictException.class);
    }

    @Test
    void topTieIsPersistedOnceAcrossRepeatedResumeAttempts() {
        OfferDecisionQueries.OfferSetView ready = offered(2, 81_000_002L);
        Map<String, Long> participants = participants();
        tieBreakRandom.reset(1);
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(ready.sessionId(), ready.offerSetId()));

        voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.offer(ready.offers().getFirst().offerId())));
        SelectionVotingQueries.SelectionView completed = voting.castVote(new SelectionVotingCommands.CastVote(
                ready.sessionId(), participants.get("TOBIAS"),
                SelectionVotingCommands.VoteChoice.offer(ready.offers().get(1).offerId())));

        assertThat(completed.completedRounds()).singleElement().satisfies(round -> {
            assertThat(round.result().winningChoice())
                    .isEqualTo(SelectionVotingCommands.VoteChoice.offer(ready.offers().get(1).offerId()));
            assertThat(round.result().tieBreakUsed()).isTrue();
        });
        voting.resume(new SelectionVotingCommands.ResumeSelection(ready.sessionId()));
        voting.resume(new SelectionVotingCommands.ResumeSelection(ready.sessionId()));
        assertThat(tieBreakRandom.calls()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)).isEqualTo(1);
    }

    @Test
    void rerollWaitsForReportedPresentationBeforeOpeningTheSecondRound() {
        OfferDecisionQueries.OfferSetView initial = offered(2, 81_000_003L);
        Map<String, Long> participants = participants();
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(initial.sessionId(), initial.offerSetId()));

        voting.castVote(new SelectionVotingCommands.CastVote(initial.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.reroll()));
        SelectionVotingQueries.SelectionView waiting = voting.castVote(new SelectionVotingCommands.CastVote(
                initial.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.reroll()));

        assertThat(waiting.currentRound()).isNull();
        assertThat(waiting.waitingForPresentation()).isNotNull();
        long rerolledOfferSetId = waiting.waitingForPresentation().offerSetId();
        assertThat(offerDecisionQueries.findOfferSet(rerolledOfferSetId).orElseThrow().status().name())
                .isEqualTo("CURATED_UNPRESENTED");
        assertThat(waiting.completedRounds()).singleElement().satisfies(round ->
                assertThat(round.result().applyState()).isEqualTo(SelectionVotingQueries.ApplyState.REROLL_OFFER_READY));

        SelectionVotingQueries.SelectionView secondRound = voting.presentationSucceeded(
                new SelectionVotingCommands.PresentationSucceeded(initial.sessionId(), rerolledOfferSetId));
        assertThat(secondRound.currentRound().roundNumber()).isEqualTo(2);
        assertThat(secondRound.currentRound().allowedOptions())
                .noneMatch(option -> option.type() == SelectionVotingCommands.VoteOptionType.REROLL);
        assertThat(secondRound.electorate()).extracting(SelectionVotingQueries.ElectorateMemberView::participantId)
                .containsExactlyInAnyOrderElementsOf(waiting.electorate().stream()
                        .map(SelectionVotingQueries.ElectorateMemberView::participantId).toList());
    }

    @Test
    void rerolledSingleOfferAutoConfirmsOnlyAfterThePresentationHandshake() {
        OfferDecisionQueries.OfferSetView initial = offered(1, 81_000_004L);
        Map<String, Long> participants = participants();
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(initial.sessionId(), initial.offerSetId()));
        voting.castVote(new SelectionVotingCommands.CastVote(initial.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.reroll()));
        SelectionVotingQueries.SelectionView waiting = voting.castVote(new SelectionVotingCommands.CastVote(
                initial.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.reroll()));
        long rerolledOfferSetId = waiting.waitingForPresentation().offerSetId();

        SelectionVotingQueries.SelectionView confirmed = voting.presentationSucceeded(
                new SelectionVotingCommands.PresentationSucceeded(initial.sessionId(), rerolledOfferSetId));

        assertThat(confirmed.currentRound()).isNull();
        assertThat(confirmed.confirmedChallenge().participants()).hasSize(2);
        assertThat(confirmed.completedRounds()).singleElement().satisfies(round ->
                assertThat(round.result().applyState()).isEqualTo(SelectionVotingQueries.ApplyState.REROLL_AUTO_CONFIRMED));
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)).isEqualTo(1);
    }

    @Test
    void genericIdentityAndLateParticipationDoNotChangeTheElectorateOrAvailability() {
        OfferDecisionQueries.OfferSetView ready = offered(1, 81_000_005L);
        Map<String, Long> participants = participants();
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(ready.sessionId(), ready.offerSetId()));
        voting.castVote(new SelectionVotingCommands.CastVote(ready.sessionId(), participants.get("GEORGIA"),
                SelectionVotingCommands.VoteChoice.accept()));
        SelectionVotingQueries.SelectionView confirmed = voting.castVote(new SelectionVotingCommands.CastVote(
                ready.sessionId(), participants.get("TOBIAS"), SelectionVotingCommands.VoteChoice.accept()));
        long extraParticipant = jdbcTemplate.queryForObject("""
                insert into participant (code, display_name) values ('SELECTION_TEST_EXTRA', 'Selection Test Extra')
                returning id
                """, Long.class);

        SelectionVotingQueries.ParticipantIdentityView identity = voting.linkExternalIdentity(
                new SelectionVotingCommands.LinkExternalIdentity(extraParticipant, "test-provider", "external-extra"));
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into participant_external_identity (participant_id, provider, external_subject)
                values (?, 'test-provider', 'external-extra')
                """, participants.get("GEORGIA"))).isInstanceOf(DataAccessException.class);
        SelectionVotingQueries.ChallengeParticipantView joined = voting.joinChallenge(
                new SelectionVotingCommands.JoinChallenge(confirmed.confirmedChallenge().challengeId(), extraParticipant));

        assertThat(votingQueries.findParticipantByExternalIdentity("test-provider", "external-extra"))
                .contains(identity);
        assertThat(joined.participantId()).isEqualTo(extraParticipant);
        assertThat(votingQueries.findSelection(ready.sessionId()).orElseThrow().electorate()).hasSize(2);
        assertThat(votingQueries.findChallengeParticipation(confirmed.confirmedChallenge().challengeId()).orElseThrow()
                .participants()).hasSize(3);
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_availability where participant_id = ?",
                Integer.class, extraParticipant)).isZero();
    }

    @Test
    void twoLastVotesCompleteExactlyOneRoundAndOnePersistedTieBreak() throws Exception {
        OfferDecisionQueries.OfferSetView ready = offered(2, 81_000_006L);
        Map<String, Long> participants = participants();
        tieBreakRandom.reset(0);
        voting.presentationSucceeded(new SelectionVotingCommands.PresentationSucceeded(ready.sessionId(), ready.offerSetId()));
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(readyLatch, start, () -> voting.castVote(new SelectionVotingCommands.CastVote(
                    ready.sessionId(), participants.get("GEORGIA"),
                    SelectionVotingCommands.VoteChoice.offer(ready.offers().getFirst().offerId())))));
            var second = executor.submit(raced(readyLatch, start, () -> voting.castVote(new SelectionVotingCommands.CastVote(
                    ready.sessionId(), participants.get("TOBIAS"),
                    SelectionVotingCommands.VoteChoice.offer(ready.offers().get(1).offerId())))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(first.get()).isInstanceOf(SelectionVotingQueries.SelectionView.class);
            assertThat(second.get()).isInstanceOf(SelectionVotingQueries.SelectionView.class);
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from selection_voting_round where status = 'COMPLETED'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select tie_break_used from selection_voting_round", Boolean.class)).isTrue();
        assertThat(tieBreakRandom.calls()).isEqualTo(1);
    }

    private OfferDecisionQueries.OfferSetView offered(int count, long seed) {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));
        Generated generated = (Generated) generationCommands.startNewSession(new StartNewSession(DATE, List.of(), seed, count));
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(CurationOrchestrationCommands.OfferReady.class);
        return offerDecisionQueries.findOfferSet(curationQueries.findOfferSet(generated.attemptId()).orElseThrow().offerSetId())
                .orElseThrow();
    }

    private Map<String, Long> participants() {
        return jdbcTemplate.query("select code, id from participant where code in ('GEORGIA', 'TOBIAS')",
                (result, row) -> Map.entry(result.getString("code"), result.getLong("id"))).stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Callable<Object> raced(CountDownLatch ready, CountDownLatch start, Callable<?> action) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent start was not released");
            }
            return action.call();
        };
    }

    static final class FixedTieBreakRandom implements TieBreakRandomSource {
        private final AtomicInteger next = new AtomicInteger();
        private final AtomicInteger calls = new AtomicInteger();

        void reset(int value) {
            next.set(value);
            calls.set(0);
        }

        int calls() {
            return calls.get();
        }

        @Override
        public int nextInt(int bound) {
            calls.incrementAndGet();
            return Math.floorMod(next.get(), bound);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SelectionVotingTestConfiguration {
        @Bean
        @Primary
        FixedTieBreakRandom fixedTieBreakRandom() {
            return new FixedTieBreakRandom();
        }
    }
}
