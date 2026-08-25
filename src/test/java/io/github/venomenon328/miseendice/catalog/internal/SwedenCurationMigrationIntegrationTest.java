package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Focused PostgreSQL coverage for the approved Sweden curation batch from issue #172. */
@SpringBootTest
@Testcontainers
class SwedenCurationMigrationIntegrationTest {

    private static final String NEW_CODES_SQL = """
            'NEW_POTATOES',
            'PICKLED_HERRING',
            'GRAVLAX',
            'CRISPBREAD',
            'LINGONBERRY',
            'LINGONBERRY_PRESERVES',
            'YELLOW_SPLIT_PEAS'
            """;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_sweden_curation")
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
    void loadsApprovedConceptsAssociationsAndMetadata() {
        assertThat(jdbcTemplate.queryForList("""
                select code || ':' || display_name
                from ingredient_concept
                where code in (%s)
                order by code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "CRISPBREAD:Knäckebrot",
                        "GRAVLAX:Gravlax",
                        "LINGONBERRY:Preiselbeeren",
                        "LINGONBERRY_PRESERVES:Preiselbeer-Konfitüre/-kompott",
                        "NEW_POTATOES:Frühkartoffeln",
                        "PICKLED_HERRING:eingelegter Hering",
                        "YELLOW_SPLIT_PEAS:gelbe Schälerbsen"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country association
                join ingredient_concept concept on concept.id = association.ingredient_concept_id
                where association.country_code = 'SE'
                order by concept.code
                """, String.class))
                .containsExactly(
                        "CARDAMOM",
                        "CINNAMON",
                        "CRAYFISH",
                        "CRISPBREAD",
                        "DILL",
                        "GRAVLAX",
                        "HERRING",
                        "LINGONBERRY",
                        "LINGONBERRY_PRESERVES",
                        "NEW_POTATOES",
                        "PICKLED_HERRING",
                        "SALMON",
                        "YELLOW_SPLIT_PEAS"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country association
                join ingredient_concept concept on concept.id = association.ingredient_concept_id
                where association.country_code = 'SE'
                  and concept.code in ('SAFFRON', 'SMOKED_SALMON')
                """, String.class)).isEmpty();

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                where concept.code in (%s)
                """.formatted(NEW_CODES_SQL), Integer.class)).isEqualTo(14);

        assertThat(jdbcTemplate.queryForList("""
                select participant.code || ':' || availability.availability_level
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                join participant on participant.id = availability.participant_id
                where concept.code = 'LINGONBERRY'
                order by participant.code
                """, String.class)).containsExactly("GEORGIA:PLANNED", "TOBIAS:PLANNED");

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_seasonality seasonality
                join ingredient_concept concept on concept.id = seasonality.ingredient_concept_id
                where concept.code = 'NEW_POTATOES'
                """, Integer.class)).isEqualTo(12);
    }

    @Test
    void loadsApprovedRefinementGraph() {
        assertThat(jdbcTemplate.queryForList("""
                select parent.code || '>' || child.code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where child.code in (%s)
                order by parent.code, child.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "BERRIES>LINGONBERRY",
                        "BREAD>CRISPBREAD",
                        "HERRING>PICKLED_HERRING",
                        "LINGONBERRY>LINGONBERRY_PRESERVES",
                        "POTATO>NEW_POTATOES",
                        "PRESERVED_FISH>GRAVLAX",
                        "PRESERVED_FISH>PICKLED_HERRING",
                        "PRESERVED_PRODUCE>LINGONBERRY_PRESERVES",
                        "SALMON>GRAVLAX",
                        "SPLIT_PEAS>YELLOW_SPLIT_PEAS"
                );
    }
}
