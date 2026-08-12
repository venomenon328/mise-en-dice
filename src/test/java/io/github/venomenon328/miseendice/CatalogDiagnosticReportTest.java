package io.github.venomenon328.miseendice;

import java.nio.file.Files;
import java.nio.file.Path;
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
class CatalogDiagnosticReportTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_catalog_diagnostic")
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
    void writesCompleteCatalogDiagnosticReport() throws Exception {
        String report = jdbcTemplate.queryForObject(
                """
                select jsonb_pretty(jsonb_build_object(
                    'concepts', (
                        select jsonb_agg(jsonb_build_object(
                            'code', concept.code,
                            'displayName', concept.display_name,
                            'active', concept.active,
                            'drawEnabled', concept.random_draw_enabled,
                            'specificity', concept.challenge_specificity,
                            'weight', concept.base_draw_weight,
                            'novelty', concept.novelty_level,
                            'note', concept.curator_note,
                            'parents', coalesce((
                                select jsonb_agg(parent.code order by parent.code)
                                from ingredient_refinement relation
                                join ingredient_concept parent on parent.id = relation.parent_concept_id
                                where relation.child_concept_id = concept.id
                            ), '[]'::jsonb),
                            'children', coalesce((
                                select jsonb_agg(child.code order by child.code)
                                from ingredient_refinement relation
                                join ingredient_concept child on child.id = relation.child_concept_id
                                where relation.parent_concept_id = concept.id
                            ), '[]'::jsonb),
                            'roles', coalesce((
                                select jsonb_agg(role.code order by role.code)
                                from ingredient_functional_role assignment
                                join functional_role role on role.id = assignment.functional_role_id
                                where assignment.ingredient_concept_id = concept.id
                            ), '[]'::jsonb),
                            'availability', coalesce((
                                select jsonb_object_agg(participant.code, availability.availability_level order by participant.code)
                                from ingredient_availability availability
                                join participant on participant.id = availability.participant_id
                                where availability.ingredient_concept_id = concept.id
                            ), '{}'::jsonb),
                            'flags', coalesce((
                                select jsonb_agg(flag.code order by flag.code)
                                from ingredient_culinary_flag assignment
                                join culinary_flag flag on flag.id = assignment.culinary_flag_id
                                where assignment.ingredient_concept_id = concept.id
                            ), '[]'::jsonb),
                            'dimensions', coalesce((
                                select jsonb_object_agg(dimension.code, assignment.level order by dimension.code)
                                from ingredient_culinary_dimension assignment
                                join culinary_dimension dimension on dimension.id = assignment.culinary_dimension_id
                                where assignment.ingredient_concept_id = concept.id
                            ), '{}'::jsonb),
                            'seasonality', coalesce((
                                select jsonb_object_agg(seasonality.month::text, seasonality.weight_multiplier order by seasonality.month)
                                from ingredient_seasonality seasonality
                                where seasonality.ingredient_concept_id = concept.id
                            ), '{}'::jsonb)
                        ) order by concept.code)
                        from ingredient_concept concept
                    ),
                    'edges', (
                        select jsonb_agg(jsonb_build_object(
                            'parent', parent.code,
                            'child', child.code
                        ) order by parent.code, child.code)
                        from ingredient_refinement relation
                        join ingredient_concept parent on parent.id = relation.parent_concept_id
                        join ingredient_concept child on child.id = relation.child_concept_id
                    ),
                    'exclusionRules', (
                        select jsonb_agg(jsonb_build_object(
                            'code', rule.code,
                            'displayText', rule.display_text,
                            'active', rule.active,
                            'weight', rule.base_draw_weight,
                            'note', rule.curator_note,
                            'targets', coalesce((
                                select jsonb_agg(jsonb_build_object(
                                    'concept', concept.code,
                                    'includeDescendants', target.include_descendants
                                ) order by concept.code)
                                from exclusion_rule_target target
                                join ingredient_concept concept on concept.id = target.ingredient_concept_id
                                where target.exclusion_rule_id = rule.id
                            ), '[]'::jsonb)
                        ) order by rule.code)
                        from exclusion_rule rule
                    )
                ))
                """,
                String.class
        );

        Path outputDirectory = Path.of("target", "catalog-diagnostic");
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("catalog.json"), report);
    }
}
