package io.github.venomenon328.miseendice.catalog.internal;

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

/** PostgreSQL canaries for the approved Issue #172 Denmark curation. */
@SpringBootTest
@Testcontainers
class DenmarkCatalogCurationIntegrationTest {

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
    void persistsExactlyTheApprovedDenmarkCountryAssociations() {
        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                where assignment.country_code = 'DK'
                order by concept.code
                """, String.class)).containsExactly(
                "AQUAVIT",
                "BUTTERMILK",
                "DANABLU",
                "DANBO",
                "DILL",
                "EEL",
                "FRIED_ONIONS",
                "HERRING",
                "LIVER_PATE",
                "NEW_POTATOES",
                "PICKLED_CUCUMBER",
                "PICKLED_HERRING",
                "PLAICE",
                "PORK_BELLY",
                "PORK_MINCE",
                "POTATO",
                "RED_CABBAGE",
                "REMOULADE",
                "ROD_POLSE",
                "RYE_BREAD",
                "RYE_FLOUR",
                "SALTY_LIQUORICE",
                "SHRIMP",
                "YELLOW_SPLIT_PEAS"
        );

        assertThat(hasCountry("PORK", "DK")).isFalse();
        assertThat(hasCountry("SAUSAGE", "DK")).isFalse();
        assertThat(hasCountry("CHOCOLATE", "DK")).isFalse();
        assertThat(hasCountry("LIQUORICE", "DK")).isFalse();
        assertThat(hasCountry("CONFECTIONERY", "DK")).isFalse();
    }

    @Test
    void persistsApprovedDenmarkConceptMetadataAndGraphChanges() {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_concept
                where code in (
                    'REMOULADE', 'FRIED_ONIONS', 'LIVER_PATE', 'DANABLU', 'DANBO', 'AQUAVIT',
                    'CONFECTIONERY', 'CHOCOLATE', 'LIQUORICE', 'SALTY_LIQUORICE', 'ROD_POLSE'
                )
                """, Integer.class)).isEqualTo(11);

        assertThat(parentCodes("CHOCOLATE")).containsExactly("COCOA_PRODUCTS", "CONFECTIONERY");
        assertThat(parentCodes("DARK_CHOCOLATE")).containsExactly("CHOCOLATE");
        assertThat(parentCodes("MILK_CHOCOLATE")).containsExactly("CHOCOLATE");
        assertThat(parentCodes("WHITE_CHOCOLATE")).containsExactly("CHOCOLATE");
        assertThat(parentCodes("LIQUORICE")).containsExactly("CONFECTIONERY");
        assertThat(parentCodes("SALTY_LIQUORICE")).containsExactly("LIQUORICE");
        assertThat(parentCodes("ROD_POLSE")).containsExactly("PORK", "SAUSAGE");

        assertThat(drawEnabled("CONFECTIONERY")).isFalse();
        assertThat(drawEnabled("CHOCOLATE")).isTrue();
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                where concept.code = 'CONFECTIONERY'
                """, Integer.class)).isZero();

        assertThat(availability("DANBO", "TOBIAS")).isEqualTo("PLANNED");
        assertThat(availability("DANBO", "GEORGIA")).isEqualTo("DIFFICULT");
        assertThat(availability("ROD_POLSE", "TOBIAS")).isEqualTo("PLANNED");
        assertThat(availability("ROD_POLSE", "GEORGIA")).isEqualTo("DIFFICULT");

        assertThat(dimension("SALTY_LIQUORICE", "SALTINESS")).isEqualTo(5);
        assertThat(dimension("REMOULADE", "FATTINESS")).isEqualTo(5);
        assertThat(flagCodes("DANABLU")).containsExactly("CURED", "FERMENTED");
        assertThat(flagCodes("DANBO")).containsExactly("CURED", "FERMENTED");
        assertThat(flagCodes("ROD_POLSE")).containsExactly("SMOKED");

        assertThat(jdbcTemplate.queryForObject("""
                select target.include_refinements
                from exclusion_rule_target target
                join exclusion_rule rule on rule.id = target.exclusion_rule_id
                join ingredient_concept concept on concept.id = target.ingredient_concept_id
                where rule.code = 'NO_EGGS' and concept.code = 'REMOULADE'
                """, Boolean.class)).isFalse();
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

    private boolean drawEnabled(String conceptCode) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select random_draw_enabled from ingredient_concept where code = ?",
                Boolean.class,
                conceptCode
        ));
    }

    private String availability(String conceptCode, String participantCode) {
        return jdbcTemplate.queryForObject("""
                select availability.availability_level
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                join participant on participant.id = availability.participant_id
                where concept.code = ? and participant.code = ?
                """, String.class, conceptCode, participantCode);
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
}
