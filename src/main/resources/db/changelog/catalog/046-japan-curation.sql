--liquibase formatted sql
--changeset venomenon328:046-japan-curation splitStatements:false
-- Issue #261 / Japan JP curation, approved Phase-3 partial delivery.
-- Gate 1: https://github.com/venomenon328/mise-en-dice/issues/261#issuecomment-5716165754
-- Gate 2: https://github.com/venomenon328/mise-en-dice/issues/261#issuecomment-5717151303
-- Phase 3: https://github.com/venomenon328/mise-en-dice/issues/261#issuecomment-5717408347
-- Persists only the approved 108 existing JP relations and 20 new concepts.
-- KONNYAKU and FERMENTED_TOFU -> JP remain explicitly out of scope.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947227);
LOCK TABLE ingredient_concept, ingredient_concept_alias, ingredient_refinement,
    ingredient_functional_role, ingredient_culinary_flag, ingredient_culinary_dimension,
    ingredient_culinary_country, ingredient_seasonality, ingredient_availability,
    exclusion_rule, exclusion_rule_target, participant, functional_role,
    culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE japan_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    challenge_specificity text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO japan_new_concept VALUES
    ('WASABI', 'Wasabi', 'SPECIFIC', 3, 1.0000,
        $note$Frisches Rhizom mit grünem Aroma und rasch verfliegender, nasaler Schärfe. Fein gerieben als Würzzutat verwenden; Meerrettich-Ersatzpasten sind nicht gemeint.$note$,
        'DIFFICULT',
        $note$Frische Rhizome sind auf wenige Spezialimporteure und gekühlten Versand beschränkt; Liefertermin und kurze Frischehaltung müssen zusammenpassen.$note$),
    ('UMEBOSHI', 'Umeboshi', 'SPECIFIC', 3, 0.8000,
        $note$Ganze gesalzene und getrocknete Ume-Früchte mit ausgeprägter Säure und Salzigkeit. Das Fruchtfleisch lässt sich als Reisbegleitung oder zum Würzen verwenden; Paste und Saft sind getrennte Produkte.$note$,
        'PLANNED',
        $note$Ganze eingelegte Früchte sind über breiten Naturkost-Versand gezielt bestellbar und benötigen keinen Kühltransport; örtlich sind sie kein verlässlicher Standardartikel.$note$),
    ('SHICHIMI_TOGARASHI', 'Shichimi Tōgarashi', 'SPECIFIC', 2, 1.0000,
        $note$Japanische trockene Mehrgewürzmischung auf Chili-Basis mit aromatischen Komponenten wie Sanshō, Sesam, Zitrusschale oder Algen. Die genaue Zusammensetzung darf variieren; reines Ichimi ist nicht gemeint.$note$,
        'SPECIALTY',
        $note$Als haltbare Gewürzmischung in mehreren breit aufgestellten Asia- und Japan-Sortimenten planbar; der allgemeine Gewürzhandel führt sie nicht zuverlässig.$note$),
    ('YUZU_KOSHO', 'Yuzu Koshō', 'SPECIFIC', 3, 0.8000,
        $note$Salzig-scharfe japanische Paste aus Yuzu-Schale, Chili und Salz; grüne und rote Varianten gehören zum selben Konzept. Konzentriert dosieren; reine Yuzu- oder Chiliprodukte bleiben getrennt.$note$,
        'SPECIALTY',
        $note$Die haltbare Paste ist in mehreren japanisch und asiatisch ausgerichteten Spezialsortimenten planbar; im allgemeinen Saucenregal ist sie kein verlässlicher Artikel.$note$),
    ('MOCHI', 'Mochi', 'SPECIFIC', 3, 1.0000,
        $note$Ungesüßte japanische Reiskuchen aus gestampftem Klebreis mit zäher, elastischer Textur. Gemeint sind schlichte Kochformen wie Kiri- oder Marumochi, nicht gefüllte Süßwaren oder Tteok.$note$,
        'SPECIALTY',
        $note$Schlichte haltbare Koch-Mochi sind in mehreren Japan- und Asia-Sortimenten planbar; Süßwaren dominieren zwar die Sichtbarkeit, die ungewürzte Grundform ist aber wiederholt erhältlich.$note$),
    ('BURDOCK_ROOT', 'Klettenwurzel', 'SPECIFIC', 3, 1.0000,
        $note$Lange braune Speisewurzel mit erdig-nussigem Aroma und fester, faseriger Textur. Dünn schneiden oder stifteln und als Gemüse garen; Tee- und Pulverprodukte sind nicht gemeint.$note$,
        'DIFFICULT',
        $note$Frische Wurzeln erscheinen nur in engen Asia-Frischsortimenten und mit schwankendem Bestand; haltbare Tee- oder Pulverware eröffnet keinen gleichwertigen Bezugsweg.$note$),
    ('NAGAIMO', 'Nagaimo', 'SPECIFIC', 3, 1.0000,
        $note$Japanische lang-zylindrische Yamknolle mit knackiger Rohtextur und charakteristisch schleimiger Bindung beim Reiben. Die engere Nagaimo-Gruppe ist gemeint, nicht sämtliche Yamaimo- oder Yam-Arten.$note$,
        'DIFFICULT',
        $note$Frische japanische Yamknollen sind nur über wenige spezialisierte Frischimporte belegt; Artbezeichnung und konkrete Sortengruppe müssen beim Einkauf genau geprüft werden.$note$),
    ('KINAKO', 'Kinako', 'SPECIFIC', 3, 1.0000,
        $note$Fein gemahlene geröstete Sojabohnen mit nussigem Aroma. Ungesüßt als Mehl- und Würzzutat verwenden; rohe Sojamehle oder bereits gesüßte Mischungen sind nicht gemeint.$note$,
        'SPECIALTY',
        $note$Ungesüßtes geröstetes Sojamehl ist als haltbare Ware in mehreren Japan- und Asia-Sortimenten planbar; allgemeines Back- oder Mehlregal führt es nicht zuverlässig.$note$),
    ('ABURAAGE', 'Aburaage', 'SPECIFIC', 3, 1.0000,
        $note$Dünne frittierte Tofublätter beziehungsweise -taschen mit schwammiger Struktur. Gemeint ist die ungewürzte Grundware zum Füllen oder Mitgaren, nicht dicker Atsuage oder bereits süß marinierte Inari-Hüllen.$note$,
        'DIFFICULT',
        $note$Ungewürzte dünne Tofutaschen sind deutlich seltener als fertige süße Inari-Hüllen und benötigen enge Kühl- oder Tiefkühlsortimente; die passende Grundform muss gezielt gesucht werden.$note$),
    ('YUBA', 'Yuba', 'SPECIFIC', 3, 1.0000,
        $note$Beim Erhitzen von Sojamilch abgenommene Haut mit feinem Sojaaroma. Frische und getrocknete Grundformen gehören dazu; trockene Ware wird eingeweicht, gepresste Tofublätter und frittierte Rollen bleiben getrennt.$note$,
        'SPECIALTY',
        $note$Getrocknete Sojamilchhaut ist bei mehreren Asia-Lebensmittelhändlern ohne Kühltransport planbar erhältlich; frische Haut wird deutlich enger und mit Kühlbedarf angeboten.$note$),
    ('BENI_SHOGA', 'Beni Shōga', 'SPECIFIC', 3, 1.0000,
        $note$Fein geschnittener roter Ingwer in salzig-saurer Ume-Lake. Deutlich würziger und weniger süß als Sushi-Gari; als Beilage und Würzzutat verwenden.$note$,
        'DIFFICULT',
        $note$Die passende rote Ume-Lake-Variante ist wesentlich enger als gewöhnlicher Sushi-Ingwer und nur in wenigen japanisch ausgerichteten Importwegen belegt.$note$),
    ('KOMATSUNA', 'Komatsuna', 'SPECIFIC', 3, 1.0000,
        $note$Japanisches Blattgemüse mit dunkelgrünen Blättern und saftigen Stielen, mild bis leicht senfig. Blätter und Stiele gemeinsam als Kochgemüse verwenden.$note$,
        'UNAVAILABLE',
        $note$Für frische Blätter ist im normalen Challenge-Vorlauf kein wiederholbarer Endkundenweg belegt; auffindbare Saatgutangebote helfen für die Küchenware nicht.$note$),
    ('MIZUNA', 'Mizuna', 'SPECIFIC', 3, 1.0000,
        $note$Japanisches Blattgemüse mit fein gezackten Blättern, knackigen Stielen und mild-pfeffriger Note. Roh oder gegart verwendbar; gemeint ist die eigenständige frische Gemüseart.$note$,
        'UNAVAILABLE',
        $note$Für reine frische Mizuna-Ware ist kein belastbarer wiederholbarer Bezugsweg innerhalb des normalen Vorlaufs belegt; Saatgut und gemischte Salatbeutel ersetzen die Einzelzutat nicht.$note$),
    ('MITSUBA', 'Mitsuba', 'SPECIFIC', 3, 1.0000,
        $note$Frisches japanisches Doldenkraut mit feinem Sellerie-, Petersilien- und Zitrusaroma. Blätter und zarte Stiele als Kräuterzutat verwenden; gewöhnliche Petersilie ist kein Ersatzprodukt dieses Konzepts.$note$,
        'DIFFICULT',
        $note$Frisches Kraut ist nur über wenige spezialisierte Importwege mit engem Frischefenster belegt; getrocknete Ware oder Saatgut eröffnen keinen gleichwertigen Weg.$note$),
    ('MYOGA', 'Myōga', 'SPECIFIC', 3, 1.0000,
        $note$Knackige Blütenknospen einer japanischen Ingwerart mit frischem, blumig-ingwerigem Aroma. Fein geschnitten roh oder kurz gegart verwenden; das gewöhnliche Ingwerrhizom ist eine andere Zutat.$note$,
        'DIFFICULT',
        $note$Frische Blütenknospen sind nur über wenige Japanimporte und mit langsamem Frischversand belegt; der normale einwöchige Challenge-Vorlauf ist dadurch fragil.$note$),
    ('AONORI', 'Aonori', 'SPECIFIC', 3, 1.0000,
        $note$Aromatische grüne Würzalge, getrocknet und fein geschnitten oder geflockt. Als Streu- und Würzzutat verwenden; Nori-Blätter und unspezifische grüne Algenprodukte bleiben getrennt.$note$,
        'DIFFICULT',
        $note$Formgenau als Aonori ausgewiesene Würzalge ist nur über wenige spezialisierte Japanwege belegt; allgemeinere grüne Algenware macht den Markt nicht breiter.$note$),
    ('FURIKAKE', 'Furikake', 'OPEN', 3, 0.8000,
        $note$Trockene zusammengesetzte Reiswürze, etwa mit Sesam, Algen, Fisch oder Gemüse. Die Mischung ist bewusst wählbar; feuchte Varianten und bloße Einzelzutaten sind ausgeschlossen.$note$,
        'SPECIALTY',
        $note$Trockene Reiswürzmischungen sind in mehreren Japan- und Gewürzsortimenten als haltbare Packungsware planbar erhältlich; die Auswahl erfordert keinen Frischeversand.$note$),
    ('JAPANESE_CURRY_ROUX', 'Japanischer Curry-Roux', 'SPECIFIC', 2, 0.8000,
        $note$Konzentrierte japanische Curry-Basis aus Fett, Mehl oder Stärke und Gewürzen, meist als feste Blöcke oder trockene Roux-Mischung. Zum Auflösen und Binden einer Sauce gedacht; fertiges Curry oder reines Currypulver sind nicht gemeint.$note$,
        'SPECIALTY',
        $note$Roux-Blöcke und trockene Basen sind bei mehreren breit aufgestellten Asia- und Japan-Händlern als haltbare Ware planbar; im allgemeinen Curry- oder Gewürzregal kein verlässlicher Standard.$note$),
    ('SANSHO', 'Sanshō', 'SPECIFIC', 3, 1.0000,
        $note$Getrocknete aromatische Fruchtschalen des japanischen Sanshō mit zitrischer Schärfe und leicht betäubender Wirkung. Ganze oder gemahlene Schalen gehören dazu; Blätter, grüne Früchte und Szechuanpfeffer bleiben getrennt.$note$,
        'SPECIALTY',
        $note$Ganze und gemahlene getrocknete Schalen sind in mehreren japanisch geprägten Gewürzsortimenten als haltbare Ware planbar; der allgemeine Gewürzhandel führt sie nicht zuverlässig.$note$),
    ('FU', 'Fu', 'OPEN', 3, 0.8000,
        $note$Japanische Weizengluten-Grundprodukte: feucht-elastisches Nama-fu mit Klebreismehl und gebacken-getrocknetes, poröses Yaki-fu. Beide Formen sind wählbar, aber nicht in jedem Gericht austauschbar; Seitan und frittierte Fu-Formen bleiben getrennt.$note$,
        'DIFFICULT',
        $note$Die trockene Yaki-fu-Form ist nur über enge Japanimporte belegt; ein breiter deutscher Spezialmarkt fehlt. Frische Nama-fu-Ware eröffnet keinen ebenso verlässlichen zusätzlichen Weg.$note$);

CREATE TEMP TABLE japan_new_alias (
    concept_code text NOT NULL,
    alias_text text NOT NULL,
    PRIMARY KEY (concept_code, alias_text)
) ON COMMIT DROP;

INSERT INTO japan_new_alias VALUES
    ('SHICHIMI_TOGARASHI', 'Shichimi Togarashi'),
    ('YUZU_KOSHO', 'Yuzu Kosho'),
    ('BURDOCK_ROOT', 'Gobō'),
    ('BURDOCK_ROOT', 'Gobo'),
    ('YUBA', 'Sojamilchhaut'),
    ('YUBA', 'Tofuhaut'),
    ('BENI_SHOGA', 'Beni Shoga'),
    ('MITSUBA', 'Japanische Petersilie'),
    ('MYOGA', 'Myoga'),
    ('AONORI', 'Ao Nori'),
    ('SANSHO', 'Sansho');

CREATE TEMP TABLE japan_refinement (
    parent_code text NOT NULL,
    child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO japan_refinement VALUES
    ('ROOT_VEGETABLES', 'WASABI'),
    ('PRESERVED_PRODUCE', 'UMEBOSHI'),
    ('SPICE_BLENDS', 'SHICHIMI_TOGARASHI'),
    ('CHILI_CONDIMENTS', 'YUZU_KOSHO'),
    ('RICE_PRODUCTS', 'MOCHI'),
    ('ROOT_VEGETABLES', 'BURDOCK_ROOT'),
    ('YAM', 'NAGAIMO'),
    ('FLOUR', 'KINAKO'),
    ('SOY_PRODUCTS', 'KINAKO'),
    ('TOFU', 'ABURAAGE'),
    ('SOY_PRODUCTS', 'YUBA'),
    ('PLANT_PROTEIN_PRODUCTS', 'YUBA'),
    ('PRESERVED_PRODUCE', 'BENI_SHOGA'),
    ('LEAFY_GREENS', 'KOMATSUNA'),
    ('CABBAGE_VEGETABLES', 'KOMATSUNA'),
    ('LEAFY_GREENS', 'MIZUNA'),
    ('CABBAGE_VEGETABLES', 'MIZUNA'),
    ('FRESH_HERBS', 'MITSUBA'),
    ('FLOWER_VEGETABLES', 'MYOGA'),
    ('SEAWEED', 'AONORI'),
    ('SPICE_BLENDS', 'FURIKAKE'),
    ('SAUCES_AND_PASTES', 'JAPANESE_CURRY_ROUX'),
    ('SPICES', 'SANSHO'),
    ('PLANT_PROTEIN_PRODUCTS', 'FU');

CREATE TEMP TABLE japan_existing_country (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO japan_existing_country VALUES
    ('ADZUKI_BEANS'),
    ('BAMBOO_SHOOTS'),
    ('BARLEY'),
    ('BEEF_TONGUE'),
    ('BITTER_MELON'),
    ('BONITO_FLAKES'),
    ('BUCKWHEAT'),
    ('BUCKWHEAT_FLOUR'),
    ('CARP'),
    ('CARROT'),
    ('CHESTNUT'),
    ('CHICKEN'),
    ('CHICKEN_LIVER'),
    ('CHICKEN_WINGS'),
    ('CHILI_OIL'),
    ('DAIKON'),
    ('DASHI'),
    ('DRIED_FISH'),
    ('DUMPLING_WRAPPERS'),
    ('EDAMAME'),
    ('EEL'),
    ('EGG'),
    ('EGGPLANT'),
    ('ENOKI'),
    ('FIRM_TOFU'),
    ('FISH'),
    ('FISH_ROE'),
    ('FISH_SAUCE'),
    ('GARLIC_CHIVES'),
    ('GINGER'),
    ('GREEN_TEA'),
    ('HIJIKI'),
    ('HORSERADISH'),
    ('KOMBU'),
    ('LAMB'),
    ('LIGHT_SOY_SAUCE'),
    ('LOTUS_ROOT'),
    ('MACKEREL'),
    ('MAITAKE'),
    ('MATCHA'),
    ('MAYONNAISE'),
    ('MIRIN'),
    ('MISO'),
    ('MUSHROOMS'),
    ('NAPA_CABBAGE'),
    ('NATTO'),
    ('NOODLES'),
    ('NORI'),
    ('OCTOPUS'),
    ('PANKO'),
    ('PERILLA_LEAVES'),
    ('PICKLED_GINGER'),
    ('PONZU'),
    ('PORK'),
    ('PORK_BELLY'),
    ('POTATO_STARCH'),
    ('PUMPKIN'),
    ('RADISH'),
    ('RAMEN_NOODLES'),
    ('RICE'),
    ('RICE_FLOUR'),
    ('RICE_VINEGAR'),
    ('SAKE'),
    ('SALMON'),
    ('SALMON_ROE'),
    ('SEAWEED'),
    ('SEA_BREAM'),
    ('SESAME_OIL'),
    ('SESAME_SEEDS'),
    ('SHIITAKE'),
    ('SHIMEJI'),
    ('SHIRATAKI'),
    ('SHRIMP'),
    ('SILKEN_TOFU'),
    ('SOBA'),
    ('SOYBEANS'),
    ('SOY_DRINK'),
    ('SOY_PRODUCTS'),
    ('SOY_SAUCE'),
    ('SPRING_ONION'),
    ('SQUID'),
    ('STICKY_RICE'),
    ('SURIMI'),
    ('SUSHI_RICE'),
    ('SWEET_POTATO'),
    ('TAMARI'),
    ('TARO'),
    ('TERIYAKI_SAUCE'),
    ('TOFU'),
    ('TUNA'),
    ('TURNIP'),
    ('UDON'),
    ('WAKAME'),
    ('WHEAT_NOODLES'),
    ('WHITE_CABBAGE'),
    ('YAM'),
    ('YUZU'),
    ('MUTTON'),
    ('PORK_MINCE'),
    ('KETCHUP'),
    ('PEANUT'),
    ('HOT_MUSTARD'),
    ('PEARL_BARLEY'),
    ('PORK_CUTLET'),
    ('CRAB'),
    ('KING_CRAB'),
    ('SNOW_CRAB'),
    ('SPINY_LOBSTER');

CREATE TEMP TABLE japan_required_existing (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO japan_required_existing
SELECT code FROM japan_existing_country
UNION
SELECT parent_code FROM japan_refinement;

DO $validation$
BEGIN
    IF (SELECT count(*) FROM japan_new_concept) <> 20
       OR (SELECT count(*) FROM japan_new_alias) <> 11
       OR (SELECT count(*) FROM japan_refinement) <> 24
       OR (SELECT count(*) FROM japan_existing_country) <> 108 THEN
        RAISE EXCEPTION 'Japan curation approval set has an unexpected cardinality';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Japan curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'JP' AND display_name = 'Japan'
    ) THEN
        RAISE EXCEPTION 'Japan culinary-country reference is missing or inconsistent';
    END IF;

    IF (SELECT count(*) FROM participant WHERE code IN ('GEORGIA', 'TOBIAS')) <> 2 THEN
        RAISE EXCEPTION 'Japan curation requires the Georgia and Tobias participants';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ACID'), ('AROMATIC'), ('FAT'), ('FRUIT'), ('PLANT_PROTEIN'),
            ('SEASONING'), ('STARCH'), ('VEGETABLE')
        ) AS required(code)
        LEFT JOIN functional_role reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires functional-role references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ACIDITY'), ('BITTERNESS'), ('DOMINANCE'), ('FATTINESS'),
            ('HEAT'), ('SWEETNESS'), ('UMAMI')
        ) AS required(code)
        LEFT JOIN culinary_dimension reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires culinary-dimension references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES ('DRIED'), ('PICKLED')) AS required(code)
        LEFT JOIN culinary_flag reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Japan curation requires culinary-flag references that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_concept proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_alias proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.alias_text))
    ) OR EXISTS (
        SELECT 1
        FROM japan_new_alias proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.alias_text))
    ) THEN
        RAISE EXCEPTION 'Japan curation proposed names or aliases collide with the current catalog';
    END IF;

    IF EXISTS (
        SELECT normalized_name
        FROM (
            SELECT lower(btrim(display_name)) AS normalized_name
            FROM japan_new_concept
            UNION ALL
            SELECT lower(btrim(alias_text))
            FROM japan_new_alias
        ) proposed_names
        GROUP BY normalized_name
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Japan curation proposed names or aliases collide with each other';
    END IF;
END;
$validation$;

CREATE TEMP TABLE japan_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO japan_existing_changes
SELECT concept.id
FROM japan_existing_country approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE NOT EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'JP'
)
UNION
SELECT concept.id
FROM japan_refinement relation
JOIN ingredient_concept concept ON concept.code = relation.parent_code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM japan_new_concept;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT concept.id, alias.alias_text
FROM japan_new_alias alias
JOIN ingredient_concept concept ON concept.code = alias.concept_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM japan_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('WASABI', 'AROMATIC'),
    ('WASABI', 'SEASONING'),
    ('UMEBOSHI', 'FRUIT'),
    ('UMEBOSHI', 'ACID'),
    ('UMEBOSHI', 'SEASONING'),
    ('SHICHIMI_TOGARASHI', 'AROMATIC'),
    ('SHICHIMI_TOGARASHI', 'SEASONING'),
    ('YUZU_KOSHO', 'AROMATIC'),
    ('YUZU_KOSHO', 'SEASONING'),
    ('MOCHI', 'STARCH'),
    ('BURDOCK_ROOT', 'VEGETABLE'),
    ('NAGAIMO', 'VEGETABLE'),
    ('NAGAIMO', 'STARCH'),
    ('KINAKO', 'PLANT_PROTEIN'),
    ('KINAKO', 'STARCH'),
    ('KINAKO', 'AROMATIC'),
    ('ABURAAGE', 'PLANT_PROTEIN'),
    ('ABURAAGE', 'FAT'),
    ('YUBA', 'PLANT_PROTEIN'),
    ('BENI_SHOGA', 'AROMATIC'),
    ('BENI_SHOGA', 'ACID'),
    ('BENI_SHOGA', 'SEASONING'),
    ('KOMATSUNA', 'VEGETABLE'),
    ('MIZUNA', 'VEGETABLE'),
    ('MITSUBA', 'AROMATIC'),
    ('MYOGA', 'VEGETABLE'),
    ('MYOGA', 'AROMATIC'),
    ('AONORI', 'AROMATIC'),
    ('AONORI', 'SEASONING'),
    ('FURIKAKE', 'AROMATIC'),
    ('FURIKAKE', 'SEASONING'),
    ('JAPANESE_CURRY_ROUX', 'FAT'),
    ('JAPANESE_CURRY_ROUX', 'STARCH'),
    ('JAPANESE_CURRY_ROUX', 'SEASONING'),
    ('SANSHO', 'AROMATIC'),
    ('SANSHO', 'SEASONING'),
    ('FU', 'PLANT_PROTEIN')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('WASABI', 'DOMINANCE', 4), ('WASABI', 'SWEETNESS', 1),
    ('WASABI', 'ACIDITY', 1), ('WASABI', 'BITTERNESS', 2),
    ('WASABI', 'FATTINESS', 1), ('WASABI', 'HEAT', 4), ('WASABI', 'UMAMI', 1),
    ('UMEBOSHI', 'DOMINANCE', 5), ('UMEBOSHI', 'SWEETNESS', 1),
    ('UMEBOSHI', 'ACIDITY', 5), ('UMEBOSHI', 'BITTERNESS', 1),
    ('UMEBOSHI', 'FATTINESS', 1), ('UMEBOSHI', 'HEAT', 1), ('UMEBOSHI', 'UMAMI', 2),
    ('SHICHIMI_TOGARASHI', 'DOMINANCE', 4), ('SHICHIMI_TOGARASHI', 'SWEETNESS', 1),
    ('SHICHIMI_TOGARASHI', 'ACIDITY', 1), ('SHICHIMI_TOGARASHI', 'BITTERNESS', 2),
    ('SHICHIMI_TOGARASHI', 'FATTINESS', 1), ('SHICHIMI_TOGARASHI', 'HEAT', 3),
    ('SHICHIMI_TOGARASHI', 'UMAMI', 2),
    ('YUZU_KOSHO', 'DOMINANCE', 5), ('YUZU_KOSHO', 'SWEETNESS', 1),
    ('YUZU_KOSHO', 'ACIDITY', 2), ('YUZU_KOSHO', 'BITTERNESS', 2),
    ('YUZU_KOSHO', 'FATTINESS', 1), ('YUZU_KOSHO', 'HEAT', 4), ('YUZU_KOSHO', 'UMAMI', 2),
    ('MOCHI', 'DOMINANCE', 2), ('MOCHI', 'SWEETNESS', 1), ('MOCHI', 'ACIDITY', 1),
    ('MOCHI', 'BITTERNESS', 1), ('MOCHI', 'FATTINESS', 1), ('MOCHI', 'HEAT', 1),
    ('MOCHI', 'UMAMI', 1),
    ('BURDOCK_ROOT', 'DOMINANCE', 3), ('BURDOCK_ROOT', 'SWEETNESS', 2),
    ('BURDOCK_ROOT', 'ACIDITY', 1), ('BURDOCK_ROOT', 'BITTERNESS', 2),
    ('BURDOCK_ROOT', 'FATTINESS', 1), ('BURDOCK_ROOT', 'HEAT', 1),
    ('BURDOCK_ROOT', 'UMAMI', 2),
    ('NAGAIMO', 'DOMINANCE', 2), ('NAGAIMO', 'SWEETNESS', 2),
    ('NAGAIMO', 'ACIDITY', 1), ('NAGAIMO', 'BITTERNESS', 1),
    ('NAGAIMO', 'FATTINESS', 1), ('NAGAIMO', 'HEAT', 1), ('NAGAIMO', 'UMAMI', 2),
    ('KINAKO', 'DOMINANCE', 3), ('KINAKO', 'SWEETNESS', 1), ('KINAKO', 'ACIDITY', 1),
    ('KINAKO', 'BITTERNESS', 2), ('KINAKO', 'FATTINESS', 2), ('KINAKO', 'HEAT', 1),
    ('KINAKO', 'UMAMI', 2),
    ('ABURAAGE', 'DOMINANCE', 2), ('ABURAAGE', 'SWEETNESS', 1),
    ('ABURAAGE', 'ACIDITY', 1), ('ABURAAGE', 'BITTERNESS', 1),
    ('ABURAAGE', 'FATTINESS', 4), ('ABURAAGE', 'HEAT', 1), ('ABURAAGE', 'UMAMI', 2),
    ('YUBA', 'DOMINANCE', 2), ('YUBA', 'SWEETNESS', 1), ('YUBA', 'ACIDITY', 1),
    ('YUBA', 'BITTERNESS', 1), ('YUBA', 'FATTINESS', 2), ('YUBA', 'HEAT', 1),
    ('YUBA', 'UMAMI', 3),
    ('BENI_SHOGA', 'DOMINANCE', 4), ('BENI_SHOGA', 'SWEETNESS', 1),
    ('BENI_SHOGA', 'ACIDITY', 4), ('BENI_SHOGA', 'BITTERNESS', 1),
    ('BENI_SHOGA', 'FATTINESS', 1), ('BENI_SHOGA', 'HEAT', 2),
    ('BENI_SHOGA', 'UMAMI', 1),
    ('KOMATSUNA', 'DOMINANCE', 2), ('KOMATSUNA', 'SWEETNESS', 2),
    ('KOMATSUNA', 'ACIDITY', 1), ('KOMATSUNA', 'BITTERNESS', 2),
    ('KOMATSUNA', 'FATTINESS', 1), ('KOMATSUNA', 'HEAT', 1), ('KOMATSUNA', 'UMAMI', 2),
    ('MIZUNA', 'DOMINANCE', 2), ('MIZUNA', 'SWEETNESS', 2),
    ('MIZUNA', 'ACIDITY', 1), ('MIZUNA', 'BITTERNESS', 2),
    ('MIZUNA', 'FATTINESS', 1), ('MIZUNA', 'HEAT', 1), ('MIZUNA', 'UMAMI', 2),
    ('MITSUBA', 'DOMINANCE', 3), ('MITSUBA', 'SWEETNESS', 1),
    ('MITSUBA', 'ACIDITY', 1), ('MITSUBA', 'BITTERNESS', 2),
    ('MITSUBA', 'FATTINESS', 1), ('MITSUBA', 'HEAT', 1), ('MITSUBA', 'UMAMI', 1),
    ('MYOGA', 'DOMINANCE', 3), ('MYOGA', 'SWEETNESS', 1), ('MYOGA', 'ACIDITY', 1),
    ('MYOGA', 'BITTERNESS', 2), ('MYOGA', 'FATTINESS', 1), ('MYOGA', 'HEAT', 2),
    ('MYOGA', 'UMAMI', 1),
    ('AONORI', 'DOMINANCE', 3), ('AONORI', 'SWEETNESS', 1),
    ('AONORI', 'ACIDITY', 1), ('AONORI', 'BITTERNESS', 2),
    ('AONORI', 'FATTINESS', 1), ('AONORI', 'HEAT', 1), ('AONORI', 'UMAMI', 4),
    ('JAPANESE_CURRY_ROUX', 'DOMINANCE', 5), ('JAPANESE_CURRY_ROUX', 'SWEETNESS', 2),
    ('JAPANESE_CURRY_ROUX', 'ACIDITY', 1), ('JAPANESE_CURRY_ROUX', 'BITTERNESS', 2),
    ('JAPANESE_CURRY_ROUX', 'FATTINESS', 4), ('JAPANESE_CURRY_ROUX', 'UMAMI', 3),
    ('SANSHO', 'DOMINANCE', 4), ('SANSHO', 'SWEETNESS', 1), ('SANSHO', 'ACIDITY', 1),
    ('SANSHO', 'BITTERNESS', 2), ('SANSHO', 'FATTINESS', 1), ('SANSHO', 'HEAT', 3),
    ('SANSHO', 'UMAMI', 1)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('UMEBOSHI', 'PICKLED'),
    ('UMEBOSHI', 'DRIED'),
    ('SHICHIMI_TOGARASHI', 'DRIED'),
    ('YUBA', 'DRIED'),
    ('BENI_SHOGA', 'PICKLED'),
    ('AONORI', 'DRIED'),
    ('FURIKAKE', 'DRIED'),
    ('SANSHO', 'DRIED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM japan_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'JP'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM japan_existing_country
    UNION ALL
    SELECT code FROM japan_new_concept
) approved ON approved.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM japan_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

-- No season rows: all 20 new concepts use the default multiplier 1.0 in every month.
-- FERMENTED_TOFU -> JP remains deferred to the explicitly scoped follow-up after #279/C3.

DO $validation$
BEGIN
    IF (SELECT count(*)
        FROM ingredient_concept concept
        JOIN japan_new_concept approved ON approved.code = concept.code
        WHERE concept.display_name = approved.display_name
          AND concept.active
          AND concept.random_draw_enabled
          AND concept.challenge_specificity = approved.challenge_specificity
          AND concept.novelty_level = approved.novelty_level
          AND concept.base_draw_weight = approved.base_draw_weight
          AND concept.curator_note = approved.curator_note) <> 20 THEN
        RAISE EXCEPTION 'Japan curation new-concept core target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept_alias alias
        JOIN ingredient_concept concept ON concept.id = alias.ingredient_concept_id
        JOIN japan_new_alias approved
          ON approved.concept_code = concept.code
         AND approved.alias_text = alias.alias_text) <> 11 THEN
        RAISE EXCEPTION 'Japan curation alias target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_refinement refinement
        JOIN ingredient_concept parent ON parent.id = refinement.parent_concept_id
        JOIN ingredient_concept child ON child.id = refinement.child_concept_id
        JOIN japan_refinement approved
          ON approved.parent_code = parent.code
         AND approved.child_code = child.code) <> 24 THEN
        RAISE EXCEPTION 'Japan curation refinement target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_functional_role assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 37 THEN
        RAISE EXCEPTION 'Japan curation functional-role target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_dimension value
        JOIN ingredient_concept concept ON concept.id = value.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 125 THEN
        RAISE EXCEPTION 'Japan curation dimension target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_flag assignment
        JOIN ingredient_concept concept ON concept.id = assignment.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)) <> 8 THEN
        RAISE EXCEPTION 'Japan curation flag target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        JOIN japan_new_concept approved ON approved.code = concept.code
        WHERE participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = approved.availability_level
          AND availability.curator_note = approved.availability_note) <> 40 THEN
        RAISE EXCEPTION 'Japan curation availability target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_country country
        JOIN ingredient_concept concept ON concept.id = country.ingredient_concept_id
        JOIN japan_existing_country approved ON approved.code = concept.code
        WHERE country.country_code = 'JP') <> 108
       OR (SELECT count(*)
           FROM ingredient_culinary_country country
           JOIN ingredient_concept concept ON concept.id = country.ingredient_concept_id
           JOIN japan_new_concept approved ON approved.code = concept.code
           WHERE country.country_code = 'JP') <> 20 THEN
        RAISE EXCEPTION 'Japan curation country-relation target write failed';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ingredient_seasonality seasonality
        JOIN ingredient_concept concept ON concept.id = seasonality.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM japan_new_concept)
    ) THEN
        RAISE EXCEPTION 'Japan curation expected default seasonality without explicit rows';
    END IF;
END;
$validation$;
