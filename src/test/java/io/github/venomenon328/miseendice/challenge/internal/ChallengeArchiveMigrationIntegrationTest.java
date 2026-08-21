package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Verifies the forward-only upgrade of confirmed pre-archive challenges with real PostgreSQL. */
@Testcontainers
class ChallengeArchiveMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_challenge_archive_migration")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @Test
    void backfillsExistingChallengesByShownAtThenIdAndInitializesTheTransactionalCounter() throws Exception {
        String databaseName = "challenge_archive_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        String upgradeUrl = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(upgradeUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-challenge-archive.yaml");
            long later = insertPreArchiveChallenge(connection, OffsetDateTime.parse("2026-08-21T11:30:00+02:00"));
            long sameTime = insertPreArchiveChallenge(connection, OffsetDateTime.parse("2026-08-21T11:30:00+02:00"));
            long earlier = insertPreArchiveChallenge(connection, OffsetDateTime.parse("2026-08-20T11:30:00+02:00"));
            if (!connection.getAutoCommit()) {
                connection.commit();
            }

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(challengeNumber(connection, earlier)).isEqualTo(1L);
            assertThat(challengeNumber(connection, later)).isEqualTo(2L);
            assertThat(challengeNumber(connection, sameTime)).isEqualTo(3L);
            assertThat(longValue(connection, "select last_challenge_number from challenge_archive_counter"))
                    .isEqualTo(3L);
            assertThat(regclass(connection, "challenge_card")).isEqualTo("challenge_card");
            Savepoint beforeImmutableNumberCheck = connection.setSavepoint();
            try {
                assertThatThrownBy(() -> execute(connection,
                        "update challenge set challenge_number = 9 where id = " + earlier))
                                .hasMessageContaining("challenge number is immutable");
            } finally {
                connection.rollback(beforeImmutableNumberCheck);
            }

            int changesetCount = count(connection, "databasechangelog");
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(changesetCount);
        }
    }

    private static long insertPreArchiveChallenge(Connection connection, OffsetDateTime shownAt) throws Exception {
        long sessionId = insertReturningId(connection, "insert into challenge_session default values returning id");
        long attemptId = insertReturningId(connection, """
                insert into generation_attempt (challenge_session_id, attempt_type, generator_version)
                values (?, 'INITIAL', 'test') returning id
                """, sessionId);
        long roundId = insertReturningId(connection, """
                insert into curation_round (
                    generation_attempt_id, round_number, curator_model, prompt_version, status, completed_at, legacy_migrated
                ) values (?, 1, 'test', 'test', 'SELECTED', now(), true) returning id
                """, attemptId);
        long batchId = insertReturningId(connection, """
                insert into generation_batch (generation_attempt_id, batch_number, status, legacy_migrated)
                values (?, 1, 'GENERATED', true) returning id
                """, attemptId);
        long candidateId = insertReturningId(connection, """
                insert into challenge_candidate (generation_batch_id, curation_round_id, candidate_number, is_selected)
                values (?, ?, 1, true) returning id
                """, batchId, roundId);

        for (ConceptSnapshot concept : fourConcepts(connection)) {
            execute(connection, """
                    insert into candidate_requirement (
                        candidate_id, position, source, ingredient_concept_id, concept_code_snapshot,
                        display_text_snapshot, challenge_specificity_snapshot
                    ) values (?, ?, 'RANDOM', ?, ?, ?, ?)
                    """, candidateId, concept.position(), concept.id(), concept.code(), concept.displayName(),
                    concept.specificity());
        }

        execute(connection, "alter table challenge disable trigger all");
        try {
            return insertReturningId(connection, """
                    insert into challenge (generation_attempt_id, selected_candidate_id, legacy_pre_offer_decision, shown_at)
                    values (?, ?, true, ?) returning id
                    """, attemptId, candidateId, shownAt);
        } finally {
            execute(connection, "alter table challenge enable trigger all");
        }
    }

    private static List<ConceptSnapshot> fourConcepts(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("""
                select id, code, display_name, challenge_specificity
                from ingredient_concept where active and random_draw_enabled
                order by id limit 4
                """)) {
            List<ConceptSnapshot> concepts = new ArrayList<>();
            int position = 1;
            while (result.next()) {
                concepts.add(new ConceptSnapshot(position++, result.getLong("id"), result.getString("code"),
                        result.getString("display_name"), result.getString("challenge_specificity")));
            }
            if (concepts.size() != 4) {
                throw new IllegalStateException("The seeded catalog must provide four random concepts");
            }
            return concepts;
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private static long insertReturningId(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private static void execute(Connection connection, String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            statement.executeUpdate();
        }
    }

    private static void bind(PreparedStatement statement, Object... values) throws Exception {
        for (int index = 0; index < values.length; index++) {
            statement.setObject(index + 1, values[index]);
        }
    }

    private static long challengeNumber(Connection connection, long challengeId) throws Exception {
        return longValue(connection, "select challenge_number from challenge where id = " + challengeId);
    }

    private static long longValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "select count(*) from " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String regclass(Connection connection, String table) throws Exception {
        try (var statement = connection.prepareStatement("select to_regclass(?)")) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1);
            }
        }
    }

    private record ConceptSnapshot(int position, long id, String code, String displayName, String specificity) {
    }
}
