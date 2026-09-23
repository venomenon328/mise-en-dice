-- Issue #293 replacement execution for catalog 047; approved values remain in the published source.
DO $$
DECLARE
    problem_codes TEXT;
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended('catalog-draw-weight-calibration', 0));

    PERFORM concept.id
      FROM ingredient_concept concept
      JOIN approved_catalog_draw_weight approved ON approved.code = concept.code
     ORDER BY concept.id
       FOR UPDATE OF concept;

    SELECT string_agg(approved.code, ', ' ORDER BY approved.code)
      INTO problem_codes
      FROM approved_catalog_draw_weight approved
 LEFT JOIN ingredient_concept concept ON concept.code = approved.code
     WHERE concept.id IS NULL;
    IF problem_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Catalog draw-weight calibration is missing target codes: %', problem_codes;
    END IF;



    UPDATE ingredient_concept concept
       SET base_draw_weight = approved.target_weight,
           version = concept.version + 1,
           updated_at = CURRENT_TIMESTAMP
      FROM approved_catalog_draw_weight approved
     WHERE concept.code = approved.code
       AND concept.base_draw_weight IS DISTINCT FROM approved.target_weight;

    SELECT string_agg(concept.code, ', ' ORDER BY concept.code)
      INTO problem_codes
      FROM ingredient_concept concept
      JOIN approved_catalog_draw_weight approved ON approved.code = concept.code
     WHERE concept.base_draw_weight <> approved.target_weight;
    IF problem_codes IS NOT NULL THEN
        RAISE EXCEPTION 'Catalog draw-weight calibration did not reach target values: %', problem_codes;
    END IF;
END $$;
