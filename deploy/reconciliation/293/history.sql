CREATE TEMP TABLE issue_293_history (position integer PRIMARY KEY, id text, author text, filename text);
INSERT INTO issue_293_history VALUES
    (1, '001-catalog-schema', 'venomenon328', 'db/changelog/schema/001-catalog-schema.sql'),
    (2, '002-challenge-history-schema', 'venomenon328', 'db/changelog/schema/002-challenge-history-schema.sql'),
    (3, '001-reference-data', 'venomenon328', 'db/changelog/reference/001-reference-data.sql'),
    (4, '002-ingredient-catalog', 'venomenon328', 'db/changelog/catalog/002-ingredient-catalog.sql'),
    (5, '003-functional-roles', 'venomenon328', 'db/changelog/catalog/003-functional-roles.sql'),
    (6, '004-availability', 'venomenon328', 'db/changelog/catalog/004-availability.sql'),
    (7, '005-culinary-properties', 'venomenon328', 'db/changelog/catalog/005-culinary-properties.sql'),
    (8, '006-seasonality', 'venomenon328', 'db/changelog/catalog/006-seasonality.sql'),
    (9, '007-exclusion-rules', 'venomenon328', 'db/changelog/catalog/007-exclusion-rules.sql'),
    (10, '008-ingredient-catalog-expansion-1', 'venomenon328', 'db/changelog/catalog/008-ingredient-catalog-expansion-1.sql'),
    (11, '009-ingredient-catalog-expansion-2', 'venomenon328', 'db/changelog/catalog/009-ingredient-catalog-expansion-2.sql'),
    (12, '010-ingredient-catalog-expansion-3', 'venomenon328', 'db/changelog/catalog/010-ingredient-catalog-expansion-3.sql'),
    (13, '011-ingredient-refinements-expansion', 'venomenon328', 'db/changelog/catalog/011-ingredient-refinements-expansion.sql'),
    (14, '012-seasonality-expansion', 'venomenon328', 'db/changelog/catalog/012-seasonality-expansion.sql'),
    (15, '013-exclusion-rules-expansion', 'venomenon328', 'db/changelog/catalog/013-exclusion-rules-expansion.sql'),
    (16, '001-seed-sanity', 'venomenon328', 'db/changelog/checks/001-seed-sanity.sql'),
    (17, '003-administration-foundation', 'venomenon328', 'db/changelog/schema/003-administration-foundation.sql'),
    (18, '014-catalog-consolidation', 'venomenon328', 'db/changelog/catalog/014-catalog-consolidation.sql'),
    (19, '002-final-catalog-sanity', 'venomenon328', 'db/changelog/checks/002-final-catalog-sanity.sql'),
    (20, '015-catalog-gap-review', 'venomenon328', 'db/changelog/catalog/015-catalog-gap-review.sql'),
    (21, '003-catalog-gap-sanity', 'venomenon328', 'db/changelog/checks/003-catalog-gap-sanity.sql'),
    (22, '004-persisted-candidate-generation', 'venomenon328', 'db/changelog/schema/004-persisted-candidate-generation.sql'),
    (23, '016-final-catalog-snapshot', 'venomenon328', 'db/changelog/catalog/016-final-catalog-snapshot.sql'),
    (24, '017-no-beef-veal-exclusion', 'venomenon328', 'db/changelog/catalog/017-no-beef-veal-exclusion.sql'),
    (25, '005-curation-offer-lifecycle', 'venomenon328', 'db/changelog/schema/005-curation-offer-lifecycle.sql'),
    (26, '006-curation-state-machine-hardening', 'venomenon328', 'db/changelog/schema/006-curation-state-machine-hardening.sql'),
    (27, '007-bounded-curator-dispatch', 'venomenon328', 'db/changelog/schema/007-bounded-curator-dispatch.sql'),
    (28, '008-offer-decision-lifecycle', 'venomenon328', 'db/changelog/schema/008-offer-decision-lifecycle.sql'),
    (29, '009-challenge-voting-participation', 'venomenon328', 'db/changelog/schema/009-challenge-voting-participation.sql'),
    (30, '010-selection-voting-review-hardening', 'venomenon328', 'db/changelog/schema/010-selection-voting-review-hardening.sql'),
    (31, '011-candidate-specific-restrictions', 'venomenon328', 'db/changelog/schema/011-candidate-specific-restrictions.sql'),
    (32, '012-remove-legacy-generator-compatibility', 'venomenon328', 'db/changelog/schema/012-remove-legacy-generator-compatibility.sql'),
    (33, '013-challenge-archive-core', 'venomenon328', 'db/changelog/schema/013-challenge-archive-core.sql'),
    (34, '014-participant-electorate-core', 'venomenon328', 'db/changelog/schema/014-participant-electorate-core.sql'),
    (35, '015-challenge-results-completion-core', 'venomenon328', 'db/changelog/schema/015-challenge-results-completion-core.sql'),
    (36, '016-result-open-requirement-concretizations', 'venomenon328', 'db/changelog/schema/016-result-open-requirement-concretizations.sql'),
    (37, '017-culinary-country-associations', 'venomenon328', 'db/changelog/schema/017-culinary-country-associations.sql'),
    (38, '017-culinary-country-reference-data', 'venomenon328', 'db/changelog/reference/002-culinary-countries.sql'),
    (39, '018-sweden-curation', 'venomenon328', 'db/changelog/catalog/018-sweden-curation.sql'),
    (40, '019-poland-curation', 'venomenon328', 'db/changelog/catalog/019-poland-curation.sql'),
    (41, '020-denmark-curation', 'venomenon328', 'db/changelog/catalog/020-denmark-curation.sql'),
    (42, '021-netherlands-curation', 'venomenon328', 'db/changelog/catalog/021-netherlands-curation.sql'),
    (43, '022-belgium-curation', 'venomenon328', 'db/changelog/catalog/022-belgium-curation.sql'),
    (44, '023-czechia-curation', 'venomenon328', 'db/changelog/catalog/023-czechia-curation.sql'),
    (45, '024-philippines-curation', 'venomenon328', 'db/changelog/catalog/024-philippines-curation.sql'),
    (46, '025-germany-curation', 'venomenon328', 'db/changelog/catalog/025-germany-curation.sql'),
    (47, '026-norway-curation', 'venomenon328', 'db/changelog/catalog/026-norway-curation.sql'),
    (48, '027-france-curation', 'venomenon328', 'db/changelog/catalog/027-france-curation.sql'),
    (49, '028-curator-note-completeness', 'venomenon328', 'db/changelog/catalog/028-curator-note-completeness.sql'),
    (50, '029-spain-curation', 'venomenon328', 'db/changelog/catalog/029-spain-curation.sql'),
    (51, '030-veal-concept-expansion', 'venomenon328', 'db/changelog/catalog/030-veal-concept-expansion.sql'),
    (52, '031-vietnam-curation', 'venomenon328', 'db/changelog/catalog/031-vietnam-curation.sql'),
    (53, '032-thailand-curation', 'venomenon328', 'db/changelog/catalog/032-thailand-curation.sql'),
    (54, '018-five-level-availability', 'venomenon328', 'db/changelog/schema/018-five-level-availability.sql'),
    (55, '019-availability-curator-note', 'venomenon328', 'db/changelog/schema/019-availability-curator-note.sql'),
    (56, '033-availability-novelty-final-review', 'venomenon328', 'db/changelog/catalog/033-availability-novelty-final-review.sql'),
    (57, '020-remove-generator-replay-result', 'mise-en-dice', 'db/changelog/schema/020-remove-generator-replay-result.sql'),
    (58, '034-austria-curation', 'venomenon328', 'db/changelog/catalog/034-austria-curation.sql'),
    (59, '021-culinary-country-subdivision-codes', 'venomenon328', 'db/changelog/schema/021-culinary-country-subdivision-codes.sql'),
    (60, '003-england-culinary-country', 'venomenon328', 'db/changelog/reference/003-england-culinary-country.sql'),
    (61, '035-england-curation', 'venomenon328', 'db/changelog/catalog/035-england-curation.sql'),
    (62, '036-availability-note-prefix-cleanup', 'venomenon328', 'db/changelog/catalog/036-availability-note-prefix-cleanup.sql'),
    (63, '037-availability-note-consolidation', 'venomenon328', 'db/changelog/catalog/037-availability-note-consolidation.sql'),
    (64, '022-remove-runtime-catalog-audit', 'venomenon328', 'db/changelog/schema/022-remove-runtime-catalog-audit.sql'),
    (65, '023-ingredient-concept-aliases', 'mise-en-dice', 'db/changelog/schema/023-ingredient-concept-aliases.sql'),
    (66, '038-availability-note-sentence-capitalization', 'venomenon328', 'db/changelog/catalog/038-availability-note-sentence-capitalization.sql'),
    (67, '039-availability-r3-corrections', 'venomenon328', 'db/changelog/catalog/039-availability-r3-corrections.sql'),
    (68, '004-scotland-culinary-country', 'venomenon328', 'db/changelog/reference/004-scotland-culinary-country.sql'),
    (69, '040-scotland-curation', 'venomenon328', 'db/changelog/catalog/040-scotland-curation.sql'),
    (70, '041-finland-curation', 'venomenon328', 'db/changelog/catalog/041-finland-curation.sql'),
    (71, '042-d3-approved-country-relations', 'venomenon328', 'db/changelog/catalog/042-d3-approved-country-relations.sql'),
    (72, '043-germany-curation', 'venomenon328', 'db/changelog/catalog/043-germany-curation.sql'),
    (73, '044-ingredient-name-alias-curation', 'venomenon328', 'db/changelog/catalog/044-ingredient-name-alias-curation.sql'),
    (74, '045-crustacean-catalog-expansion', 'venomenon328', 'db/changelog/catalog/045-crustacean-catalog-expansion.sql'),
    (75, '046-japan-curation', 'venomenon328', 'db/changelog/catalog/046-japan-curation.sql'),
    (76, '047-catalog-draw-weight-calibration', 'venomenon328', 'db/changelog/catalog/047-catalog-draw-weight-calibration.sql');
CREATE OR REPLACE FUNCTION pg_temp.issue_293_state() RETURNS text LANGUAGE plpgsql AS $$
DECLARE n integer;
BEGIN
    IF EXISTS (SELECT 1 FROM databasechangeloglock WHERE locked) THEN RETURN 'INVALID'; END IF;
    SELECT count(*) INTO n FROM databasechangelog;
    IF n NOT IN (62, 77) THEN RETURN 'INVALID'; END IF;
    IF EXISTS (
        SELECT 1 FROM (
            SELECT row_number() OVER (ORDER BY orderexecuted, dateexecuted) AS position, *
            FROM databasechangelog
        ) actual
        FULL JOIN issue_293_history expected USING (position)
        WHERE coalesce(expected.position, actual.position) <= CASE WHEN n=62 THEN 62 ELSE 76 END
          AND (actual.id IS DISTINCT FROM expected.id OR actual.author IS DISTINCT FROM expected.author
               OR actual.filename IS DISTINCT FROM expected.filename
               OR actual.exectype NOT IN ('EXECUTED','MARK_RAN'))
    ) OR EXISTS (SELECT orderexecuted FROM databasechangelog GROUP BY orderexecuted HAVING count(*) > 1)
    THEN RETURN 'INVALID'; END IF;
    IF n=62 THEN
        IF NOT EXISTS (SELECT 1 FROM databasechangelog WHERE id='033-availability-novelty-final-review' AND exectype='MARK_RAN')
           OR EXISTS (SELECT 1 FROM databasechangelog WHERE id IN ('034-austria-curation','035-england-curation','036-availability-note-prefix-cleanup') AND exectype <> 'EXECUTED')
        THEN RETURN 'INVALID'; END IF;
        RETURN 'PENDING';
    END IF;
    IF EXISTS (SELECT 1 FROM databasechangelog WHERE id='293-editorial-upgrade-corridor'
               AND author='venomenon328' AND filename='deploy/reconciliation/293'
               AND exectype='EXECUTED' AND orderexecuted=(SELECT max(orderexecuted) FROM databasechangelog))
    THEN RETURN 'APPLIED'; END IF;
    RETURN 'INVALID';
END $$;
