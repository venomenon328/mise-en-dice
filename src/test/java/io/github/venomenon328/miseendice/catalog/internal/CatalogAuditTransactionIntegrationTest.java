package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkAction;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkOperation;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkSelection;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.UpdateExclusionRuleCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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

/** Proves that audit persistence failures roll catalog mutations back with them. */
@SpringBootTest(classes = {MiseEnDiceApplication.class, CatalogAuditTransactionIntegrationTest.FailingAuditConfiguration.class})
@Testcontainers
class CatalogAuditTransactionIntegrationTest {

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
    private CatalogBulkCommands bulkCommands;

    @Autowired
    private CatalogExclusionCommands exclusionCommands;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removePhase8SetupData() {
        jdbcTemplate.update("delete from exclusion_rule where code like 'TEST_ISSUE30_AUDIT_%'");
        jdbcTemplate.update("delete from ingredient_concept where code like 'TEST_ISSUE30_AUDIT_%'");
    }

    @Test
    void rollsBackTheNewConceptWhenWritingItsAuditEntryFails() {
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                "TEST_ISSUE11_AUDIT_FAILURE", "Issue eleven audit failure", "issue11-audit-admin"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept where code = 'TEST_ISSUE11_AUDIT_FAILURE'", Integer.class
        )).isZero();
    }

    @Test
    void rollsBackExclusionFieldsTargetsAndVersionWhenAuditFails() {
        long firstTarget = insertIngredient("FIRST");
        long secondTarget = insertIngredient("SECOND");
        long ruleId = jdbcTemplate.queryForObject("""
                insert into exclusion_rule (code, display_text, active, base_draw_weight, curator_note)
                values ('TEST_ISSUE30_AUDIT_RULE', 'vorher', true, 1.0000, 'alt') returning id
                """, Long.class);
        jdbcTemplate.update("""
                insert into exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                values (?, ?, false)
                """, ruleId, firstTarget);

        assertThatThrownBy(() -> exclusionCommands.updateExclusionRule(new UpdateExclusionRuleCommand(
                ruleId, 0, "nachher", true, new BigDecimal("0.7500"), "neu",
                List.of(new ExclusionTarget(secondTarget, true)), "issue30-audit-admin")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForMap(
                "select display_text, base_draw_weight, curator_note, version from exclusion_rule where id = ?", ruleId))
                .containsEntry("display_text", "vorher")
                .containsEntry("curator_note", "alt")
                .containsEntry("version", 0L);
        assertThat(jdbcTemplate.queryForList(
                "select ingredient_concept_id from exclusion_rule_target where exclusion_rule_id = ?", Long.class, ruleId))
                .containsExactly(firstTarget);
    }

    @Test
    void rollsBackEveryBulkMutationAndVersionWhenAuditFails() {
        long first = insertIngredient("BULK_FIRST");
        long second = insertIngredient("BULK_SECOND");
        BulkOperation operation = new BulkOperation(
                List.of(new BulkSelection(first, 0), new BulkSelection(second, 0)),
                BulkAction.DEACTIVATE, null, null, true, "issue30-audit-admin");

        assertThatThrownBy(() -> bulkCommands.execute(operation))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deliberate audit failure");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept where id in (?, ?) and active and version = 0",
                Integer.class, first, second)).isEqualTo(2);
    }

    private long insertIngredient(String suffix) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept
                    (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, true, false, 'SPECIFIC', 1.0000) returning id
                """, Long.class, "TEST_ISSUE30_AUDIT_" + suffix, "Issue thirty audit " + suffix);
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
