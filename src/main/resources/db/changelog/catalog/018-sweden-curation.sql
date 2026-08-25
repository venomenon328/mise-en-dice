--liquibase formatted sql

--changeset venomenon328:018-sweden-curation
-- Issue #172: first country-by-country catalog curation pass (Sweden / SE).
-- Adds the explicitly approved catalog gaps together with their catalog metadata
-- and persists only the explicitly approved Sweden associations.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('NEW_POTATOES', 'Frühkartoffeln', true, true, 'SPECIFIC', 0.7500, 1, null),
    ('PICKLED_HERRING', 'eingelegter Hering', true, true, 'SPECIFIC', 0.5500, 2, null),
    ('GRAVLAX', 'Gravlax', true, true, 'SPECIFIC', 0.5500, 2, null),
    ('CRISPBREAD', 'Knäckebrot', true, true, 'SPECIFIC', 0.6500, 1, null),
    ('LINGONBERRY', 'Preiselbeeren', true, true, 'SPECIFIC', 0.4500, 3, null),
    ('LINGONBERRY_PRESERVES', 'Preiselbeer-Konfitüre/-kompott', true, true, 'SPECIFIC', 0.5000, 2,
        'Süße Preiselbeerzubereitung wie Konfitüre oder Kompott.'),
    ('YELLOW_SPLIT_PEAS', 'gelbe Schälerbsen', true, true, 'SPECIFIC', 0.6500, 1, null);

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('POTATO', 'NEW_POTATOES'),
    ('HERRING', 'PICKLED_HERRING'),
    ('PRESERVED_FISH', 'PICKLED_HERRING'),
    ('SALMON', 'GRAVLAX'),
    ('PRESERVED_FISH', 'GRAVLAX'),
    ('BREAD', 'CRISPBREAD'),
    ('BERRIES', 'LINGONBERRY'),
    ('LINGONBERRY', 'LINGONBERRY_PRESERVES'),
    ('PRESERVED_PRODUCE', 'LINGONBERRY_PRESERVES'),
    ('SPLIT_PEAS', 'YELLOW_SPLIT_PEAS')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('NEW_POTATOES', 'STARCH'),
    ('NEW_POTATOES', 'VEGETABLE'),
    ('PICKLED_HERRING', 'ANIMAL_PROTEIN'),
    ('PICKLED_HERRING', 'FAT'),
    ('PICKLED_HERRING', 'SEASONING'),
    ('GRAVLAX', 'ANIMAL_PROTEIN'),
    ('GRAVLAX', 'FAT'),
    ('GRAVLAX', 'SEASONING'),
    ('CRISPBREAD', 'STARCH'),
    ('LINGONBERRY', 'ACID'),
    ('LINGONBERRY', 'FRUIT'),
    ('LINGONBERRY_PRESERVES', 'ACID'),
    ('LINGONBERRY_PRESERVES', 'FRUIT'),
    ('LINGONBERRY_PRESERVES', 'SEASONING'),
    ('YELLOW_SPLIT_PEAS', 'PLANT_PROTEIN'),
    ('YELLOW_SPLIT_PEAS', 'STARCH')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('NEW_POTATOES', 'DOMINANCE', 1),
    ('NEW_POTATOES', 'SWEETNESS', 2),
    ('PICKLED_HERRING', 'DOMINANCE', 4),
    ('PICKLED_HERRING', 'SWEETNESS', 2),
    ('PICKLED_HERRING', 'ACIDITY', 4),
    ('PICKLED_HERRING', 'FATTINESS', 4),
    ('PICKLED_HERRING', 'UMAMI', 4),
    ('PICKLED_HERRING', 'SALTINESS', 4),
    ('GRAVLAX', 'DOMINANCE', 4),
    ('GRAVLAX', 'SWEETNESS', 2),
    ('GRAVLAX', 'FATTINESS', 4),
    ('GRAVLAX', 'UMAMI', 4),
    ('GRAVLAX', 'SALTINESS', 3),
    ('CRISPBREAD', 'DOMINANCE', 2),
    ('CRISPBREAD', 'SWEETNESS', 1),
    ('CRISPBREAD', 'SALTINESS', 2),
    ('LINGONBERRY', 'DOMINANCE', 3),
    ('LINGONBERRY', 'SWEETNESS', 2),
    ('LINGONBERRY', 'ACIDITY', 4),
    ('LINGONBERRY', 'BITTERNESS', 2),
    ('LINGONBERRY_PRESERVES', 'DOMINANCE', 4),
    ('LINGONBERRY_PRESERVES', 'SWEETNESS', 5),
    ('LINGONBERRY_PRESERVES', 'ACIDITY', 3),
    ('YELLOW_SPLIT_PEAS', 'DOMINANCE', 2),
    ('YELLOW_SPLIT_PEAS', 'SWEETNESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('PICKLED_HERRING', 'PICKLED'),
    ('GRAVLAX', 'CURED'),
    ('YELLOW_SPLIT_PEAS', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('NEW_POTATOES', 'TOBIAS', 'EASY'),
    ('NEW_POTATOES', 'GEORGIA', 'EASY'),
    ('PICKLED_HERRING', 'TOBIAS', 'EASY'),
    ('PICKLED_HERRING', 'GEORGIA', 'EASY'),
    ('GRAVLAX', 'TOBIAS', 'EASY'),
    ('GRAVLAX', 'GEORGIA', 'EASY'),
    ('CRISPBREAD', 'TOBIAS', 'EASY'),
    ('CRISPBREAD', 'GEORGIA', 'EASY'),
    ('LINGONBERRY', 'TOBIAS', 'PLANNED'),
    ('LINGONBERRY', 'GEORGIA', 'PLANNED'),
    ('LINGONBERRY_PRESERVES', 'TOBIAS', 'EASY'),
    ('LINGONBERRY_PRESERVES', 'GEORGIA', 'EASY'),
    ('YELLOW_SPLIT_PEAS', 'TOBIAS', 'EASY'),
    ('YELLOW_SPLIT_PEAS', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_seasonality (ingredient_concept_id, month, weight_multiplier)
SELECT concept.id, seasonality.month, seasonality.weight_multiplier
FROM (VALUES
    (1, 0.3000),
    (2, 0.3000),
    (3, 0.4000),
    (4, 0.7000),
    (5, 1.3000),
    (6, 1.8000),
    (7, 1.8000),
    (8, 1.3000),
    (9, 0.8000),
    (10, 0.5000),
    (11, 0.3500),
    (12, 0.3000)
) AS seasonality(month, weight_multiplier)
JOIN ingredient_concept concept
  ON concept.code = 'NEW_POTATOES';

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'SE'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'HERRING',
    'CRAYFISH',
    'DILL',
    'CINNAMON',
    'CARDAMOM',
    'SALMON',
    'NEW_POTATOES',
    'PICKLED_HERRING',
    'GRAVLAX',
    'CRISPBREAD',
    'LINGONBERRY',
    'LINGONBERRY_PRESERVES',
    'YELLOW_SPLIT_PEAS'
);
