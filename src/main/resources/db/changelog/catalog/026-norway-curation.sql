--liquibase formatted sql

--changeset venomenon328:026-norway-curation
-- Issue #172: country-by-country catalog curation pass (Norway / NO).
-- Adds only the explicitly approved Norwegian associations and catalog gaps,
-- the approved Flatbread graph cleanup, and the approved Sweden backfills.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('BRUNOST', 'Brunost', true, true, 'SPECIFIC', 0.3500, 4,
        'Norwegisches braunes Molkeprodukt mit charakteristisch süßlich-karamelligem Geschmack, der durch starkes Einkochen von Molke und Milchbestandteilen entsteht. Kuh- und Ziegenmilchanteile können je nach Variante variieren. Nicht mit gewöhnlichem Ziegenkäse oder Karamellaufstrich gleichsetzen.'),
    ('KLIPPFISH', 'Klippfisch', true, true, 'SPECIFIC', 0.2500, 4,
        'Stark gesalzener und anschließend getrockneter Fisch nach Klippfisch-/Klippfisk-Art, häufig Kabeljau oder anderer geeigneter Weißfisch; vor der Zubereitung typischerweise zu wässern. Nicht mit ungesalzenem Stockfisch gleichsetzen.'),
    ('CLOUDBERRY', 'Moltebeere', true, true, 'SPECIFIC', 0.2500, 4,
        'Moltebeeren (Rubus chamaemorus), frisch oder ungesüßt tiefgekühlt. Nicht gesüßte Moltebeerkonfitüre, Likör oder lediglich Moltebeer-Aroma.'),
    ('CLOUDBERRY_PRESERVES', 'Moltebeerkonfitüre/-kompott', true, true, 'SPECIFIC', 0.3500, 3,
        'Gesüßte Moltebeerzubereitung wie Konfitüre, Kompott oder vergleichbarer Fruchtaufstrich. Nicht bloßes Moltebeer-Aroma oder Moltebeerlikör.'),
    ('REINDEER', 'Rentierfleisch', true, true, 'SPECIFIC', 0.2500, 4,
        'Rentierfleisch als frisches oder tiefgekühltes Fleischstück beziehungsweise geeignetes Gulasch-/Geschnetzeltesfleisch. Nicht Rentierwurst, Trockenfleisch oder ein bereits zubereitetes Rentiergericht.'),
    ('NORTHERN_PRAWN', 'Eismeergarnele', true, true, 'SPECIFIC', 0.4500, 3,
        'Eismeergarnele beziehungsweise Nordische Garnele (Pandalus borealis), frisch oder tiefgekühlt und je nach Produkt geschält oder ungeschält. Nicht mit der Nordseekrabbe (Crangon crangon) oder beliebigen Warmwassergarnelen gleichsetzen.'),
    ('FLATBROD', 'Flatbrød', true, true, 'SPECIFIC', 0.4000, 3,
        'Sehr dünnes, trockenes und knuspriges norwegisches Fladenbrot nach Flatbrød-Art. Traditionelle Getreidebasis kann variieren. Nicht generisches Knäckebrot oder beliebiges weiches Fladenbrot.'),
    ('LEFSE', 'Lefse', true, true, 'SPECIFIC', 0.3500, 3,
        'Weiches norwegisches Fladenbrot nach Lefse-Art; je nach Variante auf Kartoffel- oder Mehlbasis. Gemeint ist die Lefse selbst, nicht eine bestimmte süße oder herzhafte Füllung beziehungsweise ein fertig belegtes Gericht.'),
    ('NORWEGIAN_WAFFLE', 'Norwegische Waffel', true, true, 'SPECIFIC', 0.4500, 2,
        'Dünne, weiche Waffel nach norwegischer Art, typischerweise herzförmig und häufig leicht süß sowie mit Kardamom aromatisiert. Das Konzept bezeichnet die Waffel selbst; Brunost, Sauerrahm, Konfitüre oder andere Beläge werden nicht vorausgesetzt.'),
    ('FENALAR', 'Fenalår', true, true, 'SPECIFIC', 0.2000, 4,
        'Gesalzene und luftgetrocknete Keule von Schaf oder Lamm nach norwegischer Fenalår-Art. Nicht beliebiges Trockenfleisch, Schinken oder bereits zubereitetes Gericht.'),
    ('PINNEKJOTT', 'Pinnekjøtt', true, true, 'SPECIFIC', 0.1500, 5,
        'Gesalzene und getrocknete Rippen vom Schaf oder Lamm nach Pinnekjøtt-Art; geräucherte Varianten sind möglich, werden aber nicht vorausgesetzt. Gemeint ist das noch zuzubereitende Fleischprodukt, nicht das vollständige Weihnachtsgericht.'),
    ('RAKFISK', 'Rakfisk', true, true, 'SPECIFIC', 0.1000, 5,
        'Professionell hergestellter, gesalzener und fermentierter Süßwasserfisch nach Rakfisk-Art. Gemeint ist ein handelsübliches verzehrfertiges Produkt; keine Aufforderung zur häuslichen Fischfermentation.'),
    ('LUTEFISK', 'Lutefisk', true, true, 'SPECIFIC', 0.1000, 5,
        'Aus getrocknetem Weißfisch durch Wässern und Laugenbehandlung hergestelltes und anschließend gründlich gespültes, küchenfertiges Fischprodukt nach Lutefisk-Art. Nicht trockener Stockfisch, Lauge oder das fertig zubereitete Gericht.'),
    ('MOOSE', 'Elchfleisch', true, true, 'SPECIFIC', 0.2000, 4,
        'Elchfleisch als frisches oder tiefgekühltes Fleischstück beziehungsweise geeignetes Gulasch-/Geschnetzeltesfleisch. Nicht Elchwurst, Trockenfleisch oder ein bereits zubereitetes Wildgericht.');

UPDATE ingredient_concept
SET display_name = 'Köhler (Seelachs)',
    curator_note = 'Gemeint ist Köhler beziehungsweise Seelachs (Pollachius virens), nicht Alaska-Seelachs (Gadus chalcogrammus).'
WHERE code = 'POLLOCK';

UPDATE ingredient_concept
SET curator_note = 'Ungesalzener, luftgetrockneter Fisch, typischerweise Kabeljau; nicht mit gesalzenem und getrocknetem Klippfisch gleichsetzen.'
WHERE code = 'STOCKFISH';

UPDATE ingredient_concept
SET challenge_specificity = 'OPEN'
WHERE code = 'FLATBREAD';

-- Pita is a concrete flatbread; keep the graph transitively reduced.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'BREAD'
  AND child.code = 'PITA';

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('CHEESE', 'BRUNOST'),
    ('PRESERVED_FISH', 'KLIPPFISH'),
    ('BERRIES', 'CLOUDBERRY'),
    ('CLOUDBERRY', 'CLOUDBERRY_PRESERVES'),
    ('PRESERVED_PRODUCE', 'CLOUDBERRY_PRESERVES'),
    ('GAME_MEAT', 'REINDEER'),
    ('SHRIMP', 'NORTHERN_PRAWN'),
    ('FLATBREAD', 'PITA'),
    ('FLATBREAD', 'FLATBROD'),
    ('FLATBREAD', 'LEFSE'),
    ('WAFFLES', 'NORWEGIAN_WAFFLE'),
    ('CURED_MEAT', 'FENALAR'),
    ('CURED_MEAT', 'PINNEKJOTT'),
    ('PRESERVED_FISH', 'RAKFISK'),
    ('PRESERVED_FISH', 'LUTEFISK'),
    ('GAME_MEAT', 'MOOSE')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('BRUNOST', 'ANIMAL_PROTEIN'),
    ('BRUNOST', 'FAT'),
    ('BRUNOST', 'SEASONING'),
    ('KLIPPFISH', 'ANIMAL_PROTEIN'),
    ('KLIPPFISH', 'SEASONING'),
    ('CLOUDBERRY', 'FRUIT'),
    ('CLOUDBERRY', 'ACID'),
    ('CLOUDBERRY_PRESERVES', 'FRUIT'),
    ('CLOUDBERRY_PRESERVES', 'ACID'),
    ('CLOUDBERRY_PRESERVES', 'SEASONING'),
    ('REINDEER', 'ANIMAL_PROTEIN'),
    ('NORTHERN_PRAWN', 'ANIMAL_PROTEIN'),
    ('FLATBROD', 'STARCH'),
    ('LEFSE', 'STARCH'),
    ('NORWEGIAN_WAFFLE', 'STARCH'),
    ('NORWEGIAN_WAFFLE', 'FAT'),
    ('FENALAR', 'ANIMAL_PROTEIN'),
    ('FENALAR', 'SEASONING'),
    ('PINNEKJOTT', 'ANIMAL_PROTEIN'),
    ('PINNEKJOTT', 'FAT'),
    ('PINNEKJOTT', 'SEASONING'),
    ('RAKFISK', 'ANIMAL_PROTEIN'),
    ('RAKFISK', 'FAT'),
    ('RAKFISK', 'SEASONING'),
    ('LUTEFISK', 'ANIMAL_PROTEIN'),
    ('MOOSE', 'ANIMAL_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('BRUNOST', 'DOMINANCE', 4),
    ('BRUNOST', 'SWEETNESS', 4),
    ('BRUNOST', 'FATTINESS', 4),
    ('BRUNOST', 'UMAMI', 3),
    ('BRUNOST', 'SALTINESS', 2),
    ('KLIPPFISH', 'DOMINANCE', 4),
    ('KLIPPFISH', 'UMAMI', 5),
    ('KLIPPFISH', 'SALTINESS', 5),
    ('CLOUDBERRY', 'DOMINANCE', 3),
    ('CLOUDBERRY', 'SWEETNESS', 3),
    ('CLOUDBERRY', 'ACIDITY', 4),
    ('CLOUDBERRY', 'BITTERNESS', 2),
    ('CLOUDBERRY_PRESERVES', 'DOMINANCE', 4),
    ('CLOUDBERRY_PRESERVES', 'SWEETNESS', 5),
    ('CLOUDBERRY_PRESERVES', 'ACIDITY', 3),
    ('REINDEER', 'DOMINANCE', 3),
    ('REINDEER', 'FATTINESS', 2),
    ('REINDEER', 'UMAMI', 4),
    ('NORTHERN_PRAWN', 'DOMINANCE', 3),
    ('NORTHERN_PRAWN', 'SWEETNESS', 2),
    ('NORTHERN_PRAWN', 'UMAMI', 4),
    ('NORTHERN_PRAWN', 'SALTINESS', 3),
    ('FLATBROD', 'DOMINANCE', 2),
    ('FLATBROD', 'SWEETNESS', 1),
    ('FLATBROD', 'SALTINESS', 1),
    ('LEFSE', 'DOMINANCE', 2),
    ('LEFSE', 'SWEETNESS', 1),
    ('NORWEGIAN_WAFFLE', 'DOMINANCE', 2),
    ('NORWEGIAN_WAFFLE', 'SWEETNESS', 3),
    ('NORWEGIAN_WAFFLE', 'FATTINESS', 3),
    ('FENALAR', 'DOMINANCE', 4),
    ('FENALAR', 'FATTINESS', 3),
    ('FENALAR', 'UMAMI', 5),
    ('FENALAR', 'SALTINESS', 5),
    ('PINNEKJOTT', 'DOMINANCE', 4),
    ('PINNEKJOTT', 'FATTINESS', 4),
    ('PINNEKJOTT', 'UMAMI', 5),
    ('PINNEKJOTT', 'SALTINESS', 5),
    ('RAKFISK', 'DOMINANCE', 5),
    ('RAKFISK', 'FATTINESS', 3),
    ('RAKFISK', 'UMAMI', 5),
    ('RAKFISK', 'SALTINESS', 4),
    ('LUTEFISK', 'DOMINANCE', 3),
    ('LUTEFISK', 'UMAMI', 3),
    ('LUTEFISK', 'SALTINESS', 2),
    ('MOOSE', 'DOMINANCE', 3),
    ('MOOSE', 'FATTINESS', 2),
    ('MOOSE', 'UMAMI', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('KLIPPFISH', 'CURED'),
    ('KLIPPFISH', 'DRIED'),
    ('FLATBROD', 'DRIED'),
    ('FENALAR', 'CURED'),
    ('FENALAR', 'DRIED'),
    ('PINNEKJOTT', 'CURED'),
    ('PINNEKJOTT', 'DRIED'),
    ('RAKFISK', 'FERMENTED'),
    ('RAKFISK', 'CURED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('BRUNOST', 'TOBIAS', 'DIFFICULT'),
    ('BRUNOST', 'GEORGIA', 'DIFFICULT'),
    ('KLIPPFISH', 'TOBIAS', 'DIFFICULT'),
    ('KLIPPFISH', 'GEORGIA', 'DIFFICULT'),
    ('CLOUDBERRY', 'TOBIAS', 'DIFFICULT'),
    ('CLOUDBERRY', 'GEORGIA', 'DIFFICULT'),
    ('CLOUDBERRY_PRESERVES', 'TOBIAS', 'PLANNED'),
    ('CLOUDBERRY_PRESERVES', 'GEORGIA', 'PLANNED'),
    ('REINDEER', 'TOBIAS', 'DIFFICULT'),
    ('REINDEER', 'GEORGIA', 'DIFFICULT'),
    ('NORTHERN_PRAWN', 'TOBIAS', 'PLANNED'),
    ('NORTHERN_PRAWN', 'GEORGIA', 'PLANNED'),
    ('FLATBROD', 'TOBIAS', 'PLANNED'),
    ('FLATBROD', 'GEORGIA', 'PLANNED'),
    ('LEFSE', 'TOBIAS', 'DIFFICULT'),
    ('LEFSE', 'GEORGIA', 'DIFFICULT'),
    ('NORWEGIAN_WAFFLE', 'TOBIAS', 'PLANNED'),
    ('NORWEGIAN_WAFFLE', 'GEORGIA', 'PLANNED'),
    ('FENALAR', 'TOBIAS', 'DIFFICULT'),
    ('FENALAR', 'GEORGIA', 'DIFFICULT'),
    ('PINNEKJOTT', 'TOBIAS', 'DIFFICULT'),
    ('PINNEKJOTT', 'GEORGIA', 'DIFFICULT'),
    ('RAKFISK', 'TOBIAS', 'DIFFICULT'),
    ('RAKFISK', 'GEORGIA', 'DIFFICULT'),
    ('LUTEFISK', 'TOBIAS', 'DIFFICULT'),
    ('LUTEFISK', 'GEORGIA', 'DIFFICULT'),
    ('MOOSE', 'TOBIAS', 'DIFFICULT'),
    ('MOOSE', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'NO'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'COD',
    'STOCKFISH',
    'HERRING',
    'PICKLED_HERRING',
    'SALMON',
    'SMOKED_SALMON',
    'GRAVLAX',
    'TROUT',
    'MACKEREL',
    'POLLOCK',
    'HALIBUT',
    'LAMB',
    'PORK_BELLY',
    'POTATO',
    'WHITE_CABBAGE',
    'RUTABAGA',
    'BARLEY',
    'OATS',
    'SOUR_CREAM',
    'LINGONBERRY',
    'LINGONBERRY_PRESERVES',
    'YELLOW_SPLIT_PEAS',
    'DILL',
    'CARAWAY',
    'CARDAMOM',
    'AQUAVIT',
    'HADDOCK',
    'VENISON',
    'BUTTERMILK',
    'CIDER',
    'CINNAMON',
    'BRUNOST',
    'KLIPPFISH',
    'CLOUDBERRY',
    'CLOUDBERRY_PRESERVES',
    'REINDEER',
    'NORTHERN_PRAWN',
    'FLATBROD',
    'LEFSE',
    'NORWEGIAN_WAFFLE',
    'FENALAR',
    'PINNEKJOTT',
    'RAKFISK',
    'LUTEFISK',
    'MOOSE'
);

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'SE'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'CLOUDBERRY',
    'CLOUDBERRY_PRESERVES',
    'REINDEER'
);
