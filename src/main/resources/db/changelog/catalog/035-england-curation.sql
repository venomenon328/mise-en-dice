--liquibase formatted sql
--changeset venomenon328:035-england-curation splitStatements:false
-- Issue #172 / ENGLAND_METADATA_V1_20260908, approved in the England review.
-- Both human approval gates are complete. Final corrections set Clotted Cream
-- and Beef Suet availability to SPECIALTY and Black Pudding novelty to 4.
-- England is represented by GB-ENG; GB remains the United Kingdom.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947223);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_seasonality, ingredient_availability, exclusion_rule, exclusion_rule_target,
    participant, functional_role, culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE england_new_concept (
    code text PRIMARY KEY,
    display_name text NOT NULL,
    challenge_specificity text NOT NULL,
    novelty_level smallint NOT NULL,
    base_draw_weight numeric(10,4) NOT NULL,
    curator_note text NOT NULL,
    availability_level text NOT NULL,
    availability_note text NOT NULL
) ON COMMIT DROP;

INSERT INTO england_new_concept VALUES
    ('CLOTTED_CREAM', 'Clotted Cream', 'SPECIFIC', 3, 0.4500,
        'Sehr dicke, fettreiche Sahnezubereitung mit löffelfester bis streichfähiger Konsistenz und mildem, vollmundigem Milcharoma. Normale Schlagsahne, Crème double und Mascarpone sind nicht gemeint.',
        'SPECIALTY', 'Echte Clotted Cream ist über mehrere deutsche Feinkost- und Käsehändler regulär erhältlich; Kühlpflicht und Spezialversand machen den Bezug zu einer gezielten Spezialbeschaffung.'),
    ('BLUE_STILTON', 'Blue Stilton', 'SPECIFIC', 3, 0.4500,
        'Blue Stilton ist der blau geaderte Stilton aus Kuhmilch mit cremig-krümeliger Textur und kräftigem, pikant-salzigem Aroma. White Stilton und generischer Blauschimmelkäse sind nicht gemeint.',
        'PLANNED', 'Blue Stilton ist bei großen allgemeinen Lebensmittelketten gelistet und zusätzlich über deutschen Feinkostversand erhältlich; Filialbestand oder Kühlzustellung müssen gezielt geplant werden.'),
    ('ENGLISH_MUSTARD', 'English Mustard', 'SPECIFIC', 2, 0.5500,
        'Glatter, gelber Senf englischer Art mit ausgeprägt stechender Senfschärfe. Senfpulver, Dijon-Senf, süßer Senf und beliebiger anderer scharfer Senf erfüllen die Vorgabe nicht.',
        'SPECIALTY', 'Zubereiteter English Mustard ist bei mehreren unabhängigen britischen Spezialhändlern in Deutschland regulär als haltbares Glas erhältlich; allgemeiner Handel ist nicht verlässlich.'),
    ('BLACK_PUDDING', 'Black Pudding', 'SPECIFIC', 4, 0.5000,
        'Schnittfeste britisch-irische Blutwurst mit deutlichem Getreideanteil, typischerweise Hafer oder Gerste, und krümelig-fester Textur. Andere Blutwursttraditionen erfüllen die Vorgabe nicht automatisch.',
        'SPECIALTY', 'Black Pudding ist über mehrere britisch-irische Spezialwege in Deutschland erhältlich; Kühl-/Expressversand und das enge Sortiment schließen einen allgemeinen planbaren Bezug aus.'),
    ('MARROWFAT_PEAS', 'Marrowfat Peas', 'SPECIFIC', 3, 0.6500,
        'Große ausgereifte grüne Erbsen mit mehlig-cremiger Textur nach dem Garen. Getrocknete oder schlicht konservierte ganze Marrowfat Peas sind gemeint; fertige Mushy Peas nicht.',
        'SPECIALTY', 'Ganze Marrowfat Peas sind bei mehreren britischen Spezialhändlern als Dose oder Trockenware regulär erhältlich; die haltbare Form macht den Spezialbezug logistisch unkompliziert.'),
    ('CUMBERLAND_SAUSAGE', 'Cumberland Sausage', 'SPECIFIC', 3, 0.6000,
        'Grobe Schweinswurst nach Cumberland-Art mit deutlicher Pfeffer- und Kräuterwürzung. Traditionelle Ringform oder kleinere Würste sind zulässig; generische englische Breakfast Sausage und Bratwurst nicht.',
        'DIFFICULT', 'Originale Cumberland Sausages hängen von wenigen britischen Tiefkühlimportwegen ab; enge Marktbreite und TK-Versand machen die Beschaffung weniger robust planbar.'),
    ('BEEF_SUET', 'Rindernierenfett (Suet)', 'SPECIFIC', 4, 0.4000,
        'Hartes Fett aus dem Bereich um die Rindernieren, roh oder fein zerkleinert als Beef Suet. Ausgelassener Rindertalg und Vegetable Suet sind nicht gemeint; mit Mehl bestäubte Beef-Suet-Flocken sind zulässig.',
        'SPECIALTY', 'Rohes Rindernierenfett ist über mehrere deutsche Fleischversender als Kühl- oder Tiefkühlware erhältlich; Kühlpflicht und Spezialversand schließen einen allgemeinen planbaren Bezug aus.'),
    ('GOLDEN_SYRUP', 'Golden Syrup', 'SPECIFIC', 3, 0.5000,
        'Zähflüssiger bernsteinfarbener Zucker-/Invertzuckersirup nach Golden-Syrup-Art mit karamelligem Aroma. Honig, Ahornsirup, Melasse und gewöhnlicher Rübensirup sind nicht gleichgesetzt.',
        'PLANNED', 'Golden Syrup ist über deutsche und europäische Backwaren- und Feinkosthändler zuverlässig als haltbare Flaschen- oder Dosenware bestellbar; ein Kulturimporteur ist nicht zwingend nötig.'),
    ('CUSTARD', 'Custard', 'SPECIFIC', 3, 0.4500,
        'Verzehrfertige britische Dessertsoße mit glatter, dickflüssiger Konsistenz, typischerweise milchbasiert und mild vanillig. Custard Powder, deutscher Vanillepudding und gewöhnliche dünne Vanillesauce sind nicht gemeint.',
        'SPECIALTY', 'Verzehrfertige britische Custard wird von mehreren unabhängigen UK-Spezialhändlern in Deutschland regulär als haltbare Dose oder Packung geführt; allgemeiner Handel ist nicht verlässlich.'),
    ('BROWN_SAUCE', 'Brown Sauce', 'SPECIFIC', 3, 0.4500,
        'Dunkle britische Würzsauce mit fruchtig-säuerlichem, würzig-süßem Profil, typischerweise auf Tomate, Malzessig, Melasse und Gewürzen aufgebaut. Ketchup, BBQ- und gewöhnliche Steak-Saucen sind nicht gemeint.',
        'SPECIALTY', 'Originale Brown Sauce wird von mehreren unabhängigen britischen und importorientierten Spezialhändlern regulär als haltbare Flaschenware geführt; allgemeiner Handel ist nicht zuverlässig.'),
    ('ALE', 'Ale', 'SPECIFIC', 4, 0.2500,
        'Obergäriges Bier der Ale-Familie; etwa Bitter, Pale Ale, Mild oder Brown Ale sind zulässige Ausprägungen. Lager ist nicht gemeint, Stout und Porter werden hier ebenfalls nicht mitgeführt.',
        'PLANNED', 'Mehrere Ale-Stile sind im breiten deutschen Bierhandel und bei großen Vollsortimentern gelistet; gezielte Sortimentsprüfung genügt, lokaler Standard ist Ale aber nicht.'),
    ('KIDNEY', 'Niere', 'OPEN', 3, 0.5500,
        'Offene Vorgabe für rohe Speisenieren von Schwein, Rind, Kalb oder Lamm. Die Tierart wird beim Kochen konkretisiert; fertige Nierengerichte und andere Innereien erfüllen die Vorgabe nicht.',
        'PLANNED', 'Frische Speisenieren sind über gut sortierte Fleischtheken und Metzgereiwege planbar; Tierart und aktueller Thekenbestand müssen vor dem Einkauf gezielt geprüft werden.'),
    ('BAKED_BEANS', 'Baked Beans', 'SPECIFIC', 2, 0.6500,
        'Verzehrfertige weiße Bohnen in süßlich-würziger Tomatensauce, typischerweise als Konserve. Schlichte weiße Bohnen ohne Sauce und andere gewürzte Bohnen-Fertiggerichte sind nicht gemeint.',
        'EASY', 'Baked Beans in Tomatensauce gehören mit Eigenmarken und mehreren Marken zum gewöhnlichen Konserven- und Dosengerichtesortiment großer Supermärkte.'),
    ('MINCEMEAT', 'Mincemeat (britische Fruchtfüllung)', 'SPECIFIC', 4, 0.3500,
        'Süße britische Backfüllung aus Äpfeln, getrockneten Weinfrüchten, kandierter Zitrusschale, Zucker beziehungsweise Sirup und Gewürzen; Suet kann enthalten sein. Trotz des Namens ist modernes Mincemeat kein Hackfleisch.',
        'SPECIALTY', 'Süßes britisches Mincemeat ist bei mehreren unabhängigen UK-Spezialhändlern in Deutschland regulär als haltbare Glasware erhältlich; allgemeiner Handel ist nicht verlässlich.');

CREATE TEMP TABLE england_refinement (
    parent_code text NOT NULL, child_code text NOT NULL,
    PRIMARY KEY (parent_code, child_code)
) ON COMMIT DROP;

INSERT INTO england_refinement VALUES
    ('CREAM', 'CLOTTED_CREAM'),
    ('BLUE_CHEESE', 'BLUE_STILTON'),
    ('HOT_MUSTARD', 'ENGLISH_MUSTARD'),
    ('BLOOD_SAUSAGE', 'BLACK_PUDDING'),
    ('PEAS', 'MARROWFAT_PEAS'),
    ('SAUSAGE', 'CUMBERLAND_SAUSAGE'),
    ('PORK', 'CUMBERLAND_SAUSAGE'),
    ('COOKING_FATS', 'BEEF_SUET'),
    ('SWEETENERS', 'GOLDEN_SYRUP'),
    ('DAIRY_PRODUCTS', 'CUSTARD'),
    ('SAUCES_AND_PASTES', 'BROWN_SAUCE'),
    ('BEER', 'ALE'),
    ('OFFAL', 'KIDNEY'),
    ('BEANS', 'BAKED_BEANS'),
    ('PRESERVED_PRODUCE', 'BAKED_BEANS'),
    ('PRESERVED_PRODUCE', 'MINCEMEAT');

CREATE TEMP TABLE england_existing_country (code text PRIMARY KEY) ON COMMIT DROP;
INSERT INTO england_existing_country VALUES
    ('CHEDDAR'),
    ('WORCESTERSHIRE_SAUCE'),
    ('MALT_VINEGAR'),
    ('COD'),
    ('HADDOCK'),
    ('CIDER'),
    ('BLACK_TEA'),
    ('RHUBARB');

CREATE TEMP TABLE england_existing_before ON COMMIT DROP AS
SELECT ic.* FROM ingredient_concept ic
WHERE ic.code IN (
    SELECT parent_code FROM england_refinement
    UNION SELECT code FROM england_existing_country
);

CREATE TEMP VIEW england_audit_snapshot AS
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
    SELECT code FROM england_new_concept
    UNION SELECT code FROM england_existing_before
);

CREATE TEMP TABLE england_audit_before ON COMMIT DROP AS
SELECT * FROM england_audit_snapshot;

INSERT INTO ingredient_concept (
    code, display_name, active, random_draw_enabled, challenge_specificity,
    base_draw_weight, novelty_level, curator_note
)
SELECT code, display_name, true, true, challenge_specificity,
       base_draw_weight, novelty_level, curator_note
FROM england_new_concept;

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = r.parent_code),
       (SELECT id FROM ingredient_concept WHERE code = r.child_code)
FROM england_refinement r;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM functional_role WHERE code = a.role_code)
FROM (VALUES
    ('CLOTTED_CREAM', 'FAT'),
    ('CLOTTED_CREAM', 'SEASONING'),
    ('BLUE_STILTON', 'ANIMAL_PROTEIN'),
    ('BLUE_STILTON', 'FAT'),
    ('BLUE_STILTON', 'SEASONING'),
    ('ENGLISH_MUSTARD', 'ACID'),
    ('ENGLISH_MUSTARD', 'SEASONING'),
    ('BLACK_PUDDING', 'ANIMAL_PROTEIN'),
    ('BLACK_PUDDING', 'FAT'),
    ('MARROWFAT_PEAS', 'PLANT_PROTEIN'),
    ('MARROWFAT_PEAS', 'STARCH'),
    ('CUMBERLAND_SAUSAGE', 'ANIMAL_PROTEIN'),
    ('CUMBERLAND_SAUSAGE', 'FAT'),
    ('BEEF_SUET', 'FAT'),
    ('GOLDEN_SYRUP', 'SEASONING'),
    ('CUSTARD', 'FAT'),
    ('CUSTARD', 'SEASONING'),
    ('BROWN_SAUCE', 'ACID'),
    ('BROWN_SAUCE', 'SEASONING'),
    ('ALE', 'ACID'),
    ('ALE', 'SEASONING'),
    ('KIDNEY', 'ANIMAL_PROTEIN'),
    ('BAKED_BEANS', 'PLANT_PROTEIN'),
    ('BAKED_BEANS', 'STARCH'),
    ('MINCEMEAT', 'FRUIT'),
    ('MINCEMEAT', 'SEASONING')
) a(concept_code, role_code);

INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM culinary_flag WHERE code = a.flag_code)
FROM (VALUES
    ('BLUE_STILTON', 'FERMENTED'),
    ('BLUE_STILTON', 'CURED')
) a(concept_code, flag_code);

INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
SELECT (SELECT id FROM ingredient_concept WHERE code = a.concept_code),
       (SELECT id FROM culinary_dimension WHERE code = a.dimension_code),
       a.level
FROM (VALUES
    ('CLOTTED_CREAM', 'DOMINANCE', 3),
    ('CLOTTED_CREAM', 'SWEETNESS', 2),
    ('CLOTTED_CREAM', 'FATTINESS', 5),
    ('BLUE_STILTON', 'DOMINANCE', 5),
    ('BLUE_STILTON', 'ACIDITY', 3),
    ('BLUE_STILTON', 'BITTERNESS', 2),
    ('BLUE_STILTON', 'FATTINESS', 4),
    ('BLUE_STILTON', 'UMAMI', 5),
    ('BLUE_STILTON', 'SALTINESS', 5),
    ('ENGLISH_MUSTARD', 'DOMINANCE', 5),
    ('ENGLISH_MUSTARD', 'ACIDITY', 3),
    ('ENGLISH_MUSTARD', 'HEAT', 5),
    ('BLACK_PUDDING', 'DOMINANCE', 4),
    ('BLACK_PUDDING', 'FATTINESS', 4),
    ('BLACK_PUDDING', 'UMAMI', 4),
    ('BLACK_PUDDING', 'SALTINESS', 3),
    ('MARROWFAT_PEAS', 'DOMINANCE', 2),
    ('MARROWFAT_PEAS', 'SWEETNESS', 2),
    ('MARROWFAT_PEAS', 'UMAMI', 2),
    ('CUMBERLAND_SAUSAGE', 'DOMINANCE', 4),
    ('CUMBERLAND_SAUSAGE', 'FATTINESS', 4),
    ('CUMBERLAND_SAUSAGE', 'UMAMI', 4),
    ('CUMBERLAND_SAUSAGE', 'SALTINESS', 3),
    ('BEEF_SUET', 'DOMINANCE', 3),
    ('BEEF_SUET', 'FATTINESS', 5),
    ('GOLDEN_SYRUP', 'DOMINANCE', 3),
    ('GOLDEN_SYRUP', 'SWEETNESS', 5),
    ('CUSTARD', 'DOMINANCE', 3),
    ('CUSTARD', 'SWEETNESS', 4),
    ('CUSTARD', 'FATTINESS', 3),
    ('BROWN_SAUCE', 'DOMINANCE', 4),
    ('BROWN_SAUCE', 'SWEETNESS', 3),
    ('BROWN_SAUCE', 'ACIDITY', 4),
    ('BROWN_SAUCE', 'UMAMI', 3),
    ('BROWN_SAUCE', 'SALTINESS', 3),
    ('ALE', 'DOMINANCE', 3),
    ('ALE', 'SWEETNESS', 2),
    ('ALE', 'ACIDITY', 2),
    ('ALE', 'BITTERNESS', 4),
    ('KIDNEY', 'DOMINANCE', 4),
    ('KIDNEY', 'FATTINESS', 2),
    ('KIDNEY', 'UMAMI', 4),
    ('BAKED_BEANS', 'DOMINANCE', 3),
    ('BAKED_BEANS', 'SWEETNESS', 3),
    ('BAKED_BEANS', 'ACIDITY', 3),
    ('BAKED_BEANS', 'UMAMI', 3),
    ('BAKED_BEANS', 'SALTINESS', 3),
    ('MINCEMEAT', 'DOMINANCE', 4),
    ('MINCEMEAT', 'SWEETNESS', 5),
    ('MINCEMEAT', 'ACIDITY', 2)
) a(concept_code, dimension_code, level);

INSERT INTO ingredient_availability (
    ingredient_concept_id, participant_id, availability_level, curator_note
)
SELECT (SELECT id FROM ingredient_concept WHERE code = c.code),
       (SELECT id FROM participant WHERE code = p.code),
       c.availability_level, c.availability_note
FROM england_new_concept c
CROSS JOIN (VALUES ('GEORGIA'), ('TOBIAS')) p(code);

INSERT INTO exclusion_rule_target (
    exclusion_rule_id, ingredient_concept_id, include_refinements
)
SELECT (SELECT id FROM exclusion_rule WHERE code = a.rule_code),
       (SELECT id FROM ingredient_concept WHERE code = 'BEEF_SUET'),
       false
FROM (VALUES ('NO_MEAT'), ('NO_BEEF')) a(rule_code);

-- No season rows: the approved multiplier is 1.0 in every month.

CREATE TEMP TABLE england_added_country (ingredient_concept_id bigint PRIMARY KEY) ON COMMIT DROP;
WITH inserted AS (
    INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
    SELECT (SELECT id FROM ingredient_concept WHERE code = a.code), 'GB-ENG'
    FROM (
        SELECT code FROM england_existing_country
        UNION SELECT code FROM england_new_concept
    ) a
    ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING
    RETURNING ingredient_concept_id
)
INSERT INTO england_added_country SELECT ingredient_concept_id FROM inserted;

UPDATE ingredient_concept ic
SET version = ic.version + 1
FROM england_existing_before old
WHERE ic.id = old.id AND (
    EXISTS (SELECT 1 FROM england_refinement r WHERE r.parent_code = ic.code)
    OR EXISTS (SELECT 1 FROM england_added_country a WHERE a.ingredient_concept_id = ic.id)
);

WITH audit_group AS MATERIALIZED (SELECT gen_random_uuid() AS group_id)
INSERT INTO catalog_audit_entry (
    change_group_id, actor_key, entity_type, entity_id, action,
    before_state, after_state, payload_version
)
SELECT group_id, 'liquibase:035-england-curation', 'INGREDIENT_CONCEPT',
       after.id, CASE WHEN before.id IS NULL THEN 'CREATE' ELSE 'UPDATE' END,
       before.state, after.state, 1
FROM england_audit_snapshot after
LEFT JOIN england_audit_before before USING (id)
CROSS JOIN audit_group
WHERE before.state IS DISTINCT FROM after.state;

DROP VIEW england_audit_snapshot;
