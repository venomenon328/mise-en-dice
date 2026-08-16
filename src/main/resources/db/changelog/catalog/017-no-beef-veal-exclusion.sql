--liquibase formatted sql

--changeset venomenon328:017-no-beef-veal-exclusion
-- Issue #62: VEAL is a sibling of BEEF under MEAT, so expanding the BEEF target cannot reach veal.
-- Keep the catalog graph intact and express the culinary exclusion explicitly through the existing target model.
INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
SELECT rule.id, concept.id, true
FROM exclusion_rule rule
JOIN ingredient_concept concept ON concept.code = 'VEAL'
WHERE rule.code = 'NO_BEEF'
ON CONFLICT (exclusion_rule_id, ingredient_concept_id)
DO UPDATE SET include_refinements = EXCLUDED.include_refinements;
