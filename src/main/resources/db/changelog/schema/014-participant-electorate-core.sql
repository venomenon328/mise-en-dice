--liquibase formatted sql
--changeset venomenon328:014-participant-electorate-core splitStatements:false

-- Phase 14A: persistent default electorate, generated stable participant codes, and
-- database-level protection for the mutable default membership. Existing selection
-- snapshots and challenge_participation rows deliberately remain untouched legacy data.

CREATE SEQUENCE participant_generated_code_sequence;

SELECT setval(
    'participant_generated_code_sequence',
    COALESCE((
        SELECT max(substring(code FROM '^PARTICIPANT-([0-9]+)$')::bigint)
        FROM participant
        WHERE code ~ '^PARTICIPANT-[0-9]+$'
    ), 0) + 1,
    false
);

ALTER TABLE participant
    ALTER COLUMN code SET DEFAULT ('PARTICIPANT-' || nextval('participant_generated_code_sequence')::text);

-- A fallback display name is deliberately not an external identity and may be shared.
DROP INDEX uq_participant_display_name_ci;

CREATE TABLE default_electorate_member (
    participant_id bigint NOT NULL REFERENCES participant(id) ON DELETE RESTRICT,
    added_at       timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (participant_id)
);

CREATE INDEX ix_default_electorate_member_added_at
    ON default_electorate_member (added_at, participant_id);

ALTER TABLE challenge_session
    ADD COLUMN selection_electorate_materialized_at timestamptz;

-- A session is the aggregate root for its snapshot. Operational cleanup or a
-- deliberately deleted session must not leave electorate rows behind.
ALTER TABLE selection_electorate
    DROP CONSTRAINT selection_electorate_challenge_session_id_fkey,
    ADD CONSTRAINT selection_electorate_challenge_session_id_fkey
        FOREIGN KEY (challenge_session_id) REFERENCES challenge_session (id) ON DELETE CASCADE;

-- Keep the immutable-snapshot rule for direct mutations. PostgreSQL executes a
-- cascading delete after the parent row is no longer visible, so that one
-- aggregate-root operation is intentionally permitted.
CREATE OR REPLACE FUNCTION prevent_selection_electorate_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' AND NOT EXISTS (
        SELECT 1 FROM challenge_session WHERE id = OLD.challenge_session_id
    ) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'selection electorate snapshots are immutable';
END;
$$;

-- The core materializes an electorate for every generated session. A voting
-- round becomes authoritative only once the explicit voting workflow has
-- created it; the mere catalog/generator snapshot must not alter the existing
-- direct offer-decision path.
CREATE OR REPLACE FUNCTION assert_selection_authorized_offer_decision(target_offer_set_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    target_status text;
    target_offer_count integer;
    target_challenge_offer_id bigint;
    own_round record;
    source_round record;
BEGIN
    SELECT offer_set.status, offer_set.requested_offer_count
      INTO target_status, target_offer_count
      FROM curated_offer_set offer_set
     WHERE offer_set.id = target_offer_set_id;
    IF NOT FOUND OR target_status NOT IN ('CONFIRMED', 'REROLLED') THEN
        RETURN;
    END IF;

    SELECT * INTO own_round
      FROM selection_voting_round
     WHERE curated_offer_set_id = target_offer_set_id;
    IF FOUND THEN
        IF own_round.status <> 'COMPLETED' THEN
            RAISE EXCEPTION 'a presented offer set with an electorate requires a completed voting round decision';
        END IF;
        IF target_status = 'REROLLED' THEN
            IF own_round.result_option_type <> 'REROLL' THEN
                RAISE EXCEPTION 'only a persisted REROLL voting result may reroll its offer set';
            END IF;
            RETURN;
        END IF;
        SELECT curated_offer_id INTO target_challenge_offer_id
          FROM challenge
         WHERE curated_offer_id IN (SELECT id FROM curated_offer WHERE curated_offer_set_id = target_offer_set_id);
        IF own_round.result_option_type = 'OFFER'
           AND own_round.result_curated_offer_id = target_challenge_offer_id THEN
            RETURN;
        END IF;
        IF own_round.result_option_type = 'ACCEPT' AND own_round.round_number = 1 AND target_offer_count = 1 THEN
            RETURN;
        END IF;
        RAISE EXCEPTION 'only the persisted winning offer or ACCEPT result may confirm its offer set';
    END IF;

    SELECT * INTO source_round
      FROM selection_voting_round
     WHERE resulting_offer_set_id = target_offer_set_id
       AND result_option_type = 'REROLL'
     ORDER BY round_number DESC
     LIMIT 1;
    IF target_status = 'CONFIRMED' AND FOUND
       AND source_round.apply_state IN ('REROLL_AUTO_CONFIRM_PENDING', 'REROLL_AUTO_CONFIRMED')
       AND target_offer_count = 1 THEN
        RETURN;
    END IF;
END;
$$;

-- Existing 11B snapshots stay exactly as they are; the marker only records that
-- their already persisted member set is closed to later additions as well.
UPDATE challenge_session session
SET selection_electorate_materialized_at = electorate.materialized_at
FROM (
    SELECT challenge_session_id, min(snapshotted_at) AS materialized_at
    FROM selection_electorate
    GROUP BY challenge_session_id
) electorate
WHERE electorate.challenge_session_id = session.id;

CREATE OR REPLACE FUNCTION prevent_selection_electorate_append_after_materialization()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM challenge_session
        WHERE id = NEW.challenge_session_id
          AND selection_electorate_materialized_at IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'selection electorate snapshots are immutable after materialization';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_selection_electorate_insert_before_materialization
BEFORE INSERT ON selection_electorate
FOR EACH ROW EXECUTE FUNCTION prevent_selection_electorate_append_after_materialization();

CREATE OR REPLACE FUNCTION prevent_participant_code_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.code IS DISTINCT FROM OLD.code THEN
        RAISE EXCEPTION 'participant codes are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_participant_code_immutable
BEFORE UPDATE ON participant
FOR EACH ROW EXECUTE FUNCTION prevent_participant_code_mutation();

CREATE OR REPLACE FUNCTION validate_default_electorate_member()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM participant WHERE id = NEW.participant_id AND active) THEN
        RAISE EXCEPTION 'only active participants may belong to the default electorate';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_default_electorate_member_validate
BEFORE INSERT OR UPDATE ON default_electorate_member
FOR EACH ROW EXECUTE FUNCTION validate_default_electorate_member();

CREATE OR REPLACE FUNCTION remove_deactivated_participant_from_default_electorate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.active AND NOT NEW.active THEN
        DELETE FROM default_electorate_member WHERE participant_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_participant_deactivation_drops_default_member
AFTER UPDATE OF active ON participant
FOR EACH ROW EXECUTE FUNCTION remove_deactivated_participant_from_default_electorate();

-- The reference participants already exist in the baseline. This keeps the upgrade
-- idempotent while respecting the invariant that only active persons may be members.
INSERT INTO default_electorate_member (participant_id)
SELECT id
FROM participant
WHERE code IN ('GEORGIA', 'TOBIAS')
  AND active
ON CONFLICT (participant_id) DO NOTHING;
