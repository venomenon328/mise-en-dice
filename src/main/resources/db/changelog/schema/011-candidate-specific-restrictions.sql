--liquibase formatted sql
--changeset venomenon328:011-candidate-specific-restrictions splitStatements:false

-- Phase 12B.5A: generator 1.2 stores restrictions on the candidate, not the attempt.
-- Historical attempt-wide fields deliberately remain untouched for 1.0/1.1 replay and audit.

ALTER TABLE challenge_session
    ADD COLUMN restriction_mode text NOT NULL DEFAULT 'AUTO',
    ADD CONSTRAINT ck_challenge_session_restriction_mode
        CHECK (restriction_mode IN ('AUTO', 'NONE', 'REQUIRED'));

ALTER TABLE challenge_candidate
    ADD COLUMN restriction_rule_id bigint REFERENCES exclusion_rule(id) ON DELETE RESTRICT,
    ADD COLUMN restriction_rule_code_snapshot text,
    ADD COLUMN restriction_text_snapshot text,
    ADD CONSTRAINT ck_challenge_candidate_restriction_snapshot CHECK (
        (restriction_rule_id IS NULL AND restriction_rule_code_snapshot IS NULL AND restriction_text_snapshot IS NULL)
        OR (restriction_rule_id IS NOT NULL AND btrim(restriction_rule_code_snapshot) <> ''
            AND btrim(restriction_text_snapshot) <> '')
    );

ALTER TABLE curated_offer
    ADD COLUMN restriction_rule_id bigint REFERENCES exclusion_rule(id) ON DELETE RESTRICT,
    ADD COLUMN restriction_rule_code_snapshot text,
    ADD COLUMN restriction_text_snapshot text,
    ADD CONSTRAINT ck_curated_offer_restriction_snapshot CHECK (
        (restriction_rule_id IS NULL AND restriction_rule_code_snapshot IS NULL AND restriction_text_snapshot IS NULL)
        OR (restriction_rule_id IS NOT NULL AND btrim(restriction_rule_code_snapshot) <> ''
            AND btrim(restriction_text_snapshot) <> '')
    );

ALTER TABLE challenge
    ADD COLUMN restriction_rule_id bigint REFERENCES exclusion_rule(id) ON DELETE RESTRICT,
    ADD COLUMN restriction_rule_code_snapshot text,
    ADD COLUMN restriction_text_snapshot text,
    ADD CONSTRAINT ck_challenge_restriction_snapshot CHECK (
        (restriction_rule_id IS NULL AND restriction_rule_code_snapshot IS NULL AND restriction_text_snapshot IS NULL)
        OR (restriction_rule_id IS NOT NULL AND btrim(restriction_rule_code_snapshot) <> ''
            AND btrim(restriction_text_snapshot) <> '')
    );

CREATE TABLE reroll_offer_exposure_restriction (
    reroll_offer_exposure_id bigint NOT NULL REFERENCES reroll_offer_exposure(id) ON DELETE CASCADE,
    curated_offer_id         bigint NOT NULL REFERENCES curated_offer(id) ON DELETE RESTRICT,
    challenge_candidate_id   bigint NOT NULL REFERENCES challenge_candidate(id) ON DELETE RESTRICT,
    restriction_rule_id      bigint REFERENCES exclusion_rule(id) ON DELETE RESTRICT,
    restriction_rule_code_snapshot text,
    restriction_text_snapshot text,

    PRIMARY KEY (reroll_offer_exposure_id, curated_offer_id),
    CONSTRAINT ck_reroll_offer_exposure_restriction_snapshot CHECK (
        (restriction_rule_id IS NULL AND restriction_rule_code_snapshot IS NULL AND restriction_text_snapshot IS NULL)
        OR (restriction_rule_id IS NOT NULL AND btrim(restriction_rule_code_snapshot) <> ''
            AND btrim(restriction_text_snapshot) <> '')
    )
);

CREATE INDEX ix_reroll_offer_exposure_restriction_rule
    ON reroll_offer_exposure_restriction (restriction_rule_id)
    WHERE restriction_rule_id IS NOT NULL;

-- Contract V1 retains its explicit attemptExclusion. V2 carries a restriction snapshot
-- with each candidate and therefore has no top-level attempt exclusion.
ALTER TABLE curation_round DROP CONSTRAINT ck_curation_round_new_contract;
ALTER TABLE curation_round ADD CONSTRAINT ck_curation_round_new_contract
    CHECK (
        legacy_migrated
        OR (
            primary_generation_batch_id IS NOT NULL
            AND request_purpose IS NOT NULL
            AND contract_version IN ('CURATION_CONTRACT_V1', 'CURATION_CONTRACT_V2')
            AND request_payload IS NOT NULL
            AND status IN ('PENDING', 'COMPLETED', 'TECHNICAL_ERROR', 'INVALID_RESPONSE')
        )
    );

CREATE OR REPLACE FUNCTION validate_curated_offer_restriction_snapshot()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    candidate_rule_id bigint;
    candidate_code text;
    candidate_text text;
BEGIN
    SELECT restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
      INTO candidate_rule_id, candidate_code, candidate_text
      FROM challenge_candidate
     WHERE id = NEW.challenge_candidate_id;
    IF candidate_rule_id IS DISTINCT FROM NEW.restriction_rule_id
       OR candidate_code IS DISTINCT FROM NEW.restriction_rule_code_snapshot
       OR candidate_text IS DISTINCT FROM NEW.restriction_text_snapshot THEN
        RAISE EXCEPTION 'curated offer restriction must copy its authoritative candidate snapshot exactly';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_curated_offer_restriction_snapshot
BEFORE INSERT OR UPDATE OF challenge_candidate_id, restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
ON curated_offer
FOR EACH ROW EXECUTE FUNCTION validate_curated_offer_restriction_snapshot();

CREATE OR REPLACE FUNCTION validate_challenge_restriction_snapshot()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    offer_rule_id bigint;
    offer_code text;
    offer_text text;
BEGIN
    IF NEW.curated_offer_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
      INTO offer_rule_id, offer_code, offer_text
      FROM curated_offer WHERE id = NEW.curated_offer_id;
    IF offer_rule_id IS DISTINCT FROM NEW.restriction_rule_id
       OR offer_code IS DISTINCT FROM NEW.restriction_rule_code_snapshot
       OR offer_text IS DISTINCT FROM NEW.restriction_text_snapshot THEN
        RAISE EXCEPTION 'challenge restriction must copy its confirmed offer snapshot exactly';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_challenge_restriction_snapshot
BEFORE INSERT OR UPDATE OF curated_offer_id, restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
ON challenge
FOR EACH ROW EXECUTE FUNCTION validate_challenge_restriction_snapshot();

CREATE OR REPLACE FUNCTION validate_reroll_offer_exposure_restriction()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    exposure_offer_set_id bigint;
    offer_candidate_id bigint;
    candidate_rule_id bigint;
    candidate_code text;
    candidate_text text;
BEGIN
    SELECT curated_offer_set_id INTO exposure_offer_set_id
      FROM reroll_offer_exposure WHERE id = NEW.reroll_offer_exposure_id;
    SELECT challenge_candidate_id INTO offer_candidate_id
      FROM curated_offer
     WHERE id = NEW.curated_offer_id AND curated_offer_set_id = exposure_offer_set_id;
    IF offer_candidate_id IS NULL OR offer_candidate_id <> NEW.challenge_candidate_id THEN
        RAISE EXCEPTION 'reroll exposure restriction must reference an offer candidate of its exposed set';
    END IF;
    SELECT restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
      INTO candidate_rule_id, candidate_code, candidate_text
      FROM challenge_candidate WHERE id = NEW.challenge_candidate_id;
    IF candidate_rule_id IS DISTINCT FROM NEW.restriction_rule_id
       OR candidate_code IS DISTINCT FROM NEW.restriction_rule_code_snapshot
       OR candidate_text IS DISTINCT FROM NEW.restriction_text_snapshot THEN
        RAISE EXCEPTION 'reroll exposure restriction must copy the authoritative candidate snapshot exactly';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_reroll_offer_exposure_restriction_validate
BEFORE INSERT OR UPDATE ON reroll_offer_exposure_restriction
FOR EACH ROW EXECUTE FUNCTION validate_reroll_offer_exposure_restriction();

-- Existing REROLL histories predate this table and have no candidate restriction.  Record that
-- authoritative null snapshot now so the strengthened completeness invariant is immediately true.
INSERT INTO reroll_offer_exposure_restriction (
    reroll_offer_exposure_id, curated_offer_id, challenge_candidate_id,
    restriction_rule_id, restriction_rule_code_snapshot, restriction_text_snapshot
)
SELECT exposure.id, offer.id, offer.challenge_candidate_id,
       offer.restriction_rule_id, offer.restriction_rule_code_snapshot, offer.restriction_text_snapshot
FROM reroll_offer_exposure exposure
JOIN curated_offer offer ON offer.curated_offer_set_id = exposure.curated_offer_set_id;

CREATE OR REPLACE FUNCTION assert_reroll_offer_exposure_complete(target_exposure_id bigint)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    target_offer_set_id bigint;
    expected_count integer;
    requirement_count integer;
    restriction_count integer;
BEGIN
    SELECT curated_offer_set_id INTO target_offer_set_id
      FROM reroll_offer_exposure WHERE id = target_exposure_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;
    SELECT count(*) INTO expected_count FROM curated_offer WHERE curated_offer_set_id = target_offer_set_id;
    SELECT count(*) INTO requirement_count
      FROM reroll_offer_exposure_requirement WHERE reroll_offer_exposure_id = target_exposure_id;
    SELECT count(*) INTO restriction_count
      FROM reroll_offer_exposure_restriction WHERE reroll_offer_exposure_id = target_exposure_id;
    IF expected_count = 0 OR requirement_count <> expected_count * 4 OR restriction_count <> expected_count THEN
        RAISE EXCEPTION 'reroll exposure % must contain every exposed offer requirement and restriction snapshot',
            target_exposure_id;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION validate_reroll_offer_exposure_restriction_complete_trigger()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM assert_reroll_offer_exposure_complete(
        CASE WHEN TG_OP = 'DELETE' THEN OLD.reroll_offer_exposure_id ELSE NEW.reroll_offer_exposure_id END
    );
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_reroll_offer_exposure_restriction_complete
AFTER INSERT OR UPDATE OR DELETE ON reroll_offer_exposure_restriction
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION validate_reroll_offer_exposure_restriction_complete_trigger();
