--liquibase formatted sql
--changeset venomenon328:016-final-catalog-snapshot splitStatements:false

-- Mise en Dice - approved canonical final catalog snapshot from 2026-08-13
-- Requires: catalog/015-catalog-gap-review.sql
-- Requires: schema/004-persisted-candidate-generation.sql
--
-- This one-time migration accepts exactly the untouched repository baseline or
-- the reviewed production catalog fixture. Technical IDs, timestamps and
-- aggregate versions are deliberately excluded from the input fingerprint.
-- The normalized target snapshot is stored at
-- db/catalog/final-catalog-snapshot-20260813.txt and is verified by PostgreSQL
-- integration tests. The migration is not runAlways: later editorial changes
-- remain authoritative after this changeset has been recorded.

DO $final_catalog$
DECLARE
    actual_fingerprint text;
BEGIN
    WITH canonical_lines(line) AS (
        SELECT 'participant|' || jsonb_build_array(code, display_name, active)::text FROM participant
        UNION ALL SELECT 'functional_role|' || jsonb_build_array(code, display_name, description)::text FROM functional_role
        UNION ALL SELECT 'culinary_flag|' || jsonb_build_array(code, display_name, description)::text FROM culinary_flag
        UNION ALL SELECT 'culinary_dimension|' || jsonb_build_array(code, display_name, description)::text FROM culinary_dimension
        UNION ALL
        SELECT 'ingredient_concept|' || jsonb_build_array(
            code, display_name, active, random_draw_enabled, challenge_specificity,
            to_char(base_draw_weight, 'FM999999990.0000'), novelty_level, curator_note
        )::text
        FROM ingredient_concept
        UNION ALL
        SELECT 'ingredient_refinement|' || jsonb_build_array(parent.code, child.code)::text
        FROM ingredient_refinement relation
        JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
        JOIN ingredient_concept child ON child.id = relation.child_concept_id
        UNION ALL
        SELECT 'ingredient_functional_role|' || jsonb_build_array(concept.code, role.code)::text
        FROM ingredient_functional_role assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        JOIN functional_role role ON role.id = assignment.functional_role_id
        UNION ALL
        SELECT 'ingredient_culinary_flag|' || jsonb_build_array(concept.code, flag.code)::text
        FROM ingredient_culinary_flag assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        JOIN culinary_flag flag ON flag.id = assignment.culinary_flag_id
        UNION ALL
        SELECT 'ingredient_culinary_dimension|' || jsonb_build_array(concept.code, dimension.code, assignment.level)::text
        FROM ingredient_culinary_dimension assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        JOIN culinary_dimension dimension ON dimension.id = assignment.culinary_dimension_id
        UNION ALL
        SELECT 'ingredient_availability|' || jsonb_build_array(concept.code, participant.code, availability.availability_level)::text
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        UNION ALL
        SELECT 'ingredient_seasonality|' || jsonb_build_array(
            concept.code, seasonality.month, to_char(seasonality.weight_multiplier, 'FM999999990.0000')
        )::text
        FROM ingredient_seasonality seasonality
        JOIN ingredient_concept concept ON concept.id = seasonality.ingredient_concept_id
        UNION ALL
        SELECT 'exclusion_rule|' || jsonb_build_array(
            code, display_text, active, to_char(base_draw_weight, 'FM999999990.0000'), curator_note
        )::text
        FROM exclusion_rule
        UNION ALL
        SELECT 'exclusion_rule_target|' || jsonb_build_array(rule.code, concept.code, target.include_refinements)::text
        FROM exclusion_rule_target target
        JOIN exclusion_rule rule ON rule.id = target.exclusion_rule_id
        JOIN ingredient_concept concept ON concept.id = target.ingredient_concept_id
    )
    SELECT md5(string_agg(line, E'\n' ORDER BY line))
      INTO actual_fingerprint
      FROM canonical_lines;

    IF actual_fingerprint NOT IN (
        '511118414a53aa9118a3212b7912a961', -- untouched repository baseline
        '94be058535b8f5cc026085bfaf268173'  -- reviewed production fixture
    ) THEN
        RAISE EXCEPTION
            'final catalog snapshot refuses unknown starting state (canonical MD5 %)',
            actual_fingerprint;
    END IF;
END;
$final_catalog$;

INSERT INTO culinary_dimension (code, display_name, description)
VALUES (
    'SALTINESS',
    'Salzigkeit',
    'Typische wahrgenommene Salzigkeit beziehungsweise notwendige Dosierung der Zutat.'
);

-- Preserve the explicitly approved production decisions for broad or very
-- ordinary draw candidates when starting from the repository baseline.
UPDATE ingredient_concept
SET random_draw_enabled = false
WHERE random_draw_enabled
  AND code = ANY (ARRAY[
      'BLACK_PEPPER', 'BROWN_SUGAR', 'BUTTER', 'CHICKPEA_FLOUR',
      'COOKING_FATS', 'CULTURED_DAIRY', 'FLOUR', 'FRESH_DAIRY_PRODUCTS',
      'FRESH_HERBS', 'GARLIC_POWDER', 'GRAINS', 'HOT_PAPRIKA_POWDER',
      'LEGUMES', 'MAGGI_SEASONING', 'MEAT', 'MSG', 'NEUTRAL_OIL', 'OILS',
      'OILY_FISH', 'PAPRIKA_POWDER', 'PEPPER', 'PLANT_PROTEIN_PRODUCTS',
      'PRESERVED_PRODUCE', 'RICE_FLOUR', 'RYE_FLOUR', 'SAUCES_AND_PASTES',
      'SEAFOOD', 'SEEDS', 'SHELLFISH', 'SPICE_BLENDS', 'SPICES',
      'STARCH_BINDERS', 'STARCHES', 'STOCKS', 'SWEET_PAPRIKA_POWDER',
      'SWEETENERS', 'TOMATO_PRODUCTS', 'VEGETABLES', 'WHEAT_FLOUR',
      'WHITE_FISH', 'WHITE_PEPPER', 'WHITE_SUGAR'
  ]::text[]);

DELETE FROM ingredient_availability availability
USING ingredient_concept concept, participant
WHERE availability.ingredient_concept_id = concept.id
  AND availability.participant_id = participant.id
  AND concept.code IN ('FRESH_HERBS', 'SPICES')
  AND participant.code IN ('TOBIAS', 'GEORGIA');

UPDATE ingredient_availability availability
SET availability_level = correction.availability_level
FROM ingredient_concept concept,
     participant,
     (VALUES
         ('KAFFIR_LIME_LEAVES', 'TOBIAS', 'EASY'),
         ('KAFFIR_LIME_LEAVES', 'GEORGIA', 'EASY'),
         ('KASHMIRI_CHILI_POWDER', 'TOBIAS', 'DIFFICULT'),
         ('KASHMIRI_CHILI_POWDER', 'GEORGIA', 'DIFFICULT')
     ) AS correction(concept_code, participant_code, availability_level)
WHERE availability.ingredient_concept_id = concept.id
  AND availability.participant_id = participant.id
  AND concept.code = correction.concept_code
  AND participant.code = correction.participant_code
  AND availability.availability_level <> correction.availability_level;

UPDATE ingredient_concept concept
SET display_name = correction.display_name,
    challenge_specificity = correction.challenge_specificity
FROM (VALUES
    ('BAGOONG', 'Bagoong', 'OPEN'),
    ('COCONUT_PRODUCTS', 'Kokoszutat', 'OPEN'),
    ('FRESH_HERBS', 'Kräuter und Würzblätter', 'OPEN'),
    ('KAFFIR_LIME_LEAVES', 'Makrut-Limettenblätter', 'SPECIFIC'),
    ('LARD', 'Schweineschmalz', 'SPECIFIC'),
    ('ROOT_VEGETABLES', 'Wurzel- und Knollengemüse', 'OPEN'),
    ('SAUCES_AND_PASTES', 'Sauce, Würzmittel oder Paste', 'OPEN'),
    ('SOY_PRODUCTS', 'Sojazutat', 'OPEN'),
    ('SPICES', 'Gewürz oder trockenes Würzmittel', 'OPEN'),
    ('STARCHES', 'stärkehaltige Zutat oder Sättigungsbeilage', 'OPEN')
) AS correction(code, display_name, challenge_specificity)
WHERE concept.code = correction.code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('READY_SAUCES_AND_PASTES', 'fertige Sauce oder Würzpaste', true, false, 'OPEN', 1.0000, 1,
        'Enger nicht ziehbarer Strukturknoten für tatsächlich fertige Saucen, Würzen und Pasten.'),
    ('BAGOONG_ALAMANG', 'Bagoong alamang', true, true, 'SPECIFIC', 0.3000, 4,
        'Filipinische fermentierte Würzzutat auf Garnelenbasis.'),
    ('BAGOONG_ISDA', 'Bagoong isda', true, true, 'SPECIFIC', 0.2500, 4,
        'Filipinische fermentierte Würzzutat auf Fischbasis.'),
    ('BAY_LEAF', 'Lorbeerblatt', true, true, 'SPECIFIC', 0.6000, 1, null),
    ('CHILI_OIL', 'Chiliöl', true, true, 'SPECIFIC', 0.5500, 2, null),
    ('DAIKON', 'Daikon', true, true, 'SPECIFIC', 0.6500, 2, null),
    ('WHITE_VINEGAR', 'Branntweinessig', true, true, 'SPECIFIC', 0.6000, 1, null),
    ('TOMATO_SAUCE', 'einfache Tomatensauce', true, true, 'SPECIFIC', 0.6000, 1,
        'Schlichte fertige Tomatensauce ohne Bindung an eine konkrete Gewürzrichtung.'),
    ('PANKO', 'Panko', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('SUSHI_RICE', 'Sushireis', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('MUSHROOM_STOCK', 'Pilzfond', true, true, 'SPECIFIC', 0.5000, 2, null),
    ('HAM', 'Schinken', true, true, 'OPEN', 0.5500, 1,
        'Offene Schinkenfamilie für gekochte und luftgetrocknete Varianten.'),
    ('LAMB_LEG', 'Lammkeule', true, true, 'SPECIFIC', 0.4500, 2, null),
    ('LAMB_SHOULDER', 'Lammschulter', true, true, 'SPECIFIC', 0.4500, 2, null),
    ('LAMB_CHOP', 'Lammkotelett', true, true, 'SPECIFIC', 0.4500, 2, null),
    ('LAMB_MINCE', 'Lammhack', true, true, 'SPECIFIC', 0.4500, 2, null),
    ('OOLONG_TEA', 'Oolongtee', true, true, 'SPECIFIC', 0.3000, 2, null),
    ('WHITE_TEA', 'weißer Tee', true, true, 'SPECIFIC', 0.3000, 2, null),
    ('MILK_CHOCOLATE', 'Milchschokolade', true, true, 'SPECIFIC', 0.4500, 1, null),
    ('WHITE_CHOCOLATE', 'weiße Schokolade', true, true, 'SPECIFIC', 0.4000, 2, null),
    ('RUM', 'Rum', true, true, 'SPECIFIC', 0.3000, 2, null),
    ('BRANDY', 'Brandy oder Cognac', true, true, 'SPECIFIC', 0.3000, 2, null),
    ('MARSALA', 'Marsala', true, true, 'SPECIFIC', 0.3000, 3, null),
    ('PORT_WINE', 'Portwein', true, true, 'SPECIFIC', 0.3000, 2, null),
    ('PORK_STOCK', 'Schweinefond', true, true, 'SPECIFIC', 0.4500, 2, null),
    ('WATER_SPINACH', 'Wasserspinat oder Kangkong', true, true, 'SPECIFIC', 0.3000, 4, null),
    ('GREEN_PAPAYA', 'grüne Papaya', true, true, 'SPECIFIC', 0.3000, 3, null),
    ('BITTER_MELON', 'Bittermelone', true, true, 'SPECIFIC', 0.3000, 4, null),
    ('PANDAN_LEAVES', 'Pandanblätter', true, true, 'SPECIFIC', 0.2500, 4, null),
    ('PLANT_DRINKS', 'pflanzlicher Drink', true, false, 'OPEN', 1.0000, 1,
        'Nicht ziehbare Familie pflanzlicher Drinks; Rollen folgen der kulinarischen Verwendung als Milchalternative.'),
    ('OAT_DRINK', 'Haferdrink', true, true, 'SPECIFIC', 0.6000, 1, null),
    ('SOY_DRINK', 'Sojadrink', true, true, 'SPECIFIC', 0.6000, 1, null),
    ('ALMOND_DRINK', 'Mandeldrink', true, true, 'SPECIFIC', 0.6000, 1, null);

-- Remove origin-only or otherwise superseded interpretations before adding the
-- approved graph. Bagoong itself remains an open fermented seasoning family.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent,
      ingredient_concept child,
      (VALUES
          ('SHRIMP', 'SHRIMP_PASTE'),
          ('CRAB', 'ALIGUE'),
          ('PEAS', 'EDAMAME'),
          ('PLANT_PROTEIN_PRODUCTS', 'NUTRITIONAL_YEAST'),
          ('SALAD_GREENS', 'SPINACH'),
          ('GRAINS', 'FLOUR'),
          ('BREAD', 'BREADCRUMBS'),
          ('SAUCES_AND_PASTES', 'TOMATO_PRODUCTS'),
          ('SHRIMP_PASTE', 'BAGOONG')
      ) AS obsolete(parent_code, child_code)
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = obsolete.parent_code
  AND child.code = obsolete.child_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('BAGOONG', 'BAGOONG_ALAMANG'),
    ('BAGOONG', 'BAGOONG_ISDA'),
    ('SHRIMP_PASTE', 'BAGOONG_ALAMANG'),
    ('SOYBEANS', 'EDAMAME'),
    ('SPICES', 'NUTRITIONAL_YEAST'),
    ('LEAFY_GREENS', 'SPINACH'),
    ('ROOT_VEGETABLES', 'POTATO'),
    ('ROOT_VEGETABLES', 'SWEET_POTATO'),
    ('STARCHES', 'FLOUR'),
    ('STARCHES', 'BREADCRUMBS'),
    ('PEANUT', 'PEANUT_BUTTER'),
    ('SESAME_SEEDS', 'TAHINI'),
    ('SAUCES_AND_PASTES', 'READY_SAUCES_AND_PASTES'),
    ('READY_SAUCES_AND_PASTES', 'READY_CURRY_PASTE'),
    ('READY_SAUCES_AND_PASTES', 'CHILI_CONDIMENTS'),
    ('READY_SAUCES_AND_PASTES', 'MUSTARD'),
    ('READY_SAUCES_AND_PASTES', 'SOY_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'AJVAR'),
    ('READY_SAUCES_AND_PASTES', 'OYSTER_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'BANANA_KETCHUP'),
    ('READY_SAUCES_AND_PASTES', 'BARBECUE_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'FISH_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'HOISIN_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'KETCHUP'),
    ('READY_SAUCES_AND_PASTES', 'MAGGI_SEASONING'),
    ('READY_SAUCES_AND_PASTES', 'MAYONNAISE'),
    ('READY_SAUCES_AND_PASTES', 'MISO'),
    ('READY_SAUCES_AND_PASTES', 'DOENJANG'),
    ('READY_SAUCES_AND_PASTES', 'SHRIMP_PASTE'),
    ('READY_SAUCES_AND_PASTES', 'MOLE_PASTE'),
    ('READY_SAUCES_AND_PASTES', 'PESTO'),
    ('READY_SAUCES_AND_PASTES', 'PONZU'),
    ('READY_SAUCES_AND_PASTES', 'SALSA'),
    ('READY_SAUCES_AND_PASTES', 'TAPENADE'),
    ('READY_SAUCES_AND_PASTES', 'TERIYAKI_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'WORCESTERSHIRE_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'XO_SAUCE'),
    ('READY_SAUCES_AND_PASTES', 'YEAST_EXTRACT'),
    ('READY_SAUCES_AND_PASTES', 'TOMATO_SAUCE'),
    ('FRESH_HERBS', 'BAY_LEAF'),
    ('CHILI_CONDIMENTS', 'CHILI_OIL'),
    ('OILS', 'CHILI_OIL'),
    ('RADISH', 'DAIKON'),
    ('VINEGAR', 'WHITE_VINEGAR'),
    ('TOMATO_PRODUCTS', 'TOMATO_SAUCE'),
    ('BREADCRUMBS', 'PANKO'),
    ('RICE', 'SUSHI_RICE'),
    ('STOCKS', 'MUSHROOM_STOCK'),
    ('CURED_MEAT', 'HAM'),
    ('PORK', 'HAM'),
    ('HAM', 'COOKED_HAM'),
    ('HAM', 'PROSCIUTTO'),
    ('HAM', 'SERRANO_HAM'),
    ('LAMB', 'LAMB_LEG'),
    ('LAMB', 'LAMB_SHOULDER'),
    ('LAMB', 'LAMB_CHOP'),
    ('LAMB', 'LAMB_MINCE'),
    ('MINCED_MEAT', 'LAMB_MINCE'),
    ('TEA', 'OOLONG_TEA'),
    ('TEA', 'WHITE_TEA'),
    ('COCOA_PRODUCTS', 'MILK_CHOCOLATE'),
    ('COCOA_PRODUCTS', 'WHITE_CHOCOLATE'),
    ('COOKING_ALCOHOL', 'RUM'),
    ('COOKING_ALCOHOL', 'BRANDY'),
    ('COOKING_ALCOHOL', 'MARSALA'),
    ('COOKING_ALCOHOL', 'PORT_WINE'),
    ('STOCKS', 'PORK_STOCK'),
    ('LEAFY_GREENS', 'WATER_SPINACH'),
    ('PAPAYA', 'GREEN_PAPAYA'),
    ('FRUIT_VEGETABLES', 'BITTER_MELON'),
    ('FRESH_HERBS', 'PANDAN_LEAVES'),
    ('PLANT_DRINKS', 'OAT_DRINK'),
    ('PLANT_DRINKS', 'SOY_DRINK'),
    ('PLANT_DRINKS', 'ALMOND_DRINK')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

-- Keep the direct graph transitively reduced after the new intermediate
-- families were introduced.
WITH RECURSIVE alternate_paths(parent_concept_id, child_concept_id) AS (
    SELECT first.parent_concept_id, second.child_concept_id
    FROM ingredient_refinement first
    JOIN ingredient_refinement second ON second.parent_concept_id = first.child_concept_id
    UNION
    SELECT alternate.parent_concept_id, next.child_concept_id
    FROM alternate_paths alternate
    JOIN ingredient_refinement next ON next.parent_concept_id = alternate.child_concept_id
)
DELETE FROM ingredient_refinement direct
USING alternate_paths alternate
WHERE direct.parent_concept_id = alternate.parent_concept_id
  AND direct.child_concept_id = alternate.child_concept_id;

DELETE FROM ingredient_functional_role assignment
USING ingredient_concept concept, functional_role role
WHERE assignment.ingredient_concept_id = concept.id
  AND assignment.functional_role_id = role.id
  AND (concept.code, role.code) IN (
      ('BAGOONG', 'ANIMAL_PROTEIN'),
      ('BELACAN', 'ANIMAL_PROTEIN'),
      ('MAM_RUOC', 'ANIMAL_PROTEIN'),
      ('SHRIMP_PASTE', 'ANIMAL_PROTEIN'),
      ('ALIGUE', 'ANIMAL_PROTEIN'),
      ('BEEF_STOCK', 'ANIMAL_PROTEIN'),
      ('CHICKEN_STOCK', 'ANIMAL_PROTEIN'),
      ('FISH_STOCK', 'ANIMAL_PROTEIN'),
      ('DASHI', 'ANIMAL_PROTEIN'),
      ('DUCK_FAT', 'ANIMAL_PROTEIN'),
      ('LARD', 'ANIMAL_PROTEIN'),
      ('VEGETABLE_STOCK', 'VEGETABLE'),
      ('NUTRITIONAL_YEAST', 'PLANT_PROTEIN')
  );

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('CHEESE', 'ANIMAL_PROTEIN'),
    ('TOMATO', 'ACID'),
    ('CULTURED_DAIRY', 'ACID'),
    ('YOGURT', 'ACID'),
    ('GREEK_YOGURT', 'ACID'),
    ('POTATO', 'VEGETABLE'),
    ('BAGOONG_ALAMANG', 'SEASONING'),
    ('BAGOONG_ISDA', 'SEASONING'),
    ('READY_SAUCES_AND_PASTES', 'SEASONING'),
    ('BAY_LEAF', 'AROMATIC'),
    ('BAY_LEAF', 'SEASONING'),
    ('CHILI_OIL', 'FAT'),
    ('CHILI_OIL', 'SEASONING'),
    ('DAIKON', 'VEGETABLE'),
    ('WHITE_VINEGAR', 'ACID'),
    ('WHITE_VINEGAR', 'SEASONING'),
    ('TOMATO_SAUCE', 'VEGETABLE'),
    ('TOMATO_SAUCE', 'ACID'),
    ('TOMATO_SAUCE', 'SEASONING'),
    ('PANKO', 'STARCH'),
    ('SUSHI_RICE', 'STARCH'),
    ('MUSHROOM_STOCK', 'SEASONING'),
    ('HAM', 'ANIMAL_PROTEIN'),
    ('HAM', 'FAT'),
    ('HAM', 'SEASONING'),
    ('LAMB_LEG', 'ANIMAL_PROTEIN'),
    ('LAMB_SHOULDER', 'ANIMAL_PROTEIN'),
    ('LAMB_SHOULDER', 'FAT'),
    ('LAMB_CHOP', 'ANIMAL_PROTEIN'),
    ('LAMB_CHOP', 'FAT'),
    ('LAMB_MINCE', 'ANIMAL_PROTEIN'),
    ('LAMB_MINCE', 'FAT'),
    ('OOLONG_TEA', 'AROMATIC'),
    ('OOLONG_TEA', 'SEASONING'),
    ('WHITE_TEA', 'AROMATIC'),
    ('WHITE_TEA', 'SEASONING'),
    ('MILK_CHOCOLATE', 'FAT'),
    ('MILK_CHOCOLATE', 'AROMATIC'),
    ('MILK_CHOCOLATE', 'SEASONING'),
    ('WHITE_CHOCOLATE', 'FAT'),
    ('WHITE_CHOCOLATE', 'AROMATIC'),
    ('WHITE_CHOCOLATE', 'SEASONING'),
    ('RUM', 'ACID'),
    ('RUM', 'SEASONING'),
    ('BRANDY', 'ACID'),
    ('BRANDY', 'SEASONING'),
    ('MARSALA', 'ACID'),
    ('MARSALA', 'SEASONING'),
    ('PORT_WINE', 'ACID'),
    ('PORT_WINE', 'SEASONING'),
    ('PORK_STOCK', 'SEASONING'),
    ('WATER_SPINACH', 'VEGETABLE'),
    ('GREEN_PAPAYA', 'FRUIT'),
    ('GREEN_PAPAYA', 'VEGETABLE'),
    ('BITTER_MELON', 'VEGETABLE'),
    ('PANDAN_LEAVES', 'AROMATIC'),
    ('PLANT_DRINKS', 'FAT'),
    ('OAT_DRINK', 'FAT'),
    ('SOY_DRINK', 'FAT'),
    ('ALMOND_DRINK', 'FAT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code
ON CONFLICT (ingredient_concept_id, functional_role_id) DO NOTHING;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('BAGOONG_ALAMANG', 'TOBIAS', 'PLANNED'), ('BAGOONG_ALAMANG', 'GEORGIA', 'EASY'),
    ('BAGOONG_ISDA', 'TOBIAS', 'DIFFICULT'), ('BAGOONG_ISDA', 'GEORGIA', 'PLANNED'),
    ('BAY_LEAF', 'TOBIAS', 'EASY'), ('BAY_LEAF', 'GEORGIA', 'EASY'),
    ('CHILI_OIL', 'TOBIAS', 'EASY'), ('CHILI_OIL', 'GEORGIA', 'EASY'),
    ('DAIKON', 'TOBIAS', 'PLANNED'), ('DAIKON', 'GEORGIA', 'EASY'),
    ('WHITE_VINEGAR', 'TOBIAS', 'EASY'), ('WHITE_VINEGAR', 'GEORGIA', 'EASY'),
    ('TOMATO_SAUCE', 'TOBIAS', 'EASY'), ('TOMATO_SAUCE', 'GEORGIA', 'EASY'),
    ('PANKO', 'TOBIAS', 'EASY'), ('PANKO', 'GEORGIA', 'EASY'),
    ('SUSHI_RICE', 'TOBIAS', 'EASY'), ('SUSHI_RICE', 'GEORGIA', 'EASY'),
    ('MUSHROOM_STOCK', 'TOBIAS', 'PLANNED'), ('MUSHROOM_STOCK', 'GEORGIA', 'EASY'),
    ('HAM', 'TOBIAS', 'EASY'), ('HAM', 'GEORGIA', 'EASY'),
    ('LAMB_LEG', 'TOBIAS', 'PLANNED'), ('LAMB_LEG', 'GEORGIA', 'PLANNED'),
    ('LAMB_SHOULDER', 'TOBIAS', 'PLANNED'), ('LAMB_SHOULDER', 'GEORGIA', 'PLANNED'),
    ('LAMB_CHOP', 'TOBIAS', 'PLANNED'), ('LAMB_CHOP', 'GEORGIA', 'PLANNED'),
    ('LAMB_MINCE', 'TOBIAS', 'PLANNED'), ('LAMB_MINCE', 'GEORGIA', 'PLANNED'),
    ('OOLONG_TEA', 'TOBIAS', 'EASY'), ('OOLONG_TEA', 'GEORGIA', 'EASY'),
    ('WHITE_TEA', 'TOBIAS', 'EASY'), ('WHITE_TEA', 'GEORGIA', 'EASY'),
    ('MILK_CHOCOLATE', 'TOBIAS', 'EASY'), ('MILK_CHOCOLATE', 'GEORGIA', 'EASY'),
    ('WHITE_CHOCOLATE', 'TOBIAS', 'EASY'), ('WHITE_CHOCOLATE', 'GEORGIA', 'EASY'),
    ('RUM', 'TOBIAS', 'EASY'), ('RUM', 'GEORGIA', 'EASY'),
    ('BRANDY', 'TOBIAS', 'EASY'), ('BRANDY', 'GEORGIA', 'EASY'),
    ('MARSALA', 'TOBIAS', 'PLANNED'), ('MARSALA', 'GEORGIA', 'PLANNED'),
    ('PORT_WINE', 'TOBIAS', 'PLANNED'), ('PORT_WINE', 'GEORGIA', 'EASY'),
    ('PORK_STOCK', 'TOBIAS', 'PLANNED'), ('PORK_STOCK', 'GEORGIA', 'PLANNED'),
    ('WATER_SPINACH', 'TOBIAS', 'DIFFICULT'), ('WATER_SPINACH', 'GEORGIA', 'PLANNED'),
    ('GREEN_PAPAYA', 'TOBIAS', 'DIFFICULT'), ('GREEN_PAPAYA', 'GEORGIA', 'PLANNED'),
    ('BITTER_MELON', 'TOBIAS', 'DIFFICULT'), ('BITTER_MELON', 'GEORGIA', 'PLANNED'),
    ('PANDAN_LEAVES', 'TOBIAS', 'DIFFICULT'), ('PANDAN_LEAVES', 'GEORGIA', 'PLANNED'),
    ('OAT_DRINK', 'TOBIAS', 'EASY'), ('OAT_DRINK', 'GEORGIA', 'EASY'),
    ('SOY_DRINK', 'TOBIAS', 'EASY'), ('SOY_DRINK', 'GEORGIA', 'EASY'),
    ('ALMOND_DRINK', 'TOBIAS', 'EASY'), ('ALMOND_DRINK', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN participant ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('BAGOONG_ALAMANG', 'FERMENTED'),
    ('BAGOONG_ISDA', 'FERMENTED'),
    ('BAY_LEAF', 'DRIED'),
    ('PANKO', 'DRIED'),
    ('OOLONG_TEA', 'DRIED'),
    ('WHITE_TEA', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

-- Approved season transfer. Cultivated champignons deliberately remain
-- without a season profile.
INSERT INTO ingredient_seasonality (ingredient_concept_id, month, weight_multiplier)
SELECT target.id, source.month, source.weight_multiplier
FROM ingredient_concept source_concept
JOIN ingredient_seasonality source ON source.ingredient_concept_id = source_concept.id
JOIN ingredient_concept target ON target.code = ANY (
    CASE source_concept.code
        WHEN 'BELL_PEPPER' THEN ARRAY['RED_BELL_PEPPER', 'YELLOW_BELL_PEPPER', 'GREEN_BELL_PEPPER']::text[]
        WHEN 'PUMPKIN' THEN ARRAY['HOKKAIDO_SQUASH', 'BUTTERNUT_SQUASH', 'SPAGHETTI_SQUASH']::text[]
    END
)
WHERE source_concept.code IN ('BELL_PEPPER', 'PUMPKIN');

INSERT INTO ingredient_seasonality (ingredient_concept_id, month, weight_multiplier)
SELECT concept.id, profile.month, profile.weight_multiplier
FROM ingredient_concept concept
CROSS JOIN (VALUES
    (1, 0.6000), (2, 0.7000), (3, 1.1000), (4, 1.5000),
    (5, 1.6000), (6, 1.4000), (7, 0.9000), (8, 0.8000),
    (9, 1.1000), (10, 1.4000), (11, 1.0000), (12, 0.7000)
) AS profile(month, weight_multiplier)
WHERE concept.code = 'ARTICHOKE';

-- Exclusion targets are reconciled explicitly because origin-only graph edges
-- were intentionally removed.
DELETE FROM exclusion_rule_target target
USING exclusion_rule rule, ingredient_concept concept
WHERE target.exclusion_rule_id = rule.id
  AND target.ingredient_concept_id = concept.id
  AND rule.code = 'NO_READY_SAUCES'
  AND concept.code = 'SAUCES_AND_PASTES';

UPDATE exclusion_rule_target target
SET include_refinements = true
FROM exclusion_rule rule, ingredient_concept concept
WHERE target.exclusion_rule_id = rule.id
  AND target.ingredient_concept_id = concept.id
  AND (rule.code, concept.code) IN (
      ('NO_RICE', 'RICE'),
      ('NO_SOY_SAUCE', 'SOY_SAUCE')
  );

INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, assignment.include_refinements
FROM (VALUES
    ('NO_READY_SAUCES', 'READY_SAUCES_AND_PASTES', true),
    ('NO_EGGS', 'MAYONNAISE', false),
    ('NO_EGGS', 'EGG_NOODLES', false),
    ('NO_EGGS', 'SPAETZLE', false),
    ('NO_FISH_OR_SEAFOOD', 'FISH_STOCK', false),
    ('NO_FISH_OR_SEAFOOD', 'FISH_SAUCE', false),
    ('NO_FISH_OR_SEAFOOD', 'OYSTER_SAUCE', false),
    ('NO_FISH_OR_SEAFOOD', 'SHRIMP_PASTE', true),
    ('NO_FISH_OR_SEAFOOD', 'BAGOONG', true),
    ('NO_FISH_OR_SEAFOOD', 'ALIGUE', false),
    ('NO_FISH_OR_SEAFOOD', 'XO_SAUCE', false),
    ('NO_CHILI', 'KIMCHI', false),
    ('NO_CHILI', 'BERBERE', false),
    ('NO_CHILI', 'NDUJA', false),
    ('NO_BEEF', 'BEEF_STOCK', false),
    ('NO_PORK', 'LARD', false),
    ('NO_PORK', 'PORK_STOCK', false),
    ('NO_POULTRY', 'CHICKEN_STOCK', false),
    ('NO_POULTRY', 'DUCK_FAT', false),
    ('NO_MEAT', 'BEEF_STOCK', false),
    ('NO_MEAT', 'CHICKEN_STOCK', false),
    ('NO_MEAT', 'DUCK_FAT', false),
    ('NO_MEAT', 'LARD', false),
    ('NO_MEAT', 'PORK_STOCK', false),
    ('NO_NUTS', 'ALMOND_DRINK', false),
    ('NO_LEGUMES', 'SOY_DRINK', false)
) AS assignment(rule_code, concept_code, include_refinements)
JOIN exclusion_rule rule ON rule.code = assignment.rule_code
JOIN ingredient_concept concept ON concept.code = assignment.concept_code;

-- Dominance is useful for every concrete draw candidate. The explicit values
-- below complete the previously unprofiled SPECIFIC candidates; heterogeneous
-- OPEN families deliberately remain unset unless a stable value already exists.
INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id,
    culinary_dimension_id,
    level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('ADZUKI_BEANS', 'DOMINANCE', 3),
    ('ALFALFA_SPROUTS', 'DOMINANCE', 3),
    ('ALMOND', 'DOMINANCE', 2),
    ('ALMOND_DRINK', 'DOMINANCE', 2),
    ('APPLE', 'DOMINANCE', 2),
    ('APRICOT', 'DOMINANCE', 2),
    ('ARTICHOKE', 'DOMINANCE', 4),
    ('ASPARAGUS', 'DOMINANCE', 3),
    ('BAGOONG_ALAMANG', 'DOMINANCE', 5),
    ('BAGOONG_ISDA', 'DOMINANCE', 5),
    ('BAMBOO_SHOOTS', 'DOMINANCE', 2),
    ('BASIL', 'DOMINANCE', 4),
    ('BAY_LEAF', 'DOMINANCE', 3),
    ('BEAN_SPROUTS', 'DOMINANCE', 3),
    ('BEEF_BRISKET', 'DOMINANCE', 3),
    ('BEEF_CHEEK', 'DOMINANCE', 4),
    ('BEEF_GOULASH', 'DOMINANCE', 3),
    ('BEEF_MINCE', 'DOMINANCE', 3),
    ('BEEF_ROAST', 'DOMINANCE', 3),
    ('BEEF_ROULADE', 'DOMINANCE', 3),
    ('BEEF_RUMP', 'DOMINANCE', 3),
    ('BEEF_SHIN', 'DOMINANCE', 3),
    ('BEEF_SHORT_RIBS', 'DOMINANCE', 3),
    ('BEEF_STEAK', 'DOMINANCE', 3),
    ('BEEF_TENDERLOIN', 'DOMINANCE', 3),
    ('BELL_PEPPER', 'DOMINANCE', 3),
    ('BELUGA_LENTILS', 'DOMINANCE', 3),
    ('BITTER_MELON', 'DOMINANCE', 4),
    ('BLACK_BEANS', 'DOMINANCE', 2),
    ('BLACKBERRY', 'DOMINANCE', 3),
    ('BLACK_EYED_PEAS', 'DOMINANCE', 3),
    ('BLUEBERRY', 'DOMINANCE', 2),
    ('BRANDY', 'DOMINANCE', 3),
    ('BRAZIL_NUT', 'DOMINANCE', 3),
    ('BROAD_BEANS', 'DOMINANCE', 3),
    ('BROCCOLI', 'DOMINANCE', 3),
    ('BROWN_CHAMPIGNON', 'DOMINANCE', 3),
    ('BRUSSELS_SPROUTS', 'DOMINANCE', 4),
    ('BULGUR', 'DOMINANCE', 2),
    ('BUTTER_BEANS', 'DOMINANCE', 2),
    ('BUTTERMILK', 'DOMINANCE', 2),
    ('BUTTERNUT_SQUASH', 'DOMINANCE', 3),
    ('CANNED_CHAMPIGNONS', 'DOMINANCE', 3),
    ('CANNED_TOMATOES', 'DOMINANCE', 2),
    ('CANNELLINI_BEANS', 'DOMINANCE', 2),
    ('CARROT', 'DOMINANCE', 2),
    ('CASHEW', 'DOMINANCE', 3),
    ('CASSAVA', 'DOMINANCE', 3),
    ('CATFISH', 'DOMINANCE', 3),
    ('CAULIFLOWER', 'DOMINANCE', 2),
    ('CELERIAC', 'DOMINANCE', 4),
    ('CHARD', 'DOMINANCE', 3),
    ('CHERRY', 'DOMINANCE', 3),
    ('CHERRY_TOMATO', 'DOMINANCE', 3),
    ('CHESTNUT', 'DOMINANCE', 3),
    ('CHIA_SEEDS', 'DOMINANCE', 3),
    ('CHICKEN', 'DOMINANCE', 2),
    ('CHICKEN_BREAST', 'DOMINANCE', 2),
    ('CHICKEN_DRUMSTICKS', 'DOMINANCE', 3),
    ('CHICKEN_THIGH', 'DOMINANCE', 3),
    ('CHICKEN_WINGS', 'DOMINANCE', 3),
    ('CHICKPEAS', 'DOMINANCE', 2),
    ('CHICORY', 'DOMINANCE', 4),
    ('CHILI_OIL', 'DOMINANCE', 3),
    ('CLAMS', 'DOMINANCE', 3),
    ('COD', 'DOMINANCE', 2),
    ('CORN', 'DOMINANCE', 2),
    ('CORNMEAL', 'DOMINANCE', 3),
    ('COUSCOUS', 'DOMINANCE', 1),
    ('CRAB', 'DOMINANCE', 3),
    ('CRANBERRY', 'DOMINANCE', 3),
    ('CRAYFISH', 'DOMINANCE', 3),
    ('CREAM_CHEESE', 'DOMINANCE', 2),
    ('CREME_FRAICHE', 'DOMINANCE', 2),
    ('CUCUMBER', 'DOMINANCE', 1),
    ('DAIKON', 'DOMINANCE', 3),
    ('DRIED_APRICOT', 'DOMINANCE', 3),
    ('DUCK_BREAST', 'DOMINANCE', 4),
    ('DUCK_EGG', 'DOMINANCE', 3),
    ('DUCK_LEG', 'DOMINANCE', 3),
    ('EDAMAME', 'DOMINANCE', 2),
    ('EGG', 'DOMINANCE', 2),
    ('EGGPLANT', 'DOMINANCE', 2),
    ('ENDIVE', 'DOMINANCE', 3),
    ('EVAPORATED_MILK', 'DOMINANCE', 2),
    ('FLAXSEED', 'DOMINANCE', 3),
    ('GOOSEBERRY', 'DOMINANCE', 3),
    ('GRAPE', 'DOMINANCE', 2),
    ('GREEK_YOGURT', 'DOMINANCE', 3),
    ('GREEN_ASPARAGUS', 'DOMINANCE', 3),
    ('GREEN_BEANS', 'DOMINANCE', 2),
    ('GREEN_BELL_PEPPER', 'DOMINANCE', 3),
    ('GREEN_PAPAYA', 'DOMINANCE', 3),
    ('GREEN_PEAS', 'DOMINANCE', 3),
    ('HADDOCK', 'DOMINANCE', 2),
    ('HAKE', 'DOMINANCE', 2),
    ('HALIBUT', 'DOMINANCE', 3),
    ('HALLOUMI', 'DOMINANCE', 3),
    ('HEMP_SEEDS', 'DOMINANCE', 3),
    ('HERRING', 'DOMINANCE', 3),
    ('HOKKAIDO_SQUASH', 'DOMINANCE', 3),
    ('JERUSALEM_ARTICHOKE', 'DOMINANCE', 3),
    ('KALE', 'DOMINANCE', 3),
    ('KEFIR', 'DOMINANCE', 3),
    ('KIDNEY_BEANS', 'DOMINANCE', 2),
    ('KIWI', 'DOMINANCE', 2),
    ('KOHLRABI', 'DOMINANCE', 3),
    ('LAMB', 'DOMINANCE', 4),
    ('LAMB_CHOP', 'DOMINANCE', 3),
    ('LAMB_LEG', 'DOMINANCE', 3),
    ('LAMB_MINCE', 'DOMINANCE', 3),
    ('LAMB_SHOULDER', 'DOMINANCE', 3),
    ('LAMBS_LETTUCE', 'DOMINANCE', 3),
    ('LEEK', 'DOMINANCE', 3),
    ('LENTILS', 'DOMINANCE', 2),
    ('LETTUCE', 'DOMINANCE', 2),
    ('LOBSTER', 'DOMINANCE', 4),
    ('LOTUS_ROOT', 'DOMINANCE', 3),
    ('LUPIN', 'DOMINANCE', 3),
    ('MACADAMIA', 'DOMINANCE', 3),
    ('MANDARIN', 'DOMINANCE', 2),
    ('MARSALA', 'DOMINANCE', 3),
    ('MASCARPONE', 'DOMINANCE', 3),
    ('MILK', 'DOMINANCE', 2),
    ('MILK_CHOCOLATE', 'DOMINANCE', 3),
    ('MIRABELLE', 'DOMINANCE', 3),
    ('MOZZARELLA', 'DOMINANCE', 2),
    ('MULBERRY', 'DOMINANCE', 4),
    ('MUNG_BEANS', 'DOMINANCE', 3),
    ('MUSHROOM_STOCK', 'DOMINANCE', 3),
    ('MYCOPROTEIN', 'DOMINANCE', 3),
    ('NAPA_CABBAGE', 'DOMINANCE', 2),
    ('NECTARINE', 'DOMINANCE', 3),
    ('OAT_DRINK', 'DOMINANCE', 2),
    ('OKRA', 'DOMINANCE', 2),
    ('ONION', 'DOMINANCE', 4),
    ('OOLONG_TEA', 'DOMINANCE', 3),
    ('ORANGE', 'DOMINANCE', 3),
    ('OXTAIL', 'DOMINANCE', 4),
    ('OYSTER_MUSHROOM', 'DOMINANCE', 2),
    ('PAK_CHOI', 'DOMINANCE', 2),
    ('PANDAN_LEAVES', 'DOMINANCE', 4),
    ('PANGASIUS', 'DOMINANCE', 2),
    ('PANKO', 'DOMINANCE', 3),
    ('PARSLEY', 'DOMINANCE', 2),
    ('PARSNIP', 'DOMINANCE', 3),
    ('PEACH', 'DOMINANCE', 2),
    ('PEANUT', 'DOMINANCE', 3),
    ('PEAR', 'DOMINANCE', 3),
    ('PECAN', 'DOMINANCE', 3),
    ('PIKEPERCH', 'DOMINANCE', 3),
    ('PINE_NUT', 'DOMINANCE', 3),
    ('PINTO_BEANS', 'DOMINANCE', 3),
    ('PISTACHIO', 'DOMINANCE', 3),
    ('PLAICE', 'DOMINANCE', 2),
    ('PLUM', 'DOMINANCE', 2),
    ('POINTED_CABBAGE', 'DOMINANCE', 2),
    ('POLENTA', 'DOMINANCE', 2),
    ('POLLOCK', 'DOMINANCE', 2),
    ('POMEGRANATE', 'DOMINANCE', 3),
    ('POMELO', 'DOMINANCE', 3),
    ('PORK_BELLY', 'DOMINANCE', 3),
    ('PORK_CHEEK', 'DOMINANCE', 3),
    ('PORK_CHOP', 'DOMINANCE', 2),
    ('PORK_CUTLET', 'DOMINANCE', 2),
    ('PORK_LOIN', 'DOMINANCE', 2),
    ('PORK_MINCE', 'DOMINANCE', 3),
    ('PORK_NECK', 'DOMINANCE', 3),
    ('PORK_RIBS', 'DOMINANCE', 3),
    ('PORK_SHOULDER', 'DOMINANCE', 3),
    ('PORK_STOCK', 'DOMINANCE', 3),
    ('PORK_TENDERLOIN', 'DOMINANCE', 2),
    ('PORT_WINE', 'DOMINANCE', 3),
    ('POTATO', 'DOMINANCE', 1),
    ('PUMPKIN', 'DOMINANCE', 3),
    ('PUMPKIN_SEEDS', 'DOMINANCE', 3),
    ('PURSLANE', 'DOMINANCE', 3),
    ('QUAIL_EGG', 'DOMINANCE', 3),
    ('QUARK', 'DOMINANCE', 2),
    ('RADICCHIO', 'DOMINANCE', 4),
    ('RASPBERRY', 'DOMINANCE', 3),
    ('RAZOR_CLAMS', 'DOMINANCE', 4),
    ('RED_BELL_PEPPER', 'DOMINANCE', 2),
    ('RED_CABBAGE', 'DOMINANCE', 3),
    ('RED_CURRANT', 'DOMINANCE', 3),
    ('REDFISH', 'DOMINANCE', 2),
    ('RED_LENTILS', 'DOMINANCE', 2),
    ('RICE', 'DOMINANCE', 1),
    ('RICE_NOODLES', 'DOMINANCE', 1),
    ('RICOTTA', 'DOMINANCE', 2),
    ('ROMAINE_LETTUCE', 'DOMINANCE', 2),
    ('ROMANESCO', 'DOMINANCE', 3),
    ('RUM', 'DOMINANCE', 3),
    ('RUTABAGA', 'DOMINANCE', 3),
    ('SALMON', 'DOMINANCE', 3),
    ('SAVOY_CABBAGE', 'DOMINANCE', 3),
    ('SCALLOPS', 'DOMINANCE', 3),
    ('SEA_BASS', 'DOMINANCE', 3),
    ('SEA_BREAM', 'DOMINANCE', 3),
    ('SEITAN', 'DOMINANCE', 2),
    ('SHIITAKE', 'DOMINANCE', 4),
    ('SHRIMP', 'DOMINANCE', 2),
    ('SOBA', 'DOMINANCE', 3),
    ('SOLE', 'DOMINANCE', 4),
    ('SOUR_CHERRY', 'DOMINANCE', 3),
    ('SOUR_CREAM', 'DOMINANCE', 2),
    ('SOYBEANS', 'DOMINANCE', 3),
    ('SOY_DRINK', 'DOMINANCE', 2),
    ('SPAGHETTI_SQUASH', 'DOMINANCE', 3),
    ('SPINACH', 'DOMINANCE', 3),
    ('SPLIT_PEAS', 'DOMINANCE', 2),
    ('SPRING_ONION', 'DOMINANCE', 3),
    ('STRAWBERRY', 'DOMINANCE', 3),
    ('SUGAR_SNAP_PEAS', 'DOMINANCE', 3),
    ('SUNFLOWER_SEEDS', 'DOMINANCE', 3),
    ('SURIMI', 'DOMINANCE', 2),
    ('SUSHI_RICE', 'DOMINANCE', 3),
    ('SWEET_POTATO', 'DOMINANCE', 3),
    ('TARO', 'DOMINANCE', 3),
    ('TOFU', 'DOMINANCE', 1),
    ('TOMATILLO', 'DOMINANCE', 3),
    ('TOMATO', 'DOMINANCE', 3),
    ('TOMATO_PASSATA', 'DOMINANCE', 2),
    ('TOMATO_SAUCE', 'DOMINANCE', 3),
    ('TROUT', 'DOMINANCE', 3),
    ('TURKEY', 'DOMINANCE', 2),
    ('TURKEY_BREAST', 'DOMINANCE', 2),
    ('TURKEY_MINCE', 'DOMINANCE', 3),
    ('TURNIP', 'DOMINANCE', 3),
    ('UDON', 'DOMINANCE', 1),
    ('VEAL_CUTLET', 'DOMINANCE', 3),
    ('VEAL_SHANK', 'DOMINANCE', 3),
    ('WATER_CHESTNUT', 'DOMINANCE', 3),
    ('WATER_SPINACH', 'DOMINANCE', 3),
    ('WHITE_ASPARAGUS', 'DOMINANCE', 3),
    ('WHITE_BEANS', 'DOMINANCE', 2),
    ('WHITE_CABBAGE', 'DOMINANCE', 3),
    ('WHITE_CHAMPIGNON', 'DOMINANCE', 3),
    ('WHITE_CHOCOLATE', 'DOMINANCE', 3),
    ('WHITE_TEA', 'DOMINANCE', 2),
    ('WHITE_VINEGAR', 'DOMINANCE', 4),
    ('YAM', 'DOMINANCE', 3),
    ('YELLOW_BELL_PEPPER', 'DOMINANCE', 3),
    ('YELLOW_LENTILS', 'DOMINANCE', 2),
    ('YOGURT', 'DOMINANCE', 2),
    ('ZUCCHINI', 'DOMINANCE', 1)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code
ON CONFLICT (ingredient_concept_id, culinary_dimension_id)
DO UPDATE SET level = EXCLUDED.level;

-- Further meaningful low-to-high profiles, including the new saltiness axis.
-- Absence still means that a stable comparison is not useful for that concept.
INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id,
    culinary_dimension_id,
    level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('HABANERO', 'SWEETNESS', 1),
    ('BROCCOLI', 'SWEETNESS', 2),
    ('BRUSSELS_SPROUTS', 'SWEETNESS', 2),
    ('CELERIAC', 'BITTERNESS', 2),
    ('SHIITAKE', 'BITTERNESS', 1),
    ('ARTICHOKE', 'SWEETNESS', 1),
    ('BAGOONG_ALAMANG', 'UMAMI', 5), ('BAGOONG_ALAMANG', 'SALTINESS', 5),
    ('BAGOONG_ISDA', 'UMAMI', 5), ('BAGOONG_ISDA', 'SALTINESS', 5),
    ('BAY_LEAF', 'BITTERNESS', 2), ('BAY_LEAF', 'UMAMI', 1),
    ('CHILI_OIL', 'FATTINESS', 5), ('CHILI_OIL', 'HEAT', 3), ('CHILI_OIL', 'SALTINESS', 1),
    ('DAIKON', 'BITTERNESS', 1), ('DAIKON', 'SWEETNESS', 2),
    ('WHITE_VINEGAR', 'ACIDITY', 5),
    ('TOMATO_SAUCE', 'ACIDITY', 3), ('TOMATO_SAUCE', 'SWEETNESS', 2), ('TOMATO_SAUCE', 'UMAMI', 3), ('TOMATO_SAUCE', 'SALTINESS', 2),
    ('PANKO', 'SWEETNESS', 1), ('PANKO', 'SALTINESS', 1),
    ('SUSHI_RICE', 'SWEETNESS', 2), ('SUSHI_RICE', 'ACIDITY', 1),
    ('MUSHROOM_STOCK', 'UMAMI', 4), ('MUSHROOM_STOCK', 'SALTINESS', 2),
    ('HAM', 'DOMINANCE', 4), ('HAM', 'FATTINESS', 3), ('HAM', 'UMAMI', 4), ('HAM', 'SALTINESS', 4),
    ('LAMB_LEG', 'FATTINESS', 2), ('LAMB_LEG', 'UMAMI', 4),
    ('LAMB_SHOULDER', 'FATTINESS', 4), ('LAMB_SHOULDER', 'UMAMI', 4),
    ('LAMB_CHOP', 'FATTINESS', 4), ('LAMB_CHOP', 'UMAMI', 4),
    ('LAMB_MINCE', 'FATTINESS', 4), ('LAMB_MINCE', 'UMAMI', 4),
    ('OOLONG_TEA', 'BITTERNESS', 3),
    ('WHITE_TEA', 'BITTERNESS', 1),
    ('MILK_CHOCOLATE', 'SWEETNESS', 5), ('MILK_CHOCOLATE', 'FATTINESS', 4), ('MILK_CHOCOLATE', 'BITTERNESS', 1),
    ('WHITE_CHOCOLATE', 'SWEETNESS', 5), ('WHITE_CHOCOLATE', 'FATTINESS', 5),
    ('RUM', 'SWEETNESS', 3), ('RUM', 'ACIDITY', 1),
    ('BRANDY', 'SWEETNESS', 2), ('BRANDY', 'ACIDITY', 2),
    ('MARSALA', 'SWEETNESS', 4), ('MARSALA', 'ACIDITY', 2),
    ('PORT_WINE', 'SWEETNESS', 5), ('PORT_WINE', 'ACIDITY', 3),
    ('PORK_STOCK', 'UMAMI', 4), ('PORK_STOCK', 'SALTINESS', 2),
    ('WATER_SPINACH', 'BITTERNESS', 1), ('WATER_SPINACH', 'UMAMI', 2),
    ('GREEN_PAPAYA', 'SWEETNESS', 1), ('GREEN_PAPAYA', 'BITTERNESS', 1),
    ('BITTER_MELON', 'BITTERNESS', 5), ('BITTER_MELON', 'SWEETNESS', 1),
    ('PANDAN_LEAVES', 'BITTERNESS', 1),
    ('OAT_DRINK', 'FATTINESS', 2), ('OAT_DRINK', 'SWEETNESS', 3),
    ('SOY_DRINK', 'FATTINESS', 2), ('SOY_DRINK', 'SWEETNESS', 2), ('SOY_DRINK', 'UMAMI', 2),
    ('ALMOND_DRINK', 'FATTINESS', 3), ('ALMOND_DRINK', 'SWEETNESS', 2),
    ('FISH_SAUCE', 'SALTINESS', 5),
    ('SOY_SAUCE', 'SALTINESS', 5),
    ('LIGHT_SOY_SAUCE', 'SALTINESS', 5),
    ('DARK_SOY_SAUCE', 'SALTINESS', 5),
    ('MAGGI_SEASONING', 'SALTINESS', 5),
    ('SHRIMP_PASTE', 'SALTINESS', 5),
    ('BELACAN', 'SALTINESS', 5),
    ('MAM_RUOC', 'SALTINESS', 5),
    ('DOENJANG', 'SALTINESS', 5),
    ('ANCHOVIES', 'SALTINESS', 5),
    ('CAPERS', 'SALTINESS', 5),
    ('MISO', 'SALTINESS', 4),
    ('OYSTER_SAUCE', 'SALTINESS', 4),
    ('WORCESTERSHIRE_SAUCE', 'SALTINESS', 4),
    ('YEAST_EXTRACT', 'SALTINESS', 4),
    ('OLIVES', 'SALTINESS', 4),
    ('KIMCHI', 'SALTINESS', 4),
    ('SAUERKRAUT', 'SALTINESS', 4),
    ('FETA', 'SALTINESS', 4),
    ('PARMESAN', 'SALTINESS', 4),
    ('BACON', 'SALTINESS', 4),
    ('COOKED_HAM', 'SALTINESS', 4),
    ('PROSCIUTTO', 'SALTINESS', 5),
    ('SERRANO_HAM', 'SALTINESS', 5),
    ('STOCKFISH', 'SALTINESS', 4),
    ('XO_SAUCE', 'SALTINESS', 4),
    ('HOISIN_SAUCE', 'SALTINESS', 3),
    ('TERIYAKI_SAUCE', 'SALTINESS', 3),
    ('BARBECUE_SAUCE', 'SALTINESS', 3),
    ('BANANA_KETCHUP', 'SALTINESS', 2),
    ('KETCHUP', 'SALTINESS', 2),
    ('PONZU', 'SALTINESS', 4),
    ('TAHINI', 'SALTINESS', 1),
    ('PEANUT_BUTTER', 'SALTINESS', 2),
    ('BEEF_STOCK', 'SALTINESS', 2),
    ('CHICKEN_STOCK', 'SALTINESS', 2),
    ('FISH_STOCK', 'SALTINESS', 2),
    ('VEGETABLE_STOCK', 'SALTINESS', 2),
    ('DASHI', 'SALTINESS', 3),
    ('CANNED_TOMATOES', 'SALTINESS', 1),
    ('TOMATO_PASSATA', 'SALTINESS', 1),
    ('CANNED_CHAMPIGNONS', 'SALTINESS', 2),
    ('SURIMI', 'SALTINESS', 3),
    ('HALLOUMI', 'SALTINESS', 4),
    ('CREAM_CHEESE', 'SALTINESS', 2),
    ('MOZZARELLA', 'SALTINESS', 1),
    ('RICOTTA', 'SALTINESS', 1),
    ('QUARK', 'SALTINESS', 1),
    ('BUTTERMILK', 'SALTINESS', 1),
    ('YOGURT', 'SALTINESS', 1),
    ('GREEK_YOGURT', 'SALTINESS', 1),
    ('SOUR_CREAM', 'SALTINESS', 1),
    ('CREME_FRAICHE', 'SALTINESS', 1)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code
ON CONFLICT (ingredient_concept_id, culinary_dimension_id)
DO UPDATE SET level = EXCLUDED.level;

-- Approved review matrix: all 158 target facts for the 67 previously entirely
-- unprofiled candidates.
INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id,
    culinary_dimension_id,
    level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('COD', 'DOMINANCE', 2), ('COD', 'FATTINESS', 1), ('COD', 'UMAMI', 3),
    ('POLLOCK', 'DOMINANCE', 2), ('POLLOCK', 'FATTINESS', 1), ('POLLOCK', 'UMAMI', 3),
    ('TROUT', 'DOMINANCE', 3), ('TROUT', 'FATTINESS', 3), ('TROUT', 'UMAMI', 4),
    ('CHICKEN', 'DOMINANCE', 2), ('CHICKEN', 'FATTINESS', 2), ('CHICKEN', 'UMAMI', 4),
    ('CHICKEN_BREAST', 'DOMINANCE', 2), ('CHICKEN_BREAST', 'FATTINESS', 1), ('CHICKEN_BREAST', 'UMAMI', 3),
    ('CHICKEN_THIGH', 'DOMINANCE', 3), ('CHICKEN_THIGH', 'FATTINESS', 3), ('CHICKEN_THIGH', 'UMAMI', 4),
    ('DUCK_BREAST', 'DOMINANCE', 4), ('DUCK_BREAST', 'FATTINESS', 4), ('DUCK_BREAST', 'UMAMI', 4),
    ('PORK_TENDERLOIN', 'DOMINANCE', 2), ('PORK_TENDERLOIN', 'FATTINESS', 2), ('PORK_TENDERLOIN', 'UMAMI', 3),
    ('BEEF_MINCE', 'DOMINANCE', 3), ('BEEF_MINCE', 'FATTINESS', 3), ('BEEF_MINCE', 'UMAMI', 4),
    ('BEEF_GOULASH', 'DOMINANCE', 3), ('BEEF_GOULASH', 'FATTINESS', 3), ('BEEF_GOULASH', 'UMAMI', 4),
    ('BEEF_STEAK', 'DOMINANCE', 3), ('BEEF_STEAK', 'FATTINESS', 3), ('BEEF_STEAK', 'UMAMI', 4),
    ('LAMB', 'DOMINANCE', 4), ('LAMB', 'FATTINESS', 3), ('LAMB', 'UMAMI', 4),
    ('EGG', 'DOMINANCE', 2), ('EGG', 'FATTINESS', 3), ('EGG', 'UMAMI', 3),
    ('TOFU', 'DOMINANCE', 1), ('TOFU', 'UMAMI', 2),
    ('SEITAN', 'DOMINANCE', 2), ('SEITAN', 'UMAMI', 3),
    ('CHICKPEAS', 'DOMINANCE', 2), ('CHICKPEAS', 'SWEETNESS', 2), ('CHICKPEAS', 'UMAMI', 2),
    ('LENTILS', 'DOMINANCE', 2), ('LENTILS', 'UMAMI', 3),
    ('KIDNEY_BEANS', 'DOMINANCE', 2), ('KIDNEY_BEANS', 'UMAMI', 2),
    ('WHITE_BEANS', 'DOMINANCE', 2), ('WHITE_BEANS', 'UMAMI', 2),
    ('EDAMAME', 'DOMINANCE', 2), ('EDAMAME', 'SWEETNESS', 2), ('EDAMAME', 'UMAMI', 3),
    ('POTATO', 'DOMINANCE', 1), ('POTATO', 'SWEETNESS', 2),
    ('RICE', 'DOMINANCE', 1),
    ('RICE_NOODLES', 'DOMINANCE', 1),
    ('UDON', 'DOMINANCE', 1),
    ('SOBA', 'DOMINANCE', 3), ('SOBA', 'BITTERNESS', 2),
    ('COUSCOUS', 'DOMINANCE', 1),
    ('BULGUR', 'DOMINANCE', 2),
    ('POLENTA', 'DOMINANCE', 2), ('POLENTA', 'SWEETNESS', 2),
    ('CORN', 'DOMINANCE', 2), ('CORN', 'SWEETNESS', 4),
    ('PUMPKIN', 'DOMINANCE', 3), ('PUMPKIN', 'SWEETNESS', 3),
    ('SWEET_POTATO', 'DOMINANCE', 3), ('SWEET_POTATO', 'SWEETNESS', 4),
    ('ONION', 'DOMINANCE', 4), ('ONION', 'SWEETNESS', 3), ('ONION', 'UMAMI', 2),
    ('SPRING_ONION', 'DOMINANCE', 3), ('SPRING_ONION', 'SWEETNESS', 1),
    ('PARSLEY', 'DOMINANCE', 2), ('PARSLEY', 'BITTERNESS', 1),
    ('BASIL', 'DOMINANCE', 4), ('BASIL', 'BITTERNESS', 1),
    ('POINTED_CABBAGE', 'DOMINANCE', 2), ('POINTED_CABBAGE', 'SWEETNESS', 3),
    ('SAVOY_CABBAGE', 'DOMINANCE', 3), ('SAVOY_CABBAGE', 'SWEETNESS', 2), ('SAVOY_CABBAGE', 'BITTERNESS', 2),
    ('RED_CABBAGE', 'DOMINANCE', 3), ('RED_CABBAGE', 'SWEETNESS', 3),
    ('PAK_CHOI', 'DOMINANCE', 2), ('PAK_CHOI', 'BITTERNESS', 1),
    ('NAPA_CABBAGE', 'DOMINANCE', 2), ('NAPA_CABBAGE', 'SWEETNESS', 2),
    ('BROCCOLI', 'DOMINANCE', 3), ('BROCCOLI', 'BITTERNESS', 2),
    ('CAULIFLOWER', 'DOMINANCE', 2), ('CAULIFLOWER', 'SWEETNESS', 2),
    ('BRUSSELS_SPROUTS', 'DOMINANCE', 4), ('BRUSSELS_SPROUTS', 'BITTERNESS', 3),
    ('CARROT', 'DOMINANCE', 2), ('CARROT', 'SWEETNESS', 4),
    ('PARSNIP', 'DOMINANCE', 3), ('PARSNIP', 'SWEETNESS', 4),
    ('CELERIAC', 'DOMINANCE', 4), ('CELERIAC', 'SWEETNESS', 2),
    ('SPINACH', 'DOMINANCE', 3), ('SPINACH', 'BITTERNESS', 2), ('SPINACH', 'UMAMI', 2),
    ('LEEK', 'DOMINANCE', 3), ('LEEK', 'SWEETNESS', 2),
    ('ASPARAGUS', 'DOMINANCE', 3), ('ASPARAGUS', 'BITTERNESS', 2), ('ASPARAGUS', 'SWEETNESS', 1),
    ('EGGPLANT', 'DOMINANCE', 2), ('EGGPLANT', 'SWEETNESS', 1), ('EGGPLANT', 'UMAMI', 2),
    ('ZUCCHINI', 'DOMINANCE', 1), ('ZUCCHINI', 'SWEETNESS', 1),
    ('BELL_PEPPER', 'DOMINANCE', 3), ('BELL_PEPPER', 'SWEETNESS', 3),
    ('CUCUMBER', 'DOMINANCE', 1), ('CUCUMBER', 'SWEETNESS', 1),
    ('TOMATO', 'DOMINANCE', 3), ('TOMATO', 'ACIDITY', 3), ('TOMATO', 'SWEETNESS', 2), ('TOMATO', 'UMAMI', 3),
    ('GREEN_BEANS', 'DOMINANCE', 2), ('GREEN_BEANS', 'SWEETNESS', 2),
    ('OKRA', 'DOMINANCE', 2), ('OKRA', 'BITTERNESS', 1),
    ('OYSTER_MUSHROOM', 'DOMINANCE', 2), ('OYSTER_MUSHROOM', 'UMAMI', 4),
    ('SHIITAKE', 'DOMINANCE', 4), ('SHIITAKE', 'UMAMI', 5),
    ('BAMBOO_SHOOTS', 'DOMINANCE', 2), ('BAMBOO_SHOOTS', 'BITTERNESS', 1),
    ('ARTICHOKE', 'DOMINANCE', 4), ('ARTICHOKE', 'BITTERNESS', 3),
    ('PEACH', 'DOMINANCE', 2), ('PEACH', 'SWEETNESS', 4), ('PEACH', 'ACIDITY', 2),
    ('PLUM', 'DOMINANCE', 2), ('PLUM', 'SWEETNESS', 4), ('PLUM', 'ACIDITY', 3),
    ('APRICOT', 'DOMINANCE', 2), ('APRICOT', 'SWEETNESS', 4), ('APRICOT', 'ACIDITY', 3),
    ('STRAWBERRY', 'DOMINANCE', 3), ('STRAWBERRY', 'SWEETNESS', 4), ('STRAWBERRY', 'ACIDITY', 3),
    ('RASPBERRY', 'DOMINANCE', 3), ('RASPBERRY', 'SWEETNESS', 3), ('RASPBERRY', 'ACIDITY', 4),
    ('BLUEBERRY', 'DOMINANCE', 2), ('BLUEBERRY', 'SWEETNESS', 3), ('BLUEBERRY', 'ACIDITY', 2),
    ('GRAPE', 'DOMINANCE', 2), ('GRAPE', 'SWEETNESS', 4), ('GRAPE', 'ACIDITY', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code
ON CONFLICT (ingredient_concept_id, culinary_dimension_id)
DO UPDATE SET level = EXCLUDED.level;
