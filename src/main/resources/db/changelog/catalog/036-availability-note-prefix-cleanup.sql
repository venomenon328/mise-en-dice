--liquibase formatted sql
--changeset venomenon328:036-availability-note-prefix-cleanup splitStatements:false
-- Issue #209, phase 1. This is deliberately data-driven against the display
-- names present at migration time; it contains no historical hit list.
-- Liquibase executes this changeset transactionally.

SELECT pg_advisory_xact_lock(20903620260909);
LOCK TABLE ingredient_concept, ingredient_refinement, ingredient_functional_role,
    ingredient_culinary_flag, ingredient_culinary_dimension, ingredient_culinary_country,
    ingredient_seasonality, ingredient_availability, exclusion_rule, exclusion_rule_target,
    participant, functional_role, culinary_flag, culinary_dimension, culinary_country
    IN SHARE ROW EXCLUSIVE MODE;

-- A candidate must begin byte-for-byte with the currently stored display name
-- and a colon. Only ASCII spaces immediately after that colon are removed.
-- Whitespace-only remainders are retained as reportable anomalies, never
-- written as an empty availability note.
CREATE TEMP TABLE availability_note_prefix_cleanup (
    ingredient_concept_id bigint NOT NULL,
    participant_id bigint NOT NULL,
    old_note text NOT NULL,
    new_note text NOT NULL,
    PRIMARY KEY (ingredient_concept_id, participant_id)
) ON COMMIT DROP;

INSERT INTO availability_note_prefix_cleanup (
    ingredient_concept_id, participant_id, old_note, new_note
)
SELECT availability.ingredient_concept_id,
       availability.participant_id,
       availability.curator_note,
       regexp_replace(
           substring(availability.curator_note FROM char_length(concept.display_name) + 2),
           '^ *',
           ''
       )
FROM ingredient_availability availability
JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
WHERE availability.curator_note IS NOT NULL
  AND left(availability.curator_note, char_length(concept.display_name) + 1)
      = concept.display_name || ':'
  AND substring(availability.curator_note FROM char_length(concept.display_name) + 2)
      ~ '[^[:space:]]';

-- The aggregate snapshot matches CatalogIngredientSnapshotFactory's payload
-- contract so that audit history exposes each note delta as such.
CREATE TEMP VIEW availability_note_prefix_cleanup_snapshot AS
SELECT concept.id, jsonb_build_object(
    'id', concept.id,
    'code', concept.code,
    'displayName', concept.display_name,
    'active', concept.active,
    'randomDrawEnabled', concept.random_draw_enabled,
    'challengeSpecificity', concept.challenge_specificity,
    'baseDrawWeight', concept.base_draw_weight,
    'noveltyLevel', concept.novelty_level,
    'curatorNote', concept.curator_note,
    'version', concept.version,
    'directParents', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_refinement relation JOIN ingredient_concept ref ON ref.id = relation.parent_concept_id WHERE relation.child_concept_id = concept.id),
    'directChildren', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_refinement relation JOIN ingredient_concept ref ON ref.id = relation.child_concept_id WHERE relation.parent_concept_id = concept.id),
    'functionalRoles', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_functional_role relation JOIN functional_role ref ON ref.id = relation.functional_role_id WHERE relation.ingredient_concept_id = concept.id),
    'culinaryFlags', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_flag relation JOIN culinary_flag ref ON ref.id = relation.culinary_flag_id WHERE relation.ingredient_concept_id = concept.id),
    'culinaryDimensions', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) || jsonb_build_object('level', relation.level) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_dimension relation JOIN culinary_dimension ref ON ref.id = relation.culinary_dimension_id WHERE relation.ingredient_concept_id = concept.id),
    'culinaryCountries', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_country relation JOIN culinary_country ref ON ref.code = relation.country_code WHERE relation.ingredient_concept_id = concept.id),
    'availability', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', NULL, 'level', relation.availability_level, 'curatorNote', relation.curator_note) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_availability relation JOIN participant ref ON ref.id = relation.participant_id WHERE relation.ingredient_concept_id = concept.id),
    'seasonality', (SELECT coalesce(jsonb_agg(jsonb_build_object('month', relation.month, 'weightMultiplier', relation.weight_multiplier) ORDER BY relation.month), '[]'::jsonb) FROM ingredient_seasonality relation WHERE relation.ingredient_concept_id = concept.id),
    'directExclusionRules', (SELECT coalesce(jsonb_agg(ref.code ORDER BY ref.code), '[]'::jsonb) FROM exclusion_rule_target relation JOIN exclusion_rule ref ON ref.id = relation.exclusion_rule_id WHERE relation.ingredient_concept_id = concept.id)
) AS state
FROM ingredient_concept concept
WHERE EXISTS (
    SELECT 1 FROM availability_note_prefix_cleanup cleanup
    WHERE cleanup.ingredient_concept_id = concept.id
);

CREATE TEMP TABLE availability_note_prefix_cleanup_before ON COMMIT DROP AS
SELECT * FROM availability_note_prefix_cleanup_snapshot;

UPDATE ingredient_availability availability
SET curator_note = cleanup.new_note
FROM availability_note_prefix_cleanup cleanup
WHERE availability.ingredient_concept_id = cleanup.ingredient_concept_id
  AND availability.participant_id = cleanup.participant_id
  AND availability.curator_note = cleanup.old_note;

-- A changed child aggregate invalidates any editor that still holds its old
-- version. No other metadata participates in this update.
UPDATE ingredient_concept concept
SET version = concept.version + 1
WHERE EXISTS (
    SELECT 1 FROM availability_note_prefix_cleanup cleanup
    WHERE cleanup.ingredient_concept_id = concept.id
);

WITH audit_group AS MATERIALIZED (SELECT gen_random_uuid() AS group_id)
INSERT INTO catalog_audit_entry (
    change_group_id, actor_key, entity_type, entity_id, action,
    before_state, after_state, payload_version
)
SELECT audit_group.group_id, 'liquibase:036-availability-note-prefix-cleanup',
       'INGREDIENT_CONCEPT', after.id, 'UPDATE', before.state, after.state, 1
FROM availability_note_prefix_cleanup_snapshot after
JOIN availability_note_prefix_cleanup_before before USING (id)
CROSS JOIN audit_group
WHERE before.state IS DISTINCT FROM after.state;

DROP VIEW availability_note_prefix_cleanup_snapshot;
