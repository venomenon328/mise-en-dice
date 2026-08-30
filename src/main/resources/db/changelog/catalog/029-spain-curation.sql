--liquibase formatted sql

--changeset venomenon328:029-spain-curation
-- Issue #172: country-by-country catalog curation pass (Spain / ES).
-- Adds only the explicitly approved Spanish associations and catalog gaps.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('AIOLI', 'Aioli', true, true, 'SPECIFIC', 0.5500, 1,
        'Knoblauch-Öl-Emulsion nach Aioli-/Allioli-Art; traditionell ohne Ei möglich, moderne Varianten können Ei enthalten. Knoblauch und Öl sind definierend; nicht beliebige Mayonnaise mit Knoblaucharoma.'),
    ('MANCHEGO', 'Manchego', true, true, 'SPECIFIC', 0.5000, 2,
        'Spanischer Käse aus Manchega-Schafmilch aus La Mancha; nicht generisch jeder Schafskäse.'),
    ('IBERICO_HAM', 'Jamón ibérico', true, true, 'SPECIFIC', 0.3000, 3,
        'Spanischer luftgetrockneter Schinken vom iberischen Schwein; Qualitäts- und Fütterungsklassen können variieren. Nicht automatisch Jamón ibérico de bellota und nicht mit Jamón serrano gleichsetzen.'),
    ('NORA_PEPPER', 'Ñora-Paprika', true, true, 'SPECIFIC', 0.3000, 4,
        'Kleine runde, reif rot geerntete und getrocknete spanische Paprika (ñora), meist eingeweicht beziehungsweise ausgeschabt als Würzzutat verwendet; nicht Paprikapulver und nicht generische getrocknete Chili.'),
    ('PIQUILLO_PEPPER', 'Piquillo-Paprika', true, true, 'SPECIFIC', 0.4500, 3,
        'Geröstete und geschälte rote Piquillo-Paprika als küchenfertiges Produkt, typischerweise im Glas oder in der Dose; nicht rohe rote Paprika.'),
    ('PADRON_PEPPER', 'Padrón-Paprika', true, true, 'SPECIFIC', 0.5000, 2,
        'Kleine grüne Paprika der Padrón-/Pemento-de-Herbón-Tradition; überwiegend mild, einzelne Früchte deutlich scharf. Gemeint ist die frische ganze Paprika, nicht ein fertiges Tapasgericht.'),
    ('BOMBA_RICE', 'Bomba-Reis', true, true, 'SPECIFIC', 0.5000, 2,
        'Spanische Rundkorn-Reissorte Bomba mit hoher Flüssigkeits- und Geschmacksaufnahme; nicht Synonym für beliebigen Paella-Reis und nicht synonym mit der Herkunftsbezeichnung Calasparra.'),
    ('MEMBRILLO', 'Quittenpaste', true, true, 'SPECIFIC', 0.3500, 3,
        'Feste bis schnittfähige süße Quittenpaste (dulce de membrillo) aus eingekochter Quitte; nicht frische Quitte, Quittengelee oder beliebige Fruchtkonfitüre.'),
    ('MORCILLA', 'Morcilla', true, true, 'OPEN', 0.3000, 4,
        'Spanische Blutwurstfamilie mit regional stark variierenden Rezepturen, etwa mit Zwiebel, Reis oder Nüssen; Schweineblut ist charakteristisch. Nicht generisch jede Blutwurst und nicht auf Morcilla de Burgos beschränkt.'),
    ('SOBRASADA', 'Sobrasada', true, true, 'SPECIFIC', 0.2500, 4,
        'Streichfähige, mit Paprika gewürzte luftgereifte Rohwurst der Balearen, typischerweise aus Schweinefleisch; nicht Chorizo und nicht ’Nduja.'),
    ('ROMESCO', 'Romesco-Sauce', true, true, 'SPECIFIC', 0.3500, 3,
        'Katalanische Sauce auf Basis gerösteter Paprika beziehungsweise Ñora, Tomate, Nüssen, Öl und Säure; genaue Nussmischung und Brotanteil können variieren. Nicht Ajvar oder allgemeine Paprikasauce.'),
    ('TURRON', 'Turrón', true, true, 'OPEN', 0.3500, 3,
        'Spanische Turrón-Süßwarenfamilie; umfasst insbesondere klassische harte und weiche nussbasierte Varianten wie Alicante und Jijona sowie etablierte weitere Ausprägungen. Nicht generisch jedes Nougat.'),
    ('CAVA', 'Cava', true, true, 'SPECIFIC', 0.2500, 2,
        'Schaumwein der geschützten Ursprungsbezeichnung Cava, überwiegend in Spanien nach traditioneller Flaschengärung erzeugt; Süßegrad und Reife können variieren. Nicht generischer Schaumwein, Champagner oder Prosecco.'),
    ('COCKLES', 'Herzmuscheln', true, true, 'SPECIFIC', 0.3000, 4,
        'Essbare Herzmuscheln (Cockles), frisch, tiefgekühlt oder naturbelassen konserviert; nicht Venusmuscheln, Miesmuscheln oder Jakobsmuscheln.');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('READY_SAUCES_AND_PASTES', 'AIOLI'),
    ('CHEESE', 'MANCHEGO'),
    ('HAM', 'IBERICO_HAM'),
    ('PRESERVED_PRODUCE', 'NORA_PEPPER'),
    ('SPICES', 'NORA_PEPPER'),
    ('ROASTED_RED_PEPPER', 'PIQUILLO_PEPPER'),
    ('CHILI', 'PADRON_PEPPER'),
    ('RICE', 'BOMBA_RICE'),
    ('QUINCE', 'MEMBRILLO'),
    ('PRESERVED_PRODUCE', 'MEMBRILLO'),
    ('BLOOD_SAUSAGE', 'MORCILLA'),
    ('PORK', 'MORCILLA'),
    ('CURED_MEAT', 'SOBRASADA'),
    ('PORK', 'SOBRASADA'),
    ('SAUSAGE', 'SOBRASADA'),
    ('READY_SAUCES_AND_PASTES', 'ROMESCO'),
    ('CONFECTIONERY', 'TURRON'),
    ('COOKING_ALCOHOL', 'CAVA'),
    ('BIVALVES', 'COCKLES')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('AIOLI', 'FAT'),
    ('AIOLI', 'AROMATIC'),
    ('AIOLI', 'SEASONING'),
    ('MANCHEGO', 'ANIMAL_PROTEIN'),
    ('MANCHEGO', 'FAT'),
    ('MANCHEGO', 'SEASONING'),
    ('IBERICO_HAM', 'ANIMAL_PROTEIN'),
    ('IBERICO_HAM', 'FAT'),
    ('IBERICO_HAM', 'SEASONING'),
    ('NORA_PEPPER', 'VEGETABLE'),
    ('NORA_PEPPER', 'AROMATIC'),
    ('NORA_PEPPER', 'SEASONING'),
    ('PIQUILLO_PEPPER', 'VEGETABLE'),
    ('PIQUILLO_PEPPER', 'AROMATIC'),
    ('PIQUILLO_PEPPER', 'SEASONING'),
    ('PADRON_PEPPER', 'VEGETABLE'),
    ('PADRON_PEPPER', 'AROMATIC'),
    ('BOMBA_RICE', 'STARCH'),
    ('MEMBRILLO', 'FRUIT'),
    ('MEMBRILLO', 'SEASONING'),
    ('MORCILLA', 'ANIMAL_PROTEIN'),
    ('MORCILLA', 'FAT'),
    ('SOBRASADA', 'ANIMAL_PROTEIN'),
    ('SOBRASADA', 'FAT'),
    ('SOBRASADA', 'SEASONING'),
    ('ROMESCO', 'ACID'),
    ('ROMESCO', 'FAT'),
    ('ROMESCO', 'AROMATIC'),
    ('ROMESCO', 'SEASONING'),
    ('TURRON', 'FAT'),
    ('TURRON', 'AROMATIC'),
    ('TURRON', 'SEASONING'),
    ('CAVA', 'ACID'),
    ('CAVA', 'SEASONING'),
    ('COCKLES', 'ANIMAL_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('AIOLI', 'DOMINANCE', 5),
    ('AIOLI', 'FATTINESS', 5),
    ('AIOLI', 'UMAMI', 2),
    ('AIOLI', 'SALTINESS', 2),
    ('MANCHEGO', 'DOMINANCE', 4),
    ('MANCHEGO', 'ACIDITY', 2),
    ('MANCHEGO', 'FATTINESS', 4),
    ('MANCHEGO', 'UMAMI', 4),
    ('MANCHEGO', 'SALTINESS', 3),
    ('IBERICO_HAM', 'DOMINANCE', 5),
    ('IBERICO_HAM', 'FATTINESS', 4),
    ('IBERICO_HAM', 'UMAMI', 5),
    ('IBERICO_HAM', 'SALTINESS', 4),
    ('NORA_PEPPER', 'DOMINANCE', 4),
    ('NORA_PEPPER', 'SWEETNESS', 3),
    ('NORA_PEPPER', 'BITTERNESS', 2),
    ('PIQUILLO_PEPPER', 'DOMINANCE', 4),
    ('PIQUILLO_PEPPER', 'SWEETNESS', 4),
    ('PADRON_PEPPER', 'DOMINANCE', 3),
    ('PADRON_PEPPER', 'SWEETNESS', 2),
    ('PADRON_PEPPER', 'BITTERNESS', 2),
    ('PADRON_PEPPER', 'HEAT', 2),
    ('BOMBA_RICE', 'DOMINANCE', 2),
    ('MEMBRILLO', 'DOMINANCE', 4),
    ('MEMBRILLO', 'SWEETNESS', 5),
    ('MEMBRILLO', 'ACIDITY', 3),
    ('MORCILLA', 'DOMINANCE', 5),
    ('MORCILLA', 'SWEETNESS', 2),
    ('MORCILLA', 'FATTINESS', 4),
    ('MORCILLA', 'UMAMI', 5),
    ('MORCILLA', 'SALTINESS', 4),
    ('SOBRASADA', 'DOMINANCE', 5),
    ('SOBRASADA', 'SWEETNESS', 2),
    ('SOBRASADA', 'FATTINESS', 5),
    ('SOBRASADA', 'HEAT', 2),
    ('SOBRASADA', 'UMAMI', 4),
    ('SOBRASADA', 'SALTINESS', 4),
    ('ROMESCO', 'DOMINANCE', 5),
    ('ROMESCO', 'SWEETNESS', 2),
    ('ROMESCO', 'ACIDITY', 3),
    ('ROMESCO', 'FATTINESS', 4),
    ('ROMESCO', 'UMAMI', 3),
    ('ROMESCO', 'SALTINESS', 2),
    ('TURRON', 'DOMINANCE', 4),
    ('TURRON', 'SWEETNESS', 5),
    ('TURRON', 'FATTINESS', 4),
    ('CAVA', 'DOMINANCE', 3),
    ('CAVA', 'SWEETNESS', 2),
    ('CAVA', 'ACIDITY', 3),
    ('COCKLES', 'DOMINANCE', 3),
    ('COCKLES', 'SWEETNESS', 2),
    ('COCKLES', 'UMAMI', 4),
    ('COCKLES', 'SALTINESS', 3)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('MANCHEGO', 'FERMENTED'),
    ('MANCHEGO', 'CURED'),
    ('IBERICO_HAM', 'CURED'),
    ('NORA_PEPPER', 'DRIED'),
    ('SOBRASADA', 'CURED'),
    ('CAVA', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('AIOLI', 'TOBIAS', 'EASY'),
    ('AIOLI', 'GEORGIA', 'EASY'),
    ('MANCHEGO', 'TOBIAS', 'EASY'),
    ('MANCHEGO', 'GEORGIA', 'EASY'),
    ('IBERICO_HAM', 'TOBIAS', 'PLANNED'),
    ('IBERICO_HAM', 'GEORGIA', 'PLANNED'),
    ('NORA_PEPPER', 'TOBIAS', 'PLANNED'),
    ('NORA_PEPPER', 'GEORGIA', 'PLANNED'),
    ('PIQUILLO_PEPPER', 'TOBIAS', 'PLANNED'),
    ('PIQUILLO_PEPPER', 'GEORGIA', 'PLANNED'),
    ('PADRON_PEPPER', 'TOBIAS', 'DIFFICULT'),
    ('PADRON_PEPPER', 'GEORGIA', 'DIFFICULT'),
    ('BOMBA_RICE', 'TOBIAS', 'PLANNED'),
    ('BOMBA_RICE', 'GEORGIA', 'PLANNED'),
    ('MEMBRILLO', 'TOBIAS', 'PLANNED'),
    ('MEMBRILLO', 'GEORGIA', 'PLANNED'),
    ('MORCILLA', 'TOBIAS', 'DIFFICULT'),
    ('MORCILLA', 'GEORGIA', 'DIFFICULT'),
    ('SOBRASADA', 'TOBIAS', 'PLANNED'),
    ('SOBRASADA', 'GEORGIA', 'PLANNED'),
    ('ROMESCO', 'TOBIAS', 'PLANNED'),
    ('ROMESCO', 'GEORGIA', 'PLANNED'),
    ('TURRON', 'TOBIAS', 'PLANNED'),
    ('TURRON', 'GEORGIA', 'PLANNED'),
    ('CAVA', 'TOBIAS', 'EASY'),
    ('CAVA', 'GEORGIA', 'EASY'),
    ('COCKLES', 'TOBIAS', 'PLANNED'),
    ('COCKLES', 'GEORGIA', 'PLANNED')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

-- Composition-based restrictions that are not safely inherited through the
-- refinement graph remain explicit. Aioli deliberately has no NO_EGGS target,
-- because traditional all-i-oli does not require egg.
INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, false
FROM (VALUES
    ('NO_ALLIUMS', 'AIOLI'),
    ('NO_ALLIUMS', 'ROMESCO'),
    ('NO_NUTS', 'ROMESCO'),
    ('NO_TOMATO', 'ROMESCO')
) AS assignment(rule_code, concept_code)
JOIN exclusion_rule rule
  ON rule.code = assignment.rule_code
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'ES'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'OLIVE_OIL',
    'GARLIC',
    'TOMATO',
    'RICE',
    'PAPRIKA_POWDER',
    'SAFFRON',
    'CHORIZO',
    'SERRANO_HAM',
    'SHERRY',
    'SHERRY_VINEGAR',
    'ANCHOVIES',
    'SARDINES',
    'OCTOPUS',
    'SQUID',
    'COD',
    'ALMOND',
    'CHICKPEAS',
    'CIDER',
    'PARSLEY',
    'BAY_LEAF',
    'ROSEMARY',
    'THYME',
    'AIOLI',
    'MANCHEGO',
    'IBERICO_HAM',
    'NORA_PEPPER',
    'PIQUILLO_PEPPER',
    'PADRON_PEPPER',
    'BOMBA_RICE',
    'MEMBRILLO',
    'MORCILLA',
    'SOBRASADA',
    'ROMESCO',
    'TURRON',
    'CAVA',
    'COCKLES'
);
