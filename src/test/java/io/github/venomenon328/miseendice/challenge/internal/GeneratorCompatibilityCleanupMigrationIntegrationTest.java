package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
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

@Testcontainers
class GeneratorCompatibilityCleanupMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_cleanup_migration")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @Test
    void removesObsoleteColumnsForwardOnlyAndRetainsArchivedCurationRows() throws Exception {
        String databaseName = "generator_cleanup_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement()) {
            statement.execute("create database " + databaseName);
        }
        String upgradeUrl = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
        try (Connection connection = DriverManager.getConnection(upgradeUrl, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            runLiquibase(connection, "db/changelog/db.changelog-before-generator-compatibility-cleanup.yaml");
            assertThat(columnExists(connection, "generation_attempt", "exclusion_rule_id")).isTrue();
            assertThat(columnExists(connection, "challenge_candidate", "exclusion_rule_id")).isTrue();
            execute(connection, "alter table generation_batch disable trigger trg_generation_batch_complete");
            execute(connection, "alter table curation_round disable trigger trg_curation_round_request_shape");

            long sessionId = insertAndReturnId(connection, "insert into challenge_session default values returning id");
            long attemptId = insertAndReturnId(connection, """
                    insert into generation_attempt (
                        challenge_session_id, attempt_type, status, generator_version, completed_at,
                        effective_date, season_month, attempt_seed, rng_algorithm, configuration_version,
                        canonical_payload_version
                    ) values (%d, 'INITIAL', 'GENERATED', '1.1.0', now(),
                        date '2026-08-18', 8, 17, 'SPLITMIX64_V1', 'legacy', 1)
                    returning id
                    """.formatted(sessionId));
            long batchId = insertAndReturnId(connection, """
                    insert into generation_batch (
                        generation_attempt_id, batch_number, batch_seed, status, fallback_level,
                        set_evaluation, result_snapshot, set_fingerprint
                    ) values (%d, 1, 19, 'GENERATED', 'STRICT', '{}'::jsonb, '{}'::jsonb,
                        repeat('a', 64))
                    returning id
                    """.formatted(attemptId));
            long roundId = insertAndReturnId(connection, """
                    insert into curation_round (
                        generation_attempt_id, round_number, curator_model, prompt_version, status,
                        request_payload, primary_generation_batch_id, request_purpose, contract_version,
                        open_offer_slots
                    ) values (%d, 1, 'legacy-curator', 'CURATOR_PROMPT_V1', 'PENDING', '{}'::jsonb,
                        %d, 'INITIAL_PASS', 'CURATION_CONTRACT_V1', 1)
                    returning id
            """.formatted(attemptId, batchId));
            execute(connection, "alter table generation_batch enable trigger trg_generation_batch_complete");

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            execute(connection, "alter table curation_round enable trigger trg_curation_round_request_shape");

            assertThat(columnExists(connection, "generation_attempt", "exclusion_rule_id")).isFalse();
            assertThat(columnExists(connection, "challenge_candidate", "exclusion_rule_id")).isFalse();
            assertThat(booleanValue(connection, "select legacy_migrated from curation_round where id = " + roundId))
                    .isTrue();
            assertThat(stringValue(connection, "select contract_version from curation_round where id = " + roundId))
                    .isEqualTo("CURATION_CONTRACT_V1");

            int changesetCount = count(connection, "databasechangelog");
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(changesetCount);
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private static long insertAndReturnId(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private static void execute(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        return booleanValue(connection, """
                select exists (
                    select 1 from information_schema.columns
                    where table_schema = 'public' and table_name = '%s' and column_name = '%s'
                )
                """.formatted(table, column));
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "select count(*) from " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static boolean booleanValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getBoolean(1);
        }
    }

    private static String stringValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getString(1);
        }
    }
}
