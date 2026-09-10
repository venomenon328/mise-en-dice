package io.github.venomenon328.miseendice;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import io.github.venomenon328.miseendice.catalog.internal.JdbcCatalogAggregateVersionRepository;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;

@SpringBootTest
class PostgresIntegrationTest extends CurrentSchemaPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcCatalogAggregateVersionRepository aggregateVersionRepository;

    @AfterEach
    void removesOperationalFixtures() {
        jdbcTemplate.execute("truncate table challenge_session restart identity cascade");
    }

    @Test
    void applicationContextStartsWithTheCompleteLiquibaseBaseline() {
        assertThat(count("databasechangelog")).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from databasechangelog where id = '018-five-level-availability'", Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' "
                        + "and table_name in ('ingredient_concept', 'ingredient_refinement', 'exclusion_rule')",
                Integer.class
        )).isEqualTo(3);
    }

    @Test
    void availabilityConstraintAcceptsAllFiveLevelsAndRejectsUnknownLevels() {
        long conceptId = insertConcept("five-level-availability");
        long participantId = insertReturningId(
                "insert into participant (code, display_name) values (?, ?) returning id",
                "TEST_FIVE_LEVEL_PARTICIPANT_" + UUID.randomUUID().toString().replace("-", ""),
                "Test five-level participant"
        );
        try {
            jdbcTemplate.update("""
                    insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                    values (?, ?, 'EASY')
                    """, conceptId, participantId);
            for (String level : List.of("EASY", "PLANNED", "SPECIALTY", "DIFFICULT", "UNAVAILABLE")) {
                jdbcTemplate.update("""
                        update ingredient_availability
                        set availability_level = ?
                        where ingredient_concept_id = ? and participant_id = ?
                        """, level, conceptId, participantId);
                assertThat(jdbcTemplate.queryForObject("""
                        select availability_level from ingredient_availability
                        where ingredient_concept_id = ? and participant_id = ?
                        """, String.class, conceptId, participantId)).isEqualTo(level);
            }

            assertThatThrownBy(() -> jdbcTemplate.update("""
                    update ingredient_availability
                    set availability_level = 'NOT_A_LEVEL'
                    where ingredient_concept_id = ? and participant_id = ?
                    """, conceptId, participantId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("delete from ingredient_availability where ingredient_concept_id = ?", conceptId);
            jdbcTemplate.update("delete from participant where id = ?", participantId);
            jdbcTemplate.update("delete from ingredient_concept where id = ?", conceptId);
        }
    }

    @Test
    void administrationVersionsRemainAvailableWithoutTheCatalogAuditTable() {
        long newConcept = insertConcept("initial-version");
        try {
            assertThat(jdbcTemplate.queryForObject("select version from ingredient_concept where id = ?",
                    Long.class, newConcept)).isZero();
        } finally {
            jdbcTemplate.update("delete from ingredient_concept where id = ?", newConcept);
        }
        long newRule = insertReturningId(
                "insert into exclusion_rule (code, display_text, base_draw_weight) values (?, ?, 1.0000) returning id",
                "TEST_DEFAULT_" + UUID.randomUUID().toString().replace("-", ""), "Test default version");
        try {
            assertThat(jdbcTemplate.queryForObject("select version from exclusion_rule where id = ?",
                    Long.class, newRule)).isZero();
        } finally {
            jdbcTemplate.update("delete from exclusion_rule where id = ?", newRule);
        }
        assertThat(jdbcTemplate.queryForObject(
                """
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name = 'catalog_audit_entry'
                """,
                Integer.class
        )).isZero();
    }

    @Test
    void ingredientConceptCuratorNotesAreCompleteAndDatabaseEnforced() {
        assertThat(countWhere("ingredient_concept", "curator_note is null or btrim(curator_note) = ''")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                select is_nullable
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'ingredient_concept'
                  and column_name = 'curator_note'
                """,
                String.class
        )).isEqualTo("NO");

        for (String invalidNote : List.of("", "   ")) {
            String token = UUID.randomUUID().toString().replace("-", "");
            assertThatThrownBy(() -> jdbcTemplate.update(
                    """
                    insert into ingredient_concept
                        (code, display_name, challenge_specificity, base_draw_weight, curator_note)
                    values (?, ?, 'SPECIFIC', 1.0000, ?)
                    """,
                    "TEST_NOTE_" + token,
                    "Invalid curator note " + token,
                    invalidNote
            )).isInstanceOf(DataIntegrityViolationException.class);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                insert into ingredient_concept
                    (code, display_name, challenge_specificity, base_draw_weight, curator_note)
                values (?, ?, 'SPECIFIC', 1.0000, null)
                """,
                "TEST_NOTE_" + token,
                "Invalid curator note " + token
        )).isInstanceOf(DataIntegrityViolationException.class);
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
                    (code, display_name, challenge_specificity, base_draw_weight, curator_note)
                values (?, ?, 'SPECIFIC', 1.0000, 'Technische Testnotiz.')
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

}
