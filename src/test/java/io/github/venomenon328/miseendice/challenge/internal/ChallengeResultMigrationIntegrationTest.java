package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.UUID;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Proves that the append-only result migration upgrades a real PostgreSQL 014 database without changing Challenge numbers. */
@Testcontainers
class ChallengeResultMigrationIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_result_migration")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @Test
    void upgradesCompletedChallengesWithEmptyNewResultTablesAndIsRestartSafe() throws Exception {
        String databaseName = "challenge_results_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-challenge-results.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(34);
            OffsetDateTime shownAt = OffsetDateTime.parse("2026-08-20T13:45:00Z");
            try (Statement statement = connection.createStatement()) {
                // The legacy challenge must retain a valid FK graph: migration 015 updates it and PostgreSQL then
                // evaluates its referencing constraints before the subsequent ALTER TABLE.
                statement.execute("set session_replication_role = replica");
                statement.executeUpdate("insert into challenge_session (id) values (999990)");
                statement.executeUpdate("""
                        insert into generation_attempt (id, challenge_session_id, attempt_type, status, generator_version)
                        values (999992, 999990, 'INITIAL', 'PENDING', 'migration-test')
                        """);
                statement.executeUpdate("""
                        insert into generation_batch (id, generation_attempt_id, batch_number, status, legacy_migrated)
                        values (999994, 999992, 1, 'GENERATED', true)
                        """);
                statement.executeUpdate("""
                        insert into challenge_candidate (id, generation_batch_id, candidate_number)
                        values (999993, 999994, 1)
                        """);
                statement.executeUpdate("""
                        insert into challenge (id, generation_attempt_id, selected_candidate_id, status, shown_at,
                                               challenge_number, legacy_pre_offer_decision)
                        values (999991, 999992, 999993, 'COMPLETED', timestamptz '2026-08-20 13:45:00+00',
                                42, true)
                        """);
                statement.execute("set session_replication_role = origin");
            }
            // Liquibase leaves a JDBC transaction open for some driver/database combinations. Commit the legacy
            // fixture before the next changeset alters its table; PostgreSQL rejects DDL with queued FK events.
            connection.setAutoCommit(true);

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(36);
            assertThat(value(connection, "select challenge_number from challenge where id = 999991", Long.class))
                    .isEqualTo(42L);
            assertThat(value(connection, "select completed_at from challenge where id = 999991", OffsetDateTime.class))
                    .isEqualTo(shownAt);
            assertThat(count(connection, "challenge_result")).isZero();
            assertThat(count(connection, "challenge_result_ingredient")).isZero();
            assertThat(count(connection, "challenge_result_photo")).isZero();
            assertThat(count(connection, "challenge_result_concretization")).isZero();

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(36);
            assertThat(value(connection, "select completed_at from challenge where id = 999991", OffsetDateTime.class))
                    .isEqualTo(shownAt);
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
        if (!connection.getAutoCommit()) {
            connection.commit();
            connection.setAutoCommit(true);
        }
    }

    private static int count(Connection connection, String table) throws Exception {
        return value(connection, "select count(*) from " + table, Integer.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(Connection connection, String query, Class<T> type) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            result.next();
            if (type == OffsetDateTime.class) {
                return (T) result.getObject(1, OffsetDateTime.class);
            }
            Object raw = result.getObject(1);
            if (type == Integer.class && raw instanceof Number number) {
                return (T) Integer.valueOf(number.intValue());
            }
            if (type == Long.class && raw instanceof Number number) {
                return (T) Long.valueOf(number.longValue());
            }
            return type.cast(raw);
        }
    }
}
