--liquibase formatted sql
--changeset venomenon328:006-curation-state-machine-hardening splitStatements:false

-- Phase 10A hardening. Migration 005 remains immutable; this changeset adds the
-- state-machine and request-shape guarantees required for persisted curator work.

ALTER TABLE curation_round
    ADD COLUMN open_offer_slots smallint,
    ADD COLUMN invalid_response_original_payload text;

-- 005 requests can already exist in an upgraded installation. Derive the new
-- relational request fields exclusively from the persisted, same-attempt data.
UPDATE curation_round round_row
SET open_offer_slots = (round_row.request_payload ->> 'openOfferSlots')::smallint,
    request_payload = round_row.request_payload || jsonb_build_object(
        'promptVersion', round_row.prompt_version,
        'attemptExclusion', jsonb_build_object(
            'exclusionRuleId', attempt.exclusion_rule_id,
            'exclusionTextSnapshot', attempt.exclusion_text_snapshot
        )
    )
FROM generation_attempt attempt
WHERE attempt.id = round_row.generation_attempt_id
  AND NOT round_row.legacy_migrated
  AND round_row.request_payload IS NOT NULL
  AND (round_row.request_payload ->> 'openOfferSlots') ~ '^[1-3]$';

ALTER TABLE curation_round
    DROP CONSTRAINT ck_curation_round_terminal_detail,
    ADD CONSTRAINT ck_curation_round_open_offer_slots
        CHECK (legacy_migrated OR open_offer_slots BETWEEN 1 AND 3),
    ADD CONSTRAINT ck_curation_round_terminal_payload
        CHECK (
            (status = 'INVALID_RESPONSE' AND terminal_reason_code IS NOT NULL
                AND ((response_payload IS NOT NULL AND invalid_response_original_payload IS NULL)
                     OR (response_payload IS NULL AND invalid_response_original_payload IS NOT NULL)))
            OR (status = 'TECHNICAL_ERROR' AND terminal_reason_code IS NOT NULL
                AND invalid_response_original_payload IS NULL)
            OR (status NOT IN ('TECHNICAL_ERROR', 'INVALID_RESPONSE')
                AND terminal_reason_code IS NULL AND terminal_detail IS NULL
                AND invalid_response_original_payload IS NULL)
        );

CREATE OR REPLACE FUNCTION validate_generation_attempt_curation_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.curation_status = OLD.curation_status THEN
        RETURN NEW;
    END IF;
    IF OLD.curation_status IN ('OFFER_READY', 'EXHAUSTED', 'LEGACY') THEN
        RAISE EXCEPTION 'terminal curation status % cannot transition to %', OLD.curation_status, NEW.curation_status;
    END IF;
    IF OLD.curation_status = 'NOT_STARTED'
       AND NEW.curation_status NOT IN ('REQUEST_PENDING', 'EXHAUSTED') THEN
        RAISE EXCEPTION 'invalid initial curation transition to %', NEW.curation_status;
    END IF;
    IF OLD.curation_status = 'REQUEST_PENDING'
       AND NEW.curation_status NOT IN ('RESPONSE_RECORDED', 'FAILED') THEN
        RAISE EXCEPTION 'a pending curation request may only record a response or failure';
    END IF;
    IF OLD.curation_status = 'RESPONSE_RECORDED'
       AND NEW.curation_status NOT IN ('REQUEST_PENDING', 'OFFER_READY', 'EXHAUSTED') THEN
        RAISE EXCEPTION 'invalid recorded-response curation transition to %', NEW.curation_status;
    END IF;
    IF OLD.curation_status = 'FAILED'
       AND NEW.curation_status NOT IN ('REQUEST_PENDING', 'OFFER_READY', 'EXHAUSTED') THEN
        RAISE EXCEPTION 'invalid failed-curation transition to %', NEW.curation_status;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_generation_attempt_curation_transition
BEFORE UPDATE OF curation_status ON generation_attempt
FOR EACH ROW
EXECUTE FUNCTION validate_generation_attempt_curation_transition();

CREATE OR REPLACE FUNCTION validate_curation_round_state_transition()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.legacy_migrated THEN
        RETURN NEW;
    END IF;
    IF OLD.status = 'PENDING'
       AND NEW.status NOT IN ('PENDING', 'COMPLETED', 'TECHNICAL_ERROR', 'INVALID_RESPONSE') THEN
        RAISE EXCEPTION 'pending curation round % has an invalid terminal status %', OLD.id, NEW.status;
    END IF;
    IF OLD.status <> 'PENDING' AND NEW.status <> OLD.status THEN
        RAISE EXCEPTION 'terminal curation round % cannot transition from % to %', OLD.id, OLD.status, NEW.status;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_curation_round_state_transition
BEFORE UPDATE OF status ON curation_round
FOR EACH ROW
EXECUTE FUNCTION validate_curation_round_state_transition();

CREATE OR REPLACE FUNCTION assert_curation_round_request_shape(target_round_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    round_attempt_id bigint;
    current_round_number smallint;
    current_purpose text;
    current_primary_batch_id bigint;
    current_open_offer_slots smallint;
    current_legacy boolean;
    requested_count smallint;
    primary_batch_number smallint;
    previous_round_id bigint;
    previous_status text;
    previous_primary_batch_id bigint;
    previous_good_count integer;
    total_count integer;
    new_count integer;
    carry_count integer;
    locked_count integer;
BEGIN
    SELECT round_row.generation_attempt_id, round_row.round_number, round_row.request_purpose,
           round_row.primary_generation_batch_id, round_row.open_offer_slots, round_row.legacy_migrated,
           session.requested_offer_count
      INTO round_attempt_id, current_round_number, current_purpose, current_primary_batch_id,
           current_open_offer_slots, current_legacy, requested_count
      FROM curation_round round_row
      JOIN generation_attempt attempt ON attempt.id = round_row.generation_attempt_id
      JOIN challenge_session session ON session.id = attempt.challenge_session_id
     WHERE round_row.id = target_round_id;
    IF NOT FOUND OR current_legacy THEN
        RETURN;
    END IF;

    SELECT batch_number INTO primary_batch_number FROM generation_batch WHERE id = current_primary_batch_id;
    SELECT count(*), count(*) FILTER (WHERE participation_type = 'NEW'),
           count(*) FILTER (WHERE participation_type = 'CARRY_OVER'),
           count(*) FILTER (WHERE participation_type = 'LOCKED_CONTEXT')
      INTO total_count, new_count, carry_count, locked_count
      FROM curation_round_candidate WHERE curation_round_id = target_round_id;

    IF current_round_number = 1 THEN
        IF current_purpose <> 'INITIAL_PASS' OR primary_batch_number <> 1
           OR current_open_offer_slots <> requested_count
           OR total_count <> 12 OR new_count <> 12 OR carry_count <> 0 OR locked_count <> 0 THEN
            RAISE EXCEPTION 'round one must contain the complete initial twelve-candidate pass';
        END IF;
        RETURN;
    END IF;

    SELECT id, status, primary_generation_batch_id
      INTO previous_round_id, previous_status, previous_primary_batch_id
      FROM curation_round
     WHERE generation_attempt_id = round_attempt_id
       AND round_number = 1
       AND NOT legacy_migrated;
    IF previous_round_id IS NULL THEN
        RAISE EXCEPTION 'round two requires a persisted first round';
    END IF;

    IF current_purpose = 'TECHNICAL_RETRY' THEN
        IF previous_status <> 'TECHNICAL_ERROR' OR current_primary_batch_id <> previous_primary_batch_id
           OR current_open_offer_slots <> requested_count
           OR total_count <> 12 OR new_count <> 12 OR carry_count <> 0 OR locked_count <> 0 THEN
            RAISE EXCEPTION 'technical retry must repeat the complete first-pass request';
        END IF;
        RETURN;
    END IF;

    IF current_purpose <> 'QUALITY_FOLLOW_UP' OR previous_status <> 'COMPLETED' OR primary_batch_number <> 2 THEN
        RAISE EXCEPTION 'quality follow-up requires a completed first round and a generated batch two';
    END IF;
    SELECT count(*) INTO previous_good_count
      FROM curation_round_candidate
     WHERE curation_round_id = previous_round_id AND evaluation_class = 'GOOD';
    IF previous_good_count >= requested_count OR current_open_offer_slots <> requested_count - previous_good_count
       OR new_count <> 12 OR locked_count <> previous_good_count
       OR carry_count > current_open_offer_slots
       OR total_count <> 12 + previous_good_count + carry_count THEN
        RAISE EXCEPTION 'quality follow-up has an invalid locked, carry-over, or new-candidate shape';
    END IF;
    IF EXISTS (
        SELECT 1
          FROM curation_round_candidate current_candidate
          LEFT JOIN curation_round_candidate source_candidate
            ON source_candidate.id = current_candidate.source_round_candidate_id
         WHERE current_candidate.curation_round_id = target_round_id
           AND ((current_candidate.participation_type = 'LOCKED_CONTEXT'
                 AND (source_candidate.curation_round_id <> previous_round_id
                      OR source_candidate.evaluation_class <> 'GOOD'))
                OR (current_candidate.participation_type = 'CARRY_OVER'
                    AND (source_candidate.curation_round_id <> previous_round_id
                         OR source_candidate.evaluation_class NOT IN ('ACCEPTABLE', 'BAD'))))
    ) OR EXISTS (
        SELECT 1
          FROM curation_round_candidate source_candidate
         WHERE source_candidate.curation_round_id = previous_round_id
           AND source_candidate.evaluation_class = 'GOOD'
           AND NOT EXISTS (
               SELECT 1 FROM curation_round_candidate current_candidate
                WHERE current_candidate.curation_round_id = target_round_id
                  AND current_candidate.participation_type = 'LOCKED_CONTEXT'
                  AND current_candidate.source_round_candidate_id = source_candidate.id
           )
    ) THEN
        RAISE EXCEPTION 'quality follow-up must preserve all GOODs and only carry evaluated fallbacks from round one';
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION validate_curation_round_request_shape_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM assert_curation_round_request_shape(CASE WHEN TG_OP = 'DELETE' THEN OLD.id ELSE NEW.id END);
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_curation_round_request_shape
AFTER INSERT OR UPDATE OR DELETE ON curation_round
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_curation_round_request_shape_trigger();

CREATE OR REPLACE FUNCTION validate_curation_round_candidate_request_shape_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' OR TG_OP = 'UPDATE' THEN
        PERFORM assert_curation_round_request_shape(OLD.curation_round_id);
    END IF;
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        PERFORM assert_curation_round_request_shape(NEW.curation_round_id);
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_curation_round_candidate_request_shape
AFTER INSERT OR UPDATE OR DELETE ON curation_round_candidate
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_curation_round_candidate_request_shape_trigger();

CREATE OR REPLACE FUNCTION prevent_terminal_curation_candidate_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    round_status text;
    round_legacy boolean;
BEGIN
    SELECT status, legacy_migrated INTO round_status, round_legacy
      FROM curation_round WHERE id = OLD.curation_round_id;
    IF NOT round_legacy AND round_status <> 'PENDING' THEN
        RAISE EXCEPTION 'participants of terminal curation round % are immutable', OLD.curation_round_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_curation_round_candidate_terminal_immutable
BEFORE UPDATE ON curation_round_candidate
FOR EACH ROW EXECUTE FUNCTION prevent_terminal_curation_candidate_mutation();

-- A successful Offer Set stays complete and contains a GOOD even after Phase 11
-- later changes only its presentation status.
CREATE OR REPLACE FUNCTION assert_curated_offer_set_complete(target_set_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    requested_count smallint;
    actual_count integer;
    good_count integer;
BEGIN
    SELECT requested_offer_count INTO requested_count
      FROM curated_offer_set WHERE id = target_set_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;
    SELECT count(*), count(*) FILTER (WHERE participation.evaluation_class = 'GOOD')
      INTO actual_count, good_count
      FROM curated_offer offer
      JOIN curation_round_candidate participation ON participation.id = offer.curation_round_candidate_id
     WHERE offer.curated_offer_set_id = target_set_id;
    IF actual_count <> requested_count OR good_count < 1
       OR EXISTS (
           SELECT 1 FROM curated_offer
            WHERE curated_offer_set_id = target_set_id
            GROUP BY curated_offer_set_id
            HAVING min(position) <> 1 OR max(position) <> requested_count
       ) THEN
        RAISE EXCEPTION 'curated offer set % must always contain positions 1..% and at least one GOOD',
            target_set_id, requested_count;
    END IF;
END;
$$;
