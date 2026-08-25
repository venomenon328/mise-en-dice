--liquibase formatted sql

--changeset venomenon328:019-poland-curation
-- Issue #172: country-by-country catalog curation pass (Poland / PL).
-- Adds the explicitly approved catalog gaps together with their catalog metadata
-- and persists only the explicitly approved Poland associations.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('FERMENTED_CUCUMBER', 'Salzgurke', true, true, 'SPECIFIC', 0.6000, 2,
        'Milchsauer in Salzlake fermentierte Gurke; bewusst von essigbasierter Gewürzgurke getrennt.'),
    ('TWAROG', 'Twaróg', true, true, 'SPECIFIC', 0.5500, 2,
        'Polnischer frischer Sauermilch-/Bruchkäse; Quark ist eine pragmatische Katalogannäherung. Nicht mit körnigem Hüttenkäse gleichsetzen.'),
    ('SOUR_RYE_STARTER', 'Żur-Saueransatz', true, true, 'SPECIFIC', 0.3000, 4,
        'Fermentierter Roggenmehl-Saueransatz (zakwas/żur) als flüssige Basis für żur/żurek; nicht die fertige Suppe. Gewürzzusätze können variieren.'),
    ('CARP', 'Karpfen', true, true, 'SPECIFIC', 0.3500, 3, null),
    ('DRIED_WILD_MUSHROOMS', 'getrocknete Waldpilze', true, true, 'SPECIFIC', 0.4500, 2,
        'Getrocknete aromatische Waldpilze, einzeln oder gemischt; nicht auf eine einzelne Pilzart festgelegt.'),
    ('PICKLED_MUSHROOMS', 'eingelegte Pilze', true, true, 'SPECIFIC', 0.4000, 3,
        'In Essiglake eingelegte Speisepilze; Fermentation wird durch dieses Konzept nicht vorausgesetzt.'),
    ('RYE_BREAD', 'Roggenbrot', true, true, 'SPECIFIC', 0.7000, 1, null);

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('BREAD', 'RYE_BREAD'),
    ('CUCUMBER', 'FERMENTED_CUCUMBER'),
    ('FERMENTED_SEASONINGS', 'SOUR_RYE_STARTER'),
    ('FISH', 'CARP'),
    ('MUSHROOMS', 'DRIED_WILD_MUSHROOMS'),
    ('MUSHROOMS', 'PICKLED_MUSHROOMS'),
    ('PRESERVED_PRODUCE', 'FERMENTED_CUCUMBER'),
    ('QUARK', 'TWAROG')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('FERMENTED_CUCUMBER', 'VEGETABLE'),
    ('FERMENTED_CUCUMBER', 'ACID'),
    ('FERMENTED_CUCUMBER', 'SEASONING'),
    ('TWAROG', 'ANIMAL_PROTEIN'),
    ('TWAROG', 'FAT'),
    ('TWAROG', 'ACID'),
    ('SOUR_RYE_STARTER', 'ACID'),
    ('SOUR_RYE_STARTER', 'SEASONING'),
    ('CARP', 'ANIMAL_PROTEIN'),
    ('DRIED_WILD_MUSHROOMS', 'VEGETABLE'),
    ('DRIED_WILD_MUSHROOMS', 'SEASONING'),
    ('PICKLED_MUSHROOMS', 'VEGETABLE'),
    ('PICKLED_MUSHROOMS', 'ACID'),
    ('PICKLED_MUSHROOMS', 'SEASONING'),
    ('RYE_BREAD', 'STARCH')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('FERMENTED_CUCUMBER', 'DOMINANCE', 3),
    ('FERMENTED_CUCUMBER', 'SWEETNESS', 1),
    ('FERMENTED_CUCUMBER', 'ACIDITY', 4),
    ('FERMENTED_CUCUMBER', 'UMAMI', 2),
    ('FERMENTED_CUCUMBER', 'SALTINESS', 4),
    ('TWAROG', 'DOMINANCE', 2),
    ('TWAROG', 'ACIDITY', 2),
    ('TWAROG', 'FATTINESS', 2),
    ('SOUR_RYE_STARTER', 'DOMINANCE', 4),
    ('SOUR_RYE_STARTER', 'ACIDITY', 5),
    ('CARP', 'DOMINANCE', 3),
    ('CARP', 'FATTINESS', 3),
    ('CARP', 'UMAMI', 3),
    ('DRIED_WILD_MUSHROOMS', 'DOMINANCE', 5),
    ('DRIED_WILD_MUSHROOMS', 'BITTERNESS', 2),
    ('DRIED_WILD_MUSHROOMS', 'UMAMI', 5),
    ('PICKLED_MUSHROOMS', 'DOMINANCE', 4),
    ('PICKLED_MUSHROOMS', 'SWEETNESS', 2),
    ('PICKLED_MUSHROOMS', 'ACIDITY', 4),
    ('PICKLED_MUSHROOMS', 'UMAMI', 3),
    ('PICKLED_MUSHROOMS', 'SALTINESS', 3),
    ('RYE_BREAD', 'DOMINANCE', 3),
    ('RYE_BREAD', 'SWEETNESS', 1),
    ('RYE_BREAD', 'ACIDITY', 2),
    ('RYE_BREAD', 'BITTERNESS', 2),
    ('RYE_BREAD', 'SALTINESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('FERMENTED_CUCUMBER', 'FERMENTED'),
    ('FERMENTED_CUCUMBER', 'PICKLED'),
    ('TWAROG', 'FERMENTED'),
    ('SOUR_RYE_STARTER', 'FERMENTED'),
    ('DRIED_WILD_MUSHROOMS', 'DRIED'),
    ('PICKLED_MUSHROOMS', 'PICKLED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('FERMENTED_CUCUMBER', 'TOBIAS', 'EASY'),
    ('FERMENTED_CUCUMBER', 'GEORGIA', 'PLANNED'),
    ('TWAROG', 'TOBIAS', 'PLANNED'),
    ('TWAROG', 'GEORGIA', 'PLANNED'),
    ('SOUR_RYE_STARTER', 'TOBIAS', 'DIFFICULT'),
    ('SOUR_RYE_STARTER', 'GEORGIA', 'PLANNED'),
    ('CARP', 'TOBIAS', 'DIFFICULT'),
    ('CARP', 'GEORGIA', 'PLANNED'),
    ('DRIED_WILD_MUSHROOMS', 'TOBIAS', 'EASY'),
    ('DRIED_WILD_MUSHROOMS', 'GEORGIA', 'EASY'),
    ('PICKLED_MUSHROOMS', 'TOBIAS', 'PLANNED'),
    ('PICKLED_MUSHROOMS', 'GEORGIA', 'PLANNED'),
    ('RYE_BREAD', 'TOBIAS', 'EASY'),
    ('RYE_BREAD', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'PL'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'POTATO',
    'BEETROOT',
    'WHITE_CABBAGE',
    'SAUERKRAUT',
    'HERRING',
    'PORK_CUTLET',
    'SAUSAGE',
    'LARD',
    'BUCKWHEAT',
    'BARLEY',
    'YELLOW_SPLIT_PEAS',
    'RYE_FLOUR',
    'SOUR_CREAM',
    'DILL',
    'MARJORAM',
    'PORCINI',
    'BLOOD_SAUSAGE',
    'POPPY_SEEDS',
    'HORSERADISH',
    'FERMENTED_CUCUMBER',
    'TWAROG',
    'SOUR_RYE_STARTER',
    'CARP',
    'DRIED_WILD_MUSHROOMS',
    'PICKLED_MUSHROOMS',
    'RYE_BREAD'
);
