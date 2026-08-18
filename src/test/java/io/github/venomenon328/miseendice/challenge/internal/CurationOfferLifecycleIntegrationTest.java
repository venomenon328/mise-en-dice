package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CandidateParticipation;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CompletedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.CuratedOfferSet;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.InvalidResponse;
import io.github.venomenon328.miseendice.challenge.api.CurationCommands.PlannedRound;
import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {MiseEnDiceApplication.class, CurationOfferLifecycleIntegrationTest.GeneratedBatchConfiguration.class})
@Testcontainers
class CurationOfferLifecycleIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 13);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_curation")
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
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanData() {
        jdbcTemplate.update("delete from curated_offer_set");
        jdbcTemplate.update("delete from curation_round");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void persistsACompleteRequestResponseAndExactlyTwoOffersWithOneGood() {
        Generated generated = generated(2, 71_000_001L);
        CurationCommands.PlanRound command = initialPlan(generated, 2);

        PlannedRound planned = (PlannedRound) curationCommands.planRound(command);
        assertThat(planned.request().requestedOfferCount()).isEqualTo(2);
        assertThat(planned.request().promptVersion()).isEqualTo(OpenAiCuratorPrompt.CURRENT_VERSION);
        assertThat(planned.request().contractVersion()).isEqualTo(CurationModel.CONTRACT_VERSION_V2);
        assertThat(planned.request().candidates()).hasSize(12);
        assertThat(planned.request().candidates()).allSatisfy(candidate -> {
            assertThat(candidate.participation()).isEqualTo(CurationModel.Participation.NEW);
            assertThat(candidate.snapshot().requirements()).hasSize(4);
        });

        CurationResponse response = response(planned, 1);
        assertThat(curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), response)))
                .isInstanceOf(CompletedRound.class);

        CurationQueries.RoundView completed = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(completed.status()).isEqualTo(CurationModel.RoundStatus.COMPLETED);
        assertThat(completed.responsePayloadJson()).isNotBlank();
        assertThat(completed.candidates()).allSatisfy(candidate -> {
            assertThat(candidate.evaluation()).isNotNull();
            assertThat(candidate.rank()).isNotNull();
            assertThat(candidate.reasonCodes()).containsExactly("CULINARY_COHERENCE");
        });

        CuratedOfferSet offerSet = (CuratedOfferSet) curationCommands.createOfferSet(
                new CurationCommands.CreateOfferSet(generated.attemptId(), List.of(
                        new CurationCommands.OfferSelection(completed.candidates().get(0).candidateId(),
                                completed.candidates().get(0).roundCandidateId()),
                        new CurationCommands.OfferSelection(completed.candidates().get(1).candidateId(),
                                completed.candidates().get(1).roundCandidateId())
                ), List.of("EXPLICIT_CURATOR_SELECTION")));

        CurationQueries.OfferSetView persisted = curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
        assertThat(offerSet.requestedOfferCount()).isEqualTo(2);
        assertThat(persisted.status()).isEqualTo(CurationModel.OfferSetStatus.CURATED_UNPRESENTED);
        assertThat(persisted.offers()).hasSize(2);
        assertThat(persisted.offers()).extracting(CurationQueries.OfferView::position).containsExactly(1, 2);
        assertThat(persisted.offers()).extracting(CurationQueries.OfferView::evaluation)
                .contains(CurationModel.Evaluation.GOOD);
        assertThat(curationQueries.findAttempt(generated.attemptId()).orElseThrow().curationStatus())
                .isEqualTo("OFFER_READY");
    }

    @Test
    void structurallyInvalidResponseIsTerminalWithoutPartialEvaluations() {
        Generated generated = generated(1, 71_000_002L);
        PlannedRound planned = (PlannedRound) curationCommands.planRound(initialPlan(generated, 1));
        CurationResponse invalid = new CurationResponse(planned.request().contractVersion(), generated.attemptId(),
                planned.roundId(), planned.request().primaryBatchId(), response(planned, 1).evaluations().subList(1, 12));

        assertThat(curationCommands.completeRound(new CurationCommands.CompleteRound(planned.roundId(), invalid)))
                .isInstanceOf(InvalidResponse.class);
        CurationQueries.RoundView stored = curationQueries.findRoundById(planned.roundId()).orElseThrow();
        assertThat(stored.status()).isEqualTo(CurationModel.RoundStatus.INVALID_RESPONSE);
        assertThat(stored.candidates()).allSatisfy(candidate -> {
            assertThat(candidate.evaluation()).isNull();
            assertThat(candidate.rank()).isNull();
        });
        assertThat(curationQueries.findAttempt(generated.attemptId()).orElseThrow().curationStatus())
                .isEqualTo("FAILED");
    }

    @Test
    void concurrentIdenticalRoundStartAndOfferCreationAreIdempotent() throws Exception {
        Generated generated = generated(1, 71_000_003L);
        CurationCommands.PlanRound plan = initialPlan(generated, 1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> curationCommands.planRound(plan));
            var second = executor.submit(() -> curationCommands.planRound(plan));
            PlannedRound left = (PlannedRound) first.get();
            PlannedRound right = (PlannedRound) second.get();
            assertThat(left.roundId()).isEqualTo(right.roundId());

            CurationQueries.RoundView round = curationQueries.findRoundById(left.roundId()).orElseThrow();
            curationCommands.completeRound(new CurationCommands.CompleteRound(left.roundId(), response(left, 1)));
            CurationCommands.CreateOfferSet offer = new CurationCommands.CreateOfferSet(generated.attemptId(), List.of(
                    new CurationCommands.OfferSelection(round.candidates().get(0).candidateId(),
                            round.candidates().get(0).roundCandidateId())), List.of("EXPLICIT_CURATOR_SELECTION"));
            var offerOne = executor.submit(() -> curationCommands.createOfferSet(offer));
            var offerTwo = executor.submit(() -> curationCommands.createOfferSet(offer));
            assertThat(((CuratedOfferSet) offerOne.get()).offerSetId())
                    .isEqualTo(((CuratedOfferSet) offerTwo.get()).offerSetId());
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from curation_round", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from curated_offer_set", Integer.class)).isEqualTo(1);
    }

    @Test
    void requestedOfferCountIsSessionScopedAndDoesNotChangeTheGeneratedSetFingerprint() {
        Generated oneOffer = generated(1, 71_000_004L);
        Generated threeOffers = generated(3, 71_000_004L);

        assertThat(oneOffer.setFingerprint()).isEqualTo(threeOffers.setFingerprint());
        assertThat(curationQueries.findAttempt(oneOffer.attemptId()).orElseThrow().requestedOfferCount()).isEqualTo(1);
        assertThat(curationQueries.findAttempt(threeOffers.attemptId()).orElseThrow().requestedOfferCount()).isEqualTo(3);
    }

    @Test
    void lockedContextIsNotReevaluatedWhileCarryOverIsEvaluatedAgain() {
        Generated generated = generated(2, 71_000_005L);
        PlannedRound first = (PlannedRound) curationCommands.planRound(initialPlan(generated, 2));
        curationCommands.completeRound(new CurationCommands.CompleteRound(first.roundId(), response(first, 1)));
        CurationQueries.RoundView firstCompleted = curationQueries.findRoundById(first.roundId()).orElseThrow();
        GenerationQueries.BatchView secondBatch = duplicateGeneratedBatch(generated.attemptId(), first.request().primaryBatchId());

        CurationCommands.PlanRound followUp = new CurationCommands.PlanRound(generated.attemptId(), 2,
                secondBatch.batchId(), CurationModel.RequestPurpose.QUALITY_FOLLOW_UP,
                "future-curator", OpenAiCuratorPrompt.CURRENT_VERSION, 1, followUpCandidates(secondBatch, firstCompleted));
        PlannedRound second = (PlannedRound) curationCommands.planRound(followUp);
        curationCommands.completeRound(new CurationCommands.CompleteRound(second.roundId(), response(second, 1)));

        CurationQueries.RoundView stored = curationQueries.findRoundById(second.roundId()).orElseThrow();
        assertThat(stored.candidates().get(0).participation()).isEqualTo(CurationModel.Participation.LOCKED_CONTEXT);
        assertThat(stored.candidates().get(0).evaluation()).isNull();
        assertThat(stored.candidates().get(1).participation()).isEqualTo(CurationModel.Participation.CARRY_OVER);
        assertThat(stored.candidates().get(1).evaluation()).isEqualTo(CurationModel.Evaluation.GOOD);
        assertThat(stored.candidates().get(1).rank()).isEqualTo(1);
    }

    private Generated generated(int requestedOfferCount, long seed) {
        return (Generated) generationCommands.startNewSession(
                new StartNewSession(DATE, List.of(), seed, requestedOfferCount, RestrictionMode.AUTO));
    }

    private CurationCommands.PlanRound initialPlan(Generated generated, int openOfferSlots) {
        GenerationQueries.BatchView batch = generationQueries.findBatch(generated.attemptId(), 1).orElseThrow();
        return new CurationCommands.PlanRound(generated.attemptId(), 1, batch.batchId(),
                CurationModel.RequestPurpose.INITIAL_PASS, "future-curator", OpenAiCuratorPrompt.CURRENT_VERSION, openOfferSlots,
                batch.candidates().stream().map(candidate -> new CandidateParticipation(candidate.candidateId(),
                        CurationModel.Participation.NEW, null)).toList());
    }

    private CurationResponse response(PlannedRound planned, int goodCount) {
        java.util.ArrayList<CurationResponse.CandidateEvaluation> evaluations = new java.util.ArrayList<>();
        for (CurationRequest.Candidate candidate : planned.request().candidates()) {
            if (candidate.participation() != CurationModel.Participation.LOCKED_CONTEXT) {
                int rank = evaluations.size() + 1;
                evaluations.add(new CurationResponse.CandidateEvaluation(candidate.candidateId(),
                        rank <= goodCount ? CurationModel.Evaluation.GOOD : CurationModel.Evaluation.ACCEPTABLE,
                        rank, List.of("CULINARY_COHERENCE"), java.util.Map.of()));
            }
        }
        return new CurationResponse(planned.request().contractVersion(), planned.attemptId(), planned.roundId(),
                planned.request().primaryBatchId(), evaluations);
    }

    private List<CandidateParticipation> followUpCandidates(GenerationQueries.BatchView secondBatch,
                                                              CurationQueries.RoundView firstCompleted) {
        java.util.ArrayList<CandidateParticipation> candidates = new java.util.ArrayList<>();
        candidates.add(new CandidateParticipation(firstCompleted.candidates().get(0).candidateId(),
                CurationModel.Participation.LOCKED_CONTEXT, firstCompleted.candidates().get(0).roundCandidateId()));
        candidates.add(new CandidateParticipation(firstCompleted.candidates().get(1).candidateId(),
                CurationModel.Participation.CARRY_OVER, firstCompleted.candidates().get(1).roundCandidateId()));
        secondBatch.candidates().forEach(candidate -> candidates.add(new CandidateParticipation(candidate.candidateId(),
                CurationModel.Participation.NEW, null)));
        return List.copyOf(candidates);
    }

    private GenerationQueries.BatchView duplicateGeneratedBatch(long attemptId, long firstBatchId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            long secondBatchId = jdbcTemplate.queryForObject("""
                    insert into generation_batch (
                        generation_attempt_id, batch_number, batch_seed, status, fallback_level,
                        reservoir_metrics, fallback_attempts, set_evaluation, diagnostics,
                        result_snapshot, set_fingerprint
                    )
                    select generation_attempt_id, 2, batch_seed, status, fallback_level,
                           reservoir_metrics, fallback_attempts, set_evaluation, diagnostics,
                           result_snapshot, set_fingerprint
                    from generation_batch where id = ?
                    returning id
                    """, Long.class, firstBatchId);
            jdbcTemplate.update("""
                    insert into challenge_candidate (
                        generation_batch_id, candidate_number, proposal_ordinal, profile, target_specificity,
                        target_novelty_band, actual_novelty_band, known_novelty_load, total_score,
                        data_confidence, component_scores, profile_slot_assignments,
                        generator_reason_codes, generator_diagnostics, canonical_signature
                    )
                    select ?, candidate_number, proposal_ordinal, profile, target_specificity,
                           target_novelty_band, actual_novelty_band, known_novelty_load, total_score,
                           data_confidence, component_scores, profile_slot_assignments,
                           generator_reason_codes, generator_diagnostics, canonical_signature
                    from challenge_candidate where generation_batch_id = ?
                    """, secondBatchId, firstBatchId);
            jdbcTemplate.update("""
                    insert into candidate_requirement (
                        candidate_id, position, source, ingredient_concept_id, manual_requirement_id,
                        challenge_specificity_snapshot, display_text_snapshot, concept_code_snapshot,
                        novelty_level_snapshot, concept_snapshot, weight_evaluation_snapshot, generator_reason_codes
                    )
                    select copied.id, requirement.position, requirement.source, requirement.ingredient_concept_id,
                           requirement.manual_requirement_id, requirement.challenge_specificity_snapshot,
                           requirement.display_text_snapshot, requirement.concept_code_snapshot,
                           requirement.novelty_level_snapshot, requirement.concept_snapshot,
                           requirement.weight_evaluation_snapshot, requirement.generator_reason_codes
                    from candidate_requirement requirement
                    join challenge_candidate original on original.id = requirement.candidate_id
                    join challenge_candidate copied on copied.generation_batch_id = ?
                        and copied.candidate_number = original.candidate_number
                    where original.generation_batch_id = ?
                    """, secondBatchId, firstBatchId);
            return generationQueries.findBatch(attemptId, 2).orElseThrow();
        });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GeneratedBatchConfiguration {
        @Bean
        @Primary
        CandidateSetEngine deterministicGeneratedCandidateSetEngine(CandidateReservoirEngine reservoirEngine) {
            return new DeterministicGeneratedCandidateSetEngine(reservoirEngine);
        }
    }
}
