package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.GenerationOutcome;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.InProgress;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.ManualRequirementInput;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartExistingSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayDifferenceType;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.ReplayStatus;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
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
        PersistedGenerationIntegrationTest.GeneratedBatchConfiguration.class
})
@Testcontainers
class PersistedGenerationIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 13);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_persisted_generation")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GenerationCommands commands;
    @Autowired GenerationQueries queries;
    @Autowired JdbcGenerationRepository repository;
    @Autowired CandidateSetEngine candidateSetEngine;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanGenerationData() {
        jdbcTemplate.execute("drop trigger if exists test_reject_candidate_five on challenge_candidate");
        jdbcTemplate.execute("drop function if exists test_reject_candidate_five()");
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void persistsBatchOneWithoutCurationAndReplaysEverySnapshot() {
        Generated generated = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_001L)));

        var attempt = queries.findAttempt(generated.attemptId()).orElseThrow();
        var context = queries.findContext(generated.attemptId()).orElseThrow();
        var batch = queries.findBatch(generated.attemptId(), 1).orElseThrow();
        assertThat(attempt.status()).isEqualTo("GENERATED");
        assertThat(attempt.batchNumbers()).containsExactly(1);
        assertThat(attempt.nextAction()).isEqualTo(GenerationQueries.NextAction.AWAIT_CURATION);
        assertThat(context.configurationSnapshotJson()).isNotBlank();
        assertThat(context.catalogSnapshotJson()).isNotBlank();
        assertThat(context.requestSnapshotJson()).isNotBlank();
        assertThat(context.visibleHistorySnapshotJson()).isNotBlank();
        assertThat(context.preparedAttemptSnapshotJson()).isNotBlank();
        assertThat(List.of(
                context.contextFingerprint(), context.configurationFingerprint(), context.catalogFingerprint(),
                context.requestFingerprint(), context.historyFingerprint()))
                .allMatch(value -> value.matches("[0-9a-f]{64}"));
        assertThat(batch.candidates()).hasSize(12);
        assertThat(batch.candidates()).allSatisfy(candidate -> assertThat(candidate.requirements()).hasSize(4));
        assertThat(batch.candidates()).extracting(GenerationQueries.CandidateView::candidateNumber)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList());
        assertThat(jdbcTemplate.queryForObject("select count(*) from curation_round", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from challenge_candidate
                where generation_batch_id = ? and curation_round_id is null and is_selected is null
                """, Integer.class, batch.batchId())).isEqualTo(12);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from challenge_candidate candidate
                join generation_batch generation_batch on generation_batch.id = candidate.generation_batch_id
                where generation_batch.generation_attempt_id = ?
                  and candidate.generation_batch_id = ?
                """, Integer.class, generated.attemptId(), batch.batchId())).isEqualTo(12);
        int persistedCandidates = jdbcTemplate.queryForObject(
                "select count(*) from challenge_candidate", Integer.class);
        int persistedBatches = jdbcTemplate.queryForObject(
                "select count(*) from generation_batch", Integer.class);
        assertThat(queries.replay(generated.attemptId(), 1).status()).isEqualTo(ReplayStatus.MATCH);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from challenge_candidate", Integer.class)).isEqualTo(persistedCandidates);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from generation_batch", Integer.class)).isEqualTo(persistedBatches);
    }

    @Test
    void replayReportsTheFirstPersistedDifferenceThroughThePublicReadOnlyQuery() {
        Generated generated = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_002L)));
        GenerationQueries.BatchView batch = queries.findBatch(generated.attemptId(), 1).orElseThrow();
        GenerationQueries.CandidateView candidate = batch.candidates().getFirst();

        assertThat(queries.replay(generated.attemptId(), 1).difference()).isNull();

        jdbcTemplate.update("update generation_batch set set_fingerprint = ? where id = ?",
                "0".repeat(64), batch.batchId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.SET_FINGERPRINT, "setFingerprint");
        jdbcTemplate.update("update generation_batch set set_fingerprint = ? where id = ?",
                batch.setFingerprint(), batch.batchId());

        jdbcTemplate.update("update challenge_candidate set canonical_signature = ? where id = ?",
                candidate.canonicalSignature() + "-changed", candidate.candidateId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.CANDIDATE_SIGNATURE,
                "candidates[" + candidate.candidateNumber() + "].canonicalSignature");
        jdbcTemplate.update("update challenge_candidate set canonical_signature = ? where id = ?",
                candidate.canonicalSignature(), candidate.candidateId());

        jdbcTemplate.update("update challenge_candidate set total_score = total_score + 0.001 where id = ?",
                candidate.candidateId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.CANDIDATE_TOTAL_SCORE,
                "candidates[" + candidate.candidateNumber() + "].totalScore");
        jdbcTemplate.update("update challenge_candidate set total_score = ? where id = ?",
                candidate.totalScore(), candidate.candidateId());

        jdbcTemplate.update("""
                update challenge_candidate
                set component_scores = jsonb_set(component_scores, '{review_probe}', '0'::jsonb)
                where id = ?
                """, candidate.candidateId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.CANDIDATE_COMPONENT_SCORES,
                "candidates[" + candidate.candidateNumber() + "].componentScores");
        jdbcTemplate.update("update challenge_candidate set component_scores = cast(? as jsonb) where id = ?",
                candidate.componentScoresJson(), candidate.candidateId());

        jdbcTemplate.update("update challenge_candidate set generator_reason_codes = '[\"REVIEW_PROBE\"]'::jsonb where id = ?",
                candidate.candidateId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.CANDIDATE_REASON_CODES,
                "candidates[" + candidate.candidateNumber() + "].reasonCodes");
        jdbcTemplate.update("update challenge_candidate set generator_reason_codes = cast(? as jsonb) where id = ?",
                candidate.reasonCodesJson(), candidate.candidateId());

        jdbcTemplate.update("""
                update generation_batch
                set set_evaluation = jsonb_set(set_evaluation, '{review_probe}', 'true'::jsonb)
                where id = ?
                """, batch.batchId());
        assertDifference(generated.attemptId(), ReplayDifferenceType.SET_EVALUATION, "setEvaluation");
    }

    @Test
    void replayKeepsMatchAndNonMismatchStatesFreeOfDifferences() {
        Generated generated = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_003L)));

        assertThat(queries.replay(generated.attemptId(), 1))
                .extracting(GenerationQueries.ReplayResult::status, GenerationQueries.ReplayResult::difference)
                .containsExactly(ReplayStatus.MATCH, null);
        assertThat(queries.replay(Long.MAX_VALUE, 1))
                .extracting(GenerationQueries.ReplayResult::status, GenerationQueries.ReplayResult::difference)
                .containsExactly(ReplayStatus.NOT_FOUND, null);

        jdbcTemplate.update("update generation_attempt set configuration_version = 'unsupported-review-test' where id = ?",
                generated.attemptId());
        assertThat(queries.replay(generated.attemptId(), 1))
                .extracting(GenerationQueries.ReplayResult::status, GenerationQueries.ReplayResult::difference)
                .containsExactly(ReplayStatus.UNSUPPORTED_VERSION, null);
    }

    @Test
    void supportsZeroOneAndTwoManualInputsThroughThePublicCommand() {
        Generated zero = generated(commands.startNewSession(new StartNewSession(DATE, List.of(), 7_100L)));
        long firstConcept = conceptId(0);
        Generated one = generated(commands.startNewSession(new StartNewSession(DATE,
                List.of(new ManualRequirementInput(1, "Miso manual", firstConcept)), 7_101L)));
        Generated two = generated(commands.startNewSession(new StartNewSession(DATE,
                List.of(new ManualRequirementInput(1, "Miso manual", firstConcept),
                        new ManualRequirementInput(2, "Freitext manual", null)), 7_102L)));

        assertThat(manualCount(zero.attemptId())).isZero();
        assertThat(manualCount(one.attemptId())).isEqualTo(1);
        assertThat(manualCount(two.attemptId())).isEqualTo(2);
        assertThat(candidateManualCount(one.attemptId())).isEqualTo(12);
        assertThat(candidateManualCount(two.attemptId())).isEqualTo(24);
    }

    @Test
    void onlyConfirmedChallengesEnterHistoryAndDriveExactCooldown() {
        Generated unconfirmed = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_011L)));
        assertThat(repository.visibleHistory().challengesNewestFirst()).isEmpty();

        LegacyConfirmedChallenge legacy = createConfirmedLegacyChallenge();
        assertThat(queries.findAttempt(legacy.attemptId()).orElseThrow().nextAction())
                .isEqualTo(GenerationQueries.NextAction.NONE);
        String snapshotCode = legacy.codes().iterator().next();
        Integer snapshotNovelty = jdbcTemplate.queryForObject("""
                select novelty_level_snapshot from candidate_requirement
                where candidate_id = ? and concept_code_snapshot = ?
                """, Integer.class, legacy.candidateId(), snapshotCode);

        var visible = repository.visibleHistory();
        assertThat(visible.challengesNewestFirst()).hasSize(1);
        assertThat(visible.challengesNewestFirst().getFirst().requirements())
                .extracting(requirement -> requirement.conceptCode())
                .containsExactlyInAnyOrderElementsOf(legacy.codes());

        long currentConceptId = jdbcTemplate.queryForObject(
                "select id from ingredient_concept where code = ?", Long.class, snapshotCode);
        Integer currentNovelty = jdbcTemplate.queryForObject(
                "select novelty_level from ingredient_concept where id = ?", Integer.class, currentConceptId);
        int changedNovelty = currentNovelty == null || currentNovelty == 5 ? 1 : currentNovelty + 1;
        try {
            jdbcTemplate.update("update ingredient_concept set novelty_level = ? where id = ?",
                    changedNovelty, currentConceptId);
            assertThat(repository.visibleHistory().challengesNewestFirst().getFirst().requirements())
                    .filteredOn(requirement -> snapshotCode.equals(requirement.conceptCode()))
                    .singleElement().extracting(requirement -> requirement.noveltyLevel())
                    .isEqualTo(snapshotNovelty);
        } finally {
            jdbcTemplate.update("update ingredient_concept set novelty_level = ? where id = ?",
                    currentNovelty, currentConceptId);
        }

        Generated reroll = generated(commands.startReroll(new StartExistingSession(
                legacy.sessionId(), DATE.plusDays(1), List.of(), 47_000_012L)));
        List<String> legacyBlocked = jdbcTemplate.queryForList("""
                select jsonb_array_elements_text(request_snapshot -> 'rerollBlockedConceptCodes')
                from generation_context_snapshot where generation_attempt_id = ?
                """, String.class, reroll.attemptId());
        assertThat(legacyBlocked).isEmpty();
        assertThat(queries.findBatch(reroll.attemptId(), 1).orElseThrow().candidates()).allSatisfy(candidate ->
                assertThat(candidate.requirements().stream()
                        .filter(requirement -> "RANDOM".equals(requirement.source()))
                        .map(requirement -> requirement.conceptCodeSnapshot())
                        .toList())
                        .doesNotContainAnyElementsOf(legacy.codes()));
    }

    @Test
    void generatedCandidateCannotBypassCurationThroughTheLegacySelectionFlag() {
        Generated generated = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_013L)));
        long candidateId = queries.findBatch(generated.attemptId(), 1).orElseThrow().candidates().getFirst().candidateId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update challenge_candidate set is_selected = true where id = ?", candidateId))
                .isInstanceOf(DataAccessException.class);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from challenge where generation_attempt_id = ?",
                Integer.class, generated.attemptId())).isZero();
    }

    @Test
    void batchTwoReusesTheFrozenContextAndBatchThreeIsRejectedByPostgresql() {
        Generated initial = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_021L)));
        var prepared = repository.snapshotCodec().decodeAndVerify(repository.loadContext(initial.attemptId()));
        var second = candidateSetEngine.generate(prepared, 2);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                repository.saveAdditionalBatch(initial.attemptId(), second));

        assertThat(queries.findAttempt(initial.attemptId()).orElseThrow().batchNumbers()).containsExactly(1, 2);
        assertThat(queries.findBatch(initial.attemptId(), 1).orElseThrow().candidates())
                .extracting(GenerationQueries.CandidateView::candidateNumber)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList());
        assertThat(queries.findBatch(initial.attemptId(), 2).orElseThrow().candidates())
                .extracting(GenerationQueries.CandidateView::candidateNumber)
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList());

        var third = candidateSetEngine.generate(prepared, 3);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                repository.saveAdditionalBatch(initial.attemptId(), third)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void staleContextReadyAttemptReplaysAfterRestartWithoutChangingItsSet() {
        Generated first = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_031L)));
        jdbcTemplate.update("delete from generation_batch where generation_attempt_id = ?", first.attemptId());
        jdbcTemplate.update("""
                update generation_attempt
                set status = 'CONTEXT_READY', completed_at = null, operation_token = ?,
                    lease_expires_at = now() - interval '1 minute'
                where id = ?
                """, UUID.randomUUID(), first.attemptId());

        Generated recovered = generated(commands.startInitial(new StartExistingSession(
                first.sessionId(), DATE.plusMonths(1), List.of(), 999L)));
        assertThat(recovered.attemptId()).isEqualTo(first.attemptId());
        assertThat(recovered.setFingerprint()).isEqualTo(first.setFingerprint());
        assertThat(queries.replay(recovered.attemptId(), 1).status()).isEqualTo(ReplayStatus.MATCH);
    }

    @Test
    void retryAfterCommittedInitialReturnsTheExistingResult() {
        Generated first = generated(commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_032L)));

        Generated repeated = generated(commands.startInitial(new StartExistingSession(
                first.sessionId(), DATE.plusMonths(1), List.of(), 999L)));

        assertThat(repeated.attemptId()).isEqualTo(first.attemptId());
        assertThat(repeated.batchId()).isEqualTo(first.batchId());
        assertThat(repeated.setFingerprint()).isEqualTo(first.setFingerprint());
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from generation_batch where generation_attempt_id = ?
                """, Integer.class, first.attemptId())).isEqualTo(1);
    }

    @Test
    void concurrentInitialCommandsCreateOnlyOneAttemptAndOneBatch() throws Exception {
        long sessionId = jdbcTemplate.queryForObject(
                "insert into challenge_session default values returning id", Long.class);
        var command = new StartExistingSession(sessionId, DATE, List.of(), 47_000_041L);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> commands.startInitial(command));
            var second = executor.submit(() -> commands.startInitial(command));
            List<GenerationOutcome> results = List.of(first.get(), second.get());
            assertThat(results).anyMatch(result -> result instanceof Generated);
            assertThat(results).allMatch(result -> result instanceof Generated || result instanceof InProgress);
        }
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from generation_attempt
                where challenge_session_id = ? and attempt_type = 'INITIAL'
                """, Integer.class, sessionId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from generation_batch batch
                join generation_attempt attempt on attempt.id = batch.generation_attempt_id
                where attempt.challenge_session_id = ?
                """, Integer.class, sessionId)).isEqualTo(1);
    }

    @Test
    void concurrentRerollCommandsCreateOnlyOneAttemptAndOneBatch() throws Exception {
        LegacyConfirmedChallenge legacy = createConfirmedLegacyChallenge();
        var command = new StartExistingSession(legacy.sessionId(), DATE.plusDays(1), List.of(), 47_000_042L);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> commands.startReroll(command));
            var second = executor.submit(() -> commands.startReroll(command));
            List<GenerationOutcome> results = List.of(first.get(), second.get());
            assertThat(results).anyMatch(result -> result instanceof Generated);
            assertThat(results).allMatch(result -> result instanceof Generated || result instanceof InProgress);
        }
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from generation_attempt
                where challenge_session_id = ? and attempt_type = 'REROLL'
                """, Integer.class, legacy.sessionId())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from generation_batch batch
                join generation_attempt attempt on attempt.id = batch.generation_attempt_id
                where attempt.challenge_session_id = ? and attempt.attempt_type = 'REROLL'
                """, Integer.class, legacy.sessionId())).isEqualTo(1);
    }

    @Test
    void databaseFailureRollsBackTheWholeBatchAndRemainsRetryable() {
        jdbcTemplate.execute("""
                create function test_reject_candidate_five() returns trigger
                language plpgsql as $$
                begin
                    if new.candidate_number = 5 then
                        raise exception 'injected candidate persistence failure';
                    end if;
                    return new;
                end
                $$
                """);
        jdbcTemplate.execute("""
                create trigger test_reject_candidate_five
                before insert on challenge_candidate
                for each row execute function test_reject_candidate_five()
                """);

        assertThatThrownBy(() -> commands.startNewSession(
                new StartNewSession(DATE, List.of(), 47_000_051L)))
                .isInstanceOf(DataAccessException.class);

        long attemptId = jdbcTemplate.queryForObject("select max(id) from generation_attempt", Long.class);
        long sessionId = jdbcTemplate.queryForObject(
                "select challenge_session_id from generation_attempt where id = ?", Long.class, attemptId);
        assertThat(jdbcTemplate.queryForObject(
                "select status from generation_attempt where id = ?", String.class, attemptId))
                .isEqualTo("CONTEXT_READY");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from generation_batch where generation_attempt_id = ?", Integer.class, attemptId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*) from challenge_candidate candidate
                join generation_batch batch on batch.id = candidate.generation_batch_id
                where batch.generation_attempt_id = ?
                """, Integer.class, attemptId)).isZero();

        jdbcTemplate.execute("drop trigger test_reject_candidate_five on challenge_candidate");
        jdbcTemplate.execute("drop function test_reject_candidate_five()");
        jdbcTemplate.update("""
                update generation_attempt set lease_expires_at = now() - interval '1 minute' where id = ?
                """, attemptId);

        Generated recovered = generated(commands.startInitial(new StartExistingSession(
                sessionId, DATE.plusMonths(1), List.of(), 999L)));
        assertThat(recovered.attemptId()).isEqualTo(attemptId);
        assertThat(queries.findBatch(attemptId, 1).orElseThrow().candidates()).hasSize(12);
    }

    private LegacyConfirmedChallenge createConfirmedLegacyChallenge() {
        long sessionId = jdbcTemplate.queryForObject(
                "insert into challenge_session default values returning id", Long.class);
        long attemptId = jdbcTemplate.queryForObject("""
                insert into generation_attempt (
                    challenge_session_id, attempt_type, status, generator_version, completed_at
                ) values (?, 'INITIAL', 'GENERATED', 'legacy-generator', now()) returning id
                """, Long.class, sessionId);
        long batchId = jdbcTemplate.queryForObject("""
                insert into generation_batch (
                    generation_attempt_id, batch_number, status, legacy_migrated
                ) values (?, 1, 'GENERATED', true) returning id
                """, Long.class, attemptId);
        long roundId = jdbcTemplate.queryForObject("""
                insert into curation_round (
                    generation_attempt_id, round_number, curator_model, prompt_version, status, completed_at,
                    legacy_migrated
                ) values (?, 1, 'legacy-model', 'legacy-prompt', 'SELECTED', now(), true) returning id
                """, Long.class, attemptId);
        long candidateId = jdbcTemplate.queryForObject("""
                insert into challenge_candidate (
                    generation_batch_id, curation_round_id, candidate_number, is_selected
                ) values (?, ?, 1, true) returning id
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
        jdbcTemplate.update("""
                insert into challenge (generation_attempt_id, selected_candidate_id)
                values (?, ?)
                """, attemptId, candidateId);
        Set<String> codes = new HashSet<>(jdbcTemplate.queryForList("""
                select concept_code_snapshot from candidate_requirement
                where candidate_id = ? order by position
                """, String.class, candidateId));
        return new LegacyConfirmedChallenge(sessionId, attemptId, candidateId, Set.copyOf(codes));
    }

    private Generated generated(GenerationOutcome outcome) {
        assertThat(outcome).isInstanceOf(Generated.class);
        return (Generated) outcome;
    }

    private void assertDifference(long attemptId, ReplayDifferenceType type, String path) {
        assertThat(queries.replay(attemptId, 1))
                .extracting(GenerationQueries.ReplayResult::status,
                        replay -> replay.difference().type(),
                        replay -> replay.difference().path())
                .containsExactly(ReplayStatus.MISMATCH, type, path);
    }

    private long conceptId(int offset) {
        return jdbcTemplate.queryForList("""
                select id from ingredient_concept
                where active and random_draw_enabled order by code, id limit 3
                """, Long.class).get(offset);
    }

    private int manualCount(long attemptId) {
        return jdbcTemplate.queryForObject("""
                select count(*) from generation_manual_requirement where generation_attempt_id = ?
                """, Integer.class, attemptId);
    }

    private int candidateManualCount(long attemptId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from candidate_requirement requirement
                join challenge_candidate candidate on candidate.id = requirement.candidate_id
                join generation_batch batch on batch.id = candidate.generation_batch_id
                where batch.generation_attempt_id = ? and requirement.source = 'MANUAL'
                """, Integer.class, attemptId);
    }

    private record LegacyConfirmedChallenge(long sessionId, long attemptId, long candidateId, Set<String> codes) {
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
