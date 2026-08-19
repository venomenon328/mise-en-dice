package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.IngredientLookupQueries;
import java.math.BigDecimal;
import java.util.List;
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

/** Verifies the Discord lookup projection against PostgreSQL, including its literal search semantics. */
@SpringBootTest
@Testcontainers
class IngredientLookupQueriesIntegrationTest {
    private static final String PREFIX = "TEST_INGREDIENT_LOOKUP_";

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
    private IngredientLookupQueries queries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestCatalogData() {
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void searchesOnlyActiveDisplayNamesWithLiteralCaseInsensitiveSubstringsAndIncludesNonDrawableConcepts() {
        long drawable = insertConcept("DRAWABLE", "Lookup literal 100%_Aktiv", true, true, 3, "sichtbar");
        long grouping = insertConcept("GROUPING", "Lookup literal 100%_Gruppe", true, false, null, null);
        insertConcept("INACTIVE", "Lookup literal 100%_Inaktiv", false, true, 2, null);
        insertConcept("CODE_ONLY_LOOKUP_LITERAL", "völlig anderer Name", true, true, 2, null);

        var result = queries.searchActiveByDisplayName("  100%_  ", 25);

        assertThat(result.totalMatches()).isEqualTo(2);
        assertThat(result.matches()).extracting(match -> match.conceptId()).containsExactly(drawable, grouping);
        assertThat(result.matches()).extracting(match -> match.displayName())
                .containsExactly("Lookup literal 100%_Aktiv", "Lookup literal 100%_Gruppe");
    }

    @Test
    void ranksStartsWithBeforeContainsThenAlphabeticallyAndReturnsTheBoundedTotal() {
        long startsA = insertConcept("START_A", "Alpha Anfang", true, true, 2, null);
        long startsB = insertConcept("START_B", "alpha Anfang 2", true, true, 2, null);
        long contains = insertConcept("CONTAINS", "Eine Alpha Ende", true, true, 2, null);
        for (int number = 0; number < 25; number++) {
            insertConcept("LIMIT_" + number, "alpha Zusatz " + String.format("%02d", number), true, true, 2, null);
        }

        var result = queries.searchActiveByDisplayName("ALPHA", 25);

        assertThat(result.totalMatches()).isEqualTo(28);
        assertThat(result.matches()).hasSize(25);
        assertThat(result.matches()).extracting(match -> match.conceptId()).contains(startsA, startsB).doesNotContain(contains);
        assertThat(result.matches().subList(0, 2)).extracting(match -> match.conceptId()).containsExactly(startsA, startsB);
        assertThat(result.matches().getLast().conceptId()).isNotEqualTo(contains);
        assertThat(result.hasMoreMatches()).isTrue();
    }

    @Test
    void projectsOnlyTheAllowedCurrentDirectFieldsAndNeverWritesAuditData() {
        long activeParentB = insertConcept("PARENT_ACTIVE_B", "Lookup Oberbegriff Beta", true, false, 1, null);
        long activeParentA = insertConcept("PARENT_ACTIVE_A", "Lookup Oberbegriff Alpha", true, false, 1, null);
        long inactiveParent = insertConcept("PARENT_INACTIVE", "Lookup Oberbegriff inaktiv", false, false, 1, null);
        long selected = insertConcept("SELECTED", "Lookup Profil", true, true, 4, "  Kurator @here *Hinweis*  ");
        long activeChildB = insertConcept("CHILD_ACTIVE_B", "Lookup Konkretisierung Beta", true, true, 2, null);
        long activeChildA = insertConcept("CHILD_ACTIVE_A", "Lookup Konkretisierung Alpha", true, true, 2, null);
        long inactiveChild = insertConcept("CHILD_INACTIVE", "Lookup Konkretisierung inaktiv", false, true, 2, null);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", activeParentB, selected);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", activeParentA, selected);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", inactiveParent, selected);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", selected, activeChildB);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", selected, activeChildA);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", selected, inactiveChild);
        assignRole(selected, "VEGETABLE");
        assignFlag(selected, "FERMENTED");
        assignDimension(selected, "UMAMI", 4);
        assignDimension(selected, "SALTINESS", 2);
        long auditBefore = jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry", Long.class);

        var search = queries.searchActiveByDisplayName("profil", 25);
        var profile = queries.findActiveProfile(selected).orElseThrow();
        long auditAfter = jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry", Long.class);

        assertThat(search.matches()).singleElement().extracting(match -> match.activeDirectParents())
                .isEqualTo(List.of("Lookup Oberbegriff Alpha", "Lookup Oberbegriff Beta"));
        assertThat(profile.displayName()).isEqualTo("Lookup Profil");
        assertThat(profile.randomDrawEnabled()).isTrue();
        assertThat(profile.baseDrawWeight()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(profile.noveltyLevel()).isEqualTo(4);
        assertThat(profile.activeDirectParents())
                .extracting(relation -> relation.conceptId() + ":" + relation.displayName())
                .containsExactly(activeParentA + ":Lookup Oberbegriff Alpha", activeParentB + ":Lookup Oberbegriff Beta");
        assertThat(profile.activeDirectChildren())
                .extracting(relation -> relation.conceptId() + ":" + relation.displayName())
                .containsExactly(activeChildA + ":Lookup Konkretisierung Alpha", activeChildB + ":Lookup Konkretisierung Beta");
        assertThat(profile.functionalRoles()).containsExactly("Gemüse");
        assertThat(profile.culinaryFlags()).containsExactly("fermentiert");
        assertThat(profile.culinaryDimensions()).extracting(dimension -> dimension.code(), dimension -> dimension.level())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("SALTINESS", 2),
                        org.assertj.core.groups.Tuple.tuple("UMAMI", 4));
        assertThat(profile.curatorNote()).isEqualTo("  Kurator @here *Hinweis*  ");
        assertThat(queries.findActiveProfile(inactiveChild)).isEmpty();
        assertThat(auditAfter).isEqualTo(auditBefore);
    }

    private long insertConcept(String suffix, String displayName, boolean active, boolean drawable, Integer noveltyLevel,
                               String curatorNote) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight, novelty_level, curator_note
                ) values (?, ?, ?, ?, 'SPECIFIC', 1.0000, ?, ?)
                returning id
                """, Long.class, PREFIX + suffix, displayName, active, drawable, noveltyLevel, curatorNote);
    }

    private void assignRole(long conceptId, String roleCode) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, conceptId, roleCode);
    }

    private void assignFlag(long conceptId, String flagCode) {
        jdbcTemplate.update("""
                insert into ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
                select ?, id from culinary_flag where code = ?
                """, conceptId, flagCode);
    }

    private void assignDimension(long conceptId, String dimensionCode, int level) {
        jdbcTemplate.update("""
                insert into ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
                select ?, id, ? from culinary_dimension where code = ?
                """, conceptId, level, dimensionCode);
    }
}
