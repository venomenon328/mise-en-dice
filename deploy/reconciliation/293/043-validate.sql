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
        WHERE code = 'DE'
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
