package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

class GeneratorCompatibilityCleanupMigrationIntegrationTest {

    @Test
    void removesObsoleteColumnsForwardOnlyWithoutTouchingCatalogData() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("generator_cleanup");
                Connection connection = database.openConnection()) {
            runLiquibase(connection, "db/changelog/db.changelog-before-generator-compatibility-cleanup.yaml");
            assertThat(columnExists(connection, "generation_attempt", "exclusion_rule_id")).isTrue();
            assertThat(columnExists(connection, "challenge_candidate", "exclusion_rule_id")).isTrue();

            long catalogConceptId = longValue(connection,
                    "select id from ingredient_concept order by id limit 1");
            String catalogDisplayName = stringValue(connection,
                    "select display_name from ingredient_concept where id = " + catalogConceptId);
            long catalogVersion = longValue(connection,
                    "select version from ingredient_concept where id = " + catalogConceptId);
            long exclusionRuleId = longValue(connection,
                    "select id from exclusion_rule order by id limit 1");
            String exclusionDisplayText = stringValue(connection,
                    "select display_text from exclusion_rule where id = " + exclusionRuleId);
            runLiquibase(connection, "db/changelog/schema/012-remove-legacy-generator-compatibility.sql");

            assertThat(columnExists(connection, "generation_attempt", "exclusion_rule_id")).isFalse();
            assertThat(columnExists(connection, "challenge_candidate", "exclusion_rule_id")).isFalse();
            // Assert this migration's preservation contract before later catalog revisions advance versions.
            assertThat(stringValue(connection,
                    "select display_name from ingredient_concept where id = " + catalogConceptId))
                    .isEqualTo(catalogDisplayName);
            assertThat(longValue(connection,
                    "select version from ingredient_concept where id = " + catalogConceptId))
                    .isEqualTo(catalogVersion);
            assertThat(stringValue(connection,
                    "select display_text from exclusion_rule where id = " + exclusionRuleId))
                    .isEqualTo(exclusionDisplayText);
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
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

    private static long longValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
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
