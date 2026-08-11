-- Mise en Dice - seed sanity checks
-- Run after schema migrations and all db/seeds/*.sql files.
-- Fails fast when the baseline catalog is incomplete in structurally relevant ways.

DO $$
DECLARE
    draw_count integer;
    open_count integer;
    specific_count integer;
    missing_roles integer;
    missing_availability integer;
    targetless_exclusions integer;
BEGIN
    SELECT count(*)
      INTO draw_count
      FROM ingredient_concept
     WHERE active
       AND random_draw_enabled;

    IF draw_count < 100 THEN
        RAISE EXCEPTION 'draw pool unexpectedly small: % active draw concepts', draw_count;
    END IF;

    SELECT count(*)
      INTO open_count
      FROM ingredient_concept
     WHERE active
       AND random_draw_enabled
       AND challenge_specificity = 'OPEN';

    IF open_count < 10 THEN
        RAISE EXCEPTION 'open requirement pool unexpectedly small: % concepts', open_count;
    END IF;

    SELECT count(*)
      INTO specific_count
      FROM ingredient_concept
     WHERE active
       AND random_draw_enabled
       AND challenge_specificity = 'SPECIFIC';

    IF specific_count < 50 THEN
        RAISE EXCEPTION 'specific requirement pool unexpectedly small: % concepts', specific_count;
    END IF;

    SELECT count(*)
      INTO missing_roles
      FROM ingredient_concept ic
     WHERE ic.active
       AND ic.random_draw_enabled
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_functional_role ifr
            WHERE ifr.ingredient_concept_id = ic.id
       );

    IF missing_roles <> 0 THEN
        RAISE EXCEPTION '% active draw concepts have no functional role', missing_roles;
    END IF;

    SELECT count(*)
      INTO missing_availability
      FROM ingredient_concept ic
      CROSS JOIN (
          SELECT id
            FROM participant
           WHERE code IN ('TOBIAS', 'GEORGIA')
      ) expected_participant
     WHERE ic.active
       AND ic.random_draw_enabled
       AND NOT EXISTS (
           SELECT 1
             FROM ingredient_availability ia
            WHERE ia.ingredient_concept_id = ic.id
              AND ia.participant_id = expected_participant.id
       );

    IF missing_availability <> 0 THEN
        RAISE EXCEPTION '% required participant availability rows are missing', missing_availability;
    END IF;

    IF (
        SELECT count(*)
          FROM participant
         WHERE code IN ('TOBIAS', 'GEORGIA')
           AND active
    ) <> 2 THEN
        RAISE EXCEPTION 'expected active participants TOBIAS and GEORGIA';
    END IF;

    SELECT count(*)
      INTO targetless_exclusions
      FROM exclusion_rule er
     WHERE er.active
       AND NOT EXISTS (
           SELECT 1
             FROM exclusion_rule_target ert
            WHERE ert.exclusion_rule_id = er.id
       );

    IF targetless_exclusions <> 0 THEN
        RAISE EXCEPTION '% active exclusion rules have no target', targetless_exclusions;
    END IF;
END;
$$;
