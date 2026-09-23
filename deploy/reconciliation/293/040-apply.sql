-- Issue #293 replacement execution for catalog 040; approved values remain in the published source.
DO $validation$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM scotland_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Scotland curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM scotland_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Scotland curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'GB-SCT'
    ) THEN
        RAISE EXCEPTION 'Scotland culinary-country reference is missing or inconsistent';
    END IF;
END;
$validation$;

CREATE TEMP TABLE scotland_existing_at_start (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO scotland_existing_at_start
SELECT concept.id
FROM ingredient_concept concept
JOIN scotland_required_existing required ON required.code = concept.code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM scotland_new_concept;

CREATE TEMP TABLE scotland_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO scotland_existing_changes
SELECT existing.ingredient_concept_id
FROM scotland_refinement target
JOIN ingredient_concept parent ON parent.code = target.parent_code
JOIN ingredient_concept child ON child.code = target.child_code
JOIN scotland_existing_at_start existing
  ON existing.ingredient_concept_id IN (parent.id, child.id)
WHERE NOT EXISTS (
    SELECT 1
    FROM ingredient_refinement relation
    WHERE relation.parent_concept_id = parent.id
      AND relation.child_concept_id = child.id
)
UNION
SELECT existing.ingredient_concept_id
FROM scotland_existing_country target
JOIN ingredient_concept concept ON concept.code = target.code
JOIN scotland_existing_at_start existing ON existing.ingredient_concept_id = concept.id
WHERE NOT EXISTS (
    SELECT 1
    FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'GB-SCT'
);

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM scotland_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('WHISKY', 'ACID'),
    ('WHISKY', 'SEASONING'),
    ('HAGGIS', 'ANIMAL_PROTEIN'),
    ('HAGGIS', 'FAT'),
    ('SMOKED_HADDOCK', 'ANIMAL_PROTEIN'),
    ('SMOKED_HADDOCK', 'SEASONING'),
    ('MUTTON', 'ANIMAL_PROTEIN'),
    ('MUTTON_LEG', 'ANIMAL_PROTEIN'),
    ('MUTTON_SHOULDER', 'ANIMAL_PROTEIN'),
    ('MUTTON_SHOULDER', 'FAT'),
    ('MUTTON_CHOP', 'ANIMAL_PROTEIN'),
    ('MUTTON_CHOP', 'FAT'),
    ('MUTTON_MINCE', 'ANIMAL_PROTEIN'),
    ('LANGOUSTINE', 'ANIMAL_PROTEIN'),
    ('LORNE_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('LORNE_SAUSAGE', 'FAT'),
    ('SCOTTISH_OATCAKES', 'STARCH')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('WHISKY', 'DOMINANCE', 4),
    ('WHISKY', 'SWEETNESS', 2),
    ('WHISKY', 'BITTERNESS', 2),
    ('HAGGIS', 'DOMINANCE', 4),
    ('HAGGIS', 'FATTINESS', 4),
    ('HAGGIS', 'UMAMI', 4),
    ('HAGGIS', 'SALTINESS', 3),
    ('SMOKED_HADDOCK', 'DOMINANCE', 4),
    ('SMOKED_HADDOCK', 'FATTINESS', 1),
    ('SMOKED_HADDOCK', 'UMAMI', 4),
    ('SMOKED_HADDOCK', 'SALTINESS', 4),
    ('MUTTON', 'DOMINANCE', 4),
    ('MUTTON', 'FATTINESS', 3),
    ('MUTTON', 'UMAMI', 4),
    ('MUTTON_LEG', 'DOMINANCE', 4),
    ('MUTTON_LEG', 'FATTINESS', 3),
    ('MUTTON_LEG', 'UMAMI', 4),
    ('MUTTON_SHOULDER', 'DOMINANCE', 4),
    ('MUTTON_SHOULDER', 'FATTINESS', 4),
    ('MUTTON_SHOULDER', 'UMAMI', 4),
    ('MUTTON_CHOP', 'DOMINANCE', 4),
    ('MUTTON_CHOP', 'FATTINESS', 4),
    ('MUTTON_CHOP', 'UMAMI', 4),
    ('MUTTON_MINCE', 'DOMINANCE', 4),
    ('MUTTON_MINCE', 'FATTINESS', 3),
    ('MUTTON_MINCE', 'UMAMI', 4),
    ('LANGOUSTINE', 'DOMINANCE', 3),
    ('LANGOUSTINE', 'SWEETNESS', 2),
    ('LANGOUSTINE', 'UMAMI', 4),
    ('LORNE_SAUSAGE', 'DOMINANCE', 3),
    ('LORNE_SAUSAGE', 'FATTINESS', 4),
    ('LORNE_SAUSAGE', 'UMAMI', 4),
    ('LORNE_SAUSAGE', 'SALTINESS', 3),
    ('SCOTTISH_OATCAKES', 'DOMINANCE', 2),
    ('SCOTTISH_OATCAKES', 'SWEETNESS', 1),
    ('SCOTTISH_OATCAKES', 'FATTINESS', 3),
    ('SCOTTISH_OATCAKES', 'SALTINESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('SMOKED_HADDOCK', 'SMOKED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM scotland_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'GB-SCT'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM scotland_existing_country
    UNION ALL
    SELECT code FROM scotland_new_concept WHERE associate_scotland
) assignment ON assignment.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM scotland_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

-- No season rows: all approved concepts use the default multiplier 1.0 in every month.
