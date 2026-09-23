-- Issue #293 replacement execution for catalog 037; approved values remain in the published source.
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM availability_note_consolidation_review r
               LEFT JOIN ingredient_concept c USING (code) WHERE c.id IS NULL) THEN
        RAISE EXCEPTION 'Issue #293: missing 037 concept code';
    END IF;
END $$;


CREATE TEMP TABLE availability_note_consolidation (
    ingredient_concept_id bigint NOT NULL,
    participant_id bigint NOT NULL,
    old_note text,
    new_note text NOT NULL,
    PRIMARY KEY (ingredient_concept_id, participant_id)
) ON COMMIT DROP;

INSERT INTO availability_note_consolidation
SELECT concept.id, participant.id, availability.curator_note, expected.new_note
FROM availability_note_consolidation_expected expected
JOIN ingredient_concept concept ON concept.code = expected.code
JOIN participant ON participant.code = expected.participant_code
JOIN ingredient_availability availability
  ON availability.ingredient_concept_id = concept.id
 AND availability.participant_id = participant.id
WHERE availability.curator_note IS DISTINCT FROM expected.new_note;

-- Full aggregate snapshots follow CatalogIngredientSnapshotFactory's payload.
CREATE TEMP VIEW availability_note_consolidation_snapshot AS
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
    'directParents', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY lower(ref.display_name), ref.id), '[]'::jsonb) FROM ingredient_refinement relation JOIN ingredient_concept ref ON ref.id = relation.parent_concept_id WHERE relation.child_concept_id = concept.id),
    'directChildren', (SELECT coalesce(jsonb_agg(jsonb_build_object('id', ref.id, 'code', ref.code, 'displayName', ref.display_name, 'active', ref.active) ORDER BY lower(ref.display_name), ref.id), '[]'::jsonb) FROM ingredient_refinement relation JOIN ingredient_concept ref ON ref.id = relation.child_concept_id WHERE relation.parent_concept_id = concept.id),
    'functionalRoles', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY lower(ref.display_name), ref.id), '[]'::jsonb) FROM ingredient_functional_role relation JOIN functional_role ref ON ref.id = relation.functional_role_id WHERE relation.ingredient_concept_id = concept.id),
    'culinaryFlags', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description) ORDER BY lower(ref.display_name), ref.id), '[]'::jsonb) FROM ingredient_culinary_flag relation JOIN culinary_flag ref ON ref.id = relation.culinary_flag_id WHERE relation.ingredient_concept_id = concept.id),
    'culinaryDimensions', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', ref.description, 'level', relation.level) ORDER BY ref.id), '[]'::jsonb) FROM culinary_dimension ref LEFT JOIN ingredient_culinary_dimension relation ON relation.culinary_dimension_id = ref.id AND relation.ingredient_concept_id = concept.id),
    'culinaryCountries', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name) ORDER BY ref.code), '[]'::jsonb) FROM ingredient_culinary_country relation JOIN culinary_country ref ON ref.code = relation.country_code WHERE relation.ingredient_concept_id = concept.id),
    'availability', (SELECT coalesce(jsonb_agg(jsonb_build_object('code', ref.code, 'displayName', ref.display_name, 'description', NULL, 'level', relation.availability_level, 'curatorNote', relation.curator_note) ORDER BY CASE ref.code WHEN 'GEORGIA' THEN 1 WHEN 'TOBIAS' THEN 2 ELSE 3 END), '[]'::jsonb) FROM participant ref LEFT JOIN ingredient_availability relation ON relation.participant_id = ref.id AND relation.ingredient_concept_id = concept.id WHERE ref.code IN ('GEORGIA', 'TOBIAS')),
    'seasonality', (SELECT jsonb_agg(jsonb_build_object('month', months.month, 'weightMultiplier', coalesce(relation.weight_multiplier, 1)) ORDER BY months.month) FROM generate_series(1, 12) AS months(month) LEFT JOIN ingredient_seasonality relation ON relation.ingredient_concept_id = concept.id AND relation.month = months.month),
    'directExclusionRules', (SELECT coalesce(jsonb_agg(ref.display_text ORDER BY lower(ref.display_text), ref.id), '[]'::jsonb) FROM exclusion_rule_target relation JOIN exclusion_rule ref ON ref.id = relation.exclusion_rule_id WHERE relation.ingredient_concept_id = concept.id)
) AS state
FROM ingredient_concept concept
WHERE EXISTS (
    SELECT 1 FROM availability_note_consolidation cleanup
    WHERE cleanup.ingredient_concept_id = concept.id
);

CREATE TEMP TABLE availability_note_consolidation_before ON COMMIT DROP AS
SELECT * FROM availability_note_consolidation_snapshot;

UPDATE ingredient_availability availability
SET curator_note = cleanup.new_note
FROM availability_note_consolidation cleanup
WHERE availability.ingredient_concept_id = cleanup.ingredient_concept_id
  AND availability.participant_id = cleanup.participant_id
  AND availability.curator_note IS NOT DISTINCT FROM cleanup.old_note;

-- A changed child aggregate invalidates any editor that still holds its old
-- version. Each affected concept advances exactly once even when both
-- participant notes change; already-installed target pairs remain untouched.
UPDATE ingredient_concept concept
SET version = concept.version + 1
WHERE EXISTS (
    SELECT 1 FROM availability_note_consolidation cleanup
    WHERE cleanup.ingredient_concept_id = concept.id
);

WITH audit_group AS MATERIALIZED (SELECT gen_random_uuid() AS group_id)
INSERT INTO catalog_audit_entry (
    change_group_id, actor_key, entity_type, entity_id, action,
    before_state, after_state, payload_version
)
SELECT audit_group.group_id, 'liquibase:037-availability-note-consolidation',
       'INGREDIENT_CONCEPT', after.id, 'UPDATE', before.state, after.state, 1
FROM availability_note_consolidation_snapshot after
JOIN availability_note_consolidation_before before USING (id)
CROSS JOIN audit_group
WHERE before.state IS DISTINCT FROM after.state;

DROP VIEW availability_note_consolidation_snapshot;

DROP VIEW availability_note_consolidation_expected;
