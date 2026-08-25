--liquibase formatted sql
--changeset venomenon328:003-catalog-gap-sanity splitStatements:false

-- Mise en Dice - strict checks for the catalog after the second gap review.
-- Requires: catalog/015-catalog-gap-review.sql
--
-- Exact seed assertions describe an untouched baseline. Once an administrator
-- has edited a catalog aggregate, the runtime database remains authoritative
-- and the exact comparison is intentionally skipped.

DO $$
DECLARE
    concept_count integer;
    drawable_count integer;
    open_count integer;
    specific_count integer;
    refinement_count integer;
    root_codes text[];
    specific_root_codes text[];
    missing_new_concepts integer;
    missing_expected_relations integer;
    obsolete_relations integer;
    missing_expected_targets integer;
    redundant_edges integer;
    role_disjoint_edges integer;
    specificity_inversions integer;
    missing_roles integer;
    missing_availability integer;
    open_without_children integer;
    implausible_weights integer;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ingredient_concept
        WHERE version <> 0
    ) OR EXISTS (
        SELECT 1
        FROM exclusion_rule
        WHERE version <> 0
    ) THEN
        RAISE NOTICE 'Skipping strict post-gap baseline checks because edited catalog aggregates exist';
        RETURN;
    END IF;

    SELECT count(*) INTO concept_count FROM ingredient_concept;
    SELECT count(*) INTO drawable_count
      FROM ingredient_concept
     WHERE active AND random_draw_enabled;
    SELECT count(*) INTO open_count
      FROM ingredient_concept
     WHERE active AND random_draw_enabled AND challenge_specificity = 'OPEN';
    SELECT count(*) INTO specific_count
      FROM ingredient_concept
     WHERE active AND random_draw_enabled AND challenge_specificity = 'SPECIFIC';
    SELECT count(*) INTO refinement_count FROM ingredient_refinement;

    IF concept_count <> 665
       OR drawable_count <> 663
       OR open_count <> 87
       OR specific_count <> 576
       OR refinement_count <> 735 THEN
        RAISE EXCEPTION
            'unexpected post-gap baseline counts: concepts %, drawable %, open %, specific %, refinements %',
            concept_count,
            drawable_count,
            open_count,
            specific_count,
            refinement_count;
    END IF;

    SELECT array_agg(concept.code ORDER BY concept.code)
      INTO root_codes
      FROM ingredient_concept concept
     WHERE concept.active
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_refinement relation
            WHERE relation.child_concept_id = concept.id
       );

    IF root_codes IS DISTINCT FROM ARRAY[
        'COCOA_PRODUCTS',
        'COCONUT_PRODUCTS',
        'COFFEE',
        'COOKING_ALCOHOL',
        'COOKING_FATS',
        'DAIRY_PRODUCTS',
        'EGGS',
        'FRESH_HERBS',
        'FRUIT',
        'LEGUMES',
        'MEAT',
        'NUTS',
        'PLANT_PROTEIN_PRODUCTS',
        'PRESERVED_PRODUCE',
        'SAUCES_AND_PASTES',
        'SEAFOOD',
        'SEEDS',
        'SPICES',
        'STARCHES',
        'STOCKS',
        'SWEETENERS',
        'TEA',
        'VEGETABLES',
        'VINEGAR'
    ]::text[] THEN
        RAISE EXCEPTION 'unexpected active root concepts after catalog gap review: %', root_codes;
    END IF;

    SELECT array_agg(concept.code ORDER BY concept.code)
      INTO specific_root_codes
      FROM ingredient_concept concept
     WHERE concept.active
       AND concept.challenge_specificity = 'SPECIFIC'
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_refinement relation
            WHERE relation.child_concept_id = concept.id
       );

    IF specific_root_codes IS DISTINCT FROM ARRAY['COFFEE']::text[] THEN
        RAISE EXCEPTION 'unexpected specific roots after catalog gap review: %', specific_root_codes;
    END IF;

    SELECT count(*)
      INTO missing_new_concepts
      FROM (
          VALUES
              ('DRIED_CHILI'),
              ('PICKLED_CHILI'),
              ('CHILI_POWDER'),
              ('PUL_BIBER'),
              ('CAYENNE_PEPPER'),
              ('KASHMIRI_CHILI_POWDER'),
              ('SWEET_PAPRIKA_POWDER'),
              ('HOT_PAPRIKA_POWDER'),
              ('PEPPER'),
              ('GREEN_PEPPER'),
              ('FLOWER_VEGETABLES'),
              ('CHAMPIGNONS'),
              ('WHITE_CHAMPIGNON'),
              ('BROWN_CHAMPIGNON'),
              ('CANNED_CHAMPIGNONS'),
              ('HERB_BUTTER'),
              ('GARLIC_BUTTER'),
              ('MAGGI_SEASONING'),
              ('GARLIC_POWDER'),
              ('ONION_POWDER'),
              ('RED_BELL_PEPPER'),
              ('YELLOW_BELL_PEPPER'),
              ('GREEN_BELL_PEPPER')
      ) AS expected(code)
     WHERE NOT EXISTS (
         SELECT 1
           FROM ingredient_concept concept
          WHERE concept.code = expected.code
     );

    IF missing_new_concepts <> 0 THEN
        RAISE EXCEPTION '% expected catalog gap concepts are missing', missing_new_concepts;
    END IF;

    SELECT count(*)
      INTO missing_expected_relations
      FROM (
          VALUES
              ('FRUIT_VEGETABLES', 'CHILI'),
              ('SPICES', 'DRIED_CHILI'),
              ('DRIED_CHILI', 'ANCHO_CHILI'),
              ('DRIED_CHILI', 'CHIPOTLE'),
              ('DRIED_CHILI', 'CHILI_FLAKES'),
              ('DRIED_CHILI', 'CHILI_POWDER'),
              ('CHILI_FLAKES', 'GOCHUGARU'),
              ('CHILI_FLAKES', 'PUL_BIBER'),
              ('CHILI_POWDER', 'CAYENNE_PEPPER'),
              ('CHILI_POWDER', 'KASHMIRI_CHILI_POWDER'),
              ('PRESERVED_PRODUCE', 'PICKLED_CHILI'),
              ('PAPRIKA_POWDER', 'SWEET_PAPRIKA_POWDER'),
              ('PAPRIKA_POWDER', 'HOT_PAPRIKA_POWDER'),
              ('SPICES', 'PEPPER'),
              ('PEPPER', 'BLACK_PEPPER'),
              ('PEPPER', 'WHITE_PEPPER'),
              ('PEPPER', 'GREEN_PEPPER'),
              ('VEGETABLES', 'FLOWER_VEGETABLES'),
              ('FLOWER_VEGETABLES', 'ARTICHOKE'),
              ('FLOWER_VEGETABLES', 'BROCCOLI'),
              ('FLOWER_VEGETABLES', 'CAULIFLOWER'),
              ('FLOWER_VEGETABLES', 'ROMANESCO'),
              ('MUSHROOMS', 'CHAMPIGNONS'),
              ('CHAMPIGNONS', 'CHAMPIGNON'),
              ('CHAMPIGNONS', 'CANNED_CHAMPIGNONS'),
              ('CHAMPIGNON', 'WHITE_CHAMPIGNON'),
              ('CHAMPIGNON', 'BROWN_CHAMPIGNON'),
              ('PRESERVED_PRODUCE', 'CANNED_CHAMPIGNONS'),
              ('BUTTER', 'HERB_BUTTER'),
              ('BUTTER', 'GARLIC_BUTTER'),
              ('SAUCES_AND_PASTES', 'MAGGI_SEASONING'),
              ('SPICES', 'GARLIC_POWDER'),
              ('SPICES', 'ONION_POWDER'),
              ('SAUCES_AND_PASTES', 'MOLE_PASTE'),
              ('BELL_PEPPER', 'RED_BELL_PEPPER'),
              ('BELL_PEPPER', 'YELLOW_BELL_PEPPER'),
              ('BELL_PEPPER', 'GREEN_BELL_PEPPER'),
              ('FISH', 'SURIMI'),
              ('CORN', 'POLENTA'),
              ('RED_BELL_PEPPER', 'ROASTED_RED_PEPPER')
      ) AS expected(parent_code, child_code)
     WHERE NOT EXISTS (
         SELECT 1
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent
             ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child
             ON child.id = relation.child_concept_id
          WHERE parent.code = expected.parent_code
            AND child.code = expected.child_code
     );

    IF missing_expected_relations <> 0 THEN
        RAISE EXCEPTION '% expected post-gap refinement relations are missing', missing_expected_relations;
    END IF;

    SELECT count(*)
      INTO obsolete_relations
      FROM (
          VALUES
              ('SPICES', 'CHILI'),
              ('CHILI', 'ANCHO_CHILI'),
              ('CHILI', 'CHILI_FLAKES'),
              ('CHILI', 'CHIPOTLE'),
              ('CHILI', 'GOCHUGARU'),
              ('SPICES', 'BLACK_PEPPER'),
              ('SPICES', 'WHITE_PEPPER'),
              ('VEGETABLES', 'ARTICHOKE'),
              ('MUSHROOMS', 'CHAMPIGNON'),
              ('CHILI_CONDIMENTS', 'LAKSA_PASTE'),
              ('CHILI_CONDIMENTS', 'MOLE_PASTE'),
              ('CHILI_CONDIMENTS', 'THAI_GREEN_CURRY_PASTE'),
              ('CHILI_CONDIMENTS', 'THAI_RED_CURRY_PASTE'),
              ('PRESERVED_FISH', 'SURIMI'),
              ('GRAINS', 'POLENTA'),
              ('BELL_PEPPER', 'ROASTED_RED_PEPPER')
      ) AS obsolete(parent_code, child_code)
     WHERE EXISTS (
         SELECT 1
           FROM ingredient_refinement relation
           JOIN ingredient_concept parent
             ON parent.id = relation.parent_concept_id
           JOIN ingredient_concept child
             ON child.id = relation.child_concept_id
          WHERE parent.code = obsolete.parent_code
            AND child.code = obsolete.child_code
     );

    IF obsolete_relations <> 0 THEN
        RAISE EXCEPTION '% obsolete post-gap refinement relations remain', obsolete_relations;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM ingredient_concept
         WHERE code = 'CHILI'
           AND display_name = 'frische Chili'
           AND challenge_specificity = 'OPEN'
    ) OR NOT EXISTS (
        SELECT 1
          FROM ingredient_concept
         WHERE code = 'CHILI_FLAKES'
           AND challenge_specificity = 'OPEN'
    ) OR NOT EXISTS (
        SELECT 1
          FROM ingredient_concept
         WHERE code = 'PAPRIKA_POWDER'
           AND challenge_specificity = 'OPEN'
    ) OR NOT EXISTS (
        SELECT 1
          FROM ingredient_concept
         WHERE code = 'CHAMPIGNON'
           AND challenge_specificity = 'OPEN'
    ) OR NOT EXISTS (
        SELECT 1
          FROM ingredient_concept
         WHERE code = 'COCONUT_PRODUCTS'
           AND display_name = 'Kokosnuss oder Kokosprodukt'
    ) THEN
        RAISE EXCEPTION 'expected catalog concept sharpenings were not applied';
    END IF;

    SELECT count(*)
      INTO missing_expected_targets
      FROM (
          VALUES
              ('NO_CHILI', 'DRIED_CHILI', true),
              ('NO_CHILI', 'PICKLED_CHILI', true),
              ('NO_CHILI', 'READY_CURRY_PASTE', true),
              ('NO_CHILI', 'MOLE_PASTE', false),
              ('NO_ALLIUMS', 'GARLIC_BUTTER', false),
              ('NO_ALLIUMS', 'GARLIC_POWDER', false),
              ('NO_ALLIUMS', 'ONION_POWDER', false)
      ) AS expected(rule_code, concept_code, include_refinements)
     WHERE NOT EXISTS (
         SELECT 1
           FROM exclusion_rule_target target
           JOIN exclusion_rule rule
             ON rule.id = target.exclusion_rule_id
           JOIN ingredient_concept concept
             ON concept.id = target.ingredient_concept_id
          WHERE rule.code = expected.rule_code
            AND concept.code = expected.concept_code
            AND target.include_refinements = expected.include_refinements
     );

    IF missing_expected_targets <> 0 THEN
        RAISE EXCEPTION '% expected post-gap exclusion targets are missing', missing_expected_targets;
    END IF;

    WITH RECURSIVE alternate_paths(parent_concept_id, child_concept_id) AS (
        SELECT first.parent_concept_id, second.child_concept_id
        FROM ingredient_refinement first
        JOIN ingredient_refinement second
          ON second.parent_concept_id = first.child_concept_id

        UNION

        SELECT alternate.parent_concept_id, next.child_concept_id
        FROM alternate_paths alternate
        JOIN ingredient_refinement next
          ON next.parent_concept_id = alternate.child_concept_id
    )
    SELECT count(*)
      INTO redundant_edges
      FROM ingredient_refinement direct
      JOIN alternate_paths alternate
        ON alternate.parent_concept_id = direct.parent_concept_id
       AND alternate.child_concept_id = direct.child_concept_id;

    IF redundant_edges <> 0 THEN
        RAISE EXCEPTION '% transitively redundant direct refinement edges remain', redundant_edges;
    END IF;

    SELECT count(*)
      INTO role_disjoint_edges
      FROM ingredient_refinement relation
     WHERE NOT EXISTS (
         SELECT 1
           FROM ingredient_functional_role parent_role
           JOIN ingredient_functional_role child_role
             ON child_role.functional_role_id = parent_role.functional_role_id
          WHERE parent_role.ingredient_concept_id = relation.parent_concept_id
            AND child_role.ingredient_concept_id = relation.child_concept_id
     );

    IF role_disjoint_edges <> 0 THEN
        RAISE EXCEPTION '% refinement relations lack a shared functional role', role_disjoint_edges;
    END IF;

    SELECT count(*)
      INTO specificity_inversions
      FROM ingredient_refinement relation
      JOIN ingredient_concept parent
        ON parent.id = relation.parent_concept_id
      JOIN ingredient_concept child
        ON child.id = relation.child_concept_id
     WHERE parent.challenge_specificity = 'SPECIFIC'
       AND child.challenge_specificity = 'OPEN';

    IF specificity_inversions <> 0 THEN
        RAISE EXCEPTION '% specific concepts directly refine to open concepts', specificity_inversions;
    END IF;

    SELECT count(*)
      INTO missing_roles
      FROM ingredient_concept concept
     WHERE concept.active
       AND concept.random_draw_enabled
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_functional_role assignment
            WHERE assignment.ingredient_concept_id = concept.id
       );

    IF missing_roles <> 0 THEN
        RAISE EXCEPTION '% drawable concepts lack a functional role', missing_roles;
    END IF;

    SELECT count(*)
      INTO missing_availability
      FROM ingredient_concept concept
      CROSS JOIN (
          SELECT id
          FROM participant
          WHERE active
            AND code IN ('TOBIAS', 'GEORGIA')
      ) expected_participant
     WHERE concept.active
       AND concept.random_draw_enabled
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_availability availability
            WHERE availability.ingredient_concept_id = concept.id
              AND availability.participant_id = expected_participant.id
       );

    IF missing_availability <> 0 THEN
        RAISE EXCEPTION '% drawable concept-participant pairs lack availability', missing_availability;
    END IF;

    SELECT count(*)
      INTO open_without_children
      FROM ingredient_concept concept
     WHERE concept.active
       AND concept.random_draw_enabled
       AND concept.challenge_specificity = 'OPEN'
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_refinement relation
            WHERE relation.parent_concept_id = concept.id
       );

    IF open_without_children <> 0 THEN
        RAISE EXCEPTION '% drawable open concepts lack known refinements', open_without_children;
    END IF;

    SELECT count(*)
      INTO implausible_weights
      FROM ingredient_concept concept
     WHERE concept.active
       AND concept.random_draw_enabled
       AND (
           (concept.novelty_level = 5 AND concept.base_draw_weight > 0.2500)
           OR (concept.novelty_level = 4 AND concept.base_draw_weight > 0.3500)
           OR (concept.novelty_level = 3 AND concept.base_draw_weight > 0.5500)
           OR (
               concept.base_draw_weight > 0.3500
               AND EXISTS (
                   SELECT 1
                     FROM ingredient_availability availability
                    WHERE availability.ingredient_concept_id = concept.id
                      AND availability.availability_level = 'DIFFICULT'
               )
           )
       );

    IF implausible_weights <> 0 THEN
        RAISE EXCEPTION '% drawable concepts exceed the catalog plausibility caps', implausible_weights;
    END IF;
END;
$$;
