--liquibase formatted sql

--changeset venomenon328:027-france-curation
-- Issue #172: country-by-country catalog curation pass (France / FR).
-- Adds only the explicitly approved French associations and catalog gaps,
-- plus the approved Brandy/Cognac catalog split.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('FOIE_GRAS', 'Foie gras', true, true, 'SPECIFIC', 0.1500, 5,
        'Fettleberprodukt aus Ente oder Gans; umfasst rohe beziehungsweise zur weiteren Verarbeitung geeignete sowie klassische ganze oder Block-Produktformen. Nicht gewöhnliche Leber und nicht mit Leberpastete gleichsetzen.'),
    ('DUCK_CONFIT', 'Entenconfit', true, true, 'SPECIFIC', 0.3000, 4,
        'Gesalzene Ententeile, typischerweise Keule beziehungsweise vergleichbare Teilstücke, langsam in Entenfett gegart und traditionell darin konserviert. Nicht bloß Entenfleisch oder eine frisch gebratene Entenkeule.'),
    ('BUCKWHEAT_FLOUR', 'Buchweizenmehl', true, true, 'SPECIFIC', 0.5500, 2,
        'Mehl aus Buchweizen; nicht Weizenmehl mit Buchweizenanteil und nicht ganzes Buchweizenkorn.'),
    ('HERBES_DE_PROVENCE', 'Kräuter der Provence', true, true, 'SPECIFIC', 0.5000, 1,
        'Getrocknete Kräutermischung nach Herbes-de-Provence-Art, typischerweise mit Thymian, Rosmarin, Oregano, Bohnenkraut und/oder vergleichbaren mediterranen Kräutern. Zusammensetzung kann variieren; nicht mit Fines herbes gleichsetzen.'),
    ('PIMENT_D_ESPELETTE', 'Piment d’Espelette', true, true, 'SPECIFIC', 0.2500, 4,
        'Hier als getrocknetes, fein gemahlenes Piment-d’Espelette-Gewürz nach französisch-baskischer Art modelliert. Nicht generisches Chilipulver und nicht die frische ganze Espelette-Schote.'),
    ('ROQUEFORT', 'Roquefort', true, true, 'SPECIFIC', 0.4000, 2,
        'Französischer Blauschimmelkäse aus Schafmilch nach Roquefort-AOP; kräftig, salzig und ausgeprägt würzig. Nicht generischer Blauschimmelkäse.'),
    ('BRIE', 'Brie', true, true, 'SPECIFIC', 0.7000, 1,
        'Weichkäse der Brie-Familie mit charakteristischer Weißschimmelrinde; umfasst handelsüblichen Brie-Stil und bekannte französische Ausprägungen, ohne auf Brie de Meaux oder Brie de Melun beschränkt zu sein. Nicht Camembert.'),
    ('CROISSANT', 'Croissant', true, true, 'SPECIFIC', 0.4500, 1,
        'Ungefüllte klassische Croissant-Viennoiserie mit blättriger, fettreicher Teigstruktur; nicht Pain au chocolat, gefülltes Croissant oder beliebiges Hörnchen.'),
    ('COGNAC', 'Cognac', true, true, 'SPECIFIC', 0.2500, 2,
        'Geschützter französischer Weinbrand Cognac; eigenständige Konkretisierung des generischen Brandy-/Weinbrand-Konzepts. Nicht beliebiger Brandy.'),
    ('CALVADOS', 'Calvados', true, true, 'SPECIFIC', 0.2500, 3,
        'Französischer Apfel- beziehungsweise Apfel-Birnen-Brand nach Calvados-Art; nicht Cider, Apfellikör oder generischer Obstbrand.'),
    ('PASTIS', 'Pastis', true, true, 'SPECIFIC', 0.2500, 3,
        'Anisbetonte französische Spirituose nach Pastis-Art, typischerweise zusätzlich mit Süßholz und weiteren Kräutern beziehungsweise Gewürzen aromatisiert. Nicht Absinth oder beliebiger Anislikör.'),
    ('RILLETTES', 'Rillettes', true, true, 'OPEN', 0.3000, 3,
        'Offene Familie langsam gegarter und zerfaserter Fleischaufstriche nach Rillettes-Art, insbesondere aus Schwein, Ente oder Gans. Fisch-Rillettes und gewöhnliche Leberpastete sind ausdrücklich nicht gemeint.'),
    ('CHERVIL', 'Kerbel', true, true, 'SPECIFIC', 0.3500, 3,
        'Frischer Kerbel als fein-aromatisches Küchenkraut. Nicht Kerbelwurzel und nicht getrocknete Kräutermischung.'),
    ('FLEUR_DE_SEL', 'Fleur de sel', true, true, 'SPECIFIC', 0.3500, 2,
        'Feine, an der Oberfläche von Salzgärten abgeschöpfte Meersalzkristalle nach Fleur-de-sel-Art; nicht gewöhnliches Tafelsalz und nicht beliebiges grobes Meersalz. Keine Herkunft aus Guérande wird vorausgesetzt.');

UPDATE ingredient_concept
SET display_name = 'Brandy oder Weinbrand',
    challenge_specificity = 'OPEN',
    curator_note = 'Offene Weinbrandfamilie; geschützte beziehungsweise kulinarisch eigenständige Ausprägungen wie Cognac können separat konkretisiert werden.'
WHERE code = 'BRANDY';

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('OFFAL', 'FOIE_GRAS'),
    ('POULTRY', 'FOIE_GRAS'),
    ('DUCK', 'DUCK_CONFIT'),
    ('BUCKWHEAT', 'BUCKWHEAT_FLOUR'),
    ('FLOUR', 'BUCKWHEAT_FLOUR'),
    ('SPICE_BLENDS', 'HERBES_DE_PROVENCE'),
    ('CHILI_POWDER', 'PIMENT_D_ESPELETTE'),
    ('BLUE_CHEESE', 'ROQUEFORT'),
    ('CHEESE', 'BRIE'),
    ('BAKED_GOODS', 'CROISSANT'),
    ('BRANDY', 'COGNAC'),
    ('COOKING_ALCOHOL', 'CALVADOS'),
    ('COOKING_ALCOHOL', 'PASTIS'),
    ('MEAT', 'RILLETTES'),
    ('FRESH_HERBS', 'CHERVIL'),
    ('SPICES', 'FLEUR_DE_SEL')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('FOIE_GRAS', 'ANIMAL_PROTEIN'),
    ('FOIE_GRAS', 'FAT'),
    ('FOIE_GRAS', 'SEASONING'),
    ('DUCK_CONFIT', 'ANIMAL_PROTEIN'),
    ('DUCK_CONFIT', 'FAT'),
    ('BUCKWHEAT_FLOUR', 'STARCH'),
    ('HERBES_DE_PROVENCE', 'AROMATIC'),
    ('HERBES_DE_PROVENCE', 'SEASONING'),
    ('PIMENT_D_ESPELETTE', 'AROMATIC'),
    ('PIMENT_D_ESPELETTE', 'SEASONING'),
    ('ROQUEFORT', 'ANIMAL_PROTEIN'),
    ('ROQUEFORT', 'FAT'),
    ('ROQUEFORT', 'SEASONING'),
    ('BRIE', 'ANIMAL_PROTEIN'),
    ('BRIE', 'FAT'),
    ('BRIE', 'SEASONING'),
    ('CROISSANT', 'STARCH'),
    ('CROISSANT', 'FAT'),
    ('COGNAC', 'ACID'),
    ('COGNAC', 'SEASONING'),
    ('CALVADOS', 'ACID'),
    ('CALVADOS', 'SEASONING'),
    ('PASTIS', 'ACID'),
    ('PASTIS', 'AROMATIC'),
    ('PASTIS', 'SEASONING'),
    ('RILLETTES', 'ANIMAL_PROTEIN'),
    ('RILLETTES', 'FAT'),
    ('RILLETTES', 'SEASONING'),
    ('CHERVIL', 'AROMATIC'),
    ('FLEUR_DE_SEL', 'SEASONING')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('FOIE_GRAS', 'DOMINANCE', 5),
    ('FOIE_GRAS', 'FATTINESS', 5),
    ('FOIE_GRAS', 'UMAMI', 4),
    ('DUCK_CONFIT', 'DOMINANCE', 4),
    ('DUCK_CONFIT', 'FATTINESS', 5),
    ('DUCK_CONFIT', 'UMAMI', 5),
    ('DUCK_CONFIT', 'SALTINESS', 4),
    ('BUCKWHEAT_FLOUR', 'DOMINANCE', 3),
    ('BUCKWHEAT_FLOUR', 'BITTERNESS', 2),
    ('HERBES_DE_PROVENCE', 'DOMINANCE', 5),
    ('HERBES_DE_PROVENCE', 'BITTERNESS', 2),
    ('PIMENT_D_ESPELETTE', 'DOMINANCE', 5),
    ('PIMENT_D_ESPELETTE', 'SWEETNESS', 2),
    ('PIMENT_D_ESPELETTE', 'HEAT', 3),
    ('ROQUEFORT', 'DOMINANCE', 5),
    ('ROQUEFORT', 'ACIDITY', 3),
    ('ROQUEFORT', 'BITTERNESS', 2),
    ('ROQUEFORT', 'FATTINESS', 4),
    ('ROQUEFORT', 'UMAMI', 5),
    ('ROQUEFORT', 'SALTINESS', 5),
    ('BRIE', 'DOMINANCE', 3),
    ('BRIE', 'ACIDITY', 2),
    ('BRIE', 'FATTINESS', 4),
    ('BRIE', 'UMAMI', 3),
    ('BRIE', 'SALTINESS', 2),
    ('CROISSANT', 'DOMINANCE', 3),
    ('CROISSANT', 'SWEETNESS', 2),
    ('CROISSANT', 'FATTINESS', 5),
    ('CROISSANT', 'SALTINESS', 2),
    ('COGNAC', 'DOMINANCE', 4),
    ('COGNAC', 'SWEETNESS', 2),
    ('COGNAC', 'ACIDITY', 2),
    ('CALVADOS', 'DOMINANCE', 4),
    ('CALVADOS', 'SWEETNESS', 2),
    ('CALVADOS', 'ACIDITY', 2),
    ('PASTIS', 'DOMINANCE', 5),
    ('PASTIS', 'SWEETNESS', 2),
    ('PASTIS', 'BITTERNESS', 2),
    ('RILLETTES', 'DOMINANCE', 4),
    ('RILLETTES', 'FATTINESS', 5),
    ('RILLETTES', 'UMAMI', 5),
    ('RILLETTES', 'SALTINESS', 4),
    ('CHERVIL', 'DOMINANCE', 3),
    ('FLEUR_DE_SEL', 'DOMINANCE', 3),
    ('FLEUR_DE_SEL', 'SALTINESS', 5)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('DUCK_CONFIT', 'CURED'),
    ('HERBES_DE_PROVENCE', 'DRIED'),
    ('PIMENT_D_ESPELETTE', 'DRIED'),
    ('ROQUEFORT', 'FERMENTED'),
    ('ROQUEFORT', 'CURED'),
    ('BRIE', 'FERMENTED'),
    ('BRIE', 'CURED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('FOIE_GRAS', 'TOBIAS', 'DIFFICULT'),
    ('FOIE_GRAS', 'GEORGIA', 'DIFFICULT'),
    ('DUCK_CONFIT', 'TOBIAS', 'DIFFICULT'),
    ('DUCK_CONFIT', 'GEORGIA', 'PLANNED'),
    ('BUCKWHEAT_FLOUR', 'TOBIAS', 'EASY'),
    ('BUCKWHEAT_FLOUR', 'GEORGIA', 'EASY'),
    ('HERBES_DE_PROVENCE', 'TOBIAS', 'EASY'),
    ('HERBES_DE_PROVENCE', 'GEORGIA', 'EASY'),
    ('PIMENT_D_ESPELETTE', 'TOBIAS', 'DIFFICULT'),
    ('PIMENT_D_ESPELETTE', 'GEORGIA', 'PLANNED'),
    ('ROQUEFORT', 'TOBIAS', 'EASY'),
    ('ROQUEFORT', 'GEORGIA', 'EASY'),
    ('BRIE', 'TOBIAS', 'EASY'),
    ('BRIE', 'GEORGIA', 'EASY'),
    ('CROISSANT', 'TOBIAS', 'EASY'),
    ('CROISSANT', 'GEORGIA', 'EASY'),
    ('COGNAC', 'TOBIAS', 'EASY'),
    ('COGNAC', 'GEORGIA', 'EASY'),
    ('CALVADOS', 'TOBIAS', 'PLANNED'),
    ('CALVADOS', 'GEORGIA', 'PLANNED'),
    ('PASTIS', 'TOBIAS', 'PLANNED'),
    ('PASTIS', 'GEORGIA', 'PLANNED'),
    ('RILLETTES', 'TOBIAS', 'PLANNED'),
    ('RILLETTES', 'GEORGIA', 'PLANNED'),
    ('CHERVIL', 'TOBIAS', 'PLANNED'),
    ('CHERVIL', 'GEORGIA', 'PLANNED'),
    ('FLEUR_DE_SEL', 'TOBIAS', 'EASY'),
    ('FLEUR_DE_SEL', 'GEORGIA', 'EASY')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, false
FROM exclusion_rule rule
JOIN ingredient_concept concept
  ON concept.code = 'CROISSANT'
WHERE rule.code = 'NO_DAIRY';

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'FR'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'BREAD',
    'BAGUETTE',
    'BUTTER',
    'CREME_FRAICHE',
    'CHEESE',
    'CAMEMBERT',
    'GOAT_CHEESE',
    'BLUE_CHEESE',
    'GRUYERE',
    'RED_WINE',
    'WHITE_WINE',
    'CIDER',
    'MUSTARD',
    'DIJON_MUSTARD',
    'BEEF',
    'DUCK',
    'DUCK_BREAST',
    'DUCK_LEG',
    'DUCK_FAT',
    'RABBIT',
    'ESCARGOT',
    'FROG_LEGS',
    'LIVER_PATE',
    'MUSSELS',
    'OYSTER',
    'SCALLOPS',
    'SARDINES',
    'ANCHOVIES',
    'LENTILS',
    'WHITE_BEANS',
    'BUCKWHEAT',
    'CHICORY',
    'ARTICHOKE',
    'TOMATO',
    'EGGPLANT',
    'ZUCCHINI',
    'BELL_PEPPER',
    'CHAMPIGNON',
    'TRUFFLE',
    'PARSLEY',
    'CHIVES',
    'TARRAGON',
    'THYME',
    'ROSEMARY',
    'BASIL',
    'GARLIC',
    'OLIVES',
    'OLIVE_OIL',
    'CAPERS',
    'TAPENADE',
    'REMOULADE',
    'PICKLED_CUCUMBER',
    'TRIPE',
    'SOLE',
    'MONKFISH',
    'COD',
    'MOREL',
    'PORCINI',
    'CHANTERELLE',
    'LEEK',
    'SHALLOT',
    'CELERIAC',
    'FENNEL',
    'BLACK_CURRANT',
    'MIRABELLE',
    'FOIE_GRAS',
    'DUCK_CONFIT',
    'BUCKWHEAT_FLOUR',
    'HERBES_DE_PROVENCE',
    'PIMENT_D_ESPELETTE',
    'ROQUEFORT',
    'BRIE',
    'CROISSANT',
    'COGNAC',
    'CALVADOS',
    'PASTIS',
    'RILLETTES',
    'CHERVIL',
    'FLEUR_DE_SEL'
);
