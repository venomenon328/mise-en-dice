--liquibase formatted sql

--changeset venomenon328:031-vietnam-curation
-- Issue #172: country-by-country catalog curation pass (Vietnam / VN).
-- Adds only the explicitly approved Vietnamese associations, catalog gaps,
-- and the approved invertebrate/mollusc graph cleanup.

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
VALUES
    ('MAM_NEM', 'Mắm nêm', true, true, 'SPECIFIC', 0.2200, 5,
        'Kräftige vietnamesische Würzpaste aus salzfermentiertem, weitgehend unfiltriertem Fisch, häufig Sardellen. Handelsprodukte können bereits mit Ananas, Zucker, Chili oder Aromaten abgeschmeckt sein; nicht flüssige Fischsauce.'),
    ('VIETNAMESE_SOYBEAN_PASTE', 'Tương oder vietnamesische Sojabohnenpaste', true, true, 'OPEN', 0.3000, 4,
        'Vietnamesische fermentierte Würze aus Sojabohnen, häufig mit Reis beziehungsweise Klebreis und Salz; je nach regionaler Form dickflüssig bis pastös. Nicht mit flüssiger Sojasauce, Miso oder Hoisinsauce gleichsetzen.'),
    ('COM_ME', 'Mẻ oder Cơm mẻ', true, true, 'SPECIFIC', 0.1500, 5,
        'Milchsauer fermentierte vietnamesische Reiswürze mit breiig-cremiger Konsistenz und ausgeprägter, komplexer Säure. Als Säuerungs- und Würzmittel gemeint; nicht Reisessig, Reiswein oder fermentierter süßer Reis.'),
    ('MAM_TOM', 'Mắm tôm', true, true, 'SPECIFIC', 0.2200, 5,
        'Sehr kräftige vietnamesische fermentierte Garnelenpaste, besonders als Dip zu Bún đậu und als Würze zu Chả cá bekannt. Nicht mit Mắm ruốc gleichsetzen; regionale Produktbezeichnungen und Übergänge können sich jedoch überschneiden.'),
    ('MUOI_TOM', 'Muối tôm', true, true, 'SPECIFIC', 0.2500, 4,
        'Vietnamesische trockene Würzmischung aus Salz, Chili und getrockneten Garnelen, häufig zusätzlich mit Knoblauch, Zucker oder Zitronengras. Besonders als Dip für Obst und Gemüse sowie zum Würzen von Reispapiersnacks verwendet.'),
    ('BLACK_CARDAMOM', 'schwarzer Kardamom oder Thảo quả', true, true, 'SPECIFIC', 0.3000, 4,
        'Große dunkle Kardamomkapseln mit kräftig harzig-kampferartigem und häufig rauchigem Aroma; meist ganz in Brühen und Schmorgerichten mitgekocht. Nicht mit dem milderen grünen Kardamom gleichsetzen.'),
    ('BROKEN_RICE', 'Bruchreis', true, true, 'SPECIFIC', 0.5500, 2,
        'Reiskörner, die bei Ernte oder Verarbeitung gebrochen wurden und dadurch kleiner sowie schneller garend sind. Gemeint ist das Kornprodukt, nicht ein fertig belegtes Cơm-tấm-Gericht.'),
    ('GREEN_RICE_FLAKES', 'Cốm oder grüne Reisflocken', true, true, 'SPECIFIC', 0.1800, 5,
        'Aus jungen, noch grünen Klebreiskörnern geröstete und flachgestampfte vietnamesische Reisflocken mit zart nussigem, leicht süßlichem Aroma. Pur, in Süßspeisen, Kuchen oder als Panierung verwendbar; nicht gefärbte reife Reisflocken.'),
    ('DRIED_SHRIMP', 'getrocknete Garnelen', true, true, 'SPECIFIC', 0.4500, 3,
        'Durch Trocknung haltbar gemachte Garnelen, ganz oder grob zerkleinert; sie liefern konzentriertes Umami für Brühen, Füllungen, Toppings und Würzpasten. Nicht Garnelenpulver oder fermentierte Garnelenpaste.'),
    ('BANANA_LEAVES', 'Bananenblätter', true, true, 'SPECIFIC', 0.3000, 4,
        'Große Bananenblätter als aromatisierende Hülle oder Unterlage zum Dämpfen, Kochen und Grillen; frisch oder tiefgekühlt verwendbar. Sie werden nicht als Blattgemüse gegessen und sind keine Konkretisierung der Bananenfrucht.'),
    ('BANANA_BLOSSOM', 'Bananenblüte', true, true, 'SPECIFIC', 0.3500, 4,
        'Essbare Bananenblüte mit faserigem Biss und mild herb-adstringierendem Geschmack; frisch oder schlicht konserviert für Salate, Suppen und Gemüsegerichte. Nicht Bananenfrucht oder bloß dekorative Blüte.'),
    ('GAC_FRUIT', 'Gấc-Frucht', true, true, 'SPECIFIC', 0.1500, 5,
        'Reife Gấc-Frucht beziehungsweise ihr tief orange-rotes Fruchtfleisch um die Samen; vor allem zum Färben und mild-fruchtigen Aromatisieren von Klebreis und Festgebäck. Nicht Drachenfrucht oder bloße Lebensmittelfarbe.'),
    ('LOTUS_SEEDS', 'Lotussamen', true, true, 'SPECIFIC', 0.3500, 4,
        'Essbare reife Lotussamen mit mild-nussigem, leicht süßlichem Geschmack; frisch, getrocknet oder vorgegart für Suppen, Reisgerichte, Füllungen und Süßspeisen. Nicht Lotuswurzel oder der bittere Keim als eigenes Produkt.'),
    ('STARFRUIT', 'Sternfrucht oder Karambole', true, true, 'SPECIFIC', 0.4500, 3,
        'Saftig-knackige Sternfrucht mit je nach Reife deutlich säuerlichem bis mild süßem Geschmack; roh, in Salaten oder als säuernde Frucht in warmen Gerichten verwendbar.'),
    ('PERILLA_LEAVES', 'Perillablätter', true, true, 'SPECIFIC', 0.3000, 4,
        'Frische Perillablätter mit kräftig kräuterigem, leicht minzig-anisigem und teils erdigem Aroma; grüne und violette kulinarische Formen sind umfasst. Nicht vietnamesischer Koriander oder Minze.'),
    ('CULANTRO', 'Culantro oder Sägeblattkoriander', true, true, 'SPECIFIC', 0.2500, 5,
        'Langblättriges Würzkraut mit gezähntem Rand und kräftigem korianderähnlichem Aroma; roh oder in Suppen und Eintöpfen verwendbar. Botanisch und kulinarisch nicht mit Koriandergrün gleichsetzen.'),
    ('RICE_PADDY_HERB', 'Reisfeldkraut', true, true, 'SPECIFIC', 0.1800, 5,
        'Frisches Reisfeldkraut (ngò om beziehungsweise ngổ) mit zitrischem und leicht kümmelartigem Aroma; besonders für saure Suppen und Brühen. Nicht vietnamesischer Koriander oder Culantro.'),
    ('FISH_MINT', 'Fischminze', true, true, 'SPECIFIC', 0.1500, 5,
        'Frische Fischminzenblätter mit intensiv kräuterigem, zitrisch-fischigem Aroma; vor allem roh in Kräutertellern, Salaten und zu Fleisch- oder Suppengerichten. Trotz des Namens keine Minzart.'),
    ('LA_LOT_LEAVES', 'Lá-lốt-Blätter', true, true, 'SPECIFIC', 0.2000, 5,
        'Als Lá lốt gehandelte herzförmige Pfefferblätter mit warm-würzigem Aroma, besonders zum Umwickeln und Grillen von Fleischfüllungen. Nicht mit Betelblättern zum Kauen oder gewöhnlichem Pfefferblatt gleichsetzen.'),
    ('GIO_LUA', 'Giò lụa oder Chả lụa', true, true, 'SPECIFIC', 0.3000, 4,
        'Fein emulgierte vietnamesische Schweinewurst mit elastisch-glatter Textur, traditionell in Bananenblättern geformt und gedämpft. Gemeint ist die schlichte Giò-lụa-/Chả-lụa-Grundform, nicht gebratene Chả-Varianten oder grobe Sülzwurst.'),
    ('CHICKEN_FEET', 'Hühnerfüße', true, true, 'SPECIFIC', 0.3500, 4,
        'Küchenfertig gereinigte Hühnerfüße mit hohem Haut-, Sehnen- und Knorpelanteil; frisch oder tiefgekühlt verwendbar. Nicht bereits marinierte, eingelegte oder vollständig zubereitete Snackprodukte.'),
    ('INVERTEBRATES', 'wirbellose Tiere', true, false, 'OPEN', 1.0000, 1,
        'Übergeordnete Familie essbarer wirbelloser Tiere wie Weichtiere und Krebstiere; weitere kulinarisch eigenständige Gruppen wie Insekten oder Ringelwürmer können separat darunter eingeordnet werden.'),
    ('EDIBLE_SNAILS', 'essbare Schnecken', true, true, 'OPEN', 0.2500, 4,
        'Offene Familie essbarer Land-, Süßwasser- und Meeresschnecken als küchenfertige Zutat; Art und Lebensraum bleiben wählbar. Nicht Muscheln oder andere Weichtiergruppen.'),
    ('LAND_SNAILS', 'Landschnecken', true, false, 'OPEN', 1.0000, 4,
        'Essbare Landschnecken als küchenfertige Zutat; umfasst Weinbergschnecken und mögliche weitere terrestrische Arten. Nicht Süßwasser- oder Meeresschnecken.'),
    ('SEA_SNAILS', 'Meeresschnecken', true, true, 'OPEN', 0.1200, 5,
        'Offene Vorgabe für kulinarisch verwendete Meeresschnecken; Art, Größe und Zuschnitt bleiben wählbar. Nicht Muscheln, Tintenfische oder Landschnecken.'),
    ('FRESHWATER_SNAILS', 'Süßwasserschnecken', true, true, 'OPEN', 0.1200, 5,
        'Offene Vorgabe für essbare Süßwasserschnecken, ganz oder ausgelöst und küchenfertig gereinigt. Nicht Weinberg- oder Meeresschnecken und nicht bereits zubereitete Schneckensuppe.');

UPDATE ingredient_concept
SET curator_note = 'Aromatische Kardamomkapseln oder -samen; ohne weitere Konkretisierung ist grüner Kardamom gemeint. Schwarzer Kardamom besitzt ein deutlich herberes, häufig rauchiges Profil.'
WHERE code = 'CARDAMOM';

UPDATE ingredient_concept
SET curator_note = 'Vietnamesische fermentierte Garnelenpaste mit intensiv salzigem Aroma, meist sparsam als Würze verwendet. Nicht mit Mắm tôm gleichsetzen; regionale Produktbezeichnungen und Übergänge können sich jedoch überschneiden.'
WHERE code = 'MAM_RUOC';

UPDATE ingredient_concept
SET curator_note = 'Offene Vorgabe für essbare Weichtiere wie Muscheln, Schnecken, Oktopus oder Kalmar; aquatische und terrestrische Formen sind umfasst. Krebstiere, Insekten und Würmer sind nicht gemeint.'
WHERE code = 'MOLLUSCS';

UPDATE ingredient_concept
SET curator_note = 'Essbare Landschnecken nach Weinbergschnecken-/Escargot-Art, küchenfertig ausgelöst oder im Gehäuse; frisch, tiefgekühlt oder konserviert. Nicht Meer- oder Süßwasserschnecken.'
WHERE code = 'ESCARGOT';

UPDATE ingredient_availability availability
SET availability_level = 'PLANNED'
FROM ingredient_concept concept, participant
WHERE availability.ingredient_concept_id = concept.id
  AND availability.participant_id = participant.id
  AND concept.code = 'ESCARGOT'
  AND participant.code IN ('TOBIAS', 'GEORGIA');

-- MOLLUSCS is a biological family and must not live exclusively below SEAFOOD.
-- ESCARGOT is a land-snail specialization rather than a MEAT refinement.
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND (parent.code, child.code) IN (
      ('SEAFOOD', 'MOLLUSCS'),
      ('MEAT', 'ESCARGOT')
  );

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (VALUES
    ('FERMENTED_SEASONINGS', 'MAM_NEM'),
    ('READY_SAUCES_AND_PASTES', 'MAM_NEM'),
    ('FERMENTED_SEASONINGS', 'VIETNAMESE_SOYBEAN_PASTE'),
    ('READY_SAUCES_AND_PASTES', 'VIETNAMESE_SOYBEAN_PASTE'),
    ('SOY_PRODUCTS', 'VIETNAMESE_SOYBEAN_PASTE'),
    ('FERMENTED_SEASONINGS', 'COM_ME'),
    ('READY_SAUCES_AND_PASTES', 'COM_ME'),
    ('RICE_PRODUCTS', 'COM_ME'),
    ('SHRIMP_PASTE', 'MAM_TOM'),
    ('SPICE_BLENDS', 'MUOI_TOM'),
    ('CARDAMOM', 'BLACK_CARDAMOM'),
    ('RICE', 'BROKEN_RICE'),
    ('RICE_PRODUCTS', 'GREEN_RICE_FLAKES'),
    ('SHRIMP', 'DRIED_SHRIMP'),
    ('FRESH_HERBS', 'BANANA_LEAVES'),
    ('FLOWER_VEGETABLES', 'BANANA_BLOSSOM'),
    ('TROPICAL_FRUIT', 'GAC_FRUIT'),
    ('SEEDS', 'LOTUS_SEEDS'),
    ('TROPICAL_FRUIT', 'STARFRUIT'),
    ('FRESH_HERBS', 'PERILLA_LEAVES'),
    ('FRESH_HERBS', 'CULANTRO'),
    ('FRESH_HERBS', 'RICE_PADDY_HERB'),
    ('FRESH_HERBS', 'FISH_MINT'),
    ('FRESH_HERBS', 'LA_LOT_LEAVES'),
    ('SAUSAGE', 'GIO_LUA'),
    ('PORK', 'GIO_LUA'),
    ('CHICKEN', 'CHICKEN_FEET'),
    ('OFFAL', 'CHICKEN_FEET'),
    ('INVERTEBRATES', 'MOLLUSCS'),
    ('INVERTEBRATES', 'CRUSTACEANS'),
    ('MOLLUSCS', 'EDIBLE_SNAILS'),
    ('EDIBLE_SNAILS', 'LAND_SNAILS'),
    ('EDIBLE_SNAILS', 'FRESHWATER_SNAILS'),
    ('EDIBLE_SNAILS', 'SEA_SNAILS'),
    ('LAND_SNAILS', 'ESCARGOT'),
    ('SHELLFISH', 'FRESHWATER_SNAILS'),
    ('SHELLFISH', 'SEA_SNAILS'),
    ('SEAFOOD', 'CUTTLEFISH'),
    ('SEAFOOD', 'OCTOPUS'),
    ('SEAFOOD', 'SQUID')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('MAM_NEM', 'SEASONING'),
    ('VIETNAMESE_SOYBEAN_PASTE', 'SEASONING'),
    ('COM_ME', 'ACID'),
    ('COM_ME', 'SEASONING'),
    ('MAM_TOM', 'SEASONING'),
    ('MUOI_TOM', 'AROMATIC'),
    ('MUOI_TOM', 'SEASONING'),
    ('BLACK_CARDAMOM', 'AROMATIC'),
    ('BLACK_CARDAMOM', 'SEASONING'),
    ('BROKEN_RICE', 'STARCH'),
    ('GREEN_RICE_FLAKES', 'STARCH'),
    ('GREEN_RICE_FLAKES', 'AROMATIC'),
    ('DRIED_SHRIMP', 'ANIMAL_PROTEIN'),
    ('DRIED_SHRIMP', 'SEASONING'),
    ('BANANA_LEAVES', 'AROMATIC'),
    ('BANANA_BLOSSOM', 'VEGETABLE'),
    ('GAC_FRUIT', 'FRUIT'),
    ('LOTUS_SEEDS', 'PLANT_PROTEIN'),
    ('LOTUS_SEEDS', 'STARCH'),
    ('STARFRUIT', 'ACID'),
    ('STARFRUIT', 'FRUIT'),
    ('PERILLA_LEAVES', 'AROMATIC'),
    ('CULANTRO', 'AROMATIC'),
    ('RICE_PADDY_HERB', 'AROMATIC'),
    ('FISH_MINT', 'AROMATIC'),
    ('LA_LOT_LEAVES', 'AROMATIC'),
    ('GIO_LUA', 'ANIMAL_PROTEIN'),
    ('GIO_LUA', 'FAT'),
    ('CHICKEN_FEET', 'ANIMAL_PROTEIN'),
    ('INVERTEBRATES', 'ANIMAL_PROTEIN'),
    ('EDIBLE_SNAILS', 'ANIMAL_PROTEIN'),
    ('LAND_SNAILS', 'ANIMAL_PROTEIN'),
    ('SEA_SNAILS', 'ANIMAL_PROTEIN'),
    ('FRESHWATER_SNAILS', 'ANIMAL_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('MAM_NEM', 'DOMINANCE', 5),
    ('MAM_NEM', 'SALTINESS', 5),
    ('MAM_NEM', 'UMAMI', 5),
    ('VIETNAMESE_SOYBEAN_PASTE', 'DOMINANCE', 4),
    ('VIETNAMESE_SOYBEAN_PASTE', 'SWEETNESS', 2),
    ('VIETNAMESE_SOYBEAN_PASTE', 'SALTINESS', 4),
    ('VIETNAMESE_SOYBEAN_PASTE', 'UMAMI', 4),
    ('COM_ME', 'DOMINANCE', 4),
    ('COM_ME', 'ACIDITY', 5),
    ('COM_ME', 'UMAMI', 2),
    ('MAM_TOM', 'DOMINANCE', 5),
    ('MAM_TOM', 'SALTINESS', 5),
    ('MAM_TOM', 'UMAMI', 5),
    ('MUOI_TOM', 'DOMINANCE', 5),
    ('MUOI_TOM', 'SWEETNESS', 2),
    ('MUOI_TOM', 'HEAT', 3),
    ('MUOI_TOM', 'SALTINESS', 5),
    ('MUOI_TOM', 'UMAMI', 4),
    ('BLACK_CARDAMOM', 'DOMINANCE', 5),
    ('BLACK_CARDAMOM', 'BITTERNESS', 2),
    ('BROKEN_RICE', 'DOMINANCE', 2),
    ('GREEN_RICE_FLAKES', 'DOMINANCE', 3),
    ('GREEN_RICE_FLAKES', 'SWEETNESS', 2),
    ('DRIED_SHRIMP', 'DOMINANCE', 5),
    ('DRIED_SHRIMP', 'SALTINESS', 4),
    ('DRIED_SHRIMP', 'UMAMI', 5),
    ('BANANA_LEAVES', 'DOMINANCE', 2),
    ('BANANA_BLOSSOM', 'DOMINANCE', 3),
    ('BANANA_BLOSSOM', 'SWEETNESS', 1),
    ('BANANA_BLOSSOM', 'BITTERNESS', 2),
    ('GAC_FRUIT', 'DOMINANCE', 3),
    ('GAC_FRUIT', 'SWEETNESS', 2),
    ('GAC_FRUIT', 'FATTINESS', 2),
    ('LOTUS_SEEDS', 'DOMINANCE', 2),
    ('LOTUS_SEEDS', 'SWEETNESS', 2),
    ('LOTUS_SEEDS', 'BITTERNESS', 1),
    ('STARFRUIT', 'DOMINANCE', 3),
    ('STARFRUIT', 'SWEETNESS', 2),
    ('STARFRUIT', 'ACIDITY', 4),
    ('PERILLA_LEAVES', 'DOMINANCE', 4),
    ('PERILLA_LEAVES', 'BITTERNESS', 2),
    ('CULANTRO', 'DOMINANCE', 5),
    ('CULANTRO', 'BITTERNESS', 2),
    ('RICE_PADDY_HERB', 'DOMINANCE', 5),
    ('FISH_MINT', 'DOMINANCE', 5),
    ('FISH_MINT', 'BITTERNESS', 2),
    ('LA_LOT_LEAVES', 'DOMINANCE', 4),
    ('LA_LOT_LEAVES', 'BITTERNESS', 2),
    ('GIO_LUA', 'DOMINANCE', 3),
    ('GIO_LUA', 'FATTINESS', 3),
    ('GIO_LUA', 'SALTINESS', 3),
    ('GIO_LUA', 'UMAMI', 4),
    ('CHICKEN_FEET', 'DOMINANCE', 2),
    ('CHICKEN_FEET', 'FATTINESS', 2),
    ('CHICKEN_FEET', 'UMAMI', 3),
    ('EDIBLE_SNAILS', 'DOMINANCE', 3),
    ('EDIBLE_SNAILS', 'UMAMI', 3),
    ('SEA_SNAILS', 'DOMINANCE', 3),
    ('SEA_SNAILS', 'SALTINESS', 2),
    ('SEA_SNAILS', 'UMAMI', 4),
    ('FRESHWATER_SNAILS', 'DOMINANCE', 3),
    ('FRESHWATER_SNAILS', 'UMAMI', 3)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension
  ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('MAM_NEM', 'FERMENTED'),
    ('VIETNAMESE_SOYBEAN_PASTE', 'FERMENTED'),
    ('COM_ME', 'FERMENTED'),
    ('MAM_TOM', 'FERMENTED'),
    ('DRIED_SHRIMP', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN culinary_flag flag
  ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level)
SELECT concept.id, participant.id, assignment.availability_level
FROM (VALUES
    ('MAM_NEM', 'TOBIAS', 'DIFFICULT'),
    ('MAM_NEM', 'GEORGIA', 'DIFFICULT'),
    ('VIETNAMESE_SOYBEAN_PASTE', 'TOBIAS', 'DIFFICULT'),
    ('VIETNAMESE_SOYBEAN_PASTE', 'GEORGIA', 'PLANNED'),
    ('COM_ME', 'TOBIAS', 'DIFFICULT'),
    ('COM_ME', 'GEORGIA', 'DIFFICULT'),
    ('MAM_TOM', 'TOBIAS', 'DIFFICULT'),
    ('MAM_TOM', 'GEORGIA', 'PLANNED'),
    ('MUOI_TOM', 'TOBIAS', 'PLANNED'),
    ('MUOI_TOM', 'GEORGIA', 'PLANNED'),
    ('BLACK_CARDAMOM', 'TOBIAS', 'PLANNED'),
    ('BLACK_CARDAMOM', 'GEORGIA', 'PLANNED'),
    ('BROKEN_RICE', 'TOBIAS', 'PLANNED'),
    ('BROKEN_RICE', 'GEORGIA', 'PLANNED'),
    ('GREEN_RICE_FLAKES', 'TOBIAS', 'DIFFICULT'),
    ('GREEN_RICE_FLAKES', 'GEORGIA', 'DIFFICULT'),
    ('DRIED_SHRIMP', 'TOBIAS', 'PLANNED'),
    ('DRIED_SHRIMP', 'GEORGIA', 'EASY'),
    ('BANANA_LEAVES', 'TOBIAS', 'DIFFICULT'),
    ('BANANA_LEAVES', 'GEORGIA', 'PLANNED'),
    ('BANANA_BLOSSOM', 'TOBIAS', 'DIFFICULT'),
    ('BANANA_BLOSSOM', 'GEORGIA', 'PLANNED'),
    ('GAC_FRUIT', 'TOBIAS', 'DIFFICULT'),
    ('GAC_FRUIT', 'GEORGIA', 'DIFFICULT'),
    ('LOTUS_SEEDS', 'TOBIAS', 'PLANNED'),
    ('LOTUS_SEEDS', 'GEORGIA', 'PLANNED'),
    ('STARFRUIT', 'TOBIAS', 'PLANNED'),
    ('STARFRUIT', 'GEORGIA', 'EASY'),
    ('PERILLA_LEAVES', 'TOBIAS', 'DIFFICULT'),
    ('PERILLA_LEAVES', 'GEORGIA', 'PLANNED'),
    ('CULANTRO', 'TOBIAS', 'DIFFICULT'),
    ('CULANTRO', 'GEORGIA', 'PLANNED'),
    ('RICE_PADDY_HERB', 'TOBIAS', 'DIFFICULT'),
    ('RICE_PADDY_HERB', 'GEORGIA', 'DIFFICULT'),
    ('FISH_MINT', 'TOBIAS', 'DIFFICULT'),
    ('FISH_MINT', 'GEORGIA', 'DIFFICULT'),
    ('LA_LOT_LEAVES', 'TOBIAS', 'DIFFICULT'),
    ('LA_LOT_LEAVES', 'GEORGIA', 'DIFFICULT'),
    ('GIO_LUA', 'TOBIAS', 'DIFFICULT'),
    ('GIO_LUA', 'GEORGIA', 'PLANNED'),
    ('CHICKEN_FEET', 'TOBIAS', 'PLANNED'),
    ('CHICKEN_FEET', 'GEORGIA', 'PLANNED'),
    ('INVERTEBRATES', 'TOBIAS', 'EASY'),
    ('INVERTEBRATES', 'GEORGIA', 'EASY'),
    ('EDIBLE_SNAILS', 'TOBIAS', 'PLANNED'),
    ('EDIBLE_SNAILS', 'GEORGIA', 'PLANNED'),
    ('LAND_SNAILS', 'TOBIAS', 'PLANNED'),
    ('LAND_SNAILS', 'GEORGIA', 'PLANNED'),
    ('SEA_SNAILS', 'TOBIAS', 'DIFFICULT'),
    ('SEA_SNAILS', 'GEORGIA', 'DIFFICULT'),
    ('FRESHWATER_SNAILS', 'TOBIAS', 'DIFFICULT'),
    ('FRESHWATER_SNAILS', 'GEORGIA', 'DIFFICULT')
) AS assignment(concept_code, participant_code, availability_level)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN participant
  ON participant.code = assignment.participant_code;

-- Cross-cutting restrictions for open concepts that straddle land and water.
-- LAND_SNAILS carries the meat restriction transitively to ESCARGOT.
INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, assignment.include_refinements
FROM (VALUES
    ('NO_FISH_OR_SEAFOOD', 'MAM_NEM', false),
    ('NO_CHILI', 'MUOI_TOM', false),
    ('NO_FISH_OR_SEAFOOD', 'MUOI_TOM', false),
    ('NO_MEAT', 'MOLLUSCS', false),
    ('NO_FISH_OR_SEAFOOD', 'MOLLUSCS', false),
    ('NO_MEAT', 'EDIBLE_SNAILS', false),
    ('NO_FISH_OR_SEAFOOD', 'EDIBLE_SNAILS', false),
    ('NO_MEAT', 'LAND_SNAILS', true)
) AS assignment(rule_code, concept_code, include_refinements)
JOIN exclusion_rule rule
  ON rule.code = assignment.rule_code
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT ingredient.id, 'VN'
FROM ingredient_concept ingredient
WHERE ingredient.code IN (
    'FISH_SAUCE',
    'SHRIMP_PASTE',
    'MAM_RUOC',
    'SOY_SAUCE',
    'OYSTER_SAUCE',
    'SRIRACHA',
    'RICE',
    'JASMINE_RICE',
    'STICKY_RICE',
    'NOODLES',
    'RICE_NOODLES',
    'GLASS_NOODLES',
    'RICE_FLOUR',
    'RICE_PAPER',
    'RICE_VINEGAR',
    'BAGUETTE',
    'LIVER_PATE',
    'GARLIC',
    'SHALLOT',
    'SPRING_ONION',
    'GINGER',
    'CHILI',
    'LIME',
    'LEMONGRASS',
    'TAMARIND',
    'TURMERIC',
    'STAR_ANISE',
    'CINNAMON',
    'CILANTRO',
    'MINT',
    'THAI_BASIL',
    'VIETNAMESE_CORIANDER',
    'WATER_SPINACH',
    'BEAN_SPROUTS',
    'DAIKON',
    'GREEN_PAPAYA',
    'BITTER_MELON',
    'BAMBOO_SHOOTS',
    'TOMATO',
    'WOOD_EAR',
    'PANDAN_LEAVES',
    'MUNG_BEANS',
    'PEANUT',
    'MANGO',
    'POMELO',
    'LOTUS_ROOT',
    'COCONUT',
    'COCONUT_MILK',
    'COCONUT_WATER',
    'FISH',
    'SHRIMP',
    'CRAB',
    'SQUID',
    'DRIED_FISH',
    'PORK',
    'PORK_BELLY',
    'PORK_MINCE',
    'BEEF',
    'BEEF_BRISKET',
    'BEEF_SHIN',
    'CHICKEN',
    'DUCK',
    'SALTED_DUCK_EGG',
    'TOFU',
    'COFFEE',
    'CONDENSED_MILK',
    'GREEN_TEA',
    'MAM_NEM',
    'VIETNAMESE_SOYBEAN_PASTE',
    'COM_ME',
    'MAM_TOM',
    'MUOI_TOM',
    'BLACK_CARDAMOM',
    'BROKEN_RICE',
    'GREEN_RICE_FLAKES',
    'DRIED_SHRIMP',
    'BANANA_LEAVES',
    'BANANA_BLOSSOM',
    'GAC_FRUIT',
    'LOTUS_SEEDS',
    'STARFRUIT',
    'PERILLA_LEAVES',
    'CULANTRO',
    'RICE_PADDY_HERB',
    'FISH_MINT',
    'LA_LOT_LEAVES',
    'GIO_LUA',
    'CHICKEN_FEET',
    'EDIBLE_SNAILS',
    'SEA_SNAILS',
    'FRESHWATER_SNAILS'
);
