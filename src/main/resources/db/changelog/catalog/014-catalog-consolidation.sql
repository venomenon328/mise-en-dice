--liquibase formatted sql
--changeset venomenon328:014-catalog-consolidation splitStatements:false

-- Mise en Dice - catalog hierarchy and draw-weight consolidation
-- Requires: catalog/013-exclusion-rules-expansion.sql
-- Requires: schema/003-administration-foundation.sql
--
-- This is an explicit, one-time curated data correction. New parent links close
-- avoidable roots, the graph is reduced to non-redundant direct relations, and
-- implausibly prominent specialty weights are lowered. Weight changes only
-- apply to untouched version-0 rows that still carry the exact old baseline
-- value, so an already edited operational value wins over this migration.

INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
SELECT parent.id, child.id
FROM (
    VALUES
        ('VEGETABLES', 'SEAWEED'),
        ('VEGETABLES', 'ARTICHOKE'),
        ('STEM_VEGETABLES', 'BAMBOO_SHOOTS'),
        ('SPICES', 'CHILI'),
        ('FRUIT', 'POMEGRANATE'),
        ('SPICES', 'GINGER'),
        ('FRUIT', 'PERSIMMON'),
        ('PRESERVED_PRODUCE', 'CAPERS'),
        ('GRAINS', 'CORN'),
        ('ROOT_VEGETABLES', 'HORSERADISH'),
        ('SPICES', 'MSG'),
        ('PRESERVED_PRODUCE', 'OLIVES'),
        ('GARLIC', 'BLACK_GARLIC'),
        ('TROPICAL_FRUIT', 'TAMARIND'),
        ('MOLLUSCS', 'ESCARGOT'),
        ('FRUIT', 'GRAPE'),
        ('FRESH_HERBS', 'LEMONGRASS')
) AS relation(parent_code, child_code)
JOIN ingredient_concept parent
  ON parent.code = relation.parent_code
JOIN ingredient_concept child
  ON child.code = relation.child_code
ON CONFLICT (parent_concept_id, child_concept_id) DO NOTHING;

-- A sprout is produced from a mung bean, but is not a more specific culinary
-- interpretation of the challenge requirement "Mungbohnen".
DELETE FROM ingredient_refinement relation
USING ingredient_concept parent, ingredient_concept child
WHERE relation.parent_concept_id = parent.id
  AND relation.child_concept_id = child.id
  AND parent.code = 'MUNG_BEANS'
  AND child.code = 'BEAN_SPROUTS';

-- Direct relations are presentation and maintenance edges. Since the graph is
-- explicitly transitive, a direct edge adds no information when another path
-- of length two or more already reaches the same child.
WITH RECURSIVE alternate_paths(parent_concept_id, child_concept_id) AS (
    SELECT first.parent_concept_id, second.child_concept_id
    FROM ingredient_refinement first
    JOIN ingredient_refinement second
      ON second.parent_concept_id = first.child_concept_id

    UNION

    SELECT alternate.parent_concept_id, next.child_concept_id
    FROM alternate_paths alternate
    JOIN ingredient_refinement next
      ON next.parent_concept_id = alternate.child_concept_id
)
DELETE FROM ingredient_refinement direct
USING alternate_paths alternate
WHERE direct.parent_concept_id = alternate.parent_concept_id
  AND direct.child_concept_id = alternate.child_concept_id;

INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
SELECT concept.id, role.id
FROM (
    VALUES
        ('EDAMAME', 'VEGETABLE'),
        ('HORSERADISH', 'VEGETABLE'),
        ('SEAWEED', 'VEGETABLE'),
        ('TAMARIND', 'FRUIT')
) AS assignment(concept_code, role_code)
JOIN ingredient_concept concept
  ON concept.code = assignment.concept_code
JOIN functional_role role
  ON role.code = assignment.role_code
ON CONFLICT (ingredient_concept_id, functional_role_id) DO NOTHING;

UPDATE ingredient_concept concept
SET base_draw_weight = correction.new_weight
FROM (
    VALUES
        ('BEER', 0.5000::numeric, 0.2500::numeric),
        ('CIDER', 0.4000::numeric, 0.2000::numeric),
        ('MIRIN', 0.4500::numeric, 0.3000::numeric),
        ('RED_WINE', 0.5500::numeric, 0.3500::numeric),
        ('SAKE', 0.4500::numeric, 0.2500::numeric),
        ('SHAOXING_WINE', 0.4000::numeric, 0.3000::numeric),
        ('SHERRY', 0.4000::numeric, 0.2500::numeric),
        ('WHITE_WINE', 0.5500::numeric, 0.3500::numeric),
        ('SQUID', 0.6500::numeric, 0.5000::numeric),
        ('SCALLOPS', 0.4500::numeric, 0.3000::numeric),
        ('OCTOPUS', 0.4500::numeric, 0.3000::numeric),
        ('MONKFISH', 0.4500::numeric, 0.3000::numeric),
        ('BEEF_CHEEK', 0.4000::numeric, 0.3500::numeric),
        ('SWORDFISH', 0.4000::numeric, 0.3500::numeric),
        ('TAMARIND', 0.4000::numeric, 0.3000::numeric),
        ('YAM', 0.4000::numeric, 0.3500::numeric),
        ('VEAL_SHANK', 0.4500::numeric, 0.3500::numeric),
        ('PORK_CHEEK', 0.4500::numeric, 0.3500::numeric),
        ('CLAMS', 0.4500::numeric, 0.3500::numeric)
) AS correction(code, old_weight, new_weight)
WHERE concept.code = correction.code
  AND concept.version = 0
  AND concept.base_draw_weight = correction.old_weight;
