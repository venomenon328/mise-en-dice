package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailabilityFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogNoveltyFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogQuickFilter;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSearchCriteria;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogSort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
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

/** Verifies administration query projections against PostgreSQL rather than an in-memory substitute. */
@SpringBootTest
@Testcontainers
class CatalogQueriesIntegrationTest {

    private static final String PREFIX = "TEST_CATALOG_QUERY_";

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
    private CatalogQueries catalogQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestCatalogData() {
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from exclusion_rule where code like ?", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void exposesARealMultiParentGraphInSearchDetailAndDirectHierarchyQueries() {
        long fish = insertConcept("PARENT_FISH", "Query parent fish", "OPEN", true, true, null);
        long seaFish = insertConcept("PARENT_SEA", "Query parent sea fish", "OPEN", true, true, null);
        long cod = insertConcept("COD", "Query graph cod", "SPECIFIC", true, true, 3);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", fish, cod);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", seaFish, cod);
        assignRole(cod, "ANIMAL_PROTEIN");
        assignFlag(cod, "FERMENTED");
        assignAvailability(cod, "GEORGIA", "EASY");
        assignAvailability(cod, "TOBIAS", "PLANNED");
        assignDimension(cod, "UMAMI", 4);
        jdbcTemplate.update("""
                insert into exclusion_rule (code, display_text, base_draw_weight)
                values (?, ?, 1.0000)
                """, PREFIX + "NO_COD", "No query graph cod " + UUID.randomUUID());
        jdbcTemplate.update("""
                insert into exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                select er.id, ?, false from exclusion_rule er where er.code = ?
                """, cod, PREFIX + "NO_COD");

        var search = catalogQueries.search(criteria("query graph cod", null, Set.of(), Set.of(),
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(), 0, 100));
        assertThat(search.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(cod);
            assertThat(item.directParents()).extracting(parent -> parent.id()).containsExactlyInAnyOrder(fish, seaFish);
        });
        assertThat(catalogQueries.findDirectChildren(fish)).extracting(node -> node.id()).contains(cod);
        assertThat(catalogQueries.findDirectChildren(seaFish)).extracting(node -> node.id()).contains(cod);

        var detail = catalogQueries.findConcept(cod).orElseThrow();
        assertThat(detail.directParents()).extracting(parent -> parent.id()).containsExactlyInAnyOrder(fish, seaFish);
        assertThat(detail.functionalRoles()).extracting(role -> role.code()).containsExactly("ANIMAL_PROTEIN");
        assertThat(detail.culinaryFlags()).extracting(flag -> flag.code()).containsExactly("FERMENTED");
        assertThat(detail.culinaryDimensions()).hasSize(7);
        assertThat(detail.culinaryDimensions()).filteredOn(dimension -> dimension.dimension().code().equals("UMAMI"))
                .singleElement().extracting(dimension -> dimension.level()).isEqualTo(4);
        assertThat(detail.availability()).extracting(value -> value.participant().code(), value -> value.level())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("GEORGIA", CatalogAvailability.EASY),
                        org.assertj.core.groups.Tuple.tuple("TOBIAS", CatalogAvailability.PLANNED)
                );
        assertThat(detail.seasonality()).hasSize(12)
                .allSatisfy(value -> assertThat(value.weightMultiplier()).isEqualByComparingTo(BigDecimal.ONE));
        assertThat(detail.directExclusionRules()).singleElement()
                .satisfies(rule -> assertThat(rule).contains("No query graph cod"));
    }

    @Test
    void combinesFiltersAndIdentifiesActualMaintenanceGaps() {
        long curated = insertConcept("FILTERED", "Query filtered concept", "SPECIFIC", true, true, 4);
        assignRole(curated, "ANIMAL_PROTEIN");
        assignFlag(curated, "FERMENTED");
        assignAvailability(curated, "GEORGIA", "EASY");
        assignAvailability(curated, "TOBIAS", "DIFFICULT");
        long incomplete = insertConcept("INCOMPLETE", "Query incomplete open", "OPEN", true, true, null);

        var incompleteDetail = catalogQueries.findConcept(incomplete).orElseThrow();
        assertThat(incompleteDetail.culinaryDimensions()).allSatisfy(dimension -> assertThat(dimension.level()).isNull());
        assertThat(incompleteDetail.availability()).allSatisfy(availability -> assertThat(availability.level()).isNull());
        assertThat(incompleteDetail.seasonality()).allSatisfy(season ->
                assertThat(season.weightMultiplier()).isEqualByComparingTo(BigDecimal.ONE));

        var filters = catalogQueries.search(criteria(
                "query", null, Set.of("ANIMAL_PROTEIN", "FRUIT"), Set.of("FERMENTED"),
                new CatalogAvailabilityFilter(Set.of(CatalogAvailability.EASY), false),
                new CatalogAvailabilityFilter(Set.of(CatalogAvailability.DIFFICULT), false),
                new CatalogNoveltyFilter(Set.of(4), false), 0, 100
        ));
        assertThat(filters.items()).extracting(item -> item.id()).containsExactly(curated);

        var maintenance = catalogQueries.search(criteria(
                "query incomplete", CatalogQuickFilter.NEEDS_ATTENTION, Set.of(), Set.of(),
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(), 0, 100
        ));
        assertThat(maintenance.items()).extracting(item -> item.id()).containsExactly(incomplete);
    }

    @Test
    void treatsLikeWildcardsInSearchTermsAsLiteralCharacters() {
        long literal = insertConcept("LITERAL", "Query literal 100%_sure", "SPECIFIC", false, false, null);
        insertConcept("WILDCARD_DECOY", "Query literal 100XXsure", "SPECIFIC", false, false, null);

        var result = catalogQueries.search(search("%_", CatalogSort.DISPLAY_NAME_ASC));

        assertThat(result.items()).extracting(item -> item.id()).containsExactly(literal);
    }

    @Test
    void sortsAndPaginatesOnTheServer() {
        for (int number = 0; number < 51; number++) {
            insertConcept("PAGE_" + number, "Query page " + String.format("%02d", number), "SPECIFIC", false, false, null);
        }

        var firstPage = catalogQueries.search(criteria("query page", null, Set.of(), Set.of(),
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(), 0, 50));
        var secondPage = catalogQueries.search(criteria("query page", null, Set.of(), Set.of(),
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(), 1, 50));

        assertThat(firstPage.totalItems()).isEqualTo(51);
        assertThat(firstPage.items()).hasSize(50);
        assertThat(firstPage.items()).first().extracting(item -> item.displayName()).isEqualTo("Query page 00");
        assertThat(secondPage.items()).singleElement().extracting(item -> item.displayName()).isEqualTo("Query page 50");

        long light = insertConcept("SORT_LIGHT", "Query sort light", "SPECIFIC", false, false, 1);
        long heavy = insertConcept("SORT_HEAVY", "Query sort heavy", "SPECIFIC", false, false, 5);
        jdbcTemplate.update("update ingredient_concept set base_draw_weight = 0.5000 where id = ?", light);
        jdbcTemplate.update("update ingredient_concept set base_draw_weight = 5.0000 where id = ?", heavy);
        assertThat(catalogQueries.search(search("query sort", CatalogSort.DRAW_WEIGHT_DESC)).items()).first()
                .extracting(item -> item.id()).isEqualTo(heavy);
        assertThat(catalogQueries.search(search("query sort", CatalogSort.NOVELTY_ASC)).items()).first()
                .extracting(item -> item.id()).isEqualTo(light);
    }

    private CatalogSearchCriteria criteria(
            String search,
            CatalogQuickFilter quickFilter,
            Set<String> roles,
            Set<String> flags,
            CatalogAvailabilityFilter georgia,
            CatalogAvailabilityFilter tobias,
            CatalogNoveltyFilter novelty,
            int page,
            int pageSize
    ) {
        return new CatalogSearchCriteria(
                search, quickFilter, null, null, null, roles, flags, georgia, tobias, novelty,
                CatalogSort.DISPLAY_NAME_ASC, page, pageSize
        );
    }

    private CatalogSearchCriteria search(String query, CatalogSort sort) {
        return new CatalogSearchCriteria(
                query, null, null, null, null, Set.of(), Set.of(),
                CatalogAvailabilityFilter.any(), CatalogAvailabilityFilter.any(), CatalogNoveltyFilter.any(), sort, 0, 50
        );
    }

    private long insertConcept(
            String suffix,
            String displayName,
            String specificity,
            boolean active,
            boolean drawable,
            Integer noveltyLevel
    ) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight, novelty_level
                ) values (?, ?, ?, ?, ?, 1.0000, ?)
                returning id
                """, Long.class, PREFIX + suffix, displayName, active, drawable, specificity, noveltyLevel);
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

    private void assignAvailability(long conceptId, String participantCode, String level) {
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select ?, id, ? from participant where code = ?
                """, conceptId, level, participantCode);
    }

    private void assignDimension(long conceptId, String dimensionCode, int level) {
        jdbcTemplate.update("""
                insert into ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
                select ?, id, ? from culinary_dimension where code = ?
                """, conceptId, level, dimensionCode);
    }
}
