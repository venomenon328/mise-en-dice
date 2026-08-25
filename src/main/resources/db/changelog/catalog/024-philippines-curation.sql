--liquibase formatted sql

--changeset venomenon328:024-philippines-curation
-- Issue #172: country-by-country catalog curation pass (Philippines / PH).
-- Adds only the explicitly approved Philippine associations and catalog gaps.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('MILKFISH', 'Milchfisch oder Bangus', true, true, 'SPECIFIC', 0.3000, 4,
        'Milchfisch (Chanos chanos), auf den Philippinen Bangus; frischer oder tiefgekühlter ganzer Fisch, Stück oder Filet. Nicht automatisch getrocknete, geräucherte oder anderweitig konservierte Bangus-Produkte.'),
    ('NATA_DE_COCO', 'Nata de coco', true, true, 'SPECIFIC', 0.3000, 4,
        'Kaubares, transparentes Cellulosegel aus fermentativ verarbeiteter Kokosrohware; typischerweise gesüßt als Dessert-, Getränke- oder Fruchtsalatzutat angeboten. Nicht mit Agar-, Konjak- oder beliebigem Kokosgelee gleichsetzen.'),
    ('MACAPUNO', 'Macapuno', true, true, 'SPECIFIC', 0.2000, 5,
        'Weiches bis gelatinöses Endosperm einer besonderen Kokosnussausprägung; frisch, tiefgekühlt oder konserviert verwendbar. Nicht gewöhnliches Kokosfleisch, Kokoscreme oder bloß mit Kokos aromatisierte Süßware.'),
    ('SALTED_DUCK_EGG', 'gesalzenes Entenei', true, true, 'SPECIFIC', 0.2500, 4,
        'Durch Salz beziehungsweise Lake konserviertes Entenei, philippinisch insbesondere als itlog na maalat bekannt. Gemeint ist das ganze Ei; nicht Balut und nicht Salted-Egg-Pulver oder bloßes Salted-Egg-Aroma.'),
    ('COCONUT_VINEGAR', 'Kokosessig', true, true, 'OPEN', 0.4000, 3,
        'Offene Kokosessigfamilie für natürlich fermentierten Essig auf Kokoswasser- und/oder Kokospalmensaftbasis. Nicht gewöhnlicher Branntweinessig mit Kokosaroma.'),
    ('TABLEA', 'Tablea oder Tableya', true, true, 'SPECIFIC', 0.2500, 4,
        'Philippinische gepresste beziehungsweise geformte Kakaomasse aus gerösteten und gemahlenen Kakaobohnen, traditionell insbesondere für Tsokolate. Gemeint ist reine oder nur gering gesüßte Tablea; nicht Kakaopulver oder gewöhnliche Tafelschokolade.'),
    ('MORINGA_LEAVES', 'Moringablätter oder Malunggay', true, true, 'SPECIFIC', 0.2500, 4,
        'Blätter von Moringa oleifera, auf den Philippinen Malunggay; frisch, tiefgekühlt oder als kulinarisch verwendete getrocknete Blattform. Nicht Samen, Öl oder Nahrungsergänzungskapseln.'),
    ('DRIED_FISH', 'getrockneter Fisch', true, true, 'OPEN', 0.3500, 3,
        'Offene Familie von Fischprodukten, deren wesentliche Konservierung durch Trocknung erfolgt; Salz kann, muss aber nicht beteiligt sein. Nicht bloß geräucherter oder konservierter Fisch ohne maßgebliche Trocknung.'),
    ('DAING', 'Daing', true, true, 'OPEN', 0.2500, 4,
        'Philippinische Familie getrockneter Fische in typischerweise aufgeschnittener beziehungsweise butterfly-artiger Form; Fischart und Salzgrad können variieren. Kein Synonym für eine bestimmte Fischart.'),
    ('LONGGANISA', 'Longganisa', true, true, 'OPEN', 0.3000, 4,
        'Offene philippinische Wurstfamilie mit regional stark unterschiedlichen Rezepturen, von süßlichen bis kräftig knoblauchbetonten Varianten. Üblicherweise, aber nicht zwingend, aus Schweinefleisch; keine bestimmte regionale Longganisa wird vorausgesetzt.'),
    ('SUGARCANE_VINEGAR', 'Zuckerrohressig', true, true, 'SPECIFIC', 0.4000, 3,
        'Natürlich fermentierter Essig auf Basis von Zuckerrohr beziehungsweise Zuckerrohrsaft. Gemeint ist die generische Essigart; nicht auf Sukang Iloko oder eine bestimmte Marke beschränkt.'),
    ('NIPA_PALM_VINEGAR', 'Nipapalmenessig', true, true, 'SPECIFIC', 0.2000, 5,
        'Natürlich fermentierter Essig aus dem Saft der Nipapalme. Gemeint ist die generische Produktart; regionale Produkte wie Sukang Paombong sind mögliche Ausprägungen, aber keine Voraussetzung.');

-- Introduce the approved reusable dried-fish family without changing the
-- transitive "is preserved fish" semantics of existing dried fish products.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'PRESERVED_FISH'
  AND child.code IN ('BONITO_FLAKES', 'STOCKFISH');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('FISH', 'MILKFISH'),
    ('COCONUT_PRODUCTS', 'NATA_DE_COCO'),
    ('COCONUT', 'MACAPUNO'),
    ('DUCK_EGG', 'SALTED_DUCK_EGG'),
    ('VINEGAR', 'COCONUT_VINEGAR'),
    ('COCONUT_PRODUCTS', 'COCONUT_VINEGAR'),
    ('COCOA_PRODUCTS', 'TABLEA'),
    ('LEAFY_GREENS', 'MORINGA_LEAVES'),
    ('PRESERVED_FISH', 'DRIED_FISH'),
    ('DRIED_FISH', 'BONITO_FLAKES'),
    ('DRIED_FISH', 'STOCKFISH'),
    ('DRIED_FISH', 'DAING'),
    ('SAUSAGE', 'LONGGANISA'),
    ('VINEGAR', 'SUGARCANE_VINEGAR'),
    ('VINEGAR', 'NIPA_PALM_VINEGAR')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('MILKFISH', 'ANIMAL_PROTEIN'),
    ('NATA_DE_COCO', 'FRUIT'),
    ('MACAPUNO', 'FRUIT'),
    ('MACAPUNO', 'FAT'),
    ('SALTED_DUCK_EGG', 'ANIMAL_PROTEIN'),
    ('SALTED_DUCK_EGG', 'FAT'),
    ('SALTED_DUCK_EGG', 'SEASONING'),
    ('COCONUT_VINEGAR', 'ACID'),
    ('COCONUT_VINEGAR', 'SEASONING'),
    ('TABLEA', 'AROMATIC'),
    ('TABLEA', 'FAT'),
    ('TABLEA', 'SEASONING'),
    ('MORINGA_LEAVES', 'VEGETABLE'),
    ('DRIED_FISH', 'ANIMAL_PROTEIN'),
    ('DRIED_FISH', 'SEASONING'),
    ('DAING', 'ANIMAL_PROTEIN'),
    ('DAING', 'SEASONING'),
    ('LONGGANISA', 'ANIMAL_PROTEIN'),
    ('LONGGANISA', 'FAT'),
    ('SUGARCANE_VINEGAR', 'ACID'),
    ('SUGARCANE_VINEGAR', 'SEASONING'),
    ('NIPA_PALM_VINEGAR', 'ACID'),
    ('NIPA_PALM_VINEGAR', 'SEASONING')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('MILKFISH', 'DOMINANCE', 3),
    ('MILKFISH', 'FATTINESS', 3),
    ('MILKFISH', 'UMAMI', 3),
    ('NATA_DE_COCO', 'DOMINANCE', 2),
    ('NATA_DE_COCO', 'SWEETNESS', 4),
    ('MACAPUNO', 'DOMINANCE', 3),
    ('MACAPUNO', 'SWEETNESS', 3),
    ('MACAPUNO', 'FATTINESS', 3),
    ('SALTED_DUCK_EGG', 'DOMINANCE', 4),
    ('SALTED_DUCK_EGG', 'FATTINESS', 4),
    ('SALTED_DUCK_EGG', 'UMAMI', 4),
    ('SALTED_DUCK_EGG', 'SALTINESS', 5),
    ('COCONUT_VINEGAR', 'DOMINANCE', 3),
    ('COCONUT_VINEGAR', 'ACIDITY', 5),
    ('TABLEA', 'DOMINANCE', 5),
    ('TABLEA', 'SWEETNESS', 1),
    ('TABLEA', 'BITTERNESS', 4),
    ('TABLEA', 'FATTINESS', 4),
    ('MORINGA_LEAVES', 'DOMINANCE', 3),
    ('MORINGA_LEAVES', 'BITTERNESS', 2),
    ('MORINGA_LEAVES', 'UMAMI', 2),
    ('DRIED_FISH', 'DOMINANCE', 5),
    ('DRIED_FISH', 'UMAMI', 5),
    ('DRIED_FISH', 'SALTINESS', 4),
    ('DAING', 'DOMINANCE', 4),
    ('DAING', 'UMAMI', 4),
    ('DAING', 'SALTINESS', 4),
    ('LONGGANISA', 'DOMINANCE', 4),
    ('LONGGANISA', 'SWEETNESS', 3),
    ('LONGGANISA', 'FATTINESS', 4),
    ('LONGGANISA', 'UMAMI', 4),
    ('LONGGANISA', 'SALTINESS', 3),
    ('SUGARCANE_VINEGAR', 'DOMINANCE', 3),
    ('SUGARCANE_VINEGAR', 'ACIDITY', 5),
    ('NIPA_PALM_VINEGAR', 'DOMINANCE', 3),
    ('NIPA_PALM_VINEGAR', 'ACIDITY', 5)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('NATA_DE_COCO', 'FERMENTED'),
    ('SALTED_DUCK_EGG', 'CURED'),
    ('DRIED_FISH', 'DRIED'),
    ('DAING', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('MILKFISH', 'TOBIAS', 'DIFFICULT'),
    ('MILKFISH', 'GEORGIA', 'PLANNED'),
    ('NATA_DE_COCO', 'TOBIAS', 'PLANNED'),
    ('NATA_DE_COCO', 'GEORGIA', 'PLANNED'),
    ('MACAPUNO', 'TOBIAS', 'DIFFICULT'),
    ('MACAPUNO', 'GEORGIA', 'PLANNED'),
    ('SALTED_DUCK_EGG', 'TOBIAS', 'DIFFICULT'),
    ('SALTED_DUCK_EGG', 'GEORGIA', 'PLANNED'),
    ('COCONUT_VINEGAR', 'TOBIAS', 'PLANNED'),
    ('COCONUT_VINEGAR', 'GEORGIA', 'PLANNED'),
    ('TABLEA', 'TOBIAS', 'DIFFICULT'),
    ('TABLEA', 'GEORGIA', 'PLANNED'),
    ('MORINGA_LEAVES', 'TOBIAS', 'DIFFICULT'),
    ('MORINGA_LEAVES', 'GEORGIA', 'PLANNED'),
    ('DRIED_FISH', 'TOBIAS', 'PLANNED'),
    ('DRIED_FISH', 'GEORGIA', 'PLANNED'),
    ('DAING', 'TOBIAS', 'DIFFICULT'),
    ('DAING', 'GEORGIA', 'PLANNED'),
    ('LONGGANISA', 'TOBIAS', 'PLANNED'),
    ('LONGGANISA', 'GEORGIA', 'PLANNED'),
    ('SUGARCANE_VINEGAR', 'TOBIAS', 'PLANNED'),
    ('SUGARCANE_VINEGAR', 'GEORGIA', 'EASY'),
    ('NIPA_PALM_VINEGAR', 'TOBIAS', 'DIFFICULT'),
    ('NIPA_PALM_VINEGAR', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'PH'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'CALAMANSI',
    'SOY_SAUCE',
    'FISH_SAUCE',
    'BAGOONG',
    'BAGOONG_ALAMANG',
    'BAGOONG_ISDA',
    'ALIGUE',
    'BANANA_KETCHUP',
    'ANNATTO',
    'TAMARIND',
    'RICE',
    'STICKY_RICE',
    'JASMINE_RICE',
    'NOODLES',
    'PORK',
    'PORK_BELLY',
    'COCONUT',
    'COCONUT_MILK',
    'COCONUT_CREAM',
    'UBE',
    'PLANTAIN',
    'GREEN_PAPAYA',
    'PANDAN_LEAVES',
    'CASSAVA',
    'JACKFRUIT',
    'MANGO',
    'MUNG_BEANS',
    'BITTER_MELON',
    'WATER_SPINACH',
    'CONDENSED_MILK',
    'EVAPORATED_MILK',
    'TARO',
    'SWEET_POTATO',
    'MILKFISH',
    'NATA_DE_COCO',
    'MACAPUNO',
    'SALTED_DUCK_EGG',
    'COCONUT_VINEGAR',
    'TABLEA',
    'MORINGA_LEAVES',
    'DRIED_FISH',
    'DAING',
    'LONGGANISA',
    'SUGARCANE_VINEGAR',
    'NIPA_PALM_VINEGAR'
);
