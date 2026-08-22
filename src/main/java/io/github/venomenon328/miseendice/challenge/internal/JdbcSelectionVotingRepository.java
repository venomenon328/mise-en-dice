package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.SelectionVotingCommands;
import io.github.venomenon328.miseendice.challenge.api.SelectionVotingQueries;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit JDBC persistence for the Phase-11B state owned by the challenge module. */
@Repository
class JdbcSelectionVotingRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcSelectionVotingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    boolean lockSession(long sessionId) {
        return !jdbcTemplate.queryForList(
                "select id from challenge_session where id = ? for update", Long.class, sessionId).isEmpty();
    }

    Optional<Participant> findParticipant(long participantId) {
        return jdbcTemplate.query("""
                select id, code, display_name, active from participant where id = ?
                """, this::mapParticipant, participantId).stream().findFirst();
    }

    List<ElectorateMember> electorate(long sessionId) {
        return jdbcTemplate.query("""
                select participant.id, participant.code, participant.display_name, participant.active,
                       electorate.snapshotted_at
                from selection_electorate electorate
                join participant on participant.id = electorate.participant_id
                where electorate.challenge_session_id = ?
                order by participant.code, participant.id
                """, this::mapElectorateMember, sessionId);
    }

    boolean sessionElectorateMaterialized(long sessionId) {
        Boolean materialized = jdbcTemplate.queryForObject("""
                select selection_electorate_materialized_at is not null
                from challenge_session where id = ?
                """, Boolean.class, sessionId);
        return Boolean.TRUE.equals(materialized);
    }

    void insertElectorate(long sessionId, List<Long> participantIds) {
        for (long participantId : participantIds) {
            jdbcTemplate.update("""
                    insert into selection_electorate (challenge_session_id, participant_id) values (?, ?)
                    """, sessionId, participantId);
        }
    }

    Optional<Round> findRound(long sessionId, int roundNumber) {
        return jdbcTemplate.query(roundSelect() + " where voting_round.challenge_session_id = ? and voting_round.round_number = ?",
                this::mapRound, sessionId, roundNumber).stream().findFirst();
    }

    Optional<Round> lockRound(long roundId) {
        return jdbcTemplate.query(roundSelect() + " where voting_round.id = ? for update", this::mapRound, roundId)
                .stream().findFirst();
    }

    Optional<Round> lockOpenRound(long sessionId) {
        return jdbcTemplate.query(roundSelect() + " where voting_round.challenge_session_id = ? and voting_round.status = 'OPEN' "
                        + "order by voting_round.round_number desc for update",
                this::mapRound, sessionId).stream().findFirst();
    }

    List<Round> rounds(long sessionId) {
        return jdbcTemplate.query(roundSelect() + " where voting_round.challenge_session_id = ? "
                        + "order by voting_round.round_number", this::mapRound, sessionId);
    }

    Round insertRound(long sessionId, int roundNumber, long offerSetId) {
        long roundId = jdbcTemplate.queryForObject("""
                insert into selection_voting_round (challenge_session_id, round_number, curated_offer_set_id)
                values (?, ?, ?) returning id
                """, Long.class, sessionId, roundNumber, offerSetId);
        return lockRound(roundId).orElseThrow(() -> new IllegalStateException("New voting round was not persisted"));
    }

    void upsertVote(long roundId, long participantId, SelectionVotingCommands.VoteChoice choice) {
        jdbcTemplate.update("""
                insert into selection_vote (voting_round_id, participant_id, option_type, curated_offer_id)
                values (?, ?, ?, ?)
                on conflict (voting_round_id, participant_id) do update
                   set option_type = excluded.option_type,
                       curated_offer_id = excluded.curated_offer_id,
                       updated_at = now()
                """, roundId, participantId, choice.type().name(), choice.offerId());
    }

    boolean allElectorateMembersVoted(long sessionId, long roundId) {
        Boolean result = jdbcTemplate.queryForObject("""
                select (select count(*) from selection_electorate where challenge_session_id = ?)
                       = (select count(*) from selection_vote where voting_round_id = ?)
                """, Boolean.class, sessionId, roundId);
        return Boolean.TRUE.equals(result);
    }

    List<Vote> votes(long roundId) {
        return jdbcTemplate.query("""
                select voting_round_id, participant_id, option_type, curated_offer_id, voted_at, updated_at
                from selection_vote where voting_round_id = ? order by participant_id
                """, this::mapVote, roundId);
    }

    void completeRound(long roundId, VotingRoundEvaluator.Evaluation evaluation) {
        int updated = jdbcTemplate.update("""
                update selection_voting_round
                   set status = 'COMPLETED', result_option_type = ?, result_curated_offer_id = ?,
                       tie_break_used = ?, completed_at = now()
                 where id = ? and status = 'OPEN'
                """, evaluation.winningChoice().type().name(), evaluation.winningChoice().offerId(),
                evaluation.tieBreakUsed(), roundId);
        if (updated != 1) {
            throw new IllegalStateException("Voting round completion changed while the session was locked");
        }
    }

    boolean markConfirmed(long roundId) {
        return updateApplyState(roundId, "apply_state = 'PENDING'", SelectionVotingQueries.ApplyState.CONFIRMED,
                null, null);
    }

    boolean recordRerollInProgress(long roundId, String detail) {
        return updateApplyState(roundId, "apply_state = 'PENDING'",
                SelectionVotingQueries.ApplyState.REROLL_IN_PROGRESS, null, detail);
    }

    boolean recordRerollOfferReady(long roundId, long resultingOfferSetId) {
        return updateApplyState(roundId, "apply_state in ('PENDING', 'REROLL_IN_PROGRESS')",
                SelectionVotingQueries.ApplyState.REROLL_OFFER_READY, resultingOfferSetId, null);
    }

    boolean recordRerollTerminal(long roundId, SelectionVotingQueries.ApplyState terminalState, String detail) {
        if (terminalState != SelectionVotingQueries.ApplyState.REROLL_EXHAUSTED
                && terminalState != SelectionVotingQueries.ApplyState.REROLL_FAILED) {
            throw new IllegalArgumentException("Only terminal reroll states are supported");
        }
        return updateApplyState(roundId, "apply_state in ('PENDING', 'REROLL_IN_PROGRESS')", terminalState,
                null, detail);
    }

    boolean markRerollAutoConfirmPending(long roundId, long resultingOfferSetId, String detail) {
        return jdbcTemplate.update("""
                update selection_voting_round
                   set apply_state = 'REROLL_AUTO_CONFIRM_PENDING', resulting_offer_set_id = ?,
                       apply_detail = ?, applied_at = now()
                 where id = ? and status = 'COMPLETED'
                   and apply_state = 'REROLL_OFFER_READY' and resulting_offer_set_id = ?
                """, resultingOfferSetId, detail, roundId, resultingOfferSetId) == 1;
    }

    boolean markRerollAutoConfirmed(long roundId, long resultingOfferSetId) {
        return jdbcTemplate.update("""
                update selection_voting_round
                   set apply_state = 'REROLL_AUTO_CONFIRMED', resulting_offer_set_id = ?,
                       apply_detail = null, applied_at = now()
                 where id = ? and status = 'COMPLETED'
                   and apply_state = 'REROLL_AUTO_CONFIRM_PENDING' and resulting_offer_set_id = ?
                """, resultingOfferSetId, roundId, resultingOfferSetId) == 1;
    }

    private boolean updateApplyState(long roundId, String expectedState, SelectionVotingQueries.ApplyState state,
                                     Long resultingOfferSetId, String detail) {
        return jdbcTemplate.update("""
                update selection_voting_round
                   set apply_state = ?, resulting_offer_set_id = ?, apply_detail = ?, applied_at = now()
                 where id = ? and status = 'COMPLETED' and %s
                """.formatted(expectedState), state.name(), resultingOfferSetId, detail, roundId) == 1;
    }

    SelectionSnapshot selection(long sessionId) {
        return new SelectionSnapshot(sessionId, electorate(sessionId), rounds(sessionId));
    }

    Optional<Identity> findIdentity(String provider, String externalSubject) {
        return jdbcTemplate.query("""
                select participant.id, participant.code, participant.display_name, participant.active,
                       identity.provider, identity.external_subject
                from participant_external_identity identity
                join participant on participant.id = identity.participant_id
                where identity.provider = ? and identity.external_subject = ?
                """, this::mapIdentity, provider, externalSubject).stream().findFirst();
    }

    void insertIdentity(long participantId, String provider, String externalSubject) {
        jdbcTemplate.update("""
                insert into participant_external_identity (participant_id, provider, external_subject)
                values (?, ?, ?)
                """, participantId, provider, externalSubject);
    }

    private Participant mapParticipant(ResultSet result, int row) throws SQLException {
        return new Participant(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"));
    }

    private ElectorateMember mapElectorateMember(ResultSet result, int row) throws SQLException {
        return new ElectorateMember(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"), instant(result, "snapshotted_at"));
    }

    private Round mapRound(ResultSet result, int row) throws SQLException {
        String resultType = result.getString("result_option_type");
        SelectionVotingCommands.VoteChoice resultChoice = resultType == null ? null
                : new SelectionVotingCommands.VoteChoice(
                        SelectionVotingCommands.VoteOptionType.valueOf(resultType),
                        (Long) result.getObject("result_curated_offer_id"));
        return new Round(result.getLong("id"), result.getLong("challenge_session_id"), result.getInt("round_number"),
                result.getLong("curated_offer_set_id"),
                SelectionVotingQueries.VotingRoundStatus.valueOf(result.getString("status")), resultChoice,
                result.getBoolean("tie_break_used"), instant(result, "completed_at"),
                SelectionVotingQueries.ApplyState.valueOf(result.getString("apply_state")),
                (Long) result.getObject("resulting_offer_set_id"), result.getString("apply_detail"),
                instant(result, "applied_at"));
    }

    private Vote mapVote(ResultSet result, int row) throws SQLException {
        return new Vote(result.getLong("voting_round_id"), result.getLong("participant_id"),
                new SelectionVotingCommands.VoteChoice(
                        SelectionVotingCommands.VoteOptionType.valueOf(result.getString("option_type")),
                        (Long) result.getObject("curated_offer_id")), instant(result, "voted_at"),
                instant(result, "updated_at"));
    }

    private Identity mapIdentity(ResultSet result, int row) throws SQLException {
        return new Identity(result.getLong("id"), result.getString("code"), result.getString("display_name"),
                result.getBoolean("active"), result.getString("provider"), result.getString("external_subject"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String roundSelect() {
        return """
                select voting_round.id, voting_round.challenge_session_id, voting_round.round_number,
                       voting_round.curated_offer_set_id, voting_round.status, voting_round.result_option_type,
                       voting_round.result_curated_offer_id, voting_round.tie_break_used, voting_round.completed_at,
                       voting_round.apply_state, voting_round.resulting_offer_set_id, voting_round.apply_detail,
                       voting_round.applied_at
                from selection_voting_round voting_round
                """;
    }

    record Participant(long participantId, String code, String displayName, boolean active) {
    }

    record ElectorateMember(long participantId, String code, String displayName, boolean active, Instant snapshottedAt) {
    }

    record Round(long roundId, long sessionId, int roundNumber, long offerSetId,
                 SelectionVotingQueries.VotingRoundStatus status, SelectionVotingCommands.VoteChoice resultChoice,
                 boolean tieBreakUsed, Instant completedAt, SelectionVotingQueries.ApplyState applyState,
                 Long resultingOfferSetId, String applyDetail, Instant appliedAt) {
    }

    record Vote(long roundId, long participantId, SelectionVotingCommands.VoteChoice choice,
                Instant votedAt, Instant updatedAt) {
    }

    record SelectionSnapshot(long sessionId, List<ElectorateMember> electorate, List<Round> rounds) {
    }

    record Identity(long participantId, String code, String displayName, boolean active,
                    String provider, String externalSubject) {
    }

}
