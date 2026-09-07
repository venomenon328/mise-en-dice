-- Read-only code-based catalog fingerprint for the #189 persisted review handoff.
-- Excludes IDs, timestamps and aggregate versions; includes G/T notes, not other participants.
-- This is an audit fingerprint, never a generator fingerprint or an editorial test oracle.
WITH concept_states AS (
select ic.code, jsonb_build_object(
        'concept', to_jsonb(ic) - ARRAY['id','created_at','updated_at','version'],
        'roles', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select ref.code as v from ingredient_functional_role rel join functional_role ref on ref.id = rel.functional_role_id where rel.ingredient_concept_id = ic.id) values_by_code),
        'flags', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select ref.code as v from ingredient_culinary_flag rel join culinary_flag ref on ref.id = rel.culinary_flag_id where rel.ingredient_concept_id = ic.id) values_by_code),
        'dimensions', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select jsonb_build_array(ref.code, rel.level) as v from ingredient_culinary_dimension rel join culinary_dimension ref on ref.id = rel.culinary_dimension_id where rel.ingredient_concept_id = ic.id) values_by_code),
        'countries', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select rel.country_code as v from ingredient_culinary_country rel  where rel.ingredient_concept_id = ic.id) values_by_code),
        'seasonality', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select jsonb_build_array(rel.month, rel.weight_multiplier) as v from ingredient_seasonality rel  where rel.ingredient_concept_id = ic.id) values_by_code),
        'availability', (select coalesce(jsonb_agg(v order by v::text collate "C"), '[]'::jsonb) from (select jsonb_build_array(ref.code, rel.availability_level) as v from ingredient_availability rel join participant ref on ref.id = rel.participant_id and ref.code in ('GEORGIA','TOBIAS') where rel.ingredient_concept_id = ic.id) values_by_code),
        'parents', (select coalesce(jsonb_agg(parent.code order by parent.code collate "C"), '[]'::jsonb) from ingredient_refinement rel join ingredient_concept parent on parent.id = rel.parent_concept_id where rel.child_concept_id = ic.id)
    ) as state from ingredient_concept ic
), snapshot AS (
SELECT jsonb_build_object(
    'concepts', (SELECT jsonb_agg(state ORDER BY code COLLATE "C") FROM concept_states),
    'availabilityNotes', (
        SELECT jsonb_agg(jsonb_build_array(c.code, p.code, a.curator_note)
                         ORDER BY c.code COLLATE "C", p.code COLLATE "C")
        FROM ingredient_availability a
        JOIN ingredient_concept c ON c.id = a.ingredient_concept_id
        JOIN participant p ON p.id = a.participant_id
        WHERE p.code IN ('GEORGIA','TOBIAS')
    )
) AS value
)
SELECT encode(sha256(convert_to(value::text, 'UTF8')), 'hex') AS catalog_sha256 FROM snapshot;
