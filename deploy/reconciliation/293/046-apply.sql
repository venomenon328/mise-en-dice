-- Issue #293 replacement execution for catalog 046; approved values remain in the published source.
DO $validation$
BEGIN
    IF (SELECT count(*) FROM japan_new_concept) <> 20
       OR (SELECT count(*) FROM japan_new_alias) <> 11
       OR (SELECT count(*) FROM japan_refinement) <> 24
       OR (SELECT count(*) FROM japan_existing_country) <> 108 THEN
        RAISE EXCEPTION 'Japan curation approval set has an unexpected cardinality';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Japan curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'JP'
    ) THEN
        RAISE EXCEPTION 'Japan culinary-country reference is missing or inconsistent';
    END IF;

    IF (SELECT count(*) FROM participant WHERE code IN ('GEORGIA', 'TOBIAS')) <> 2 THEN
        RAISE EXCEPTION 'Japan curation requires the Georgia and Tobias participants';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ACID'), ('AROMATIC'), ('FAT'), ('FRUIT'), ('PLANT_PROTEIN'),
            ('SEASONING'), ('STARCH'), ('VEGETABLE')
        ) AS required(code)
        LEFT JOIN functional_role reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires functional-role references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ACIDITY'), ('BITTERNESS'), ('DOMINANCE'), ('FATTINESS'),
            ('HEAT'), ('SWEETNESS'), ('UMAMI')
        ) AS required(code)
        LEFT JOIN culinary_dimension reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires culinary-dimension references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES ('DRIED'), ('PICKLED')) AS required(code)
        LEFT JOIN culinary_flag reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires culinary-flag references that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_alias proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.alias_text))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_alias proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.alias_text))
    ) THEN
        RAISE EXCEPTION 'Japan curation proposed names or aliases collide with the current catalog';
    END IF;

    IF EXISTS (
        SELECT normalized_name
        FROM (
            SELECT lower(btrim(display_name)) AS normalized_name
            FROM japan_new_concept
            UNION ALL
            SELECT lower(btrim(alias_text))
            FROM japan_new_alias
        ) proposed_names
        GROUP BY normalized_name
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Japan curation proposed names or aliases collide with each other';
    END IF;
END;
$validation$;

CREATE TEMP TABLE japan_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO japan_existing_changes
SELECT concept.id
FROM japan_existing_country approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE NOT EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'JP'
)
UNION
SELECT concept.id
FROM japan_refinement relation
JOIN ingredient_concept concept ON concept.code = relation.parent_code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM japan_new_concept;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT concept.id, alias.alias_text
FROM japan_new_alias alias
JOIN ingredient_concept concept ON concept.code = alias.concept_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM japan_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('WASABI', 'AROMATIC'),
    ('WASABI', 'SEASONING'),
    ('UMEBOSHI', 'FRUIT'),
    ('UMEBOSHI', 'ACID'),
    ('UMEBOSHI', 'SEASONING'),
    ('SHICHIMI_TOGARASHI', 'AROMATIC'),
    ('SHICHIMI_TOGARASHI', 'SEASONING'),
    ('YUZU_KOSHO', 'AROMATIC'),
    ('YUZU_KOSHO', 'SEASONING'),
    ('MOCHI', 'STARCH'),
    ('BURDOCK_ROOT', 'VEGETABLE'),
    ('NAGAIMO', 'VEGETABLE'),
    ('NAGAIMO', 'STARCH'),
    ('KINAKO', 'PLANT_PROTEIN'),
    ('KINAKO', 'STARCH'),
    ('KINAKO', 'AROMATIC'),
    ('ABURAAGE', 'PLANT_PROTEIN'),
    ('ABURAAGE', 'FAT'),
    ('YUBA', 'PLANT_PROTEIN'),
    ('BENI_SHOGA', 'AROMATIC'),
    ('BENI_SHOGA', 'ACID'),
    ('BENI_SHOGA', 'SEASONING'),
    ('KOMATSUNA', 'VEGETABLE'),
    ('MIZUNA', 'VEGETABLE'),
    ('MITSUBA', 'AROMATIC'),
    ('MYOGA', 'VEGETABLE'),
    ('MYOGA', 'AROMATIC'),
    ('AONORI', 'AROMATIC'),
    ('AONORI', 'SEASONING'),
    ('FURIKAKE', 'AROMATIC'),
    ('FURIKAKE', 'SEASONING'),
    ('JAPANESE_CURRY_ROUX', 'FAT'),
    ('JAPANESE_CURRY_ROUX', 'STARCH'),
    ('JAPANESE_CURRY_ROUX', 'SEASONING'),
    ('SANSHO', 'AROMATIC'),
    ('SANSHO', 'SEASONING'),
    ('FU', 'PLANT_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('WASABI', 'DOMINANCE', 4), ('WASABI', 'SWEETNESS', 1),
    ('WASABI', 'ACIDITY', 1), ('WASABI', 'BITTERNESS', 2),
    ('WASABI', 'FATTINESS', 1), ('WASABI', 'HEAT', 4), ('WASABI', 'UMAMI', 1),
    ('UMEBOSHI', 'DOMINANCE', 5), ('UMEBOSHI', 'SWEETNESS', 1),
    ('UMEBOSHI', 'ACIDITY', 5), ('UMEBOSHI', 'BITTERNESS', 1),
    ('UMEBOSHI', 'FATTINESS', 1), ('UMEBOSHI', 'HEAT', 1), ('UMEBOSHI', 'UMAMI', 2),
    ('SHICHIMI_TOGARASHI', 'DOMINANCE', 4), ('SHICHIMI_TOGARASHI', 'SWEETNESS', 1),
    ('SHICHIMI_TOGARASHI', 'ACIDITY', 1), ('SHICHIMI_TOGARASHI', 'BITTERNESS', 2),
    ('SHICHIMI_TOGARASHI', 'FATTINESS', 1), ('SHICHIMI_TOGARASHI', 'HEAT', 3),
    ('SHICHIMI_TOGARASHI', 'UMAMI', 2),
    ('YUZU_KOSHO', 'DOMINANCE', 5), ('YUZU_KOSHO', 'SWEETNESS', 1),
    ('YUZU_KOSHO', 'ACIDITY', 2), ('YUZU_KOSHO', 'BITTERNESS', 2),
    ('YUZU_KOSHO', 'FATTINESS', 1), ('YUZU_KOSHO', 'HEAT', 4), ('YUZU_KOSHO', 'UMAMI', 2),
    ('MOCHI', 'DOMINANCE', 2), ('MOCHI', 'SWEETNESS', 1), ('MOCHI', 'ACIDITY', 1),
    ('MOCHI', 'BITTERNESS', 1), ('MOCHI', 'FATTINESS', 1), ('MOCHI', 'HEAT', 1),
    ('MOCHI', 'UMAMI', 1),
    ('BURDOCK_ROOT', 'DOMINANCE', 3), ('BURDOCK_ROOT', 'SWEETNESS', 2),
    ('BURDOCK_ROOT', 'ACIDITY', 1), ('BURDOCK_ROOT', 'BITTERNESS', 2),
    ('BURDOCK_ROOT', 'FATTINESS', 1), ('BURDOCK_ROOT', 'HEAT', 1),
    ('BURDOCK_ROOT', 'UMAMI', 2),
    ('NAGAIMO', 'DOMINANCE', 2), ('NAGAIMO', 'SWEETNESS', 2),
    ('NAGAIMO', 'ACIDITY', 1), ('NAGAIMO', 'BITTERNESS', 1),
    ('NAGAIMO', 'FATTINESS', 1), ('NAGAIMO', 'HEAT', 1), ('NAGAIMO', 'UMAMI', 2),
    ('KINAKO', 'DOMINANCE', 3), ('KINAKO', 'SWEETNESS', 1), ('KINAKO', 'ACIDITY', 1),
    ('KINAKO', 'BITTERNESS', 2), ('KINAKO', 'FATTINESS', 2), ('KINAKO', 'HEAT', 1),
    ('KINAKO', 'UMAMI', 2),
    ('ABURAAGE', 'DOMINANCE', 2), ('ABURAAGE', 'SWEETNESS', 1),
    ('ABURAAGE', 'ACIDITY', 1), ('ABURAAGE', 'BITTERNESS', 1),
    ('ABURAAGE', 'FATTINESS', 4), ('ABURAAGE', 'HEAT', 1), ('ABURAAGE', 'UMAMI', 2),
    ('YUBA', 'DOMINANCE', 2), ('YUBA', 'SWEETNESS', 1), ('YUBA', 'ACIDITY', 1),
    ('YUBA', 'BITTERNESS', 1), ('YUBA', 'FATTINESS', 2), ('YUBA', 'HEAT', 1),
    ('YUBA', 'UMAMI', 3),
    ('BENI_SHOGA', 'DOMINANCE', 4), ('BENI_SHOGA', 'SWEETNESS', 1),
    ('BENI_SHOGA', 'ACIDITY', 4), ('BENI_SHOGA', 'BITTERNESS', 1),
    ('BENI_SHOGA', 'FATTINESS', 1), ('BENI_SHOGA', 'HEAT', 2),
    ('BENI_SHOGA', 'UMAMI', 1),
    ('KOMATSUNA', 'DOMINANCE', 2), ('KOMATSUNA', 'SWEETNESS', 2),
    ('KOMATSUNA', 'ACIDITY', 1), ('KOMATSUNA', 'BITTERNESS', 2),
    ('KOMATSUNA', 'FATTINESS', 1), ('KOMATSUNA', 'HEAT', 1), ('KOMATSUNA', 'UMAMI', 2),
    ('MIZUNA', 'DOMINANCE', 2), ('MIZUNA', 'SWEETNESS', 2),
    ('MIZUNA', 'ACIDITY', 1), ('MIZUNA', 'BITTERNESS', 2),
    ('MIZUNA', 'FATTINESS', 1), ('MIZUNA', 'HEAT', 1), ('MIZUNA', 'UMAMI', 2),
    ('MITSUBA', 'DOMINANCE', 3), ('MITSUBA', 'SWEETNESS', 1),
    ('MITSUBA', 'ACIDITY', 1), ('MITSUBA', 'BITTERNESS', 2),
    ('MITSUBA', 'FATTINESS', 1), ('MITSUBA', 'HEAT', 1), ('MITSUBA', 'UMAMI', 1),
    ('MYOGA', 'DOMINANCE', 3), ('MYOGA', 'SWEETNESS', 1), ('MYOGA', 'ACIDITY', 1),
    ('MYOGA', 'BITTERNESS', 2), ('MYOGA', 'FATTINESS', 1), ('MYOGA', 'HEAT', 2),
    ('MYOGA', 'UMAMI', 1),
    ('AONORI', 'DOMINANCE', 3), ('AONORI', 'SWEETNESS', 1),
    ('AONORI', 'ACIDITY', 1), ('AONORI', 'BITTERNESS', 2),
    ('AONORI', 'FATTINESS', 1), ('AONORI', 'HEAT', 1), ('AONORI', 'UMAMI', 4),
    ('JAPANESE_CURRY_ROUX', 'DOMINANCE', 5), ('JAPANESE_CURRY_ROUX', 'SWEETNESS', 2),
    ('JAPANESE_CURRY_ROUX', 'ACIDITY', 1), ('JAPANESE_CURRY_ROUX', 'BITTERNESS', 2),
    ('JAPANESE_CURRY_ROUX', 'FATTINESS', 4), ('JAPANESE_CURRY_ROUX', 'UMAMI', 3),
    ('SANSHO', 'DOMINANCE', 4), ('SANSHO', 'SWEETNESS', 1), ('SANSHO', 'ACIDITY', 1),
    ('SANSHO', 'BITTERNESS', 2), ('SANSHO', 'FATTINESS', 1), ('SANSHO', 'HEAT', 3),
    ('SANSHO', 'UMAMI', 1)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('UMEBOSHI', 'PICKLED'),
    ('UMEBOSHI', 'DRIED'),
    ('SHICHIMI_TOGARASHI', 'DRIED'),
    ('YUBA', 'DRIED'),
    ('BENI_SHOGA', 'PICKLED'),
    ('AONORI', 'DRIED'),
    ('FURIKAKE', 'DRIED'),
    ('SANSHO', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM japan_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'JP'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM japan_existing_country
    UNION ALL
    SELECT code FROM japan_new_concept
) approved ON approved.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM japan_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

-- No season rows: all 20 new concepts use the default multiplier 1.0 in every month.
-- FERMENTED_TOFU -> JP remains deferred to the explicitly scoped follow-up after #279/C3.

DO $validation$
BEGIN
    IF (SELECT count(*)
        FROM ingredient_concept concept
        JOIN japan_new_concept approved ON approved.code = concept.code
        WHERE concept.display_name = approved.display_name
          AND concept.active
          AND concept.random_draw_enabled
          AND concept.challenge_specificity = approved.challenge_specificity
          AND concept.novelty_level = approved.novelty_level
          AND concept.base_draw_weight = approved.base_draw_weight
          AND concept.curator_note = approved.curator_note) <> 20 THEN
        RAISE EXCEPTION 'Japan curation new-concept core target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept_alias alias
        JOIN ingredient_concept concept ON concept.id = alias.ingredient_concept_id
        JOIN japan_new_alias approved
          ON approved.concept_code = concept.code
         AND approved.alias_text = alias.alias_text) <> 11 THEN
        RAISE EXCEPTION 'Japan curation alias target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_refinement refinement
        JOIN ingredient_concept parent ON parent.id = refinement.parent_concept_id
        JOIN ingredient_concept child ON child.id = refinement.child_concept_id
        JOIN japan_refinement approved
          ON approved.parent_code = parent.code
         AND approved.child_code = child.code) <> 24 THEN
        RAISE EXCEPTION 'Japan curation refinement target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_functional_role assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 37 THEN
        RAISE EXCEPTION 'Japan curation functional-role target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_dimension value
        JOIN ingredient_concept concept ON concept.id = value.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 125 THEN
        RAISE EXCEPTION 'Japan curation dimension target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_flag assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 8 THEN
        RAISE EXCEPTION 'Japan curation flag target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        JOIN japan_new_concept approved ON approved.code = concept.code
        WHERE participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = approved.availability_level
          AND availability.curator_note = approved.availability_note) <> 40 THEN
        RAISE EXCEPTION 'Japan curation availability target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_country country
        JOIN ingredient_concept concept ON concept.id = country.ingredient_concept_id
        JOIN japan_existing_country approved ON approved.code = concept.code
        WHERE country.country_code = 'JP') <> 108
       OR (SELECT count(*)
           FROM ingredient_culinary_country country
           JOIN ingredient_concept concept ON concept.id = country.ingredient_concept_id
           JOIN japan_new_concept approved ON approved.code = concept.code
           WHERE country.country_code = 'JP') <> 20 THEN
        RAISE EXCEPTION 'Japan curation country-relation target write failed';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ingredient_seasonality seasonality
        JOIN ingredient_concept concept ON concept.id = seasonality.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)
    ) THEN
        RAISE EXCEPTION 'Japan curation expected default seasonality without explicit rows';
    END IF;
END;
$validation$;
