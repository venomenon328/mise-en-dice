package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionSearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL read-model coverage for Phase-8 exclusion administration queries. */
@SpringBootTest
@Testcontainers
class CatalogPhase8QueryIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE30_QUERY_";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CatalogExclusionQueries exclusionQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from exclusion_rule where code like ?", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void filtersAndPagesExclusionsWithTargetProjectionAndExclusiveRefinementPresence() {
        long activeTarget = insertConcept("ACTIVE_TARGET", true);
        long inactiveTarget = insertConcept("INACTIVE_TARGET", false);
        long matchingRule = insertRule("MATCH", "Passender Ausschluss", true);
        long noRefinementRule = insertRule("NO_REFINEMENT", "Ausschluss ohne Konkretisierungen", true);
        long mixedRule = insertRule("MIXED", "Gemischter Ausschluss", true);
        jdbcTemplate.update("""
                insert into exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                values (?, ?, true), (?, ?, false), (?, ?, true), (?, ?, false)
                """, matchingRule, inactiveTarget, noRefinementRule, activeTarget,
                mixedRule, inactiveTarget, mixedRule, activeTarget);

        var filtered = exclusionQueries.search(new CatalogExclusionSearchCriteria(
                true, inactiveTarget, true, 0, 25));
        assertThat(filtered.items()).extracting(CatalogExclusionQueries.CatalogExclusionListItem::id)
                .containsExactlyInAnyOrder(matchingRule, mixedRule);

        var withoutRefinements = exclusionQueries.search(new CatalogExclusionSearchCriteria(
                true, null, false, 0, 25));
        assertThat(withoutRefinements.items()).extracting(CatalogExclusionQueries.CatalogExclusionListItem::id)
                .contains(noRefinementRule)
                .doesNotContain(matchingRule, mixedRule);

        var detail = exclusionQueries.findExclusionRule(matchingRule).orElseThrow();
        assertThat(detail.targets()).singleElement().satisfies(target -> {
            assertThat(target.ingredientConceptId()).isEqualTo(inactiveTarget);
            assertThat(target.active()).isFalse();
            assertThat(target.includeRefinements()).isTrue();
        });
        assertThat(exclusionQueries.searchTargetCandidates("inactive_target"))
                .anySatisfy(candidate -> assertThat(candidate.id()).isEqualTo(inactiveTarget));
    }

    private long insertConcept(String suffix, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled,
                    challenge_specificity, base_draw_weight, curator_note)
                values (?, ?, ?, false, 'SPECIFIC', 1.0000, 'Technische Testnotiz.') returning id
                """, Long.class, PREFIX + suffix, "Issue thirty query " + suffix, active);
    }

    private long insertRule(String suffix, String text, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into exclusion_rule (code, display_text, active, base_draw_weight)
                values (?, ?, ?, 1.0000) returning id
                """, Long.class, PREFIX + suffix, text, active);
    }

}
