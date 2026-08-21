package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.CurationModel;
import io.github.venomenon328.miseendice.challenge.api.CurationRequest;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.CandidateRestriction;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionQueries;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Explicit JDBC persistence for the Phase-11A offer-decision state machine. */
@Repository
class JdbcOfferDecisionRepository {
    private final JdbcTemplate jdbcTemplate;

    JdbcOfferDecisionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    OfferSet lockOfferSet(long offerSetId) {
        return jdbcTemplate.query(offerSetSelect() + " where offer_set.id = ? for update", this::mapOfferSet, offerSetId)
                .stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Curated offer set does not exist"));
    }

    Optional<OfferSet> findOfferSet(long offerSetId) {
        return jdbcTemplate.query(offerSetSelect() + " where offer_set.id = ?", this::mapOfferSet, offerSetId)
                .stream().findFirst();
    }

    boolean lockSession(long sessionId) {
        return !jdbcTemplate.queryForList(
                "select id from challenge_session where id = ? for update", Long.class, sessionId).isEmpty();
    }

    boolean rerollConsumed(long sessionId) {
        Boolean result = jdbcTemplate.queryForObject("""
                select exists (
                    select 1 from reroll_offer_exposure where challenge_session_id = ?
                )
                """, Boolean.class, sessionId);
        return Boolean.TRUE.equals(result);
    }

    Optional<Offer> findOffer(long offerSetId, long offerId) {
        return jdbcTemplate.query("""
                select offer.id, offer.curated_offer_set_id, offer.position, offer.challenge_candidate_id,
                       offer.restriction_rule_id, offer.restriction_rule_code_snapshot, offer.restriction_text_snapshot
                from curated_offer offer where offer.id = ? and offer.curated_offer_set_id = ?
                """, this::mapOffer, offerId, offerSetId).stream().findFirst();
    }

    Optional<Challenge> challengeForOfferSet(long offerSetId) {
        return jdbcTemplate.query("""
                select challenge.id, challenge.curated_offer_id, challenge.selected_candidate_id,
                       challenge.shown_at, challenge.status
                from challenge
                join curated_offer offer on offer.id = challenge.curated_offer_id
                where offer.curated_offer_set_id = ?
                """, this::mapChallenge, offerSetId).stream().findFirst();
    }

    void markPresented(long offerSetId) {
        int updated = jdbcTemplate.update("""
                update curated_offer_set
                set status = 'PRESENTED_PENDING_DECISION', presented_at = now()
                where id = ? and status = 'CURATED_UNPRESENTED'
                """, offerSetId);
        if (updated != 1) {
            throw new IllegalStateException("Offer set presentation state changed while locked");
        }
    }

    void markConfirmed(long offerSetId) {
        int updated = jdbcTemplate.update("""
                update curated_offer_set set status = 'CONFIRMED', decided_at = now()
                where id = ? and status = 'PRESENTED_PENDING_DECISION'
                """, offerSetId);
        if (updated != 1) {
            throw new IllegalStateException("Offer set confirmation state changed while locked");
        }
    }

    long insertChallenge(OfferSet offerSet, Offer offer) {
        Long challengeNumber = jdbcTemplate.queryForObject("""
                update challenge_archive_counter
                   set last_challenge_number = last_challenge_number + 1
                 where singleton = true
                returning last_challenge_number
                """, Long.class);
        if (challengeNumber == null) {
            throw new IllegalStateException("Challenge archive counter is missing");
        }
        return jdbcTemplate.queryForObject("""
                insert into challenge (generation_attempt_id, selected_candidate_id, curated_offer_id,
                                       restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot,
                                       challenge_number)
                select ?, ?, offer.id, offer.restriction_rule_id, offer.restriction_rule_code_snapshot,
                       offer.restriction_text_snapshot, ?
                from curated_offer offer where offer.id = ? returning id
                """, Long.class, offerSet.attemptId(), offer.candidateId(), challengeNumber, offer.offerId());
    }

    void markRerolled(long offerSetId) {
        int updated = jdbcTemplate.update("""
                update curated_offer_set set status = 'REROLLED', decided_at = now()
                where id = ? and status = 'PRESENTED_PENDING_DECISION'
                """, offerSetId);
        if (updated != 1) {
            throw new IllegalStateException("Offer set reroll state changed while locked");
        }
    }

    Optional<Exposure> exposureForOfferSet(long offerSetId) {
        return jdbcTemplate.query("""
                select id, challenge_session_id, curated_offer_set_id, exposed_at
                from reroll_offer_exposure where curated_offer_set_id = ?
                """, this::mapExposure, offerSetId).stream().findFirst();
    }

    long insertExposure(OfferSet offerSet) {
        long exposureId = jdbcTemplate.queryForObject("""
                insert into reroll_offer_exposure (challenge_session_id, curated_offer_set_id)
                values (?, ?) returning id
                """, Long.class, offerSet.sessionId(), offerSet.offerSetId());
        int copied = jdbcTemplate.update("""
                insert into reroll_offer_exposure_requirement (
                    reroll_offer_exposure_id, curated_offer_id, challenge_candidate_id, requirement_position,
                    source, ingredient_concept_id, concept_code_snapshot, display_text_snapshot
                )
                select ?, offer.id, offer.challenge_candidate_id, requirement.position,
                       requirement.source, requirement.ingredient_concept_id, requirement.concept_code_snapshot,
                       requirement.display_text_snapshot
                from curated_offer offer
                join candidate_requirement requirement on requirement.candidate_id = offer.challenge_candidate_id
                where offer.curated_offer_set_id = ?
                order by offer.position, requirement.position
                """, exposureId, offerSet.offerSetId());
        if (copied != offerSet.requestedOfferCount() * 4) {
            throw new IllegalStateException("Presented offer set does not have complete requirement snapshots");
        }
        int copiedRestrictions = jdbcTemplate.update("""
                insert into reroll_offer_exposure_restriction (
                    reroll_offer_exposure_id, curated_offer_id, challenge_candidate_id,
                    restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
                )
                select ?, offer.id, offer.challenge_candidate_id,
                       offer.restriction_rule_id, offer.restriction_rule_code_snapshot, offer.restriction_text_snapshot
                from curated_offer offer
                where offer.curated_offer_set_id = ?
                order by offer.position
                """, exposureId, offerSet.offerSetId());
        if (copiedRestrictions != offerSet.requestedOfferCount()) {
            throw new IllegalStateException("Presented offer set does not have complete restriction snapshots");
        }
        return exposureId;
    }

    Optional<OfferDecisionQueries.OfferSetView> offerSetView(long offerSetId) {
        return findOfferSet(offerSetId).map(this::view);
    }

    Optional<OfferDecisionQueries.SessionDecisionView> sessionView(long sessionId) {
        return jdbcTemplate.query("""
                select session.id, session.requested_offer_count,
                       max(offer_set.id) filter (where offer_set.status = 'PRESENTED_PENDING_DECISION') as pending_offer_set_id,
                       max(offer_set.id) filter (where offer_set.status = 'CONFIRMED') as confirmed_offer_set_id,
                       max(offer_set.id) filter (where offer_set.status = 'REROLLED') as rerolled_offer_set_id,
                       max(challenge.id) as confirmed_challenge_id,
                       max(reroll_attempt.id) as reroll_attempt_id,
                       exists (select 1 from reroll_offer_exposure exposure
                               where exposure.challenge_session_id = session.id) as reroll_consumed
                from challenge_session session
                left join generation_attempt attempt on attempt.challenge_session_id = session.id
                left join curated_offer_set offer_set on offer_set.generation_attempt_id = attempt.id
                left join curated_offer offer on offer.curated_offer_set_id = offer_set.id
                left join challenge on challenge.curated_offer_id = offer.id
                left join generation_attempt reroll_attempt on reroll_attempt.challenge_session_id = session.id
                    and reroll_attempt.attempt_type = 'REROLL'
                where session.id = ?
                group by session.id, session.requested_offer_count
                """, (result, row) -> new OfferDecisionQueries.SessionDecisionView(
                result.getLong("id"), result.getInt("requested_offer_count"),
                (Long) result.getObject("pending_offer_set_id"), (Long) result.getObject("confirmed_offer_set_id"),
                (Long) result.getObject("rerolled_offer_set_id"), (Long) result.getObject("confirmed_challenge_id"),
                (Long) result.getObject("reroll_attempt_id"), result.getBoolean("reroll_consumed")), sessionId)
                .stream().findFirst();
    }

    Optional<OfferDecisionQueries.RerollExposureView> exposureView(long offerSetId) {
        return exposureForOfferSet(offerSetId).map(exposure -> new OfferDecisionQueries.RerollExposureView(
                exposure.exposureId(), exposure.sessionId(), exposure.offerSetId(), exposure.exposedAt(),
                jdbcTemplate.query("""
                        select requirement.curated_offer_id, requirement.challenge_candidate_id,
                               requirement.requirement_position, requirement.source,
                               requirement.ingredient_concept_id, requirement.concept_code_snapshot,
                               requirement.display_text_snapshot
                        from reroll_offer_exposure_requirement requirement
                        join curated_offer offer on offer.id = requirement.curated_offer_id
                        where requirement.reroll_offer_exposure_id = ?
                        order by offer.position, requirement.requirement_position
                        """, (result, row) -> new OfferDecisionQueries.ExposedRequirementView(
                        result.getLong("curated_offer_id"), result.getLong("challenge_candidate_id"),
                        result.getInt("requirement_position"), result.getString("source"),
                        (Long) result.getObject("ingredient_concept_id"), result.getString("concept_code_snapshot"),
                        result.getString("display_text_snapshot")), exposure.exposureId()),
                jdbcTemplate.query("""
                        select restriction.curated_offer_id, restriction.challenge_candidate_id,
                               restriction.restriction_rule_id, restriction.restriction_rule_code_snapshot,
                               restriction.restriction_text_snapshot
                        from reroll_offer_exposure_restriction restriction
                        join curated_offer offer on offer.id = restriction.curated_offer_id
                        where restriction.reroll_offer_exposure_id = ?
                        order by offer.position
                        """, (result, row) -> new OfferDecisionQueries.ExposedRestrictionView(
                        result.getLong("curated_offer_id"), result.getLong("challenge_candidate_id"),
                        new CandidateRestriction((Long) result.getObject("restriction_rule_id"),
                                result.getString("restriction_rule_code_snapshot"),
                                result.getString("restriction_text_snapshot"))), exposure.exposureId())));
    }

    private OfferDecisionQueries.OfferSetView view(OfferSet offerSet) {
        Challenge challenge = challengeForOfferSet(offerSet.offerSetId()).orElse(null);
        OfferDecisionQueries.ChallengeView challengeView = challenge == null ? null
                : new OfferDecisionQueries.ChallengeView(challenge.challengeId(), challenge.offerId(),
                challenge.candidateId(), challenge.shownAt(), challenge.status());
        return new OfferDecisionQueries.OfferSetView(offerSet.sessionId(), offerSet.attemptId(), offerSet.offerSetId(),
                offerSet.requestedOfferCount(), offerSet.status(), offerSet.curatedAt(), offerSet.presentedAt(),
                offerSet.decidedAt(), challengeView, jdbcTemplate.query("""
                        select offer.id, offer.curated_offer_set_id, offer.position, offer.challenge_candidate_id,
                               offer.restriction_rule_id, offer.restriction_rule_code_snapshot,
                               offer.restriction_text_snapshot
                        from curated_offer offer
                        where offer.curated_offer_set_id = ? order by offer.position
                        """, (result, row) -> {
                    Offer offer = mapOffer(result, row);
                    return new OfferDecisionQueries.OfferView(offer.offerId(), offer.position(), offer.candidateId(),
                            requirements(offer.candidateId()), new CandidateRestriction(offer.restrictionRuleId(),
                            offer.restrictionRuleCodeSnapshot(), offer.restrictionTextSnapshot()));
                }, offerSet.offerSetId()));
    }

    private List<CurationRequest.RequirementSnapshot> requirements(long candidateId) {
        return jdbcTemplate.query("""
                select position, source, ingredient_concept_id, manual_requirement_id, concept_code_snapshot,
                       display_text_snapshot, challenge_specificity_snapshot, novelty_level_snapshot,
                       concept_snapshot::text, weight_evaluation_snapshot::text, generator_reason_codes::text
                from candidate_requirement where candidate_id = ? order by position
                """, (result, row) -> new CurationRequest.RequirementSnapshot(result.getInt("position"),
                result.getString("source"), (Long) result.getObject("ingredient_concept_id"),
                (Long) result.getObject("manual_requirement_id"), result.getString("concept_code_snapshot"),
                result.getString("display_text_snapshot"), result.getString("challenge_specificity_snapshot"),
                (Integer) result.getObject("novelty_level_snapshot"), result.getString("concept_snapshot"),
                result.getString("weight_evaluation_snapshot"), result.getString("generator_reason_codes")), candidateId);
    }

    private OfferSet mapOfferSet(ResultSet result, int row) throws SQLException {
        return new OfferSet(result.getLong("id"), result.getLong("challenge_session_id"),
                result.getLong("generation_attempt_id"), result.getInt("requested_offer_count"),
                CurationModel.OfferSetStatus.valueOf(result.getString("status")), instant(result, "curated_at"),
                instant(result, "presented_at"), instant(result, "decided_at"));
    }

    private Offer mapOffer(ResultSet result, int row) throws SQLException {
        return new Offer(result.getLong("id"), result.getLong("curated_offer_set_id"), result.getInt("position"),
                result.getLong("challenge_candidate_id"), (Long) result.getObject("restriction_rule_id"),
                result.getString("restriction_rule_code_snapshot"),
                result.getString("restriction_text_snapshot"));
    }

    private Challenge mapChallenge(ResultSet result, int row) throws SQLException {
        return new Challenge(result.getLong("id"), result.getLong("curated_offer_id"),
                result.getLong("selected_candidate_id"), instant(result, "shown_at"), result.getString("status"));
    }

    private Exposure mapExposure(ResultSet result, int row) throws SQLException {
        return new Exposure(result.getLong("id"), result.getLong("challenge_session_id"),
                result.getLong("curated_offer_set_id"), instant(result, "exposed_at"));
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        OffsetDateTime value = result.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static String offerSetSelect() {
        return """
                select offer_set.id, attempt.challenge_session_id, offer_set.generation_attempt_id,
                       offer_set.requested_offer_count, offer_set.status, offer_set.curated_at,
                       offer_set.presented_at, offer_set.decided_at
                from curated_offer_set offer_set
                join generation_attempt attempt on attempt.id = offer_set.generation_attempt_id
                """;
    }

    record OfferSet(long offerSetId, long sessionId, long attemptId, int requestedOfferCount,
                    CurationModel.OfferSetStatus status, Instant curatedAt, Instant presentedAt, Instant decidedAt) {
    }

    record Offer(long offerId, long offerSetId, int position, long candidateId, Long restrictionRuleId,
                 String restrictionRuleCodeSnapshot, String restrictionTextSnapshot) {
    }

    record Challenge(long challengeId, long offerId, long candidateId, Instant shownAt, String status) {
    }

    record Exposure(long exposureId, long sessionId, long offerSetId, Instant exposedAt) {
    }
}
