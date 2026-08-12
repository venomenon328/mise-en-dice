package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditEntityType;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditQueries.CatalogAuditSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries.CatalogExclusionSearchCriteria;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
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

/** PostgreSQL read-model coverage for Phase-8 exclusion and audit administration queries. */
@SpringBootTest
@Testcontainers
class CatalogPhase8QueryIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE30_QUERY_";
    private static final String ACTOR = "issue30-query-admin";

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
    private CatalogAuditQueries auditQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
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

    @Test
    void filtersPagesAndLoadsAuditDetailsAndEntityHistoryFromPostgresql() {
        OffsetDateTime base = OffsetDateTime.of(2026, 8, 12, 10, 0, 0, 0, ZoneOffset.UTC);
        long repeatedEntity = 900_001L;
        for (int index = 0; index < 30; index++) {
            long entityId = index < 2 ? repeatedEntity : 900_100L + index;
            String before = "{\"displayName\":\"Vorher " + index
                    + "\",\"availability\":[{\"code\":\"GEORGIA\",\"displayName\":\"Georgia\",\"description\":null,\"level\":\"EASY\"}]}";
            String after = "{\"displayName\":\"Nachher " + index
                    + "\",\"availability\":[{\"code\":\"GEORGIA\",\"displayName\":\"Georgia\",\"description\":null,\"level\":\"DIFFICULT\"}]}";
            insertAudit(entityId, "UPDATE", before, after, base.plusMinutes(index));
        }
        insertAudit(999_999L, "CREATE", null,
                "{\"displayText\":\"anderer Typ\",\"code\":\"OTHER\",\"active\":true,\"targets\":[]}",
                base.plusHours(2), "EXCLUSION_RULE");

        CatalogAuditSearchCriteria criteria = new CatalogAuditSearchCriteria(
                ACTOR, base.minusMinutes(1), base.plusHours(1), CatalogAuditEntityType.INGREDIENT_CONCEPT,
                null, "UPDATE", 0, 25);
        var firstPage = auditQueries.search(criteria);
        assertThat(firstPage.totalItems()).isEqualTo(30);
        assertThat(firstPage.pageCount()).isEqualTo(2);
        assertThat(firstPage.items()).hasSize(25);
        var secondPage = auditQueries.search(new CatalogAuditSearchCriteria(
                ACTOR, base.minusMinutes(1), base.plusHours(1), CatalogAuditEntityType.INGREDIENT_CONCEPT,
                null, "UPDATE", 1, 25));
        assertThat(secondPage.items()).hasSize(5);

        var history = auditQueries.findEntityHistory(CatalogAuditEntityType.INGREDIENT_CONCEPT, repeatedEntity, 5);
        assertThat(history).hasSize(2);
        long entryId = history.getFirst().id();
        var detail = auditQueries.findAuditEntry(entryId).orElseThrow();
        assertThat(detail.entityLabel()).startsWith("Nachher");
        assertThat(detail.diff())
                .anySatisfy(diff -> assertThat(diff.label()).isEqualTo("Anzeigename"))
                .anySatisfy(diff -> {
                    assertThat(diff.label()).isEqualTo("Beschaffbarkeit");
                    assertThat(diff.afterValue()).contains("Georgia", "DIFFICULT");
                });

        var entityFiltered = auditQueries.search(new CatalogAuditSearchCriteria(
                ACTOR, null, null, CatalogAuditEntityType.INGREDIENT_CONCEPT, repeatedEntity, "", 0, 25));
        assertThat(entityFiltered.totalItems()).isEqualTo(2);
        assertThat(entityFiltered.items()).allSatisfy(item -> assertThat(item.entityId()).isEqualTo(repeatedEntity));
    }

    private long insertConcept(String suffix, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, ?, false, 'SPECIFIC', 1.0000) returning id
                """, Long.class, PREFIX + suffix, "Issue thirty query " + suffix, active);
    }

    private long insertRule(String suffix, String text, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into exclusion_rule (code, display_text, active, base_draw_weight)
                values (?, ?, ?, 1.0000) returning id
                """, Long.class, PREFIX + suffix, text, active);
    }

    private void insertAudit(long entityId, String action, String before, String after, OffsetDateTime occurredAt) {
        insertAudit(entityId, action, before, after, occurredAt, "INGREDIENT_CONCEPT");
    }

    private void insertAudit(
            long entityId,
            String action,
            String before,
            String after,
            OffsetDateTime occurredAt,
            String entityType
    ) {
        jdbcTemplate.update("""
                insert into catalog_audit_entry
                    (change_group_id, actor_key, entity_type, entity_id, action, before_state, after_state, payload_version, occurred_at)
                values (?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), 1, ?)
                """, UUID.randomUUID(), ACTOR, entityType, entityId, action, before, after, occurredAt);
    }
}
