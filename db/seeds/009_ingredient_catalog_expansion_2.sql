-- Mise en Dice - extensive ingredient catalog expansion, part 2 of 3
-- Requires: db/migrations/001_catalog_schema.sql
-- Requires: db/seeds/001_reference_data.sql
-- Requires: db/seeds/002_ingredient_catalog.sql
--
-- Each compact manifest keeps concept, role, availability and culinary
-- metadata together. Hierarchy edges are inserted after all three parts.
-- Re-running the seed never overwrites existing curated rows.
--
-- Manifest fields:
-- code | display name | specificity (O/S) | weight | novelty | note |
-- roles | Tobias availability | Georgia availability | flags | dimensions
--
-- Role tokens: A animal protein, P plant protein, V vegetable, R fruit,
-- S starch, F fat, C acid, O aromatic, W seasoning.
-- Availability tokens: E easy, P planned, D difficult, U unavailable.
-- Flag tokens: F fermented, P pickled, S smoked, C cured, D dried.
-- Dimension tokens: D dominance, S sweetness, A acidity, B bitterness,
-- F fattiness, H heat, U umami; the following digit is the level.

DO $seed$
DECLARE
    manifest text := $catalog$
FIRM_TOFU|fester Tofu|S|0.9000|1||P|E|E||D1,U2
FISH_ROE|Fischrogen|O|0.3000|3||A,F,W|E|E||
FISH_STOCK|Fischfond|S|0.4000|3||A,W|P|P||D4,U5
FIVE_SPICE|Fünf-Gewürze-Pulver|S|0.4000|3||O,W|P|P||D5,S2
FLATBREAD|Fladenbrot|S|0.7000|1||S|E|E||D2
FLAXSEED|Leinsamen|S|0.5500|1||P,F|E|E||F4
FLOUR|Mehl|O|0.5500|1||S|E|E||
FREEKEH|Freekeh|S|0.3500|4||S|D|P|S|D4,U2
FRESH_DAIRY_PRODUCTS|frisches Milchprodukt|O|0.5000|1||F|E|E||
FRESH_TURMERIC|frischer Kurkuma|S|0.4000|3||O,W|P|P||B2,D4
FROG_LEGS|Froschschenkel|S|0.1200|5|Spezialzutat; bewusst sehr selten gewichtet.|A|D|D||D2,U2
FRUIT|Obst|O|0.4500|1||R|E|E||
FRUIT_VEGETABLES|Fruchtgemüse|O|0.6000|1||V|E|E||
GALANGAL|Galgant|S|0.4500|3||O,W|P|P||D5,H2
GAME_MEAT|Wildfleisch|O|0.4500|3||A|E|E||
GARAM_MASALA|Garam Masala|S|0.5000|2||O,W|E|E||D5
GARLIC_CHIVES|Knoblauch-Schnittlauch|S|0.3500|4||V,O|D|P||D4
GHEE|Ghee|S|0.5500|2||F|E|E||D3,F5
GLASS_NOODLES|Glasnudeln|S|0.7000|2||S|E|E||D1
GNOCCHI|Gnocchi|S|0.7500|1||S|E|E||D2
GOAT|Ziegenfleisch|S|0.3000|4||A|D|P||D4,U4
GOAT_CHEESE|Ziegenkäse|S|0.6500|2||F,A,W|E|E|F|D4,F4,U4
GOCHUGARU|Gochugaru|S|0.5000|3||O,W|P|E|D|D4,H3,S2
GOOSE|Gans|S|0.3500|3||A,F|P|P||D4,F5,U4
GOOSEBERRY|Stachelbeere|S|0.4500|2||R,C|P|P||A4,S2
GOUDA|Gouda|S|0.7500|1||F,A,W|E|E|F,C|D3,F4,U4
GRAINS|Getreide oder Pseudogetreide|O|0.5500|1||S|E|E||
GREEK_YOGURT|griechischer Joghurt|S|0.8000|1||F,W|E|E|F|A3,F4
GREEN_ASPARAGUS|grüner Spargel|S|0.6500|1||V|P|P||B2,S2
GREEN_PEAS|grüne Erbsen|S|0.9000|1||P,V,S|E|E||S3
GREEN_TEA|grüner Tee|S|0.3500|2||O,W|E|E|D|B4,D4,U2
GRUYERE|Gruyère|S|0.5000|2||F,A,W|P|E|F,C|D4,F4,U5
GUAVA|Guave|S|0.3500|4||R|D|P||A2,D4,S3
HADDOCK|Schellfisch|S|0.8000|1||A|P|P||U3
HAKE|Seehecht|S|0.8000|1||A|P|P||U3
HALIBUT|Heilbutt|S|0.6500|2||A,F|P|P||F3,U3
HALLOUMI|Halloumi|S|0.7500|1||F,A|E|E||F4,U4
HARISSA|Harissa|S|0.5000|2||W|E|E||D5,H4,U2
HAZELNUT|Haselnüsse|S|0.6500|1||P,F|E|E||D3,F4
HEMP_SEEDS|Hanfsamen|S|0.4000|3||P,F|P|P||F4
HERRING|Hering|S|0.7500|1||A,F|E|E||F4,U4
HIJIKI|Hijiki|S|0.2500|4||W,V|D|P|D|D4,U4
HOISIN_SAUCE|Hoisinsauce|S|0.5000|2||W|E|E|F|D4,S4,U4
HOKKAIDO_SQUASH|Hokkaido-Kürbis|S|0.8000|1||V,S|E|E||S3
HONEYDEW_MELON|Honigmelone|S|0.6500|1||R|E|E||D3,S4
HOT_MUSTARD|scharfer Senf|S|0.5000|1||C,W|E|E||A3,D5,H4
JACKFRUIT|Jackfruit|S|0.5500|3|Gemeint ist je nach Gericht junge grüne oder reife Jackfruit; für herzhafte Anwendungen meist junge Jackfruit.|R,V,S|P|P||D2,S2
JASMINE_RICE|Jasminreis|S|0.8500|1||S|E|E||D2
JERK_SEASONING|Jerk-Gewürzmischung|S|0.3500|4||O,W|P|P||D5,H3
JERUSALEM_ARTICHOKE|Topinambur|S|0.5000|3||V,S|P|P||S2
KAFFIR_LIME_LEAVES|Kaffirlimettenblätter|S|0.4500|3||O|P|P||D5
KECAP_MANIS|Kecap Manis|S|0.5500|2||W|E|E|F|D4,S5,U4
KEFIR|Kefir|S|0.5500|2||F,C|E|E|F|A4,F2
KETCHUP|Ketchup|S|0.4500|1||V,C,W|E|E||A3,D4,S4,U3
KING_OYSTER_MUSHROOM|Kräuterseitlinge|S|0.7000|2||V|E|E||D2,U4
KIWI|Kiwi|S|0.7500|1||R,C|E|E||A4,S3
KOHLRABI|Kohlrabi|S|0.8500|1||V|E|E||S2
KOMBU|Kombu|S|0.4500|3||W|P|P|D|D4,U5
LAKSA_PASTE|Laksa-Paste|S|0.3000|4||O,W|P|P||D5,H3,U4
LAMBS_LETTUCE|Feldsalat|S|0.7000|1||V|E|E||B1
LARD|Schmalz|S|0.5000|2||F,A|E|E||D3,F5,U3
LASAGNE_SHEETS|Lasagneplatten|S|0.6500|1||S|E|E||D2
LETTUCE|Kopfsalat|S|0.8000|1||V|E|E||B1
LIGHT_SOY_SAUCE|helle Sojasauce|S|0.5500|1||W|E|E|F|D4,U5
LOBSTER|Hummer|S|0.1800|5|Bewusst selten gewichtete Luxus- und Spezialzutat.|A|D|D||S3,U4
LOTUS_ROOT|Lotuswurzel|S|0.3500|4||V,S|D|P||S2
LOVAGE|Liebstöckel|S|0.4000|3||O|P|P||D5,U2
LUPIN|Lupinen|S|0.3500|3||P,S|P|P||B2
LYCHEE|Litschi|S|0.5500|2||R|P|E||D3,S4
MACADAMIA|Macadamianüsse|S|0.4500|2||P,F|E|E||F5,S2
MAITAKE|Maitake|S|0.3000|4||V|D|P||D4,U5
MALT_VINEGAR|Malzessig|S|0.4000|3||C,W|P|P||A5,D4
MAM_RUOC|Mắm ruốc|S|0.2200|5||A,W|D|D|F|D5,U5
MANDARIN|Mandarine|S|0.7500|1||R,C|E|E||A3,S4
MAPLE_SYRUP|Ahornsirup|S|0.5000|1||W|E|E||D4,S5
MARJORAM|Majoran|S|0.5000|1||O|E|E||D4
MASA_HARINA|Masa Harina|S|0.3500|4||S|D|P||D3
MASCARPONE|Mascarpone|S|0.6000|1||F|E|E||F5,S2
MASSAMAN_CURRY_PASTE|Massaman-Currypaste|S|0.3500|3||O,W|P|P||D5,H2,S2,U3
MATCHA|Matcha|S|0.3000|3||O,W|E|E|D|B4,D5,U3
MAYONNAISE|Mayonnaise|S|0.5500|1||F,C,W|E|E||A2,D3,F5
MEAT|Fleisch|O|0.5500|1||A|E|E||
MELONS|Melone|O|0.5000|1||R|E|E||
MERGUEZ|Merguez|S|0.4500|3||A,F,W|P|P||D4,F4,H2,U4
MILK|Milch|S|0.7000|1||F|E|E||F2,S2
MILLET|Hirse|S|0.6000|2||S|E|E||D2
MINCED_MEAT|Hackfleisch|O|0.7000|1||A|E|E||
MIRABELLE|Mirabelle|S|0.4000|2||R|P|P||A2,S4
MIRIN|Mirin|S|0.4500|2||W|P|P||D3,S4,U2
MOLASSES|Melasse|S|0.3500|3||W|P|P||B3,D5,S5
MOLE_PASTE|Mole-Paste|S|0.2500|5||F,W|D|P||B3,D5,H2,S2,U5
MOLLUSCS|Weichtiere|O|0.5500|2||A|E|E||
MONKFISH|Seeteufel|S|0.4500|4||A|D|P||D2,U3
MOREL|Morcheln|S|0.1800|5||V,W|D|D||D5,U5
MOZZARELLA|Mozzarella|S|0.8500|1||F,A|E|E|F|F3,U3
MSG|Mononatriumglutamat|S|0.3500|2||W|E|E||D2,U5
MULBERRY|Maulbeeren|S|0.2500|4||R|D|P||S4
MUNG_BEANS|Mungbohnen|S|0.6000|2||P,S|P|P||U2
MUSTARD_SEED|Senfsaat|S|0.5000|2||O,W|E|E||B2,D4,H2
MYCOPROTEIN|Mykoprotein|S|0.4500|3|Pilzproteinbasierte Fleischalternative; konkrete Markenprodukte werden nicht getrennt modelliert.|P|P|P||U2
NATTO|Nattō|S|0.2000|5||P,W|D|P|F|D5,U5
NDUJA|’Nduja|S|0.3000|4||A,F,W|D|P|C|D5,F5,H4,U5
NECTARINE|Nektarine|S|0.6000|1||R|E|E||A2,S4
NEUTRAL_OIL|neutrales Pflanzenöl|S|0.5500|1||F|E|E||D1,F5
NUTMEG|Muskatnuss|S|0.5000|1||O,W|E|E||B2,D5
NUTRITIONAL_YEAST|Hefeflocken|S|0.5500|2||P,W|E|E|D|D4,U5
NUT_AND_SEED_PASTES|Nuss- oder Samenpaste|O|0.4500|2||P,F,W|E|E||
OATS|Hafer|S|0.7000|1||S|E|E||D2,S2
OCTOPUS|Oktopus|S|0.4500|4||A|D|P||D3,U3
OFFAL|Innereien|O|0.3500|3||A|E|E||
OILS|Speiseöl|O|0.4000|1||F|E|E||
OLIVE_OIL|Olivenöl|S|0.6500|1||F,W|E|E||B2,D3,F5
OREGANO|Oregano|S|0.6500|1||O|E|E||B2,D4
ORZO|Kritharaki oder Orzo|S|0.6500|2||S|E|E||D2
OXTAIL|Ochsenschwanz|S|0.3500|4||A,F|D|P||F4,U5
OYSTER|Austern|S|0.2500|5||A|D|D||D4,U5
PALM_SUGAR|Palmzucker|S|0.4500|2||W|P|E||D3,S5
PANCETTA|Pancetta|S|0.4500|3||A,F,W|P|P|C|D4,F5,U5
PANEER|Paneer|S|0.6000|2||F,A|P|E||D1,F3
PANGASIUS|Pangasius|S|0.7500|1||A|E|E||U2
PAPAYA|Papaya|S|0.5500|2||R|P|E||D2,S3
PAPRIKA_POWDER|Paprikapulver|S|0.6500|1||O,W|E|E||D3,S2
PEARL_BARLEY|Graupen|S|0.6500|1||S|E|E||D2
PEAS|Erbsen|O|0.6000|1||P,V|E|E||
PECAN|Pekannüsse|S|0.5500|2||P,F|E|E||F5,S2
PECORINO|Pecorino|S|0.5500|2||F,A,W|E|E|F,C|D4,F4,U5
PENNE|Penne|S|0.8000|1||S|E|E||D2
PERSIMMON|Kaki|S|0.5500|2||R|E|E||D3,S4
PESTO|Pesto|S|0.5500|1||F,O,W|E|E||D5,F4,U4
PICKLED_CUCUMBER|Gewürzgurke|S|0.7000|1||V,C,W|E|E|P|A4,D3,S2
PICKLED_GINGER|eingelegter Ingwer|S|0.4500|2||O,C,W|E|E|P|A3,D4,H2,S3
PIKEPERCH|Zander|S|0.6000|2||A|P|P||U3
PINE_NUT|Pinienkerne|S|0.5000|2||P,F|E|E||F5,S2
PINTO_BEANS|Pintobohnen|S|0.6500|2||P,S|P|P||U2
PISTACHIO|Pistazien|S|0.6000|1||P,F|E|E||F4,S2
PITA|Pita|S|0.7000|1||S|E|E||D2
PLAICE|Scholle|S|0.7000|2||A|P|P||U3
PLANTAIN|Kochbanane|S|0.6000|2||R,S|P|E||D3,S3
PLANT_PROTEIN_PRODUCTS|pflanzliches Proteinprodukt|O|0.4500|2||P|E|E||
POBLANO|Poblano|S|0.3000|4||O,V,W|D|P||D3,H2
POD_VEGETABLES|Hülsen- und Schotengemüse|O|0.5500|1||V|E|E||
POMEGRANATE_MOLASSES|Granatapfelmelasse|S|0.4500|3||R,C,W|P|P||A4,D5,S4
POMELO|Pomelo|S|0.5500|2||R,C|E|E||A3,B2,S3
POME_FRUIT|Kernobst|O|0.6000|1||R|E|E||
PONZU|Ponzu|S|0.4500|3||C,W|P|P||A4,D4,U4
POPPY_SEEDS|Mohn|S|0.5500|1||P,F,W|E|E||D3,F4
PORCINI|Steinpilze|S|0.5500|2||V,W|P|P||D4,U5
PORK|Schweinefleisch|O|0.7500|1||A|E|E||
PORK_CHEEK|Schweinebäckchen|S|0.4500|3||A,F|D|P||F4,U4
PORK_CHOP|Schweinekotelett|S|0.8500|1||A|E|E||F3,U3
PORK_CUTLET|Schweineschnitzel|S|0.8500|1||A|E|E||F2,U3
PORK_KNUCKLE|Schweinshaxe|S|0.6000|2||A,F|P|P||D3,F4,U4
PORK_LIVER|Schweineleber|S|0.3500|3||A|P|P||D5,U4
PORK_LOIN|Schweinerücken|S|0.8500|1||A|E|E||F2,U3
PORK_MINCE|Schweinehack|S|0.8000|1||A,F|E|E||F4,U3
PORK_NECK|Schweinenacken|S|0.8500|1||A,F|E|E||F4,U3
PORK_RIBS|Schweinerippchen|S|0.7500|2||A,F|E|E||F4,U4
PORK_SHOULDER|Schweineschulter|S|0.9000|1||A,F|E|E||F4,U3
PORK_TONGUE|Schweinezunge|S|0.2000|5||A,F|D|D||D3,F3,U4
POTATO_STARCH|Kartoffelstärke|S|0.6000|1||S|E|E||D1
PRESERVED_FISH|konservierter Fisch|O|0.3500|2||A,W|E|E||
PRESERVED_LEMON|Salzzitrone|S|0.4500|3||R,C,W|P|P|P,F|A4,B2,D5,U3
$catalog$;
BEGIN
    DROP TABLE IF EXISTS _ingredient_catalog_raw;
    DROP TABLE IF EXISTS _ingredient_catalog_expansion;

    CREATE TEMP TABLE _ingredient_catalog_raw (
        line text NOT NULL
    );

    INSERT INTO _ingredient_catalog_raw (line)
    SELECT source.line
    FROM regexp_split_to_table(manifest, E'\n') AS source(line)
    WHERE source.line <> '';

    CREATE TEMP TABLE _ingredient_catalog_expansion AS
    SELECT
        split_part(line, '|', 1) AS code,
        split_part(line, '|', 2) AS display_name,
        CASE split_part(line, '|', 3)
            WHEN 'O' THEN 'OPEN'
            WHEN 'S' THEN 'SPECIFIC'
        END AS challenge_specificity,
        split_part(line, '|', 4)::numeric(10, 4) AS base_draw_weight,
        nullif(split_part(line, '|', 5), '')::smallint AS novelty_level,
        nullif(split_part(line, '|', 6), '') AS curator_note,
        CASE
            WHEN split_part(line, '|', 7) = '' THEN ARRAY[]::text[]
            ELSE string_to_array(split_part(line, '|', 7), ',')
        END AS role_tokens,
        split_part(line, '|', 8) AS availability_tobias_token,
        split_part(line, '|', 9) AS availability_georgia_token,
        CASE
            WHEN split_part(line, '|', 10) = '' THEN ARRAY[]::text[]
            ELSE string_to_array(split_part(line, '|', 10), ',')
        END AS flag_tokens,
        CASE
            WHEN split_part(line, '|', 11) = '' THEN ARRAY[]::text[]
            ELSE string_to_array(split_part(line, '|', 11), ',')
        END AS dimension_tokens
    FROM _ingredient_catalog_raw;

    INSERT INTO ingredient_concept (
        code,
        display_name,
        active,
        random_draw_enabled,
        challenge_specificity,
        base_draw_weight,
        novelty_level,
        curator_note
    )
    SELECT
        source.code,
        source.display_name,
        true,
        true,
        source.challenge_specificity,
        source.base_draw_weight,
        source.novelty_level,
        source.curator_note
    FROM _ingredient_catalog_expansion source
    ON CONFLICT (code) DO NOTHING;

    INSERT INTO ingredient_functional_role (
        ingredient_concept_id,
        functional_role_id
    )
    SELECT ic.id, fr.id
    FROM _ingredient_catalog_expansion source
    CROSS JOIN LATERAL unnest(source.role_tokens) AS role(role_token)
    JOIN ingredient_concept ic ON ic.code = source.code
    JOIN functional_role fr
      ON fr.code = CASE role.role_token
          WHEN 'A' THEN 'ANIMAL_PROTEIN'
          WHEN 'P' THEN 'PLANT_PROTEIN'
          WHEN 'V' THEN 'VEGETABLE'
          WHEN 'R' THEN 'FRUIT'
          WHEN 'S' THEN 'STARCH'
          WHEN 'F' THEN 'FAT'
          WHEN 'C' THEN 'ACID'
          WHEN 'O' THEN 'AROMATIC'
          WHEN 'W' THEN 'SEASONING'
      END
    ON CONFLICT (ingredient_concept_id, functional_role_id) DO NOTHING;

    INSERT INTO ingredient_availability (
        ingredient_concept_id,
        participant_id,
        availability_level
    )
    SELECT
        ic.id,
        p.id,
        CASE p.code
            WHEN 'TOBIAS' THEN
                CASE source.availability_tobias_token
                    WHEN 'E' THEN 'EASY'
                    WHEN 'P' THEN 'PLANNED'
                    WHEN 'D' THEN 'DIFFICULT'
                    WHEN 'U' THEN 'UNAVAILABLE'
                END
            WHEN 'GEORGIA' THEN
                CASE source.availability_georgia_token
                    WHEN 'E' THEN 'EASY'
                    WHEN 'P' THEN 'PLANNED'
                    WHEN 'D' THEN 'DIFFICULT'
                    WHEN 'U' THEN 'UNAVAILABLE'
                END
        END
    FROM _ingredient_catalog_expansion source
    JOIN ingredient_concept ic ON ic.code = source.code
    CROSS JOIN participant p
    WHERE p.code IN ('TOBIAS', 'GEORGIA')
    ON CONFLICT (ingredient_concept_id, participant_id) DO NOTHING;

    INSERT INTO ingredient_culinary_flag (
        ingredient_concept_id,
        culinary_flag_id
    )
    SELECT ic.id, cf.id
    FROM _ingredient_catalog_expansion source
    CROSS JOIN LATERAL unnest(source.flag_tokens) AS flag(flag_token)
    JOIN ingredient_concept ic ON ic.code = source.code
    JOIN culinary_flag cf
      ON cf.code = CASE flag.flag_token
          WHEN 'F' THEN 'FERMENTED'
          WHEN 'P' THEN 'PICKLED'
          WHEN 'S' THEN 'SMOKED'
          WHEN 'C' THEN 'CURED'
          WHEN 'D' THEN 'DRIED'
      END
    ON CONFLICT (ingredient_concept_id, culinary_flag_id) DO NOTHING;

    INSERT INTO ingredient_culinary_dimension (
        ingredient_concept_id,
        culinary_dimension_id,
        level
    )
    SELECT
        ic.id,
        cd.id,
        substring(dimension.dimension_token FROM 2)::smallint
    FROM _ingredient_catalog_expansion source
    CROSS JOIN LATERAL unnest(source.dimension_tokens)
        AS dimension(dimension_token)
    JOIN ingredient_concept ic ON ic.code = source.code
    JOIN culinary_dimension cd
      ON cd.code = CASE left(dimension.dimension_token, 1)
          WHEN 'D' THEN 'DOMINANCE'
          WHEN 'S' THEN 'SWEETNESS'
          WHEN 'A' THEN 'ACIDITY'
          WHEN 'B' THEN 'BITTERNESS'
          WHEN 'F' THEN 'FATTINESS'
          WHEN 'H' THEN 'HEAT'
          WHEN 'U' THEN 'UMAMI'
      END
    ON CONFLICT (ingredient_concept_id, culinary_dimension_id) DO NOTHING;

    DROP TABLE _ingredient_catalog_expansion;
    DROP TABLE _ingredient_catalog_raw;
END;
$seed$;
