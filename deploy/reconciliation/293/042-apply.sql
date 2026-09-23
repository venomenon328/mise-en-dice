-- Issue #293 replacement execution for catalog 042; approved values remain in the published source.
DO $validation$
BEGIN
    IF (SELECT count(*) FROM d3_approved_country_relation) <> 17 THEN
        RAISE EXCEPTION 'D3 approved country relation batch must contain exactly 17 relations';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM d3_approved_country_relation approved
        LEFT JOIN ingredient_concept concept ON concept.code = approved.concept_code
        WHERE concept.id IS NULL
    ) THEN
        RAISE EXCEPTION 'D3 approved country relation batch requires ingredient concepts that are missing';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM d3_approved_country_relation approved
        LEFT JOIN culinary_country country ON country.code = approved.country_code
        WHERE country.code IS NULL
    ) THEN
        RAISE EXCEPTION 'D3 approved country relation batch requires culinary countries that are missing';
    END IF;


END;
$validation$;

CREATE TEMP TABLE d3_existing_before ON COMMIT DROP AS
SELECT concept.id, concept.code, concept.version
FROM ingredient_concept concept
JOIN (
    SELECT DISTINCT concept_code
    FROM d3_approved_country_relation
) approved ON approved.concept_code = concept.code
WHERE EXISTS (
    SELECT 1 FROM d3_approved_country_relation target
    WHERE target.concept_code = concept.code AND NOT EXISTS (
        SELECT 1 FROM ingredient_culinary_country actual
        WHERE actual.ingredient_concept_id = concept.id AND actual.country_code = target.country_code
    )
);

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, approved.country_code
FROM d3_approved_country_relation approved
JOIN ingredient_concept concept ON concept.code = approved.concept_code
ON CONFLICT (ingredient_concept_id, country_code) DO NOTHING;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM d3_existing_before old
WHERE concept.id = old.id;
