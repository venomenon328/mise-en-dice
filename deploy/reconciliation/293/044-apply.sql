-- Issue #293 replacement execution for catalog 044; approved values remain in the published source.
DO $guard$
DECLARE
    mismatches text;
    invalid_targets text;
BEGIN
    IF (SELECT count(*) FROM ingredient_name_alias_r3_review) <> 906 THEN
        RAISE EXCEPTION 'Issue #264: invalid R3 review cardinality (expected 906 concepts)';
    END IF;

    IF (SELECT count(*) FROM ingredient_name_alias_r3_review
        WHERE previous_display_name IS DISTINCT FROM target_display_name) <> 37
       OR (SELECT coalesce(sum(cardinality(target_aliases)), 0)
           FROM ingredient_name_alias_r3_review) <> 206
       OR (SELECT count(*) FROM ingredient_name_alias_r3_review
           WHERE previous_display_name IS DISTINCT FROM target_display_name
              OR previous_aliases IS DISTINCT FROM target_aliases) <> 191 THEN
        RAISE EXCEPTION 'Issue #264: invalid approved R3 target cardinality (expected 37 names / 206 aliases / 191 changed concepts)';
    END IF;

    -- The approved R3 review covers the 906 concepts present at its baseline.
    -- Concepts appended by already-published later changesets are allowed, but
    -- every reviewed stable code remains mandatory; its old contents do not.
    SELECT string_agg(review.code, ', ' ORDER BY review.code COLLATE "C")
    INTO mismatches
    FROM ingredient_name_alias_r3_review review
    LEFT JOIN ingredient_concept ic USING (code)
    WHERE ic.code IS NULL;

    IF mismatches IS NOT NULL THEN
        RAISE EXCEPTION 'Issue #264: missing reviewed catalog codes: %', mismatches;
    END IF;

    SELECT string_agg(review.code, ', ' ORDER BY review.code COLLATE "C")
    INTO invalid_targets
    FROM ingredient_name_alias_r3_review review
    WHERE btrim(review.target_display_name) = ''
       OR review.target_display_name IS DISTINCT FROM btrim(review.target_display_name)
       OR EXISTS (
            SELECT 1
            FROM unnest(review.target_aliases) alias_text
            WHERE btrim(alias_text) = ''
               OR alias_text IS DISTINCT FROM btrim(alias_text)
               OR lower(alias_text) = lower(review.target_display_name)
       )
       OR cardinality(review.target_aliases) IS DISTINCT FROM (
            SELECT count(DISTINCT lower(alias_text))::integer
            FROM unnest(review.target_aliases) alias_text
       );

    IF invalid_targets IS NOT NULL THEN
        RAISE EXCEPTION 'Issue #264: invalid normalized R3 target values for codes: %', invalid_targets;
    END IF;

    SELECT string_agg(collision, ', ' ORDER BY collision COLLATE "C")
    INTO invalid_targets
    FROM (
        SELECT left_review.code || '<->' || right_review.code || ':' || left_review.target_display_name AS collision
        FROM ingredient_name_alias_r3_review left_review
        JOIN ingredient_name_alias_r3_review right_review
          ON left_review.code < right_review.code
         AND lower(left_review.target_display_name) = lower(right_review.target_display_name)
        UNION ALL
        SELECT alias_review.code || '<->' || canonical_review.code || ':' || alias_text AS collision
        FROM ingredient_name_alias_r3_review alias_review
        CROSS JOIN LATERAL unnest(alias_review.target_aliases) alias_text
        JOIN ingredient_name_alias_r3_review canonical_review
          ON canonical_review.code <> alias_review.code
         AND lower(canonical_review.target_display_name) = lower(alias_text)
        UNION ALL
        SELECT left_review.code || '<->' || right_review.code || ':' || left_alias AS collision
        FROM ingredient_name_alias_r3_review left_review
        CROSS JOIN LATERAL unnest(left_review.target_aliases) left_alias
        JOIN ingredient_name_alias_r3_review right_review ON right_review.code > left_review.code
        CROSS JOIN LATERAL unnest(right_review.target_aliases) right_alias
        WHERE lower(left_alias) = lower(right_alias)
    ) collisions;

    IF invalid_targets IS NOT NULL THEN
        RAISE EXCEPTION 'Issue #264: unapproved cross-concept R3 target collisions: %', invalid_targets;
    END IF;

END $guard$;

-- Project only the actual write set: 37 canonical targets and additive aliases.
-- Unchanged reviewed names and unrelated operational aliases remain authoritative.
CREATE TEMP TABLE issue_293_final_names ON COMMIT DROP AS
SELECT c.id, c.code,
       CASE WHEN r.previous_display_name IS DISTINCT FROM r.target_display_name
            THEN r.target_display_name ELSE c.display_name END AS display_name
FROM ingredient_concept c LEFT JOIN ingredient_name_alias_r3_review r USING (code);

DO $$ BEGIN
    IF EXISTS (SELECT lower(display_name) FROM issue_293_final_names
               GROUP BY lower(display_name) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Issue #293: canonical target name collision';
    END IF;
    IF EXISTS (
        SELECT 1 FROM ingredient_name_alias_r3_review r
        JOIN ingredient_concept c USING (code)
        JOIN ingredient_concept_alias a ON lower(a.alias_text)=lower(r.target_display_name)
        WHERE r.previous_display_name IS DISTINCT FROM r.target_display_name
          AND a.ingredient_concept_id<>c.id
    ) OR EXISTS (
        SELECT 1 FROM ingredient_name_alias_r3_review r
        CROSS JOIN LATERAL unnest(r.target_aliases) target_alias
        JOIN issue_293_final_names c ON lower(c.display_name)=lower(target_alias) AND c.code<>r.code
    ) OR EXISTS (
        SELECT 1 FROM ingredient_name_alias_r3_review r
        CROSS JOIN LATERAL unnest(r.target_aliases) target_alias
        JOIN ingredient_concept_alias a ON lower(a.alias_text)=lower(target_alias)
        JOIN ingredient_concept c ON c.id=a.ingredient_concept_id AND c.code<>r.code
    ) THEN
        RAISE EXCEPTION 'Issue #293: target name/alias collision';
    END IF;
END $$;

CREATE TEMP TABLE issue_293_name_changes ON COMMIT DROP AS
SELECT c.id FROM ingredient_concept c
JOIN ingredient_name_alias_r3_review r USING (code)
WHERE (r.previous_display_name IS DISTINCT FROM r.target_display_name
       AND c.display_name IS DISTINCT FROM r.target_display_name)
   OR EXISTS (
       SELECT 1 FROM unnest(r.target_aliases) target_alias
       WHERE lower(target_alias)<>lower(c.display_name)
         AND NOT EXISTS (SELECT 1 FROM ingredient_concept_alias a
                         WHERE a.ingredient_concept_id=c.id AND a.alias_text=target_alias)
   );

-- A name already present as this concept's alias is promoted, not duplicated.
DELETE FROM ingredient_concept_alias a
USING ingredient_concept c, ingredient_name_alias_r3_review r
WHERE c.code=r.code AND a.ingredient_concept_id=c.id
  AND r.previous_display_name IS DISTINCT FROM r.target_display_name
  AND lower(a.alias_text)=lower(r.target_display_name);

UPDATE ingredient_concept c SET display_name=r.target_display_name
FROM ingredient_name_alias_r3_review r
WHERE c.code=r.code AND r.previous_display_name IS DISTINCT FROM r.target_display_name
  AND c.display_name IS DISTINCT FROM r.target_display_name;

INSERT INTO ingredient_concept_alias (ingredient_concept_id, alias_text)
SELECT c.id, target_alias FROM ingredient_name_alias_r3_review r
JOIN ingredient_concept c USING (code)
CROSS JOIN LATERAL unnest(r.target_aliases) target_alias
WHERE lower(target_alias)<>lower(c.display_name)
ON CONFLICT (ingredient_concept_id, lower(alias_text)) DO UPDATE
SET alias_text=excluded.alias_text
WHERE ingredient_concept_alias.alias_text IS DISTINCT FROM excluded.alias_text;

UPDATE ingredient_concept c SET version=version+1
FROM issue_293_name_changes changed WHERE c.id=changed.id;

DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM ingredient_name_alias_r3_review r JOIN ingredient_concept c USING (code)
        WHERE (r.previous_display_name IS DISTINCT FROM r.target_display_name
               AND c.display_name IS DISTINCT FROM r.target_display_name)
           OR EXISTS (SELECT 1 FROM unnest(r.target_aliases) target_alias
                      WHERE lower(target_alias)<>lower(c.display_name)
                        AND NOT EXISTS (SELECT 1 FROM ingredient_concept_alias a
                                        WHERE a.ingredient_concept_id=c.id AND a.alias_text=target_alias))
    ) THEN RAISE EXCEPTION 'Issue #293: name/alias target write failed'; END IF;
END $$;
