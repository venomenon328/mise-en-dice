-- Issue #288 source export. Run read-only against a fresh PostgreSQL 17 rebuild of
-- main@c6044088c3ff501261e01e36bdec915f9bfdb1d5 (complete master through changeset 046).
-- psql -X -qAt -v ON_ERROR_STOP=1 -f docs/analysis/catalog-draw-weights-source-export.sql
BEGIN TRANSACTION READ ONLY;

SELECT jsonb_build_object(
    'active', concept.active,
    'aliases', coalesce((
        SELECT jsonb_agg(alias.alias_text ORDER BY alias.alias_text)
          FROM ingredient_concept_alias alias
         WHERE alias.ingredient_concept_id = concept.id
    ), '[]'::jsonb),
    'availability', coalesce((
        SELECT jsonb_object_agg(participant.code, jsonb_build_object(
                   'level', availability.availability_level,
                   'note', availability.curator_note
               ) ORDER BY participant.code)
          FROM ingredient_availability availability
          JOIN participant ON participant.id = availability.participant_id
         WHERE availability.ingredient_concept_id = concept.id
    ), '{}'::jsonb),
    'baseDrawWeight', to_char(concept.base_draw_weight, 'FM999999990.0000'),
    'challengeSpecificity', concept.challenge_specificity,
    'code', concept.code,
    'culinaryCountries', coalesce((
        SELECT jsonb_agg(jsonb_build_object(
                   'code', country.code,
                   'displayName', country.display_name
               ) ORDER BY country.code)
          FROM ingredient_culinary_country relation
          JOIN culinary_country country ON country.code = relation.country_code
         WHERE relation.ingredient_concept_id = concept.id
    ), '[]'::jsonb),
    'curatorNote', concept.curator_note,
    'dimensions', coalesce((
        SELECT jsonb_object_agg(dimension.code, assignment.level ORDER BY dimension.code)
          FROM ingredient_culinary_dimension assignment
          JOIN culinary_dimension dimension ON dimension.id = assignment.culinary_dimension_id
         WHERE assignment.ingredient_concept_id = concept.id
    ), '{}'::jsonb),
    'directChildren', coalesce((
        SELECT jsonb_agg(child.code ORDER BY child.code)
          FROM ingredient_refinement relation
          JOIN ingredient_concept child ON child.id = relation.child_concept_id
         WHERE relation.parent_concept_id = concept.id
    ), '[]'::jsonb),
    'directParents', coalesce((
        SELECT jsonb_agg(parent.code ORDER BY parent.code)
          FROM ingredient_refinement relation
          JOIN ingredient_concept parent ON parent.id = relation.parent_concept_id
         WHERE relation.child_concept_id = concept.id
    ), '[]'::jsonb),
    'displayName', concept.display_name,
    'flags', coalesce((
        SELECT jsonb_agg(flag.code ORDER BY flag.code)
          FROM ingredient_culinary_flag assignment
          JOIN culinary_flag flag ON flag.id = assignment.culinary_flag_id
         WHERE assignment.ingredient_concept_id = concept.id
    ), '[]'::jsonb),
    'noveltyLevel', concept.novelty_level,
    'randomDrawEnabled', concept.random_draw_enabled,
    'roles', coalesce((
        SELECT jsonb_agg(role.code ORDER BY role.code)
          FROM ingredient_functional_role assignment
          JOIN functional_role role ON role.id = assignment.functional_role_id
         WHERE assignment.ingredient_concept_id = concept.id
    ), '[]'::jsonb),
    'seasonality', coalesce((
        SELECT jsonb_object_agg(season.month::text,
                   to_char(season.weight_multiplier, 'FM999999990.0000') ORDER BY season.month)
          FROM ingredient_seasonality season
         WHERE season.ingredient_concept_id = concept.id
    ), '{}'::jsonb),
    'version', concept.version
)::text
FROM ingredient_concept concept
ORDER BY concept.code COLLATE "C";

COMMIT;
