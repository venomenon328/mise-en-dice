--liquibase formatted sql
--changeset venomenon328:034-austria-curation splitStatements:false
-- Issue #172 / AT_METADATA_V2_20260908, approved in the Austria review.
-- Both human approval gates are complete, including the later note/name/rating
-- corrections and the additional PUMPKIN_SEED_OIL parent. BEEF and COFFEE do not
-- receive AT; the three explicitly accepted borderline associations are included.
-- Keep all published changesets and the #188/#189 review artifacts unchanged.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947221);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_seasonality, ingredient_availability, participant,
    functional_role, culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE austria_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL,
    associate_at boolean NOT NULL
) ON COMMIT DROP;

INSERT INTO austria_new_concept VALUES
    ('PUMPKIN_SEED_OIL', 'Kürbiskernöl', 2, 0.5000,
        'Speiseöl aus Kürbiskernen, je nach Herstellung mild-kernig oder deutlich nussig mit Röstaromen. Sowohl Öl aus ungerösteten als auch aus gerösteten Kernen ist gemeint.',
        'PLANNED', 'Reines Kürbiskernöl ist im gut sortierten Lebensmittelhandel und über deutsche Anbieter in haltbaren Kleinflaschen erhältlich; ungekühlter Paketversand erleichtert den Bezug.', false),
    ('ROASTED_PUMPKIN_SEED_OIL', 'Kürbiskernöl, geröstet', 2, 0.5500,
        'Dunkles Öl aus gerösteten Kürbiskernen mit ausgeprägtem nussigem Röstaroma. Besonders prägend als abschließendes Würzöl; ungeröstetes Kürbiskernöl und Mischöle sind nicht gemeint.',
        'PLANNED', 'In gut sortierten allgemeinen Ölregalen und im regulären deutschen Onlinehandel erhältlich; die geröstete Form wird ausdrücklich ausgezeichnet und in haltbaren Kleinflaschen angeboten.', true),
    ('RUNNER_BEANS', 'Käferbohnen', 3, 0.7500,
        'Große, ausgereifte Kerne der Feuerbohne mit kräftiger Schale und cremigem Inneren. Gemeint sind getrocknete oder schlicht gekochte Käferbohnen, nicht die grünen Hülsen.',
        'SPECIALTY', 'Getrocknete Kerne erfordern gezielten Feinkostbezug; der belegte deutsche Versand bietet ein lagerfähiges 1-kg-Gebinde. Ein verlässlicher örtlicher Einkaufsweg ist nicht belegt.', true),
    ('BERG_CHEESE', 'Bergkäse', 2, 0.6000,
        'Gereifter alpiner Hartkäse aus Kuhmilch mit kräftigem, nussigem Geschmack. Je nach Reifegrad eignet er sich zum Schmelzen, Reiben oder als deutlich schmeckende Käsekomponente.',
        'EASY', 'Abgepackte Stücke gehören zur gewöhnlichen Supermarkt- und Discounterkäseauswahl; mehrere Marken und Eigenmarken bieten alltägliche Ausweichquellen.', true),
    ('TIROLER_GRAUKAESE', 'Tiroler Graukäse', 3, 0.4000,
        'Fettarmer Tiroler Sauermilchkäse mit kräftiger Reifewürze und bröckeliger bis geschmeidiger Konsistenz. Er bringt einen deutlich säuerlich-würzigen Käsecharakter ein.',
        'SPECIALTY', 'Die genaue Tiroler Sorte ist über ausgewählte deutsche Käsefachhändler erhältlich; gezielte Bestellung und gekühlte Annahme sind nötig, ein örtlicher Standardbezug ist nicht belegt.', true),
    ('STRUDEL_DOUGH', 'Strudelteig', 2, 0.6500,
        'Sehr dünn gezogener, ungefüllter Weizenteig für süße und herzhafte Strudel. Die feinen Blätter werden um Füllungen gelegt oder geschichtet; Blätterteig ist nicht gemeint.',
        'PLANNED', 'Dünne ungefüllte Strudelteigblätter werden von mehreren Marken im allgemeinen Kühlteigsortiment geführt; gezielter Einkauf in einem gut sortierten Markt.', true),
    ('BEEF_TAFELSPITZ', 'Tafelspitz', 2, 0.7000,
        'Spitz zulaufendes Teilstück aus dem Hüftdeckel des Rindes, häufig mit aufliegender Fettschicht. Gemeint ist das rohe Fleischstück, besonders zum Sieden oder Schmoren, nicht das fertige Gericht.',
        'SPECIALTY', 'Der genaue rohe Zuschnitt erfordert eine gezielte Metzgereianfrage oder Bestellung im Fleischfachversand; Stückgröße und gekühlte Warenannahme müssen passen.', true),
    ('APRICOT_PRESERVES', 'Aprikosenkonfitüre', 2, 0.5500,
        'Süß-säuerliche, eingekochte Aprikosenzubereitung, glatt passiert oder mit Fruchtstücken. Als Füllung, Glasur oder fruchtige Würzkomponente verwendbar.',
        'EASY', 'Eine gewöhnliche Sorte im Konfitürenregal von Supermärkten und Discountern; Marken und Eigenmarken bieten haltbare Gläser ohne Importbedarf.', true),
    ('TIROLER_SPECK', 'Tiroler Speck', 2, 0.5000,
        'Gepökelter, kaltgeräucherter und getrockneter Tiroler Speck aus unterschiedlichen Schweinefleischteilstücken. Rauch, Salz und Reifearoma prägen ihn; der Fettanteil hängt stark vom Zuschnitt ab.',
        'PLANNED', 'Originale Tiroler Varianten sind im gut sortierten allgemeinen Wursthandel erhältlich; Stücke und Aufschnitt ermöglichen gezielte Beschaffung ohne engen Importweg.', true),
    ('EGG_FLECKERL', 'Eierfleckerl', 3, 0.6500,
        'Kleine, annähernd quadratische Eiernudeln mit guter Haftfläche für fein geschnittene Begleiter. Sie behalten in Pfannengerichten und Aufläufen ihre charakteristische kompakte Form.',
        'SPECIALTY', 'Die genaue Eiernudelform ist über österreichischen Lebensmittelversand in haltbaren Haushaltspackungen erhältlich; ein verlässlicher deutscher Ladenbezug ist nicht belegt.', true),
    ('VEAL_LUNG', 'Kalbslunge', 4, 0.6000,
        'Kalbslunge ist eine leichte, poröse Innerei mit eigenständiger elastischer Textur. Sie wird vor der weiteren Verarbeitung gegart; gemeint ist das rohe Lebensmittel, nicht fertiges Beuschel.',
        'DIFFICULT', 'Nur wenige Metzgereiversender bieten rohe essbare Ware; kurze Haltbarkeit und abgestimmter Kühlversand begrenzen den Bezug. Tierfutter und fertiges Beuschel zählen nicht.', true),
    ('WIENER_SAUSAGE', 'Wiener Würstchen', 1, 0.6500,
        'Dünne, fein gekutterte und geräucherte Brühwürstchen aus Schweine- und/oder Rindfleisch. Sie bringen mildes Raucharoma, Salz und eine elastische Wursttextur in warme Gerichte ein.',
        'EASY', 'Abgepackte Varianten aus Schweine- oder Rindfleisch gehören zum gewöhnlichen Wurstsortiment von Supermärkten und Discountern; zahlreiche alltägliche Ausweichprodukte.', true),
    ('KAESEKRAINER', 'Käsekrainer', 3, 0.5000,
        'Grobe geräucherte Brühwurst mit eingearbeiteten Käsestücken. Beim Erhitzen verbindet sich die würzige Wurst mit schmelzendem Käse; eine beliebige Käsegrillwurst ist nicht gemeint.',
        'PLANNED', 'Mehrere Hersteller führen die geräucherte Wurst mit Käseeinlage im allgemeinen Lebensmittelhandel; gezielter Einkauf im gut sortierten Kühlregal.', true),
    ('PORK_CRACKLINGS', 'Schweinegrieben', 3, 0.4500,
        'Knusprige bis mürbe Stückchen, die beim Auslassen von Schweinespeck entstehen. Sie liefern Röstaroma und Fett; gemeint sind die festen Grieben, nicht Griebenschmalz oder gepuffte Schweineschwarten.',
        'SPECIALTY', 'Feste Grieben sind über gezielten Spezialitätenbezug erhältlich; der belegte österreichische Versand verlangt Kühlung. Griebenschmalz und gepuffte Schwarten erfüllen die Vorgabe nicht.', true),
    ('SOFT_WHEAT_SEMOLINA', 'Weichweizengrieß', 1, 0.7000,
        'Grieß aus Weichweizen, der beim Garen weich und cremig quillt. Besonders für Grießspeisen und zarte gebundene Massen geeignet; Hartweizengrieß ist nicht gemeint.',
        'EASY', 'Gewöhnliche haltbare Eigenmarkenware im Grieß- und Backzutatenregal von Supermärkten und Discountern; die Weichweizenform ist ausdrücklich ausgezeichnet.', false);

CREATE TEMP TABLE austria_refinement (
    parent_code text NOT NULL, child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;
INSERT INTO austria_refinement VALUES
    ('OILS', 'PUMPKIN_SEED_OIL'),
    ('PUMPKIN_SEED_OIL', 'ROASTED_PUMPKIN_SEED_OIL'),
    ('BEANS', 'RUNNER_BEANS'),
    ('CHEESE', 'BERG_CHEESE'),
    ('CHEESE', 'TIROLER_GRAUKAESE'),
    ('DUMPLING_WRAPPERS', 'STRUDEL_DOUGH'),
    ('BEEF', 'BEEF_TAFELSPITZ'),
    ('PRESERVED_PRODUCE', 'APRICOT_PRESERVES'),
    ('CURED_MEAT', 'TIROLER_SPECK'),
    ('PORK', 'TIROLER_SPECK'),
    ('EGG_NOODLES', 'EGG_FLECKERL'),
    ('VEAL', 'VEAL_LUNG'),
    ('OFFAL', 'VEAL_LUNG'),
    ('SAUSAGE', 'WIENER_SAUSAGE'),
    ('SAUSAGE', 'KAESEKRAINER'),
    ('PORK', 'PORK_CRACKLINGS'),
    ('GRAINS', 'SOFT_WHEAT_SEMOLINA');

-- Original recommendations, minus BEEF/COFFEE, plus the accepted borderline cases.
CREATE TEMP TABLE austria_existing_country (code text PRIMARY KEY) ON COMMIT DROP;
INSERT INTO austria_existing_country VALUES
    ('VEAL_CUTLET'),
    ('BEEF_GOULASH'),
    ('HORSERADISH'),
    ('QUARK'),
    ('APRICOT'),
    ('POPPY_SEEDS'),
    ('PLUM_BUTTER'),
    ('BREADCRUMBS'),
    ('BREAD_DUMPLING'),
    ('POTATO_DUMPLING'),
    ('FRUIT_DUMPLING'),
    ('SPAETZLE'),
    ('CARAWAY'),
    ('MARJORAM'),
    ('SWEET_PAPRIKA_POWDER'),
    ('LEBERKAESE'),
    ('BLOOD_SAUSAGE'),
    ('CHANTERELLE'),
    ('PORCINI'),
    ('CARP'),
    ('SAUERKRAUT'),
    ('LINGONBERRY_PRESERVES'),
    ('RAISIN'),
    ('LARD');

-- Preserve the old versions and identities before attaching children/countries.
CREATE TEMP TABLE austria_existing_before ON COMMIT DROP AS
SELECT ic.* FROM ingredient_concept ic
WHERE ic.code IN (
    SELECT parent_code FROM austria_refinement
    UNION SELECT code FROM austria_existing_country
);

-- Runtime audit snapshots use the existing CatalogIngredientSnapshotFactory
-- field names. These are observed states, not frozen content expectations.
CREATE TEMP VIEW austria_audit_snapshot AS
SELECT ic.id, ic.code, jsonb_build_object(
    'id', ic.id,
    'code', ic.code,
    'displayName', ic.display_name,
    'active', ic.active,
    'randomDrawEnabled', ic.random_draw_enabled,
    'challengeSpecificity', ic.challenge_specificity,
    'baseDrawWeight', ic.base_draw_weight,
    'noveltyLevel', ic.novelty_level,
    'curatorNote', ic.curator_note,
    'version', ic.version,
    'directParents', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_refinement r JOIN ingredient_concept ref ON ref.id = r.parent_concept_id WHERE r.child_concept_id = ic.id),
    'directChildren', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_refinement r JOIN ingredient_concept ref ON ref.id = r.child_concept_id WHERE r.parent_concept_id = ic.id),
    'functionalRoles', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_functional_role r JOIN functional_role ref ON ref.id = r.functional_role_id WHERE r.ingredient_concept_id = ic.id),
    'culinaryFlags', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_flag r JOIN culinary_flag ref ON ref.id = r.culinary_flag_id WHERE r.ingredient_concept_id = ic.id),
    'culinaryDimensions', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) || jsonb_build_object('level', r.level) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_dimension r JOIN culinary_dimension ref ON ref.id = r.culinary_dimension_id WHERE r.ingredient_concept_id = ic.id),
    'culinaryCountries', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_country r JOIN culinary_country ref ON ref.code = r.country_code WHERE r.ingredient_concept_id = ic.id),
    'availability', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', NULL, 'level', r.availability_level, 'curatorNote', r.curator_note) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_availability r JOIN participant ref ON ref.id = r.participant_id WHERE r.ingredient_concept_id = ic.id),
    'seasonality', (SELECT coalesce(jsonb_agg(jsonb_build_object('month', r.month, 'weightMultiplier', r.weight_multiplier) ORDER BY r.month), '[]'::jsonb) FROM ingredient_seasonality r WHERE r.ingredient_concept_id = ic.id),
    'directExclusionRules', (SELECT coalesce(jsonb_agg(ref.code ORDER BY ref.code), '[]'::jsonb) FROM exclusion_rule_target r JOIN exclusion_rule ref ON ref.id = r.exclusion_rule_id WHERE r.ingredient_concept_id = ic.id)
) AS state
FROM ingredient_concept ic
WHERE ic.code IN (
    SELECT code FROM austria_new_concept
    UNION SELECT code FROM austria_existing_before
);

CREATE TEMP TABLE austria_audit_before ON COMMIT DROP AS
SELECT * FROM austria_audit_snapshot;

-- New-code/name collisions fail; no upsert can overwrite operative metadata.
INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, 'SPECIFIC',
       base_draw_weight, novelty_level, curator_note
FROM austria_new_concept;

-- Scalar reference lookups deliberately fail NOT NULL/FK constraints if a
-- required concept or reference is absent; joins must not silently drop rows.
INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = r.parent_code),
       (SELECT id FROM ingredient_concept WHERE code = r.child_code)
FROM austria_refinement r;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM functional_role WHERE code = a.role_code)
FROM (VALUES
    ('PUMPKIN_SEED_OIL', 'FAT'),
    ('PUMPKIN_SEED_OIL', 'SEASONING'),
    ('ROASTED_PUMPKIN_SEED_OIL', 'FAT'),
    ('ROASTED_PUMPKIN_SEED_OIL', 'SEASONING'),
    ('RUNNER_BEANS', 'PLANT_PROTEIN'),
    ('RUNNER_BEANS', 'STARCH'),
    ('BERG_CHEESE', 'ANIMAL_PROTEIN'),
    ('BERG_CHEESE', 'FAT'),
    ('BERG_CHEESE', 'SEASONING'),
    ('TIROLER_GRAUKAESE', 'ANIMAL_PROTEIN'),
    ('TIROLER_GRAUKAESE', 'SEASONING'),
    ('STRUDEL_DOUGH', 'STARCH'),
    ('BEEF_TAFELSPITZ', 'ANIMAL_PROTEIN'),
    ('APRICOT_PRESERVES', 'FRUIT'),
    ('APRICOT_PRESERVES', 'SEASONING'),
    ('TIROLER_SPECK', 'ANIMAL_PROTEIN'),
    ('TIROLER_SPECK', 'SEASONING'),
    ('EGG_FLECKERL', 'STARCH'),
    ('VEAL_LUNG', 'ANIMAL_PROTEIN'),
    ('WIENER_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('WIENER_SAUSAGE', 'FAT'),
    ('KAESEKRAINER', 'ANIMAL_PROTEIN'),
    ('KAESEKRAINER', 'FAT'),
    ('KAESEKRAINER', 'SEASONING'),
    ('PORK_CRACKLINGS', 'FAT'),
    ('PORK_CRACKLINGS', 'SEASONING'),
    ('SOFT_WHEAT_SEMOLINA', 'STARCH')
) a(concept_code, role_code);

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM culinary_flag WHERE code = a.flag_code)
FROM (VALUES
    ('BERG_CHEESE', 'FERMENTED'),
    ('BERG_CHEESE', 'CURED'),
    ('TIROLER_GRAUKAESE', 'FERMENTED'),
    ('TIROLER_GRAUKAESE', 'CURED'),
    ('TIROLER_SPECK', 'SMOKED'),
    ('TIROLER_SPECK', 'CURED'),
    ('TIROLER_SPECK', 'DRIED'),
    ('WIENER_SAUSAGE', 'SMOKED'),
    ('WIENER_SAUSAGE', 'CURED'),
    ('KAESEKRAINER', 'SMOKED'),
    ('KAESEKRAINER', 'CURED')
) a(concept_code, flag_code);

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM culinary_dimension WHERE code = a.dimension_code),
       a.level
FROM (VALUES
    ('PUMPKIN_SEED_OIL', 'DOMINANCE', 3),
    ('PUMPKIN_SEED_OIL', 'FATTINESS', 5),
    ('ROASTED_PUMPKIN_SEED_OIL', 'DOMINANCE', 4),
    ('ROASTED_PUMPKIN_SEED_OIL', 'BITTERNESS', 2),
    ('ROASTED_PUMPKIN_SEED_OIL', 'FATTINESS', 5),
    ('RUNNER_BEANS', 'DOMINANCE', 2),
    ('RUNNER_BEANS', 'FATTINESS', 1),
    ('BERG_CHEESE', 'DOMINANCE', 4),
    ('BERG_CHEESE', 'ACIDITY', 2),
    ('BERG_CHEESE', 'FATTINESS', 4),
    ('BERG_CHEESE', 'UMAMI', 4),
    ('BERG_CHEESE', 'SALTINESS', 3),
    ('TIROLER_GRAUKAESE', 'DOMINANCE', 5),
    ('TIROLER_GRAUKAESE', 'ACIDITY', 3),
    ('TIROLER_GRAUKAESE', 'FATTINESS', 1),
    ('TIROLER_GRAUKAESE', 'UMAMI', 4),
    ('TIROLER_GRAUKAESE', 'SALTINESS', 3),
    ('STRUDEL_DOUGH', 'DOMINANCE', 1),
    ('BEEF_TAFELSPITZ', 'DOMINANCE', 3),
    ('BEEF_TAFELSPITZ', 'FATTINESS', 2),
    ('BEEF_TAFELSPITZ', 'UMAMI', 3),
    ('APRICOT_PRESERVES', 'DOMINANCE', 3),
    ('APRICOT_PRESERVES', 'SWEETNESS', 5),
    ('APRICOT_PRESERVES', 'ACIDITY', 3),
    ('TIROLER_SPECK', 'DOMINANCE', 4),
    ('TIROLER_SPECK', 'UMAMI', 4),
    ('TIROLER_SPECK', 'SALTINESS', 4),
    ('EGG_FLECKERL', 'DOMINANCE', 2),
    ('VEAL_LUNG', 'DOMINANCE', 3),
    ('VEAL_LUNG', 'FATTINESS', 1),
    ('VEAL_LUNG', 'UMAMI', 3),
    ('WIENER_SAUSAGE', 'DOMINANCE', 3),
    ('WIENER_SAUSAGE', 'FATTINESS', 3),
    ('WIENER_SAUSAGE', 'UMAMI', 3),
    ('WIENER_SAUSAGE', 'SALTINESS', 3),
    ('KAESEKRAINER', 'DOMINANCE', 4),
    ('KAESEKRAINER', 'FATTINESS', 4),
    ('KAESEKRAINER', 'UMAMI', 4),
    ('KAESEKRAINER', 'SALTINESS', 3),
    ('PORK_CRACKLINGS', 'DOMINANCE', 4),
    ('PORK_CRACKLINGS', 'FATTINESS', 5),
    ('PORK_CRACKLINGS', 'UMAMI', 3),
    ('SOFT_WHEAT_SEMOLINA', 'DOMINANCE', 1)
) a(concept_code, dimension_code, level);

-- Two separate rows with the exact approved text; identical person notes are intentional.
INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT (SELECT id FROM ingredient_concept WHERE code = c.code),
       (SELECT id FROM participant WHERE code = p.code),
       c.availability_level, c.availability_note
FROM austria_new_concept c
CROSS JOIN (VALUES ('GEORGIA'), ('TOBIAS')) p(code);

-- No season rows: the approved multiplier is 1.0 in every month.
-- No additional exclusion targets or inherited metadata.

CREATE TEMP TABLE austria_added_country (ingredient_concept_id bigint PRIMARY KEY) ON COMMIT DROP;
WITH inserted AS (
    INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
    SELECT (SELECT id FROM ingredient_concept WHERE code = a.code), 'AT'
    FROM (
        SELECT code FROM austria_existing_country
        UNION SELECT code FROM austria_new_concept WHERE associate_at
    ) a
    ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING
    RETURNING ingredient_concept_id
)
INSERT INTO austria_added_country SELECT ingredient_concept_id FROM inserted;

-- Invalidate existing editors once per modified existing aggregate. New
-- concept aggregates retain their initial version, including the new oil parent.
UPDATE ingredient_concept ic
SET version = ic.version + 1
FROM austria_existing_before old
WHERE ic.id = old.id AND (
    EXISTS (SELECT 1 FROM austria_refinement r WHERE r.parent_code = ic.code)
    OR EXISTS (SELECT 1 FROM austria_added_country a WHERE a.ingredient_concept_id = ic.id)
);

WITH audit_group AS MATERIALIZED (SELECT gen_random_uuid() AS group_id)
INSERT INTO catalog_audit_entry (
    change_group_id, actor_key, entity_type, entity_id, action,
    before_state, after_state, payload_version
)
SELECT group_id, 'liquibase:034-austria-curation', 'INGREDIENT_CONCEPT',
       after.id, CASE WHEN before.id IS NULL THEN 'CREATE' ELSE 'UPDATE' END,
       before.state, after.state, 1
FROM austria_audit_snapshot after
LEFT JOIN austria_audit_before before USING (id)
CROSS JOIN audit_group
WHERE before.state IS DISTINCT FROM after.state;

DROP VIEW austria_audit_snapshot;
