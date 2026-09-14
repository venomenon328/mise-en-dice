--liquibase formatted sql
--changeset venomenon328:042-d3-approved-country-relations splitStatements:false
-- Issues #172 / #252: persist only the 17 Gate-1-approved D3 country relations.
-- Gate-1 snapshot: https://github.com/venomenon328/mise-en-dice/issues/252#issuecomment-5660102322
-- No metadata, rating, note, refinement, flag, availability, seasonality, or concept-definition changes.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(6241884431947226);
LOCK TABLE ingredient_concept, ingredient_culinary_country, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE d3_approved_country_relation (
    concept_code text NOT NULL,
    country_code text NOT NULL,
    PRIMARY KEY (concept_code, country_code)
) ON COMMIT DROP;

INSERT INTO d3_approved_country_relation VALUES
    ('LEEK', 'GB-SCT'),
    ('COUSCOUS', 'FR'),
    ('MERGUEZ', 'FR'),
    ('ANNATTO', 'VN'),
    ('SMOKED_SALMON', 'SE'),
    ('SAUSAGE', 'DK'),
    ('PORK', 'DK'),
    ('CHEESE', 'NL'),
    ('VINEGAR', 'PH'),
    ('SAUSAGE', 'PH'),
    ('DUCK_EGG', 'PH'),
    ('MUSHROOMS', 'DE'),
    ('BEEF', 'AT'),
    ('PEAS', 'FI'),
    ('POTATO', 'BE'),
    ('SHRIMP', 'BE'),
    ('CREAM', 'FR');

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

    IF EXISTS (
        SELECT 1
        FROM d3_approved_country_relation approved
        JOIN ingredient_concept concept ON concept.code = approved.concept_code
        JOIN ingredient_culinary_country existing
          ON existing.ingredient_concept_id = concept.id
         AND existing.country_code = approved.country_code
    ) THEN
        RAISE EXCEPTION 'D3 approved country relation batch contains a relation that already exists';
    END IF;
END;
$validation$;

CREATE TEMP TABLE d3_existing_before ON COMMIT DROP AS
SELECT concept.id, concept.code, concept.version
FROM ingredient_concept concept
JOIN (
    SELECT DISTINCT concept_code
    FROM d3_approved_country_relation
) approved ON approved.concept_code = concept.code;

INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
SELECT concept.id, approved.country_code
FROM d3_approved_country_relation approved
JOIN ingredient_concept concept ON concept.code = approved.concept_code;

UPDATE ingredient_concept concept
SET version = concept.version + 1
FROM d3_existing_before old
WHERE concept.id = old.id;
