-- Mise en Dice - initial curated exclusion rules
-- Requires: db/migrations/001_catalog_schema.sql
-- Requires: db/seeds/002_ingredient_catalog.sql
--
-- Exclusion rules are a deliberately small curated set. They are not generated
-- automatically from ingredient concepts.

INSERT INTO exclusion_rule (code, display_text, active, base_draw_weight, curator_note)
VALUES
    ('NO_COCONUT_MILK', 'keine Kokosmilch', true, 1.0000, NULL),
    ('NO_RICE', 'kein Reis', true, 1.0000, NULL),
    ('NO_NOODLES', 'keine Nudeln', true, 1.0000, NULL),
    ('NO_SOY_SAUCE', 'keine Sojasauce', true, 1.0000, NULL),
    ('NO_DAIRY', 'keine Milchprodukte', true, 0.8000, NULL),
    ('NO_READY_CURRY_PASTE', 'keine fertige Currypaste', true, 0.8000, NULL)
ON CONFLICT (code) DO NOTHING;

INSERT INTO exclusion_rule_target (
    exclusion_rule_id,
    ingredient_concept_id,
    include_refinements
)
SELECT er.id, ic.id, target.include_refinements
FROM (
    VALUES
        ('NO_COCONUT_MILK', 'COCONUT_MILK', false),
        ('NO_RICE', 'RICE', false),
        ('NO_NOODLES', 'NOODLES', true),
        ('NO_SOY_SAUCE', 'SOY_SAUCE', false),
        ('NO_DAIRY', 'DAIRY_PRODUCTS', true),
        ('NO_READY_CURRY_PASTE', 'READY_CURRY_PASTE', true)
) AS target(rule_code, concept_code, include_refinements)
JOIN exclusion_rule er ON er.code = target.rule_code
JOIN ingredient_concept ic ON ic.code = target.concept_code
ON CONFLICT (exclusion_rule_id, ingredient_concept_id) DO NOTHING;
