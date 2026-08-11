-- Mise en Dice - extensive ingredient catalog expansion, part 3 of 3
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
PRESERVED_PRODUCE|konservierte pflanzliche Zutat|O|0.4000|2||V,R,W|E|E||
PROSCIUTTO|Prosciutto|S|0.4500|2||A,F,W|E|E|C|D4,F3,U5
PRUNE|Trockenpflaumen|S|0.5500|1||R,W|E|E|D|D3,S4
PUMPKIN_SEEDS|Kürbiskerne|S|0.6500|1||P,F|E|E||F4,U2
PURSLANE|Portulak|S|0.3000|4||V|D|P||A2
QUAIL|Wachtel|S|0.2500|4||A,F|D|P||D3,F3,U3
QUAIL_EGG|Wachtelei|S|0.4500|3||A,F|P|E||F3,U3
QUARK|Quark|S|0.7500|1||F,A,C|E|E|F|A2,F2
QUINCE|Quitte|S|0.4500|2||R|P|P||A3,D3,S2
QUINOA|Quinoa|S|0.7000|1||S,P|E|E||D2
RABBIT|Kaninchen|S|0.4000|3||A|P|P||D3,F2,U3
RADICCHIO|Radicchio|S|0.6500|2||V|E|E||B4
RAISIN|Rosinen|S|0.6500|1||R,W|E|E|D|D3,S5
RAMEN_NOODLES|Ramennudeln|S|0.7500|2||S|E|E||D2
RAS_EL_HANOUT|Ras el-Hanout|S|0.4000|3||O,W|P|P||D5
RAZOR_CLAMS|Schwertmuscheln|S|0.1500|5||A|D|D||U4
REDFISH|Rotbarsch|S|0.8500|1||A|E|E||U3
RED_CURRANT|rote Johannisbeere|S|0.5000|2||R,C|P|P||A4,S2
RED_LENTILS|rote Linsen|S|0.9000|1||P,S|E|E||S2
RED_ONION|rote Zwiebel|S|0.8000|1||V,O|E|E||D3,S2
RED_WINE|Rotwein|S|0.5500|1||C,W|E|E||A3,B2,D4
RED_WINE_VINEGAR|Rotweinessig|S|0.6000|1||C,W|E|E||A5,D3
RENDANG_PASTE|Rendang-Paste|S|0.3000|4||O,W|P|P||D5,H3,U4
RHUBARB|Rhabarber|S|0.5500|2||V,R,C|P|P||A5,D4
RICE_CAKES|koreanische Reiskuchen|S|0.5000|3||S|P|P||D2
RICE_FLOUR|Reismehl|S|0.5500|2||S|E|E||D1
RICE_PAPER|Reispapier|S|0.6500|2||S|E|E|D|D1
RICE_PRODUCTS|Reisprodukt|O|0.5000|2||S|E|E||
RICOTTA|Ricotta|S|0.7000|1||F,A|E|E||F3,S2
RISOTTO_RICE|Risottoreis|S|0.7500|1||S|E|E||D2
ROASTED_RED_PEPPER|geröstete Paprika|S|0.6500|2||V,W|E|E||D3,S3
ROMAINE_LETTUCE|Römersalat|S|0.8000|1||V|E|E||B1
ROMANESCO|Romanesco|S|0.7000|2||V|P|P||S2
ROSEMARY|Rosmarin|S|0.6500|1||O|E|E||B2,D5
RUTABAGA|Steckrübe|S|0.6500|1||V,S|E|E||S3
RYE_FLOUR|Roggenmehl|S|0.6000|1||S|E|E||D3
SAFFRON|Safran|S|0.1800|5|Bewusst sehr selten gewichtet; geringe Mengen prägen ein Gericht bereits deutlich.|O,W|P|P||B2,D5
SAGE|Salbei|S|0.5500|2||O|E|E||B3,D5
SAKE|Sake|S|0.4500|2||C,W|P|P||D3,S2,U2
SALAD_GREENS|Blattsalat|O|0.5500|1||V|E|E||
SALAMI|Salami|S|0.5000|1||A,F,W|E|E|C|D4,F4,U4
SALMON_ROE|Lachsrogen|S|0.2800|4||A,F,W|D|P||D4,F3,U5
SALSA|Salsa|S|0.5500|1||V,C,W|E|E||A3,D4,H2
SAMBAL_BRANDAL|Sambal Brandal|S|0.3500|4||W|P|P||D5,H5,U3
SAMBAL_OELEK|Sambal Oelek|S|0.5500|1||W|E|E||D4,H4
SARDINES|Sardinen|S|0.7000|2||A,F|P|E||D4,F4,U4
SAUCES_AND_PASTES|Sauce oder Würzpaste|O|0.3500|1||W|E|E||
SAUSAGE|Wurst|O|0.5500|1||A,F|E|E||
SEAFOOD|Fisch und Meeresfrüchte|O|0.5500|1||A|E|E||
SEA_BASS|Wolfsbarsch|S|0.6500|2||A|P|P||U3
SEA_BREAM|Dorade|S|0.7000|2||A|P|P||U3
SEEDS|Kerne und Samen|O|0.5500|1||P,F|E|E||
SEMOLINA|Hartweizengrieß|S|0.6500|1||S|E|E||D2
SERRANO_CHILI|Serrano-Chili|S|0.3500|4||O,W|D|P||D4,H3
SERRANO_HAM|Serrano-Schinken|S|0.4500|2||A,F,W|E|E|C|D4,F3,U5
SESAME_OIL|Sesamöl|S|0.5500|2||F,W|E|E||D5,F5,U2
SESAME_SEEDS|Sesam|S|0.7000|1||P,F,W|E|E||D3,F4
SHALLOT|Schalotte|S|0.8500|1||V,O|E|E||D3,S2
SHAOXING_WINE|Shaoxing-Reiswein|S|0.4000|3||W|P|P||D4,S2,U3
SHERRY|Sherry|S|0.4000|3||C,W|P|P||A2,D4,S2
SHERRY_VINEGAR|Sherryessig|S|0.4500|2||C,W|P|P||A5,D4
SHIMEJI|Shimeji|S|0.4000|3||V|P|P||D3,U4
SHIRATAKI|Shirataki-Nudeln|S|0.4500|3||S|P|P||D1
SHRIMP_PASTE|Garnelenpaste|S|0.3500|4||A,W|P|E|F|D5,U5
SICHUAN_PEPPER|Szechuanpfeffer|S|0.4500|3||O,W|P|P||D5,H2
SILKEN_TOFU|Seidentofu|S|0.6500|2||P|P|P||D1,U2
SMOKED_PAPRIKA|geräuchertes Paprikapulver|S|0.5500|2||O,W|E|E|S|D5,S2,U2
SMOKED_SALMON|Räucherlachs|S|0.5500|2||A,F,W|E|E|S|D4,F4,U4
SMOKED_TOFU|Räuchertofu|S|0.8000|1||P,W|E|E|S|D4,U4
SMOKED_TROUT|Räucherforelle|S|0.4500|3||A,F,W|P|P|S|D4,F3,U4
SOLE|Seezunge|S|0.3500|4||A|D|P||U3
SOURDOUGH_BREAD|Sauerteigbrot|S|0.7500|1||S,C|E|E|F|A2,D3
SOUR_CHERRY|Sauerkirsche|S|0.5000|2||R,C|P|P||A4,S2
SOUR_CREAM|saure Sahne|S|0.7500|1||F,C|E|E|F|A3,F3
SOYBEANS|Sojabohnen|S|0.5000|2||P,S,F|P|P||F3,U3
SOY_PRODUCTS|Sojaprodukt|O|0.5500|1||P|E|E||
SPAETZLE|Spätzle|S|0.7500|1||S|E|E||D2
SPAGHETTI|Spaghetti|S|0.8500|1||S|E|E||D2
SPAGHETTI_SQUASH|Spaghettikürbis|S|0.4500|3||V,S|P|P||S2
SPELT|Dinkel|S|0.6000|1||S|E|E||D2
SPICES|Gewürz|O|0.4500|1||O,W|E|E||
SPICE_BLENDS|Gewürzmischung|O|0.3500|2||O,W|E|E||
SPLIT_PEAS|Spalterbsen|S|0.7000|1||P,S|E|E|D|S2
SPROUTS|Sprossen|O|0.4500|2||V|E|E||
SRIRACHA|Sriracha|S|0.5500|1||W|E|E||D4,H3,S3
STARCHES|stärkehaltige Sättigungsbeilage|O|0.4500|1||S|E|E||
STARCH_BINDERS|Stärke oder Bindemittel|O|0.4000|1||S|E|E||
STAR_ANISE|Sternanis|S|0.4500|2||O,W|E|E|D|D5,S2
STEM_VEGETABLES|Stängelgemüse|O|0.5000|2||V|E|E||
STICKY_RICE|Klebreis|S|0.5500|2||S|P|E||D2
STINKY_TOFU|Stinky Tofu|S|0.1200|5||P,W|D|D|F|D5,U5
STOCKFISH|Stockfisch|S|0.2200|5||A,W|D|D|D|D5,U5
STOCKS|Brühe oder Fond|O|0.3500|1||W|E|E||
SUGAR_SNAP_PEAS|Zuckerschoten|S|0.7500|1||P,V|E|E||S3
SUMAC|Sumach|S|0.5000|2||O,C,W|E|E|D|A4,D4
SUNFLOWER_SEEDS|Sonnenblumenkerne|S|0.6500|1||P,F|E|E||F4
SUN_DRIED_TOMATO|getrocknete Tomaten|S|0.7000|2||V,W|E|E|D|D4,S3,U5
SURIMI|Surimi|S|0.6500|2||A|E|E||S2,U3
SWEETENERS|Süßungsmittel|O|0.3500|1||W|E|E||
SWORDFISH|Schwertfisch|S|0.4000|4||A|D|P||D3,U3
TAMARI|Tamari|S|0.4000|3||W|P|E|F|D4,U5
TAPENADE|Tapenade|S|0.4500|3||F,W|P|P||B2,D5,U4
TAPIOCA_STARCH|Tapiokastärke|S|0.5000|2||S|E|E||D1
TARO|Taro|S|0.4500|3||V,S|P|P||S2
TARRAGON|Estragon|S|0.5000|2||O|E|E||D5
TEA|Tee|O|0.3000|2||O,W|E|E||
TERIYAKI_SAUCE|Teriyakisauce|S|0.4500|2||W|E|E||D4,S4,U4
THAI_BASIL|Thai-Basilikum|S|0.5500|2||O|P|E||D4
THAI_GREEN_CURRY_PASTE|grüne Thai-Currypaste|S|0.4500|2||O,W|E|E||D5,H4,U3
THAI_RED_CURRY_PASTE|rote Thai-Currypaste|S|0.4500|2||O,W|E|E||D5,H4,U3
THAI_YELLOW_CURRY_PASTE|gelbe Thai-Currypaste|S|0.4000|3||O,W|P|P||D5,H2,U3
THYME|Thymian|S|0.6500|1||O|E|E||D4
TOMATILLO|Tomatillo|S|0.3000|4||V,C|D|P||A4,S2
TOMATO_PASSATA|passierte Tomaten|S|0.8000|1||V,C|E|E||A3,S2,U3
TOMATO_PRODUCTS|Tomatenprodukt|O|0.5000|1||V,W|E|E||
TORTILLA|Tortilla|S|0.7000|1||S|E|E||D2
TRIPE|Kutteln|S|0.1800|5||A|D|D||D4,U3
TROPICAL_FRUIT|Tropenfrucht|O|0.5500|2||R|E|E||
TROUT_ROE|Forellenrogen|S|0.2200|5||A,F,W|D|D||D4,U5
TRUFFLE|Trüffel|S|0.1200|5|Frische Trüffel oder hochwertiges Trüffelprodukt; bewusst extrem selten gewichtet.|V,F,W|D|D||D5,U5
TURKEY|Pute|S|0.8500|1||A|E|E||U2
TURKEY_BREAST|Putenbrust|S|0.9000|1||A|E|E||F1,U2
TURKEY_MINCE|Putenhack|S|0.7500|2||A|E|E||F2,U3
TURMERIC|Kurkuma|S|0.6000|1||O,W|E|E||B2,D4
TURNIP|Speiserübe|S|0.6000|2||V|P|P||B2,S2
TVP|Sojagranulat|S|0.6500|2|Texturiertes Sojaprotein, häufig als Granulat oder Schnetzel verkauft.|P|E|E|D|D1,U2
UBE|Ube|S|0.3500|4|Violette Yamswurzel; nicht mit violetter Süßkartoffel gleichzusetzen.|V,S|D|P||D3,S3
VANILLA|Vanille|S|0.3500|2||O,W|E|E||D5,S2
VEAL|Kalbfleisch|O|0.5500|2||A|E|E||
VEAL_CUTLET|Kalbsschnitzel|S|0.5500|2||A|P|P||F2,U3
VEAL_LIVER|Kalbsleber|S|0.3000|4||A|D|P||D5,U4
VEAL_SHANK|Kalbshaxe|S|0.4500|3||A,F|D|P||F3,U4
VEGETABLES|Gemüse|O|0.4500|1||V|E|E||
VEGETABLE_STOCK|Gemüsebrühe|S|0.5500|1||V,W|E|E||D3,U3
VENISON|Hirschfleisch|S|0.4000|3||A|P|P||D4,U4
VIETNAMESE_CORIANDER|vietnamesischer Koriander|S|0.2500|5||O|D|D||D5
VINEGAR|Essig|O|0.4500|1||C,W|E|E||
WATERCRESS|Brunnenkresse|S|0.4500|3||V,O|P|P||B2,D3,H2
WATERMELON|Wassermelone|S|0.7000|1||R|E|E||D2,S3
WATER_CHESTNUT|Wasserkastanien|S|0.5500|3||V,S|P|P||S2
WHEAT_FLOUR|Weizenmehl|S|0.7500|1||S|E|E||D1
WHEAT_NOODLES|Weizennudeln|S|0.8000|1||S|E|E||D2
WHEAT_PASTA|Weizenpasta|O|0.6000|1||S|E|E||
WHITE_ASPARAGUS|weißer Spargel|S|0.6500|1||V|P|P||B2,S2
WHITE_CABBAGE|Weißkohl|S|0.9500|1||V|E|E||S2
WHITE_PEPPER|weißer Pfeffer|S|0.5000|2||O,W|E|E||D4,H2
WHITE_SAUSAGE|Weißwurst|S|0.3500|2||A,F|P|P||D3,F4,U3
WHITE_SUGAR|Zucker|S|0.4500|1||W|E|E||D2,S5
WHITE_WINE|Weißwein|S|0.5500|1||C,W|E|E||A3,D3
WHITE_WINE_VINEGAR|Weißweinessig|S|0.6000|1||C,W|E|E||A5,D3
WHOLEGRAIN_MUSTARD|körniger Senf|S|0.5500|1||C,W|E|E||A3,D4,H2
WILD_BOAR|Wildschwein|S|0.4000|3||A,F|P|P||D4,F3,U4
WILD_RICE|Wildreis|S|0.4500|3|Botanisch kein Reis, kulinarisch jedoch ein reisähnliches Getreideprodukt.|S|P|P||D3
WONTON_WRAPPERS|Wonton-Teigblätter|S|0.4500|3||S|P|P||D1
WOOD_EAR|Mu-Err-Pilze|S|0.5500|3||V|P|P|D|D1,U3
WORCESTERSHIRE_SAUCE|Worcestersauce|S|0.4500|2||C,W|E|E|F|A3,D4,U5
XO_SAUCE|XO-Sauce|S|0.3000|4||A,F,W|P|P||D5,F4,H2,U5
YAM|Yamswurzel|S|0.4000|4||V,S|D|P||S2
YEAST_EXTRACT|Hefeextrakt|S|0.4000|2||W|E|E|F|D5,U5
YELLOW_LENTILS|gelbe Linsen|S|0.7000|2||P,S|P|P||S2
YUZU|Yuzu|S|0.2500|5||R,C,O|D|P||A5,D5
ZAATAR|Za’atar|S|0.4500|3||O,W|P|P||A2,D5
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
