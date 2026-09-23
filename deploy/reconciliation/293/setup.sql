BEGIN;
-- Lock both Liquibase tables before classifying again. A concurrent Liquibase
-- acquisition cannot race the check; an already held logical lock is rejected.
SET LOCAL lock_timeout = '15s';
LOCK TABLE databasechangeloglock, databasechangelog IN EXCLUSIVE MODE;
DO $$ BEGIN
    IF pg_temp.issue_293_state() <> 'PENDING' THEN
        RAISE EXCEPTION 'Issue #293: expected exact post-036 incident history';
    END IF;
END $$;

SELECT pg_advisory_xact_lock(6241884431947226);
SELECT pg_advisory_xact_lock(6241884431947263);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_seasonality, ingredient_availability, exclusion_rule, exclusion_rule_target,
    participant, functional_role, culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

DO $$ BEGIN
    IF (SELECT count(*) FROM participant WHERE code IN ('GEORGIA','TOBIAS')) <> 2 THEN
        RAISE EXCEPTION 'Issue #293: missing required participants';
    END IF;
    IF EXISTS (
        SELECT code FROM (VALUES ('ANIMAL_PROTEIN'),('PLANT_PROTEIN'),('VEGETABLE'),
          ('FRUIT'),('STARCH'),('FAT'),('ACID'),('AROMATIC'),('SEASONING')) required(code)
        EXCEPT SELECT code FROM functional_role
    ) OR EXISTS (
        SELECT code FROM (VALUES ('DOMINANCE'),('SWEETNESS'),('ACIDITY'),('BITTERNESS'),
          ('FATTINESS'),('HEAT'),('UMAMI')) required(code)
        EXCEPT SELECT code FROM culinary_dimension
    ) THEN RAISE EXCEPTION 'Issue #293: missing role/dimension reference'; END IF;
END $$;
