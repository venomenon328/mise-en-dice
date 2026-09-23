CREATE TEMP TABLE issue_291_availability_novelty_target ON COMMIT DROP AS
SELECT concept_code AS code,
       review_applicability = 'APPLICABLE' AS applicable,
       CASE WHEN review_applicability = 'APPLICABLE' THEN cooking_novelty::smallint END AS novelty_level,
       CASE WHEN review_applicability = 'APPLICABLE' THEN availability_georgia END AS georgia_level,
       CASE WHEN review_applicability = 'APPLICABLE' THEN availability_tobias END AS tobias_level,
       CASE WHEN review_applicability = 'APPLICABLE' THEN availability_note_georgia END AS georgia_note,
       CASE WHEN review_applicability = 'APPLICABLE' THEN availability_note_tobias END AS tobias_note
FROM issue_291_availability_novelty_source;

DO $guard$
DECLARE
    problem_codes text;
    predecessor_order integer;
BEGIN
    IF (SELECT count(*) FROM issue_291_availability_novelty_source) <> 860
        OR (SELECT count(DISTINCT concept_code) FROM issue_291_availability_novelty_source) <> 860
        OR (SELECT count(*) FROM issue_291_availability_novelty_source
            WHERE review_applicability = 'APPLICABLE') <> 853
        OR (SELECT count(*) FROM issue_291_availability_novelty_source
            WHERE review_applicability = 'NOT_APPLICABLE_STRUCTURE') <> 7 THEN
        RAISE EXCEPTION 'Issue #291: invalid reconciliation manifest cardinality';
    END IF;

    IF EXISTS (
        SELECT 1 FROM issue_291_availability_novelty_source
        WHERE review_version <> 'AVAILABILITY_NOVELTY_V1_20260907'
           OR catalog_commit <> 'f8855121af336a7c13cd799cafede5f9b9420f28'
           OR final_acceptance_status <> 'APPROVED_FINAL'
           OR overall_approval_origin = ''
           OR review_applicability NOT IN ('APPLICABLE', 'NOT_APPLICABLE_STRUCTURE')
    ) THEN
        RAISE EXCEPTION 'Issue #291: reconciliation manifest identity or approval is invalid';
    END IF;

    IF EXISTS (
        SELECT 1 FROM issue_291_availability_novelty_target
        WHERE CASE WHEN applicable THEN
            novelty_level IS NULL OR novelty_level NOT BETWEEN 1 AND 5
            OR georgia_level IS NULL OR georgia_level NOT IN ('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
            OR tobias_level IS NULL OR tobias_level NOT IN ('EASY','PLANNED','SPECIALTY','DIFFICULT','UNAVAILABLE')
            OR georgia_note IS NULL OR georgia_note !~ '[^[:space:]]'
            OR tobias_note IS NULL OR tobias_note !~ '[^[:space:]]'
        ELSE novelty_level IS NOT NULL OR georgia_level IS NOT NULL OR tobias_level IS NOT NULL
            OR georgia_note IS NOT NULL OR tobias_note IS NOT NULL END
    ) THEN
        RAISE EXCEPTION 'Issue #291: reconciliation manifest contains invalid target values';
    END IF;

    IF (SELECT count(*) FROM databasechangelog
        WHERE id = '033-availability-novelty-final-review') <> 0 THEN
        RAISE EXCEPTION 'Issue #291: changeset 033 is already recorded or ambiguously duplicated';
    END IF;

    SELECT orderexecuted INTO predecessor_order
    FROM databasechangelog
    WHERE id = '019-availability-curator-note'
      AND author = 'venomenon328'
      AND filename = 'db/changelog/schema/019-availability-curator-note.sql';
    IF predecessor_order IS NULL
        OR predecessor_order <> (SELECT max(orderexecuted) FROM databasechangelog) THEN
        RAISE EXCEPTION 'Issue #291: database is not at the immediate pre-033 state';
    END IF;

    IF (SELECT count(*) FROM databasechangelog
        WHERE (id, author, filename) IN (
            ('031-vietnam-curation', 'venomenon328', 'db/changelog/catalog/031-vietnam-curation.sql'),
            ('032-thailand-curation', 'venomenon328', 'db/changelog/catalog/032-thailand-curation.sql'),
            ('018-five-level-availability', 'venomenon328', 'db/changelog/schema/018-five-level-availability.sql'),
            ('019-availability-curator-note', 'venomenon328', 'db/changelog/schema/019-availability-curator-note.sql')
        )) <> 4 THEN
        RAISE EXCEPTION 'Issue #291: required pre-033 changesets are missing';
    END IF;

    IF EXISTS (SELECT 1 FROM databasechangeloglock WHERE locked) THEN
        RAISE EXCEPTION 'Issue #291: Liquibase changelog lock is active';
    END IF;

    SELECT string_agg(target.code, ', ' ORDER BY target.code COLLATE "C")
    INTO problem_codes
    FROM issue_291_availability_novelty_target target
    LEFT JOIN ingredient_concept concept ON concept.code = target.code
    WHERE concept.id IS NULL;
    IF problem_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Issue #291: required review concepts are missing: %', problem_codes;
    END IF;

    SELECT string_agg(required.code, ', ' ORDER BY required.code COLLATE "C")
    INTO problem_codes
    FROM (VALUES ('GEORGIA'), ('TOBIAS')) required(code)
    LEFT JOIN participant ON participant.code = required.code
    WHERE participant.id IS NULL;
    IF problem_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Issue #291: required participants are missing: %', problem_codes;
    END IF;
END $guard$;

-- This is exactly the persistent write scope of changeset 033: novelty, the G/T
-- availability rows and notes, plus one aggregate-version advance per reviewed concept.
UPDATE ingredient_concept concept
SET novelty_level = target.novelty_level,
    version = concept.version + 1
FROM issue_291_availability_novelty_target target
WHERE concept.code = target.code;

DELETE FROM ingredient_availability availability
USING ingredient_concept concept, participant participant_row,
      issue_291_availability_novelty_target target
WHERE availability.ingredient_concept_id = concept.id
  AND availability.participant_id = participant_row.id
  AND concept.code = target.code
  AND NOT target.applicable
  AND participant_row.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant_row.id, value_by_participant.availability_level,
       value_by_participant.curator_note
FROM issue_291_availability_novelty_target target
JOIN ingredient_concept concept ON concept.code = target.code
CROSS JOIN LATERAL (
    VALUES ('GEORGIA', target.georgia_level, target.georgia_note),
           ('TOBIAS', target.tobias_level, target.tobias_note)
) value_by_participant(code, availability_level, curator_note)
JOIN participant participant_row ON participant_row.code = value_by_participant.code
WHERE target.applicable
ON CONFLICT (ingredient_concept_id, participant_id) DO UPDATE
SET availability_level = excluded.availability_level,
    curator_note = excluded.curator_note;

-- A NULL checksum is intentional. Liquibase recognizes the exact changeset identity as
-- MARK_RAN and populates its current checksum on the following normal master update.
INSERT INTO databasechangelog (
    id, author, filename, dateexecuted, orderexecuted, exectype, md5sum,
    description, comments, tag, liquibase, contexts, labels, deployment_id
)
SELECT '033-availability-novelty-final-review',
       'venomenon328',
       'db/changelog/catalog/033-availability-novelty-final-review.sql',
       current_timestamp,
       max(orderexecuted) + 1,
       'MARK_RAN',
       NULL,
       'sql',
       'Issue #291 production reconciliation',
       NULL,
       (array_agg(liquibase ORDER BY orderexecuted DESC))[1],
       NULL,
       NULL,
       (array_agg(deployment_id ORDER BY orderexecuted DESC))[1]
FROM databasechangelog;

COMMIT;
