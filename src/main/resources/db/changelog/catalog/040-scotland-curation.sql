--liquibase formatted sql
--changeset venomenon328:040-scotland-curation splitStatements:false
-- Issue #172 / Scotland GB-SCT curation, both human approval gates complete.
-- Persists only the approved Scotland relations and fully approved new concepts.
-- The four generic Mutton refinements deliberately receive no Scotland relation.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947224);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_availability, participant, functional_role, culinary_flag,
    culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE scotland_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    challenge_specificity text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL,
    associate_scotland boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO scotland_new_concept VALUES
    ('WHISKY', 'Whisky', 'SPECIFIC', 4, 0.3000,
        $note$Whisky/Whiskey als fassgereifte Getreidespirituose; Scotch, Irish Whiskey, Bourbon, Rye und vergleichbare Stile sind zulässig. Whisky-Liköre und aromatisierte Spirituosenmischungen sind nicht gemeint.$note$,
        'EASY',
        $note$Mehrere Marken und Stilrichtungen gehören zum gewöhnlichen Spirituosenregal großer Supermärkte und bieten eine alltägliche Ausweichquelle.$note$,
        true),
    ('HAGGIS', 'Haggis', 'SPECIFIC', 4, 0.4500,
        $note$Herzhafte schottische Fleisch- und Innereienzubereitung mit Hafer, Fett, Zwiebel und Gewürzen, traditionell in einer Hülle gegart. Moderne Fleischvarianten in Kunstdarm, Dose oder Glas sind zulässig; vegetarisches Haggis nicht.$note$,
        'SPECIALTY',
        $note$Haltbare Fleischvarianten sind über mehrere unabhängige britisch-schottische Spezialwege sowie einen deutschen Produzenten planbar erhältlich; allgemeiner Handel führt sie nicht verlässlich.$note$,
        true),
    ('SMOKED_HADDOCK', 'geräucherter Schellfisch', 'SPECIFIC', 3, 0.5500,
        $note$Geräucherter Schellfisch mit fester, blättriger Textur und deutlich salzig-rauchigem Aroma; Filets und größere Stücke, gekühlt oder tiefgekühlt, sind zulässig. Geräucherter Kabeljau, Seelachs und andere Räucherfische sind nicht gemeint.$note$,
        'DIFFICULT',
        $note$Nur wenige Räucherfisch- und Seafood-Spezialwege führen die genaue Fischart; wechselnder Bestand und notwendiger Kühlversand machen den Bezug fragil.$note$,
        true),
    ('MUTTON', 'Mutton (Fleisch ausgewachsener Schafe)', 'OPEN', 3, 0.5500,
        $note$Rohes Fleisch ausgewachsener Schafe mit kräftigerem Eigengeschmack als Lamm. Unterschiedliche unverarbeitete Zuschnitte und Hackfleisch sind zulässig; Lammfleisch sowie Wurst- und Fertigprodukte sind nicht gemeint.$note$,
        'DIFFICULT',
        $note$Fleisch ausgewachsener Schafe ist nur über wenige Direktvermarkter und Spezialfleischwege erhältlich; Schlachttermine, Vorbestellung und Kühlversand machen den Bezug wenig robust.$note$,
        true),
    ('LANGOUSTINE', 'Kaisergranat', 'SPECIFIC', 3, 0.4500,
        $note$Kaisergranat (Nephrops norvegicus), ganz oder als Schwanzfleisch, frisch oder tiefgekühlt. Handelsbezeichnungen wie Langoustine oder Scampi sind nur umfasst, wenn tatsächlich Kaisergranat gemeint ist; Garnelen, Flusskrebse und Hummer sind keine Ersatzform.$note$,
        'SPECIALTY',
        $note$Frischer oder tiefgekühlter Kaisergranat ist bei mehreren Seafood-Spezialhändlern zuverlässig erhältlich; Kühl- beziehungsweise TK-Logistik macht den Spezialweg erforderlich.$note$,
        true),
    ('LORNE_SAUSAGE', 'Lorne Sausage', 'SPECIFIC', 3, 0.5500,
        $note$Schottische, nicht in Darm gefüllte Wurst aus gewürztem Hackfleisch, als kompakter Block geformt und typischerweise in quadratische oder rechteckige Scheiben geschnitten. Rind-, Schweine- und Mischfleischvarianten sind zulässig; vegetarische Square Sausage, gewöhnliche Bratwurst und Burger-Patties nicht.$note$,
        'DIFFICULT',
        $note$Die frische Wurst ist nur über wenige britisch-schottische Spezialwege erhältlich; gekühlter Auslandsversand macht die Beschaffung eng und logistisch empfindlich.$note$,
        true),
    ('SCOTTISH_OATCAKES', 'schottische Oatcakes', 'SPECIFIC', 3, 0.6500,
        $note$Flaches schottisches Haferbrot beziehungsweise -gebäck, traditionell aus Hafermehl oder Haferflocken und meist dünn sowie fest bis knusprig. Weiche Staffordshire-/Derbyshire-Oatcakes und süße Haferkekse sind nicht gemeint.$note$,
        'PLANNED',
        $note$Schottische Oatcakes sind bei gut sortierten allgemeinen Händlern und im breiten Versandhandel als haltbare Packungsware erhältlich, lokal aber kein verlässlicher Standard.$note$,
        true),
    ('MUTTON_LEG', 'Mutton-Keule', 'SPECIFIC', 3, 0.6500,
        $note$Keule vom ausgewachsenen Schaf, mit oder ohne Knochen; ganze Bratenstücke sowie daraus geschnittene Steaks oder Würfel sind zulässig. Lammkeule ist nicht gemeint.$note$,
        'DIFFICULT',
        $note$Keulenstücke vom ausgewachsenen Schaf sind nur bei wenigen Direktvermarktern erhältlich; Vorbestellung, Schlachttermine und Kühlweg machen den Bezug wenig robust.$note$,
        false),
    ('MUTTON_SHOULDER', 'Mutton-Schulter', 'SPECIFIC', 3, 0.6500,
        $note$Schulter beziehungsweise Bug vom ausgewachsenen Schaf, mit oder ohne Knochen und als Ganzstück oder zugeschnitten. Lammschulter ist nicht gemeint.$note$,
        'DIFFICULT',
        $note$Schulterstücke vom ausgewachsenen Schaf sind nur bei wenigen Direktvermarktern erhältlich; Vorbestellung, Schlachttermine und Kühlweg machen den Bezug wenig robust.$note$,
        false),
    ('MUTTON_CHOP', 'Mutton-Kotelett', 'SPECIFIC', 3, 0.5500,
        $note$Kotelett aus dem Rückenbereich eines ausgewachsenen Schafs, typischerweise mit Knochen. Lammkotelett ist nicht gemeint.$note$,
        'DIFFICULT',
        $note$Koteletts vom ausgewachsenen Schaf sind nur punktuell über Direktvermarkter und Spezialfleischwege erhältlich; exakter Zuschnitt und Kühlweg müssen vorab geklärt werden.$note$,
        false),
    ('MUTTON_MINCE', 'Mutton-Hackfleisch', 'SPECIFIC', 3, 0.6500,
        $note$Ungewürztes Hackfleisch aus Fleisch ausgewachsener Schafe. Lammhack, gemischtes Hack sowie bereits gewürzte Patties oder Wurstmasse sind nicht gemeint.$note$,
        'DIFFICULT',
        $note$Reines Hackfleisch vom ausgewachsenen Schaf wird nur punktuell von Spezialbetrieben angeboten; Schlachttermine, Vorbestellung und Kühlweg machen den Bezug wenig robust.$note$,
        false);

CREATE TEMP TABLE scotland_refinement (
    parent_code text NOT NULL,
    child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO scotland_refinement VALUES
    ('COOKING_ALCOHOL', 'WHISKY'),
    ('OFFAL', 'HAGGIS'),
    ('HADDOCK', 'SMOKED_HADDOCK'),
    ('PRESERVED_FISH', 'SMOKED_HADDOCK'),
    ('MEAT', 'MUTTON'),
    ('MUTTON', 'MUTTON_LEG'),
    ('MUTTON', 'MUTTON_SHOULDER'),
    ('MUTTON', 'MUTTON_CHOP'),
    ('MUTTON', 'MUTTON_MINCE'),
    ('MINCED_MEAT', 'MUTTON_MINCE'),
    ('CRUSTACEANS', 'LANGOUSTINE'),
    ('SAUSAGE', 'LORNE_SAUSAGE'),
    ('FLATBREAD', 'SCOTTISH_OATCAKES');

CREATE TEMP TABLE scotland_existing_country (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO scotland_existing_country VALUES
    ('OATS'),
    ('BARLEY'),
    ('POTATO'),
    ('NEW_POTATOES'),
    ('RUTABAGA'),
    ('HADDOCK'),
    ('SALMON'),
    ('SMOKED_SALMON'),
    ('BLACK_PUDDING'),
    ('LAMB'),
    ('RASPBERRY'),
    ('VENISON'),
    ('SCALLOPS'),
    ('OYSTER'),
    ('LOBSTER');

CREATE TEMP TABLE scotland_required_existing (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO scotland_required_existing VALUES
    ('COOKING_ALCOHOL'),
    ('OFFAL'),
    ('HADDOCK'),
    ('PRESERVED_FISH'),
    ('MEAT'),
    ('MINCED_MEAT'),
    ('CRUSTACEANS'),
    ('SAUSAGE'),
    ('FLATBREAD'),
    ('OATS'),
    ('BARLEY'),
    ('POTATO'),
    ('NEW_POTATOES'),
    ('RUTABAGA'),
    ('SALMON'),
    ('SMOKED_SALMON'),
    ('BLACK_PUDDING'),
    ('LAMB'),
    ('RASPBERRY'),
    ('VENISON'),
    ('SCALLOPS'),
    ('OYSTER'),
    ('LOBSTER');

DO $validation$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM scotland_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Scotland curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM scotland_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Scotland curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'GB-SCT' AND display_name = 'Schottland'
    ) THEN
        RAISE EXCEPTION 'Scotland culinary-country reference is missing or inconsistent';
    END IF;
END;
$validation$;

CREATE TEMP TABLE scotland_existing_before ON COMMIT DROP AS
SELECT concept.id, concept.code, concept.version
FROM ingredient_concept concept
JOIN scotland_required_existing required ON required.code = concept.code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM scotland_new_concept;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM scotland_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('WHISKY', 'ACID'),
    ('WHISKY', 'SEASONING'),
    ('HAGGIS', 'ANIMAL_PROTEIN'),
    ('HAGGIS', 'FAT'),
    ('SMOKED_HADDOCK', 'ANIMAL_PROTEIN'),
    ('SMOKED_HADDOCK', 'SEASONING'),
    ('MUTTON', 'ANIMAL_PROTEIN'),
    ('MUTTON_LEG', 'ANIMAL_PROTEIN'),
    ('MUTTON_SHOULDER', 'ANIMAL_PROTEIN'),
    ('MUTTON_SHOULDER', 'FAT'),
    ('MUTTON_CHOP', 'ANIMAL_PROTEIN'),
    ('MUTTON_CHOP', 'FAT'),
    ('MUTTON_MINCE', 'ANIMAL_PROTEIN'),
    ('LANGOUSTINE', 'ANIMAL_PROTEIN'),
    ('LORNE_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('LORNE_SAUSAGE', 'FAT'),
    ('SCOTTISH_OATCAKES', 'STARCH')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('WHISKY', 'DOMINANCE', 4),
    ('WHISKY', 'SWEETNESS', 2),
    ('WHISKY', 'BITTERNESS', 2),
    ('HAGGIS', 'DOMINANCE', 4),
    ('HAGGIS', 'FATTINESS', 4),
    ('HAGGIS', 'UMAMI', 4),
    ('HAGGIS', 'SALTINESS', 3),
    ('SMOKED_HADDOCK', 'DOMINANCE', 4),
    ('SMOKED_HADDOCK', 'FATTINESS', 1),
    ('SMOKED_HADDOCK', 'UMAMI', 4),
    ('SMOKED_HADDOCK', 'SALTINESS', 4),
    ('MUTTON', 'DOMINANCE', 4),
    ('MUTTON', 'FATTINESS', 3),
    ('MUTTON', 'UMAMI', 4),
    ('MUTTON_LEG', 'DOMINANCE', 4),
    ('MUTTON_LEG', 'FATTINESS', 3),
    ('MUTTON_LEG', 'UMAMI', 4),
    ('MUTTON_SHOULDER', 'DOMINANCE', 4),
    ('MUTTON_SHOULDER', 'FATTINESS', 4),
    ('MUTTON_SHOULDER', 'UMAMI', 4),
    ('MUTTON_CHOP', 'DOMINANCE', 4),
    ('MUTTON_CHOP', 'FATTINESS', 4),
    ('MUTTON_CHOP', 'UMAMI', 4),
    ('MUTTON_MINCE', 'DOMINANCE', 4),
    ('MUTTON_MINCE', 'FATTINESS', 3),
    ('MUTTON_MINCE', 'UMAMI', 4),
    ('LANGOUSTINE', 'DOMINANCE', 3),
    ('LANGOUSTINE', 'SWEETNESS', 2),
    ('LANGOUSTINE', 'UMAMI', 4),
    ('LORNE_SAUSAGE', 'DOMINANCE', 3),
    ('LORNE_SAUSAGE', 'FATTINESS', 4),
    ('LORNE_SAUSAGE', 'UMAMI', 4),
    ('LORNE_SAUSAGE', 'SALTINESS', 3),
    ('SCOTTISH_OATCAKES', 'DOMINANCE', 2),
    ('SCOTTISH_OATCAKES', 'SWEETNESS', 1),
    ('SCOTTISH_OATCAKES', 'FATTINESS', 3),
    ('SCOTTISH_OATCAKES', 'SALTINESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('SMOKED_HADDOCK', 'SMOKED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM scotland_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'GB-SCT'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM scotland_existing_country
    UNION ALL
    SELECT code FROM scotland_new_concept WHERE associate_scotland
) assignment ON assignment.code = concept.code;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM scotland_existing_before old
WHERE concept.id = old.id;

-- No season rows: all approved concepts use the default multiplier 1.0 in every month.
