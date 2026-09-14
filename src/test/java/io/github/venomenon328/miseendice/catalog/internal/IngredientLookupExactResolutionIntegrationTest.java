package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Regression coverage for the exact-before-substring /zutat lookup contract from PR #269 review R1. */
@SpringBootTest
class IngredientLookupExactResolutionIntegrationTest extends CurrentSchemaPostgresIntegrationTest {
    private static final String PREFIX = "TEST_INGREDIENT_LOOKUP_B1_";
    private static final String SEARCH_TEXT = "issue 263 b1 exact needle q7v9k";
    private static final String SEARCH_LABEL = "Issue 263 B1 Exact Needle Q7V9K";

    @Autowired
    private IngredientLookupQueries queries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestCatalogData() {
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void resolvesAllExactConceptsBeforeTheDiscordLimitAndExcludesSubstringMatches() {
        long canonicalExact = insertConcept("EXACT_CANONICAL", SEARCH_LABEL);
        long aliasExact = insertConcept("EXACT_ALIAS", "Zulu issue 263 B1 exact alias owner Q7V9K");
        insertAlias(aliasExact, "ISSUE 263 B1 EXACT NEEDLE Q7V9K");

        for (int number = 0; number < 30; number++) {
            insertConcept("PREFIX_" + number, SEARCH_LABEL + " " + String.format("%02d", number));
        }
        insertConcept("CONTAINS", "Former " + SEARCH_TEXT + " label");

        var result = queries.searchActiveByDisplayName("  ISSUE 263 B1 EXACT NEEDLE Q7V9K  ", 25);

        assertThat(result.totalMatches()).isEqualTo(2);
        assertThat(result.hasMoreMatches()).isFalse();
        assertThat(result.matches()).extracting(match -> match.conceptId())
                .containsExactly(canonicalExact, aliasExact);
        assertThat(result.matches()).allSatisfy(match -> assertThat(match.exactMatch()).isTrue());
    }

    private long insertConcept(String suffix, String displayName) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity,
                    base_draw_weight, novelty_level, curator_note
                ) values (?, ?, true, true, 'SPECIFIC', 1.0000, 2, 'Technische B-1-Testnotiz.')
                returning id
                """, Long.class, PREFIX + suffix, displayName);
    }

    private void insertAlias(long conceptId, String alias) {
        jdbcTemplate.update(
                "insert into ingredient_concept_alias (ingredient_concept_id, alias_text) values (?, ?)",
                conceptId, alias);
    }
}
