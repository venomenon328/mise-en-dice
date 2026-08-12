-- Candidate generator data-readiness analysis
-- PostgreSQL / psql
--
-- Run against a database built by the current Liquibase changelog, for example:
--   psql "$DATABASE_URL" -X -v ON_ERROR_STOP=1 \
--     -f docs/analysis/candidate-generator-data-readiness.sql
--
-- The script is read-only and uses one repeatable-read snapshot.

\pset pager off
\pset null '<NULL>'
\timing on

BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;

\echo '=== ACTIVE DRAW POOL ==='
SELECT count(*) AS drawable,
       count(*) FILTER (WHERE challenge_specificity = 'SPECIFIC') AS specific,
       count(*) FILTER (WHERE challenge_specificity = 'OPEN') AS open,
       count(*) FILTER (WHERE novelty_level IS NOT NULL) AS novelty_known,
       count(*) FILTER (WHERE novelty_level IS NULL) AS novelty_unknown
FROM ingredient_concept
WHERE active AND random_draw_enabled;

\echo '=== HARD METADATA COMPLETENESS ==='
WITH pool AS (
    SELECT id
    FROM ingredient_concept
    WHERE active AND random_draw_enabled
), active_participants AS (
    SELECT id
    FROM participant
    WHERE active
)
SELECT (SELECT count(*) FROM pool) AS drawable,
       count(*) FILTER (WHERE EXISTS (
           SELECT 1
           FROM ingredient_functional_role assignment
           WHERE assignment.ingredient_concept_id = pool.id
       )) AS with_role,
       count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1
           FROM ingredient_functional_role assignment
           WHERE assignment.ingredient_concept_id = pool.id
       )) AS without_role,
       count(*) FILTER (WHERE concept.novelty_level IS NOT NULL) AS with_novelty,
       count(*) FILTER (WHERE concept.novelty_level IS NULL) AS without_novelty,
       (SELECT count(*) FROM active_participants) AS active_participants,
       (
           SELECT count(*)
           FROM pool p
           CROSS JOIN active_participants participant
           LEFT JOIN ingredient_availability availability
             ON availability.ingredient_concept_id = p.id
            AND availability.participant_id = participant.id
           WHERE availability.ingredient_concept_id IS NULL
       ) AS missing_availability_assignments
FROM pool
JOIN ingredient_concept concept ON concept.id = pool.id;

\echo '=== ROLE DISTRIBUTION ==='
SELECT role.code,
       count(*) AS total,
       count(*) FILTER (WHERE concept.challenge_specificity = 'SPECIFIC') AS specific,
       count(*) FILTER (WHERE concept.challenge_specificity = 'OPEN') AS open
FROM ingredient_concept concept
JOIN ingredient_functional_role assignment
  ON assignment.ingredient_concept_id = concept.id
JOIN functional_role role
  ON role.id = assignment.functional_role_id
WHERE concept.active AND concept.random_draw_enabled
GROUP BY role.code
ORDER BY role.code;

\echo '=== ROLE SIGNATURES ==='
WITH signatures AS (
    SELECT concept.id,
           concept.challenge_specificity,
           string_agg(role.code, '+' ORDER BY role.code) AS signature
    FROM ingredient_concept concept
    JOIN ingredient_functional_role assignment
      ON assignment.ingredient_concept_id = concept.id
    JOIN functional_role role
      ON role.id = assignment.functional_role_id
    WHERE concept.active AND concept.random_draw_enabled
    GROUP BY concept.id, concept.challenge_specificity
)
SELECT signature,
       count(*) AS total,
       count(*) FILTER (WHERE challenge_specificity = 'SPECIFIC') AS specific,
       count(*) FILTER (WHERE challenge_specificity = 'OPEN') AS open
FROM signatures
GROUP BY signature
ORDER BY total DESC, signature;

\echo '=== BROAD STRUCTURAL POOLS ==='
WITH pool AS (
    SELECT concept.id,
           concept.challenge_specificity,
           bool_or(role.code IN (
               'ANIMAL_PROTEIN', 'PLANT_PROTEIN', 'VEGETABLE',
               'FRUIT', 'STARCH', 'FAT'
           )) AS structural_or_support,
           bool_or(role.code IN ('AROMATIC', 'SEASONING', 'ACID')) AS flavouring
    FROM ingredient_concept concept
    JOIN ingredient_functional_role assignment
      ON assignment.ingredient_concept_id = concept.id
    JOIN functional_role role
      ON role.id = assignment.functional_role_id
    WHERE concept.active AND concept.random_draw_enabled
    GROUP BY concept.id, concept.challenge_specificity
)
SELECT count(*) AS drawable,
       count(*) FILTER (WHERE structural_or_support) AS structural_or_support,
       count(*) FILTER (WHERE NOT structural_or_support AND flavouring) AS flavouring_only,
       count(*) FILTER (
           WHERE structural_or_support AND challenge_specificity = 'SPECIFIC'
       ) AS structural_or_support_specific,
       count(*) FILTER (
           WHERE structural_or_support AND challenge_specificity = 'OPEN'
       ) AS structural_or_support_open,
       count(*) FILTER (
           WHERE NOT structural_or_support
             AND flavouring
             AND challenge_specificity = 'SPECIFIC'
       ) AS flavouring_only_specific,
       count(*) FILTER (
           WHERE NOT structural_or_support
             AND flavouring
             AND challenge_specificity = 'OPEN'
       ) AS flavouring_only_open
FROM pool;

\echo '=== AVAILABILITY PER PARTICIPANT ==='
SELECT participant.code,
       availability.availability_level,
       count(*) AS concepts
FROM ingredient_concept concept
JOIN ingredient_availability availability
  ON availability.ingredient_concept_id = concept.id
JOIN participant
  ON participant.id = availability.participant_id
WHERE concept.active
  AND concept.random_draw_enabled
  AND participant.active
GROUP BY participant.code, availability.availability_level
ORDER BY participant.code, availability.availability_level;

\echo '=== JOINT WORST AVAILABILITY ==='
WITH pool AS (
    SELECT id
    FROM ingredient_concept
    WHERE active AND random_draw_enabled
), ranked AS (
    SELECT pool.id,
           max(CASE availability.availability_level
               WHEN 'EASY' THEN 1
               WHEN 'PLANNED' THEN 2
               WHEN 'DIFFICULT' THEN 3
               WHEN 'UNAVAILABLE' THEN 4
               ELSE 5
           END) AS worst_rank
    FROM pool
    CROSS JOIN (SELECT id FROM participant WHERE active) participant
    LEFT JOIN ingredient_availability availability
      ON availability.ingredient_concept_id = pool.id
     AND availability.participant_id = participant.id
    GROUP BY pool.id
)
SELECT CASE worst_rank
           WHEN 1 THEN 'EASY'
           WHEN 2 THEN 'PLANNED'
           WHEN 3 THEN 'DIFFICULT'
           WHEN 4 THEN 'UNAVAILABLE'
           ELSE 'MISSING'
       END AS worst_level,
       count(*) AS concepts
FROM ranked
GROUP BY worst_rank
ORDER BY worst_rank;

\echo '=== NOVELTY DISTRIBUTION ==='
SELECT coalesce(novelty_level::text, 'UNKNOWN') AS novelty,
       count(*) AS total,
       count(*) FILTER (WHERE challenge_specificity = 'SPECIFIC') AS specific,
       count(*) FILTER (WHERE challenge_specificity = 'OPEN') AS open
FROM ingredient_concept
WHERE active AND random_draw_enabled
GROUP BY novelty_level
ORDER BY novelty_level NULLS LAST;

\echo '=== NOVELTY COVERAGE BY ROLE ==='
SELECT role.code,
       count(DISTINCT concept.id) AS total,
       count(DISTINCT concept.id) FILTER (
           WHERE concept.novelty_level IS NOT NULL
       ) AS known,
       count(DISTINCT concept.id) FILTER (
           WHERE concept.novelty_level IS NULL
       ) AS unknown
FROM ingredient_concept concept
JOIN ingredient_functional_role assignment
  ON assignment.ingredient_concept_id = concept.id
JOIN functional_role role
  ON role.id = assignment.functional_role_id
WHERE concept.active AND concept.random_draw_enabled
GROUP BY role.code
ORDER BY role.code;

\echo '=== DIMENSION COVERAGE ==='
WITH pool AS (
    SELECT id
    FROM ingredient_concept
    WHERE active AND random_draw_enabled
)
SELECT dimension.code,
       count(DISTINCT value.ingredient_concept_id) AS covered,
       (SELECT count(*) FROM pool)
           - count(DISTINCT value.ingredient_concept_id) AS missing,
       round(
           100.0 * count(DISTINCT value.ingredient_concept_id)
           / (SELECT count(*) FROM pool),
           1
       ) AS percent
FROM culinary_dimension dimension
CROSS JOIN pool
LEFT JOIN ingredient_culinary_dimension value
  ON value.culinary_dimension_id = dimension.id
 AND value.ingredient_concept_id = pool.id
GROUP BY dimension.code
ORDER BY dimension.code;

\echo '=== DOMINANCE DISTRIBUTION ==='
SELECT coalesce(value.level::text, 'UNKNOWN') AS dominance,
       count(*) AS concepts
FROM ingredient_concept concept
LEFT JOIN culinary_dimension dimension
  ON dimension.code = 'DOMINANCE'
LEFT JOIN ingredient_culinary_dimension value
  ON value.culinary_dimension_id = dimension.id
 AND value.ingredient_concept_id = concept.id
WHERE concept.active AND concept.random_draw_enabled
GROUP BY value.level
ORDER BY value.level NULLS LAST;

\echo '=== DOMINANCE COVERAGE BY ROLE ==='
SELECT role.code,
       count(DISTINCT concept.id) AS total,
       count(DISTINCT concept.id) FILTER (WHERE value.level IS NOT NULL) AS known,
       count(DISTINCT concept.id) FILTER (WHERE value.level IS NULL) AS unknown
FROM ingredient_concept concept
JOIN ingredient_functional_role assignment
  ON assignment.ingredient_concept_id = concept.id
JOIN functional_role role
  ON role.id = assignment.functional_role_id
LEFT JOIN culinary_dimension dimension
  ON dimension.code = 'DOMINANCE'
LEFT JOIN ingredient_culinary_dimension value
  ON value.culinary_dimension_id = dimension.id
 AND value.ingredient_concept_id = concept.id
WHERE concept.active AND concept.random_draw_enabled
GROUP BY role.code
ORDER BY role.code;

\echo '=== FLAG DISTRIBUTION ==='
SELECT flag.code,
       count(*) AS concepts
FROM ingredient_concept concept
JOIN ingredient_culinary_flag assignment
  ON assignment.ingredient_concept_id = concept.id
JOIN culinary_flag flag
  ON flag.id = assignment.culinary_flag_id
WHERE concept.active AND concept.random_draw_enabled
GROUP BY flag.code
ORDER BY flag.code;

\echo '=== GRAPH SUMMARY ==='
WITH RECURSIVE paths(root_id, current_id, depth) AS (
    SELECT relation.parent_concept_id,
           relation.child_concept_id,
           1
    FROM ingredient_refinement relation

    UNION ALL

    SELECT paths.root_id,
           relation.child_concept_id,
           paths.depth + 1
    FROM paths
    JOIN ingredient_refinement relation
      ON relation.parent_concept_id = paths.current_id
    WHERE paths.depth < 32
), parent_counts AS (
    SELECT child_concept_id,
           count(*) AS parents
    FROM ingredient_refinement
    GROUP BY child_concept_id
)
SELECT (SELECT count(*) FROM ingredient_refinement) AS direct_edges,
       coalesce((SELECT max(depth) FROM paths), 0) AS maximum_depth,
       (SELECT count(*) FROM parent_counts WHERE parents > 1)
           AS multiple_parent_concepts,
       (
           SELECT count(*)
           FROM ingredient_concept concept
           WHERE concept.active
             AND NOT EXISTS (
                 SELECT 1
                 FROM ingredient_refinement relation
                 WHERE relation.child_concept_id = concept.id
             )
       ) AS active_roots,
       (
           SELECT count(*)
           FROM ingredient_concept concept
           WHERE concept.active
             AND NOT EXISTS (
                 SELECT 1
                 FROM ingredient_refinement relation
                 WHERE relation.parent_concept_id = concept.id
             )
       ) AS active_leaves;

\echo '=== DRAWABLE GRAPH COVERAGE ==='
SELECT count(*) AS drawable,
       count(*) FILTER (WHERE EXISTS (
           SELECT 1
           FROM ingredient_refinement relation
           WHERE relation.parent_concept_id = concept.id
              OR relation.child_concept_id = concept.id
       )) AS graph_connected,
       count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1
           FROM ingredient_refinement relation
           WHERE relation.parent_concept_id = concept.id
              OR relation.child_concept_id = concept.id
       )) AS graph_isolated
FROM ingredient_concept concept
WHERE concept.active AND concept.random_draw_enabled;

\echo '=== OPEN CONCEPT CHILD COVERAGE ==='
SELECT count(*) AS drawable_open,
       count(*) FILTER (WHERE EXISTS (
           SELECT 1
           FROM ingredient_refinement relation
           WHERE relation.parent_concept_id = concept.id
       )) AS with_direct_child,
       count(*) FILTER (WHERE NOT EXISTS (
           SELECT 1
           FROM ingredient_refinement relation
           WHERE relation.parent_concept_id = concept.id
       )) AS without_direct_child
FROM ingredient_concept concept
WHERE concept.active
  AND concept.random_draw_enabled
  AND concept.challenge_specificity = 'OPEN';

COMMIT;
