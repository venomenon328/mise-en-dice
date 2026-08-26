package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CatalogMetadata;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Proves that an audit failure also rolls an existing aggregate update and version advance back. */
@SpringBootTest(classes = {MiseEnDiceApplication.class, CatalogAuditUpdateRollbackIntegrationTest.FailingAuditConfiguration.class})
@Testcontainers
class CatalogAuditUpdateRollbackIntegrationTest {

    private static final String CODE = "TEST_ISSUE11_AUDIT_UPDATE_FAILURE";
    private static final String RELATION_PARENT_CODE = "TEST_ISSUE21_AUDIT_PARENT";
    private static final String RELATION_CHILD_CODE = "TEST_ISSUE21_AUDIT_CHILD";

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
    void removeTestConcept() {
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code in (?, ?))
                   or child_concept_id in (select id from ingredient_concept where code in (?, ?))
                """, RELATION_PARENT_CODE, RELATION_CHILD_CODE, RELATION_PARENT_CODE, RELATION_CHILD_CODE);
        jdbcTemplate.update("delete from ingredient_concept where code = ?", CODE);
        jdbcTemplate.update("delete from ingredient_concept where code in (?, ?)", RELATION_PARENT_CODE, RELATION_CHILD_CODE);
    }

    @Test
    void rollsBackExistingConceptFieldsAndVersionWhenWritingAuditFails() {
        long conceptId = jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    curator_note
                ) values (?, 'Audit update original', true, false, 'SPECIFIC', 1.0000, 'Technische Testnotiz.')
                returning id
                """, Long.class, CODE);

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                conceptId,
                0,
                "Audit update changed",
                true,
                false,
                "SPECIFIC",
                new BigDecimal("0.7500"),
                2,
                "must roll back",
                "issue11-audit-update-admin",
                false,
                List.of(),
                Map.of(),
                false,
                new CatalogMetadata(
                        Set.of("VEGETABLE"), Set.of("FERMENTED"), Map.of("HEAT", 4),
                        Map.of("GEORGIA", io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability.EASY,
                                "TOBIAS", io.github.venomenon328.miseendice.catalog.api.CatalogQueries.CatalogAvailability.PLANNED),
                        Map.of(1, new BigDecimal("1.4")))
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForMap("""
                select display_name, version, base_draw_weight, novelty_level, curator_note
                from ingredient_concept
                where id = ?
                """, conceptId))
                .containsEntry("display_name", "Audit update original")
                .containsEntry("version", 0L)
                .containsEntry("base_draw_weight", new BigDecimal("1.0000"))
                .containsEntry("novelty_level", null)
                .containsEntry("curator_note", "Technische Testnotiz.");
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_functional_role where ingredient_concept_id = ?", Integer.class, conceptId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_culinary_flag where ingredient_concept_id = ?", Integer.class, conceptId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_culinary_dimension where ingredient_concept_id = ?", Integer.class, conceptId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_availability where ingredient_concept_id = ?", Integer.class, conceptId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_seasonality where ingredient_concept_id = ?", Integer.class, conceptId)).isZero();
    }

    @Test
    void rollsBackVersionsAndPendingRefinementWhenAnyAggregateAuditWriteFails() {
        long parentId = insertRefinementConcept(RELATION_PARENT_CODE, "Audit relation parent", "OPEN");
        long childId = insertRefinementConcept(RELATION_CHILD_CODE, "Audit relation child", "SPECIFIC");

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                childId, 0, "Audit relation child", true, false, "SPECIFIC", BigDecimal.ONE, null,
                "Technische Testnotiz.",
                "issue21-audit-update-admin", false,
                List.of(new CatalogCommands.RefinementChange(parentId, childId, CatalogCommands.RefinementChangeType.ADD)),
                Map.of(parentId, 0L), false
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForObject("select count(*) from ingredient_refinement where parent_concept_id = ? and child_concept_id = ?",
                Integer.class, parentId, childId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?", Long.class, parentId)).isZero();
        assertThat(jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?", Long.class, childId)).isZero();
    }

    private long insertRefinementConcept(String code, String displayName, String specificity) {
        long conceptId = jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    curator_note
                ) values (?, ?, true, false, ?, 1.0000, 'Technische Testnotiz.')
                returning id
                """, Long.class, code, displayName, specificity);
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = 'ANIMAL_PROTEIN'
                """, conceptId);
        return conceptId;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingAuditConfiguration {

        @Bean
        @Primary
        CatalogAuditLog failingCatalogAuditLog() {
            return new CatalogAuditLog() {
                @Override
                public CatalogAuditEntry append(CatalogAuditEntryDraft entry) {
                    throw new IllegalStateException("deliberate audit failure");
                }

                @Override
                public Optional<CatalogAuditEntry> findById(long id) {
                    return Optional.empty();
                }
            };
        }
    }
}
