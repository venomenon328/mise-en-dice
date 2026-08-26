--liquibase formatted sql

--changeset venomenon328:025-germany-curation
-- Issue #172: country-by-country catalog curation pass (Germany / DE).
-- Adds only the explicitly approved German associations and catalog gaps.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('PRETZEL', 'Laugenbrezel', true, true, 'SPECIFIC', 0.3500, 1,
        'Herzhafte Laugenbrezel als gebackenes Einzelprodukt; nicht süße Brezel, Salzbrezel-Snack oder beliebiges Laugengebäck.'),
    ('JUNIPER_BERRIES', 'Wacholderbeeren', true, true, 'SPECIFIC', 0.4500, 2,
        'Getrocknete Wacholderbeeren als kräftiges Würzmittel insbesondere für Kohl-, Wild-, Fleisch-, Fisch- und Schmorgerichte; nicht Wacholderholz, Gin oder Wacholderextrakt.'),
    ('KASSELER', 'Kasseler', true, true, 'SPECIFIC', 0.6000, 2,
        'Gepökeltes und geräuchertes Schweinefleisch aus geeigneten Teilstücken wie Rücken, Nacken oder Rippe; nicht das fertig zubereitete Gericht.'),
    ('LIVER_SAUSAGE', 'Leberwurst', true, true, 'SPECIFIC', 0.5500, 2,
        'Leberhaltige Koch- oder Streichwurst; Tierart, Körnung und Rezeptur können variieren. Nicht mit Leberpastete gleichsetzen.'),
    ('MARZIPAN', 'Marzipan', true, true, 'SPECIFIC', 0.3000, 2,
        'Süße Mandel-Zucker-Masse einschließlich als Backzutat verwendbarer Marzipanmasse; nicht Persipan oder bloßes Mandelaroma.'),
    ('LEBERKAESE', 'Leberkäse', true, true, 'SPECIFIC', 0.4500, 2,
        'Fein zerkleinertes, gebackenes Fleisch- oder Brühwursterzeugnis nach Leberkäse- beziehungsweise Fleischkäse-Art; konkrete Fleischmischung kann variieren. Nicht gewöhnlicher Hackbraten.'),
    ('PUMPERNICKEL', 'Pumpernickel', true, true, 'SPECIFIC', 0.6000, 2,
        'Sehr dunkles, dichtes Vollkorn-Roggenbrot nach Pumpernickel-Art mit charakteristisch langer Backzeit; nicht beliebiges dunkles Roggenbrot.'),
    ('FRANKFURT_GREEN_SAUCE', 'Frankfurter Grüne Soße', true, true, 'SPECIFIC', 0.3000, 4,
        'Frankfurter Kräutersauce mit charakteristischer Sieben-Kräuter-Basis; weitere Bestandteile und genaue Rezeptur können variieren. Gemeint ist die zubereitete Sauce, nicht lediglich das Kräuterpäckchen.'),
    ('HARZER_CHEESE', 'Harzer Käse', true, true, 'SPECIFIC', 0.3500, 3,
        'Kräftig gereifter, sehr fettarmer Sauermilchkäse nach Harzer Art; nicht generisch jeder Sauermilchkäse oder Handkäse.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('BREAD', 'PRETZEL'),
    ('SPICES', 'JUNIPER_BERRIES'),
    ('PORK', 'KASSELER'),
    ('CURED_MEAT', 'KASSELER'),
    ('SAUSAGE', 'LIVER_SAUSAGE'),
    ('OFFAL', 'LIVER_SAUSAGE'),
    ('CONFECTIONERY', 'MARZIPAN'),
    ('ALMOND', 'MARZIPAN'),
    ('SAUSAGE', 'LEBERKAESE'),
    ('RYE_BREAD', 'PUMPERNICKEL'),
    ('READY_SAUCES_AND_PASTES', 'FRANKFURT_GREEN_SAUCE'),
    ('CHEESE', 'HARZER_CHEESE')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('PRETZEL', 'STARCH'),
    ('JUNIPER_BERRIES', 'AROMATIC'),
    ('JUNIPER_BERRIES', 'SEASONING'),
    ('KASSELER', 'ANIMAL_PROTEIN'),
    ('LIVER_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('LIVER_SAUSAGE', 'FAT'),
    ('LIVER_SAUSAGE', 'SEASONING'),
    ('MARZIPAN', 'AROMATIC'),
    ('MARZIPAN', 'FAT'),
    ('MARZIPAN', 'SEASONING'),
    ('LEBERKAESE', 'ANIMAL_PROTEIN'),
    ('LEBERKAESE', 'FAT'),
    ('PUMPERNICKEL', 'STARCH'),
    ('FRANKFURT_GREEN_SAUCE', 'ACID'),
    ('FRANKFURT_GREEN_SAUCE', 'AROMATIC'),
    ('FRANKFURT_GREEN_SAUCE', 'FAT'),
    ('FRANKFURT_GREEN_SAUCE', 'SEASONING'),
    ('HARZER_CHEESE', 'ANIMAL_PROTEIN'),
    ('HARZER_CHEESE', 'SEASONING')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('PRETZEL', 'DOMINANCE', 2),
    ('PRETZEL', 'SWEETNESS', 1),
    ('PRETZEL', 'SALTINESS', 3),
    ('JUNIPER_BERRIES', 'DOMINANCE', 5),
    ('JUNIPER_BERRIES', 'BITTERNESS', 3),
    ('KASSELER', 'DOMINANCE', 4),
    ('KASSELER', 'FATTINESS', 3),
    ('KASSELER', 'UMAMI', 4),
    ('KASSELER', 'SALTINESS', 4),
    ('LIVER_SAUSAGE', 'DOMINANCE', 4),
    ('LIVER_SAUSAGE', 'BITTERNESS', 2),
    ('LIVER_SAUSAGE', 'FATTINESS', 5),
    ('LIVER_SAUSAGE', 'UMAMI', 5),
    ('LIVER_SAUSAGE', 'SALTINESS', 4),
    ('MARZIPAN', 'DOMINANCE', 4),
    ('MARZIPAN', 'SWEETNESS', 5),
    ('MARZIPAN', 'FATTINESS', 4),
    ('LEBERKAESE', 'DOMINANCE', 3),
    ('LEBERKAESE', 'FATTINESS', 4),
    ('LEBERKAESE', 'UMAMI', 4),
    ('LEBERKAESE', 'SALTINESS', 3),
    ('PUMPERNICKEL', 'DOMINANCE', 3),
    ('PUMPERNICKEL', 'SWEETNESS', 2),
    ('PUMPERNICKEL', 'BITTERNESS', 2),
    ('PUMPERNICKEL', 'SALTINESS', 2),
    ('FRANKFURT_GREEN_SAUCE', 'DOMINANCE', 4),
    ('FRANKFURT_GREEN_SAUCE', 'ACIDITY', 3),
    ('FRANKFURT_GREEN_SAUCE', 'BITTERNESS', 2),
    ('FRANKFURT_GREEN_SAUCE', 'FATTINESS', 3),
    ('FRANKFURT_GREEN_SAUCE', 'UMAMI', 2),
    ('FRANKFURT_GREEN_SAUCE', 'SALTINESS', 2),
    ('HARZER_CHEESE', 'DOMINANCE', 5),
    ('HARZER_CHEESE', 'ACIDITY', 3),
    ('HARZER_CHEESE', 'FATTINESS', 1),
    ('HARZER_CHEESE', 'UMAMI', 5),
    ('HARZER_CHEESE', 'SALTINESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('JUNIPER_BERRIES', 'DRIED'),
    ('KASSELER', 'CURED'),
    ('KASSELER', 'SMOKED'),
    ('HARZER_CHEESE', 'FERMENTED'),
    ('HARZER_CHEESE', 'CURED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('PRETZEL', 'TOBIAS', 'EASY'),
    ('PRETZEL', 'GEORGIA', 'EASY'),
    ('JUNIPER_BERRIES', 'TOBIAS', 'EASY'),
    ('JUNIPER_BERRIES', 'GEORGIA', 'EASY'),
    ('KASSELER', 'TOBIAS', 'EASY'),
    ('KASSELER', 'GEORGIA', 'EASY'),
    ('LIVER_SAUSAGE', 'TOBIAS', 'EASY'),
    ('LIVER_SAUSAGE', 'GEORGIA', 'EASY'),
    ('MARZIPAN', 'TOBIAS', 'EASY'),
    ('MARZIPAN', 'GEORGIA', 'EASY'),
    ('LEBERKAESE', 'TOBIAS', 'EASY'),
    ('LEBERKAESE', 'GEORGIA', 'EASY'),
    ('PUMPERNICKEL', 'TOBIAS', 'EASY'),
    ('PUMPERNICKEL', 'GEORGIA', 'EASY'),
    ('FRANKFURT_GREEN_SAUCE', 'TOBIAS', 'DIFFICULT'),
    ('FRANKFURT_GREEN_SAUCE', 'GEORGIA', 'PLANNED'),
    ('HARZER_CHEESE', 'TOBIAS', 'EASY'),
    ('HARZER_CHEESE', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, false
FROM exclusion_rule rule
JOIN ingredient_concept concept
  ON concept.code = 'FRANKFURT_GREEN_SAUCE'
WHERE rule.code = 'NO_DAIRY';

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'DE'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'BREAD',
    'RYE_BREAD',
    'SOURDOUGH_BREAD',
    'POTATO',
    'SPAETZLE',
    'BREAD_DUMPLING',
    'POTATO_DUMPLING',
    'WHITE_CABBAGE',
    'SAUERKRAUT',
    'RED_CABBAGE',
    'KALE',
    'WHITE_ASPARAGUS',
    'PICKLED_CUCUMBER',
    'SAUSAGE',
    'BRATWURST',
    'WHITE_SAUSAGE',
    'BLOOD_SAUSAGE',
    'PORK_KNUCKLE',
    'BEEF_ROULADE',
    'GOOSE',
    'VENISON',
    'MEAT_ASPIC',
    'MATJES',
    'NORTH_SEA_SHRIMP',
    'PLAICE',
    'QUARK',
    'MUSTARD',
    'CARAWAY',
    'BEER',
    'PILSNER_LAGER',
    'WHITE_WINE',
    'CIDER',
    'EEL',
    'PORK_CUTLET',
    'PICKLED_HERRING',
    'CARP',
    'HORSERADISH',
    'SPECULOOS',
    'CHANTERELLE',
    'PORCINI',
    'PRETZEL',
    'JUNIPER_BERRIES',
    'KASSELER',
    'LIVER_SAUSAGE',
    'MARZIPAN',
    'LEBERKAESE',
    'PUMPERNICKEL',
    'FRANKFURT_GREEN_SAUCE',
    'HARZER_CHEESE'
);
