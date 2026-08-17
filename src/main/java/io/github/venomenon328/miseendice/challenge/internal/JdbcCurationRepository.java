package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CurationResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit JDBC adapter for the Phase-10A curation contract and offer lifecycle. */
@Repository
class JdbcCurationRepository {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcCurationRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    Attempt lockAttempt(long attemptId) {
        return jdbcTemplate.query("""
                select attempt.id, attempt.challenge_session_id, session.requested_offer_count,
                       attempt.status, attempt.curation_status, attempt.curation_terminal_reason_code,
                       attempt.curation_terminal_detail
                from generation_attempt attempt
                join challenge_session session on session.id = attempt.challenge_session_id
                where attempt.id = ? for update
                """, this::mapAttempt, attemptId).stream().findFirst().orElseThrow(
                () -> new IllegalArgumentException("Generation attempt does not exist"));
    }

    Optional<Round> findRoundForUpdate(long attemptId, int roundNumber) {
        return jdbcTemplate.query(roundSelect() + " where round_row.generation_attempt_id = ? and round_row.round_number = ?"
                        + " and not round_row.legacy_migrated for update",
                this::mapRound, attemptId, roundNumber).stream().findFirst();
    }

    Round findRoundForUpdate(long roundId) {
        return jdbcTemplate.query(roundSelect() + " where round_row.id = ? and not round_row.legacy_migrated for update", this::mapRound, roundId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Curation round does not exist"));
    }

    Optional<Round> findRound(long attemptId, int roundNumber) {
        return jdbcTemplate.query(roundSelect() + " where round_row.generation_attempt_id = ? and round_row.round_number = ?"
                        + " and not round_row.legacy_migrated",
                this::mapRound, attemptId, roundNumber).stream().findFirst();
    }

    Optional<Round> findRoundById(long roundId) {
        return jdbcTemplate.query(roundSelect() + " where round_row.id = ? and not round_row.legacy_migrated", this::mapRound, roundId)
                .stream().findFirst();
    }

    long nextRoundId() {
        return jdbcTemplate.queryForObject(
                "select nextval(pg_get_serial_sequence('curation_round', 'id'))", Long.class);
    }

    void insertRound(long roundId, long attemptId, int roundNumber, long primaryBatchId,
                     CurationModel.RequestPurpose purpose, String curatorModel, String promptVersion,
                     String requestPayload) {
        jdbcTemplate.update("""
                insert into curation_round (
                    id, generation_attempt_id, round_number, curator_model, prompt_version, status,
                    request_payload, legacy_migrated, primary_generation_batch_id, request_purpose, contract_version
                ) values (?, ?, ?, ?, ?, 'PENDING', cast(? as jsonb), false, ?, ?, 'CURATION_CONTRACT_V1')
                """, roundId, attemptId, roundNumber, curatorModel, promptVersion, requestPayload,
                primaryBatchId, purpose.name());
    }

    long insertRoundCandidate(long roundId, long candidateId, int requestPosition,
                              CurationModel.Participation participation, Long sourceRoundCandidateId) {
        return jdbcTemplate.queryForObject("""
                insert into curation_round_candidate (
                    curation_round_id, challenge_candidate_id, request_position, participation_type,
                    source_round_candidate_id
                ) values (?, ?, ?, ?, ?) returning id
                """, Long.class, roundId, candidateId, requestPosition, participation.name(), sourceRoundCandidateId);
    }

    void markAttemptCurationStatus(long attemptId, String status) {
        jdbcTemplate.update("update generation_attempt set curation_status = ? where id = ?", status, attemptId);
    }

    void completeRound(Round round, String responsePayload, List<CurationResponse.CandidateEvaluation> evaluations) {
        for (CurationResponse.CandidateEvaluation evaluation : evaluations) {
            int updated = jdbcTemplate.update("""
                    update curation_round_candidate
                    set evaluation_class = ?, evaluation_rank = ?, reason_codes = cast(? as jsonb),
                        diagnostics = cast(? as jsonb)
                    where curation_round_id = ? and challenge_candidate_id = ?
                    """, evaluation.evaluation().name(), evaluation.rank(), json(evaluation.reasonCodes()),
                    json(evaluation.diagnostics()), round.id(), evaluation.candidateId());
            if (updated != 1) {
                throw new IllegalStateException("Validated curation candidate was not found while completing a round");
            }
        }
        jdbcTemplate.update("""
                update curation_round
                set status = 'COMPLETED', response_payload = cast(? as jsonb), completed_at = now()
                where id = ? and status = 'PENDING'
                """, responsePayload, round.id());
        markAttemptCurationStatus(round.attemptId(), "RESPONSE_RECORDED");
    }

    void invalidateRound(Round round, String responsePayload, String reasonCode, String detail) {
        jdbcTemplate.update("""
                update curation_round
                set status = 'INVALID_RESPONSE', response_payload = cast(? as jsonb),
                    terminal_reason_code = ?, terminal_detail = ?, completed_at = now()
                where id = ? and status = 'PENDING'
                """, responsePayload, reasonCode, detail, round.id());
        markAttemptCurationStatus(round.attemptId(), "FAILED");
    }

    void recordTechnicalFailure(Round round, String reasonCode, String detail) {
        jdbcTemplate.update("""
                update curation_round
                set status = 'TECHNICAL_ERROR', terminal_reason_code = ?, terminal_detail = ?, completed_at = now()
                where id = ? and status = 'PENDING'
                """, reasonCode, detail, round.id());
        markAttemptCurationStatus(round.attemptId(), "FAILED");
    }

    boolean hasGoodEvaluation(long attemptId) {
        Boolean value = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from curation_round_candidate participation
                    join curation_round round_row on round_row.id = participation.curation_round_id
                    where round_row.generation_attempt_id = ? and round_row.status = 'COMPLETED'
                      and participation.evaluation_class = 'GOOD'
                )
                """, Boolean.class, attemptId);
        return Boolean.TRUE.equals(value);
    }

    void markAttemptExhausted(long attemptId, String reasonCode, String detail) {
        jdbcTemplate.update("""
                update generation_attempt
                set curation_status = 'EXHAUSTED', curation_terminal_reason_code = ?, curation_terminal_detail = ?
                where id = ?
                """, reasonCode, detail, attemptId);
    }

    Optional<CurationQueries.OfferSetView> findOfferSet(long attemptId) {
        return jdbcTemplate.query("""
                select id, generation_attempt_id, requested_offer_count, status, selection_path::text,
                       curated_at, presented_at, decided_at
                from curated_offer_set where generation_attempt_id = ?
                """, (result, row) -> offerSet(result), attemptId).stream().findFirst();
    }

    long insertOfferSet(long attemptId, int requestedOfferCount, String selectionPath) {
        return jdbcTemplate.queryForObject("""
                insert into curated_offer_set (generation_attempt_id, requested_offer_count, selection_path)
                values (?, ?, cast(? as jsonb)) returning id
                """, Long.class, attemptId, requestedOfferCount, selectionPath);
    }

    void insertOffer(long offerSetId, int position, long candidateId, long curationRoundCandidateId) {
        jdbcTemplate.update("""
                insert into curated_offer (curated_offer_set_id, position, challenge_candidate_id,
                                           curation_round_candidate_id)
                values (?, ?, ?, ?)
                """, offerSetId, position, candidateId, curationRoundCandidateId);
    }

    CurationRequest.CandidateSnapshot candidateSnapshot(long candidateId) {
        return jdbcTemplate.query("""
                select id, candidate_number, profile, target_specificity, target_novelty_band,
                       actual_novelty_band, known_novelty_load, total_score, data_confidence,
                       canonical_signature, component_scores::text, generator_reason_codes::text,
                       generator_diagnostics::text
                from challenge_candidate where id = ?
                """, (result, row) -> snapshot(result), candidateId).stream().findFirst().orElseThrow(
                () -> new IllegalArgumentException("Challenge candidate does not exist"));
    }

    List<RoundCandidate> roundCandidates(long roundId) {
        return jdbcTemplate.query("""
                select id, challenge_candidate_id, request_position, participation_type,
                       source_round_candidate_id, evaluation_class, evaluation_rank,
                       reason_codes::text, diagnostics::text
                from curation_round_candidate where curation_round_id = ? order by request_position
                """, (result, row) -> new RoundCandidate(
                result.getLong("id"), result.getLong("challenge_candidate_id"), result.getInt("request_position"),
                CurationModel.Participation.valueOf(result.getString("participation_type")),
                (Long) result.getObject("source_round_candidate_id"), nullableEvaluation(result, "evaluation_class"),
                (Integer) result.getObject("evaluation_rank"), strings(result.getString("reason_codes")),
                result.getString("diagnostics"), candidateSnapshot(result.getLong("challenge_candidate_id"))), roundId);
    }

    CurationQueries.AttemptView attemptView(long attemptId) {
        Attempt attempt = jdbcTemplate.query("""
                select attempt.id, attempt.challenge_session_id, session.requested_offer_count,
                       attempt.status, attempt.curation_status, attempt.curation_terminal_reason_code,
                       attempt.curation_terminal_detail
                from generation_attempt attempt join challenge_session session on session.id = attempt.challenge_session_id
                where attempt.id = ?
                """, this::mapAttempt, attemptId).stream().findFirst().orElseThrow();
        List<CurationQueries.RoundSummary> rounds = jdbcTemplate.query("""
                select id, round_number, status, request_purpose, primary_generation_batch_id, completed_at
                from curation_round where generation_attempt_id = ? and not legacy_migrated order by round_number
                """, (result, row) -> new CurationQueries.RoundSummary(
                result.getLong("id"), result.getInt("round_number"),
                CurationModel.RoundStatus.valueOf(result.getString("status")),
                CurationModel.RequestPurpose.valueOf(result.getString("request_purpose")),
                result.getLong("primary_generation_batch_id"), instant(result, "completed_at")), attemptId);
        return new CurationQueries.AttemptView(attempt.id(), attempt.sessionId(), attempt.requestedOfferCount(),
                attempt.curationStatus(), attempt.terminalReasonCode(), attempt.terminalDetail(), rounds,
                findOfferSet(attemptId).orElse(null));
    }

    CurationQueries.RoundView roundView(Round round) {
        CurationRequest request = readRequest(round.requestPayload());
        List<CurationQueries.RoundCandidateView> candidates = roundCandidates(round.id()).stream().map(candidate ->
                new CurationQueries.RoundCandidateView(candidate.id(), candidate.candidateId(), candidate.requestPosition(),
                        candidate.participation(), candidate.sourceRoundCandidateId(), candidate.evaluation(), candidate.rank(),
                        candidate.reasonCodes(), candidate.diagnosticsJson(), candidate.snapshot())).toList();
        return new CurationQueries.RoundView(round.id(), round.attemptId(), round.roundNumber(), round.primaryBatchId(),
                round.curatorModel(), round.promptVersion(), round.purpose(), round.status(), request,
                round.requestPayload(), round.responsePayload(), round.terminalReasonCode(), round.terminalDetail(),
                round.createdAt(), round.completedAt(), candidates);
    }

    private CurationQueries.OfferSetView offerSet(ResultSet result) throws SQLException {
        long id = result.getLong("id");
        List<CurationQueries.OfferView> offers = jdbcTemplate.query("""
                select offer.position, offer.challenge_candidate_id, offer.curation_round_candidate_id,
                       participation.evaluation_class, participation.evaluation_rank
                from curated_offer offer
                join curation_round_candidate participation on participation.id = offer.curation_round_candidate_id
                where offer.curated_offer_set_id = ? order by offer.position
                """, (row, number) -> new CurationQueries.OfferView(row.getInt("position"),
                row.getLong("challenge_candidate_id"), row.getLong("curation_round_candidate_id"),
                CurationModel.Evaluation.valueOf(row.getString("evaluation_class")),
                (Integer) row.getObject("evaluation_rank"), candidateSnapshot(row.getLong("challenge_candidate_id"))), id);
        return new CurationQueries.OfferSetView(id, result.getLong("generation_attempt_id"),
                result.getInt("requested_offer_count"), CurationModel.OfferSetStatus.valueOf(result.getString("status")),
                result.getString("selection_path"), instant(result, "curated_at"), instant(result, "presented_at"),
                instant(result, "decided_at"), offers);
    }

    private CurationRequest.CandidateSnapshot snapshot(ResultSet result) throws SQLException {
        long candidateId = result.getLong("id");
        List<CurationRequest.RequirementSnapshot> requirements = jdbcTemplate.query("""
                select position, source, ingredient_concept_id, manual_requirement_id, concept_code_snapshot,
                       display_text_snapshot, challenge_specificity_snapshot, novelty_level_snapshot,
                       concept_snapshot::text, weight_evaluation_snapshot::text, generator_reason_codes::text
                from candidate_requirement where candidate_id = ? order by position
                """, (row, number) -> new CurationRequest.RequirementSnapshot(row.getInt("position"),
                row.getString("source"), (Long) row.getObject("ingredient_concept_id"),
                (Long) row.getObject("manual_requirement_id"), row.getString("concept_code_snapshot"),
                row.getString("display_text_snapshot"), row.getString("challenge_specificity_snapshot"),
                (Integer) row.getObject("novelty_level_snapshot"), row.getString("concept_snapshot"),
                row.getString("weight_evaluation_snapshot"), row.getString("generator_reason_codes")), candidateId);
        return new CurationRequest.CandidateSnapshot(result.getInt("candidate_number"), result.getString("profile"),
                (Integer) result.getObject("target_specificity"), result.getString("target_novelty_band"),
                result.getString("actual_novelty_band"), (Integer) result.getObject("known_novelty_load"),
                result.getBigDecimal("total_score"), result.getBigDecimal("data_confidence"),
                result.getString("canonical_signature"), result.getString("component_scores"),
                result.getString("generator_reason_codes"), result.getString("generator_diagnostics"), requirements);
    }

    String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Curation contract is not JSON serializable", exception);
        }
    }

    boolean sameJson(String left, String right) {
        try {
            return objectMapper.readValue(left, Object.class).equals(objectMapper.readValue(right, Object.class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted curation JSON is invalid", exception);
        }
    }

    private CurationRequest readRequest(String json) {
        try {
            return objectMapper.readValue(json, CurationRequest.class);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted curation request is invalid", exception);
        }
    }

    private List<String> strings(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Persisted curation reason codes are invalid", exception);
        }
    }

    private Round mapRound(ResultSet result, int row) throws SQLException {
        return new Round(result.getLong("id"), result.getLong("generation_attempt_id"), result.getInt("round_number"),
                result.getLong("primary_generation_batch_id"), result.getString("curator_model"),
                result.getString("prompt_version"), CurationModel.RequestPurpose.valueOf(result.getString("request_purpose")),
                CurationModel.RoundStatus.valueOf(result.getString("status")), result.getString("request_payload"),
                result.getString("response_payload"), result.getString("terminal_reason_code"),
                result.getString("terminal_detail"), instant(result, "created_at"), instant(result, "completed_at"));
    }

    private Attempt mapAttempt(ResultSet result, int row) throws SQLException {
        return new Attempt(result.getLong("id"), result.getLong("challenge_session_id"),
                result.getInt("requested_offer_count"), result.getString("status"), result.getString("curation_status"),
                result.getString("curation_terminal_reason_code"), result.getString("curation_terminal_detail"));
    }

    private static CurationModel.Evaluation nullableEvaluation(ResultSet result, String column) throws SQLException {
        String value = result.getString(column);
        return value == null ? null : CurationModel.Evaluation.valueOf(value);
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String roundSelect() {
        return """
                select round_row.id, round_row.generation_attempt_id, round_row.round_number,
                       round_row.primary_generation_batch_id, round_row.curator_model, round_row.prompt_version,
                       round_row.request_purpose, round_row.status, round_row.request_payload::text,
                       round_row.response_payload::text, round_row.terminal_reason_code, round_row.terminal_detail,
                       round_row.created_at, round_row.completed_at
                from curation_round round_row
                """;
    }

    record Attempt(long id, long sessionId, int requestedOfferCount, String generationStatus, String curationStatus,
                   String terminalReasonCode, String terminalDetail) {
    }

    record Round(long id, long attemptId, int roundNumber, long primaryBatchId, String curatorModel,
                 String promptVersion, CurationModel.RequestPurpose purpose, CurationModel.RoundStatus status,
                 String requestPayload, String responsePayload, String terminalReasonCode, String terminalDetail,
                 Instant createdAt, Instant completedAt) {
    }

    record RoundCandidate(long id, long candidateId, int requestPosition, CurationModel.Participation participation,
                          Long sourceRoundCandidateId, CurationModel.Evaluation evaluation, Integer rank,
                          List<String> reasonCodes, String diagnosticsJson, CurationRequest.CandidateSnapshot snapshot) {
    }
}
