--liquibase formatted sql
--changeset venomenon328:002-final-catalog-sanity splitStatements:false

-- Mise en Dice - strict checks for the consolidated untouched baseline.
-- Requires: catalog/014-catalog-consolidation.sql
--
-- Once an operational catalog row has been edited, the runtime database is the
-- source of truth. In that case the migration remains recorded, but these exact
-- baseline assertions are intentionally skipped.

DO $$
DECLARE
    root_count integer;
    specific_roots text[];
    refinement_count integer;
    missing_expected_relations integer;
    redundant_edges integer;
    specificity_inversions integer;
    implausible_weights integer;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM ingredient_concept
        WHERE version <> 0
    ) THEN
        RAISE NOTICE 'Skipping strict consolidated-baseline checks because edited ingredient concepts exist';
        RETURN;
    END IF;

    SELECT count(*)
      INTO root_count
      FROM ingredient_concept ic
     WHERE ic.active
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_refinement ir
            WHERE ir.child_concept_id = ic.id
       );

    IF root_count <> 24 THEN
        RAISE EXCEPTION 'unexpected active root count after catalog consolidation: %', root_count;
    END IF;

    SELECT array_agg(ic.code ORDER BY ic.code)
      INTO specific_roots
      FROM ingredient_concept ic
     WHERE ic.active
       AND ic.challenge_specificity = 'SPECIFIC'
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_refinement ir
            WHERE ir.child_concept_id = ic.id
       );

    IF specific_roots IS DISTINCT FROM ARRAY['COFFEE']::text[] THEN
        RAISE EXCEPTION 'unexpected specific root concepts after catalog consolidation: %', specific_roots;
    END IF;

    SELECT count(*)
      INTO refinement_count
      FROM ingredient_refinement;

    IF refinement_count <> 711 THEN
        RAISE EXCEPTION 'unexpected refinement count after catalog consolidation: %', refinement_count;
    END IF;

    SELECT count(*)
      INTO missing_expected_relations
      FROM (
          VALUES
              ('VEGETABLES', 'SEAWEED'),
              ('VEGETABLES', 'ARTICHOKE'),
              ('STEM_VEGETABLES', 'BAMBOO_SHOOTS'),
              ('SPICES', 'CHILI'),
              ('FRUIT', 'POMEGRANATE'),
              ('SPICES', 'GINGER'),
              ('FRUIT', 'PERSIMMON'),
              ('PRESERVED_PRODUCE', 'CAPERS'),
              ('GRAINS', 'CORN'),
              ('ROOT_VEGETABLES', 'HORSERADISH'),
              ('SPICES', 'MSG'),
              ('PRESERVED_PRODUCE', 'OLIVES'),
              ('GARLIC', 'BLACK_GARLIC'),
              ('TROPICAL_FRUIT', 'TAMARIND'),
              ('MEAT', 'ESCARGOT'),
              ('FRUIT', 'GRAPE'),
              ('FRESH_HERBS', 'LEMONGRASS')
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
        RAISE EXCEPTION '% expected root-closing refinement relations are missing', missing_expected_relations;
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
      INTO specificity_inversions
      FROM ingredient_refinement ir
      JOIN ingredient_concept parent
        ON parent.id = ir.parent_concept_id
      JOIN ingredient_concept child
        ON child.id = ir.child_concept_id
     WHERE parent.challenge_specificity = 'SPECIFIC'
       AND child.challenge_specificity = 'OPEN';

    IF specificity_inversions <> 0 THEN
        RAISE EXCEPTION '% specific concepts directly refine to open concepts', specificity_inversions;
    END IF;

    SELECT count(*)
      INTO implausible_weights
      FROM ingredient_concept ic
     WHERE ic.active
       AND ic.random_draw_enabled
       AND (
           (ic.novelty_level = 5 AND ic.base_draw_weight > 0.2500)
           OR (ic.novelty_level = 4 AND ic.base_draw_weight > 0.3500)
           OR (ic.novelty_level = 3 AND ic.base_draw_weight > 0.5500)
           OR (
               ic.base_draw_weight > 0.3500
               AND EXISTS (
                   SELECT 1
                     FROM ingredient_availability ia
                    WHERE ia.ingredient_concept_id = ic.id
                      AND ia.availability_level = 'DIFFICULT'
               )
           )
           OR (
               ic.base_draw_weight > 0.3500
               AND EXISTS (
                   SELECT 1
                     FROM ingredient_refinement ir
                     JOIN ingredient_concept parent
                       ON parent.id = ir.parent_concept_id
                    WHERE ir.child_concept_id = ic.id
                      AND parent.code = 'COOKING_ALCOHOL'
               )
           )
       );

    IF implausible_weights <> 0 THEN
        RAISE EXCEPTION '% active draw concepts exceed the consolidated plausibility caps', implausible_weights;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM ingredient_refinement ir
          JOIN ingredient_concept parent ON parent.id = ir.parent_concept_id
          JOIN ingredient_concept child ON child.id = ir.child_concept_id
         WHERE parent.code = 'SEAFOOD'
           AND child.code = 'SHELLFISH'
    ) OR NOT EXISTS (
        SELECT 1
          FROM ingredient_refinement ir
          JOIN ingredient_concept parent ON parent.id = ir.parent_concept_id
          JOIN ingredient_concept child ON child.id = ir.child_concept_id
         WHERE parent.code = 'SHELLFISH'
           AND child.code = 'CRUSTACEANS'
    ) OR EXISTS (
        SELECT 1
          FROM ingredient_refinement ir
          JOIN ingredient_concept parent ON parent.id = ir.parent_concept_id
          JOIN ingredient_concept child ON child.id = ir.child_concept_id
         WHERE parent.code = 'SEAFOOD'
           AND child.code = 'CRUSTACEANS'
    ) THEN
        RAISE EXCEPTION 'seafood, shellfish and crustacean hierarchy is not canonical';
    END IF;
END;
$$;
