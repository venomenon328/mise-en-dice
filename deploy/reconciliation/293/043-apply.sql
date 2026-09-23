-- Issue #293 replacement execution for catalog 043; approved values remain in the published source.
CREATE OR REPLACE TEMP VIEW germany_existing_metadata_actual AS
SELECT metadata.*,
       concept.id AS ingredient_concept_id,
       concept.curator_note AS actual_curator_note,
       georgia.availability_level AS georgia_level,
       georgia.curator_note AS georgia_note,
       tobias.availability_level AS tobias_level,
       tobias.curator_note AS tobias_note
FROM germany_existing_metadata metadata
JOIN ingredient_concept concept ON concept.code = metadata.code
JOIN participant georgia_participant ON georgia_participant.code = 'GEORGIA'
LEFT JOIN ingredient_availability georgia
  ON georgia.ingredient_concept_id = concept.id
 AND georgia.participant_id = georgia_participant.id
JOIN participant tobias_participant ON tobias_participant.code = 'TOBIAS'
LEFT JOIN ingredient_availability tobias
  ON tobias.ingredient_concept_id = concept.id
 AND tobias.participant_id = tobias_participant.id;



CREATE TEMP TABLE germany_metadata_changes (
    ingredient_concept_id bigint PRIMARY KEY,
    code text NOT NULL,
    target_curator_note text NOT NULL,
    target_availability_level text NOT NULL,
    target_availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO germany_metadata_changes
SELECT ingredient_concept_id, code, target_curator_note,
       target_availability_level, target_availability_note
FROM germany_existing_metadata_actual actual
WHERE ROW(actual_curator_note,
          georgia_level, georgia_note,
          tobias_level, tobias_note)
      IS DISTINCT FROM
      ROW(target_curator_note,
          target_availability_level, target_availability_note,
          target_availability_level, target_availability_note);

CREATE TEMP TABLE germany_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_existing_changes
SELECT ingredient_concept_id FROM germany_metadata_changes
UNION
SELECT concept.id
FROM germany_refinement relation
JOIN ingredient_concept concept ON concept.code = relation.parent_code
UNION
SELECT concept.id
FROM germany_existing_country_add approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE NOT EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'DE'
)
UNION
SELECT concept.id
FROM germany_existing_country_remove approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'DE'
);

CREATE TEMP TABLE germany_exclusion_rule_changes (
    exclusion_rule_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_exclusion_rule_changes
SELECT id FROM exclusion_rule WHERE code IN ('NO_MEAT', 'NO_PORK');

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM germany_new_concept;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT concept.id, alias.alias_text
FROM germany_new_alias alias
JOIN ingredient_concept concept ON concept.code = alias.concept_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM germany_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('GRUENKERN', 'STARCH'),
    ('LINSEED_OIL', 'FAT'),
    ('PINKEL', 'ANIMAL_PROTEIN'),
    ('PINKEL', 'FAT'),
    ('HESSIAN_HAND_CHEESE', 'ANIMAL_PROTEIN'),
    ('HESSIAN_HAND_CHEESE', 'SEASONING'),
    ('MAULTASCHEN', 'STARCH'),
    ('STOLLEN', 'STARCH'),
    ('OBAZDA', 'ANIMAL_PROTEIN'),
    ('OBAZDA', 'FAT'),
    ('OBAZDA', 'SEASONING'),
    ('BAVARIAN_SWEET_MUSTARD', 'SEASONING'),
    ('GRIEBENSCHMALZ', 'FAT'),
    ('LEBKUCHEN', 'STARCH'),
    ('LEBKUCHEN', 'AROMATIC')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('GRUENKERN', 'DOMINANCE', 3),
    ('GRUENKERN', 'SWEETNESS', 2),
    ('GRUENKERN', 'UMAMI', 2),
    ('LINSEED_OIL', 'DOMINANCE', 3),
    ('LINSEED_OIL', 'BITTERNESS', 2),
    ('LINSEED_OIL', 'FATTINESS', 5),
    ('PINKEL', 'DOMINANCE', 4),
    ('PINKEL', 'FATTINESS', 5),
    ('PINKEL', 'HEAT', 1),
    ('PINKEL', 'UMAMI', 4),
    ('PINKEL', 'SALTINESS', 4),
    ('HESSIAN_HAND_CHEESE', 'DOMINANCE', 4),
    ('HESSIAN_HAND_CHEESE', 'ACIDITY', 3),
    ('HESSIAN_HAND_CHEESE', 'FATTINESS', 1),
    ('HESSIAN_HAND_CHEESE', 'UMAMI', 4),
    ('HESSIAN_HAND_CHEESE', 'SALTINESS', 4),
    ('MAULTASCHEN', 'DOMINANCE', 2),
    ('STOLLEN', 'DOMINANCE', 4),
    ('STOLLEN', 'SWEETNESS', 4),
    ('STOLLEN', 'FATTINESS', 4),
    ('STOLLEN', 'SALTINESS', 1),
    ('OBAZDA', 'DOMINANCE', 4),
    ('OBAZDA', 'ACIDITY', 2),
    ('OBAZDA', 'FATTINESS', 4),
    ('OBAZDA', 'HEAT', 1),
    ('OBAZDA', 'UMAMI', 4),
    ('OBAZDA', 'SALTINESS', 3),
    ('BAVARIAN_SWEET_MUSTARD', 'DOMINANCE', 3),
    ('BAVARIAN_SWEET_MUSTARD', 'SWEETNESS', 4),
    ('BAVARIAN_SWEET_MUSTARD', 'ACIDITY', 2),
    ('BAVARIAN_SWEET_MUSTARD', 'BITTERNESS', 1),
    ('BAVARIAN_SWEET_MUSTARD', 'HEAT', 1),
    ('BAVARIAN_SWEET_MUSTARD', 'SALTINESS', 1),
    ('GRIEBENSCHMALZ', 'DOMINANCE', 3),
    ('GRIEBENSCHMALZ', 'FATTINESS', 5),
    ('GRIEBENSCHMALZ', 'UMAMI', 3),
    ('GRIEBENSCHMALZ', 'SALTINESS', 2),
    ('LEBKUCHEN', 'DOMINANCE', 4),
    ('LEBKUCHEN', 'SWEETNESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('GRUENKERN', 'DRIED'),
    ('PINKEL', 'SMOKED'),
    ('HESSIAN_HAND_CHEESE', 'FERMENTED'),
    ('OBAZDA', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM germany_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_seasonality (
    ingredient_concept_id, month, weight_multiplier
)
SELECT concept.id, assignment.month, assignment.weight_multiplier
FROM (VALUES
    ('STOLLEN', 1, 0.0100),
    ('STOLLEN', 2, 0.0100),
    ('STOLLEN', 3, 0.0100),
    ('STOLLEN', 4, 0.0100),
    ('STOLLEN', 5, 0.0100),
    ('STOLLEN', 6, 0.0100),
    ('STOLLEN', 7, 0.0100),
    ('STOLLEN', 8, 0.0100),
    ('STOLLEN', 9, 0.0100),
    ('STOLLEN', 10, 0.5000),
    ('STOLLEN', 11, 1.0000),
    ('STOLLEN', 12, 1.2000),
    ('LEBKUCHEN', 1, 0.4000),
    ('LEBKUCHEN', 2, 0.0500),
    ('LEBKUCHEN', 3, 0.0500),
    ('LEBKUCHEN', 4, 0.0500),
    ('LEBKUCHEN', 5, 0.0500),
    ('LEBKUCHEN', 6, 0.0500),
    ('LEBKUCHEN', 7, 0.0500),
    ('LEBKUCHEN', 8, 0.0500),
    ('LEBKUCHEN', 9, 0.6000),
    ('LEBKUCHEN', 10, 1.0000),
    ('LEBKUCHEN', 11, 1.4000),
    ('LEBKUCHEN', 12, 1.6000)
) AS assignment(concept_code, month, weight_multiplier)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code;

UPDATE ingredient_concept concept
SET curator_note = changes.target_curator_note
FROM germany_metadata_changes changes
WHERE concept.id = changes.ingredient_concept_id;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level, curator_note)
SELECT changes.ingredient_concept_id, p.id, changes.target_availability_level, changes.target_availability_note
FROM germany_metadata_changes changes CROSS JOIN participant p
WHERE p.code IN ('GEORGIA', 'TOBIAS')
ON CONFLICT (ingredient_concept_id, participant_id) DO UPDATE
SET availability_level = excluded.availability_level, curator_note = excluded.curator_note;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'DE'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM germany_existing_country_add
    UNION ALL
    SELECT code FROM germany_new_concept
) approved ON approved.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

DELETE FROM ingredient_culinary_country country
USING ingredient_concept concept, germany_existing_country_remove approved
WHERE country.ingredient_concept_id = concept.id
  AND concept.code = approved.code
  AND country.country_code = 'DE';

INSERT INTO exclusion_rule_target (
    exclusion_rule_id, ingredient_concept_id, include_refinements
)
SELECT rule.id, concept.id, false
FROM (VALUES
    ('NO_MEAT'),
    ('NO_PORK')
) AS assignment(rule_code)
JOIN exclusion_rule rule ON rule.code = assignment.rule_code
JOIN ingredient_concept concept ON concept.code = 'GRIEBENSCHMALZ';

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM germany_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

UPDATE exclusion_rule rule
SET version = rule.version + 1
FROM germany_exclusion_rule_changes changes
WHERE rule.id = changes.exclusion_rule_id;

DO $validation$
DECLARE conflicts text;
BEGIN
    SELECT string_agg(code, ', ' ORDER BY code)
    INTO conflicts
    FROM germany_existing_metadata_actual actual
    WHERE ROW(actual_curator_note,
              georgia_level, georgia_note,
              tobias_level, tobias_note)
          IS DISTINCT FROM
          ROW(target_curator_note,
              target_availability_level, target_availability_note,
              target_availability_level, target_availability_note);

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'Germany curation metadata target write failed for %', conflicts;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT code FROM germany_existing_country_add
            UNION ALL
            SELECT code FROM germany_new_concept
        ) approved
        JOIN ingredient_concept concept ON concept.code = approved.code
        WHERE NOT EXISTS (
            SELECT 1 FROM ingredient_culinary_country country
            WHERE country.ingredient_concept_id = concept.id
              AND country.country_code = 'DE'
        )
    ) OR EXISTS (
        SELECT 1
        FROM germany_existing_country_remove approved
        JOIN ingredient_concept concept ON concept.code = approved.code
        JOIN ingredient_culinary_country country
          ON country.ingredient_concept_id = concept.id
         AND country.country_code = 'DE'
    ) THEN
        RAISE EXCEPTION 'Germany curation country-relation target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept concept
        JOIN germany_new_concept approved ON approved.code = concept.code
        WHERE concept.display_name = approved.display_name
          AND concept.active
          AND concept.random_draw_enabled
          AND concept.challenge_specificity = approved.challenge_specificity
          AND concept.novelty_level = approved.novelty_level
          AND concept.base_draw_weight = approved.base_draw_weight
          AND concept.curator_note = approved.curator_note) <> 10 THEN
        RAISE EXCEPTION 'Germany curation new-concept core target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept_alias alias
        JOIN ingredient_concept concept ON concept.id = alias.ingredient_concept_id
        JOIN germany_new_alias approved
          ON approved.concept_code = concept.code
         AND approved.alias_text = alias.alias_text) <> 6 THEN
        RAISE EXCEPTION 'Germany curation alias target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_refinement refinement
        JOIN ingredient_concept parent ON parent.id = refinement.parent_concept_id
        JOIN ingredient_concept child ON child.id = refinement.child_concept_id
        JOIN germany_refinement approved
          ON approved.parent_code = parent.code
         AND approved.child_code = child.code) <> 12 THEN
        RAISE EXCEPTION 'Germany curation refinement target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        JOIN germany_new_concept approved ON approved.code = concept.code
        WHERE participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = approved.availability_level
          AND availability.curator_note = approved.availability_note) <> 20 THEN
        RAISE EXCEPTION 'Germany curation new-concept availability target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_seasonality seasonality
        JOIN ingredient_concept concept ON concept.id = seasonality.ingredient_concept_id
        WHERE concept.code IN ('STOLLEN', 'LEBKUCHEN')) <> 24 THEN
        RAISE EXCEPTION 'Germany curation seasonality target write failed';
    END IF;

    IF (SELECT count(*)
        FROM exclusion_rule_target target
        JOIN exclusion_rule rule ON rule.id = target.exclusion_rule_id
        JOIN ingredient_concept concept ON concept.id = target.ingredient_concept_id
        WHERE rule.code IN ('NO_MEAT', 'NO_PORK')
          AND concept.code = 'GRIEBENSCHMALZ'
          AND NOT target.include_refinements) <> 2 THEN
        RAISE EXCEPTION 'Germany curation exclusion-rule target write failed';
    END IF;
END;
$validation$;

DROP VIEW germany_existing_metadata_actual;
