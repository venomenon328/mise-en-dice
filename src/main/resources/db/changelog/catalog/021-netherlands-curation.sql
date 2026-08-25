--liquibase formatted sql

--changeset venomenon328:021-netherlands-curation
-- Issue #172: country-by-country catalog curation pass (Netherlands / NL).
-- Adds only the explicitly approved catalog gaps and metadata and persists only
-- the explicitly approved Netherlands associations.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('EDAM', 'Edamer', true, true, 'SPECIFIC', 0.7000, 1,
        'Halbfester Kuhmilchkäse nach Edamer Art; umfasst Edam/Edamer als Produkttyp und ist nicht auf die geschützte Herkunftsbezeichnung Edam Holland beschränkt.'),
    ('ROOKWORST', 'Rookworst', true, true, 'SPECIFIC', 0.5000, 2,
        'Niederländische geräucherte Koch-/Brühwurst. Die konkrete Fleischzusammensetzung kann variieren; deshalb bewusst nicht als Konkretisierung von Schweinefleisch modelliert.'),
    ('GREEN_SPLIT_PEAS', 'grüne Schälerbsen', true, true, 'SPECIFIC', 0.6500, 1,
        'Getrocknete grüne Schälerbsen; bewusst als Gegenstück zu gelben Schälerbsen separat modelliert.'),
    ('CHOCOLATE_HAGELSLAG', 'Schoko-Hagelslag', true, true, 'SPECIFIC', 0.4000, 3,
        'Niederländische Schokoladenstreusel insbesondere als Brotbelag; nicht beliebige Backdekoration und nicht Frucht-, Anis- oder andere Hagelslag-Varianten.'),
    ('SPECULAAS_SPICE', 'Spekulatiusgewürz', true, true, 'SPECIFIC', 0.4000, 2,
        'Gewürzmischung für Speculaas/Spekulatius, typischerweise auf Basis von Zimt mit weiteren warmen Gewürzen wie Nelke und Muskat; Zusammensetzung kann variieren und Zucker wird nicht vorausgesetzt.'),
    ('JENEVER', 'Jenever', true, true, 'SPECIFIC', 0.2500, 3,
        'Jenever/Genever: wacholderaromatisierte Spirituose mit Getreidedestillat beziehungsweise Moutwijn-Anteil; nicht mit Gin gleichsetzen.'),
    ('PEANUT_SATAY_SAUCE', 'Erdnuss-Satay-Sauce', true, true, 'SPECIFIC', 0.5000, 2,
        'Fertige erdnussbasierte Satay-/Pindasaus; typischerweise süß-salzig und kräftig nussig, konkrete Würzung und Schärfe können variieren. Nicht mit reinem Erdnussmus oder Erdnussbutter gleichsetzen.'),
    ('MATJES', 'Matjes', true, true, 'SPECIFIC', 0.5000, 2,
        'Mild gesalzener und enzymatisch gereifter Hering beziehungsweise Matjes. Umfasst klassische norddeutsche und niederländische Matjesformen; nicht automatisch essigsauer und nicht auf Hollandse Nieuwe beschränkt.'),
    ('STROOPWAFEL', 'Stroopwafel', true, true, 'SPECIFIC', 0.4500, 2,
        'Zwei dünne Waffeln mit süßer Sirup-/Karamellfüllung; als fertige Süßware und eigenständige Challengezutat modelliert, nicht als allgemeine Waffel oder als Sirup.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('CHEESE', 'EDAM'),
    ('SAUSAGE', 'ROOKWORST'),
    ('SPLIT_PEAS', 'GREEN_SPLIT_PEAS'),
    ('CHOCOLATE', 'CHOCOLATE_HAGELSLAG'),
    ('SPICE_BLENDS', 'SPECULAAS_SPICE'),
    ('COOKING_ALCOHOL', 'JENEVER'),
    ('READY_SAUCES_AND_PASTES', 'PEANUT_SATAY_SAUCE'),
    ('HERRING', 'MATJES'),
    ('PRESERVED_FISH', 'MATJES'),
    ('CONFECTIONERY', 'STROOPWAFEL')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('EDAM', 'ANIMAL_PROTEIN'),
    ('EDAM', 'FAT'),
    ('EDAM', 'SEASONING'),
    ('ROOKWORST', 'ANIMAL_PROTEIN'),
    ('ROOKWORST', 'FAT'),
    ('GREEN_SPLIT_PEAS', 'PLANT_PROTEIN'),
    ('GREEN_SPLIT_PEAS', 'STARCH'),
    ('CHOCOLATE_HAGELSLAG', 'AROMATIC'),
    ('CHOCOLATE_HAGELSLAG', 'FAT'),
    ('CHOCOLATE_HAGELSLAG', 'SEASONING'),
    ('SPECULAAS_SPICE', 'AROMATIC'),
    ('SPECULAAS_SPICE', 'SEASONING'),
    ('JENEVER', 'ACID'),
    ('JENEVER', 'SEASONING'),
    ('PEANUT_SATAY_SAUCE', 'FAT'),
    ('PEANUT_SATAY_SAUCE', 'SEASONING'),
    ('MATJES', 'ANIMAL_PROTEIN'),
    ('MATJES', 'FAT'),
    ('MATJES', 'SEASONING'),
    ('STROOPWAFEL', 'STARCH'),
    ('STROOPWAFEL', 'FAT'),
    ('STROOPWAFEL', 'SEASONING')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('EDAM', 'DOMINANCE', 3),
    ('EDAM', 'ACIDITY', 2),
    ('EDAM', 'FATTINESS', 4),
    ('EDAM', 'UMAMI', 3),
    ('EDAM', 'SALTINESS', 3),
    ('ROOKWORST', 'DOMINANCE', 4),
    ('ROOKWORST', 'FATTINESS', 5),
    ('ROOKWORST', 'UMAMI', 4),
    ('ROOKWORST', 'SALTINESS', 4),
    ('GREEN_SPLIT_PEAS', 'DOMINANCE', 2),
    ('GREEN_SPLIT_PEAS', 'SWEETNESS', 2),
    ('CHOCOLATE_HAGELSLAG', 'DOMINANCE', 4),
    ('CHOCOLATE_HAGELSLAG', 'SWEETNESS', 5),
    ('CHOCOLATE_HAGELSLAG', 'BITTERNESS', 2),
    ('CHOCOLATE_HAGELSLAG', 'FATTINESS', 3),
    ('SPECULAAS_SPICE', 'DOMINANCE', 5),
    ('SPECULAAS_SPICE', 'BITTERNESS', 2),
    ('JENEVER', 'DOMINANCE', 4),
    ('JENEVER', 'SWEETNESS', 1),
    ('JENEVER', 'ACIDITY', 2),
    ('JENEVER', 'BITTERNESS', 2),
    ('PEANUT_SATAY_SAUCE', 'DOMINANCE', 5),
    ('PEANUT_SATAY_SAUCE', 'SWEETNESS', 3),
    ('PEANUT_SATAY_SAUCE', 'ACIDITY', 2),
    ('PEANUT_SATAY_SAUCE', 'FATTINESS', 5),
    ('PEANUT_SATAY_SAUCE', 'UMAMI', 4),
    ('PEANUT_SATAY_SAUCE', 'SALTINESS', 4),
    ('MATJES', 'DOMINANCE', 4),
    ('MATJES', 'SWEETNESS', 1),
    ('MATJES', 'ACIDITY', 1),
    ('MATJES', 'FATTINESS', 4),
    ('MATJES', 'UMAMI', 4),
    ('MATJES', 'SALTINESS', 3),
    ('STROOPWAFEL', 'DOMINANCE', 4),
    ('STROOPWAFEL', 'SWEETNESS', 5),
    ('STROOPWAFEL', 'FATTINESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('EDAM', 'FERMENTED'),
    ('EDAM', 'CURED'),
    ('ROOKWORST', 'SMOKED'),
    ('GREEN_SPLIT_PEAS', 'DRIED'),
    ('MATJES', 'CURED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('EDAM', 'TOBIAS', 'EASY'),
    ('EDAM', 'GEORGIA', 'EASY'),
    ('ROOKWORST', 'TOBIAS', 'DIFFICULT'),
    ('ROOKWORST', 'GEORGIA', 'PLANNED'),
    ('GREEN_SPLIT_PEAS', 'TOBIAS', 'EASY'),
    ('GREEN_SPLIT_PEAS', 'GEORGIA', 'EASY'),
    ('CHOCOLATE_HAGELSLAG', 'TOBIAS', 'DIFFICULT'),
    ('CHOCOLATE_HAGELSLAG', 'GEORGIA', 'PLANNED'),
    ('SPECULAAS_SPICE', 'TOBIAS', 'PLANNED'),
    ('SPECULAAS_SPICE', 'GEORGIA', 'PLANNED'),
    ('JENEVER', 'TOBIAS', 'DIFFICULT'),
    ('JENEVER', 'GEORGIA', 'PLANNED'),
    ('PEANUT_SATAY_SAUCE', 'TOBIAS', 'EASY'),
    ('PEANUT_SATAY_SAUCE', 'GEORGIA', 'EASY'),
    ('MATJES', 'TOBIAS', 'EASY'),
    ('MATJES', 'GEORGIA', 'EASY'),
    ('STROOPWAFEL', 'TOBIAS', 'PLANNED'),
    ('STROOPWAFEL', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'NL'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'POTATO',
    'KALE',
    'ENDIVE',
    'SAUERKRAUT',
    'HERRING',
    'GOUDA',
    'LIQUORICE',
    'SALTY_LIQUORICE',
    'EDAM',
    'ROOKWORST',
    'GREEN_SPLIT_PEAS',
    'CHOCOLATE_HAGELSLAG',
    'SPECULAAS_SPICE',
    'JENEVER',
    'PEANUT_SATAY_SAUCE',
    'MATJES',
    'STROOPWAFEL'
);
