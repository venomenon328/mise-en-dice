package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import io.github.venomenon328.miseendice.catalog.api.CatalogAggregateSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntry;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditEntryDraft;
import io.github.venomenon328.miseendice.catalog.api.CatalogAuditLog;
import io.github.venomenon328.miseendice.catalog.internal.JdbcCatalogAggregateVersionRepository;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class PostgresIntegrationTest {

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

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CatalogAuditLog catalogAuditLog;

    @Autowired
    private JdbcCatalogAggregateVersionRepository aggregateVersionRepository;

    @Test
    void applicationContextStartsWithTheCompleteLiquibaseBaseline() {
        assertThat(count("databasechangelog")).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' "
                        + "and table_name in ('ingredient_concept', 'ingredient_refinement', 'exclusion_rule')",
                Integer.class
        )).isEqualTo(3);
    }

    @Test
    void administrationChangesetInitializesVersionsAndAuditSchema() {
        assertThat(countWhere("ingredient_concept", "version = 0")).isEqualTo(count("ingredient_concept"));
        assertThat(countWhere("exclusion_rule", "version = 0")).isEqualTo(count("exclusion_rule"));
        assertThat(jdbcTemplate.queryForList(
                """
                select column_name || ':' || data_type
                from information_schema.columns
                where table_schema = 'public' and table_name = 'catalog_audit_entry'
                """,
                String.class
        )).contains("before_state:jsonb", "after_state:jsonb");
        assertThat(jdbcTemplate.queryForList(
                """
                select indexname
                from pg_indexes
                where schemaname = 'public' and tablename = 'catalog_audit_entry'
                """,
                String.class
        )).contains(
                "ix_catalog_audit_entry_entity_occurred_at",
                "ix_catalog_audit_entry_actor_occurred_at",
                "ix_catalog_audit_entry_change_group"
        );
        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.table_constraints
                where table_schema = 'public'
                  and table_name = 'catalog_audit_entry'
                  and constraint_type = 'FOREIGN KEY'
                """,
                Integer.class
        )).isZero();
    }

    @Test
    void completeLiquibaseBaselineSatisfiesTheDirectRefinementRoleContract() {
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from ingredient_refinement refinement
                where not exists (
                    select 1
                    from ingredient_functional_role parent_role
                    join ingredient_functional_role child_role
                      on child_role.functional_role_id = parent_role.functional_role_id
                    where parent_role.ingredient_concept_id = refinement.parent_concept_id
                      and child_role.ingredient_concept_id = refinement.child_concept_id
                )
                """, Integer.class)).isZero();
    }

    @Test
    void upgradeFromThePreviousLiquibaseBaselineAppliesAdministrationFoundation() throws Exception {
        String upgradeDatabase = "administration_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create database " + upgradeDatabase);
        }

        String upgradeUrl = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + upgradeDatabase);
        try (Connection connection = DriverManager.getConnection(upgradeUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-administration.yaml");
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(countWhere(connection, "ingredient_concept", "version = 0"))
                    .isEqualTo(count(connection, "ingredient_concept"));
            assertThat(countWhere(connection, "exclusion_rule", "version = 0"))
                    .isEqualTo(count(connection, "exclusion_rule"));
            assertThat(count(connection, "catalog_audit_entry")).isZero();
            assertThat(count(connection, "ingredient_refinement")).isPositive();
        }
    }

    @Test
    void catalogAuditEntriesPersistAndReadBackAggregateSnapshots() {
        UUID changeGroupId = UUID.randomUUID();
        CatalogAuditEntry persisted = catalogAuditLog.append(new CatalogAuditEntryDraft(
                changeGroupId,
                "editor-tobias",
                "INGREDIENT_CONCEPT",
                42,
                "UPDATED",
                new CatalogAggregateSnapshot(Map.of("displayName", "Test concept", "active", true)),
                new CatalogAggregateSnapshot(Map.of("displayName", "Updated test concept", "active", true))
        ));

        assertThat(persisted.id()).isPositive();
        assertThat(persisted.changeGroupId()).isEqualTo(changeGroupId);
        assertThat(persisted.payloadVersion()).isEqualTo((short) 1);
        assertThat(persisted.occurredAt()).isNotNull();
        assertThat(catalogAuditLog.findById(persisted.id()))
                .contains(persisted);
    }

    @Test
    void aggregateVersionUpdatesRequireTheExpectedVersion() {
        long conceptId = insertConcept("versioned-concept");
        long exclusionRuleId = insertReturningId(
                """
                insert into exclusion_rule (code, display_text, base_draw_weight)
                values (?, ?, 1.0000)
                returning id
                """,
                "TEST_VERSION_" + UUID.randomUUID().toString().replace("-", ""),
                "Test version " + UUID.randomUUID()
        );
        try {
            assertThat(aggregateVersionRepository.advanceIngredientConceptVersion(conceptId, 0)).isTrue();
            assertThat(aggregateVersionRepository.advanceIngredientConceptVersion(conceptId, 0)).isFalse();
            assertThat(aggregateVersionRepository.advanceExclusionRuleVersion(exclusionRuleId, 0)).isTrue();
            assertThat(aggregateVersionRepository.advanceExclusionRuleVersion(exclusionRuleId, 0)).isFalse();
        } finally {
            jdbcTemplate.update("delete from exclusion_rule where id = ?", exclusionRuleId);
            jdbcTemplate.update("delete from ingredient_concept where id = ?", conceptId);
        }
    }

    @Test
    void secondLiquibaseExecutionLeavesOperationalCatalogChangesUntouched() throws Exception {
        long conceptId = insertConcept("liquibase-rerun");
        String changedName = "Local curation " + UUID.randomUUID();
        try {
            jdbcTemplate.update("update ingredient_concept set display_name = ? where id = ?", changedName, conceptId);
            rerunLiquibase();

            assertThat(jdbcTemplate.queryForObject(
                    "select display_name from ingredient_concept where id = ?", String.class, conceptId))
                    .isEqualTo(changedName);
        } finally {
            jdbcTemplate.update("delete from ingredient_concept where id = ?", conceptId);
        }
    }

    @Test
    void refinementCycleIsRejectedByThePostgresqlTrigger() {
        long parent = insertConcept("cycle-parent");
        long child = insertConcept("cycle-child");
        long grandchild = insertConcept("cycle-grandchild");

        try {
            jdbcTemplate.update(
                    "insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)",
                    parent,
                    child
            );
            jdbcTemplate.update(
                    "insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)",
                    child,
                    grandchild
            );

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)",
                    grandchild,
                    parent
            ))
                    .isInstanceOf(UncategorizedSQLException.class)
                    .hasMessageContaining("ingredient refinement would create a cycle");
        } finally {
            jdbcTemplate.update(
                    "delete from ingredient_refinement where parent_concept_id in (?, ?, ?) or child_concept_id in (?, ?, ?)",
                    parent,
                    child,
                    grandchild,
                    parent,
                    child,
                    grandchild
            );
            jdbcTemplate.update("delete from ingredient_concept where id in (?, ?, ?)", parent, child, grandchild);
        }
    }

    @Test
    void updatedAtTriggerTouchesIngredientConcepts() {
        long conceptId = insertConcept("updated-at");
        try {
            OffsetDateTime before = jdbcTemplate.queryForObject(
                    "select updated_at from ingredient_concept where id = ?", OffsetDateTime.class, conceptId);

            jdbcTemplate.update(
                    "update ingredient_concept set curator_note = ? where id = ?",
                    "updated-at trigger test",
                    conceptId
            );

            OffsetDateTime after = jdbcTemplate.queryForObject(
                    "select updated_at from ingredient_concept where id = ?", OffsetDateTime.class, conceptId);
            assertThat(after).isAfter(before);
        } finally {
            jdbcTemplate.update("delete from ingredient_concept where id = ?", conceptId);
        }
    }

    @Test
    void updatedAtTriggersTouchAvailabilityAndExclusionRules() {
        long conceptId = insertConcept("availability-updated-at");
        long participantId = insertReturningId(
                "insert into participant (code, display_name) values (?, ?) returning id",
                "TEST_TRIGGER_PARTICIPANT_" + UUID.randomUUID().toString().replace("-", ""),
                "Test trigger participant"
        );
        long exclusionRuleId = insertReturningId(
                "insert into exclusion_rule (code, display_text, base_draw_weight) values (?, ?, 1.0000) returning id",
                "TEST_TRIGGER_RULE_" + UUID.randomUUID().toString().replace("-", ""),
                "Test trigger rule"
        );
        try {
            jdbcTemplate.update(
                    "insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level) values (?, ?, 'EASY')",
                    conceptId,
                    participantId
            );
            OffsetDateTime availabilityBefore = jdbcTemplate.queryForObject(
                    "select updated_at from ingredient_availability where ingredient_concept_id = ? and participant_id = ?",
                    OffsetDateTime.class,
                    conceptId,
                    participantId
            );
            jdbcTemplate.update(
                    "update ingredient_availability set availability_level = availability_level where ingredient_concept_id = ? and participant_id = ?",
                    conceptId,
                    participantId
            );
            OffsetDateTime availabilityAfter = jdbcTemplate.queryForObject(
                    "select updated_at from ingredient_availability where ingredient_concept_id = ? and participant_id = ?",
                    OffsetDateTime.class,
                    conceptId,
                    participantId
            );
            assertThat(availabilityAfter).isAfter(availabilityBefore);

            OffsetDateTime exclusionBefore = jdbcTemplate.queryForObject(
                    "select updated_at from exclusion_rule where id = ?", OffsetDateTime.class, exclusionRuleId);
            jdbcTemplate.update(
                    "update exclusion_rule set curator_note = ? where id = ?",
                    "updated-at trigger test",
                    exclusionRuleId
            );
            OffsetDateTime exclusionAfter = jdbcTemplate.queryForObject(
                    "select updated_at from exclusion_rule where id = ?", OffsetDateTime.class, exclusionRuleId);
            assertThat(exclusionAfter).isAfter(exclusionBefore);
        } finally {
            jdbcTemplate.update("delete from exclusion_rule where id = ?", exclusionRuleId);
            jdbcTemplate.update("delete from ingredient_availability where ingredient_concept_id = ?", conceptId);
            jdbcTemplate.update("delete from participant where id = ?", participantId);
            jdbcTemplate.update("delete from ingredient_concept where id = ?", conceptId);
        }
    }

    @Test
    void rerollAttemptRequiresACommittedRerollExposure() {
        long session = insertReturningId("insert into challenge_session default values returning id");
        insertAttempt(session, "INITIAL");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into generation_attempt (challenge_session_id, attempt_type, generator_version) values (?, 'REROLL', 'test')",
                session
        ))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("requires a committed rerolled offer exposure");
    }

    @Test
    void manualRequirementsMustBelongToTheCandidateAttempt() {
        long firstAttempt = insertAttempt(
                insertReturningId("insert into challenge_session default values returning id"),
                "INITIAL"
        );
        long secondAttempt = insertAttempt(
                insertReturningId("insert into challenge_session default values returning id"),
                "INITIAL"
        );
        long manualRequirement = insertReturningId(
                """
                insert into generation_manual_requirement (generation_attempt_id, position, display_text)
                values (?, 1, 'manual')
                returning id
                """,
                firstAttempt
        );
        long candidate = insertCandidate(secondAttempt, false);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                insert into candidate_requirement
                    (candidate_id, position, source, manual_requirement_id, display_text_snapshot)
                values (?, 1, 'MANUAL', ?, 'manual')
                """,
                candidate,
                manualRequirement
        ))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("does not belong to candidate generation attempt");
    }

    @Test
    void visibleChallengeRequiresItsConfirmedOfferAndExactlyFourRequirements() {
        long attempt = insertAttempt(
                insertReturningId("insert into challenge_session default values returning id"),
                "INITIAL"
        );
        long candidate = insertCandidate(attempt, false);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)",
                attempt,
                candidate
        ))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("must contain exactly four requirements");

        insertRandomRequirements(candidate, 4);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)",
                attempt,
                candidate
        ))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("new challenges require a confirmed curated offer");

        jdbcTemplate.update("update challenge_candidate set is_selected = true where id = ?", candidate);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into challenge (generation_attempt_id, selected_candidate_id, legacy_pre_offer_decision) values (?, ?, true)",
                attempt,
                candidate
        ))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("legacy challenge marker is reserved for rows present before migration 008");
    }

    private void rerunLiquibase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private long insertConcept(String label) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return insertReturningId(
                """
                insert into ingredient_concept
                    (code, display_name, challenge_specificity, base_draw_weight)
                values (?, ?, 'SPECIFIC', 1.0000)
                returning id
                """,
                "TEST_" + label.toUpperCase() + "_" + token,
                "Test " + label + " " + token
        );
    }

    private long insertAttempt(long sessionId, String attemptType) {
        return insertReturningId(
                """
                insert into generation_attempt (challenge_session_id, attempt_type, generator_version)
                values (?, ?, 'test')
                returning id
                """,
                sessionId,
                attemptType
        );
    }

    private long insertCandidate(long attemptId, boolean selected) {
        long round = insertReturningId(
                """
                insert into curation_round (
                    generation_attempt_id, round_number, curator_model, prompt_version, status, completed_at,
                    legacy_migrated
                ) values (?, 1, 'test', 'test', 'SELECTED', now(), true)
                returning id
                """,
                attemptId
        );
        long batch = insertReturningId(
                """
                insert into generation_batch
                    (generation_attempt_id, batch_number, status, legacy_migrated)
                values (?, 1, 'GENERATED', true)
                returning id
                """,
                attemptId
        );
        return insertReturningId(
                """
                insert into challenge_candidate
                    (generation_batch_id, curation_round_id, candidate_number, is_selected)
                values (?, ?, 1, ?)
                returning id
                """,
                batch,
                round,
                selected
        );
    }

    private void insertRandomRequirements(long candidateId, int amount) {
        int existing = jdbcTemplate.queryForObject(
                "select count(*) from candidate_requirement where candidate_id = ?",
                Integer.class,
                candidateId
        );
        for (int offset = 0; offset < amount; offset++) {
            long conceptId = insertConcept("candidate-requirement-" + offset);
            String displayName = jdbcTemplate.queryForObject(
                    "select display_name from ingredient_concept where id = ?",
                    String.class,
                    conceptId
            );
            String specificity = jdbcTemplate.queryForObject(
                    "select challenge_specificity from ingredient_concept where id = ?",
                    String.class,
                    conceptId
            );
            jdbcTemplate.update(
                    """
                    insert into candidate_requirement
                        (candidate_id, position, source, ingredient_concept_id, challenge_specificity_snapshot, display_text_snapshot)
                    values (?, ?, 'RANDOM', ?, ?, ?)
                    """,
                    candidateId,
                    existing + offset + 1,
                    conceptId,
                    specificity,
                    displayName
            );
        }
    }

    private long insertReturningId(String sql, Object... arguments) {
        return jdbcTemplate.queryForObject(sql, Long.class, arguments);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private int countWhere(String table, String whereClause) {
        return jdbcTemplate.queryForObject(
                "select count(*) from " + table + " where " + whereClause,
                Integer.class
        );
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static int countWhere(Connection connection, String table, String whereClause) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "select count(*) from " + table + " where " + whereClause
             )) {
            result.next();
            return result.getInt(1);
        }
    }

}
