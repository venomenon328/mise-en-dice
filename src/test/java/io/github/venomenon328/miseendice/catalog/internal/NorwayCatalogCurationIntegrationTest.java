package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL canaries for the approved Issue #172 Norway curation. */
@SpringBootTest
@Testcontainers
class NorwayCatalogCurationIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Test
    void persistsExactlyTheApprovedNorwayCountryAssociationsAndSwedenBackfills() {
        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                where assignment.country_code = 'NO'
                order by concept.code
                """, String.class)).containsExactly(
                "AQUAVIT",
                "BARLEY",
                "BRUNOST",
                "BUTTERMILK",
                "CARAWAY",
                "CARDAMOM",
                "CIDER",
                "CINNAMON",
                "CLOUDBERRY",
                "CLOUDBERRY_PRESERVES",
                "COD",
                "DILL",
                "FENALAR",
                "FLATBROD",
                "GRAVLAX",
                "HADDOCK",
                "HALIBUT",
                "HERRING",
                "KLIPPFISH",
                "LAMB",
                "LEFSE",
                "LINGONBERRY",
                "LINGONBERRY_PRESERVES",
                "LUTEFISK",
                "MACKEREL",
                "MOOSE",
                "NORTHERN_PRAWN",
                "NORWEGIAN_WAFFLE",
                "OATS",
                "PICKLED_HERRING",
                "PINNEKJOTT",
                "POLLOCK",
                "PORK_BELLY",
                "POTATO",
                "RAKFISK",
                "REINDEER",
                "RUTABAGA",
                "SALMON",
                "SMOKED_SALMON",
                "SOUR_CREAM",
                "STOCKFISH",
                "TROUT",
                "VENISON",
                "WHITE_CABBAGE",
                "YELLOW_SPLIT_PEAS"
        );

        assertThat(hasCountry("CLOUDBERRY", "SE")).isTrue();
        assertThat(hasCountry("CLOUDBERRY_PRESERVES", "SE")).isTrue();
        assertThat(hasCountry("REINDEER", "SE")).isTrue();
        assertThat(hasCountry("MOOSE", "SE")).isFalse();
    }

    @Test
    void persistsApprovedNorwayConceptMetadataAndGraphChanges() {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_concept
                where code in (
                    'BRUNOST', 'KLIPPFISH', 'CLOUDBERRY', 'CLOUDBERRY_PRESERVES', 'REINDEER',
                    'NORTHERN_PRAWN', 'FLATBROD', 'LEFSE', 'NORWEGIAN_WAFFLE', 'FENALAR',
                    'PINNEKJOTT', 'RAKFISK', 'LUTEFISK', 'MOOSE'
                )
                """, Integer.class)).isEqualTo(14);

        assertConceptMetadata("BRUNOST", "Brunost", "0.3500", 4);
        assertConceptMetadata("KLIPPFISH", "Klippfisch", "0.2500", 4);
        assertConceptMetadata("CLOUDBERRY", "Moltebeere", "0.2500", 4);
        assertConceptMetadata("CLOUDBERRY_PRESERVES", "Moltebeerkonfitüre/-kompott", "0.3500", 3);
        assertConceptMetadata("REINDEER", "Rentierfleisch", "0.2500", 4);
        assertConceptMetadata("NORTHERN_PRAWN", "Eismeergarnele", "0.4500", 3);
        assertConceptMetadata("FLATBROD", "Flatbrød", "0.4000", 3);
        assertConceptMetadata("LEFSE", "Lefse", "0.3500", 3);
        assertConceptMetadata("NORWEGIAN_WAFFLE", "Norwegische Waffel", "0.4500", 2);
        assertConceptMetadata("FENALAR", "Fenalår", "0.2000", 4);
        assertConceptMetadata("PINNEKJOTT", "Pinnekjøtt", "0.1500", 5);
        assertConceptMetadata("RAKFISK", "Rakfisk", "0.1000", 5);
        assertConceptMetadata("LUTEFISK", "Lutefisk", "0.1000", 5);
        assertConceptMetadata("MOOSE", "Elchfleisch", "0.2000", 4);

        assertThat(parentCodes("BRUNOST")).containsExactly("CHEESE");
        assertThat(parentCodes("KLIPPFISH")).containsExactly("PRESERVED_FISH");
        assertThat(parentCodes("CLOUDBERRY")).containsExactly("BERRIES");
        assertThat(parentCodes("CLOUDBERRY_PRESERVES")).containsExactly("CLOUDBERRY", "PRESERVED_PRODUCE");
        assertThat(parentCodes("REINDEER")).containsExactly("GAME_MEAT");
        assertThat(parentCodes("NORTHERN_PRAWN")).containsExactly("SHRIMP");
        assertThat(parentCodes("FLATBROD")).containsExactly("FLATBREAD");
        assertThat(parentCodes("LEFSE")).containsExactly("FLATBREAD");
        assertThat(parentCodes("NORWEGIAN_WAFFLE")).containsExactly("WAFFLES");
        assertThat(parentCodes("FENALAR")).containsExactly("CURED_MEAT");
        assertThat(parentCodes("PINNEKJOTT")).containsExactly("CURED_MEAT");
        assertThat(parentCodes("RAKFISK")).containsExactly("PRESERVED_FISH");
        assertThat(parentCodes("LUTEFISK")).containsExactly("PRESERVED_FISH");
        assertThat(parentCodes("MOOSE")).containsExactly("GAME_MEAT");
        assertThat(parentCodes("PITA")).containsExactly("FLATBREAD");
        assertThat(hasDirectRelation("BREAD", "PITA")).isFalse();
        assertThat(challengeSpecificity("FLATBREAD")).isEqualTo("OPEN");

        assertThat(roleCodes("BRUNOST")).containsExactly("ANIMAL_PROTEIN", "FAT", "SEASONING");
        assertThat(roleCodes("KLIPPFISH")).containsExactly("ANIMAL_PROTEIN", "SEASONING");
        assertThat(roleCodes("CLOUDBERRY")).containsExactly("ACID", "FRUIT");
        assertThat(roleCodes("CLOUDBERRY_PRESERVES")).containsExactly("ACID", "FRUIT", "SEASONING");
        assertThat(roleCodes("REINDEER")).containsExactly("ANIMAL_PROTEIN");
        assertThat(roleCodes("NORTHERN_PRAWN")).containsExactly("ANIMAL_PROTEIN");
        assertThat(roleCodes("FLATBROD")).containsExactly("STARCH");
        assertThat(roleCodes("LEFSE")).containsExactly("STARCH");
        assertThat(roleCodes("NORWEGIAN_WAFFLE")).containsExactly("FAT", "STARCH");
        assertThat(roleCodes("FENALAR")).containsExactly("ANIMAL_PROTEIN", "SEASONING");
        assertThat(roleCodes("PINNEKJOTT")).containsExactly("ANIMAL_PROTEIN", "FAT", "SEASONING");
        assertThat(roleCodes("RAKFISK")).containsExactly("ANIMAL_PROTEIN", "FAT", "SEASONING");
        assertThat(roleCodes("LUTEFISK")).containsExactly("ANIMAL_PROTEIN");
        assertThat(roleCodes("MOOSE")).containsExactly("ANIMAL_PROTEIN");

        assertThat(dimension("BRUNOST", "SWEETNESS")).isEqualTo(4);
        assertThat(dimension("KLIPPFISH", "SALTINESS")).isEqualTo(5);
        assertThat(dimension("CLOUDBERRY", "ACIDITY")).isEqualTo(4);
        assertThat(dimension("CLOUDBERRY_PRESERVES", "SWEETNESS")).isEqualTo(5);
        assertThat(dimension("REINDEER", "FATTINESS")).isEqualTo(2);
        assertThat(dimension("NORTHERN_PRAWN", "SALTINESS")).isEqualTo(3);
        assertThat(dimension("FLATBROD", "SALTINESS")).isEqualTo(1);
        assertThat(dimension("LEFSE", "SWEETNESS")).isEqualTo(1);
        assertThat(dimension("NORWEGIAN_WAFFLE", "SWEETNESS")).isEqualTo(3);
        assertThat(dimension("FENALAR", "SALTINESS")).isEqualTo(5);
        assertThat(dimension("PINNEKJOTT", "FATTINESS")).isEqualTo(4);
        assertThat(dimension("RAKFISK", "DOMINANCE")).isEqualTo(5);
        assertThat(dimension("LUTEFISK", "SALTINESS")).isEqualTo(2);
        assertThat(dimension("MOOSE", "UMAMI")).isEqualTo(4);

        assertThat(flagCodes("KLIPPFISH")).containsExactly("CURED", "DRIED");
        assertThat(flagCodes("FLATBROD")).containsExactly("DRIED");
        assertThat(flagCodes("FENALAR")).containsExactly("CURED", "DRIED");
        assertThat(flagCodes("PINNEKJOTT")).containsExactly("CURED", "DRIED");
        assertThat(flagCodes("RAKFISK")).containsExactly("CURED", "FERMENTED");
        assertThat(flagCodes("LUTEFISK")).isEmpty();

        assertThat(availabilityRows()).containsExactly(
                "BRUNOST:GEORGIA:DIFFICULT",
                "BRUNOST:TOBIAS:DIFFICULT",
                "CLOUDBERRY:GEORGIA:DIFFICULT",
                "CLOUDBERRY:TOBIAS:DIFFICULT",
                "CLOUDBERRY_PRESERVES:GEORGIA:PLANNED",
                "CLOUDBERRY_PRESERVES:TOBIAS:PLANNED",
                "FENALAR:GEORGIA:DIFFICULT",
                "FENALAR:TOBIAS:DIFFICULT",
                "FLATBROD:GEORGIA:PLANNED",
                "FLATBROD:TOBIAS:PLANNED",
                "KLIPPFISH:GEORGIA:DIFFICULT",
                "KLIPPFISH:TOBIAS:DIFFICULT",
                "LEFSE:GEORGIA:DIFFICULT",
                "LEFSE:TOBIAS:DIFFICULT",
                "LUTEFISK:GEORGIA:DIFFICULT",
                "LUTEFISK:TOBIAS:DIFFICULT",
                "MOOSE:GEORGIA:DIFFICULT",
                "MOOSE:TOBIAS:DIFFICULT",
                "NORTHERN_PRAWN:GEORGIA:PLANNED",
                "NORTHERN_PRAWN:TOBIAS:PLANNED",
                "NORWEGIAN_WAFFLE:GEORGIA:PLANNED",
                "NORWEGIAN_WAFFLE:TOBIAS:PLANNED",
                "PINNEKJOTT:GEORGIA:DIFFICULT",
                "PINNEKJOTT:TOBIAS:DIFFICULT",
                "RAKFISK:GEORGIA:DIFFICULT",
                "RAKFISK:TOBIAS:DIFFICULT",
                "REINDEER:GEORGIA:DIFFICULT",
                "REINDEER:TOBIAS:DIFFICULT"
        );

        assertThat(displayName("POLLOCK")).isEqualTo("Köhler (Seelachs)");
        assertThat(curatorNote("POLLOCK")).contains("Pollachius virens", "Alaska-Seelachs");
        assertThat(curatorNote("STOCKFISH")).contains("Ungesalzener", "Klippfisch");

        assertThat(newConceptSeasonalityCount()).isZero();
        assertThat(newConceptExplicitExclusionTargetCount()).isZero();
    }

    private void assertConceptMetadata(String code, String displayName, String weight, int novelty) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                select display_name, active, random_draw_enabled, challenge_specificity,
                       base_draw_weight, novelty_level
                from ingredient_concept
                where code = ?
                """, code);

        assertThat(row.get("display_name")).isEqualTo(displayName);
        assertThat(row.get("active")).isEqualTo(true);
        assertThat(row.get("random_draw_enabled")).isEqualTo(true);
        assertThat(row.get("challenge_specificity")).isEqualTo("SPECIFIC");
        assertThat((BigDecimal) row.get("base_draw_weight")).isEqualByComparingTo(weight);
        assertThat(((Number) row.get("novelty_level")).intValue()).isEqualTo(novelty);
    }

    private boolean hasCountry(String conceptCode, String countryCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from ingredient_culinary_country assignment
                    join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                    where concept.code = ? and assignment.country_code = ?
                )
                """, Boolean.class, conceptCode, countryCode));
    }

    private List<String> parentCodes(String childCode) {
        return jdbcTemplate.queryForList("""
                select parent.code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where child.code = ?
                order by parent.code
                """, String.class, childCode);
    }

    private boolean hasDirectRelation(String parentCode, String childCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from ingredient_refinement relation
                    join ingredient_concept parent on parent.id = relation.parent_concept_id
                    join ingredient_concept child on child.id = relation.child_concept_id
                    where parent.code = ? and child.code = ?
                )
                """, Boolean.class, parentCode, childCode));
    }

    private String challengeSpecificity(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select challenge_specificity from ingredient_concept where code = ?",
                String.class,
                conceptCode
        );
    }

    private List<String> roleCodes(String conceptCode) {
        return jdbcTemplate.queryForList("""
                select role.code
                from ingredient_functional_role assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join functional_role role on role.id = assignment.functional_role_id
                where concept.code = ?
                order by role.code
                """, String.class, conceptCode);
    }

    private int dimension(String conceptCode, String dimensionCode) {
        return jdbcTemplate.queryForObject("""
                select assignment.level
                from ingredient_culinary_dimension assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                where concept.code = ? and dimension.code = ?
                """, Integer.class, conceptCode, dimensionCode);
    }

    private List<String> flagCodes(String conceptCode) {
        return jdbcTemplate.queryForList("""
                select flag.code
                from ingredient_culinary_flag assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_flag flag on flag.id = assignment.culinary_flag_id
                where concept.code = ?
                order by flag.code
                """, String.class, conceptCode);
    }

    private List<String> availabilityRows() {
        return jdbcTemplate.queryForList("""
                select concept.code || ':' || participant.code || ':' || availability.availability_level
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                join participant on participant.id = availability.participant_id
                where concept.code in (
                    'BRUNOST', 'KLIPPFISH', 'CLOUDBERRY', 'CLOUDBERRY_PRESERVES', 'REINDEER',
                    'NORTHERN_PRAWN', 'FLATBROD', 'LEFSE', 'NORWEGIAN_WAFFLE', 'FENALAR',
                    'PINNEKJOTT', 'RAKFISK', 'LUTEFISK', 'MOOSE'
                )
                order by concept.code, participant.code
                """, String.class);
    }

    private String displayName(String conceptCode) {
        return jdbcTemplate.queryForObject(
                "select display_name from ingredient_concept where code = ?",
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

    private int newConceptSeasonalityCount() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_seasonality seasonality
                join ingredient_concept concept on concept.id = seasonality.ingredient_concept_id
                where concept.code in (
                    'BRUNOST', 'KLIPPFISH', 'CLOUDBERRY', 'CLOUDBERRY_PRESERVES', 'REINDEER',
                    'NORTHERN_PRAWN', 'FLATBROD', 'LEFSE', 'NORWEGIAN_WAFFLE', 'FENALAR',
                    'PINNEKJOTT', 'RAKFISK', 'LUTEFISK', 'MOOSE'
                )
                """, Integer.class);
    }

    private int newConceptExplicitExclusionTargetCount() {
        return jdbcTemplate.queryForObject("""
                select count(*)
                from exclusion_rule_target target
                join ingredient_concept concept on concept.id = target.ingredient_concept_id
                where concept.code in (
                    'BRUNOST', 'KLIPPFISH', 'CLOUDBERRY', 'CLOUDBERRY_PRESERVES', 'REINDEER',
                    'NORTHERN_PRAWN', 'FLATBROD', 'LEFSE', 'NORWEGIAN_WAFFLE', 'FENALAR',
                    'PINNEKJOTT', 'RAKFISK', 'LUTEFISK', 'MOOSE'
                )
                """, Integer.class);
    }
}
