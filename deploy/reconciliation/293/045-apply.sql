-- Issue #293 replacement execution for catalog 045; approved values remain in the published source.
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

-- Promote an already present own alias; cross-concept collisions were rejected.
DELETE FROM ingredient_concept_alias alias
USING ingredient_concept concept
WHERE concept.code = 'CRAB' AND alias.ingredient_concept_id = concept.id
  AND lower(alias.alias_text) = lower('Krabben');

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
JOIN ingredient_concept child ON child.code = approved.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

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
          AND curator_note = $note$Tiergruppe mit festem, leicht süßlichem Fleisch. Ganze oder geschälte Tiere, frisch, tiefgekühlt, gegart oder getrocknet, sind zulässig; Nordseekrabben gehören dazu, während Pasten, Saucen und Extrakte nicht allein über ihren Rohstoff zählen.$note$
    ) THEN
        RAISE EXCEPTION 'Crustacean existing-concept target write failed';
    END IF;

    IF EXISTS (
        SELECT 1 FROM ingredient_availability a
        JOIN ingredient_concept c ON c.id=a.ingredient_concept_id
        JOIN participant p ON p.id=a.participant_id
        WHERE c.code='CRAB' AND p.code IN ('GEORGIA','TOBIAS')
          AND a.curator_note IS DISTINCT FROM $note$Ausgelöstes Krabbenfleisch ist im Fisch- und Feinkostsortiment größerer Supermärkte gezielt erhältlich; ganze Tiere eher im Fischfachhandel.$note$
    ) THEN
        RAISE EXCEPTION 'Issue #293: CRAB note target write failed';
    END IF;

    IF EXISTS (
        SELECT 1 FROM crustacean_new_refinement target
        JOIN ingredient_concept p ON p.code = target.parent_code
        JOIN ingredient_concept c ON c.code = target.child_code
        LEFT JOIN ingredient_refinement r ON r.parent_concept_id=p.id AND r.child_concept_id=c.id
        WHERE r.parent_concept_id IS NULL
    ) OR EXISTS (
        SELECT 1 FROM ingredient_refinement r
        JOIN ingredient_concept p ON p.id=r.parent_concept_id
        JOIN ingredient_concept c ON c.id=r.child_concept_id
        WHERE p.code='CRUSTACEANS' AND c.code IN ('CRAYFISH','LANGOUSTINE','LOBSTER')
    ) THEN
        RAISE EXCEPTION 'Issue #293: crustacean target graph write failed';
    END IF;
END;
$validation$;
