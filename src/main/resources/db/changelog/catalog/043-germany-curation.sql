--liquibase formatted sql
--changeset venomenon328:043-germany-curation splitStatements:false
-- Issue #271 / Germany DE curation, Gate 1 and Gate 2 complete.
-- Gate 1: https://github.com/venomenon328/mise-en-dice/issues/271#issuecomment-5680456965
-- Gate 2: https://github.com/venomenon328/mise-en-dice/issues/271#issuecomment-5693882773
-- Persists only the approved Germany relations, ten approved new concepts,
-- and the approved HERRING and TEA metadata deltas.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947227);
LOCK TABLE ingredient_concept, ingredient_concept_alias, ingredient_refinement,
    ingredient_functional_role, ingredient_culinary_flag, ingredient_culinary_dimension,
    ingredient_culinary_country, ingredient_seasonality, ingredient_availability,
    exclusion_rule, exclusion_rule_target, participant, functional_role,
    culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE germany_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    challenge_specificity text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO germany_new_concept VALUES
    ('GRUENKERN', 'Grünkern', 'SPECIFIC', 3, 0.5500,
        $note$Unreif geernteter und gedarrter Dinkel mit kräftig nussigem, leicht röstigem Aroma; als ganzes Korn oder Schrot für Beilagen, Suppen und Bratlinge verwendbar. Nicht reifes Dinkelkorn, Freekeh oder eine gewürzte Bratlingsmischung.$note$,
        'PLANNED',
        $note$Im Getreide- und Biosortiment gezielt erhältlich; haltbare Trockenware lässt sich zusätzlich über allgemeinen Versandhandel ohne Kühlkette planen.$note$),
    ('LINSEED_OIL', 'Leinöl', 'SPECIFIC', 2, 0.4500,
        $note$Speiseöl aus Leinsaat mit nussigem, teils herbem Eigengeschmack, besonders für kalte Speisen und zum abschließenden Verfeinern. Gemeint ist reines Leinöl, nicht Leinsamen, ein Ölgemisch oder technisches Leinöl.$note$,
        'PLANNED',
        $note$Im gut sortierten Speiseöl- und Biosortiment gezielt suchen; ungeöffnete Flaschen sind auch über allgemeinen Paketversand planbar.$note$),
    ('PINKEL', 'Pinkel', 'SPECIFIC', 3, 0.3000,
        $note$Nordwestdeutsche geräucherte Grützwurst mit Schweinefett beziehungsweise Schweinefleisch und Hafer- oder Gerstengrütze; kräftig gewürzt und beim Erwärmen weich bis bröselig. Nicht Knipp, Blutwurst oder beliebige Grützwurst.$note$,
        'SPECIALTY',
        $note$Außerhalb des regionalen Kernsortiments vor allem über spezialisierte Fleischereien oder Versand erhältlich; Saison, Liefertermin und Kühltransport vorher klären.$note$),
    ('HESSIAN_HAND_CHEESE', 'Hessischer Handkäse', 'SPECIFIC', 3, 0.4000,
        $note$Gereifter hessischer Sauermilchkäse aus Quark mit kräftigem, säuerlich-würzigem Geschmack und sehr geringem Fettgehalt; mit oder ohne Kümmel. Nicht Harzer Käse oder beliebiger handgeformter Sauermilchkäse; eine Marinade ist nicht vorausgesetzt.$note$,
        'SPECIALTY',
        $note$Über spezialisierten Käse- oder Regionalitätenhandel gezielt beschaffbar; im gewöhnlichen Supermarkt ist die hessische Produktform nicht verlässlich erhältlich.$note$),
    ('MAULTASCHEN', 'Maultaschen', 'SPECIFIC', 2, 0.6500,
        $note$Gefüllte schwäbische Teigtaschen mit unterschiedlichen herzhaften Füllungen, auch fleischlos; frisch, tiefgekühlt oder bereits gegart verwendbar. Gemeint ist das Teigtaschenprodukt, nicht ein bestimmtes fertiges Gericht oder beliebige Ravioli und Dumplings.$note$,
        'PLANNED',
        $note$Im Kühlregal gut sortierter Supermärkte bei den frischen Teigwaren gezielt erhältlich; Auswahl und örtlicher Bestand können variieren.$note$),
    ('STOLLEN', 'Stollen', 'SPECIFIC', 4, 0.2500,
        $note$Reichhaltiges klassisches Stollengebäck aus schwerem Hefeteig, je nach Variante mit Rosinen, Mandeln oder Marzipan und meist gezuckerter Oberfläche. Als fertiges Gebäck für weitere süße Zubereitungen gemeint; nicht Panettone, gewöhnliches Früchtebrot oder Quarkstollen ohne Hefeteig.$note$,
        'EASY',
        $note$Während der Weihnachtsbacksaison im gewöhnlichen Supermarktangebot breit erhältlich; außerhalb dieses Zeitfensters nicht als verlässliche Standardware einplanen.$note$),
    ('OBAZDA', 'Obazda', 'SPECIFIC', 3, 0.4000,
        $note$Bayerische würzige Käsezubereitung aus zerdrücktem reifem Weichkäse, typischerweise mit Butter und Paprika; streichfähig und kräftig im Geschmack. Nicht der verwendete Käse allein, Frischkäse natur oder beliebiger Käseaufstrich.$note$,
        'PLANNED',
        $note$Im gekühlten Käse- und Aufstrichsortiment gut sortierter Supermärkte gezielt erhältlich; der örtliche Dauerbestand ist nicht überall verlässlich.$note$),
    ('BAVARIAN_SWEET_MUSTARD', 'süßer Senf', 'SPECIFIC', 2, 0.5000,
        $note$Milde, deutlich süße Senfzubereitung bayerischer Art mit würzigem Senfaroma; Körnung und konkrete Rezeptur bleiben offen. Nicht Honig-Senf-Dressing, scharfer Senf oder beliebiger grobkörniger Senf.$note$,
        'EASY',
        $note$Im gewöhnlichen Senfregal von Supermärkten regelmäßig erhältlich; kein Spezialgeschäft, Versand oder besondere Planung nötig.$note$),
    ('GRIEBENSCHMALZ', 'Griebenschmalz', 'SPECIFIC', 2, 0.4500,
        $note$Streichfähige Zubereitung aus Schweineschmalz mit Grieben und herzhafter Würzung; Apfel- oder Zwiebelzusätze sind möglich. Nicht reines Schweineschmalz und nicht lose Schweinegrieben.$note$,
        'PLANNED',
        $note$Im Fett- und herzhaften Aufstrichsortiment gut sortierter Supermärkte gezielt suchen; die Auswahl an Varianten und der örtliche Bestand unterscheiden sich.$note$),
    ('LEBKUCHEN', 'Lebkuchen', 'OPEN', 4, 0.3500,
        $note$Gewürzgebäck der Lebkuchen- beziehungsweise Pfefferkuchenfamilie, etwa Oblatenlebkuchen, Printen oder zum Kochen verwendeter Soßenkuchen; Teigbasis, Festigkeit und Überzug bleiben wählbar. Nicht Spekulatius, eine Gewürzmischung oder beliebiges süßes Gebäck.$note$,
        'EASY',
        $note$Während der Herbst- und Weihnachtszeit im gewöhnlichen Supermarktangebot breit erhältlich; außerhalb dieses Saisonfensters nicht als verlässliche Standardware einplanen.$note$);

CREATE TEMP TABLE germany_new_alias (
    concept_code text NOT NULL,
    alias_text text NOT NULL,
    PRIMARY KEY (concept_code, alias_text)
) ON COMMIT DROP;

INSERT INTO germany_new_alias VALUES
    ('PINKEL', 'Pinkelwurst'),
    ('STOLLEN', 'Christstollen'),
    ('STOLLEN', 'Weihnachtsstollen'),
    ('OBAZDA', 'Obatzter'),
    ('BAVARIAN_SWEET_MUSTARD', 'Weißwurstsenf'),
    ('LEBKUCHEN', 'Pfefferkuchen');

CREATE TEMP TABLE germany_refinement (
    parent_code text NOT NULL,
    child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO germany_refinement VALUES
    ('SPELT', 'GRUENKERN'),
    ('OILS', 'LINSEED_OIL'),
    ('PORK', 'PINKEL'),
    ('SAUSAGE', 'PINKEL'),
    ('CHEESE', 'HESSIAN_HAND_CHEESE'),
    ('NOODLES', 'MAULTASCHEN'),
    ('BAKED_GOODS', 'STOLLEN'),
    ('DAIRY_PRODUCTS', 'OBAZDA'),
    ('MUSTARD', 'BAVARIAN_SWEET_MUSTARD'),
    ('COOKING_FATS', 'GRIEBENSCHMALZ'),
    ('BAKED_GOODS', 'LEBKUCHEN'),
    ('CONFECTIONERY', 'LEBKUCHEN');

CREATE TEMP TABLE germany_existing_country_add (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_existing_country_add VALUES
    ('BERG_CHEESE'),
    ('BLACK_TEA'),
    ('FERMENTED_CUCUMBER'),
    ('HAM'),
    ('HERRING'),
    ('HOT_MUSTARD'),
    ('LENTILS'),
    ('MAGGI_SEASONING'),
    ('MARJORAM'),
    ('MUSSELS'),
    ('PORK'),
    ('RYE_FLOUR'),
    ('SEA_BUCKTHORN'),
    ('TRIPE'),
    ('VEAL_LUNG');

CREATE TEMP TABLE germany_existing_country_remove (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_existing_country_remove VALUES
    ('VENISON'),
    ('WHITE_WINE');

CREATE TEMP TABLE germany_required_existing (
    code text PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_required_existing
SELECT code FROM germany_existing_country_add
UNION
SELECT code FROM germany_existing_country_remove
UNION
SELECT parent_code FROM germany_refinement
UNION
SELECT 'TEA';

DO $validation$
BEGIN
    IF (SELECT count(*) FROM germany_new_concept) <> 10
       OR (SELECT count(*) FROM germany_new_alias) <> 6
       OR (SELECT count(*) FROM germany_refinement) <> 12
       OR (SELECT count(*) FROM germany_existing_country_add) <> 15
       OR (SELECT count(*) FROM germany_existing_country_remove) <> 2 THEN
        RAISE EXCEPTION 'Germany curation approval set has an unexpected cardinality';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM germany_required_existing required
        LEFT JOIN ingredient_concept concept ON concept.code = required.code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Germany curation requires existing ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM germany_new_concept proposed
        JOIN ingredient_concept existing ON existing.code = proposed.code
    ) THEN
        RAISE EXCEPTION 'Germany curation new ingredient concept already exists';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM culinary_country
        WHERE code = 'DE' AND display_name = 'Deutschland'
    ) THEN
        RAISE EXCEPTION 'Germany culinary-country reference is missing or inconsistent';
    END IF;

    IF (SELECT count(*) FROM participant WHERE code IN ('GEORGIA', 'TOBIAS')) <> 2 THEN
        RAISE EXCEPTION 'Germany curation requires the Georgia and Tobias participants';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ANIMAL_PROTEIN'), ('AROMATIC'), ('FAT'), ('SEASONING'), ('STARCH')
        ) AS required(code)
        LEFT JOIN functional_role reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Germany curation requires functional-role references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES
            ('ACIDITY'), ('BITTERNESS'), ('DOMINANCE'), ('FATTINESS'),
            ('HEAT'), ('SALTINESS'), ('SWEETNESS'), ('UMAMI')
        ) AS required(code)
        LEFT JOIN culinary_dimension reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Germany curation requires culinary-dimension references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES ('DRIED'), ('FERMENTED'), ('SMOKED')) AS required(code)
        LEFT JOIN culinary_flag reference ON reference.code = required.code
        WHERE reference.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Germany curation requires culinary-flag references that are missing';
    END IF;

    IF EXISTS (
        SELECT required.code
        FROM (VALUES ('NO_MEAT'), ('NO_PORK')) AS required(code)
        LEFT JOIN exclusion_rule rule ON rule.code = required.code
        WHERE rule.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Germany curation requires exclusion rules that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM germany_new_concept proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM germany_new_concept proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.display_name))
    ) OR EXISTS (
        SELECT 1
        FROM germany_new_alias proposed
        JOIN ingredient_concept existing
          ON lower(btrim(existing.display_name)) = lower(btrim(proposed.alias_text))
    ) OR EXISTS (
        SELECT 1
        FROM germany_new_alias proposed
        JOIN ingredient_concept_alias existing
          ON lower(btrim(existing.alias_text)) = lower(btrim(proposed.alias_text))
    ) THEN
        RAISE EXCEPTION 'Germany curation proposed names or aliases collide with the current catalog';
    END IF;

    IF EXISTS (
        SELECT normalized_name
        FROM (
            SELECT lower(btrim(display_name)) AS normalized_name
            FROM germany_new_concept
            UNION ALL
            SELECT lower(btrim(alias_text))
            FROM germany_new_alias
        ) proposed_names
        GROUP BY normalized_name
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Germany curation proposed names or aliases collide with each other';
    END IF;
END;
$validation$;

CREATE TEMP TABLE germany_existing_metadata (
    code text PRIMARY KEY,
    source_curator_note text NOT NULL,
    target_curator_note text NOT NULL,
    source_availability_level text NOT NULL,
    source_availability_note text NOT NULL,
    target_availability_level text NOT NULL,
    target_availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO germany_existing_metadata VALUES
    ('HERRING',
        $note$Hering als fettreicher Fisch; frisch oder schlicht tiefgekühlt gemeint, spezifische Reife- und Einlegeformen sind getrennt.$note$,
        $note$Hering in sämtlichen Zubereitungs- und Konservierungsformen, etwa frisch, tiefgekühlt, gereift, eingelegt, geräuchert oder als Konserve; die konkrete Produktform bleibt frei wählbar.$note$,
        'PLANNED',
        $note$An Fischtheken oder im normalen Fischfachhandel gezielt erhältlich; frische Ware schwankt regional, naturbelassene TK-Ware ist ein üblicher Ausweichweg.$note$,
        'EASY',
        $note$Konserven und eingelegte Kühlware gehören zum gewöhnlichen Supermarktsortiment; dadurch ist kein besonderer Frischfisch-Bezugsweg erforderlich.$note$),
    ('TEA',
        $note$Offene Vorgabe für Aufguss und Blattmaterial der Teepflanze; Oxidation, Röstung und Sorte bleiben wählbar.$note$,
        $note$Offene Vorgabe für Tee aus der Teepflanze sowie Kräutertee, jeweils als Aufguss oder geeignetes Pflanzenmaterial. Teesorte und Kräuterauswahl bleiben offen.$note$,
        'EASY',
        $note$Gewöhnlicher lokaler Bezugsweg über den Bereich »Tee- und Aufgussregal«.$note$,
        'EASY',
        $note$Im gewöhnlichen Tee- und Kräuterteesortiment von Supermärkten und Drogerien regelmäßig erhältlich; kein Fachgeschäft oder besonderer Beschaffungsweg nötig.$note$);

CREATE TEMP VIEW germany_existing_metadata_actual AS
SELECT metadata.*,
       concept.id AS ingredient_concept_id,
       concept.curator_note AS actual_curator_note,
       georgia.availability_level AS georgia_level,
       georgia.curator_note AS georgia_note,
       tobias.availability_level AS tobias_level,
       tobias.curator_note AS tobias_note
FROM germany_existing_metadata metadata
JOIN ingredient_concept concept ON concept.code = metadata.code
JOIN participant georgia_participant ON georgia_participant.code = 'GEORGIA'
JOIN ingredient_availability georgia
  ON georgia.ingredient_concept_id = concept.id
 AND georgia.participant_id = georgia_participant.id
JOIN participant tobias_participant ON tobias_participant.code = 'TOBIAS'
JOIN ingredient_availability tobias
  ON tobias.ingredient_concept_id = concept.id
 AND tobias.participant_id = tobias_participant.id;

DO $validation$
DECLARE conflicts text;
BEGIN
    IF (SELECT count(*) FROM germany_existing_metadata_actual) <> 2 THEN
        RAISE EXCEPTION 'Germany curation requires complete Georgia/Tobias metadata for HERRING and TEA';
    END IF;

    SELECT string_agg(code, ', ' ORDER BY code)
    INTO conflicts
    FROM germany_existing_metadata_actual actual
    WHERE NOT (
        ROW(actual_curator_note,
            georgia_level, georgia_note,
            tobias_level, tobias_note)
            IS NOT DISTINCT FROM
        ROW(source_curator_note,
            source_availability_level, source_availability_note,
            source_availability_level, source_availability_note)
        OR
        ROW(actual_curator_note,
            georgia_level, georgia_note,
            tobias_level, tobias_note)
            IS NOT DISTINCT FROM
        ROW(target_curator_note,
            target_availability_level, target_availability_note,
            target_availability_level, target_availability_note)
    );

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'Germany curation found unknown or partially installed metadata for %', conflicts;
    END IF;
END;
$validation$;

CREATE TEMP TABLE germany_metadata_changes (
    ingredient_concept_id bigint PRIMARY KEY,
    code text NOT NULL,
    target_curator_note text NOT NULL,
    target_availability_level text NOT NULL,
    target_availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO germany_metadata_changes
SELECT ingredient_concept_id, code, target_curator_note,
       target_availability_level, target_availability_note
FROM germany_existing_metadata_actual actual
WHERE ROW(actual_curator_note,
          georgia_level, georgia_note,
          tobias_level, tobias_note)
      IS NOT DISTINCT FROM
      ROW(source_curator_note,
          source_availability_level, source_availability_note,
          source_availability_level, source_availability_note)
  AND ROW(source_curator_note,
          source_availability_level, source_availability_note,
          source_availability_level, source_availability_note)
      IS DISTINCT FROM
      ROW(target_curator_note,
          target_availability_level, target_availability_note,
          target_availability_level, target_availability_note);

CREATE TEMP TABLE germany_existing_changes (
    ingredient_concept_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_existing_changes
SELECT ingredient_concept_id FROM germany_metadata_changes
UNION
SELECT concept.id
FROM germany_refinement relation
JOIN ingredient_concept concept ON concept.code = relation.parent_code
UNION
SELECT concept.id
FROM germany_existing_country_add approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE NOT EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'DE'
)
UNION
SELECT concept.id
FROM germany_existing_country_remove approved
JOIN ingredient_concept concept ON concept.code = approved.code
WHERE EXISTS (
    SELECT 1 FROM ingredient_culinary_country country
    WHERE country.ingredient_concept_id = concept.id
      AND country.country_code = 'DE'
);

CREATE TEMP TABLE germany_exclusion_rule_changes (
    exclusion_rule_id bigint PRIMARY KEY
) ON COMMIT DROP;

INSERT INTO germany_exclusion_rule_changes
SELECT id FROM exclusion_rule WHERE code IN ('NO_MEAT', 'NO_PORK');

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM germany_new_concept;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT concept.id, alias.alias_text
FROM germany_new_alias alias
JOIN ingredient_concept concept ON concept.code = alias.concept_code;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM germany_refinement relation
JOIN ingredient_concept parent ON parent.code = relation.parent_code
JOIN ingredient_concept child ON child.code = relation.child_code;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (VALUES
    ('GRUENKERN', 'STARCH'),
    ('LINSEED_OIL', 'FAT'),
    ('PINKEL', 'ANIMAL_PROTEIN'),
    ('PINKEL', 'FAT'),
    ('HESSIAN_HAND_CHEESE', 'ANIMAL_PROTEIN'),
    ('HESSIAN_HAND_CHEESE', 'SEASONING'),
    ('MAULTASCHEN', 'STARCH'),
    ('STOLLEN', 'STARCH'),
    ('OBAZDA', 'ANIMAL_PROTEIN'),
    ('OBAZDA', 'FAT'),
    ('OBAZDA', 'SEASONING'),
    ('BAVARIAN_SWEET_MUSTARD', 'SEASONING'),
    ('GRIEBENSCHMALZ', 'FAT'),
    ('LEBKUCHEN', 'STARCH'),
    ('LEBKUCHEN', 'AROMATIC')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN functional_role role ON role.code = assignment.role_code;

INSERT INTO ingredient_culinary_dimension (
    ingredient_concept_id, culinary_dimension_id, level
)
SELECT concept.id, dimension.id, assignment.level
FROM (VALUES
    ('GRUENKERN', 'DOMINANCE', 3),
    ('GRUENKERN', 'SWEETNESS', 2),
    ('GRUENKERN', 'UMAMI', 2),
    ('LINSEED_OIL', 'DOMINANCE', 3),
    ('LINSEED_OIL', 'BITTERNESS', 2),
    ('LINSEED_OIL', 'FATTINESS', 5),
    ('PINKEL', 'DOMINANCE', 4),
    ('PINKEL', 'FATTINESS', 5),
    ('PINKEL', 'HEAT', 1),
    ('PINKEL', 'UMAMI', 4),
    ('PINKEL', 'SALTINESS', 4),
    ('HESSIAN_HAND_CHEESE', 'DOMINANCE', 4),
    ('HESSIAN_HAND_CHEESE', 'ACIDITY', 3),
    ('HESSIAN_HAND_CHEESE', 'FATTINESS', 1),
    ('HESSIAN_HAND_CHEESE', 'UMAMI', 4),
    ('HESSIAN_HAND_CHEESE', 'SALTINESS', 4),
    ('MAULTASCHEN', 'DOMINANCE', 2),
    ('STOLLEN', 'DOMINANCE', 4),
    ('STOLLEN', 'SWEETNESS', 4),
    ('STOLLEN', 'FATTINESS', 4),
    ('STOLLEN', 'SALTINESS', 1),
    ('OBAZDA', 'DOMINANCE', 4),
    ('OBAZDA', 'ACIDITY', 2),
    ('OBAZDA', 'FATTINESS', 4),
    ('OBAZDA', 'HEAT', 1),
    ('OBAZDA', 'UMAMI', 4),
    ('OBAZDA', 'SALTINESS', 3),
    ('BAVARIAN_SWEET_MUSTARD', 'DOMINANCE', 3),
    ('BAVARIAN_SWEET_MUSTARD', 'SWEETNESS', 4),
    ('BAVARIAN_SWEET_MUSTARD', 'ACIDITY', 2),
    ('BAVARIAN_SWEET_MUSTARD', 'BITTERNESS', 1),
    ('BAVARIAN_SWEET_MUSTARD', 'HEAT', 1),
    ('BAVARIAN_SWEET_MUSTARD', 'SALTINESS', 1),
    ('GRIEBENSCHMALZ', 'DOMINANCE', 3),
    ('GRIEBENSCHMALZ', 'FATTINESS', 5),
    ('GRIEBENSCHMALZ', 'UMAMI', 3),
    ('GRIEBENSCHMALZ', 'SALTINESS', 2),
    ('LEBKUCHEN', 'DOMINANCE', 4),
    ('LEBKUCHEN', 'SWEETNESS', 4)
) AS assignment(concept_code, dimension_code, level)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_dimension dimension ON dimension.code = assignment.dimension_code;

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT concept.id, flag.id
FROM (VALUES
    ('GRUENKERN', 'DRIED'),
    ('PINKEL', 'SMOKED'),
    ('HESSIAN_HAND_CHEESE', 'FERMENTED'),
    ('OBAZDA', 'FERMENTED')
) AS assignment(concept_code, flag_code)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code
JOIN culinary_flag flag ON flag.code = assignment.flag_code;

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT concept.id, participant.id,
       proposed.availability_level, proposed.availability_note
FROM germany_new_concept proposed
JOIN ingredient_concept concept ON concept.code = proposed.code
CROSS JOIN participant
WHERE participant.code IN ('GEORGIA', 'TOBIAS');

INSERT INTO ingredient_seasonality (
    ingredient_concept_id, month, weight_multiplier
)
SELECT concept.id, assignment.month, assignment.weight_multiplier
FROM (VALUES
    ('STOLLEN', 1, 0.0100),
    ('STOLLEN', 2, 0.0100),
    ('STOLLEN', 3, 0.0100),
    ('STOLLEN', 4, 0.0100),
    ('STOLLEN', 5, 0.0100),
    ('STOLLEN', 6, 0.0100),
    ('STOLLEN', 7, 0.0100),
    ('STOLLEN', 8, 0.0100),
    ('STOLLEN', 9, 0.0100),
    ('STOLLEN', 10, 0.5000),
    ('STOLLEN', 11, 1.0000),
    ('STOLLEN', 12, 1.2000),
    ('LEBKUCHEN', 1, 0.4000),
    ('LEBKUCHEN', 2, 0.0500),
    ('LEBKUCHEN', 3, 0.0500),
    ('LEBKUCHEN', 4, 0.0500),
    ('LEBKUCHEN', 5, 0.0500),
    ('LEBKUCHEN', 6, 0.0500),
    ('LEBKUCHEN', 7, 0.0500),
    ('LEBKUCHEN', 8, 0.0500),
    ('LEBKUCHEN', 9, 0.6000),
    ('LEBKUCHEN', 10, 1.0000),
    ('LEBKUCHEN', 11, 1.4000),
    ('LEBKUCHEN', 12, 1.6000)
) AS assignment(concept_code, month, weight_multiplier)
JOIN ingredient_concept concept ON concept.code = assignment.concept_code;

UPDATE ingredient_concept concept
SET curator_note = changes.target_curator_note
FROM germany_metadata_changes changes
WHERE concept.id = changes.ingredient_concept_id;

UPDATE ingredient_availability availability
SET availability_level = changes.target_availability_level,
    curator_note = changes.target_availability_note
FROM germany_metadata_changes changes
WHERE availability.ingredient_concept_id = changes.ingredient_concept_id
  AND availability.participant_id IN (
      SELECT id FROM participant WHERE code IN ('GEORGIA', 'TOBIAS')
  );

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, 'DE'
FROM ingredient_concept concept
JOIN (
    SELECT code FROM germany_existing_country_add
    UNION ALL
    SELECT code FROM germany_new_concept
) approved ON approved.code = concept.code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

DELETE FROM ingredient_culinary_country country
USING ingredient_concept concept, germany_existing_country_remove approved
WHERE country.ingredient_concept_id = concept.id
  AND concept.code = approved.code
  AND country.country_code = 'DE';

INSERT INTO exclusion_rule_target (
    exclusion_rule_id, ingredient_concept_id, include_refinements
)
SELECT rule.id, concept.id, false
FROM (VALUES
    ('NO_MEAT'),
    ('NO_PORK')
) AS assignment(rule_code)
JOIN exclusion_rule rule ON rule.code = assignment.rule_code
JOIN ingredient_concept concept ON concept.code = 'GRIEBENSCHMALZ';

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM germany_existing_changes changes
WHERE concept.id = changes.ingredient_concept_id;

UPDATE exclusion_rule rule
SET version = rule.version + 1
FROM germany_exclusion_rule_changes changes
WHERE rule.id = changes.exclusion_rule_id;

DO $validation$
DECLARE conflicts text;
BEGIN
    SELECT string_agg(code, ', ' ORDER BY code)
    INTO conflicts
    FROM germany_existing_metadata_actual actual
    WHERE ROW(actual_curator_note,
              georgia_level, georgia_note,
              tobias_level, tobias_note)
          IS DISTINCT FROM
          ROW(target_curator_note,
              target_availability_level, target_availability_note,
              target_availability_level, target_availability_note);

    IF conflicts IS NOT NULL THEN
        RAISE EXCEPTION 'Germany curation metadata target write failed for %', conflicts;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT code FROM germany_existing_country_add
            UNION ALL
            SELECT code FROM germany_new_concept
        ) approved
        JOIN ingredient_concept concept ON concept.code = approved.code
        WHERE NOT EXISTS (
            SELECT 1 FROM ingredient_culinary_country country
            WHERE country.ingredient_concept_id = concept.id
              AND country.country_code = 'DE'
        )
    ) OR EXISTS (
        SELECT 1
        FROM germany_existing_country_remove approved
        JOIN ingredient_concept concept ON concept.code = approved.code
        JOIN ingredient_culinary_country country
          ON country.ingredient_concept_id = concept.id
         AND country.country_code = 'DE'
    ) THEN
        RAISE EXCEPTION 'Germany curation country-relation target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept concept
        JOIN germany_new_concept approved ON approved.code = concept.code
        WHERE concept.display_name = approved.display_name
          AND concept.active
          AND concept.random_draw_enabled
          AND concept.challenge_specificity = approved.challenge_specificity
          AND concept.novelty_level = approved.novelty_level
          AND concept.base_draw_weight = approved.base_draw_weight
          AND concept.curator_note = approved.curator_note) <> 10 THEN
        RAISE EXCEPTION 'Germany curation new-concept core target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_concept_alias alias
        JOIN ingredient_concept concept ON concept.id = alias.ingredient_concept_id
        JOIN germany_new_alias approved
          ON approved.concept_code = concept.code
         AND approved.alias_text = alias.alias_text) <> 6 THEN
        RAISE EXCEPTION 'Germany curation alias target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_refinement refinement
        JOIN ingredient_concept parent ON parent.id = refinement.parent_concept_id
        JOIN ingredient_concept child ON child.id = refinement.child_concept_id
        JOIN germany_refinement approved
          ON approved.parent_code = parent.code
         AND approved.child_code = child.code) <> 12 THEN
        RAISE EXCEPTION 'Germany curation refinement target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_availability availability
        JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
        JOIN participant ON participant.id = availability.participant_id
        JOIN germany_new_concept approved ON approved.code = concept.code
        WHERE participant.code IN ('GEORGIA', 'TOBIAS')
          AND availability.availability_level = approved.availability_level
          AND availability.curator_note = approved.availability_note) <> 20 THEN
        RAISE EXCEPTION 'Germany curation new-concept availability target write failed';
    END IF;

    IF (SELECT count(*)
        FROM ingredient_seasonality seasonality
        JOIN ingredient_concept concept ON concept.id = seasonality.ingredient_concept_id
        WHERE concept.code IN ('STOLLEN', 'LEBKUCHEN')) <> 24 THEN
        RAISE EXCEPTION 'Germany curation seasonality target write failed';
    END IF;

    IF (SELECT count(*)
        FROM exclusion_rule_target target
        JOIN exclusion_rule rule ON rule.id = target.exclusion_rule_id
        JOIN ingredient_concept concept ON concept.id = target.ingredient_concept_id
        WHERE rule.code IN ('NO_MEAT', 'NO_PORK')
          AND concept.code = 'GRIEBENSCHMALZ'
          AND NOT target.include_refinements) <> 2 THEN
        RAISE EXCEPTION 'Germany curation exclusion-rule target write failed';
    END IF;
END;
$validation$;

DROP VIEW germany_existing_metadata_actual;
