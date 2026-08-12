package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.CreateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.ExclusionTarget;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionCommands.UpdateExclusionRuleCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogExclusionVersionConflictException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL coverage for the exclusion aggregate, optimistic locking, and audit snapshots. */
@SpringBootTest
@Testcontainers
class CatalogExclusionCommandServiceIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE30_EXCLUSION_";
    private static final String ACTOR = "issue30-exclusion-admin";

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
    private CatalogExclusionCommands exclusionCommands;

    @Autowired
    private CatalogExclusionQueries exclusionQueries;

    @Autowired
    private CatalogAuditLog auditLog;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("delete from exclusion_rule where code like ?", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void savesTargetsAtomicallyWithTheirRuleAndWritesCompleteBeforeAndAfterSnapshots() {
        long firstTarget = insertTarget("FIRST", true);
        long secondTarget = insertTarget("SECOND", false);
        var created = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "RULE", "Issue thirty Ausschluss", true, new BigDecimal("0.8000"), "erste Notiz",
                List.of(new ExclusionTarget(firstTarget, true)), ACTOR));

        var createdDetail = exclusionQueries.findExclusionRule(created.exclusionRuleId()).orElseThrow();
        assertThat(createdDetail).extracting(CatalogExclusionQueries.CatalogExclusionRuleDetail::code,
                        CatalogExclusionQueries.CatalogExclusionRuleDetail::active,
                        CatalogExclusionQueries.CatalogExclusionRuleDetail::version)
                .containsExactly(PREFIX + "RULE", true, 0L);
        assertThat(createdDetail.targets()).extracting(CatalogExclusionQueries.CatalogExclusionTarget::ingredientConceptId,
                CatalogExclusionQueries.CatalogExclusionTarget::includeRefinements).containsExactly(tuple(firstTarget, true));

        var updated = exclusionCommands.updateExclusionRule(new UpdateExclusionRuleCommand(created.exclusionRuleId(), 0,
                "Issue thirty Ausschluss aktualisiert", true, new BigDecimal("0.7500"), "zweite Notiz",
                List.of(new ExclusionTarget(secondTarget, false)), ACTOR));
        var updatedDetail = exclusionQueries.findExclusionRule(updated.exclusionRuleId()).orElseThrow();
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updatedDetail.targets()).extracting(CatalogExclusionQueries.CatalogExclusionTarget::ingredientConceptId,
                CatalogExclusionQueries.CatalogExclusionTarget::active,
                CatalogExclusionQueries.CatalogExclusionTarget::includeRefinements).containsExactly(tuple(secondTarget, false, false));
        assertThat(jdbcTemplate.queryForObject("select count(*) from exclusion_rule_target where exclusion_rule_id = ?", Integer.class,
                created.exclusionRuleId())).isEqualTo(1);
        var audit = auditLog.findById(latestAuditId()).orElseThrow();
        assertThat(audit.beforeState().values()).containsKeys("targets", "code", "active");
        assertThat(audit.afterState().values()).containsKeys("targets", "code", "active");
        assertThat(audit.beforeState().values().get("targets").toString()).contains("includeRefinements=true");
        assertThat(audit.afterState().values().get("targets").toString()).contains("includeRefinements=false");
    }

    @Test
    void allowsTargetlessInactiveRulesButNotActiveRulesAndRejectsStaleVersionsWithoutAudit() {
        var inactive = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "INACTIVE", "Issue thirty inaktiv", false, BigDecimal.ONE, null, List.of(), ACTOR));
        assertThat(exclusionQueries.findExclusionRule(inactive.exclusionRuleId()).orElseThrow().targets()).isEmpty();

        assertThatThrownBy(() -> exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "TARGETLESS", "Issue thirty aktiv ohne Ziel", true, BigDecimal.ONE, null, List.of(), ACTOR)))
                .isInstanceOf(CatalogCommandValidationException.class);

        exclusionCommands.updateExclusionRule(new UpdateExclusionRuleCommand(inactive.exclusionRuleId(), 0,
                "Issue thirty inaktiv geändert", false, BigDecimal.ONE, null, List.of(), ACTOR));
        int auditCountBeforeStaleWrite = auditCount();
        assertThatThrownBy(() -> exclusionCommands.updateExclusionRule(new UpdateExclusionRuleCommand(inactive.exclusionRuleId(), 0,
                "veraltet", false, BigDecimal.ONE, null, List.of(), ACTOR)))
                .isInstanceOf(CatalogExclusionVersionConflictException.class);
        assertThat(auditCount()).isEqualTo(auditCountBeforeStaleWrite);
        assertThat(exclusionQueries.findExclusionRule(inactive.exclusionRuleId()).orElseThrow().displayText())
                .isEqualTo("Issue thirty inaktiv geändert");
    }

    @Test
    void rejectsDuplicateAndUnknownTargetsAndClassifiesCaseInsensitiveDisplayUniqueness() {
        long target = insertTarget("VALIDATION", true);
        assertThatThrownBy(() -> new CreateExclusionRuleCommand(
                PREFIX + "DUP_TARGET", "Issue thirty duplicate target", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(target, false), new ExclusionTarget(target, true)), ACTOR))
                .isInstanceOf(CatalogCommandValidationException.class);

        assertThatThrownBy(() -> exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "UNKNOWN_TARGET", "Issue thirty unknown target", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(Long.MAX_VALUE, false)), ACTOR)))
                .isInstanceOf(CatalogCommandValidationException.class);

        exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "DISPLAY_A", "Issue Thirty Case Unique", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(target, false)), ACTOR));
        assertThatThrownBy(() -> exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(
                PREFIX + "DISPLAY_B", "issue thirty case unique", true, BigDecimal.ONE, null,
                List.of(new ExclusionTarget(target, false)), ACTOR)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKey("displayText"));
    }

    @Test
    void classifiesKnownUniquenessButDoesNotMaskUnknownDatabaseFailures() {
        long target = insertTarget("UNKNOWN", true);
        exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(PREFIX + "UNIQUE", "Issue thirty eindeutig", true,
                BigDecimal.ONE, null, List.of(new ExclusionTarget(target, false)), ACTOR));
        assertThatThrownBy(() -> exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(PREFIX + "UNIQUE",
                "anderer Text", true, BigDecimal.ONE, null, List.of(new ExclusionTarget(target, false)), ACTOR)))
                .isInstanceOf(CatalogCommandValidationException.class);

        long failureTarget = insertTarget("FAILURE", true);
        var failure = exclusionCommands.createExclusionRule(new CreateExclusionRuleCommand(PREFIX + "FAILURE", "Issue thirty Fehler",
                true, BigDecimal.ONE, null, List.of(new ExclusionTarget(failureTarget, false)), ACTOR));
        jdbcTemplate.execute("alter table exclusion_rule add constraint ck_issue30_unknown_error check (curator_note is distinct from 'FORCE_UNKNOWN_ERROR')");
        try {
            assertThatThrownBy(() -> exclusionCommands.updateExclusionRule(new UpdateExclusionRuleCommand(failure.exclusionRuleId(), 0,
                    "Issue thirty Fehler", true, BigDecimal.ONE, "FORCE_UNKNOWN_ERROR",
                    List.of(new ExclusionTarget(failureTarget, false)), ACTOR)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CatalogCommandValidationException.class);
        } finally {
            jdbcTemplate.execute("alter table exclusion_rule drop constraint ck_issue30_unknown_error");
        }
    }

    private long insertTarget(String suffix, boolean active) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight)
                values (?, ?, ?, false, 'SPECIFIC', 1.0000) returning id
                """, Long.class, PREFIX + suffix, "Issue thirty target " + suffix, active);
    }

    private long latestAuditId() {
        return jdbcTemplate.queryForObject("select id from catalog_audit_entry where actor_key = ? order by id desc limit 1", Long.class, ACTOR);
    }

    private int auditCount() {
        return jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR);
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
