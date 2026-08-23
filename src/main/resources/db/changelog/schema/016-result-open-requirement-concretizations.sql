--liquibase formatted sql
--changeset venomenon328:016-result-open-requirement-concretizations splitStatements:false

-- Phase 15D: optional personal concretizations of historically OPEN requirements.
-- The free text is authoritative; the catalog ID is only a constrained analytical reference.
CREATE TABLE challenge_result_concretization (
    challenge_result_id     bigint NOT NULL REFERENCES challenge_result(id) ON DELETE CASCADE,
    requirement_position    smallint NOT NULL,
    display_text            text NOT NULL,
    ingredient_concept_id   bigint REFERENCES ingredient_concept(id) ON DELETE RESTRICT,

    PRIMARY KEY (challenge_result_id, requirement_position),
    CONSTRAINT ck_challenge_result_concretization_position
        CHECK (requirement_position BETWEEN 1 AND 4),
    CONSTRAINT ck_challenge_result_concretization_display
        CHECK (display_text = btrim(display_text) AND display_text <> '' AND char_length(display_text) <= 200)
);

CREATE INDEX ix_challenge_result_concretization_concept
    ON challenge_result_concretization (ingredient_concept_id)
    WHERE ingredient_concept_id IS NOT NULL;

CREATE OR REPLACE FUNCTION validate_challenge_result_concretization()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    requirement_specificity text;
    requirement_concept_id bigint;
BEGIN
    SELECT requirement.challenge_specificity_snapshot, requirement.ingredient_concept_id
      INTO requirement_specificity, requirement_concept_id
      FROM challenge_result result
      JOIN challenge ON challenge.id = result.challenge_id
      JOIN candidate_requirement requirement
        ON requirement.candidate_id = challenge.selected_candidate_id
       AND requirement.position = NEW.requirement_position
     WHERE result.id = NEW.challenge_result_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'challenge result concretization must target a confirmed challenge requirement';
    END IF;
    IF requirement_specificity IS DISTINCT FROM 'OPEN' OR requirement_concept_id IS NULL THEN
        RAISE EXCEPTION 'challenge result concretization may only target an OPEN catalog requirement';
    END IF;

    IF NEW.ingredient_concept_id IS NOT NULL AND NOT EXISTS (
        WITH RECURSIVE descendants(concept_id) AS (
            SELECT refinement.child_concept_id
              FROM ingredient_refinement refinement
             WHERE refinement.parent_concept_id = requirement_concept_id
            UNION
            SELECT refinement.child_concept_id
              FROM ingredient_refinement refinement
              JOIN descendants ON descendants.concept_id = refinement.parent_concept_id
        )
        SELECT 1 FROM descendants WHERE concept_id = NEW.ingredient_concept_id
    ) THEN
        RAISE EXCEPTION 'catalog reference is not a known refinement of the OPEN requirement';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_challenge_result_concretization_validate
BEFORE INSERT OR UPDATE OF challenge_result_id, requirement_position, ingredient_concept_id
ON challenge_result_concretization
FOR EACH ROW
EXECUTE FUNCTION validate_challenge_result_concretization();
