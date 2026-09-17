--liquibase formatted sql
--changeset venomenon328:045-crustacean-catalog-expansion splitStatements:false
-- Issue #277 / crustacean catalog restructuring and expansion.
-- Gate 1: https://github.com/venomenon328/mise-en-dice/issues/277#issuecomment-5701268230
-- Gate 2: https://github.com/venomenon328/mise-en-dice/issues/277#issuecomment-5711659402
-- Persists only CRUSTACEANS_A2_R3_20260916 and its unchanged R2/R1 fields.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947227);
LOCK TABLE ingredient_concept, ingredient_concept_alias, ingredient_refinement,
    ingredient_functional_role, ingredient_culinary_flag, ingredient_culinary_dimension,
    ingredient_culinary_country, ingredient_seasonality, ingredient_availability,
    exclusion_rule, exclusion_rule_target, participant, functional_role,
    culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE crustacean_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    random_draw_enabled boolean NOT NULL,
    challenge_specificity text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO crustacean_new_concept VALUES
    ('CRAYFISH_AND_LOBSTERS', 'Krebse', true, 'OPEN', 2, 0.5000,
        $note$Offene Vorgabe für Flusskrebse, Hummer, Kaisergranat, Langusten oder Bärenkrebse. Ganze Tiere, Schwänze, Scheren und ausgelöstes Fleisch sind zulässig; Krabben und Garnelen sind nicht gemeint.$note$,
        'PLANNED',
        $note$Ausgelöste Flusskrebsschwänze sind im gekühlten Sortiment gut sortierter Vollsortimenter gezielt erhältlich; andere zulässige Krebsformen erfordern teils den Fischfachhandel.$note$),
    ('EDIBLE_CRAB', 'Taschenkrebs', true, 'SPECIFIC', 2, 0.4000,
        $note$Kräftiges, leicht süßliches Fleisch. Ganze gekochte Tiere, Scheren sowie weißes oder braunes ausgelöstes Fleisch sind zulässig; bloße Krabbenaromen und Ersatzprodukte zählen nicht.$note$,
        'SPECIALTY',
        $note$Ganze Tiere, Scheren und reines Fleisch sind über mehrere Seafood-Fachhändler planbar; Frisch-, Kühl- oder TK-Versand bleibt der tragende Bezugsweg.$note$),
    ('KING_CRAB', 'Königskrabbe', true, 'SPECIFIC', 2, 0.3000,
        $note$Umfasst unter anderem Alaska- und Kamtschatka-Königskrabben mit festem, süßlichem Fleisch. Ganze Tiere, Beine, Scheren und ausgelöstes Fleisch sind zulässig; Schneekrabbe und Surimi zählen nicht.$note$,
        'SPECIALTY',
        $note$Rohe oder gegarte Beine und Scheren sind bei mehreren breit aufgestellten Seafood-Spezialhändlern mit gekühltem beziehungsweise tiefgekühltem Versand regulär erhältlich.$note$),
    ('SNOW_CRAB', 'Schneekrabbe', true, 'SPECIFIC', 3, 0.3000,
        $note$Zartes, leicht süßliches Fleisch. Ganze Tiere, Sektionen, Scheren, Beine und reines ausgelöstes Fleisch sind zulässig; Königskrabbe und Surimi zählen nicht.$note$,
        'SPECIALTY',
        $note$Sektionen, Scheren und reines Fleisch sind bei mehreren Seafood- und Delikatessenversendern als gekühlte oder tiefgekühlte Ware regulär erhältlich.$note$),
    ('BLUE_CRAB', 'Blaukrabbe', true, 'SPECIFIC', 3, 0.3000,
        $note$Ganze Tiere, Scheren und ausgelöstes Fleisch sind zulässig; weichschalige Ware nur bei eindeutiger Artangabe. Andere Arten, die ebenfalls als blaue Schwimmkrabbe gehandelt werden, zählen nicht.$note$,
        'DIFFICULT',
        $note$Artgenaue lebende Ware erscheint nur in wenigen gastronomiegeprägten Spezialsortimenten und meist auf Anfrage; Endkundenbestand und Lebendlogistik bleiben fragil.$note$),
    ('MANGROVE_CRAB', 'Mangrovenkrabben', true, 'SPECIFIC', 3, 0.3000,
        $note$Ganze Tiere, Scheren, ausgelöstes Fleisch und eindeutig artzugeordnete Softshell-Formen sind zulässig; andere Krabbenarten zählen nicht.$note$,
        'SPECIALTY',
        $note$Artgenaue Scylla-Ware ist über mehrere Seafood-Spezialsortimente als ganze tiefgekühlte Softshell-Krabbe planbar; der Kühlversand bleibt notwendig.$note$),
    ('SOFT_SHELL_CRAB', 'Softshell-Krabben', true, 'SPECIFIC', 3, 0.3000,
        $note$Ganze Krabben unmittelbar nach der Häutung besitzen einen noch weichen, vollständig essbaren Panzer. Der weiche Panzer ist eine artübergreifende Produktform; ausgelöstes Fleisch und bloße Artbezeichnungen erfüllen sie nicht.$note$,
        'SPECIALTY',
        $note$Ganze küchenfertige Softshell-Krabben werden von mehreren Seafood-Spezialhändlern als tiefgekühlte Ware geführt; der Kühlversand bleibt notwendig.$note$),
    ('SPINY_LOBSTER', 'Langusten', true, 'SPECIFIC', 2, 0.2500,
        $note$Festes, süßliches Schwanzfleisch. Ganze Tiere und rohe oder gegarte Schwänze mit oder ohne Panzer sind zulässig; Hummer, Kaisergranat und Bärenkrebse zählen nicht.$note$,
        'SPECIALTY',
        $note$Rohe tiefgekühlte Langustenschwänze sind bei mehreren Seafood- und Feinkostspezialisten regulär bestellbar; allgemeine Theken führen sie nur standortabhängig.$note$),
    ('SLIPPER_LOBSTER', 'Bärenkrebse', false, 'SPECIFIC', 4, 0.1500,
        $note$Abgeflachter Körper und essbares Schwanzfleisch. Ganze Tiere und eindeutig ausgewiesene Schwänze sind zulässig; Langusten, Hummer und Kaisergranat zählen nicht.$note$,
        'UNAVAILABLE',
        $note$Für artgenaue Bärenkrebse ist nach gezielter Suche kein realistischer wiederholbarer Endkundenweg belegt; Gastronomie-, Auslands- und bloße Handelslisten tragen keinen normalen Bezug.$note$),
    ('BLACK_TIGER_SHRIMP', 'Black Tiger Garnele', true, 'SPECIFIC', 2, 0.4500,
        $note$Ganze oder geschälte Tiere, mit oder ohne Kopf und Schale, roh oder gegart sind zulässig. Entscheidend ist eine eindeutige Artangabe; andere als Tiger-Garnele beworbene Arten zählen nicht.$note$,
        'PLANNED',
        $note$Mehrere artgenau gekennzeichnete Kühl- und TK-Produkte sind im Sortiment großer Vollsortimenter gelistet; der konkrete Filialbestand muss gezielt geprüft werden.$note$),
    ('WHITELEG_SHRIMP', 'Weißbeingarnele', true, 'SPECIFIC', 2, 0.4500,
        $note$Im Handel auch als White Tiger oder Vannamei bezeichnet. Ganze oder geschälte Tiere, roh oder gegart, sind zulässig; Black Tiger Garnelen zählen nicht.$note$,
        'PLANNED',
        $note$Artgenau als White Tiger oder Vannamei gekennzeichnete Kühl- und TK-Produkte sind im allgemeinen Vollsortiment breit gelistet, aber nicht in jeder Filiale sicher vorrätig.$note$),
    ('ARGENTINE_RED_SHRIMP', 'Argentinische Rotgarnele', true, 'SPECIFIC', 2, 0.4500,
        $note$Natürliche rote Färbung; ganze oder geschälte Tiere, mit oder ohne Kopf und Schale, roh oder gegart sind zulässig. Andere Rot- oder Tiefseegarnelen zählen nicht.$note$,
        'PLANNED',
        $note$Mehrere artgenau gekennzeichnete Eigenmarken- und Markenprodukte sind im TK-Sortiment großer Vollsortimenter gelistet; der Filialbestand bleibt standortabhängig.$note$),
    ('GIANT_RIVER_PRAWN', 'Rosenberg-Süßwassergarnele', true, 'SPECIFIC', 3, 0.3500,
        $note$Große Süßwassergarnele; ganze oder geschälte Tiere und Schwanzfleisch, roh oder gegart, sind zulässig. Andere Süßwasser- oder Riesengarnelen zählen nicht.$note$,
        'DIFFICULT',
        $note$Artgenaue rohe oder tiefgekühlte Ware ist fast nur in wenigen Großhandels- und Seafood-Spezialsortimenten sichtbar; ein robuster Endkundenweg ist nicht belegt.$note$);

CREATE TEMP TABLE crustacean_new_alias (
    concept_code text NOT NULL,
    alias_text text NOT NULL,
    PRIMARY KEY (concept_code, alias_text)
) ON COMMIT DROP;

INSERT INTO crustacean_new_alias VALUES
    ('EDIBLE_CRAB', 'Braunkrabbe'),
    ('EDIBLE_CRAB', 'Cancer pagurus'),
    ('KING_CRAB', 'King Crab'),
    ('SNOW_CRAB', 'Chionoecetes opilio'),
    ('SNOW_CRAB', 'Snow Crab'),
    ('BLUE_CRAB', 'Blue Crab'),
    ('BLUE_CRAB', 'Callinectes sapidus'),
    ('SOFT_SHELL_CRAB', 'Butterkrabben'),
    ('SOFT_SHELL_CRAB', 'Soft Shell Crabs'),
    ('SOFT_SHELL_CRAB', 'Weichschalenkrabben'),
    ('BLACK_TIGER_SHRIMP', 'Black Tiger Shrimp'),
    ('BLACK_TIGER_SHRIMP', 'Penaeus monodon'),
    ('BLACK_TIGER_SHRIMP', 'Tiger-Garnele'),
    ('BLACK_TIGER_SHRIMP', 'Tiger-Prawns'),
    ('WHITELEG_SHRIMP', 'Litopenaeus vannamei'),
    ('WHITELEG_SHRIMP', 'Penaeus vannamei'),
    ('WHITELEG_SHRIMP', 'Vannamei-Garnele'),
    ('WHITELEG_SHRIMP', 'Weißfußgarnele'),
    ('WHITELEG_SHRIMP', 'White Tiger Garnele'),
    ('ARGENTINE_RED_SHRIMP', 'Pleoticus muelleri'),
    ('GIANT_RIVER_PRAWN', 'Macrobrachium rosenbergii'),
    ('GIANT_RIVER_PRAWN', 'Rosenberggarnele');

CREATE TEMP TABLE crustacean_new_refinement (
    parent_code text NOT NULL,
    child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO crustacean_new_refinement VALUES
    ('CRUSTACEANS', 'CRAYFISH_AND_LOBSTERS'),
    ('CRAB', 'EDIBLE_CRAB'),
    ('CRAB', 'KING_CRAB'),
    ('CRAB', 'SNOW_CRAB'),
    ('CRAB', 'BLUE_CRAB'),
    ('CRAB', 'MANGROVE_CRAB'),
    ('CRAB', 'SOFT_SHELL_CRAB'),
    ('CRAYFISH_AND_LOBSTERS', 'CRAYFISH'),
    ('CRAYFISH_AND_LOBSTERS', 'LANGOUSTINE'),
    ('CRAYFISH_AND_LOBSTERS', 'LOBSTER'),
    ('CRAYFISH_AND_LOBSTERS', 'SPINY_LOBSTER'),
    ('CRAYFISH_AND_LOBSTERS', 'SLIPPER_LOBSTER'),
    ('SHRIMP', 'BLACK_TIGER_SHRIMP'),
    ('SHRIMP', 'WHITELEG_SHRIMP'),
    ('SHRIMP', 'ARGENTINE_RED_SHRIMP'),
    ('SHRIMP', 'GIANT_RIVER_PRAWN');

DO $validation$
BEGIN
    IF (SELECT count(*) FROM crustacean_new_concept) <> 13
       OR (SELECT count(*) FROM crustacean_new_alias) <> 22
       OR (SELECT count(*) FROM crustacean_new_refinement) <> 16 THEN
        RAISE EXCEPTION 'Crustacean approval set has an unexpected cardinality';
    END IF;

    IF (SELECT count(*) FROM participant WHERE code IN ('GEORGIA', 'TOBIAS')) <> 2 THEN
        RAISE EXCEPTION 'Crustacean curation requires Georgia and Tobias participants';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM functional_role WHERE code = 'ANIMAL_PROTEIN')
       OR (SELECT count(*) FROM culinary_dimension WHERE code IN ('DOMINANCE', 'SWEETNESS', 'UMAMI')) <> 3 THEN
        RAISE EXCEPTION 'Crustacean curation requires role and dimension references';
    END IF;

    IF (SELECT count(*) FROM ingredient_concept WHERE code IN (
        'CRUSTACEANS', 'CRAB', 'SHRIMP', 'CRAYFISH', 'LANGOUSTINE', 'LOBSTER'
    )) <> 6 THEN
        RAISE EXCEPTION 'Crustacean curation requires existing source concepts';
    END IF;

    IF EXISTS (
        SELECT 1 FROM crustacean_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Crustacean new concept code already exists';
    END IF;

    IF EXISTS (
        SELECT 1 FROM crustacean_new_concept proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1 FROM crustacean_new_concept proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1 FROM crustacean_new_alias proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.alias_text))
    ) OR EXISTS (
        SELECT 1 FROM crustacean_new_alias proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.alias_text))
    ) OR EXISTS (
        SELECT 1 FROM ingredient_concept existing
        LEFT JOIN ingredient_concept_alias alias ON alias.ingredient_concept_id = existing.id
        WHERE existing.code <> 'CRAB'
          AND (lower(btrim(existing.display_name)) = lower('Krabben')
               OR lower(btrim(alias.alias_text)) = lower('Krabben'))
    ) THEN
        RAISE EXCEPTION 'Crustacean proposed names or aliases collide with the current catalog';
    END IF;

    IF EXISTS (
        SELECT normalized_name FROM (
            SELECT lower(btrim(display_name)) AS normalized_name FROM crustacean_new_concept
            UNION ALL
            SELECT lower(btrim(alias_text)) FROM crustacean_new_alias
            UNION ALL
            SELECT lower('Krabben')
        ) names
        GROUP BY normalized_name
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Crustacean proposed names or aliases collide with each other';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'CRUSTACEANS'
          AND display_name = 'Krustentiere'
          AND active AND random_draw_enabled
          AND challenge_specificity = 'OPEN'
          AND base_draw_weight = 0.6000
          AND novelty_level = 2
          AND curator_note = $note$Offene Vorgabe für Krebstiere wie Garnelen, Krabben, Hummer oder Flusskrebse; nicht Muscheln oder Tintenfische.$note$
    ) OR NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'CRAB'
          AND display_name = 'Krabben oder Krebsfleisch'
          AND active AND random_draw_enabled
          AND challenge_specificity = 'SPECIFIC'
          AND base_draw_weight = 0.5500
          AND novelty_level = 2
          AND curator_note = $note$Essbares Fleisch von Krabben oder Krebsen; ganzes Tier, Scheren- oder ausgelöstes Fleisch sind möglich.$note$
    ) OR NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'SHRIMP'
          AND display_name = 'Garnelen'
          AND active AND random_draw_enabled
          AND challenge_specificity = 'SPECIFIC'
          AND base_draw_weight = 1.0000
          AND novelty_level = 1
          AND curator_note = $note$Garnelen als Krebstiere mit festem, leicht süßlichem Fleisch; Größe, Schale und frisch oder tiefgekühlt bleiben offen.$note$
    ) THEN
        RAISE EXCEPTION 'Crustacean source metadata differs from the reviewed catalog';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        WHERE concept.code = 'CRAB'
          AND participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = 'PLANNED'
          AND availability.curator_note = $note$Ausgelöstes Krebsfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$) <> 2 THEN
        RAISE EXCEPTION 'Crustacean CRAB availability source differs from the reviewed catalog';
    END IF;

    IF (SELECT array_agg(child.code ORDER BY child.code)
        FROM ingredient_refinement relation
        JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
        JOIN ingredient_concept child ON child.id = relation.child_concept_id
        WHERE parent.code = 'CRUSTACEANS')
       IS DISTINCT FROM ARRAY['CRAB','CRAYFISH','LANGOUSTINE','LOBSTER','SHRIMP']::text[]
       OR EXISTS (
           SELECT 1 FROM ingredient_refinement relation
           JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
           WHERE parent.code = 'CRAB'
       )
       OR (SELECT array_agg(child.code ORDER BY child.code)
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child ON child.id = relation.child_concept_id
           WHERE parent.code = 'SHRIMP')
          IS DISTINCT FROM ARRAY['DRIED_SHRIMP','NORTHERN_PRAWN','NORTH_SEA_SHRIMP']::text[] THEN
        RAISE EXCEPTION 'Crustacean source graph differs from the reviewed catalog';
    END IF;
END;
$validation$;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, random_draw_enabled, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM crustacean_new_concept;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT concept.id, alias.alias_text
FROM crustacean_new_alias alias
JOIN ingredient_concept concept ON concept.code = alias.concept_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM crustacean_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
JOIN functional_role role ON role.code = 'ANIMAL_PROTEIN';

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('CRAYFISH_AND_LOBSTERS', 'DOMINANCE', 3), ('CRAYFISH_AND_LOBSTERS', 'SWEETNESS', 2), ('CRAYFISH_AND_LOBSTERS', 'UMAMI', 4),
    ('EDIBLE_CRAB', 'DOMINANCE', 3), ('EDIBLE_CRAB', 'SWEETNESS', 2), ('EDIBLE_CRAB', 'UMAMI', 4),
    ('KING_CRAB', 'DOMINANCE', 3), ('KING_CRAB', 'SWEETNESS', 3), ('KING_CRAB', 'UMAMI', 4),
    ('SNOW_CRAB', 'DOMINANCE', 3), ('SNOW_CRAB', 'SWEETNESS', 3), ('SNOW_CRAB', 'UMAMI', 4),
    ('BLUE_CRAB', 'DOMINANCE', 3), ('BLUE_CRAB', 'SWEETNESS', 2), ('BLUE_CRAB', 'UMAMI', 4),
    ('MANGROVE_CRAB', 'DOMINANCE', 3), ('MANGROVE_CRAB', 'SWEETNESS', 2), ('MANGROVE_CRAB', 'UMAMI', 4),
    ('SOFT_SHELL_CRAB', 'DOMINANCE', 3), ('SOFT_SHELL_CRAB', 'SWEETNESS', 2), ('SOFT_SHELL_CRAB', 'UMAMI', 4),
    ('SPINY_LOBSTER', 'DOMINANCE', 4), ('SPINY_LOBSTER', 'SWEETNESS', 3), ('SPINY_LOBSTER', 'UMAMI', 4),
    ('SLIPPER_LOBSTER', 'DOMINANCE', 3), ('SLIPPER_LOBSTER', 'SWEETNESS', 2), ('SLIPPER_LOBSTER', 'UMAMI', 4),
    ('BLACK_TIGER_SHRIMP', 'DOMINANCE', 3), ('BLACK_TIGER_SHRIMP', 'SWEETNESS', 2), ('BLACK_TIGER_SHRIMP', 'UMAMI', 4),
    ('WHITELEG_SHRIMP', 'DOMINANCE', 3), ('WHITELEG_SHRIMP', 'SWEETNESS', 2), ('WHITELEG_SHRIMP', 'UMAMI', 4),
    ('ARGENTINE_RED_SHRIMP', 'DOMINANCE', 3), ('ARGENTINE_RED_SHRIMP', 'SWEETNESS', 2), ('ARGENTINE_RED_SHRIMP', 'UMAMI', 4),
    ('GIANT_RIVER_PRAWN', 'DOMINANCE', 3), ('GIANT_RIVER_PRAWN', 'SWEETNESS', 2), ('GIANT_RIVER_PRAWN', 'UMAMI', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id, proposed.availability_level, proposed.availability_note
FROM crustacean_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

UPDATE ingredient_concept
SET curator_note = $note$Offene Vorgabe für Krabben, Krebse und Garnelen. Ganze Tiere oder klar erkennbare Fleisch- und Produktformen einschließlich getrockneter Garnelen und Softshell-Krabben sind zulässig; Pasten, Saucen, Extrakte und Surimi zählen nicht allein über ihren Rohstoff.$note$
WHERE code = 'CRUSTACEANS';

UPDATE ingredient_concept
SET display_name = 'Krabben',
    challenge_specificity = 'OPEN',
    curator_note = $note$Offene Vorgabe einschließlich Taschen-, Königs-, Schnee-, Blau- und Mangrovenkrabben. Ganze Tiere, Scheren, Beine, ausgelöstes Fleisch und Softshell-Krabben sind zulässig; Nordseekrabben gehören zu den Garnelen, während Pasten, Saucen, Extrakte und Surimi nicht zählen.$note$
WHERE code = 'CRAB';

UPDATE ingredient_concept
SET curator_note = $note$Tiergruppe mit festem, leicht süßlichem Fleisch. Ganze oder geschälte Tiere, frisch, tiefgekühlt, gegart oder getrocknet, sind zulässig; Nordseekrabben gehören dazu, während Pasten, Saucen und Extrakte nicht allein über ihren Rohstoff zählen.$note$
WHERE code = 'SHRIMP';

UPDATE ingredient_availability availability
SET curator_note = $note$Ausgelöstes Krabbenfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$
FROM ingredient_concept concept, participant
WHERE concept.code = 'CRAB'
  AND availability.ingredient_concept_id = concept.id
  AND participant.id = availability.participant_id
  AND participant.code IN ('GEORGIA', 'TOBIAS');

DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'CRUSTACEANS'
  AND child.code IN ('CRAYFISH', 'LANGOUSTINE', 'LOBSTER');

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM crustacean_new_refinement approved
JOIN ingredient_concept parent ON parent.code = approved.parent_code
JOIN ingredient_concept child ON child.code = approved.child_code;

UPDATE ingredient_concept
SET version = version + 1
WHERE code IN ('CRUSTACEANS', 'CRAB', 'SHRIMP');

DO $validation$
BEGIN
    IF (SELECT count(*)
        FROM ingredient_concept concept
        JOIN crustacean_new_concept approved ON approved.code = concept.code
        WHERE concept.display_name = approved.display_name
          AND concept.active
          AND concept.random_draw_enabled = approved.random_draw_enabled
          AND concept.challenge_specificity = approved.challenge_specificity
          AND concept.novelty_level = approved.novelty_level
          AND concept.base_draw_weight = approved.base_draw_weight
          AND concept.curator_note = approved.curator_note) <> 13 THEN
        RAISE EXCEPTION 'Crustacean new-concept core target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept_alias alias
        JOIN ingredient_concept concept ON concept.id = alias.ingredient_concept_id
        JOIN crustacean_new_alias approved
          ON approved.concept_code = concept.code
         AND approved.alias_text = alias.alias_text) <> 22 THEN
        RAISE EXCEPTION 'Crustacean alias target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_culinary_dimension value
        JOIN ingredient_concept concept ON concept.id = value.ingredient_concept_id
        WHERE concept.code IN (SELECT code FROM crustacean_new_concept)
          AND value.culinary_dimension_id IN (
              SELECT id FROM culinary_dimension WHERE code IN ('DOMINANCE','SWEETNESS','UMAMI')
          )) <> 39 THEN
        RAISE EXCEPTION 'Crustacean dimension target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        JOIN crustacean_new_concept approved ON approved.code = concept.code
        WHERE participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = approved.availability_level
          AND availability.curator_note = approved.availability_note) <> 26 THEN
        RAISE EXCEPTION 'Crustacean availability target write failed';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'CRUSTACEANS'
          AND curator_note = $note$Offene Vorgabe für Krabben, Krebse und Garnelen. Ganze Tiere oder klar erkennbare Fleisch- und Produktformen einschließlich getrockneter Garnelen und Softshell-Krabben sind zulässig; Pasten, Saucen, Extrakte und Surimi zählen nicht allein über ihren Rohstoff.$note$
    ) OR NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'CRAB'
          AND display_name = 'Krabben'
          AND challenge_specificity = 'OPEN'
          AND curator_note = $note$Offene Vorgabe einschließlich Taschen-, Königs-, Schnee-, Blau- und Mangrovenkrabben. Ganze Tiere, Scheren, Beine, ausgelöstes Fleisch und Softshell-Krabben sind zulässig; Nordseekrabben gehören zu den Garnelen, während Pasten, Saucen, Extrakte und Surimi nicht zählen.$note$
    ) OR NOT EXISTS (
        SELECT 1 FROM ingredient_concept
        WHERE code = 'SHRIMP'
          AND challenge_specificity = 'SPECIFIC'
          AND curator_note = $note$Tiergruppe mit festem, leicht süßlichem Fleisch. Ganze oder geschälte Tiere, frisch, tiefgekühlt, gegart oder getrocknet, sind zulässig; Nordseekrabben gehören dazu, während Pasten, Saucen und Extrakte nicht allein über ihren Rohstoff zählen.$note$
    ) THEN
        RAISE EXCEPTION 'Crustacean existing-concept target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        WHERE concept.code = 'CRAB'
          AND participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = 'PLANNED'
          AND availability.curator_note = $note$Ausgelöstes Krabbenfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$) <> 2 THEN
        RAISE EXCEPTION 'Crustacean CRAB availability target write failed';
    END IF;

    IF (SELECT array_agg(child.code ORDER BY child.code)
        FROM ingredient_refinement relation
        JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
        JOIN ingredient_concept child ON child.id = relation.child_concept_id
        WHERE parent.code = 'CRUSTACEANS')
       IS DISTINCT FROM ARRAY['CRAB','CRAYFISH_AND_LOBSTERS','SHRIMP']::text[]
       OR (SELECT array_agg(child.code ORDER BY child.code)
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child ON child.id = relation.child_concept_id
           WHERE parent.code = 'CRAB')
          IS DISTINCT FROM ARRAY['BLUE_CRAB','EDIBLE_CRAB','KING_CRAB','MANGROVE_CRAB','SNOW_CRAB','SOFT_SHELL_CRAB']::text[]
       OR (SELECT array_agg(child.code ORDER BY child.code)
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child ON child.id = relation.child_concept_id
           WHERE parent.code = 'CRAYFISH_AND_LOBSTERS')
          IS DISTINCT FROM ARRAY['CRAYFISH','LANGOUSTINE','LOBSTER','SLIPPER_LOBSTER','SPINY_LOBSTER']::text[]
       OR (SELECT array_agg(child.code ORDER BY child.code)
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child ON child.id = relation.child_concept_id
           WHERE parent.code = 'SHRIMP')
          IS DISTINCT FROM ARRAY['ARGENTINE_RED_SHRIMP','BLACK_TIGER_SHRIMP','DRIED_SHRIMP','GIANT_RIVER_PRAWN','NORTHERN_PRAWN','NORTH_SEA_SHRIMP','WHITELEG_SHRIMP']::text[] THEN
        RAISE EXCEPTION 'Crustacean target graph write failed';
    END IF;
END;
$validation$;
