-- Mise en Dice - exclusion rules for the expanded catalog
-- Requires: db/migrations/001_catalog_schema.sql
-- Requires: db/seeds/011_ingredient_refinements_expansion.sql
--
-- Exclusion rules remain an intentionally curated subset. Existing rule text,
-- weights and targets are preserved on re-run.

INSERT INTO exclusion_rule (
    code,
    display_text,
    active,
    base_draw_weight,
    curator_note
)
VALUES
    ('NO_MEAT', 'kein Fleisch', true, 0.5500, 'Breite Ausschlussregel; betrifft über den Konkretisierungsgraphen auch Geflügel und sämtliche hinterlegten Fleischarten.'),
    ('NO_PORK', 'kein Schweinefleisch', true, 0.7500, NULL),
    ('NO_BEEF', 'kein Rindfleisch', true, 0.7500, NULL),
    ('NO_POULTRY', 'kein Geflügel', true, 0.7500, NULL),
    ('NO_FISH_OR_SEAFOOD', 'kein Fisch und keine Meeresfrüchte', true, 0.6000, NULL),
    ('NO_EGGS', 'keine Eier', true, 0.7500, NULL),
    ('NO_NUTS', 'keine Nüsse', true, 0.7000, NULL),
    ('NO_SEEDS', 'keine Kerne oder Samen', true, 0.6500, NULL),
    ('NO_LEGUMES', 'keine Hülsenfrüchte', true, 0.6500, 'Betrifft über die bekannten Konkretisierungen auch hinterlegte Sojaprodukte.'),
    ('NO_CHILI', 'keine Chili', true, 0.7000, NULL),
    ('NO_COOKING_ALCOHOL', 'kein Kochalkohol', true, 0.6500, NULL),
    ('NO_MUSHROOMS', 'keine Pilze', true, 0.7000, NULL),
    ('NO_ALLIUMS', 'keine Zwiebel-, Knoblauch- oder Lauchgewächse', true, 0.6000, NULL),
    ('NO_TOMATO', 'keine Tomaten', true, 0.7000, NULL),
    ('NO_ADDED_SWEETENER', 'kein zusätzliches Süßungsmittel', true, 0.6000, NULL),
    ('NO_READY_SAUCES', 'keine fertige Sauce oder Würzpaste', true, 0.4500, 'Sehr breite und entsprechend niedriger gewichtete Ausschlussregel.')
ON CONFLICT (code) DO NOTHING;

INSERT INTO exclusion_rule_target (
    exclusion_rule_id,
    ingredient_concept_id,
    include_refinements
)
SELECT er.id, ic.id, target.include_refinements
FROM (
    VALUES
    ('NO_MEAT', 'MEAT', true),
    ('NO_PORK', 'PORK', true),
    ('NO_BEEF', 'BEEF', true),
    ('NO_POULTRY', 'POULTRY', true),
    ('NO_FISH_OR_SEAFOOD', 'SEAFOOD', true),
    ('NO_EGGS', 'EGGS', true),
    ('NO_NUTS', 'NUTS', true),
    ('NO_SEEDS', 'SEEDS', true),
    ('NO_LEGUMES', 'LEGUMES', true),
    ('NO_CHILI', 'CHILI', true),
    ('NO_CHILI', 'CHILI_CONDIMENTS', true),
    ('NO_COOKING_ALCOHOL', 'COOKING_ALCOHOL', true),
    ('NO_MUSHROOMS', 'MUSHROOMS', true),
    ('NO_ALLIUMS', 'ALLIUM_VEGETABLES', true),
    ('NO_TOMATO', 'TOMATO', true),
    ('NO_TOMATO', 'TOMATO_PRODUCTS', true),
    ('NO_ADDED_SWEETENER', 'SWEETENERS', true),
    ('NO_READY_SAUCES', 'SAUCES_AND_PASTES', true)
) AS target(rule_code, concept_code, include_refinements)
JOIN exclusion_rule er ON er.code = target.rule_code
JOIN ingredient_concept ic ON ic.code = target.concept_code
ON CONFLICT (exclusion_rule_id, ingredient_concept_id) DO NOTHING;
