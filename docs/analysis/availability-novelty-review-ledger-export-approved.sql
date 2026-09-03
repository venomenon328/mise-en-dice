-- Read-only export for the post-anchor-approval Issue #188 working ledger.
--
-- Run this query against a fresh database migrated from the documented catalog
-- commit. Use psql CSV output with the footer disabled and preserve UTF-8.
--
-- IMPORTANT: review applicability is an explicit editorial decision. It is not
-- inferred from random_draw_enabled or missing availability rows.
WITH review_applicability(concept_code, applicability) AS (
    VALUES
        ('BAKED_GOODS', 'NOT_APPLICABLE_STRUCTURE'),
        ('CONFECTIONERY', 'NOT_APPLICABLE_STRUCTURE'),
        ('DAIRY_PRODUCTS', 'NOT_APPLICABLE_STRUCTURE'),
        ('FRESH_HERBS', 'NOT_APPLICABLE_STRUCTURE'),
        ('PLANT_DRINKS', 'NOT_APPLICABLE_STRUCTURE'),
        ('READY_CURRY_PASTE', 'APPLICABLE'),
        ('READY_SAUCES_AND_PASTES', 'NOT_APPLICABLE_STRUCTURE'),
        ('SPICES', 'NOT_APPLICABLE_STRUCTURE')
),
availability AS (
    SELECT
        value.ingredient_concept_id,
        max(value.availability_level) FILTER (WHERE participant.code = 'GEORGIA') AS georgia,
        max(value.availability_level) FILTER (WHERE participant.code = 'TOBIAS') AS tobias
    FROM ingredient_availability value
    JOIN participant ON participant.id = value.participant_id
    GROUP BY value.ingredient_concept_id
),
parents AS (
    SELECT
        refinement.child_concept_id AS ingredient_concept_id,
        string_agg(parent.code, '|' ORDER BY parent.code) AS direct_parent_codes
    FROM ingredient_refinement refinement
    JOIN ingredient_concept parent ON parent.id = refinement.parent_concept_id
    GROUP BY refinement.child_concept_id
),
children AS (
    SELECT
        refinement.parent_concept_id AS ingredient_concept_id,
        string_agg(child.code, '|' ORDER BY child.code) AS direct_child_codes
    FROM ingredient_refinement refinement
    JOIN ingredient_concept child ON child.id = refinement.child_concept_id
    GROUP BY refinement.parent_concept_id
)
SELECT
    concept.code AS concept_code,
    concept.display_name,
    CASE WHEN concept.active THEN 'true' ELSE 'false' END AS active,
    CASE WHEN concept.random_draw_enabled THEN 'true' ELSE 'false' END AS random_draw_enabled,
    concept.challenge_specificity,
    concept.novelty_level AS current_cooking_novelty,
    availability.georgia AS current_availability_georgia,
    availability.tobias AS current_availability_tobias,
    concept.base_draw_weight AS current_base_draw_weight,
    coalesce(parents.direct_parent_codes, '') AS direct_parent_codes,
    coalesce(children.direct_child_codes, '') AS direct_child_codes,
    concept.curator_note,
    coalesce(review_applicability.applicability, 'APPLICABLE') AS proposed_review_applicability,
    CASE
        WHEN review_applicability.applicability = 'NOT_APPLICABLE_STRUCTURE'
        THEN 'NOT_APPLICABLE'
        ELSE ''
    END AS proposed_cooking_novelty,
    CASE
        WHEN review_applicability.applicability = 'NOT_APPLICABLE_STRUCTURE'
        THEN 'NOT_APPLICABLE'
        ELSE ''
    END AS proposed_availability_georgia,
    CASE
        WHEN review_applicability.applicability = 'NOT_APPLICABLE_STRUCTURE'
        THEN 'NOT_APPLICABLE'
        ELSE ''
    END AS proposed_availability_tobias,
    '' AS proposed_base_draw_weight_review,
    '' AS review_rationale,
    '' AS evidence,
    CASE
        WHEN review_applicability.applicability = 'NOT_APPLICABLE_STRUCTURE'
        THEN 'APPROVED_NOT_APPLICABLE'
        ELSE 'READY_FOR_FULL_REVIEW'
    END AS approval_status
FROM ingredient_concept concept
LEFT JOIN availability ON availability.ingredient_concept_id = concept.id
LEFT JOIN parents ON parents.ingredient_concept_id = concept.id
LEFT JOIN children ON children.ingredient_concept_id = concept.id
LEFT JOIN review_applicability ON review_applicability.concept_code = concept.code
ORDER BY concept.code;
