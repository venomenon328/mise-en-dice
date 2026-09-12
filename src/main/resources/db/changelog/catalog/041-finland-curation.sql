--liquibase formatted sql
--changeset venomenon328:041-finland-curation splitStatements:false
-- Issue #172 / Finland FI curation, both human approval gates complete.
-- Persists only the approved Finland relations, approved new concepts, and the approved GREEN_PEAS metadata/graph delta.
-- Existing overly narrow fish/berry concept descriptions are intentionally out of scope for this package.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947225);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_availability, participant, functional_role, culinary_flag,
    culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE finland_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    challenge_specificity text NOT NULL,
    active boolean NOT NULL,
    random_draw_enabled boolean NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL,
    associate_finland boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO finland_new_concept VALUES
    ('VENDACE', 'Kleine Maräne (Muikku)', 'SPECIFIC', true, true, 3, 0.5000,
        $note$Kleine Maräne (Coregonus albula), ein kleiner silbriger Süßwasserfisch mit zartem Fleisch und mildem Geschmack; in Finnland als Muikku bekannt.$note$,
        'DIFFICULT',
        $note$Der deutsche Bezug läuft über wenige Binnenfisch-Spezialwege; wechselnder Bestand und Kühlversand machen ihn fragil.$note$,
        true),
    ('LEIPAJUUSTO', 'Leipäjuusto (Brotkäse)', 'SPECIFIC', true, true, 3, 0.5500,
        $note$Finnischer gebackener Käse mit mildem Milcharoma, braun gefleckter Oberfläche und charakteristisch elastisch-quietschender Textur; auch Juustoleipä genannt.$note$,
        'DIFFICULT',
        $note$Der Bezug läuft über wenige nordische Spezialwege; Tiefkühlware wird für den Versand teils aufgetaut und erfordert eng geplante Zustellung.$note$,
        true),
    ('VIILI', 'Viili', 'SPECIFIC', true, false, 4, 0.4000,
        $note$Finnisches mild-säuerliches fermentiertes Milchprodukt mit gelartiger bis charakteristisch fädenziehender Konsistenz.$note$,
        'UNAVAILABLE',
        $note$Im deutschen und sinnvoll erreichbaren EU-Endkundenhandel ist kein wiederholbarer Kühlweg belegt.$note$,
        true),
    ('SAHTI', 'Sahti', 'SPECIFIC', true, false, 4, 0.2500,
        $note$Traditionelles finnisches ungefiltertes Starkbier auf Gerstenmalzbasis, häufig mit etwas Roggen und Wacholder und nur geringer Hopfenbittere.$note$,
        'UNAVAILABLE',
        $note$Belastbare Endkundenwege liegen im finnischen Markt; ein wiederholbarer Versand nach Deutschland ist nicht belegt.$note$,
        true),
    ('ARCTIC_CHAR', 'Arktischer Saibling', 'SPECIFIC', true, true, 3, 0.6000,
        $note$Arktischer Saibling (Salvelinus alpinus), ein lachsartiger Kaltwasserfisch mit zartem, mäßig fettem Fleisch und mildem Geschmack.$note$,
        'SPECIALTY',
        $note$Mehrere Seafood-Spezialwege führen TK-Ware; notwendiger Tiefkühlversand macht gezielte Spezialbeschaffung erforderlich.$note$,
        true),
    ('SEA_BUCKTHORN', 'Sanddornbeeren', 'SPECIFIC', true, true, 3, 0.4500,
        $note$Sanddornbeeren mit sehr ausgeprägter Säure, leichter Bitterkeit und intensiv fruchtig-herbem Aroma; auch daraus hergestellte Produkte wie Saft, Sirup, Fruchtaufstrich oder Öl sind umfasst.$note$,
        'PLANNED',
        $note$Haltbare Verarbeitungsformen sind im breiten allgemeinen Handel und Versand planbar erhältlich; frische oder tiefgekühlte Beeren erfordern dagegen Spezialwege.$note$,
        false);

CREATE TEMP TABLE finland_refinement (
    parent_code text NOT NULL,
    child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO finland_refinement VALUES
    ('WHITE_FISH', 'VENDACE'),
    ('CHEESE', 'LEIPAJUUSTO'),
    ('FRESH_DAIRY_PRODUCTS', 'LEIPAJUUSTO'),
    ('CULTURED_DAIRY', 'VIILI'),
    ('FRESH_DAIRY_PRODUCTS', 'VIILI'),
    ('BEER', 'SAHTI'),
    ('OILY_FISH', 'ARCTIC_CHAR'),
    ('BERRIES', 'SEA_BUCKTHORN'),
    ('GREEN_PEAS', 'GREEN_SPLIT_PEAS');

CREATE TEMP TABLE finland_existing_country (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO finland_existing_country VALUES
    ('RYE_BREAD'),
    ('RYE_FLOUR'),
    ('OATS'),
    ('NEW_POTATOES'),
    ('HERRING'),
    ('PICKLED_HERRING'),
    ('SALMON'),
    ('SMOKED_SALMON'),
    ('GRAVLAX'),
    ('DILL'),
    ('REINDEER'),
    ('LINGONBERRY'),
    ('LINGONBERRY_PRESERVES'),
    ('CLOUDBERRY'),
    ('CLOUDBERRY_PRESERVES'),
    ('BLUEBERRY'),
    ('CRAYFISH'),
    ('CARDAMOM'),
    ('SALTY_LIQUORICE'),
    ('RUTABAGA'),
    ('BUTTERMILK'),
    ('GREEN_SPLIT_PEAS');

CREATE TEMP TABLE finland_required_existing (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO finland_required_existing
SELECT code FROM finland_existing_country
UNION
SELECT code FROM (VALUES
    ('WHITE_FISH'),
    ('CHEESE'),
    ('FRESH_DAIRY_PRODUCTS'),
    ('CULTURED_DAIRY'),
    ('BEER'),
    ('OILY_FISH'),
    ('BERRIES'),
    ('POD_VEGETABLES'),
    ('GREEN_PEAS'),
    ('GREEN_SPLIT_PEAS')
) AS required(code);

DO $validation$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM finland_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Finland curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM finland_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Finland curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'FI' AND display_name = 'Finnland'
    ) THEN
        RAISE EXCEPTION 'Finland culinary-country reference is missing or inconsistent';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM ingredient_refinement relation
        JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
        JOIN ingredient_concept child ON child.id = relation.child_concept_id
        WHERE parent.code = 'POD_VEGETABLES'
          AND child.code = 'GREEN_PEAS'
    ) THEN
        RAISE EXCEPTION 'Finland curation expected POD_VEGETABLES -> GREEN_PEAS refinement is missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM ingredient_refinement relation
        JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
        JOIN ingredient_concept child ON child.id = relation.child_concept_id
        WHERE parent.code = 'GREEN_PEAS'
          AND child.code = 'GREEN_SPLIT_PEAS'
    ) THEN
        RAISE EXCEPTION 'Finland curation expected GREEN_PEAS -> GREEN_SPLIT_PEAS refinement to be absent';
    END IF;
END;
$validation$;

CREATE TEMP TABLE finland_existing_before ON COMMIT DROP AS
SELECT concept.id, concept.code, concept.version
FROM ingredient_concept concept
JOIN finland_required_existing required ON required.code = concept.code;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, active, random_draw_enabled, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM finland_new_concept;

UPDATE ingredient_concept
SET curator_note =
    'Grüne Erbsen unabhängig von der Produktform; frisch, tiefgekühlt, getrocknet, gespalten oder schlicht vorgegart beziehungsweise konserviert sind umfasst. Gelbe Erbsen sind nicht gemeint.'
WHERE code = 'GREEN_PEAS';

DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'POD_VEGETABLES'
  AND child.code = 'GREEN_PEAS';

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM finland_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('VENDACE', 'ANIMAL_PROTEIN'),
    ('LEIPAJUUSTO', 'ANIMAL_PROTEIN'),
    ('LEIPAJUUSTO', 'FAT'),
    ('LEIPAJUUSTO', 'SEASONING'),
    ('VIILI', 'ACID'),
    ('VIILI', 'FAT'),
    ('VIILI', 'SEASONING'),
    ('SAHTI', 'ACID'),
    ('SAHTI', 'SEASONING'),
    ('ARCTIC_CHAR', 'ANIMAL_PROTEIN'),
    ('SEA_BUCKTHORN', 'ACID'),
    ('SEA_BUCKTHORN', 'FRUIT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('VENDACE', 'DOMINANCE', 3),
    ('VENDACE', 'FATTINESS', 2),
    ('VENDACE', 'UMAMI', 3),
    ('LEIPAJUUSTO', 'DOMINANCE', 3),
    ('LEIPAJUUSTO', 'FATTINESS', 4),
    ('LEIPAJUUSTO', 'UMAMI', 3),
    ('LEIPAJUUSTO', 'SALTINESS', 2),
    ('VIILI', 'DOMINANCE', 2),
    ('VIILI', 'ACIDITY', 3),
    ('VIILI', 'FATTINESS', 2),
    ('VIILI', 'SALTINESS', 1),
    ('SAHTI', 'DOMINANCE', 4),
    ('SAHTI', 'SWEETNESS', 3),
    ('SAHTI', 'BITTERNESS', 1),
    ('ARCTIC_CHAR', 'DOMINANCE', 3),
    ('ARCTIC_CHAR', 'FATTINESS', 3),
    ('ARCTIC_CHAR', 'UMAMI', 4),
    ('SEA_BUCKTHORN', 'DOMINANCE', 4),
    ('SEA_BUCKTHORN', 'SWEETNESS', 1),
    ('SEA_BUCKTHORN', 'ACIDITY', 5),
    ('SEA_BUCKTHORN', 'BITTERNESS', 2)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('VIILI', 'FERMENTED'),
    ('SAHTI', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM finland_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'FI'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM finland_existing_country
    UNION ALL
    SELECT code FROM finland_new_concept WHERE associate_finland
) assignment ON assignment.code = concept.code;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM finland_existing_before old
WHERE concept.id = old.id;

-- No season rows: all six new concepts use the default multiplier 1.0 in every month.
-- SEA_BUCKTHORN deliberately remains non-seasonal because its approved concept includes durable processed forms.
