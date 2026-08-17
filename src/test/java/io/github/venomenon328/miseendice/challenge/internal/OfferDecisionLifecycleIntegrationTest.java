package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.OfferReady;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.ManualRequirementInput;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands.Confirmation;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands.RerollOfferReady;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionConflictException;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class
})
@Testcontainers
class OfferDecisionLifecycleIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_offer_decision")
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
    @Autowired GenerationQueries generationQueries;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionCommands decisions;
    @Autowired OfferDecisionQueries decisionQueries;
    @Autowired JdbcGenerationRepository generationRepository;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;
    @Autowired CurationOrchestrationIntegrationTest.SwitchableCandidateSetEngine generator;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanData() {
        curator.reset();
        generator.reset();
        jdbcTemplate.execute("truncate table reroll_offer_exposure_requirement, reroll_offer_exposure, challenge, "
                + "curated_offer_set, curation_round, generation_batch, generation_attempt, challenge_session cascade");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void presentPersistsOneStableSnapshotPerOfferedCandidateAndIsIdempotent(int count) {
        CurationQueries.OfferSetView ready = offered(count, 76_100_000L + count);

        OfferDecisionCommands.Presentation first = decisions.present(
                new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        OfferDecisionCommands.Presentation repeated = decisions.present(
                new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        OfferDecisionQueries.OfferSetView view = decisionQueries.findOfferSet(ready.offerSetId()).orElseThrow();

        assertThat(first.presentedAt()).isEqualTo(repeated.presentedAt()).isNotNull();
        assertThat(view.status().name()).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(view.offers()).hasSize(count).allSatisfy(offer -> {
            assertThat(offer.offerId()).isPositive();
            assertThat(offer.requirements()).hasSize(4);
        });
        assertThat(jdbcTemplate.queryForObject("select count(*) from curated_offer_set "
                + "where id = ? and presented_at is not null", Integer.class, ready.offerSetId())).isEqualTo(1);
    }

    @Test
    void confirmUsesOnlyTheAuthoritativeOfferAndLeavesOtherVisibleOffersHistoricallyInvisible() {
        CurationQueries.OfferSetView ready = offered(2, 76_100_011L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        long selectedOffer = ready.offers().getFirst().offerId();

        Confirmation confirmed = decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), selectedOffer));
        Confirmation repeated = decisions.confirm(new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), selectedOffer));

        assertThat(repeated.challengeId()).isEqualTo(confirmed.challengeId());
        assertThat(jdbcTemplate.queryForObject("select curated_offer_id from challenge where id = ?", Long.class,
                confirmed.challengeId())).isEqualTo(selectedOffer);
        assertThat(generationRepository.visibleHistory().challengesNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().challengesNewestFirst().getFirst().requirements())
                .extracting(requirement -> requirement.conceptCode())
                .containsExactlyElementsOf(ready.offers().getFirst().candidate().requirements().stream()
                        .map(requirement -> requirement.conceptCodeSnapshot()).toList());
        assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                ready.offerSetId(), ready.offers().get(1).offerId())))
                .isInstanceOf(OfferDecisionConflictException.class);
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)).isEqualTo(1);
    }

    @Test
    void confirmRejectsForeignOfferInApplicationAndPostgres() {
        CurationQueries.OfferSetView first = offered(1, 76_100_021L);
        CurationQueries.OfferSetView second = offered(1, 76_100_022L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(first.offerSetId()));

        assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                first.offerSetId(), second.offers().getFirst().offerId())))
                .isInstanceOf(OfferDecisionConflictException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into challenge (generation_attempt_id, selected_candidate_id, curated_offer_id)
                values (?, ?, ?)
                """, first.attemptId(), first.offers().getFirst().candidateId(), second.offers().getFirst().offerId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void rerollMaterializesOneExactCooldownExposureAndContinuesTheExistingWorkflow() {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2),
                CurationOrchestrationIntegrationTest.Script.success(2));
        Generated initial = (Generated) generationCommands.startNewSession(new StartNewSession(
                DATE, List.of(new ManualRequirementInput(1, "manual text", null)), 76_100_031L, 2));
        CurationQueries.OfferSetView source = offerReady(initial);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(source.offerSetId()));

        RerollOfferReady outcome = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 76_100_032L));
        OfferDecisionQueries.RerollExposureView exposure = decisionQueries.findRerollExposure(source.offerSetId())
                .orElseThrow();
        GenerationQueries.ContextView rerollContext = generationQueries.findContext(outcome.rerollAttemptId()).orElseThrow();

        assertThat(exposure.requirements()).hasSize(8);
        assertThat(exposure.requirements()).extracting(OfferDecisionQueries.ExposedRequirementView::conceptCodeSnapshot)
                .containsExactlyElementsOf(source.offers().stream().flatMap(offer -> offer.candidate().requirements().stream())
                        .map(requirement -> requirement.conceptCodeSnapshot()).toList());
        assertThat(generationRepository.visibleHistory().challengesNewestFirst()).isEmpty();
        assertThat(generationRepository.visibleHistory().rerollExposuresNewestFirst()).hasSize(1);
        assertThat(generationRepository.visibleHistory().cooldownExposuresNewestFirst()).hasSize(1);
        assertThat(rerollContext.visibleHistorySnapshotJson()).contains("rerollExposuresNewestFirst");
        assertThat(curationQueries.findAttempt(outcome.rerollAttemptId()).orElseThrow().requestedOfferCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList("""
                select position || ':' || display_text || ':' || coalesce(matched_ingredient_concept_id::text, '')
                from generation_manual_requirement where generation_attempt_id in (?, ?)
                order by generation_attempt_id, position
                """, String.class, initial.attemptId(), outcome.rerollAttemptId()))
                .containsExactly("1:manual text:", "1:manual text:");

        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(source.offerSetId(), 999L));
        assertThat(resumed.rerollAttemptId()).isEqualTo(outcome.rerollAttemptId());
        assertThat(resumed.offerSetId()).isEqualTo(outcome.offerSetId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentConfirmAndRerollLeaveExactlyOneTerminalDecision() throws Exception {
        CurationQueries.OfferSetView ready = offered(2, 76_100_041L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var confirm = executor.submit(raced(readyLatch, start, () -> decisions.confirm(
                    new OfferDecisionCommands.ConfirmOffer(ready.offerSetId(), ready.offers().getFirst().offerId()))));
            var reroll = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_042L))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = List.of(confirm.get(), reroll.get());
            assertThat(results.stream().filter(value -> !(value instanceof OfferDecisionConflictException)).count())
                    .isEqualTo(1);
        }
        String status = jdbcTemplate.queryForObject("select status from curated_offer_set where id = ?", String.class,
                ready.offerSetId());
        assertThat(status).isIn("CONFIRMED", "REROLLED");
        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge", Integer.class)
                + jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
    }

    @Test
    void concurrentRerollsPersistOneExposureAndResumeOneAttempt() throws Exception {
        CurationQueries.OfferSetView ready = offered(2, 76_100_045L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(ready.offerSetId()));
        curator.script(CurationOrchestrationIntegrationTest.Script.success(2));
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_046L))));
            var second = executor.submit(raced(readyLatch, start, () -> decisions.reroll(
                    new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_047L))));
            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> results = List.of(first.get(), second.get());
            assertThat(results).allMatch(OfferDecisionCommands.RerollOutcome.class::isInstance);
            assertThat(results.stream().map(OfferDecisionCommands.RerollOutcome.class::cast)
                    .map(OfferDecisionCommands.RerollOutcome::rerollAttemptId).distinct()).hasSize(1);
        }
        RerollOfferReady resumed = (RerollOfferReady) decisions.reroll(
                new OfferDecisionCommands.RerollOfferSet(ready.offerSetId(), 76_100_048L));
        assertThat(resumed.sourceOfferSetId()).isEqualTo(ready.offerSetId());
        assertThat(jdbcTemplate.queryForObject("select count(*) from reroll_offer_exposure", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from generation_attempt where attempt_type = 'REROLL'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void failedChallengeOrExposureWriteRollsTheDecisionBackWithoutMaskingTheDatabaseFailure() {
        CurationQueries.OfferSetView confirmation = offered(1, 76_100_051L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(confirmation.offerSetId()));
        jdbcTemplate.execute("""
                create function reject_offer_decision_challenge() returns trigger language plpgsql as $$
                begin raise exception 'test challenge rejection'; end; $$
                """);
        jdbcTemplate.execute("create trigger trg_test_reject_offer_decision_challenge before insert on challenge "
                + "for each row execute function reject_offer_decision_challenge()");
        try {
            assertThatThrownBy(() -> decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                    confirmation.offerSetId(), confirmation.offers().getFirst().offerId())))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_offer_decision_challenge on challenge");
            jdbcTemplate.execute("drop function reject_offer_decision_challenge()");
        }
        assertThat(status(confirmation.offerSetId())).isEqualTo("PRESENTED_PENDING_DECISION");

        CurationQueries.OfferSetView reroll = offered(1, 76_100_052L);
        decisions.present(new OfferDecisionCommands.PresentOfferSet(reroll.offerSetId()));
        jdbcTemplate.execute("""
                create function reject_offer_exposure_requirement() returns trigger language plpgsql as $$
                begin raise exception 'test exposure rejection'; end; $$
                """);
        jdbcTemplate.execute("create trigger trg_test_reject_offer_exposure_requirement before insert "
                + "on reroll_offer_exposure_requirement for each row execute function reject_offer_exposure_requirement()");
        try {
            assertThatThrownBy(() -> decisions.reroll(new OfferDecisionCommands.RerollOfferSet(reroll.offerSetId())))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_offer_exposure_requirement "
                    + "on reroll_offer_exposure_requirement");
            jdbcTemplate.execute("drop function reject_offer_exposure_requirement()");
        }
        assertThat(status(reroll.offerSetId())).isEqualTo("PRESENTED_PENDING_DECISION");
        assertThat(decisionQueries.findRerollExposure(reroll.offerSetId())).isEmpty();
    }

    private CurationQueries.OfferSetView offered(int count, long seed) {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(count));
        Generated generated = (Generated) generationCommands.startNewSession(new StartNewSession(DATE, List.of(), seed, count));
        return offerReady(generated);
    }

    private CurationQueries.OfferSetView offerReady(Generated generated) {
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        return curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
    }

    private String status(long offerSetId) {
        return jdbcTemplate.queryForObject("select status from curated_offer_set where id = ?", String.class, offerSetId);
    }

    private static <T> Callable<Object> raced(CountDownLatch ready, CountDownLatch start, Callable<T> action) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Concurrent start was not released");
            }
            try {
                return action.call();
            } catch (OfferDecisionConflictException exception) {
                return exception;
            }
        };
    }
}
