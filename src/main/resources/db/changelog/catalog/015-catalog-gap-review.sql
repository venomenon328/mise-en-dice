--liquibase formatted sql
--changeset venomenon328:015-catalog-gap-review splitStatements:false

-- Mise en Dice - second curated catalog plausibility and gap review
-- Requires: catalog/014-catalog-consolidation.sql
-- Requires: schema/003-administration-foundation.sql
--
-- The refinement graph expresses valid challenge interpretations, not merely
-- botanical origin or a product's ingredient list. Fresh, dried and pickled
-- chilli forms are therefore separated instead of making every chilli product
-- a descendant of fruit vegetables. Existing scalar values are only sharpened
-- on untouched version-0 baseline rows; operational edits remain authoritative.

INSERT INTO ingredient_concept (
    code,
    display_name,
    active,
    random_draw_enabled,
    challenge_specificity,
    base_draw_weight,
    novelty_level,
    curator_note
)
VALUES
    ('DRIED_CHILI', 'getrocknete Chili', true, true, 'OPEN', 0.5000, 2,
        'Getrocknete ganze Chili oder gröbere Trockenform; Pulver und Flocken sind bekannte Konkretisierungen.'),
    ('PICKLED_CHILI', 'eingelegte Chili', true, true, 'SPECIFIC', 0.5000, 2,
        'Bewusst getrennt von frischer Chili und trockenen Gewürzformen.'),
    ('CHILI_POWDER', 'Chilipulver', true, true, 'OPEN', 0.5000, 1,
        'Reines oder sortentypisches Chilipulver; keine zusammengesetzte Gewürzmischung.'),
    ('PUL_BIBER', 'Pul Biber', true, true, 'SPECIFIC', 0.5000, 2, null),
    ('CAYENNE_PEPPER', 'Cayennepfeffer', true, true, 'SPECIFIC', 0.6000, 1, null),
    ('KASHMIRI_CHILI_POWDER', 'Kashmiri-Chilipulver', true, true, 'SPECIFIC', 0.3500, 4, null),
    ('SWEET_PAPRIKA_POWDER', 'Paprikapulver edelsüß', true, true, 'SPECIFIC', 0.7000, 1, null),
    ('HOT_PAPRIKA_POWDER', 'Paprikapulver rosenscharf', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('PEPPER', 'Pfeffer', true, true, 'OPEN', 0.6000, 1,
        'Echter Pfeffer und seine Reife- beziehungsweise Verarbeitungsformen; Szechuanpfeffer bleibt eigenständig.'),
    ('GREEN_PEPPER', 'grüne Pfefferkörner', true, true, 'SPECIFIC', 0.5000, 2, null),
    ('FLOWER_VEGETABLES', 'Blütengemüse', true, true, 'OPEN', 0.5000, 2, null),
    ('CHAMPIGNONS', 'Champignons', true, true, 'OPEN', 0.7000, 1, null),
    ('WHITE_CHAMPIGNON', 'weiße Champignons', true, true, 'SPECIFIC', 0.9000, 1, null),
    ('BROWN_CHAMPIGNON', 'braune Champignons', true, true, 'SPECIFIC', 0.9000, 1, null),
    ('CANNED_CHAMPIGNONS', 'Dosenchampignons', true, true, 'SPECIFIC', 0.7000, 1, null),
    ('HERB_BUTTER', 'Kräuterbutter', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('GARLIC_BUTTER', 'Knoblauchbutter', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('MAGGI_SEASONING', 'Maggi-Würze', true, true, 'SPECIFIC', 0.5500, 1,
        'Flüssige Würze; bewusst nicht als Liebstöckel-Konkretisierung modelliert.'),
    ('GARLIC_POWDER', 'Knoblauchpulver', true, true, 'SPECIFIC', 0.6500, 1,
        'Schließt für die Challenge auch granulierten getrockneten Knoblauch ein.'),
    ('ONION_POWDER', 'Zwiebelpulver', true, true, 'SPECIFIC', 0.6000, 1,
        'Schließt für die Challenge auch granulierte getrocknete Zwiebel ein.'),
    ('RED_BELL_PEPPER', 'rote Paprika', true, true, 'SPECIFIC', 0.9000, 1, null),
    ('YELLOW_BELL_PEPPER', 'gelbe Paprika', true, true, 'SPECIFIC', 0.8500, 1, null),
    ('GREEN_BELL_PEPPER', 'grüne Paprika', true, true, 'SPECIFIC', 0.8500, 1, null)
ON CONFLICT DO NOTHING;

UPDATE ingredient_concept
SET display_name = 'frische Chili',
    challenge_specificity = 'OPEN',
    base_draw_weight = 0.6500,
    curator_note = 'Frische Chili als Fruchtgemüse; trockene, pulverisierte und eingelegte Formen sind getrennte Konzepte.'
WHERE code = 'CHILI'
  AND version = 0
  AND display_name = 'Chili'
  AND challenge_specificity = 'SPECIFIC'
  AND base_draw_weight = 0.8500;

UPDATE ingredient_concept
SET challenge_specificity = 'OPEN',
    curator_note = coalesce(curator_note, 'Offene Trockenform mit sorten- und regionsspezifischen Konkretisierungen.')
WHERE code = 'CHILI_FLAKES'
  AND version = 0
  AND challenge_specificity = 'SPECIFIC';

UPDATE ingredient_concept
SET challenge_specificity = 'OPEN',
    curator_note = coalesce(curator_note, 'Offene Gewürzvorgabe mit süßer, scharfer und geräucherter Konkretisierung.')
WHERE code = 'PAPRIKA_POWDER'
  AND version = 0
  AND challenge_specificity = 'SPECIFIC';

UPDATE ingredient_concept
SET challenge_specificity = 'OPEN',
    base_draw_weight = 0.7500,
    curator_note = coalesce(curator_note, 'Offene Vorgabe für frische weiße oder braune Champignons.')
WHERE code = 'CHAMPIGNON'
  AND version = 0
  AND display_name = 'frische Champignons'
  AND challenge_specificity = 'SPECIFIC'
  AND base_draw_weight = 1.0000;

UPDATE ingredient_concept
SET display_name = 'Kokosnuss oder Kokosprodukt',
    curator_note = coalesce(
        curator_note,
        'Bewusstes Root-Konzept: Frucht, Flüssigkeit, Fett und verarbeitete Produkte besitzen keinen gemeinsamen Parent, der zugleich eine gültige Challenge-Konkretisierung wäre.'
    )
WHERE code = 'COCONUT_PRODUCTS'
  AND version = 0
  AND display_name = 'Kokosprodukt';

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (
    VALUES
        ('CHILI', 'AROMATIC'),
        ('CHILI', 'SEASONING'),
        ('CHILI', 'VEGETABLE'),
        ('JALAPENO', 'VEGETABLE'),
        ('HABANERO', 'VEGETABLE'),
        ('BIRDS_EYE_CHILI', 'VEGETABLE'),
        ('POBLANO', 'VEGETABLE'),
        ('SERRANO_CHILI', 'VEGETABLE'),
        ('DRIED_CHILI', 'AROMATIC'),
        ('DRIED_CHILI', 'SEASONING'),
        ('PICKLED_CHILI', 'AROMATIC'),
        ('PICKLED_CHILI', 'SEASONING'),
        ('PICKLED_CHILI', 'VEGETABLE'),
        ('CHILI_POWDER', 'AROMATIC'),
        ('CHILI_POWDER', 'SEASONING'),
        ('PUL_BIBER', 'AROMATIC'),
        ('PUL_BIBER', 'SEASONING'),
        ('CAYENNE_PEPPER', 'AROMATIC'),
        ('CAYENNE_PEPPER', 'SEASONING'),
        ('KASHMIRI_CHILI_POWDER', 'AROMATIC'),
        ('KASHMIRI_CHILI_POWDER', 'SEASONING'),
        ('SWEET_PAPRIKA_POWDER', 'AROMATIC'),
        ('SWEET_PAPRIKA_POWDER', 'SEASONING'),
        ('HOT_PAPRIKA_POWDER', 'AROMATIC'),
        ('HOT_PAPRIKA_POWDER', 'SEASONING'),
        ('PEPPER', 'AROMATIC'),
        ('PEPPER', 'SEASONING'),
        ('GREEN_PEPPER', 'AROMATIC'),
        ('GREEN_PEPPER', 'SEASONING'),
        ('FLOWER_VEGETABLES', 'VEGETABLE'),
        ('CHAMPIGNONS', 'VEGETABLE'),
        ('WHITE_CHAMPIGNON', 'VEGETABLE'),
        ('BROWN_CHAMPIGNON', 'VEGETABLE'),
        ('CANNED_CHAMPIGNONS', 'VEGETABLE'),
        ('HERB_BUTTER', 'FAT'),
        ('HERB_BUTTER', 'AROMATIC'),
        ('HERB_BUTTER', 'SEASONING'),
        ('GARLIC_BUTTER', 'FAT'),
        ('GARLIC_BUTTER', 'AROMATIC'),
        ('GARLIC_BUTTER', 'SEASONING'),
        ('MAGGI_SEASONING', 'AROMATIC'),
        ('MAGGI_SEASONING', 'SEASONING'),
        ('GARLIC_POWDER', 'AROMATIC'),
        ('GARLIC_POWDER', 'SEASONING'),
        ('ONION_POWDER', 'AROMATIC'),
        ('ONION_POWDER', 'SEASONING'),
        ('RED_BELL_PEPPER', 'VEGETABLE'),
        ('YELLOW_BELL_PEPPER', 'VEGETABLE'),
        ('GREEN_BELL_PEPPER', 'VEGETABLE')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code
ON CONFLICT (ingredient_concept_id, functional_role_id) DO NOTHING;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (
    VALUES
        ('DRIED_CHILI', 'TOBIAS', 'EASY'),
        ('DRIED_CHILI', 'GEORGIA', 'EASY'),
        ('PICKLED_CHILI', 'TOBIAS', 'EASY'),
        ('PICKLED_CHILI', 'GEORGIA', 'EASY'),
        ('CHILI_POWDER', 'TOBIAS', 'EASY'),
        ('CHILI_POWDER', 'GEORGIA', 'EASY'),
        ('PUL_BIBER', 'TOBIAS', 'PLANNED'),
        ('PUL_BIBER', 'GEORGIA', 'EASY'),
        ('CAYENNE_PEPPER', 'TOBIAS', 'EASY'),
        ('CAYENNE_PEPPER', 'GEORGIA', 'EASY'),
        ('KASHMIRI_CHILI_POWDER', 'TOBIAS', 'PLANNED'),
        ('KASHMIRI_CHILI_POWDER', 'GEORGIA', 'PLANNED'),
        ('SWEET_PAPRIKA_POWDER', 'TOBIAS', 'EASY'),
        ('SWEET_PAPRIKA_POWDER', 'GEORGIA', 'EASY'),
        ('HOT_PAPRIKA_POWDER', 'TOBIAS', 'EASY'),
        ('HOT_PAPRIKA_POWDER', 'GEORGIA', 'EASY'),
        ('PEPPER', 'TOBIAS', 'EASY'),
        ('PEPPER', 'GEORGIA', 'EASY'),
        ('GREEN_PEPPER', 'TOBIAS', 'EASY'),
        ('GREEN_PEPPER', 'GEORGIA', 'EASY'),
        ('FLOWER_VEGETABLES', 'TOBIAS', 'EASY'),
        ('FLOWER_VEGETABLES', 'GEORGIA', 'EASY'),
        ('CHAMPIGNONS', 'TOBIAS', 'EASY'),
        ('CHAMPIGNONS', 'GEORGIA', 'EASY'),
        ('WHITE_CHAMPIGNON', 'TOBIAS', 'EASY'),
        ('WHITE_CHAMPIGNON', 'GEORGIA', 'EASY'),
        ('BROWN_CHAMPIGNON', 'TOBIAS', 'EASY'),
        ('BROWN_CHAMPIGNON', 'GEORGIA', 'EASY'),
        ('CANNED_CHAMPIGNONS', 'TOBIAS', 'EASY'),
        ('CANNED_CHAMPIGNONS', 'GEORGIA', 'EASY'),
        ('HERB_BUTTER', 'TOBIAS', 'EASY'),
        ('HERB_BUTTER', 'GEORGIA', 'EASY'),
        ('GARLIC_BUTTER', 'TOBIAS', 'EASY'),
        ('GARLIC_BUTTER', 'GEORGIA', 'EASY'),
        ('MAGGI_SEASONING', 'TOBIAS', 'EASY'),
        ('MAGGI_SEASONING', 'GEORGIA', 'EASY'),
        ('GARLIC_POWDER', 'TOBIAS', 'EASY'),
        ('GARLIC_POWDER', 'GEORGIA', 'EASY'),
        ('ONION_POWDER', 'TOBIAS', 'EASY'),
        ('ONION_POWDER', 'GEORGIA', 'EASY'),
        ('RED_BELL_PEPPER', 'TOBIAS', 'EASY'),
        ('RED_BELL_PEPPER', 'GEORGIA', 'EASY'),
        ('YELLOW_BELL_PEPPER', 'TOBIAS', 'EASY'),
        ('YELLOW_BELL_PEPPER', 'GEORGIA', 'EASY'),
        ('GREEN_BELL_PEPPER', 'TOBIAS', 'EASY'),
        ('GREEN_BELL_PEPPER', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code
ON CONFLICT (ingredient_concept_id, participant_id) DO NOTHING;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (
    VALUES
        ('DRIED_CHILI', 'DRIED'),
        ('PICKLED_CHILI', 'PICKLED'),
        ('CHILI_POWDER', 'DRIED'),
        ('PUL_BIBER', 'DRIED'),
        ('CAYENNE_PEPPER', 'DRIED'),
        ('KASHMIRI_CHILI_POWDER', 'DRIED'),
        ('PAPRIKA_POWDER', 'DRIED'),
        ('SWEET_PAPRIKA_POWDER', 'DRIED'),
        ('HOT_PAPRIKA_POWDER', 'DRIED'),
        ('SMOKED_PAPRIKA', 'DRIED'),
        ('GARLIC_POWDER', 'DRIED'),
        ('ONION_POWDER', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code
ON CONFLICT (ingredient_concept_id, culinary_flag_id) DO NOTHING;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id,
    culinary_dimension_id,
    level
)
SELECT concept.id, dimension.id, assignment.level
FROM (
    VALUES
        ('DRIED_CHILI', 'DOMINANCE', 4),
        ('DRIED_CHILI', 'HEAT', 3),
        ('PICKLED_CHILI', 'ACIDITY', 2),
        ('PICKLED_CHILI', 'DOMINANCE', 4),
        ('PICKLED_CHILI', 'HEAT', 3),
        ('CHILI_POWDER', 'DOMINANCE', 4),
        ('CHILI_POWDER', 'HEAT', 3),
        ('PUL_BIBER', 'DOMINANCE', 4),
        ('PUL_BIBER', 'HEAT', 3),
        ('CAYENNE_PEPPER', 'DOMINANCE', 4),
        ('CAYENNE_PEPPER', 'HEAT', 4),
        ('KASHMIRI_CHILI_POWDER', 'DOMINANCE', 4),
        ('KASHMIRI_CHILI_POWDER', 'HEAT', 3),
        ('KASHMIRI_CHILI_POWDER', 'SWEETNESS', 2),
        ('SWEET_PAPRIKA_POWDER', 'DOMINANCE', 3),
        ('SWEET_PAPRIKA_POWDER', 'SWEETNESS', 2),
        ('HOT_PAPRIKA_POWDER', 'DOMINANCE', 4),
        ('HOT_PAPRIKA_POWDER', 'HEAT', 2),
        ('PEPPER', 'DOMINANCE', 4),
        ('PEPPER', 'HEAT', 2),
        ('GREEN_PEPPER', 'DOMINANCE', 4),
        ('GREEN_PEPPER', 'HEAT', 2),
        ('WHITE_CHAMPIGNON', 'UMAMI', 3),
        ('BROWN_CHAMPIGNON', 'UMAMI', 3),
        ('CANNED_CHAMPIGNONS', 'UMAMI', 3),
        ('HERB_BUTTER', 'DOMINANCE', 4),
        ('HERB_BUTTER', 'FATTINESS', 5),
        ('GARLIC_BUTTER', 'DOMINANCE', 4),
        ('GARLIC_BUTTER', 'FATTINESS', 5),
        ('GARLIC_BUTTER', 'UMAMI', 2),
        ('MAGGI_SEASONING', 'DOMINANCE', 5),
        ('MAGGI_SEASONING', 'UMAMI', 5),
        ('GARLIC_POWDER', 'DOMINANCE', 4),
        ('GARLIC_POWDER', 'UMAMI', 2),
        ('ONION_POWDER', 'DOMINANCE', 4),
        ('ONION_POWDER', 'SWEETNESS', 2),
        ('RED_BELL_PEPPER', 'SWEETNESS', 3),
        ('YELLOW_BELL_PEPPER', 'SWEETNESS', 3),
        ('GREEN_BELL_PEPPER', 'BITTERNESS', 2),
        ('GREEN_BELL_PEPPER', 'SWEETNESS', 1)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code
ON CONFLICT (ingredient_concept_id, culinary_dimension_id) DO NOTHING;

DELETE FROM ingredient_refinement relation
USING ingredient_concept parent,
      ingredient_concept child,
      (
          VALUES
              ('SPICES', 'CHILI'),
              ('CHILI', 'ANCHO_CHILI'),
              ('CHILI', 'CHILI_FLAKES'),
              ('CHILI', 'CHIPOTLE'),
              ('CHILI', 'GOCHUGARU'),
              ('SPICES', 'BLACK_PEPPER'),
              ('SPICES', 'WHITE_PEPPER'),
              ('VEGETABLES', 'ARTICHOKE'),
              ('MUSHROOMS', 'CHAMPIGNON'),
              ('CHILI_CONDIMENTS', 'LAKSA_PASTE'),
              ('CHILI_CONDIMENTS', 'MOLE_PASTE'),
              ('CHILI_CONDIMENTS', 'THAI_GREEN_CURRY_PASTE'),
              ('CHILI_CONDIMENTS', 'THAI_RED_CURRY_PASTE'),
              ('PRESERVED_FISH', 'SURIMI'),
              ('GRAINS', 'POLENTA'),
              ('BELL_PEPPER', 'ROASTED_RED_PEPPER')
      ) AS obsolete(parent_code, child_code)
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = obsolete.parent_code
  AND child.code = obsolete.child_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (
    VALUES
        ('FRUIT_VEGETABLES', 'CHILI'),
        ('SPICES', 'DRIED_CHILI'),
        ('DRIED_CHILI', 'ANCHO_CHILI'),
        ('DRIED_CHILI', 'CHIPOTLE'),
        ('DRIED_CHILI', 'CHILI_FLAKES'),
        ('DRIED_CHILI', 'CHILI_POWDER'),
        ('CHILI_FLAKES', 'GOCHUGARU'),
        ('CHILI_FLAKES', 'PUL_BIBER'),
        ('CHILI_POWDER', 'CAYENNE_PEPPER'),
        ('CHILI_POWDER', 'KASHMIRI_CHILI_POWDER'),
        ('PRESERVED_PRODUCE', 'PICKLED_CHILI'),
        ('PAPRIKA_POWDER', 'SWEET_PAPRIKA_POWDER'),
        ('PAPRIKA_POWDER', 'HOT_PAPRIKA_POWDER'),
        ('SPICES', 'PEPPER'),
        ('PEPPER', 'BLACK_PEPPER'),
        ('PEPPER', 'WHITE_PEPPER'),
        ('PEPPER', 'GREEN_PEPPER'),
        ('VEGETABLES', 'FLOWER_VEGETABLES'),
        ('FLOWER_VEGETABLES', 'ARTICHOKE'),
        ('FLOWER_VEGETABLES', 'BROCCOLI'),
        ('FLOWER_VEGETABLES', 'CAULIFLOWER'),
        ('FLOWER_VEGETABLES', 'ROMANESCO'),
        ('MUSHROOMS', 'CHAMPIGNONS'),
        ('CHAMPIGNONS', 'CHAMPIGNON'),
        ('CHAMPIGNONS', 'CANNED_CHAMPIGNONS'),
        ('CHAMPIGNON', 'WHITE_CHAMPIGNON'),
        ('CHAMPIGNON', 'BROWN_CHAMPIGNON'),
        ('PRESERVED_PRODUCE', 'CANNED_CHAMPIGNONS'),
        ('BUTTER', 'HERB_BUTTER'),
        ('BUTTER', 'GARLIC_BUTTER'),
        ('SAUCES_AND_PASTES', 'MAGGI_SEASONING'),
        ('SPICES', 'GARLIC_POWDER'),
        ('SPICES', 'ONION_POWDER'),
        ('SAUCES_AND_PASTES', 'MOLE_PASTE'),
        ('BELL_PEPPER', 'RED_BELL_PEPPER'),
        ('BELL_PEPPER', 'YELLOW_BELL_PEPPER'),
        ('BELL_PEPPER', 'GREEN_BELL_PEPPER'),
        ('FISH', 'SURIMI'),
        ('CORN', 'POLENTA'),
        ('RED_BELL_PEPPER', 'ROASTED_RED_PEPPER')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

INSERT INTO exclusion_rule_target (
    exclusion_rule_id,
    ingredient_concept_id,
    include_refinements
)
SELECT rule.id, concept.id, assignment.include_refinements
FROM (
    VALUES
        ('NO_CHILI', 'DRIED_CHILI', true),
        ('NO_CHILI', 'PICKLED_CHILI', true),
        ('NO_CHILI', 'READY_CURRY_PASTE', true),
        ('NO_CHILI', 'MOLE_PASTE', false),
        ('NO_ALLIUMS', 'GARLIC_BUTTER', false),
        ('NO_ALLIUMS', 'GARLIC_POWDER', false),
        ('NO_ALLIUMS', 'ONION_POWDER', false)
) AS assignment(rule_code, concept_code, include_refinements)
JOIN exclusion_rule rule
  ON rule.code = assignment.rule_code
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
ON CONFLICT (exclusion_rule_id, ingredient_concept_id) DO NOTHING;
