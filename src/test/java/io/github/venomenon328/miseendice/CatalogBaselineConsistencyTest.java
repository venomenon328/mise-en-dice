package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CatalogBaselineConsistencyTest {

    private static final Set<String> EXPECTED_ACTIVE_ROOTS = Set.of(
            "COCOA_PRODUCTS",
            "COCONUT_PRODUCTS",
            "COFFEE",
            "COOKING_ALCOHOL",
            "COOKING_FATS",
            "DAIRY_PRODUCTS",
            "EGGS",
            "FRESH_HERBS",
            "FRUIT",
            "LEGUMES",
            "MEAT",
            "NUTS",
            "PLANT_DRINKS",
            "PLANT_PROTEIN_PRODUCTS",
            "PRESERVED_PRODUCE",
            "SAUCES_AND_PASTES",
            "SEAFOOD",
            "SEEDS",
            "SPICES",
            "STARCHES",
            "STOCKS",
            "SWEETENERS",
            "TEA",
            "TOMATO_PRODUCTS",
            "VEGETABLES",
            "VINEGAR"
    );

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_catalog_consistency")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void consolidatedCatalogRetainsDeliberateRootsAndStructuralInvariants() {
        // The exact 2026-08-13 baseline is covered by FinalCatalogSnapshotIntegrationTest.
        // This full-master test intentionally permits later append-only catalog curation.
        assertThat(count("ingredient_concept")).isPositive();
        assertThat(countWhere("ingredient_concept", "active and random_draw_enabled")).isPositive();
        assertThat(countWhere(
                "ingredient_concept",
                "active and random_draw_enabled and challenge_specificity = 'OPEN'"
        )).isPositive();
        assertThat(countWhere(
                "ingredient_concept",
                "active and random_draw_enabled and challenge_specificity = 'SPECIFIC'"
        )).isPositive();
        assertThat(count("ingredient_refinement")).isPositive();

        Set<String> activeRoots = Set.copyOf(jdbcTemplate.queryForList(
                """
                select concept.code
                from ingredient_concept concept
                where concept.active
                  and not exists (
                      select 1
                      from ingredient_refinement relation
                      where relation.child_concept_id = concept.id
                  )
                """,
                String.class
        ));
        assertThat(activeRoots).containsAll(EXPECTED_ACTIVE_ROOTS);

        assertThat(jdbcTemplate.queryForList(
                """
                select concept.code
                from ingredient_concept concept
                where concept.active
                  and concept.challenge_specificity = 'SPECIFIC'
                  and not exists (
                      select 1
                      from ingredient_refinement relation
                      where relation.child_concept_id = concept.id
                  )
                """,
                String.class
        )).contains("COFFEE");
    }

    @Test
    void refinementGraphIsTransitivelyReducedAndRoleCompatible() {
        assertThat(jdbcTemplate.queryForObject(
                """
                with recursive paths(root_id, current_id, depth) as (
                    select relation.parent_concept_id,
                           relation.child_concept_id,
                           1
                    from ingredient_refinement relation

                    union all

                    select paths.root_id,
                           relation.child_concept_id,
                           paths.depth + 1
                    from paths
                    join ingredient_refinement relation
                      on relation.parent_concept_id = paths.current_id
                    where paths.depth < 32
                )
                select count(*)
                from ingredient_refinement direct
                where exists (
                    select 1
                    from paths
                    where paths.root_id = direct.parent_concept_id
                      and paths.current_id = direct.child_concept_id
                      and paths.depth >= 2
                )
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_refinement relation
                where not exists (
                    select 1
                    from ingredient_functional_role parent_role
                    join ingredient_functional_role child_role
                      on child_role.functional_role_id = parent_role.functional_role_id
                    where parent_role.ingredient_concept_id = relation.parent_concept_id
                      and child_role.ingredient_concept_id = relation.child_concept_id
                )
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where parent.challenge_specificity = 'SPECIFIC'
                  and child.challenge_specificity = 'OPEN'
                """,
                Integer.class
        )).isZero();
    }

    @Test
    void seafoodHierarchyUsesOneCanonicalCrustaceanPath() {
        assertThat(parentCodes("SEAFOOD")).isEmpty();
        assertThat(parentCodes("SHELLFISH")).containsExactly("SEAFOOD");
        assertThat(parentCodes("CRUSTACEANS")).containsExactly("SHELLFISH");
        assertThat(childCodes("SHELLFISH")).containsExactlyInAnyOrder("BIVALVES", "CRUSTACEANS");

        assertThat(refinementExists("SEAFOOD", "SHELLFISH")).isTrue();
        assertThat(refinementExists("SHELLFISH", "CRUSTACEANS")).isTrue();
        assertThat(refinementExists("SEAFOOD", "CRUSTACEANS")).isFalse();
        assertThat(refinementExists("SHELLFISH", "SHRIMP")).isFalse();
        assertThat(refinementExists("CRUSTACEANS", "SHRIMP")).isTrue();
    }

    @Test
    void everyDrawableConceptHasRequiredGeneratorMetadata() {
        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_concept concept
                where concept.active
                  and concept.random_draw_enabled
                  and not exists (
                      select 1
                      from ingredient_functional_role assignment
                      where assignment.ingredient_concept_id = concept.id
                  )
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_concept concept
                cross join (
                    select id
                    from participant
                    where code in ('TOBIAS', 'GEORGIA')
                ) expected_participant
                where concept.active
                  and concept.random_draw_enabled
                  and not exists (
                      select 1
                      from ingredient_availability availability
                      where availability.ingredient_concept_id = concept.id
                        and availability.participant_id = expected_participant.id
                  )
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_concept concept
                where concept.active
                  and concept.random_draw_enabled
                  and concept.challenge_specificity = 'OPEN'
                  and not exists (
                      select 1
                      from ingredient_refinement relation
                      where relation.parent_concept_id = concept.id
                  )
                """,
                Integer.class
        )).isZero();
    }

    @Test
    void drawWeightsRespectConsolidatedPlausibilityCaps() {
        assertThat(countWhere(
                "ingredient_concept",
                "active and random_draw_enabled and novelty_level >= 5 and base_draw_weight > 0.2500"
        )).isZero();
        assertThat(countWhere(
                "ingredient_concept",
                "active and random_draw_enabled and novelty_level >= 4 and base_draw_weight > 0.3500"
        )).isZero();
        assertThat(countWhere(
                "ingredient_concept",
                "active and random_draw_enabled and novelty_level >= 3 and base_draw_weight > 0.5500"
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from ingredient_concept concept
                where concept.active
                  and concept.random_draw_enabled
                  and concept.base_draw_weight > 0.3500
                  and exists (
                      select 1
                      from ingredient_availability availability
                      where availability.ingredient_concept_id = concept.id
                        and availability.availability_level = 'DIFFICULT'
                  )
                """,
                Integer.class
        )).isZero();

        assertThat(jdbcTemplate.queryForObject(
                """
                with recursive descendants(concept_id) as (
                    select relation.child_concept_id
                    from ingredient_concept parent
                    join ingredient_refinement relation
                      on relation.parent_concept_id = parent.id
                    where parent.code = 'COOKING_ALCOHOL'

                    union

                    select relation.child_concept_id
                    from descendants
                    join ingredient_refinement relation
                      on relation.parent_concept_id = descendants.concept_id
                )
                select count(*)
                from descendants
                join ingredient_concept concept on concept.id = descendants.concept_id
                where concept.base_draw_weight > 0.3500
                """,
                Integer.class
        )).isZero();

        assertWeight("BEER", "0.2500");
        assertWeight("CIDER", "0.2000");
        assertWeight("RED_WINE", "0.3500");
        assertWeight("WHITE_WINE", "0.3500");
        assertWeight("SQUID", "0.5000");
    }

    private List<String> parentCodes(String childCode) {
        return jdbcTemplate.queryForList(
                """
                select parent.code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where child.code = ?
                order by parent.code
                """,
                String.class,
                childCode
        );
    }

    private List<String> childCodes(String parentCode) {
        return jdbcTemplate.queryForList(
                """
                select child.code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where parent.code = ?
                order by child.code
                """,
                String.class,
                parentCode
        );
    }

    private boolean refinementExists(String parentCode, String childCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    join ingredient_concept child on child.id = relation.child_concept_id
                    where parent.code = ?
                      and child.code = ?
                )
                """,
                Boolean.class,
                parentCode,
                childCode
        ));
    }

    private void assertWeight(String conceptCode, String expectedWeight) {
        assertThat(jdbcTemplate.queryForObject(
                "select base_draw_weight from ingredient_concept where code = ?",
                BigDecimal.class,
                conceptCode
        )).isEqualByComparingTo(new BigDecimal(expectedWeight));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private int countWhere(String table, String whereClause) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + whereClause,
                Integer.class
        );
    }
}
