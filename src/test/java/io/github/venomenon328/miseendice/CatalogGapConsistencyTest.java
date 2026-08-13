package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
class CatalogGapConsistencyTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_catalog_gap")
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
    void separatesFreshDriedPowderedAndPickledChilliForms() {
        assertThat(displayName("CHILI")).isEqualTo("frische Chili");
        assertThat(specificity("CHILI")).isEqualTo("OPEN");
        assertThat(parentCodes("CHILI")).containsExactly("FRUIT_VEGETABLES");
        assertThat(childCodes("CHILI")).containsExactlyInAnyOrder(
                "BIRDS_EYE_CHILI",
                "HABANERO",
                "JALAPENO",
                "POBLANO",
                "SERRANO_CHILI"
        );

        assertThat(parentCodes("DRIED_CHILI")).containsExactly("SPICES");
        assertThat(childCodes("DRIED_CHILI")).containsExactlyInAnyOrder(
                "ANCHO_CHILI",
                "CHILI_FLAKES",
                "CHILI_POWDER",
                "CHIPOTLE"
        );
        assertThat(specificity("CHILI_FLAKES")).isEqualTo("OPEN");
        assertThat(childCodes("CHILI_FLAKES")).containsExactlyInAnyOrder("GOCHUGARU", "PUL_BIBER");
        assertThat(specificity("CHILI_POWDER")).isEqualTo("OPEN");
        assertThat(childCodes("CHILI_POWDER"))
                .containsExactlyInAnyOrder("CAYENNE_PEPPER", "KASHMIRI_CHILI_POWDER");
        assertThat(parentCodes("PICKLED_CHILI")).containsExactly("PRESERVED_PRODUCE");

        assertThat(roleCodes("CHILI")).contains("VEGETABLE");
        assertThat(roleCodes("JALAPENO")).contains("VEGETABLE");
        assertThat(roleCodes("HABANERO")).contains("VEGETABLE");
        assertThat(roleCodes("BIRDS_EYE_CHILI")).contains("VEGETABLE");
        assertThat(roleCodes("SERRANO_CHILI")).contains("VEGETABLE");
        assertThat(refinementExists("SPICES", "CHILI")).isFalse();
    }

    @Test
    void sharpensPaprikaAndPepperFamilies() {
        assertThat(specificity("PAPRIKA_POWDER")).isEqualTo("OPEN");
        assertThat(childCodes("PAPRIKA_POWDER")).containsExactlyInAnyOrder(
                "HOT_PAPRIKA_POWDER",
                "SMOKED_PAPRIKA",
                "SWEET_PAPRIKA_POWDER"
        );

        assertThat(parentCodes("PEPPER")).containsExactly("SPICES");
        assertThat(childCodes("PEPPER"))
                .containsExactlyInAnyOrder("BLACK_PEPPER", "GREEN_PEPPER", "WHITE_PEPPER");
        assertThat(parentCodes("BLACK_PEPPER")).containsExactly("PEPPER");
        assertThat(parentCodes("WHITE_PEPPER")).containsExactly("PEPPER");
        assertThat(parentCodes("SICHUAN_PEPPER")).containsExactly("SPICES");

        assertThat(childCodes("BELL_PEPPER")).contains(
                "GREEN_BELL_PEPPER",
                "RED_BELL_PEPPER",
                "YELLOW_BELL_PEPPER"
        );
        assertThat(parentCodes("ROASTED_RED_PEPPER"))
                .containsExactlyInAnyOrder("PRESERVED_PRODUCE", "RED_BELL_PEPPER");
    }

    @Test
    void addsUsefulVegetableMushroomAndButterIntermediateConcepts() {
        assertThat(parentCodes("FLOWER_VEGETABLES")).containsExactly("VEGETABLES");
        assertThat(childCodes("FLOWER_VEGETABLES")).containsExactlyInAnyOrder(
                "ARTICHOKE",
                "BROCCOLI",
                "CAULIFLOWER",
                "ROMANESCO"
        );
        assertThat(parentCodes("ARTICHOKE")).containsExactly("FLOWER_VEGETABLES");

        assertThat(parentCodes("CHAMPIGNONS")).containsExactly("MUSHROOMS");
        assertThat(childCodes("CHAMPIGNONS")).containsExactlyInAnyOrder(
                "CANNED_CHAMPIGNONS",
                "CHAMPIGNON"
        );
        assertThat(specificity("CHAMPIGNON")).isEqualTo("OPEN");
        assertThat(childCodes("CHAMPIGNON"))
                .containsExactlyInAnyOrder("BROWN_CHAMPIGNON", "WHITE_CHAMPIGNON");
        assertThat(parentCodes("CANNED_CHAMPIGNONS"))
                .containsExactlyInAnyOrder("CHAMPIGNONS", "PRESERVED_PRODUCE");
        assertThat(refinementExists("MUSHROOMS", "CHAMPIGNON")).isFalse();

        assertThat(childCodes("BUTTER")).containsExactlyInAnyOrder("GARLIC_BUTTER", "HERB_BUTTER");
    }

    @Test
    void classifiesReadyCurryPastesConsistentlyAndKeepsChilliExclusionsComplete() {
        assertThat(parentCodes("THAI_GREEN_CURRY_PASTE")).containsExactly("READY_CURRY_PASTE");
        assertThat(parentCodes("THAI_RED_CURRY_PASTE")).containsExactly("READY_CURRY_PASTE");
        assertThat(parentCodes("THAI_YELLOW_CURRY_PASTE")).containsExactly("READY_CURRY_PASTE");
        assertThat(parentCodes("LAKSA_PASTE")).containsExactly("READY_CURRY_PASTE");
        assertThat(parentCodes("MOLE_PASTE")).containsExactly("READY_SAUCES_AND_PASTES");

        assertThat(exclusionTargetExists("NO_CHILI", "DRIED_CHILI", true)).isTrue();
        assertThat(exclusionTargetExists("NO_CHILI", "PICKLED_CHILI", true)).isTrue();
        assertThat(exclusionTargetExists("NO_CHILI", "READY_CURRY_PASTE", true)).isTrue();
        assertThat(exclusionTargetExists("NO_CHILI", "MOLE_PASTE", false)).isTrue();
    }

    @Test
    void addsMissingEverydaySeasoningsWithoutAbusingTheRefinementGraph() {
        assertThat(parentCodes("MAGGI_SEASONING")).containsExactly("READY_SAUCES_AND_PASTES");
        assertThat(parentCodes("GARLIC_POWDER")).containsExactly("SPICES");
        assertThat(parentCodes("ONION_POWDER")).containsExactly("SPICES");
        assertThat(exclusionTargetExists("NO_ALLIUMS", "GARLIC_BUTTER", false)).isTrue();
        assertThat(exclusionTargetExists("NO_ALLIUMS", "GARLIC_POWDER", false)).isTrue();
        assertThat(exclusionTargetExists("NO_ALLIUMS", "ONION_POWDER", false)).isTrue();

        assertThat(displayName("COCONUT_PRODUCTS")).isEqualTo("Kokoszutat");
        assertThat(parentCodes("COCONUT_PRODUCTS")).isEmpty();
        assertThat(curatorNote("COCONUT_PRODUCTS"))
                .contains("keinen gemeinsamen Parent");
    }

    @Test
    void correctsAdditionalClearProcessingAndOriginMisclassifications() {
        assertThat(parentCodes("SURIMI")).containsExactly("FISH");
        assertThat(refinementExists("PRESERVED_FISH", "SURIMI")).isFalse();

        assertThat(parentCodes("POLENTA")).containsExactly("CORN");
        assertThat(refinementExists("GRAINS", "POLENTA")).isFalse();
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

    private List<String> roleCodes(String conceptCode) {
        return jdbcTemplate.queryForList(
                """
                select role.code
                from ingredient_functional_role assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join functional_role role on role.id = assignment.functional_role_id
                where concept.code = ?
                order by role.code
                """,
                String.class,
                conceptCode
        );
    }

    private String displayName(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select display_name from ingredient_concept where code = ?",
                String.class,
                conceptCode
        );
    }

    private String specificity(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select challenge_specificity from ingredient_concept where code = ?",
                String.class,
                conceptCode
        );
    }

    private String curatorNote(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select curator_note from ingredient_concept where code = ?",
                String.class,
                conceptCode
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

    private boolean exclusionTargetExists(
            String ruleCode,
            String conceptCode,
            boolean includeRefinements
    ) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                select exists (
                    select 1
                    from exclusion_rule_target target
                    join exclusion_rule rule on rule.id = target.exclusion_rule_id
                    join ingredient_concept concept on concept.id = target.ingredient_concept_id
                    where rule.code = ?
                      and concept.code = ?
                      and target.include_refinements = ?
                )
                """,
                Boolean.class,
                ruleCode,
                conceptCode,
                includeRefinements
        ));
    }
}
