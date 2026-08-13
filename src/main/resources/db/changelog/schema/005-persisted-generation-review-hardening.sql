--liquibase formatted sql
--changeset venomenon328:005-persisted-generation-review-hardening splitStatements:false

-- Review hardening for Phase 9D. Keep mutable historical metadata untouched, but retain
-- stable concept identity for pre-9D requirements so confirmed legacy challenges remain
-- usable for exact reroll blocking after the migration.
UPDATE candidate_requirement requirement
SET concept_code_snapshot = concept.code
FROM ingredient_concept concept
WHERE requirement.source = 'RANDOM'
  AND requirement.ingredient_concept_id = concept.id
  AND requirement.concept_code_snapshot IS NULL;

UPDATE candidate_requirement requirement
SET concept_code_snapshot = concept.code
FROM generation_manual_requirement manual_requirement
JOIN ingredient_concept concept
  ON concept.id = manual_requirement.matched_ingredient_concept_id
WHERE requirement.source = 'MANUAL'
  AND requirement.manual_requirement_id = manual_requirement.id
  AND requirement.concept_code_snapshot IS NULL;

-- Phase 9D generator candidates are deliberately uncurated. The old pre-9D columns
-- must therefore remain empty for new batches; otherwise setting is_selected=true
-- would provide a backdoor around the later Phase-10 offer-confirmation lifecycle.
CREATE OR REPLACE FUNCTION validate_candidate_batch_context()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    batch_attempt_id bigint;
    batch_is_legacy boolean;
    round_attempt_id bigint;
BEGIN
    SELECT generation_attempt_id, legacy_migrated
      INTO batch_attempt_id, batch_is_legacy
      FROM generation_batch
     WHERE id = NEW.generation_batch_id;

    IF batch_attempt_id IS NULL THEN
        RAISE EXCEPTION 'generation batch % does not exist', NEW.generation_batch_id;
    END IF;

    IF NOT batch_is_legacy AND NEW.candidate_number NOT BETWEEN 1 AND 12 THEN
        RAISE EXCEPTION 'generated candidate number % must be between 1 and 12', NEW.candidate_number;
    END IF;

    IF NOT batch_is_legacy AND (
        NEW.curation_round_id IS NOT NULL
        OR NEW.is_selected IS NOT NULL
        OR NEW.curator_evaluation IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'new generator candidate must not contain legacy curation state';
    END IF;

    IF NEW.curation_round_id IS NOT NULL THEN
        SELECT generation_attempt_id INTO round_attempt_id
        FROM curation_round WHERE id = NEW.curation_round_id;
        IF round_attempt_id IS DISTINCT FROM batch_attempt_id THEN
            RAISE EXCEPTION 'legacy curation round and generation batch must belong to the same attempt';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER trg_candidate_validate_batch_context ON challenge_candidate;
CREATE TRIGGER trg_candidate_validate_batch_context
BEFORE INSERT OR UPDATE OF generation_batch_id, curation_round_id, candidate_number, is_selected, curator_evaluation
ON challenge_candidate
FOR EACH ROW
EXECUTE FUNCTION validate_candidate_batch_context();

-- Until Phase 10 introduces curated_offer confirmation, only historically curated
-- legacy candidates may back a visible challenge. New Phase-9D candidates cannot be
-- published merely by toggling the retained legacy selection flag.
CREATE OR REPLACE FUNCTION validate_challenge_selected_candidate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    candidate_attempt_id bigint;
    candidate_batch_is_legacy boolean;
    candidate_is_selected boolean;
    candidate_curation_round_id bigint;
    candidate_curation_status text;
BEGIN
    SELECT batch.generation_attempt_id,
           batch.legacy_migrated,
           candidate.is_selected,
           candidate.curation_round_id,
           round_row.status
      INTO candidate_attempt_id,
           candidate_batch_is_legacy,
           candidate_is_selected,
           candidate_curation_round_id,
           candidate_curation_status
      FROM challenge_candidate candidate
      JOIN generation_batch batch ON batch.id = candidate.generation_batch_id
      LEFT JOIN curation_round round_row ON round_row.id = candidate.curation_round_id
     WHERE candidate.id = NEW.selected_candidate_id;

    IF candidate_attempt_id IS NULL THEN
        RAISE EXCEPTION 'selected challenge candidate % does not exist', NEW.selected_candidate_id;
    END IF;

    IF candidate_attempt_id <> NEW.generation_attempt_id THEN
        RAISE EXCEPTION 'selected candidate % does not belong to generation attempt %',
            NEW.selected_candidate_id, NEW.generation_attempt_id;
    END IF;

    IF candidate_batch_is_legacy IS DISTINCT FROM true
       OR candidate_curation_round_id IS NULL
       OR candidate_curation_status IS DISTINCT FROM 'SELECTED'
       OR candidate_is_selected IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'candidate % has no completed legacy curation; Phase-9D candidates require Phase-10 offer confirmation before visibility',
            NEW.selected_candidate_id;
    END IF;

    IF (SELECT count(*) FROM candidate_requirement WHERE candidate_id = NEW.selected_candidate_id) <> 4 THEN
        RAISE EXCEPTION 'selected candidate % must contain exactly four requirements', NEW.selected_candidate_id;
    END IF;

    RETURN NEW;
END;
$$;
