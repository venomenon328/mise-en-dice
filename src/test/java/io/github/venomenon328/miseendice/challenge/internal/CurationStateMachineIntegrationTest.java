package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CandidateParticipation;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CuratedOfferSet;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.PlannedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationConflictException;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartExistingSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {MiseEnDiceApplication.class,
        CurationOfferLifecycleIntegrationTest.GeneratedBatchConfiguration.class})
@Testcontainers
class CurationStateMachineIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_curation_state")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GenerationCommands generationCommands;
    @Autowired GenerationQueries generationQueries;
    @Autowired CurationCommands curationCommands;
    @Autowired CurationQueries curationQueries;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanData() {
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from curated_offer_set");
        jdbcTemplate.update("delete from curation_round");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void terminalOfferAndExhaustionStatesCannotReopenOrRacePendingCompletion() {
        Generated offered = generated(1, 71_100_001L);
        PlannedRound offeredRound = planInitial(offered);
        assertThatThrownBy(() -> curationCommands.recordExhaustion(
                new CurationCommands.Exhaustion(offered.attemptId(), "NO_GOOD", null)))
                .isInstanceOf(CurationConflictException.class);
        curationCommands.completeRound(new CurationCommands.CompleteRound(offeredRound.roundId(), response(offeredRound, 1)));
        createOffers(offered, offeredRound, 1);

        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(
                offered.attemptId(), 2, offeredRound.request().primaryBatchId(),
                CurationModel.RequestPurpose.TECHNICAL_RETRY, "future-curator", "prompt-v1", 1,
                newParticipants(offeredRound)))).isInstanceOf(CurationConflictException.class);
        assertThatThrownBy(() -> curationCommands.recordExhaustion(
                new CurationCommands.Exhaustion(offered.attemptId(), "NO_GOOD", null)))
                .isInstanceOf(CurationConflictException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("update generation_attempt set curation_status = 'REQUEST_PENDING' where id = ?",
                offered.attemptId())).isInstanceOf(DataAccessException.class);

        Generated exhausted = generated(1, 71_100_002L);
        PlannedRound exhaustedRound = planInitial(exhausted);
        curationCommands.completeRound(new CurationCommands.CompleteRound(exhaustedRound.roundId(), response(exhaustedRound, 0)));
        CurationCommands.Exhaustion exhaustion = new CurationCommands.Exhaustion(exhausted.attemptId(), "NO_GOOD", "curator found none");
        curationCommands.recordExhaustion(exhaustion);
        assertThat(curationCommands.recordExhaustion(exhaustion).attemptId()).isEqualTo(exhausted.attemptId());
        assertThatThrownBy(() -> curationCommands.recordExhaustion(
                new CurationCommands.Exhaustion(exhausted.attemptId(), "DIFFERENT_REASON", null)))
                .isInstanceOf(CurationConflictException.class);
        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(
                exhausted.attemptId(), 2, exhaustedRound.request().primaryBatchId(),
                CurationModel.RequestPurpose.TECHNICAL_RETRY, "future-curator", "prompt-v1", 1,
                newParticipants(exhaustedRound)))).isInstanceOf(CurationConflictException.class);
    }

    @Test
    void concurrentCompletionIsIdempotentAndDifferentRetriesConflict() throws Exception {
        Generated generated = generated(1, 71_100_003L);
        PlannedRound planned = planInitial(generated);
        CurationResponse response = response(planned, 1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> curationCommands.completeRound(
                    new CurationCommands.CompleteRound(planned.roundId(), response)));
            var second = executor.submit(() -> curationCommands.completeRound(
                    new CurationCommands.CompleteRound(planned.roundId(), response)));
            assertThat(first.get().roundId()).isEqualTo(second.get().roundId());
        }
        CurationQueries.RoundView completed = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(completed.status()).isEqualTo(CurationModel.RoundStatus.COMPLETED);
        assertThat(completed.candidates()).allSatisfy(candidate -> assertThat(candidate.rank()).isNotNull());
        assertThatThrownBy(() -> curationCommands.completeRound(
                new CurationCommands.CompleteRound(planned.roundId(), response(planned, 0))))
                .isInstanceOf(CurationConflictException.class);
    }

    @Test
    void concurrentCompletionAndExhaustionCannotCommitConflictingAttemptStates() throws Exception {
        Generated generated = generated(1, 71_100_003_1L);
        PlannedRound planned = planInitial(generated);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var completion = executor.submit(() -> raced(ready, start, () -> curationCommands.completeRound(
                    new CurationCommands.CompleteRound(planned.roundId(), response(planned, 0)))));
            var exhaustion = executor.submit(() -> raced(ready, start, () -> curationCommands.recordExhaustion(
                    new CurationCommands.Exhaustion(generated.attemptId(), "NO_GOOD", null))));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Object> outcomes = List.of(completion.get(), exhaustion.get());
            assertThat(outcomes.stream().filter(value -> !(value instanceof CurationConflictException)).count())
                    .isGreaterThan(0);
        }

        CurationQueries.AttemptView attempt = curationQueries.findAttempt(generated.attemptId()).orElseThrow();
        assertThat(attempt.curationStatus()).isIn("RESPONSE_RECORDED", "EXHAUSTED");
        assertThat(curationQueries.findRoundById(planned.roundId()).orElseThrow().status())
                .isEqualTo(CurationModel.RoundStatus.COMPLETED);
        if ("EXHAUSTED".equals(attempt.curationStatus())) {
            assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(
                    generated.attemptId(), 2, planned.request().primaryBatchId(),
                    CurationModel.RequestPurpose.TECHNICAL_RETRY, "future-curator", "prompt-v1", 1,
                    newParticipants(planned)))).isInstanceOf(CurationConflictException.class);
        }
    }

    @Test
    void rawUndeserializablePayloadIsPersistedAsInvalidResponseAndCanOnlyRetryIdentically() {
        Generated generated = generated(1, 71_100_004L);
        PlannedRound planned = planInitial(generated);
        CurationCommands.InvalidResponsePayload invalid = new CurationCommands.InvalidResponsePayload(planned.roundId(),
                "{ definitely-not-json", "MALFORMED_CURATOR_RESPONSE", "parser rejected response");
        assertThat(curationCommands.recordInvalidResponse(invalid).roundId()).isEqualTo(planned.roundId());

        CurationQueries.RoundView stored = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(stored.status()).isEqualTo(CurationModel.RoundStatus.INVALID_RESPONSE);
        assertThat(stored.responsePayloadJson()).isNull();
        assertThat(stored.invalidResponseOriginalPayload()).isEqualTo("{ definitely-not-json");
        assertThat(curationCommands.recordInvalidResponse(invalid).roundId()).isEqualTo(planned.roundId());
        assertThatThrownBy(() -> curationCommands.recordInvalidResponse(new CurationCommands.InvalidResponsePayload(
                planned.roundId(), "different", "MALFORMED_CURATOR_RESPONSE", "parser rejected response")))
                .isInstanceOf(CurationConflictException.class);
    }

    @Test
    void validatesInitialAndFollowUpShapesWithoutSelectingFallbacks() {
        Generated generated = generated(2, 71_100_005L);
        GenerationQueries.BatchView batch = generationQueries.findBatch(generated.attemptId(), 1).orElseThrow();
        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(generated.attemptId(), 1,
                batch.batchId(), CurationModel.RequestPurpose.INITIAL_PASS, "future-curator", "prompt-v1", 2,
                newParticipants(batch.candidates().subList(0, 11))))).isInstanceOf(CurationConflictException.class);
        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(generated.attemptId(), 2,
                batch.batchId(), CurationModel.RequestPurpose.QUALITY_FOLLOW_UP, "future-curator", "prompt-v1", 1,
                newParticipants(batch.candidates())))).isInstanceOf(CurationConflictException.class);

        PlannedRound first = planInitial(generated);
        curationCommands.completeRound(new CurationCommands.CompleteRound(first.roundId(), response(first, 1)));
        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(generated.attemptId(), 2,
                first.request().primaryBatchId(), CurationModel.RequestPurpose.QUALITY_FOLLOW_UP,
                "future-curator", "prompt-v1", 1, newParticipants(first))))
                .isInstanceOf(CurationConflictException.class);
    }

    @Test
    void technicalRetryUsesTheSecondAndLastRoundForTheSameCompleteFirstBatch() {
        Generated generated = generated(2, 71_100_006L);
        PlannedRound first = planInitial(generated);
        curationCommands.recordTechnicalFailure(new CurationCommands.TechnicalFailure(first.roundId(), "TIMEOUT", null));
        PlannedRound retry = (PlannedRound) curationCommands.planRound(new CurationCommands.PlanRound(
                generated.attemptId(), 2, first.request().primaryBatchId(), CurationModel.RequestPurpose.TECHNICAL_RETRY,
                "future-curator", "prompt-v1", 2, newParticipants(first)));
        assertThat(retry.request().candidates()).hasSize(12);
        assertThat(retry.request().openOfferSlots()).isEqualTo(2);
        assertThatThrownBy(() -> new CurationCommands.PlanRound(generated.attemptId(), 3,
                first.request().primaryBatchId(), CurationModel.RequestPurpose.TECHNICAL_RETRY,
                "future-curator", "prompt-v1", 2, newParticipants(first)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void persistsOneTwoAndThreeOffersAndRejectsWrongCountsOrNoGood() {
        for (int requested : List.of(1, 2, 3)) {
            Generated generated = generated(requested, 71_100_010L + requested);
            PlannedRound planned = planInitial(generated);
            curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), classifiedResponse(planned)));
            CuratedOfferSet offerSet = createOffers(generated, planned, requested);
            CurationQueries.OfferSetView persisted = curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
            assertThat(offerSet.requestedOfferCount()).isEqualTo(requested);
            assertThat(curationQueries.findRoundById(planned.roundId()).orElseThrow().candidates())
                    .extracting(CurationQueries.RoundCandidateView::evaluation)
                    .contains(CurationModel.Evaluation.GOOD, CurationModel.Evaluation.ACCEPTABLE,
                            CurationModel.Evaluation.BAD);
            assertThat(persisted.offers()).extracting(CurationQueries.OfferView::position)
                    .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, requested).boxed().toList());
        }

        Generated two = generated(2, 71_100_020L);
        PlannedRound twoRound = planInitial(two);
        curationCommands.completeRound(new CurationCommands.CompleteRound(twoRound.roundId(), response(twoRound, 1)));
        assertThatThrownBy(() -> createOffers(two, twoRound, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createOffers(two, twoRound, 3)).isInstanceOf(IllegalArgumentException.class);

        Generated noGood = generated(1, 71_100_021L);
        PlannedRound noGoodRound = planInitial(noGood);
        curationCommands.completeRound(new CurationCommands.CompleteRound(noGoodRound.roundId(), response(noGoodRound, 0)));
        assertThatThrownBy(() -> createOffers(noGood, noGoodRound, 1))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(CurationConflictException.class);
        assertThat(curationQueries.findOfferSet(noGood.attemptId())).isEmpty();
        assertThat(curationQueries.findAttempt(noGood.attemptId()).orElseThrow().curationStatus())
                .isEqualTo("RESPONSE_RECORDED");
    }

    @Test
    void rollsBackRoundEvaluationsAndOfferSetWhenPostgresRejectsOneWrite() {
        Generated generated = generated(2, 71_100_030L);
        PlannedRound planned = planInitial(generated);
        jdbcTemplate.execute("""
                create function reject_second_curation_evaluation() returns trigger language plpgsql as $$
                begin
                    if new.evaluation_rank = 2 then raise exception 'test evaluation rejection'; end if;
                    return new;
                end;
                $$
                """);
        jdbcTemplate.execute("""
                create trigger trg_test_reject_second_curation_evaluation before update of evaluation_class
                on curation_round_candidate for each row execute function reject_second_curation_evaluation()
                """);
        try {
            assertThatThrownBy(() -> curationCommands.completeRound(
                    new CurationCommands.CompleteRound(planned.roundId(), response(planned, 1))))
                    .isInstanceOf(DataAccessException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_second_curation_evaluation on curation_round_candidate");
            jdbcTemplate.execute("drop function reject_second_curation_evaluation()");
        }
        CurationQueries.RoundView pending = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(pending.status()).isEqualTo(CurationModel.RoundStatus.PENDING);
        assertThat(pending.candidates()).allSatisfy(candidate -> assertThat(candidate.evaluation()).isNull());

        curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), response(planned, 1)));
        List<CurationQueries.RoundCandidateView> candidates = curationQueries.findRoundById(planned.roundId()).orElseThrow().candidates();
        assertThatThrownBy(() -> curationCommands.createOfferSet(new CurationCommands.CreateOfferSet(generated.attemptId(), List.of(
                new CurationCommands.OfferSelection(candidates.get(0).candidateId(), candidates.get(0).roundCandidateId()),
                new CurationCommands.OfferSelection(candidates.get(1).candidateId(), candidates.get(2).roundCandidateId())
        ), List.of("EXPLICIT_CURATOR_SELECTION")))).isInstanceOf(DataAccessException.class);
        assertThat(curationQueries.findOfferSet(generated.attemptId())).isEmpty();
        assertThat(curationQueries.findAttempt(generated.attemptId()).orElseThrow().curationStatus())
                .isEqualTo("RESPONSE_RECORDED");
    }

    @Test
    void foreignAttemptReferencesAndUnknownDatabaseFailuresRemainTechnical() {
        Generated first = generated(1, 71_100_040L);
        Generated second = generated(1, 71_100_041L);
        GenerationQueries.BatchView foreignBatch = generationQueries.findBatch(second.attemptId(), 1).orElseThrow();
        assertThatThrownBy(() -> curationCommands.planRound(new CurationCommands.PlanRound(first.attemptId(), 1,
                foreignBatch.batchId(), CurationModel.RequestPurpose.INITIAL_PASS, "future-curator", "prompt-v1", 1,
                newParticipants(foreignBatch.candidates())))).isInstanceOf(CurationConflictException.class);

        PlannedRound planned = planInitial(first);
        long foreignCandidateId = foreignBatch.candidates().getFirst().candidateId();
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into curation_round_candidate (
                    curation_round_id, challenge_candidate_id, request_position, participation_type
                ) values (?, ?, 13, 'NEW')
                """, planned.roundId(), foreignCandidateId)).isInstanceOf(DataAccessException.class);

        PlannedRound foreignRound = planInitial(second);
        curationCommands.completeRound(new CurationCommands.CompleteRound(foreignRound.roundId(), response(foreignRound, 1)));
        CurationQueries.RoundCandidateView foreignEvaluation = curationQueries.findRoundById(foreignRound.roundId())
                .orElseThrow().candidates().getFirst();
        assertThatThrownBy(() -> curationCommands.createOfferSet(new CurationCommands.CreateOfferSet(first.attemptId(),
                List.of(new CurationCommands.OfferSelection(foreignEvaluation.candidateId(),
                        foreignEvaluation.roundCandidateId())), List.of("EXPLICIT_CURATOR_SELECTION"))))
                .isInstanceOf(DataAccessException.class);
        assertThat(curationQueries.findOfferSet(first.attemptId())).isEmpty();

        jdbcTemplate.execute("""
                create function reject_attempt_curation_write() returns trigger language plpgsql as $$
                begin raise exception 'unexpected database failure'; end; $$
                """);
        jdbcTemplate.execute("""
                create trigger trg_test_reject_attempt_curation_write before update of curation_status
                on generation_attempt for each row execute function reject_attempt_curation_write()
                """);
        try {
            assertThatThrownBy(() -> curationCommands.completeRound(
                    new CurationCommands.CompleteRound(planned.roundId(), response(planned, 1))))
                    .isInstanceOf(DataAccessException.class)
                    .isNotInstanceOf(CurationConflictException.class);
        } finally {
            jdbcTemplate.execute("drop trigger trg_test_reject_attempt_curation_write on generation_attempt");
            jdbcTemplate.execute("drop function reject_attempt_curation_write()");
        }
        assertThat(curationQueries.findRoundById(planned.roundId()).orElseThrow().status())
                .isEqualTo(CurationModel.RoundStatus.PENDING);
    }

    @Test
    void rejectsEveryInvalidCandidateAndRankResponseShapeWithoutPartialEvaluations() {
        assertInvalidResponse(responseMutation -> {
            responseMutation.set(0, evaluation(responseMutation.get(0).candidateId() + 99_999, 1));
        });
        assertInvalidResponse(responseMutation -> {
            responseMutation.set(1, evaluation(responseMutation.get(0).candidateId(), 2));
        });
        assertInvalidResponse(responseMutation -> {
            responseMutation.set(1, evaluation(responseMutation.get(1).candidateId(), 1));
        });
        assertInvalidResponse(responseMutation -> {
            responseMutation.set(11, evaluation(responseMutation.get(11).candidateId(), 13));
        });
    }

    @Test
    void preservesRequestedOfferCountAndCarriesPromptAndExclusionInRequest() {
        assertThatThrownBy(() -> new StartNewSession(DATE, List.of(), 1L, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new StartNewSession(DATE, List.of(), 1L, 4)).isInstanceOf(IllegalArgumentException.class);
        Generated generated = generated(3, 71_100_050L);
        long exclusionRuleId = jdbcTemplate.queryForObject("select id from exclusion_rule where active order by id limit 1", Long.class);
        jdbcTemplate.update("""
                update generation_attempt set exclusion_rule_id = ?, exclusion_text_snapshot = 'No test exclusion'
                where id = ?
                """, exclusionRuleId, generated.attemptId());
        PlannedRound planned = planInitial(generated);
        assertThat(planned.request().promptVersion()).isEqualTo("prompt-v1");
        assertThat(planned.request().attemptExclusion().exclusionRuleId()).isEqualTo(exclusionRuleId);
        assertThat(planned.request().attemptExclusion().exclusionTextSnapshot()).isEqualTo("No test exclusion");

        assertThat(curationQueries.findAttempt(generated.attemptId()).orElseThrow().requestedOfferCount()).isEqualTo(3);
    }

    @Test
    void offerCompletenessRemainsEnforcedForPreparedLaterPresentationStatuses() {
        Generated generated = generated(1, 71_100_060L);
        PlannedRound planned = planInitial(generated);
        curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), response(planned, 1)));
        CuratedOfferSet offerSet = createOffers(generated, planned, 1);
        jdbcTemplate.update("""
                update curated_offer_set set status = 'PRESENTED_PENDING_DECISION', presented_at = now()
                where id = ?
                """, offerSet.offerSetId());
        assertThatThrownBy(() -> jdbcTemplate.update("delete from curated_offer where curated_offer_set_id = ?", offerSet.offerSetId()))
                .isInstanceOf(DataAccessException.class);
    }

    private Generated generated(int requestedOfferCount, long seed) {
        return (Generated) generationCommands.startNewSession(new StartNewSession(DATE, List.of(), seed, requestedOfferCount));
    }

    private PlannedRound planInitial(Generated generated) {
        GenerationQueries.BatchView batch = generationQueries.findBatch(generated.attemptId(), 1).orElseThrow();
        return (PlannedRound) curationCommands.planRound(new CurationCommands.PlanRound(generated.attemptId(), 1,
                batch.batchId(), CurationModel.RequestPurpose.INITIAL_PASS, "future-curator", "prompt-v1",
                curationQueries.findAttempt(generated.attemptId()).orElseThrow().requestedOfferCount(),
                newParticipants(batch.candidates())));
    }

    private static List<CandidateParticipation> newParticipants(List<GenerationQueries.CandidateView> candidates) {
        return candidates.stream().map(candidate -> new CandidateParticipation(candidate.candidateId(),
                CurationModel.Participation.NEW, null)).toList();
    }

    private static List<CandidateParticipation> newParticipants(PlannedRound round) {
        return round.request().candidates().stream().map(candidate -> new CandidateParticipation(candidate.candidateId(),
                CurationModel.Participation.NEW, null)).toList();
    }

    private static CurationResponse response(PlannedRound planned, int goodCount) {
        java.util.ArrayList<CurationResponse.CandidateEvaluation> evaluations = new java.util.ArrayList<>();
        for (CurationRequest.Candidate candidate : planned.request().candidates()) {
            if (candidate.participation() != CurationModel.Participation.LOCKED_CONTEXT) {
                int rank = evaluations.size() + 1;
                evaluations.add(new CurationResponse.CandidateEvaluation(candidate.candidateId(),
                        rank <= goodCount ? CurationModel.Evaluation.GOOD : CurationModel.Evaluation.ACCEPTABLE,
                        rank, List.of("CULINARY_COHERENCE"), java.util.Map.of()));
            }
        }
        return new CurationResponse(CurationModel.CONTRACT_VERSION, planned.attemptId(), planned.roundId(),
                planned.request().primaryBatchId(), evaluations);
    }

    private static CurationResponse classifiedResponse(PlannedRound planned) {
        java.util.ArrayList<CurationResponse.CandidateEvaluation> evaluations = new java.util.ArrayList<>();
        for (CurationRequest.Candidate candidate : planned.request().candidates()) {
            int rank = evaluations.size() + 1;
            CurationModel.Evaluation classification = rank == 1 ? CurationModel.Evaluation.GOOD
                    : rank == 2 ? CurationModel.Evaluation.ACCEPTABLE : CurationModel.Evaluation.BAD;
            evaluations.add(new CurationResponse.CandidateEvaluation(candidate.candidateId(), classification, rank,
                    List.of("CULINARY_COHERENCE"), java.util.Map.of()));
        }
        return new CurationResponse(CurationModel.CONTRACT_VERSION, planned.attemptId(), planned.roundId(),
                planned.request().primaryBatchId(), evaluations);
    }

    private void assertInvalidResponse(java.util.function.Consumer<java.util.ArrayList<CurationResponse.CandidateEvaluation>> mutation) {
        Generated generated = generated(1, 71_100_070L + jdbcTemplate.queryForObject("select count(*) from generation_attempt", Long.class));
        PlannedRound planned = planInitial(generated);
        java.util.ArrayList<CurationResponse.CandidateEvaluation> evaluations = new java.util.ArrayList<>(response(planned, 1).evaluations());
        mutation.accept(evaluations);
        CurationResponse invalid = new CurationResponse(CurationModel.CONTRACT_VERSION, planned.attemptId(), planned.roundId(),
                planned.request().primaryBatchId(), evaluations);
        assertThat(curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), invalid)))
                .isInstanceOf(CurationCommands.InvalidResponse.class);
        CurationQueries.RoundView stored = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(stored.candidates()).allSatisfy(candidate -> assertThat(candidate.evaluation()).isNull());
    }

    private static CurationResponse.CandidateEvaluation evaluation(long candidateId, int rank) {
        return new CurationResponse.CandidateEvaluation(candidateId, CurationModel.Evaluation.ACCEPTABLE, rank,
                List.of("CULINARY_COHERENCE"), java.util.Map.of());
    }

    private static Object raced(CountDownLatch ready, CountDownLatch start,
                                java.util.concurrent.Callable<?> action) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent curation test did not start");
        }
        try {
            return action.call();
        } catch (CurationConflictException exception) {
            return exception;
        }
    }

    private CuratedOfferSet createOffers(Generated generated, PlannedRound planned, int count) {
        List<CurationQueries.RoundCandidateView> candidates = curationQueries.findRoundById(planned.roundId()).orElseThrow().candidates();
        return (CuratedOfferSet) curationCommands.createOfferSet(new CurationCommands.CreateOfferSet(generated.attemptId(),
                candidates.subList(0, count).stream().map(candidate -> new CurationCommands.OfferSelection(
                        candidate.candidateId(), candidate.roundCandidateId())).toList(),
                List.of("EXPLICIT_CURATOR_SELECTION")));
    }

    private long confirmedLegacySession(int requestedOfferCount) {
        long sessionId = jdbcTemplate.queryForObject(
                "insert into challenge_session (requested_offer_count) values (?) returning id", Long.class, requestedOfferCount);
        long attemptId = jdbcTemplate.queryForObject("""
                insert into generation_attempt (
                    challenge_session_id, attempt_type, status, generator_version, completed_at
                ) values (?, 'INITIAL', 'GENERATED', 'legacy-generator', now()) returning id
                """, Long.class, sessionId);
        long batchId = jdbcTemplate.queryForObject("""
                insert into generation_batch (generation_attempt_id, batch_number, status, legacy_migrated)
                values (?, 1, 'GENERATED', true) returning id
                """, Long.class, attemptId);
        long roundId = jdbcTemplate.queryForObject("""
                insert into curation_round (
                    generation_attempt_id, round_number, curator_model, prompt_version, status, completed_at, legacy_migrated
                ) values (?, 1, 'legacy-model', 'legacy-prompt', 'SELECTED', now(), true) returning id
                """, Long.class, attemptId);
        long candidateId = jdbcTemplate.queryForObject("""
                insert into challenge_candidate (generation_batch_id, curation_round_id, candidate_number, is_selected)
                values (?, ?, 1, true) returning id
                """, Long.class, batchId, roundId);
        jdbcTemplate.update("""
                insert into candidate_requirement (
                    candidate_id, position, source, ingredient_concept_id,
                    challenge_specificity_snapshot, display_text_snapshot, concept_code_snapshot,
                    novelty_level_snapshot, concept_snapshot
                )
                select ?, row_number() over (order by id), 'RANDOM', id,
                       challenge_specificity, display_name, code, novelty_level,
                       jsonb_build_object(
                           'functionalRoles', '[]'::jsonb,
                           'culinaryFlags', '[]'::jsonb,
                           'transitiveAncestorCodes', '[]'::jsonb
                       )
                from ingredient_concept
                where active and random_draw_enabled and novelty_level is not null
                order by id limit 4
                """, candidateId);
        jdbcTemplate.update("insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)",
                attemptId, candidateId);
        return sessionId;
    }
}
