--liquibase formatted sql

--changeset venomenon328:020-denmark-curation
-- Issue #172: country-by-country catalog curation pass (Denmark / DK).
-- Adds the explicitly approved catalog gaps and metadata, introduces the approved
-- confectionery/chocolate hierarchy, and persists only the approved Denmark associations.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('REMOULADE', 'Remoulade', true, true, 'SPECIFIC', 0.5500, 1,
        'Würzige mayonnaiseartige Sauce mit typischerweise Gurken/Pickles, Senf, Kräutern und weiteren Würzzutaten; konkrete Rezepturen können variieren.'),
    ('FRIED_ONIONS', 'Röstzwiebeln', true, true, 'SPECIFIC', 0.6500, 1,
        'Knusprig frittierte Röstzwiebeln als fertige Zutat beziehungsweise Topping; nicht lediglich frisch angebratene Zwiebel.'),
    ('LIVER_PATE', 'Leberpastete', true, true, 'SPECIFIC', 0.3500, 3,
        'Streichfähige Pastete auf Leberbasis; umfasst typische Schweine-, Geflügel- und vergleichbare Leberpasteten, nicht reine Leber und nicht automatisch Leberwurst.'),
    ('DANABLU', 'Danablu', true, true, 'SPECIFIC', 0.4000, 3,
        'Dänischer, mit Blauschimmel gereifter Kuhmilchkäse mit geschützter geografischer Angabe; kräftig, pikant und deutlich salzig.'),
    ('DANBO', 'Danbo', true, true, 'SPECIFIC', 0.4500, 3,
        'Dänischer halbfester Kuhmilchkäse mit geschützter geografischer Angabe; je nach Reifegrad mild bis kräftiger, typischerweise leicht säuerlich. Nicht als Gouda-Variante modellieren.'),
    ('AQUAVIT', 'Aquavit', true, true, 'SPECIFIC', 0.2500, 3,
        'Mit Kümmel, Dill oder anderen Gewürzen aromatisierte skandinavische Spirituose; als würzende Spirituose beziehungsweise Kochalkohol modelliert, nicht auf eine Marke oder ein einzelnes nationales Rezept beschränkt.'),
    ('CONFECTIONERY', 'Süßware', true, false, 'OPEN', 1.0000, 1,
        'Nicht ziehbarer Strukturknoten für verzehrfertige Süßwaren, die als eigenständige Koch- oder Challengezutat verwendet werden können. Süßungsmittel und nicht verzehrfertige Grundprodukte gehören nicht automatisch hierher.'),
    ('CHOCOLATE', 'Schokolade', true, true, 'OPEN', 0.5000, 1,
        'Offene Schokoladenfamilie für dunkle, Milch- und weiße Schokolade. Kakaopulver bleibt als eigenständige Konkretisierung von Kakao oder Schokolade außerhalb dieser engeren Familie.'),
    ('LIQUORICE', 'Lakritz', true, true, 'OPEN', 0.3000, 3,
        'Lakritzware als eigenständige Koch- oder Challengezutat; umfasst süße und salzige Ausprägungen. Nicht mit Süßholzwurzel oder reinem Lakritzextrakt gleichsetzen.'),
    ('SALTY_LIQUORICE', 'Salzlakritz', true, true, 'SPECIFIC', 0.2000, 4,
        'Salzige Lakritzware mit Süßholz und typischerweise Salmiak (Ammoniumchlorid); bewusst von allgemeiner beziehungsweise rein süßer Lakritzware abgegrenzt.'),
    ('ROD_POLSE', 'Rød pølse', true, true, 'SPECIFIC', 0.4500, 3,
        'Dänische rote Hotdog-/Brühwurst (rød pølse), typischerweise aus Schweinefleisch, gekocht und geräuchert; nicht generisch jede rot gefärbte Wurst.');

-- The new CHOCOLATE node replaces the former direct cocoa-product edges so the
-- graph remains transitively reduced while also gaining a confectionery view.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'COCOA_PRODUCTS'
  AND child.code IN ('DARK_CHOCOLATE', 'MILK_CHOCOLATE', 'WHITE_CHOCOLATE');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('READY_SAUCES_AND_PASTES', 'REMOULADE'),
    ('ONION', 'FRIED_ONIONS'),
    ('OFFAL', 'LIVER_PATE'),
    ('BLUE_CHEESE', 'DANABLU'),
    ('CHEESE', 'DANBO'),
    ('COOKING_ALCOHOL', 'AQUAVIT'),
    ('CONFECTIONERY', 'CHOCOLATE'),
    ('CONFECTIONERY', 'LIQUORICE'),
    ('COCOA_PRODUCTS', 'CHOCOLATE'),
    ('CHOCOLATE', 'DARK_CHOCOLATE'),
    ('CHOCOLATE', 'MILK_CHOCOLATE'),
    ('CHOCOLATE', 'WHITE_CHOCOLATE'),
    ('LIQUORICE', 'SALTY_LIQUORICE'),
    ('SAUSAGE', 'ROD_POLSE'),
    ('PORK', 'ROD_POLSE')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('REMOULADE', 'ACID'),
    ('REMOULADE', 'FAT'),
    ('REMOULADE', 'SEASONING'),
    ('FRIED_ONIONS', 'AROMATIC'),
    ('FRIED_ONIONS', 'SEASONING'),
    ('LIVER_PATE', 'ANIMAL_PROTEIN'),
    ('LIVER_PATE', 'FAT'),
    ('LIVER_PATE', 'SEASONING'),
    ('DANABLU', 'FAT'),
    ('DANABLU', 'SEASONING'),
    ('DANBO', 'ANIMAL_PROTEIN'),
    ('DANBO', 'FAT'),
    ('DANBO', 'SEASONING'),
    ('AQUAVIT', 'ACID'),
    ('AQUAVIT', 'SEASONING'),
    ('CONFECTIONERY', 'SEASONING'),
    ('CHOCOLATE', 'AROMATIC'),
    ('CHOCOLATE', 'FAT'),
    ('CHOCOLATE', 'SEASONING'),
    ('LIQUORICE', 'AROMATIC'),
    ('LIQUORICE', 'SEASONING'),
    ('SALTY_LIQUORICE', 'AROMATIC'),
    ('SALTY_LIQUORICE', 'SEASONING'),
    ('ROD_POLSE', 'ANIMAL_PROTEIN'),
    ('ROD_POLSE', 'FAT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('REMOULADE', 'DOMINANCE', 4),
    ('REMOULADE', 'SWEETNESS', 2),
    ('REMOULADE', 'ACIDITY', 3),
    ('REMOULADE', 'FATTINESS', 5),
    ('REMOULADE', 'UMAMI', 2),
    ('REMOULADE', 'SALTINESS', 3),
    ('FRIED_ONIONS', 'DOMINANCE', 4),
    ('FRIED_ONIONS', 'SWEETNESS', 3),
    ('FRIED_ONIONS', 'FATTINESS', 4),
    ('FRIED_ONIONS', 'UMAMI', 3),
    ('FRIED_ONIONS', 'SALTINESS', 2),
    ('LIVER_PATE', 'DOMINANCE', 4),
    ('LIVER_PATE', 'BITTERNESS', 2),
    ('LIVER_PATE', 'FATTINESS', 5),
    ('LIVER_PATE', 'UMAMI', 5),
    ('LIVER_PATE', 'SALTINESS', 3),
    ('DANABLU', 'DOMINANCE', 5),
    ('DANABLU', 'ACIDITY', 2),
    ('DANABLU', 'FATTINESS', 4),
    ('DANABLU', 'UMAMI', 5),
    ('DANABLU', 'SALTINESS', 4),
    ('DANBO', 'DOMINANCE', 3),
    ('DANBO', 'ACIDITY', 2),
    ('DANBO', 'FATTINESS', 4),
    ('DANBO', 'UMAMI', 3),
    ('DANBO', 'SALTINESS', 3),
    ('AQUAVIT', 'DOMINANCE', 4),
    ('AQUAVIT', 'SWEETNESS', 1),
    ('AQUAVIT', 'ACIDITY', 2),
    ('AQUAVIT', 'BITTERNESS', 2),
    ('CHOCOLATE', 'DOMINANCE', 4),
    ('CHOCOLATE', 'SWEETNESS', 4),
    ('CHOCOLATE', 'FATTINESS', 4),
    ('LIQUORICE', 'DOMINANCE', 5),
    ('LIQUORICE', 'SWEETNESS', 4),
    ('LIQUORICE', 'BITTERNESS', 3),
    ('SALTY_LIQUORICE', 'DOMINANCE', 5),
    ('SALTY_LIQUORICE', 'SWEETNESS', 3),
    ('SALTY_LIQUORICE', 'BITTERNESS', 3),
    ('SALTY_LIQUORICE', 'SALTINESS', 5),
    ('ROD_POLSE', 'DOMINANCE', 3),
    ('ROD_POLSE', 'FATTINESS', 4),
    ('ROD_POLSE', 'UMAMI', 4),
    ('ROD_POLSE', 'SALTINESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('DANABLU', 'FERMENTED'),
    ('DANABLU', 'CURED'),
    ('DANBO', 'FERMENTED'),
    ('DANBO', 'CURED'),
    ('ROD_POLSE', 'SMOKED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('REMOULADE', 'TOBIAS', 'EASY'),
    ('REMOULADE', 'GEORGIA', 'EASY'),
    ('FRIED_ONIONS', 'TOBIAS', 'EASY'),
    ('FRIED_ONIONS', 'GEORGIA', 'EASY'),
    ('LIVER_PATE', 'TOBIAS', 'EASY'),
    ('LIVER_PATE', 'GEORGIA', 'EASY'),
    ('DANABLU', 'TOBIAS', 'PLANNED'),
    ('DANABLU', 'GEORGIA', 'PLANNED'),
    ('DANBO', 'TOBIAS', 'PLANNED'),
    ('DANBO', 'GEORGIA', 'DIFFICULT'),
    ('AQUAVIT', 'TOBIAS', 'EASY'),
    ('AQUAVIT', 'GEORGIA', 'EASY'),
    ('CHOCOLATE', 'TOBIAS', 'EASY'),
    ('CHOCOLATE', 'GEORGIA', 'EASY'),
    ('LIQUORICE', 'TOBIAS', 'EASY'),
    ('LIQUORICE', 'GEORGIA', 'EASY'),
    ('SALTY_LIQUORICE', 'TOBIAS', 'EASY'),
    ('SALTY_LIQUORICE', 'GEORGIA', 'EASY'),
    ('ROD_POLSE', 'TOBIAS', 'PLANNED'),
    ('ROD_POLSE', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, false
FROM exclusion_rule rule
JOIN ingredient_concept concept
  ON concept.code = 'REMOULADE'
WHERE rule.code = 'NO_EGGS';

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'DK'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'RYE_BREAD',
    'RYE_FLOUR',
    'HERRING',
    'PICKLED_HERRING',
    'PORK_BELLY',
    'PORK_MINCE',
    'POTATO',
    'NEW_POTATOES',
    'RED_CABBAGE',
    'PLAICE',
    'SHRIMP',
    'YELLOW_SPLIT_PEAS',
    'DILL',
    'PICKLED_CUCUMBER',
    'EEL',
    'BUTTERMILK',
    'REMOULADE',
    'FRIED_ONIONS',
    'LIVER_PATE',
    'DANABLU',
    'DANBO',
    'AQUAVIT',
    'SALTY_LIQUORICE',
    'ROD_POLSE'
);
