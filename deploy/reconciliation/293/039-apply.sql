-- Issue #293 replacement execution for catalog 039; approved values remain in the published source.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM availability_r3_actual WHERE ingredient_concept_id IS NULL) THEN
        RAISE EXCEPTION 'Issue #293: missing 039 concept code';
    END IF;
END $$;


CREATE TEMP TABLE availability_r3_changes ON COMMIT DROP AS
SELECT ingredient_concept_id,
       georgia_target_level, georgia_target_note,
       tobias_target_level, tobias_target_note
FROM availability_r3_actual
WHERE ROW(georgia_actual_level, georgia_actual_note, tobias_actual_level, tobias_actual_note)
    IS DISTINCT FROM ROW(georgia_target_level, georgia_target_note, tobias_target_level, tobias_target_note);

ALTER TABLE availability_r3_changes
    ADD PRIMARY KEY (ingredient_concept_id);

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level, curator_note)
SELECT changes.ingredient_concept_id, p.id, target.level, target.note
FROM availability_r3_changes changes
CROSS JOIN LATERAL (VALUES
    ('GEORGIA', changes.georgia_target_level, changes.georgia_target_note),
    ('TOBIAS', changes.tobias_target_level, changes.tobias_target_note)
) target(code, level, note)
JOIN participant p ON p.code = target.code
ON CONFLICT (ingredient_concept_id, participant_id) DO UPDATE
SET availability_level = excluded.availability_level, curator_note = excluded.curator_note;

-- Availability is part of the ingredient aggregate. Advance each actually
-- changed concept exactly once; fully installed target pairs remain no-ops.
UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM availability_r3_changes changes
WHERE concept.id = changes.ingredient_concept_id;

DO $$
DECLARE conflicts text;
BEGIN
    SELECT string_agg(review.code, ', ' ORDER BY review.code)
    INTO conflicts
    FROM availability_r3_review review
    JOIN ingredient_concept concept ON concept.code = review.code
    JOIN participant georgia_participant ON georgia_participant.code = 'GEORGIA'
    JOIN ingredient_availability georgia
      ON georgia.ingredient_concept_id = concept.id
     AND georgia.participant_id = georgia_participant.id
    JOIN participant tobias_participant ON tobias_participant.code = 'TOBIAS'
    JOIN ingredient_availability tobias
      ON tobias.ingredient_concept_id = concept.id
     AND tobias.participant_id = tobias_participant.id
    WHERE ROW(georgia.availability_level, georgia.curator_note,
              tobias.availability_level, tobias.curator_note)
              IS DISTINCT FROM
          ROW(review.georgia_target_level, review.georgia_target_note,
              review.tobias_target_level, review.tobias_target_note);

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'Availability R3 corrections: target write failed for %', conflicts;
    END IF;
END $$;

DROP VIEW availability_r3_actual;
