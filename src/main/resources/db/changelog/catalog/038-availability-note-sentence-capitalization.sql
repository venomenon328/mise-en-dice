--liquibase formatted sql
--changeset venomenon328:038-availability-note-sentence-capitalization splitStatements:false
-- Issue #217. The selection is deliberately data-driven against the catalog
-- present at migration time; no historic concept or note list is an input.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(21703820260910);
LOCK TABLE ingredient_concept, ingredient_availability, participant
    IN SHARE ROW EXCLUSIVE MODE;

-- First reject a lowercase Unicode letter for which PostgreSQL cannot make a
-- distinct, one-character uppercase replacement. This happens before any
-- write, so a special case cannot produce a partial catalog mutation.
DO $$
DECLARE unsupported_cases text;
BEGIN
    WITH analyzed_notes AS (
        SELECT concept.code,
               participant.code AS participant_code,
               (regexp_match(availability.curator_note, '[[:alpha:]]'))[1] AS first_alpha
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        WHERE availability.curator_note IS NOT NULL
          AND availability.curator_note <> ''
    )
    SELECT string_agg(code || '/' || participant_code || ' (' || first_alpha || ')', ', '
                      ORDER BY code, participant_code)
    INTO unsupported_cases
    FROM analyzed_notes
    WHERE first_alpha ~ '^[[:lower:]]$'
      AND (upper(first_alpha) = first_alpha
           OR char_length(upper(first_alpha)) <> 1);

    IF unsupported_cases IS NOT NULL THEN
        RAISE EXCEPTION
            'Availability note sentence capitalization: unsupported uppercase replacement for %',
            unsupported_cases;
    END IF;
END $$;

CREATE TEMP TABLE availability_note_sentence_capitalization (
    ingredient_concept_id bigint NOT NULL,
    participant_id bigint NOT NULL,
    old_note text NOT NULL,
    new_note text NOT NULL,
    PRIMARY KEY (ingredient_concept_id, participant_id)
) ON COMMIT DROP;

INSERT INTO availability_note_sentence_capitalization (
    ingredient_concept_id, participant_id, old_note, new_note
)
SELECT availability.ingredient_concept_id,
       availability.participant_id,
       availability.curator_note,
       leading_nonalpha || upper(first_alpha)
           || substring(
               availability.curator_note
               FROM char_length(leading_nonalpha) + char_length(first_alpha) + 1
           )
FROM ingredient_availability availability
CROSS JOIN LATERAL (
    SELECT substring(availability.curator_note FROM '^[^[:alpha:]]*') AS leading_nonalpha,
           (regexp_match(availability.curator_note, '[[:alpha:]]'))[1] AS first_alpha
) analyzed
WHERE availability.curator_note IS NOT NULL
  AND availability.curator_note <> ''
  AND first_alpha ~ '^[[:lower:]]$';

-- Preserve every leading nonalphabetic character and every character after
-- the first alphabetic one. The availability trigger owns updated_at; no
-- availability level or other catalog metadata is written here.
UPDATE ingredient_availability availability
SET curator_note = capitalization.new_note
FROM availability_note_sentence_capitalization capitalization
WHERE availability.ingredient_concept_id = capitalization.ingredient_concept_id
  AND availability.participant_id = capitalization.participant_id
  AND availability.curator_note = capitalization.old_note;

-- A changed availability child invalidates a stale editor exactly once per
-- aggregate, even when several participant notes of that concept changed.
UPDATE ingredient_concept concept
SET version = concept.version + 1
WHERE EXISTS (
    SELECT 1
    FROM availability_note_sentence_capitalization capitalization
    WHERE capitalization.ingredient_concept_id = concept.id
);
