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

/** Focused PostgreSQL coverage for the approved Poland curation batch from issue #172. */
@SpringBootTest
@Testcontainers
class PolandCurationMigrationIntegrationTest {

    private static final String NEW_CODES_SQL = """
            'FERMENTED_CUCUMBER',
            'TWAROG',
            'SOUR_RYE_STARTER',
            'CARP',
            'DRIED_WILD_MUSHROOMS',
            'PICKLED_MUSHROOMS',
            'RYE_BREAD'
            """;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_poland_curation")
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
                select code || ':' || display_name || ':' || base_draw_weight || ':' || novelty_level || ':'
                       || coalesce(curator_note, '')
                from ingredient_concept
                where code in (%s)
                order by code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "CARP:Karpfen:0.3500:3:",
                        "DRIED_WILD_MUSHROOMS:getrocknete Waldpilze:0.4500:2:Getrocknete aromatische Waldpilze, einzeln oder gemischt; nicht auf eine einzelne Pilzart festgelegt.",
                        "FERMENTED_CUCUMBER:Salzgurke:0.6000:2:Milchsauer in Salzlake fermentierte Gurke; bewusst von essigbasierter Gewürzgurke getrennt.",
                        "PICKLED_MUSHROOMS:eingelegte Pilze:0.4000:3:In Essiglake eingelegte Speisepilze; Fermentation wird durch dieses Konzept nicht vorausgesetzt.",
                        "RYE_BREAD:Roggenbrot:0.7000:1:",
                        "SOUR_RYE_STARTER:Żur-Saueransatz:0.3000:4:Fermentierter Roggenmehl-Saueransatz (zakwas/żur) als flüssige Basis für żur/żurek; nicht die fertige Suppe. Gewürzzusätze können variieren.",
                        "TWAROG:Twaróg:0.5500:2:Polnischer frischer Sauermilch-/Bruchkäse; Quark ist eine pragmatische Katalogannäherung. Nicht mit körnigem Hüttenkäse gleichsetzen."
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country association
                join ingredient_concept concept on concept.id = association.ingredient_concept_id
                where association.country_code = 'PL'
                order by concept.code
                """, String.class))
                .containsExactly(
                        "BARLEY", "BEETROOT", "BLOOD_SAUSAGE", "BUCKWHEAT", "CARP", "DILL",
                        "DRIED_WILD_MUSHROOMS", "FERMENTED_CUCUMBER", "HERRING", "HORSERADISH", "LARD",
                        "MARJORAM", "PICKLED_MUSHROOMS", "POPPY_SEEDS", "PORCINI", "PORK_CUTLET", "POTATO",
                        "RYE_BREAD", "RYE_FLOUR", "SAUERKRAUT", "SAUSAGE", "SOUR_CREAM", "SOUR_RYE_STARTER",
                        "TWAROG", "WHITE_CABBAGE", "YELLOW_SPLIT_PEAS"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code
                from ingredient_culinary_country association
                join ingredient_concept concept on concept.id = association.ingredient_concept_id
                where association.country_code = 'PL'
                  and concept.code in ('PORK', 'CUCUMBER', 'QUARK', 'RYE_FLOUR', 'MUSHROOMS', 'SOURDOUGH_BREAD')
                """, String.class)).containsExactly("RYE_FLOUR");

        assertThat(jdbcTemplate.queryForList("""
                select concept.code || ':' || role.code
                from ingredient_functional_role assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join functional_role role on role.id = assignment.functional_role_id
                where concept.code in (%s)
                order by concept.code, role.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "CARP:ANIMAL_PROTEIN", "DRIED_WILD_MUSHROOMS:SEASONING", "DRIED_WILD_MUSHROOMS:VEGETABLE",
                        "FERMENTED_CUCUMBER:ACID", "FERMENTED_CUCUMBER:SEASONING", "FERMENTED_CUCUMBER:VEGETABLE",
                        "PICKLED_MUSHROOMS:ACID", "PICKLED_MUSHROOMS:SEASONING", "PICKLED_MUSHROOMS:VEGETABLE",
                        "RYE_BREAD:STARCH", "SOUR_RYE_STARTER:ACID", "SOUR_RYE_STARTER:SEASONING",
                        "TWAROG:ACID", "TWAROG:ANIMAL_PROTEIN", "TWAROG:FAT"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code || ':' || participant.code || ':' || availability.availability_level
                from ingredient_availability availability
                join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                join participant on participant.id = availability.participant_id
                where concept.code in (%s)
                order by concept.code, participant.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "CARP:GEORGIA:PLANNED", "CARP:TOBIAS:DIFFICULT",
                        "DRIED_WILD_MUSHROOMS:GEORGIA:EASY", "DRIED_WILD_MUSHROOMS:TOBIAS:EASY",
                        "FERMENTED_CUCUMBER:GEORGIA:PLANNED", "FERMENTED_CUCUMBER:TOBIAS:EASY",
                        "PICKLED_MUSHROOMS:GEORGIA:PLANNED", "PICKLED_MUSHROOMS:TOBIAS:PLANNED",
                        "RYE_BREAD:GEORGIA:EASY", "RYE_BREAD:TOBIAS:EASY",
                        "SOUR_RYE_STARTER:GEORGIA:PLANNED", "SOUR_RYE_STARTER:TOBIAS:DIFFICULT",
                        "TWAROG:GEORGIA:PLANNED", "TWAROG:TOBIAS:PLANNED"
                );

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_seasonality seasonality
                join ingredient_concept concept on concept.id = seasonality.ingredient_concept_id
                where concept.code in (%s)
                """.formatted(NEW_CODES_SQL), Integer.class)).isZero();
    }

    @Test
    void loadsApprovedRefinementGraphDimensionsAndFlags() {
        assertThat(jdbcTemplate.queryForList("""
                select parent.code || '>' || child.code
                from ingredient_refinement relation
                join ingredient_concept parent on parent.id = relation.parent_concept_id
                join ingredient_concept child on child.id = relation.child_concept_id
                where child.code in (%s)
                order by parent.code, child.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "BREAD>RYE_BREAD", "CUCUMBER>FERMENTED_CUCUMBER",
                        "FERMENTED_SEASONINGS>SOUR_RYE_STARTER", "FISH>CARP",
                        "MUSHROOMS>DRIED_WILD_MUSHROOMS", "MUSHROOMS>PICKLED_MUSHROOMS",
                        "PRESERVED_PRODUCE>FERMENTED_CUCUMBER", "QUARK>TWAROG"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code || ':' || dimension.code || ':' || assignment.level
                from ingredient_culinary_dimension assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                where concept.code in (%s)
                order by concept.code, dimension.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "CARP:DOMINANCE:3", "CARP:FATTINESS:3", "CARP:UMAMI:3",
                        "DRIED_WILD_MUSHROOMS:BITTERNESS:2", "DRIED_WILD_MUSHROOMS:DOMINANCE:5", "DRIED_WILD_MUSHROOMS:UMAMI:5",
                        "FERMENTED_CUCUMBER:ACIDITY:4", "FERMENTED_CUCUMBER:DOMINANCE:3", "FERMENTED_CUCUMBER:SALTINESS:4", "FERMENTED_CUCUMBER:SWEETNESS:1", "FERMENTED_CUCUMBER:UMAMI:2",
                        "PICKLED_MUSHROOMS:ACIDITY:4", "PICKLED_MUSHROOMS:DOMINANCE:4", "PICKLED_MUSHROOMS:SALTINESS:3", "PICKLED_MUSHROOMS:SWEETNESS:2", "PICKLED_MUSHROOMS:UMAMI:3",
                        "RYE_BREAD:ACIDITY:2", "RYE_BREAD:BITTERNESS:2", "RYE_BREAD:DOMINANCE:3", "RYE_BREAD:SALTINESS:2", "RYE_BREAD:SWEETNESS:1",
                        "SOUR_RYE_STARTER:ACIDITY:5", "SOUR_RYE_STARTER:DOMINANCE:4",
                        "TWAROG:ACIDITY:2", "TWAROG:DOMINANCE:2", "TWAROG:FATTINESS:2"
                );

        assertThat(jdbcTemplate.queryForList("""
                select concept.code || ':' || flag.code
                from ingredient_culinary_flag assignment
                join ingredient_concept concept on concept.id = assignment.ingredient_concept_id
                join culinary_flag flag on flag.id = assignment.culinary_flag_id
                where concept.code in (%s)
                order by concept.code, flag.code
                """.formatted(NEW_CODES_SQL), String.class))
                .containsExactly(
                        "DRIED_WILD_MUSHROOMS:DRIED", "FERMENTED_CUCUMBER:FERMENTED", "FERMENTED_CUCUMBER:PICKLED",
                        "PICKLED_MUSHROOMS:PICKLED", "SOUR_RYE_STARTER:FERMENTED", "TWAROG:FERMENTED"
                );
    }
}
