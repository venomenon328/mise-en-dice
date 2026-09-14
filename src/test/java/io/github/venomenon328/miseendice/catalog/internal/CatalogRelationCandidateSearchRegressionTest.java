package io.github.venomenon328.miseendice.catalog.internal;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CatalogRelationCandidateSearchRegressionTest extends CurrentSchemaPostgresIntegrationTest {

    private static final String TEST_PREFIX = "TEST_PICKER_";

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeFixtureRows() {
        jdbcTemplate.update("delete from ingredient_concept where code like ?", TEST_PREFIX + "%");
    }

    @Test
    void findsExactTechnicalCodesContainingUnderscoresCaseInsensitively() {
        insertConcept(TEST_PREFIX + "EXACT_UNDERSCORE", "Picker exact underscore");

        assertThat(codes(catalogQueries.searchRelationCandidates(TEST_PREFIX + "EXACT_UNDERSCORE", 0)))
                .contains(TEST_PREFIX + "EXACT_UNDERSCORE");
        assertThat(codes(catalogQueries.searchRelationCandidates(TEST_PREFIX.toLowerCase() + "exact_underscore", 0)))
                .contains(TEST_PREFIX + "EXACT_UNDERSCORE");
    }

    @Test
    void consecutiveDifferentSearchesDoNotReuseThePreviousResult() {
        insertConcept(TEST_PREFIX + "FIRST", "Picker first");
        insertConcept(TEST_PREFIX + "SECOND", "Picker second");

        assertThat(codes(catalogQueries.searchRelationCandidates(TEST_PREFIX + "FIRST", 0)))
                .contains(TEST_PREFIX + "FIRST");
        assertThat(codes(catalogQueries.searchRelationCandidates(TEST_PREFIX + "SECOND", 0)))
                .contains(TEST_PREFIX + "SECOND")
                .doesNotContain(TEST_PREFIX + "FIRST");
    }

    @Test
    void treatsLikeWildcardCharactersAsLiteralPickerText() {
        insertConcept(TEST_PREFIX + "LITERAL", "Picker literal 100%_sure");
        insertConcept(TEST_PREFIX + "DECOY", "Picker literal 100XXsure");

        assertThat(codes(catalogQueries.searchRelationCandidates("%_", 0)))
                .containsExactly(TEST_PREFIX + "LITERAL");
    }

    @Test
    void findsAliasesLiterallyAndDeduplicatesConceptsWithSeveralMatchingAliases() {
        long matching = insertConcept(TEST_PREFIX + "ALIAS", "Picker canonical alias owner");
        insertAlias(matching, "Picker former 100%_\\name");
        insertAlias(matching, "Picker second 100%_\\name");
        insertConcept(TEST_PREFIX + "ALIAS_DECOY", "Picker former 100XXname");

        assertThat(codes(catalogQueries.searchRelationCandidates("100%_\\", 0)))
                .containsExactly(TEST_PREFIX + "ALIAS");
    }

    private long insertConcept(String code, String displayName) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity,
                    base_draw_weight, novelty_level, curator_note
                ) values (?, ?, false, false, 'SPECIFIC', 1.0000, null, 'Technische Testnotiz.')
                returning id
                """, Long.class, code, displayName);
    }

    private void insertAlias(long conceptId, String alias) {
        jdbcTemplate.update(
                "insert into ingredient_concept_alias (ingredient_concept_id, alias_text) values (?, ?)",
                conceptId, alias);
    }

    private static List<String> codes(List<CatalogQueries.CatalogRelationCandidate> candidates) {
        return candidates.stream().map(CatalogQueries.CatalogRelationCandidate::code).toList();
    }
}
