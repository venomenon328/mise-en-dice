package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.CandidateSetResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.ManualRequirementInput;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.AttemptView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.BatchView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.CandidateView;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.NextAction;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries.RequirementView;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Explicit Spring-JDBC persistence adapter for generation lifecycle use cases. */
@Repository
class JdbcGenerationRepository {
    private static final TypeReference<Map<String, Object>> JSON_MAP = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final GenerationSnapshotCodec snapshotCodec;

    JdbcGenerationRepository(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine reservoirEngine,
            GeneratorProperties generatorProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.snapshotCodec = new GenerationSnapshotCodec(
                objectMapper, reservoirEngine, generatorProperties.configuration());
    }

    long createSession() {
        return jdbcTemplate.queryForObject("insert into challenge_session default values returning id", Long.class);
    }

    boolean lockSession(long sessionId) {
        return !jdbcTemplate.queryForList(
                "select id from challenge_session where id = ? for update", Long.class, sessionId).isEmpty();
    }

    Optional<AttemptState> findAttemptForUpdate(long sessionId, AttemptType type) {
        return jdbcTemplate.query("""
                select id, challenge_session_id, attempt_type, status, effective_date, season_month,
                       attempt_seed, operation_token, lease_expires_at, failure_reason_code, failure_detail
                from generation_attempt
                where challenge_session_id = ? and attempt_type = ?
                for update
                """, this::mapAttemptState, sessionId, type.name()).stream().findFirst();
    }

    Optional<AttemptState> findAttemptState(long attemptId) {
        return jdbcTemplate.query("""
                select id, challenge_session_id, attempt_type, status, effective_date, season_month,
                       attempt_seed, operation_token, lease_expires_at, failure_reason_code, failure_detail
                from generation_attempt where id = ?
                """, this::mapAttemptState, attemptId).stream().findFirst();
    }

    AttemptState createAttempt(
            long sessionId,
            AttemptType type,
            LocalDate effectiveDate,
            long seed,
            String generatorVersion,
            String configurationVersion,
            String rngAlgorithm,
            int canonicalPayloadVersion,
            List<ManualRequirementInput> manuals,
            UUID operationToken,
            Duration lease
    ) {
        long attemptId = jdbcTemplate.queryForObject("""
                insert into generation_attempt (
                    challenge_session_id, attempt_type, status, generator_version, effective_date,
                    season_month, attempt_seed, rng_algorithm, configuration_version,
                    canonical_payload_version, operation_token, lease_expires_at
                ) values (?, ?, 'PENDING', ?, ?, ?, ?, ?, ?, ?, ?, now() + (? * interval '1 millisecond'))
                returning id
                """, Long.class, sessionId, type.name(), generatorVersion, effectiveDate,
                effectiveDate.getMonthValue(), seed, rngAlgorithm, configurationVersion,
                canonicalPayloadVersion, operationToken, lease.toMillis());
        for (ManualRequirementInput manual : manuals) {
            jdbcTemplate.update("""
                    insert into generation_manual_requirement (
                        generation_attempt_id, position, display_text, matched_ingredient_concept_id
                    ) values (?, ?, ?, ?)
                    """, attemptId, manual.position(), manual.displayText(), manual.matchedIngredientConceptId());
        }
        return findAttemptForUpdate(sessionId, type).orElseThrow();
    }

    AttemptState reclaim(AttemptState attempt, UUID operationToken, Duration lease) {
        jdbcTemplate.update("""
                update generation_attempt
                set operation_token = ?, lease_expires_at = now() + (? * interval '1 millisecond')
                where id = ?
                """, operationToken, lease.toMillis(), attempt.attemptId());
        return findAttemptForUpdate(attempt.sessionId(), attempt.attemptType()).orElseThrow();
    }

    List<ManualRequirementRow> loadManualRequirements(long attemptId) {
        return jdbcTemplate.query("""
                select position, display_text, matched_ingredient_concept_id
                from generation_manual_requirement
                where generation_attempt_id = ?
                order by position
                """, (result, row) -> new ManualRequirementRow(
                result.getInt("position"), result.getString("display_text"),
                (Long) result.getObject("matched_ingredient_concept_id")), attemptId);
    }

    void saveContext(long attemptId, UUID operationToken, GenerationSnapshotCodec.EncodedContext context,
                     Long exclusionRuleId, String exclusionText) {
        int updated = jdbcTemplate.update("""
                insert into generation_context_snapshot (
                    generation_attempt_id, configuration_snapshot, catalog_snapshot, request_snapshot,
                    visible_history_snapshot, prepared_attempt_snapshot, context_fingerprint,
                    configuration_fingerprint, catalog_fingerprint, request_fingerprint, history_fingerprint
                ) values (?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), cast(? as jsonb),
                          cast(? as jsonb), ?, ?, ?, ?, ?)
                on conflict (generation_attempt_id) do nothing
                """, attemptId, context.configurationSnapshot(), context.catalogSnapshot(), context.requestSnapshot(),
                context.visibleHistorySnapshot(), context.preparedAttemptSnapshot(), context.contextFingerprint(),
                context.configurationFingerprint(), context.catalogFingerprint(), context.requestFingerprint(),
                context.historyFingerprint());
        if (updated != 1) {
            throw new LostGenerationClaimException("Generation context was already frozen");
        }
        updated = jdbcTemplate.update("""
                update generation_attempt
                set status = 'CONTEXT_READY', exclusion_rule_id = ?, exclusion_text_snapshot = ?
                where id = ? and status = 'PENDING' and operation_token = ?
                """, exclusionRuleId, exclusionText, attemptId, operationToken);
        if (updated != 1) {
            throw new LostGenerationClaimException("Generation attempt claim changed while freezing context");
        }
    }

    GenerationSnapshotCodec.StoredContext loadContext(long attemptId) {
        try {
            return jdbcTemplate.queryForObject("""
                    select configuration_snapshot::text, catalog_snapshot::text, request_snapshot::text,
                           visible_history_snapshot::text, prepared_attempt_snapshot::text,
                           context_fingerprint, configuration_fingerprint, catalog_fingerprint,
                           request_fingerprint, history_fingerprint
                    from generation_context_snapshot where generation_attempt_id = ?
                    """, (result, row) -> new GenerationSnapshotCodec.StoredContext(
                    result.getString(1), result.getString(2), result.getString(3), result.getString(4),
                    result.getString(5), result.getString(6), result.getString(7), result.getString(8),
                    result.getString(9), result.getString(10)), attemptId);
        } catch (IncorrectResultSizeDataAccessException exception) {
            throw new GenerationSnapshotCodec.InvalidContextSnapshotException(
                    "Generation context snapshot is missing", exception);
        }
    }

    GenerationSnapshotCodec snapshotCodec() {
        return snapshotCodec;
    }

    PersistedBatch saveBatch(long attemptId, UUID operationToken, CandidateSetResult result) {
        AttemptState state = jdbcTemplate.query("""
                select id, challenge_session_id, attempt_type, status, effective_date, season_month,
                       attempt_seed, operation_token, lease_expires_at, failure_reason_code, failure_detail
                from generation_attempt where id = ? for update
                """, this::mapAttemptState, attemptId).stream().findFirst().orElseThrow();
        Optional<PersistedBatch> existing = findPersistedBatch(attemptId, result.batchNumber());
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!operationToken.equals(state.operationToken())) {
            throw new LostGenerationClaimException("Generation attempt claim changed before batch persistence");
        }

        PersistedBatch persisted = persistResult(attemptId, result);
        jdbcTemplate.update("""
                update generation_attempt
                set status = ?, completed_at = now(), operation_token = null, lease_expires_at = null
                where id = ? and operation_token = ?
                """, persisted.status().equals("GENERATED") ? "GENERATED" : "EXHAUSTED",
                attemptId, operationToken);
        return persisted;
    }

    /** Internal persistence boundary reserved for Phase 10; phase-9D public commands never call it. */
    PersistedBatch saveAdditionalBatch(long attemptId, CandidateSetResult result) {
        AttemptState state = jdbcTemplate.query("""
                select id, challenge_session_id, attempt_type, status, effective_date, season_month,
                       attempt_seed, operation_token, lease_expires_at, failure_reason_code, failure_detail
                from generation_attempt where id = ? for update
                """, this::mapAttemptState, attemptId).stream().findFirst().orElseThrow();
        if (!"GENERATED".equals(state.status())) {
            throw new IllegalStateException("An additional batch requires an already generated attempt");
        }
        Optional<PersistedBatch> existing = findPersistedBatch(attemptId, result.batchNumber());
        return existing.orElseGet(() -> persistResult(attemptId, result));
    }

    private PersistedBatch persistResult(long attemptId, CandidateSetResult result) {

        if (result instanceof GeneratedCandidateSet generated) {
            long batchId = insertGeneratedBatch(attemptId, generated);
            Map<Integer, Long> manualIds = manualIds(attemptId);
            int number = 1;
            for (AcceptedProposal candidate : generated.candidates()) {
                long candidateId = insertCandidate(batchId, number++, candidate);
                for (RequirementSnapshot requirement : candidate.requirements()) {
                    insertRequirement(candidateId, requirement, manualIds);
                }
            }
            return new PersistedBatch(batchId, result.batchNumber(), "GENERATED", generated.fingerprint());
        }

        ExhaustedCandidateSet exhausted = (ExhaustedCandidateSet) result;
        long batchId = jdbcTemplate.queryForObject("""
                insert into generation_batch (
                    generation_attempt_id, batch_number, batch_seed, status, reservoir_metrics,
                    fallback_attempts, diagnostics, result_snapshot
                ) values (?, ?, ?, 'EXHAUSTED', cast(? as jsonb), cast(? as jsonb), cast(? as jsonb), cast(? as jsonb))
                returning id
                """, Long.class, attemptId, exhausted.batchNumber(), exhausted.batchSeed(),
                snapshotCodec.json(exhausted.reservoir().metrics()), snapshotCodec.json(exhausted.fallbackAttempts()),
                reasonJson(exhausted.diagnostics()), snapshotCodec.json(exhausted));
        return new PersistedBatch(batchId, result.batchNumber(), "EXHAUSTED", null);
    }

    private long insertGeneratedBatch(long attemptId, GeneratedCandidateSet generated) {
        return jdbcTemplate.queryForObject("""
                insert into generation_batch (
                    generation_attempt_id, batch_number, batch_seed, status, fallback_level,
                    reservoir_metrics, fallback_attempts, set_evaluation, diagnostics,
                    result_snapshot, set_fingerprint
                ) values (?, ?, ?, 'GENERATED', ?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb),
                          cast(? as jsonb), cast(? as jsonb), ?)
                returning id
                """, Long.class, attemptId, generated.batchNumber(), generated.batchSeed(),
                generated.fallbackLevel().name(), snapshotCodec.json(generated.reservoir().metrics()),
                snapshotCodec.json(generated.fallbackAttempts()), snapshotCodec.json(generated.evaluation()),
                reasonJson(generated.diagnostics()), snapshotCodec.json(generated), generated.fingerprint());
    }

    private long insertCandidate(long batchId, int number, AcceptedProposal candidate) {
        return jdbcTemplate.queryForObject("""
                insert into challenge_candidate (
                    generation_batch_id, candidate_number, proposal_ordinal, profile, target_specificity,
                    target_novelty_band, actual_novelty_band, known_novelty_load, total_score,
                    data_confidence, component_scores, profile_slot_assignments,
                    generator_reason_codes, generator_diagnostics, canonical_signature
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb),
                          cast(? as jsonb), cast(? as jsonb), ?)
                returning id
                """, Long.class, batchId, number, candidate.proposalOrdinal(), candidate.profile().name(),
                candidate.targetSpecificity(), candidate.targetNoveltyBand().name(),
                candidate.evaluation().actualNoveltyBand().name(), candidate.evaluation().knownNoveltyLoad(),
                candidate.evaluation().totalScore(), candidate.evaluation().dataConfidence(),
                snapshotCodec.json(candidate.evaluation().components()),
                snapshotCodec.json(candidate.evaluation().profileSlotAssignments()),
                reasonJson(candidate.evaluation().reasonCodes()), reasonJson(candidate.diagnostics()),
                candidate.canonicalSignature());
    }

    private void insertRequirement(long candidateId, RequirementSnapshot requirement, Map<Integer, Long> manualIds) {
        Long conceptId = requirement.source() == RequirementSource.RANDOM && requirement.concept() != null
                ? requirement.concept().id() : null;
        Long manualId = requirement.source() == RequirementSource.MANUAL
                ? manualIds.get(requirement.position()) : null;
        jdbcTemplate.update("""
                insert into candidate_requirement (
                    candidate_id, position, source, ingredient_concept_id, manual_requirement_id,
                    challenge_specificity_snapshot, display_text_snapshot, concept_code_snapshot,
                    novelty_level_snapshot, concept_snapshot, weight_evaluation_snapshot, generator_reason_codes
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), cast(? as jsonb))
                """, candidateId, requirement.position(), requirement.source().name(), conceptId, manualId,
                requirement.specificity().name(), requirement.displayText(),
                requirement.concept() == null ? null : requirement.concept().code(),
                requirement.concept() == null ? null : requirement.concept().noveltyLevel(),
                snapshotCodec.conceptJson(requirement.concept()), snapshotCodec.weightJson(requirement.weightEvaluation()),
                requirement.weightEvaluation() == null ? "[]"
                        : reasonJson(requirement.weightEvaluation().diagnostics()));
    }

    private Map<Integer, Long> manualIds(long attemptId) {
        Map<Integer, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select id, position from generation_manual_requirement
                where generation_attempt_id = ? order by position
                """, (org.springframework.jdbc.core.RowCallbackHandler) row ->
                result.put(row.getInt("position"), row.getLong("id")), attemptId);
        return result;
    }

    Optional<PersistedBatch> findPersistedBatch(long attemptId, int batchNumber) {
        return jdbcTemplate.query("""
                select id, batch_number, status, set_fingerprint
                from generation_batch where generation_attempt_id = ? and batch_number = ?
                """, (result, row) -> new PersistedBatch(result.getLong("id"), result.getInt("batch_number"),
                result.getString("status"), result.getString("set_fingerprint")), attemptId, batchNumber)
                .stream().findFirst();
    }

    void markFailed(long attemptId, UUID operationToken, String reason, String detail) {
        jdbcTemplate.update("""
                update generation_attempt
                set status = 'FAILED', completed_at = now(), failure_reason_code = ?, failure_detail = ?,
                    operation_token = null, lease_expires_at = null
                where id = ? and operation_token = ? and status in ('PENDING', 'CONTEXT_READY')
                """, reason, detail, attemptId, operationToken);
    }

    VisibleHistorySnapshot visibleHistory() {
        List<HistoryRow> rows = jdbcTemplate.query("""
                select challenge.id as challenge_id, challenge.shown_at, challenge.status,
                       session.id as session_id, attempt.attempt_type,
                       candidate.profile, candidate.actual_novelty_band,
                       context.prepared_attempt_snapshot #>> '{exclusionDecision,ruleCode}' as exclusion_rule_code,
                       requirement.position, requirement.concept_code_snapshot,
                       requirement.novelty_level_snapshot, requirement.concept_snapshot::text
                from challenge
                join generation_attempt attempt on attempt.id = challenge.generation_attempt_id
                join challenge_session session on session.id = attempt.challenge_session_id
                join challenge_candidate candidate on candidate.id = challenge.selected_candidate_id
                join candidate_requirement requirement on requirement.candidate_id = candidate.id
                left join generation_context_snapshot context on context.generation_attempt_id = attempt.id
                order by challenge.shown_at desc, challenge.id desc, requirement.position
                """, this::mapHistoryRow);
        Map<Long, HistoryBuilder> grouped = new LinkedHashMap<>();
        for (HistoryRow row : rows) {
            grouped.computeIfAbsent(row.challengeId(), ignored -> new HistoryBuilder(row)).requirements.add(
                    visibleRequirement(row));
        }
        return new VisibleHistorySnapshot(grouped.values().stream().map(HistoryBuilder::build).toList());
    }

    Set<String> confirmedInitialRequirementCodes(long sessionId) {
        return new LinkedHashSet<>(jdbcTemplate.queryForList("""
                select requirement.concept_code_snapshot
                from challenge
                join generation_attempt attempt on attempt.id = challenge.generation_attempt_id
                join candidate_requirement requirement on requirement.candidate_id = challenge.selected_candidate_id
                where attempt.challenge_session_id = ?
                  and attempt.attempt_type = 'INITIAL'
                  and requirement.concept_code_snapshot is not null
                order by requirement.position
                """, String.class, sessionId));
    }

    int confirmedInitialRequirementCount(long sessionId) {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from challenge
                join generation_attempt attempt on attempt.id = challenge.generation_attempt_id
                join candidate_requirement requirement on requirement.candidate_id = challenge.selected_candidate_id
                where attempt.challenge_session_id = ? and attempt.attempt_type = 'INITIAL'
                """, Integer.class, sessionId);
    }

    Optional<AttemptView> findAttemptView(long attemptId) {
        return jdbcTemplate.query("""
                select id, challenge_session_id, attempt_type, status, effective_date, season_month,
                       attempt_seed, rng_algorithm, generator_version, configuration_version,
                       canonical_payload_version, created_at, completed_at, failure_reason_code, failure_detail
                from generation_attempt where id = ?
                """, (result, row) -> new AttemptView(
                result.getLong("challenge_session_id"), result.getLong("id"),
                AttemptType.valueOf(result.getString("attempt_type")), result.getString("status"),
                result.getObject("effective_date", LocalDate.class), (Integer) result.getObject("season_month"),
                (Long) result.getObject("attempt_seed"), result.getString("rng_algorithm"),
                result.getString("generator_version"), result.getString("configuration_version"),
                (Integer) result.getObject("canonical_payload_version"),
                instant(result, "created_at"), instant(result, "completed_at"),
                result.getString("failure_reason_code"), result.getString("failure_detail"),
                nextAction(result.getString("status")), batchNumbers(result.getLong("id"))), attemptId)
                .stream().findFirst();
    }

    Optional<BatchView> findBatchView(long attemptId, int batchNumber) {
        return jdbcTemplate.query("""
                select id, generation_attempt_id, batch_number, legacy_migrated, batch_seed, status, fallback_level,
                       set_fingerprint, reservoir_metrics::text, fallback_attempts::text,
                       set_evaluation::text, diagnostics::text, result_snapshot::text, completed_at
                from generation_batch
                where generation_attempt_id = ? and batch_number = ?
                """, (result, row) -> new BatchView(
                result.getLong("id"), result.getLong("generation_attempt_id"), result.getInt("batch_number"),
                result.getBoolean("legacy_migrated"),
                (Long) result.getObject("batch_seed"), result.getString("status"),
                result.getString("fallback_level"), result.getString("set_fingerprint"),
                result.getString("reservoir_metrics"), result.getString("fallback_attempts"),
                result.getString("set_evaluation"), result.getString("diagnostics"),
                result.getString("result_snapshot"), candidates(result.getLong("id")),
                instant(result, "completed_at")), attemptId, batchNumber).stream().findFirst();
    }

    private List<CandidateView> candidates(long batchId) {
        return jdbcTemplate.query("""
                select id, candidate_number, proposal_ordinal, profile, target_specificity,
                       target_novelty_band, actual_novelty_band, known_novelty_load, total_score,
                       data_confidence, canonical_signature, component_scores::text,
                       profile_slot_assignments::text,
                       generator_reason_codes::text, generator_diagnostics::text
                from challenge_candidate where generation_batch_id = ? order by candidate_number
                """, (result, row) -> new CandidateView(
                result.getLong("id"), result.getInt("candidate_number"),
                (Long) result.getObject("proposal_ordinal"),
                result.getString("profile"), (Integer) result.getObject("target_specificity"),
                result.getString("target_novelty_band"), result.getString("actual_novelty_band"),
                (Integer) result.getObject("known_novelty_load"), result.getBigDecimal("total_score"),
                result.getBigDecimal("data_confidence"), result.getString("canonical_signature"),
                result.getString("component_scores"), result.getString("profile_slot_assignments"),
                result.getString("generator_reason_codes"),
                result.getString("generator_diagnostics"), requirements(result.getLong("id"))), batchId);
    }

    private List<RequirementView> requirements(long candidateId) {
        return jdbcTemplate.query("""
                select position, source, ingredient_concept_id, manual_requirement_id,
                       concept_code_snapshot, display_text_snapshot, challenge_specificity_snapshot,
                       novelty_level_snapshot, concept_snapshot::text, weight_evaluation_snapshot::text,
                       generator_reason_codes::text
                from candidate_requirement where candidate_id = ? order by position
                """, (result, row) -> new RequirementView(
                result.getInt("position"), result.getString("source"),
                (Long) result.getObject("ingredient_concept_id"),
                (Long) result.getObject("manual_requirement_id"), result.getString("concept_code_snapshot"),
                result.getString("display_text_snapshot"), result.getString("challenge_specificity_snapshot"),
                (Integer) result.getObject("novelty_level_snapshot"), result.getString("concept_snapshot"),
                result.getString("weight_evaluation_snapshot"), result.getString("generator_reason_codes")),
                candidateId);
    }

    private List<Integer> batchNumbers(long attemptId) {
        return jdbcTemplate.queryForList("""
                select batch_number from generation_batch
                where generation_attempt_id = ? order by batch_number
                """, Integer.class, attemptId);
    }

    private VisibleRequirement visibleRequirement(HistoryRow row) {
        if (row.conceptSnapshot() == null) {
            return new VisibleRequirement(row.conceptCode(), row.noveltyLevel(), Set.of(), Set.of(), Set.of());
        }
        try {
            Map<String, Object> concept = objectMapper.readValue(row.conceptSnapshot(), JSON_MAP);
            return new VisibleRequirement(row.conceptCode(), row.noveltyLevel(),
                    strings(concept.get("functionalRoles")), strings(concept.get("culinaryFlags")),
                    strings(concept.get("transitiveAncestorCodes")));
        } catch (JacksonException exception) {
            throw new GenerationSnapshotCodec.InvalidContextSnapshotException(
                    "Confirmed challenge contains an invalid historical concept snapshot", exception);
        }
    }

    private static Set<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return Set.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        list.forEach(item -> result.add(String.valueOf(item)));
        return Set.copyOf(result);
    }

    private HistoryRow mapHistoryRow(ResultSet result, int row) throws SQLException {
        return new HistoryRow(result.getLong("challenge_id"), instant(result, "shown_at"),
                result.getString("status"), result.getLong("session_id"),
                AttemptType.valueOf(result.getString("attempt_type")), result.getString("profile"),
                result.getString("actual_novelty_band"), result.getString("exclusion_rule_code"),
                result.getInt("position"), result.getString("concept_code_snapshot"),
                (Integer) result.getObject("novelty_level_snapshot"), result.getString("concept_snapshot"));
    }

    private AttemptState mapAttemptState(ResultSet result, int row) throws SQLException {
        return new AttemptState(result.getLong("challenge_session_id"), result.getLong("id"),
                AttemptType.valueOf(result.getString("attempt_type")), result.getString("status"),
                result.getObject("effective_date", LocalDate.class), (Integer) result.getObject("season_month"),
                (Long) result.getObject("attempt_seed"), result.getObject("operation_token", UUID.class),
                instant(result, "lease_expires_at"), result.getString("failure_reason_code"),
                result.getString("failure_detail"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static NextAction nextAction(String status) {
        return switch (status) {
            case "PENDING", "CONTEXT_READY" -> NextAction.WAIT_OR_RECOVER;
            case "GENERATED" -> NextAction.AWAIT_CURATION;
            default -> NextAction.NONE;
        };
    }

    private static String reasonJson(Iterable<GeneratorReasonCode> reasons) {
        List<String> values = new ArrayList<>();
        reasons.forEach(reason -> values.add(reason.name()));
        values.sort(String::compareTo);
        return "[" + values.stream().map(value -> "\"" + value + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";
    }

    record AttemptState(
            long sessionId,
            long attemptId,
            AttemptType attemptType,
            String status,
            LocalDate effectiveDate,
            Integer seasonMonth,
            Long attemptSeed,
            UUID operationToken,
            Instant leaseExpiresAt,
            String failureReason,
            String failureDetail
    ) {
        boolean leaseActive(Instant now) {
            return leaseExpiresAt != null && leaseExpiresAt().isAfter(now);
        }
    }

    record ManualRequirementRow(int position, String displayText, Long matchedConceptId) {
    }

    record PersistedBatch(long batchId, int batchNumber, String status, String fingerprint) {
    }

    private record HistoryRow(long challengeId, Instant shownAt, String status, long sessionId,
                              AttemptType attemptType, String profile, String noveltyBand,
                              String exclusionRuleCode, int position, String conceptCode,
                              Integer noveltyLevel, String conceptSnapshot) {
    }

    private static final class HistoryBuilder {
        private final HistoryRow first;
        private final List<VisibleRequirement> requirements = new ArrayList<>();

        private HistoryBuilder(HistoryRow first) {
            this.first = first;
        }

        private VisibleChallenge build() {
            return new VisibleChallenge(first.shownAt(), Long.toString(first.sessionId()), first.attemptType(),
                    first.status(), requirements,
                    first.profile() == null ? null
                            : io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile
                            .valueOf(first.profile()),
                    first.noveltyBand() == null ? null
                            : io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand
                            .valueOf(first.noveltyBand()),
                    first.exclusionRuleCode());
        }
    }

    static final class LostGenerationClaimException extends RuntimeException {
        LostGenerationClaimException(String message) {
            super(message);
        }
    }
}
