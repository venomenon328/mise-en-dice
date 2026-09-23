-- Issue #293 replacement execution for catalog 041; approved values remain in the published source.
DO $validation$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM finland_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Finland curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM finland_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Finland curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'FI'
    ) THEN
        RAISE EXCEPTION 'Finland culinary-country reference is missing or inconsistent';
    END IF;


END;
$validation$;

CREATE TEMP TABLE finland_existing_at_start (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO finland_existing_at_start
SELECT concept.id
FROM ingredient_concept concept
JOIN finland_required_existing required ON required.code = concept.code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, active, random_draw_enabled, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM finland_new_concept;

CREATE TEMP TABLE finland_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO finland_existing_changes
SELECT existing.ingredient_concept_id
FROM ingredient_concept concept
JOIN finland_existing_at_start existing ON existing.ingredient_concept_id = concept.id
WHERE concept.code = 'GREEN_PEAS'
  AND concept.curator_note IS DISTINCT FROM
      'Grüne Erbsen unabhängig von der Produktform; frisch, tiefgekühlt, getrocknet, gespalten oder schlicht vorgegart beziehungsweise konserviert sind umfasst. Gelbe Erbsen sind nicht gemeint.'
UNION
SELECT existing.ingredient_concept_id
FROM ingredient_refinement relation
JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
JOIN ingredient_concept child ON child.id = relation.child_concept_id
JOIN finland_existing_at_start existing
  ON existing.ingredient_concept_id IN (parent.id, child.id)
WHERE parent.code = 'POD_VEGETABLES'
  AND child.code = 'GREEN_PEAS'
UNION
SELECT existing.ingredient_concept_id
FROM finland_refinement target
JOIN ingredient_concept parent ON parent.code = target.parent_code
JOIN ingredient_concept child ON child.code = target.child_code
JOIN finland_existing_at_start existing
  ON existing.ingredient_concept_id IN (parent.id, child.id)
WHERE NOT EXISTS (
    SELECT 1
    FROM ingredient_refinement relation
    WHERE relation.parent_concept_id = parent.id
      AND relation.child_concept_id = child.id
)
UNION
SELECT existing.ingredient_concept_id
FROM finland_existing_country target
JOIN ingredient_concept concept ON concept.code = target.code
JOIN finland_existing_at_start existing ON existing.ingredient_concept_id = concept.id
WHERE NOT EXISTS (
    SELECT 1
    FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'FI'
);

UPDATE ingredient_concept
SET curator_note =
    'Grüne Erbsen unabhängig von der Produktform; frisch, tiefgekühlt, getrocknet, gespalten oder schlicht vorgegart beziehungsweise konserviert sind umfasst. Gelbe Erbsen sind nicht gemeint.'
WHERE code = 'GREEN_PEAS'
  AND curator_note IS DISTINCT FROM
      'Grüne Erbsen unabhängig von der Produktform; frisch, tiefgekühlt, getrocknet, gespalten oder schlicht vorgegart beziehungsweise konserviert sind umfasst. Gelbe Erbsen sind nicht gemeint.';

DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'POD_VEGETABLES'
  AND child.code = 'GREEN_PEAS';

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM finland_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('VENDACE', 'ANIMAL_PROTEIN'),
    ('LEIPAJUUSTO', 'ANIMAL_PROTEIN'),
    ('LEIPAJUUSTO', 'FAT'),
    ('LEIPAJUUSTO', 'SEASONING'),
    ('VIILI', 'ACID'),
    ('VIILI', 'FAT'),
    ('VIILI', 'SEASONING'),
    ('SAHTI', 'ACID'),
    ('SAHTI', 'SEASONING'),
    ('ARCTIC_CHAR', 'ANIMAL_PROTEIN'),
    ('SEA_BUCKTHORN', 'ACID'),
    ('SEA_BUCKTHORN', 'FRUIT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('VENDACE', 'DOMINANCE', 3),
    ('VENDACE', 'FATTINESS', 2),
    ('VENDACE', 'UMAMI', 3),
    ('LEIPAJUUSTO', 'DOMINANCE', 3),
    ('LEIPAJUUSTO', 'FATTINESS', 4),
    ('LEIPAJUUSTO', 'UMAMI', 3),
    ('LEIPAJUUSTO', 'SALTINESS', 2),
    ('VIILI', 'DOMINANCE', 2),
    ('VIILI', 'ACIDITY', 3),
    ('VIILI', 'FATTINESS', 2),
    ('VIILI', 'SALTINESS', 1),
    ('SAHTI', 'DOMINANCE', 4),
    ('SAHTI', 'SWEETNESS', 3),
    ('SAHTI', 'BITTERNESS', 1),
    ('ARCTIC_CHAR', 'DOMINANCE', 3),
    ('ARCTIC_CHAR', 'FATTINESS', 3),
    ('ARCTIC_CHAR', 'UMAMI', 4),
    ('SEA_BUCKTHORN', 'DOMINANCE', 4),
    ('SEA_BUCKTHORN', 'SWEETNESS', 1),
    ('SEA_BUCKTHORN', 'ACIDITY', 5),
    ('SEA_BUCKTHORN', 'BITTERNESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('VIILI', 'FERMENTED'),
    ('SAHTI', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM finland_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'FI'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM finland_existing_country
    UNION ALL
    SELECT code FROM finland_new_concept WHERE associate_finland
) assignment ON assignment.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM finland_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

-- No season rows: all six new concepts use the default multiplier 1.0 in every month.
-- SEA_BUCKTHORN deliberately remains non-seasonal because its approved concept includes durable processed forms.
