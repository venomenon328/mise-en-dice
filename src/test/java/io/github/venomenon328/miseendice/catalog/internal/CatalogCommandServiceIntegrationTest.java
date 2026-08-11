package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommandValidationException;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.CreateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogDrawWeightWarningException;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import io.github.venomenon328.miseendice.catalog.api.CatalogVersionConflictException;
import java.math.BigDecimal;
import java.util.Map;
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

/** Exercises the public write API with PostgreSQL locking, constraints, snapshots, and transactions. */
@SpringBootTest
@Testcontainers
class CatalogCommandServiceIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE11_";
    private static final String ACTOR = "issue11-integration-admin";

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
    private CatalogQueries catalogQueries;

    @Autowired
    private CatalogAuditLog catalogAuditLog;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void createsAConservativeConceptAndAuditsTheCompleteInitialAggregate() {
        var result = catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "CREATE", "Issue eleven creation", ACTOR
        ));

        var detail = catalogQueries.findConcept(result.conceptId()).orElseThrow();
        assertThat(detail).extracting(
                CatalogQueries.CatalogConceptDetail::active,
                CatalogQueries.CatalogConceptDetail::randomDrawEnabled,
                CatalogQueries.CatalogConceptDetail::challengeSpecificity,
                CatalogQueries.CatalogConceptDetail::baseDrawWeight,
                CatalogQueries.CatalogConceptDetail::noveltyLevel,
                CatalogQueries.CatalogConceptDetail::curatorNote,
                CatalogQueries.CatalogConceptDetail::version
        ).containsExactly(true, false, "SPECIFIC", new BigDecimal("1.0000"), null, null, 0L);

        var audit = latestAudit();
        assertThat(audit).extracting(entry -> entry.actorKey(), entry -> entry.entityType(), entry -> entry.action())
                .containsExactly(ACTOR, "INGREDIENT_CONCEPT", "CREATE");
        assertThat(audit.beforeState()).isNull();
        assertThat(audit.afterState().values()).containsKeys(
                "code", "displayName", "active", "randomDrawEnabled", "challengeSpecificity", "baseDrawWeight",
                "functionalRoles", "availability", "directParents", "directChildren", "seasonality"
        );
    }

    @Test
    void updatesExactlyOnceAndKeepsReadOnlyRelationsInBothAuditSnapshots() {
        long parent = insertConcept("PARENT", "Issue eleven parent", "OPEN", true, false, null);
        long concept = insertConcept("UPDATE", "Issue eleven update", "SPECIFIC", true, false, 2);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, concept);
        assignRequiredDrawMetadata(concept);
        var before = catalogQueries.findConcept(concept).orElseThrow();

        var result = catalogCommands.updateIngredientConcept(command(before, "Issue eleven updated", true, false,
                "SPECIFIC", new BigDecimal("0.7500"), 3, "Jetzt mit Notiz", false));

        assertThat(result.version()).isEqualTo(1);
        var after = catalogQueries.findConcept(concept).orElseThrow();
        assertThat(after).extracting(CatalogQueries.CatalogConceptDetail::displayName,
                        CatalogQueries.CatalogConceptDetail::version, CatalogQueries.CatalogConceptDetail::curatorNote)
                .containsExactly("Issue eleven updated", 1L, "Jetzt mit Notiz");
        var audit = latestAudit();
        assertThat(audit.beforeState().values().get("directParents")).asList().hasSize(1);
        assertThat(audit.afterState().values().get("directParents")).asList().hasSize(1);
        assertThat(audit.beforeState().values().get("functionalRoles")).asList().isNotEmpty();
        assertThat(audit.afterState().values().get("availability")).asList().hasSize(2);
    }

    @Test
    void staleVersionCannotChangeTheConceptOrCreateASecondAuditEntry() {
        long concept = insertConcept("STALE", "Issue eleven stale", "SPECIFIC", true, false, null);
        var original = catalogQueries.findConcept(concept).orElseThrow();
        catalogCommands.updateIngredientConcept(command(original, "First editor", true, false,
                "SPECIFIC", BigDecimal.ONE, null, null, false));

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(original, "Stale editor", true, false,
                "SPECIFIC", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogVersionConflictException.class);

        assertThat(catalogQueries.findConcept(concept).orElseThrow())
                .extracting(CatalogQueries.CatalogConceptDetail::displayName, CatalogQueries.CatalogConceptDetail::version)
                .containsExactly("First editor", 1L);
        assertThat(auditCount()).isEqualTo(1);
    }

    @Test
    void rejectsDrawableConceptsWithoutTheirStillReadOnlyRequiredMetadata() {
        long concept = insertConcept("MISSING_METADATA", "Issue eleven incomplete", "OPEN", true, false, null);
        var before = catalogQueries.findConcept(concept).orElseThrow();

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, true,
                "OPEN", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKeys("functionalRoles", "availabilityGeorgia", "availabilityTobias", "challengeSpecificity"));
        assertThat(auditCount()).isZero();
    }

    @Test
    void rejectsSpecificityInversionsOnExistingDirectGraphEdgesWithoutAudit() {
        long parent = insertConcept("SPECIFIC_PARENT", "Issue eleven specific parent", "SPECIFIC", true, false, null);
        long child = insertConcept("OPEN_CHILD", "Issue eleven child", "SPECIFIC", true, false, null);
        jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
        var before = catalogQueries.findConcept(child).orElseThrow();

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, false,
                "OPEN", BigDecimal.ONE, null, null, false)))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors())
                        .containsKey("challengeSpecificity"));
        assertThat(catalogQueries.findConcept(child).orElseThrow().challengeSpecificity()).isEqualTo("SPECIFIC");
        assertThat(auditCount()).isZero();
    }

    @Test
    void requiresThenHonoursExplicitAcknowledgementForAWeightGuidelineWarning() {
        long concept = insertConcept("WARNING", "Issue eleven warning", "SPECIFIC", true, false, 5);
        assignRequiredDrawMetadata(concept);
        var before = catalogQueries.findConcept(concept).orElseThrow();

        assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, true,
                "SPECIFIC", new BigDecimal("0.50"), 5, null, false)))
                .isInstanceOf(CatalogDrawWeightWarningException.class);
        assertThat(auditCount()).isZero();

        var result = catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, true,
                "SPECIFIC", new BigDecimal("0.50"), 5, null, true));
        assertThat(result.version()).isEqualTo(1);
        assertThat(auditCount()).isEqualTo(1);
    }

    @Test
    void classifiesKnownUniqueViolationsButDoesNotMaskAnUnknownDatabaseFailure() {
        catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(PREFIX + "UNIQUE", "Issue eleven unique", ACTOR));
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "UNIQUE", "Another name", ACTOR
        )))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors()).containsKey("code"));
        assertThatThrownBy(() -> catalogCommands.createIngredientConcept(new CreateIngredientConceptCommand(
                PREFIX + "OTHER", "ISSUE ELEVEN UNIQUE", ACTOR
        )))
                .isInstanceOf(CatalogCommandValidationException.class)
                .satisfies(exception -> assertThat(((CatalogCommandValidationException) exception).fieldErrors()).containsKey("displayName"));

        long concept = insertConcept("UNKNOWN", "Issue eleven unknown", "SPECIFIC", true, false, null);
        var before = catalogQueries.findConcept(concept).orElseThrow();
        jdbcTemplate.execute("alter table ingredient_concept add constraint ck_issue11_unknown_error check (curator_note is distinct from 'FORCE_UNKNOWN_ERROR')");
        try {
            assertThatThrownBy(() -> catalogCommands.updateIngredientConcept(command(before, before.displayName(), true, false,
                    "SPECIFIC", BigDecimal.ONE, null, "FORCE_UNKNOWN_ERROR", false)))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(CatalogCommandValidationException.class);
        } finally {
            jdbcTemplate.execute("alter table ingredient_concept drop constraint ck_issue11_unknown_error");
        }
    }

    private CatalogCommands.UpdateIngredientConceptCommand command(
            CatalogQueries.CatalogConceptDetail detail,
            String displayName,
            boolean active,
            boolean randomDrawEnabled,
            String specificity,
            BigDecimal weight,
            Integer novelty,
            String note,
            boolean acknowledgeWarnings
    ) {
        return new UpdateIngredientConceptCommand(
                detail.id(), detail.version(), displayName, active, randomDrawEnabled, specificity,
                weight, novelty, note, ACTOR, acknowledgeWarnings
        );
    }

    private long insertConcept(String suffix, String displayName, String specificity, boolean active, boolean drawable, Integer novelty) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight, novelty_level
                ) values (?, ?, ?, ?, ?, 1.0000, ?)
                returning id
                """, Long.class, PREFIX + suffix, displayName, active, drawable, specificity, novelty);
    }

    private void assignRequiredDrawMetadata(long conceptId) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = 'ANIMAL_PROTEIN'
                """, conceptId);
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select ?, id, 'EASY' from participant where code in ('GEORGIA', 'TOBIAS')
                """, conceptId);
    }

    private io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry latestAudit() {
        long id = jdbcTemplate.queryForObject(
                "select id from catalog_audit_entry where actor_key = ? order by id desc limit 1", Long.class, ACTOR
        );
        return catalogAuditLog.findById(id).orElseThrow();
    }

    private int auditCount() {
        return jdbcTemplate.queryForObject("select count(*) from catalog_audit_entry where actor_key = ?", Integer.class, ACTOR);
    }
}
