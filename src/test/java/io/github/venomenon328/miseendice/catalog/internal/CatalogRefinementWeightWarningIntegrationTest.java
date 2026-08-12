package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChange;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.RefinementChangeType;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

/** Proves that draw-weight guidance uses the graph produced by the pending relation save. */
@SpringBootTest
@Testcontainers
class CatalogRefinementWeightWarningIntegrationTest {

    private static final String CODE = "TEST_ISSUE21_COOKING_ALCOHOL_WARNING";
    private static final String ACTOR = "issue21-weight-review-admin";

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
    private CatalogCommands catalogCommands;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from ingredient_concept where code = ?", CODE);
    }

    @Test
    void pendingCookingAlcoholParentTriggersWarningBeforeAnyGraphWrite() {
        long parentId = jdbcTemplate.queryForObject(
                "select id from ingredient_concept where code = 'COOKING_ALCOHOL'", Long.class);
        long parentVersion = jdbcTemplate.queryForObject(
                "select version from ingredient_concept where id = ?", Long.class, parentId);
        String sharedRole = jdbcTemplate.queryForObject("""
                select fr.code
                from ingredient_functional_role ifr
                join functional_role fr on fr.id = ifr.functional_role_id
                where ifr.ingredient_concept_id = ?
                order by fr.code
                limit 1
                """, String.class, parentId);

        long childId = jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, 'Issue 21 cooking alcohol warning', true, false, 'SPECIFIC', 1.0000)
                returning id
                """, Long.class, CODE);
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, childId, sharedRole);
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select ?, id, 'EASY' from participant where code in ('GEORGIA', 'TOBIAS')
                """, childId);

        UpdateIngredientConceptCommand command = new UpdateIngredientConceptCommand(
                childId,
                0,
                "Issue 21 cooking alcohol warning",
                true,
                true,
                "SPECIFIC",
                new BigDecimal("0.50"),
                null,
                null,
                ACTOR,
                false,
                List.of(new RefinementChange(parentId, childId, RefinementChangeType.ADD)),
                Map.of(parentId, parentVersion),
                false
        );

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command))
                .isInstanceOf(CatalogDrawWeightWarningException.class)
                .satisfies(exception -> assertThat(((CatalogDrawWeightWarningException) exception).warnings())
                        .anyMatch(warning -> warning.contains("Kochalkohol")));

        assertThat(jdbcTemplate.queryForObject("""
                select exists (
                    select 1 from ingredient_refinement
                    where parent_concept_id = ? and child_concept_id = ?
                )
                """, Boolean.class, parentId, childId)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "select version from ingredient_concept where id = ?", Long.class, childId)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select version from ingredient_concept where id = ?", Long.class, parentId)).isEqualTo(parentVersion);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR)).isZero();
    }
}
