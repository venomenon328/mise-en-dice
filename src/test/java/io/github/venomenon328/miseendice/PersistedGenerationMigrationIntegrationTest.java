package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class PersistedGenerationMigrationIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generation_upgrade")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired DataSource dataSource;

    @Test
    void upgradesAndPreservesHistoricalCurationCandidateRequirementAndChallengeRows() throws Exception {
        String databaseName = "generation_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-persisted-generation.yaml");
            long session;
            long attempt;
            long round;
            long candidate;
            long legacyPendingAttempt;
            long legacyFailedAttempt;
            long legacySuccessWithoutBatch;
            try (Statement statement = connection.createStatement()) {
                session = returning(statement, "insert into challenge_session default values returning id");
                attempt = returning(statement, """
                        insert into generation_attempt (
                            challenge_session_id, attempt_type, status, generator_version, completed_at
                        ) values (%d, 'INITIAL', 'SUCCEEDED', 'legacy-generator', now()) returning id
                        """.formatted(session));
                round = returning(statement, """
                        insert into curation_round (
                            generation_attempt_id, round_number, curator_model, prompt_version,
                            status, completed_at
                        ) values (%d, 1, 'legacy-model', 'legacy-prompt', 'SELECTED', now()) returning id
                        """.formatted(attempt));
                candidate = returning(statement, """
                        insert into challenge_candidate (curation_round_id, candidate_number, is_selected)
                        values (%d, 1, true) returning id
                        """.formatted(round));
                statement.executeUpdate("""
                        insert into candidate_requirement (
                            candidate_id, position, source, ingredient_concept_id,
                            challenge_specificity_snapshot, display_text_snapshot
                        )
                        select %d, row_number() over (order by id), 'RANDOM', id,
                               challenge_specificity, display_name
                        from ingredient_concept order by id limit 4
                        """.formatted(candidate));
                statement.executeUpdate("""
                        insert into challenge (generation_attempt_id, selected_candidate_id)
                        values (%d, %d)
                        """.formatted(attempt, candidate));
                long pendingSession = returning(statement,
                        "insert into challenge_session default values returning id");
                legacyPendingAttempt = returning(statement, """
                        insert into generation_attempt (
                            challenge_session_id, attempt_type, status, generator_version
                        ) values (%d, 'INITIAL', 'PENDING', 'legacy-generator') returning id
                        """.formatted(pendingSession));
                long failedSession = returning(statement,
                        "insert into challenge_session default values returning id");
                legacyFailedAttempt = returning(statement, """
                        insert into generation_attempt (
                            challenge_session_id, attempt_type, status, generator_version, completed_at
                        ) values (%d, 'INITIAL', 'FAILED', 'legacy-generator', now()) returning id
                        """.formatted(failedSession));
                long emptySuccessSession = returning(statement,
                        "insert into challenge_session default values returning id");
                legacySuccessWithoutBatch = returning(statement, """
                        insert into generation_attempt (
                            challenge_session_id, attempt_type, status, generator_version, completed_at
                        ) values (%d, 'INITIAL', 'SUCCEEDED', 'legacy-generator', now()) returning id
                        """.formatted(emptySuccessSession));
            }

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(value(connection, "select status from generation_attempt where id = " + attempt))
                    .isEqualTo("GENERATED");
            assertThat(countWhere(connection, "curation_round", "id = " + round)).isEqualTo(1);
            assertThat(countWhere(connection, "challenge_candidate", "id = " + candidate)).isEqualTo(1);
            assertThat(countWhere(connection, "candidate_requirement", "candidate_id = " + candidate)).isEqualTo(4);
            assertThat(countWhere(connection, "candidate_requirement",
                    "candidate_id = " + candidate + " and concept_code_snapshot is not null")).isEqualTo(4);
            assertThat(countWhere(connection, "challenge", "generation_attempt_id = " + attempt)).isEqualTo(1);
            assertThat(countWhere(connection, "generation_batch",
                    "generation_attempt_id = " + attempt + " and batch_number = 1 and legacy_migrated"))
                    .isEqualTo(1);
            assertThat(value(connection,
                    "select generation_batch_id is not null from challenge_candidate where id = " + candidate))
                    .isEqualTo("t");
            assertThat(value(connection,
                    "select status || ':' || failure_reason_code from generation_attempt where id = "
                            + legacyPendingAttempt)).isEqualTo("FAILED:CONTEXT_SNAPSHOT_INVALID");
            assertThat(value(connection,
                    "select status || ':' || failure_reason_code from generation_attempt where id = "
                            + legacyFailedAttempt)).isEqualTo("FAILED:TECHNICAL_GENERATION_FAILURE");
            assertThat(value(connection,
                    "select status || ':' || failure_reason_code from generation_attempt where id = "
                            + legacySuccessWithoutBatch)).isEqualTo("FAILED:CONTEXT_SNAPSHOT_INVALID");

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(countWhere(connection, "databasechangelog", "true")).isEqualTo(26);
            assertThat(countWhere(connection, "generation_batch", "generation_attempt_id = " + attempt)).isEqualTo(1);
        }
    }

    @Test
    void upgradesTheImmediatelyPreviousMainAndKeepsLegacyCurationExplicit() throws Exception {
        String databaseName = "curation_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-curation.yaml");
            long session;
            long attempt;
            long round;
            try (Statement statement = connection.createStatement()) {
                session = returning(statement, "insert into challenge_session default values returning id");
                attempt = returning(statement, """
                        insert into generation_attempt (
                            challenge_session_id, attempt_type, status, generator_version, completed_at
                        ) values (%d, 'INITIAL', 'GENERATED', 'legacy-generator', now()) returning id
                        """.formatted(session));
                round = returning(statement, """
                        insert into curation_round (
                            generation_attempt_id, round_number, curator_model, prompt_version, status
                        ) values (%d, 1, 'legacy-model', 'legacy-prompt', 'PENDING') returning id
                        """.formatted(attempt));
            }

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(value(connection, "select requested_offer_count from challenge_session where id = " + session))
                    .isEqualTo("1");
            assertThat(value(connection, "select legacy_migrated from curation_round where id = " + round))
                    .isEqualTo("t");
            assertThat(value(connection, "select status from curation_round where id = " + round))
                    .isEqualTo("PENDING");
            assertThat(value(connection, "select curation_status from generation_attempt where id = " + attempt))
                    .isEqualTo("LEGACY");
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(countWhere(connection, "databasechangelog", "true")).isEqualTo(26);
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
    }

    private static long returning(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static int countWhere(Connection connection, String table, String where) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from " + table + " where " + where)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String value(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
