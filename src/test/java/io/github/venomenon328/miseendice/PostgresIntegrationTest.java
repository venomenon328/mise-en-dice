package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
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

    @Test
    void applicationContextStartsWithTheCompleteLiquibaseBaseline() {
        assertThat(count("databasechangelog")).isEqualTo(16);
        assertThat(count("ingredient_concept")).isEqualTo(642);
        assertThat(countWhere("ingredient_concept", "active and random_draw_enabled")).isEqualTo(640);
        assertThat(countWhere("ingredient_concept", "active and random_draw_enabled and challenge_specificity = 'OPEN'"))
                .isEqualTo(78);
        assertThat(count("ingredient_refinement")).isEqualTo(765);
        assertThat(countWhere("exclusion_rule", "active")).isEqualTo(22);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ingredient_concept where code = 'ALIGUE'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void secondLiquibaseExecutionLeavesOperationalCatalogChangesUntouched() throws Exception {
        Long conceptId = jdbcTemplate.queryForObject(
                "select id from ingredient_concept where active and random_draw_enabled order by id limit 1", Long.class);
        String changedName = "Local curation " + UUID.randomUUID();

        jdbcTemplate.update("update ingredient_concept set display_name = ? where id = ?", changedName, conceptId);
        rerunLiquibase();

        assertThat(jdbcTemplate.queryForObject(
                "select display_name from ingredient_concept where id = ?", String.class, conceptId))
                .isEqualTo(changedName);
        assertThat(count("ingredient_concept")).isEqualTo(642);
    }

    @Test
    void refinementCycleIsRejectedByThePostgresqlTrigger() {
        long parent = insertConcept("cycle-parent");
        long child = insertConcept("cycle-child");
        long grandchild = insertConcept("cycle-grandchild");

        try {
            jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", parent, child);
            jdbcTemplate.update("insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", child, grandchild);

            assertThatThrownBy(() -> jdbcTemplate.update(
                    "insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)", grandchild, parent))
                    .isInstanceOf(UncategorizedSQLException.class)
                    .hasMessageContaining("ingredient refinement would create a cycle");
        } finally {
            jdbcTemplate.update("delete from ingredient_refinement where parent_concept_id in (?, ?, ?) or child_concept_id in (?, ?, ?)",
                    parent, child, grandchild, parent, child, grandchild);
            jdbcTemplate.update("delete from ingredient_concept where id in (?, ?, ?)", parent, child, grandchild);
        }
    }

    @Test
    void updatedAtTriggerTouchesIngredientConcepts() {
        Long conceptId = jdbcTemplate.queryForObject(
                "select id from ingredient_concept order by id limit 1", Long.class);
        OffsetDateTime before = jdbcTemplate.queryForObject(
                "select updated_at from ingredient_concept where id = ?", OffsetDateTime.class, conceptId);

        jdbcTemplate.update("update ingredient_concept set curator_note = ? where id = ?", "updated-at trigger test", conceptId);

        OffsetDateTime after = jdbcTemplate.queryForObject(
                "select updated_at from ingredient_concept where id = ?", OffsetDateTime.class, conceptId);
        assertThat(after).isAfter(before);
    }

    @Test
    void updatedAtTriggersTouchAvailabilityAndExclusionRules() {
        Long availabilityConceptId = jdbcTemplate.queryForObject(
                "select ingredient_concept_id from ingredient_availability order by ingredient_concept_id limit 1", Long.class);
        Long participantId = jdbcTemplate.queryForObject(
                "select participant_id from ingredient_availability order by ingredient_concept_id limit 1", Long.class);
        OffsetDateTime availabilityBefore = jdbcTemplate.queryForObject(
                "select updated_at from ingredient_availability where ingredient_concept_id = ? and participant_id = ?",
                OffsetDateTime.class,
                availabilityConceptId,
                participantId);

        jdbcTemplate.update(
                "update ingredient_availability set availability_level = availability_level where ingredient_concept_id = ? and participant_id = ?",
                availabilityConceptId,
                participantId);

        OffsetDateTime availabilityAfter = jdbcTemplate.queryForObject(
                "select updated_at from ingredient_availability where ingredient_concept_id = ? and participant_id = ?",
                OffsetDateTime.class,
                availabilityConceptId,
                participantId);
        assertThat(availabilityAfter).isAfter(availabilityBefore);

        Long exclusionRuleId = jdbcTemplate.queryForObject("select id from exclusion_rule order by id limit 1", Long.class);
        OffsetDateTime exclusionBefore = jdbcTemplate.queryForObject(
                "select updated_at from exclusion_rule where id = ?", OffsetDateTime.class, exclusionRuleId);

        jdbcTemplate.update("update exclusion_rule set curator_note = ? where id = ?", "updated-at trigger test", exclusionRuleId);

        OffsetDateTime exclusionAfter = jdbcTemplate.queryForObject(
                "select updated_at from exclusion_rule where id = ?", OffsetDateTime.class, exclusionRuleId);
        assertThat(exclusionAfter).isAfter(exclusionBefore);
    }

    @Test
    void rerollAttemptRequiresAnInitialAttempt() {
        long session = insertReturningId("insert into challenge_session default values returning id");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into generation_attempt (challenge_session_id, attempt_type, generator_version) values (?, 'REROLL', 'test')",
                session))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("reroll attempt requires an initial attempt");

        insertAttempt(session, "INITIAL");
        insertAttempt(session, "REROLL");

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from generation_attempt where challenge_session_id = ?", Integer.class, session))
                .isEqualTo(2);
    }

    @Test
    void manualRequirementsMustBelongToTheCandidateAttempt() {
        long firstAttempt = insertAttempt(insertReturningId("insert into challenge_session default values returning id"), "INITIAL");
        long secondAttempt = insertAttempt(insertReturningId("insert into challenge_session default values returning id"), "INITIAL");
        long manualRequirement = insertReturningId(
                "insert into generation_manual_requirement (generation_attempt_id, position, display_text) values (?, 1, 'manual') returning id",
                firstAttempt);
        long candidate = insertCandidate(secondAttempt, false);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                insert into candidate_requirement
                    (candidate_id, position, source, manual_requirement_id, display_text_snapshot)
                values (?, 1, 'MANUAL', ?, 'manual')
                """,
                candidate, manualRequirement))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("does not belong to candidate generation attempt");
    }

    @Test
    void visibleChallengeRequiresItsSelectedCandidateAndExactlyFourRequirements() {
        long attempt = insertAttempt(insertReturningId("insert into challenge_session default values returning id"), "INITIAL");
        long candidate = insertCandidate(attempt, false);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)", attempt, candidate))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("is not marked as selected");

        jdbcTemplate.update("update challenge_candidate set is_selected = true where id = ?", candidate);
        insertRandomRequirements(candidate, 3);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)", attempt, candidate))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("must contain exactly four requirements");

        insertRandomRequirements(candidate, 1);
        jdbcTemplate.update("insert into challenge (generation_attempt_id, selected_candidate_id) values (?, ?)", attempt, candidate);

        assertThat(jdbcTemplate.queryForObject("select count(*) from challenge where generation_attempt_id = ?", Integer.class, attempt))
                .isEqualTo(1);
    }

    private void rerunLiquibase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(),
                    database
            );
            liquibase.update(new Contexts(), new LabelExpression());
        }
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
                insert into curation_round (generation_attempt_id, round_number, curator_model, prompt_version)
                values (?, 1, 'test', 'test')
                returning id
                """,
                attemptId
        );
        return insertReturningId(
                """
                insert into challenge_candidate (curation_round_id, candidate_number, is_selected)
                values (?, 1, ?)
                returning id
                """,
                round,
                selected
        );
    }

    private void insertRandomRequirements(long candidateId, int amount) {
        List<Long> conceptIds = jdbcTemplate.queryForList(
                "select id from ingredient_concept where active and random_draw_enabled order by id limit 4",
                Long.class
        );
        int existing = jdbcTemplate.queryForObject(
                "select count(*) from candidate_requirement where candidate_id = ?", Integer.class, candidateId);
        for (int offset = 0; offset < amount; offset++) {
            long conceptId = conceptIds.get(existing + offset);
            String displayName = jdbcTemplate.queryForObject(
                    "select display_name from ingredient_concept where id = ?", String.class, conceptId);
            String specificity = jdbcTemplate.queryForObject(
                    "select challenge_specificity from ingredient_concept where id = ?", String.class, conceptId);
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
        return jdbcTemplate.queryForObject("select count(*) from " + table + " where " + whereClause, Integer.class);
    }
}
