--liquibase formatted sql
--changeset venomenon328:008-ingredient-catalog-expansion-1 splitStatements:false

-- Mise en Dice - extensive ingredient catalog expansion, part 1 of 3
-- Requires: schema/001-catalog-schema.sql
-- Requires: reference/001-reference-data.sql
-- Requires: catalog/002-ingredient-catalog.sql
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
ADZUKI_BEANS|Adzukibohnen|S|0.4500|3||P,S|P|P||S2,U2
AGAVE_SYRUP|Agavendicksaft|S|0.4500|1||W|E|E||D2,S5
AJVAR|Ajvar|S|0.5500|2||V,W|E|E||D4,H2,S3
AJWAIN|Ajwain|S|0.2500|5||O,W|D|P||D5
ALFALFA_SPROUTS|Alfalfasprossen|S|0.4500|3||V|P|P||B1
ALIGUE|Aligue|S|0.1800|5|Filipinische Würzpaste aus Krabbenfett; bewusst sehr selten gewichtet.|A,F,W|D|D||D5,F5,U5
ALLIUM_VEGETABLES|Lauch- und Zwiebelgemüse|O|0.5500|1||V,O|E|E||
ALLSPICE|Piment|S|0.4500|2||O,W|E|E||D5
ALMOND_BUTTER|Mandelmus|S|0.5500|2||P,F,W|E|E||D4,F5,S2
AMARANTH|Amaranth|S|0.4500|3||S,P|P|P||D2
ANCHOVIES|Sardellen|S|0.4500|2||A,F,W|E|E||D5,U5
ANCHO_CHILI|Ancho-Chili|S|0.3500|4||O,W|D|P|D|D5,H2,S2
ANNATTO|Annatto|S|0.3000|4||O,W|D|P||B2,D3
APPLE_CIDER_VINEGAR|Apfelessig|S|0.6500|1||C,W|E|E||A5,D3
ARUGULA|Rucola|S|0.8500|1||V,O|E|E||B3,D3
AVOCADO|Avocado|S|0.8500|1||R,V,F|E|E||D2,F5
BAGOONG|Bagoong|S|0.3500|4||A,W|P|E|F|D5,U5
BAGUETTE|Baguette|S|0.7500|1||S|E|E||D2
BANANA_KETCHUP|Bananenketchup|S|0.3500|4||R,C,W|P|E||A3,D4,S4
BARBECUE_SAUCE|Barbecuesauce|S|0.4500|2||W|E|E||A2,D5,S4,U3
BARLEY|Gerste|S|0.5500|2||S|P|P||D2
BASMATI_RICE|Basmatireis|S|0.8500|1||S|E|E||D2
BEANS|Bohnen|O|0.6500|1||P,S|E|E||
BEAN_SPROUTS|Mungbohnensprossen|S|0.8000|1||V|E|E||S1
BEEF|Rindfleisch|O|0.7500|1||A|E|E||
BEEF_BRISKET|Rinderbrust|S|0.7500|2||A,F|P|P||F4,U4
BEEF_CHEEK|Rinderbäckchen|S|0.4000|4||A,F|D|P||F3,U5
BEEF_HEART|Rinderherz|S|0.2500|4||A|D|P||D4,U4
BEEF_LIVER|Rinderleber|S|0.4000|3||A|P|P||D5,U4
BEEF_ROAST|Rinderbraten|S|0.8500|1||A|E|E||U4
BEEF_ROULADE|Rinderroulade|S|0.7500|1||A|E|E||U4
BEEF_RUMP|Rinderhüfte|S|0.7000|1||A|E|E||F2,U4
BEEF_SHIN|Rinderbeinscheibe|S|0.8500|1||A|E|E||U4
BEEF_SHORT_RIBS|Rinderrippen|S|0.5500|3||A,F|P|P||F4,U4
BEEF_STOCK|Rinderfond|S|0.5000|1||A,W|E|E||D4,U5
BEEF_TENDERLOIN|Rinderfilet|S|0.4500|3||A|P|P||F2,U4
BEEF_TONGUE|Rinderzunge|S|0.2500|4||A,F|D|P||D4,F3,U4
BEER|Bier|S|0.5000|1||C,W|E|E||B3,D3
BELACAN|Belacan|S|0.3000|4||A,W|D|P|F|D5,U5
BELUGA_LENTILS|Belugalinsen|S|0.6500|2||P,S|E|E||U2
BERBERE|Berbere|S|0.3000|4||O,W|D|P||D5,H4
BIRDS_EYE_CHILI|Bird’s-Eye-Chili|S|0.5000|3||O,W|P|E||D5,H5
BIVALVES|Muscheln|O|0.5500|2||A|P|E||
BLACKBERRY|Brombeere|S|0.5500|2||R|E|E||A3,S3
BLACK_BEANS|schwarze Bohnen|S|0.8000|1||P,S|E|E||U2
BLACK_CURRANT|schwarze Johannisbeere|S|0.4500|3||R,C|P|P||A4,D4,S2
BLACK_EYED_PEAS|Augenbohnen|S|0.5500|3||P,S|P|P||U2
BLACK_PEPPER|schwarzer Pfeffer|S|0.6000|1||O,W|E|E||D4,H2
BLACK_TEA|schwarzer Tee|S|0.3500|2||O,W|E|E|D|B4,D4
BLACK_VINEGAR|chinesischer schwarzer Essig|S|0.4500|3||C,W|P|P||A4,D4,S2,U2
BLOOD_ORANGE|Blutorange|S|0.5000|2||R,C|P|P||A3,D3,S3
BLOOD_SAUSAGE|Blutwurst|S|0.3000|3||A,F|P|P||D5,F4,U4
BONE_MARROW|Knochenmark|S|0.2500|5||A,F|D|P||D4,F5,U5
BONITO_FLAKES|Bonitoflocken|S|0.4000|3||A,W|P|P|D|D4,U5
BRATWURST|Bratwurst|S|0.7500|1||A,F|E|E||D3,F4,U4
BRAZIL_NUT|Paranüsse|S|0.4500|2||P,F|E|E||F5
BREAD|Brot oder Fladenbrot|O|0.5500|1||S|E|E||
BREADCRUMBS|Semmelbrösel|S|0.6000|1||S|E|E|D|D1
BROAD_BEANS|Dicke Bohnen|S|0.6500|2||P,V,S|P|E||B2,S2
BROWN_RICE|Vollkornreis|S|0.7000|1||S|E|E||D2
BROWN_SUGAR|brauner Zucker|S|0.4500|1||W|E|E||D3,S5
BUCKWHEAT|Buchweizen|S|0.6000|2||S|E|E||B2,D3
BUTTER|Butter|S|0.7500|1||F|E|E||D3,F5
BUTTERMILK|Buttermilch|S|0.6500|1||F,C|E|E|F|A3,F1
BUTTERNUT_SQUASH|Butternut-Kürbis|S|0.8000|1||V,S|E|E||S3
BUTTER_BEANS|Limabohnen|S|0.6000|2||P,S|P|P||S2,U2
CALAMANSI|Calamansi|S|0.3000|4||R,C|D|P||A5,D4
CAMEMBERT|Camembert|S|0.6500|1||F,A,W|E|E|F,C|D4,F5,U4
CANNED_SARDINES|Sardinen aus der Dose|S|0.5500|2||A,F|E|E||D4,F4,U5
CANNED_TOMATOES|Dosentomaten|S|0.8000|1||V,C|E|E||A3,S2,U3
CANNED_TUNA|Thunfisch aus der Dose|S|0.7500|1||A|E|E||D3,U4
CANNELLINI_BEANS|Cannellini-Bohnen|S|0.7500|1||P,S|E|E||U2
CANTALOUPE|Cantaloupe-Melone|S|0.5500|2||R|P|E||D3,S4
CARAWAY|Kümmel|S|0.5000|1||O,W|E|E||D5
CARDAMOM|Kardamom|S|0.5000|2||O,W|E|E||D5
CASHEW_BUTTER|Cashewmus|S|0.4500|3||P,F,W|P|P||D4,F5,S2
CASSAVA|Maniok|S|0.5500|3||V,S|P|P||S2
CATFISH|Wels|S|0.5500|3||A,F|P|P||F3,U3
CELERY_STALK|Staudensellerie|S|0.8500|1||V,O|E|E||B2,D3
CHANTERELLE|Pfifferlinge|S|0.6000|2||V|P|P||D3,U4
CHARD|Mangold|S|0.7500|1||V|E|E||B2
CHEDDAR|Cheddar|S|0.7500|1||F,A,W|E|E|F,C|D3,F4,U4
CHERRY|Kirsche|S|0.6500|1||R|E|E||A2,S4
CHERRY_TOMATO|Kirschtomaten|S|0.9000|1||V|E|E||A2,S3,U2
CHESTNUT|Esskastanien|S|0.5500|2||P,F,S|P|P||F2,S3
CHIA_SEEDS|Chiasamen|S|0.5000|2||P,F|E|E||F3
CHICKEN_DRUMSTICKS|Hähnchenunterkeulen|S|0.9000|1||A,F|E|E||F3,U4
CHICKEN_LIVER|Hähnchenleber|S|0.4500|3||A|P|E||D4,U4
CHICKEN_STOCK|Hühnerbrühe|S|0.5500|1||A,W|E|E||D3,U4
CHICKEN_WINGS|Hähnchenflügel|S|0.8000|1||A,F|E|E||F3,U4
CHICKPEA_FLOUR|Kichererbsenmehl|S|0.6000|2||S,P|E|E||D3,U2
CHICORY|Chicorée|S|0.7000|2||V|E|E||B4
CHILI_CONDIMENTS|Chilisauce oder -paste|O|0.4500|2||W|E|E||
CHILI_CRISP|Chili-Crisp|S|0.5000|2||F,W|E|E||D5,F4,H3,U4
CHILI_FLAKES|Chiliflocken|S|0.6000|1||O,W|E|E|D|D4,H3
CHIPOTLE|Chipotle|S|0.4500|3||O,W|P|P|S,D|D5,H3,U3
CHIVES|Schnittlauch|S|0.7000|1||O,V|E|E||D3
CHORIZO|Chorizo|S|0.5500|2||A,F,W|E|E|C|D5,F4,H2,U5
CIDER|Cider|S|0.4000|3||C,R,W|P|P||A3,D3,S3
CINNAMON|Zimt|S|0.5500|1||O,W|E|E||B2,D5,S2
CLAMS|Venusmuscheln|S|0.4500|3||A|D|P||U4
CLOVES|Gewürznelken|S|0.4000|2||O,W|E|E|D|B2,D5
COCOA_POWDER|Kakaopulver|S|0.5000|2||O,W|E|E|D|B5,D5,F2
COCOA_PRODUCTS|Kakao oder Schokolade|O|0.3500|2||F,O,W|E|E||
COCONUT|Kokosnuss|S|0.5500|2||R,F|P|E||D4,F4,S2
COCONUT_CREAM|Kokoscreme|S|0.6500|2||F,W|E|E||D4,F5,S2
COCONUT_FLAKES|Kokosraspeln|S|0.6000|1||R,F,W|E|E|D|D4,F4,S3
COCONUT_OIL|Kokosöl|S|0.5000|2||F|E|E||D4,F5,S2
COCONUT_PRODUCTS|Kokosprodukt|O|0.5000|2||F,R|E|E||
COCONUT_WATER|Kokoswasser|S|0.4000|3||R,W|E|E||D2,S2
COFFEE|Kaffee|S|0.3500|3||O,W|E|E||A2,B5,D5
CONDENSED_MILK|gezuckerte Kondensmilch|S|0.5500|2||F,W|E|E||D4,F3,S5
COOKED_HAM|Kochschinken|S|0.6500|1||A,F|E|E|C|D3,F2,U4
COOKING_ALCOHOL|Kochalkohol|O|0.3000|2||C,W|E|E||
COOKING_FATS|Kochfett|O|0.3500|1||F|E|E||
CORIANDER_SEED|Koriandersaat|S|0.6000|1||O,W|E|E||D4
CORNMEAL|Maismehl|S|0.6500|1||S|E|E||S2
CORNSTARCH|Maisstärke|S|0.7000|1||S|E|E||D1
CRAB|Krabben oder Krebsfleisch|S|0.5500|2||A|P|P||S2,U4
CRANBERRY|Cranberry|S|0.4500|2||R,C|P|E||A4,B2,S2
CRAYFISH|Flusskrebse|S|0.3500|4||A|D|P||S2,U4
CREAM_CHEESE|Frischkäse|S|0.8000|1||F,A|E|E||A2,F4
CREME_FRAICHE|Crème fraîche|S|0.7500|1||F,C|E|E|F|A2,F5
CRUSTACEANS|Krustentiere|O|0.6000|2||A|E|E||
CULTURED_DAIRY|gesäuertes Milchprodukt|O|0.4500|2||F,W|E|E||
CUMIN|Kreuzkümmel|S|0.6500|1||O,W|E|E||B2,D5
CURED_MEAT|gepökeltes oder luftgetrocknetes Fleisch|O|0.4000|2||A,F,W|E|E||
CURRY_LEAVES|Curryblätter|S|0.3500|4||O|D|P||D5
CURRY_POWDER|Currypulver|S|0.5500|1||O,W|E|E||B2,D5
CUTTLEFISH|Sepia|S|0.3500|4||A|D|P||D3,U3
DARK_CHOCOLATE|Zartbitterschokolade|S|0.5000|2||F,O,W|E|E||B4,D5,F4,S3
DARK_SOY_SAUCE|dunkle Sojasauce|S|0.5000|2||W|E|E|F|D4,S2,U5
DASHI|Dashi|S|0.4500|3||A,W|P|P||D4,U5
DATE|Dattel|S|0.6500|1||R,W|E|E||D3,S5
DATE_SYRUP|Dattelsirup|S|0.4500|2||R,W|E|E||D4,S5
DIJON_MUSTARD|Dijon-Senf|S|0.6000|1||C,W|E|E||A3,D4,H3
DOENJANG|Doenjang|S|0.4500|3||W|P|E|F|D5,U5
DOUBANJIANG|Doubanjiang|S|0.4500|3||W|P|P|F|D5,H3,U5
DRAGON_FRUIT|Drachenfrucht|S|0.3500|3||R|P|P||D1,S2
DRIED_APRICOT|getrocknete Aprikosen|S|0.6000|1||R,W|E|E|D|A2,S4
DRIED_FRUIT|Trockenfrüchte|O|0.4500|2||R,W|E|E||
DUCK|Ente|S|0.6000|2||A,F|P|P||D3,F4,U4
DUCK_EGG|Entenei|S|0.3000|4||A,F|D|P||F4,U3
DUCK_FAT|Entenfett|S|0.3500|4||F,A|D|P||D4,F5,U4
DUCK_LEG|Entenkeule|S|0.6000|2||A,F|P|P||F4,U4
DULSE|Dulse|S|0.2500|4||W|D|P|D|D4,U4
DUMPLING_DOUGH|Dumpling-Teig|S|0.4500|3||S|P|P||D1
DUMPLING_WRAPPERS|Teigblatt oder Dumpling-Hülle|O|0.3500|3||S|E|E||
EEL|Aal|S|0.3000|4||A,F|D|D||D4,F5,U4
EGGS|Eier|O|0.7000|1||A|E|E||
EGG_NOODLES|Eiernudeln|S|0.8500|1||S|E|E||D2
EGUSI_SEEDS|Egusi-Samen|S|0.2000|5||P,F|D|D||D3,F4
ELDERBERRY|Holunderbeeren|S|0.3000|3||R|P|P||B2,D4,S2
ENDIVE|Endivie|S|0.6000|2||V|P|P||B4
ENOKI|Enoki|S|0.4500|3||V|P|P||D2,U3
EVAPORATED_MILK|Kondensmilch|S|0.6000|1||F|E|E||F3,S2
FARRO|Emmer oder Farro|S|0.4000|3||S|P|P||D3
FENNEL_SEED|Fenchelsaat|S|0.4500|2||O,W|E|E||D5,S2
FENUGREEK|Bockshornklee|S|0.4000|3||O,W|P|P||B4,D5
FERMENTED_BLACK_BEANS|fermentierte schwarze Bohnen|S|0.3500|4||P,W|D|P|F|D5,U5
FERMENTED_SEASONINGS|fermentierte Würzzutat|O|0.4000|2||W|E|E||
FERMENTED_TOFU|fermentierter Tofu|S|0.3000|4||P,W|D|P|F|D5,U5
FIG|Feige|S|0.6500|1||R|E|E||D3,S4
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
