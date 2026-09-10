package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("migration")
class CatalogAuditCleanupMigrationIntegrationTest {

    @Test
    void upgradesTheImmediatelyPreviousMainAndRestartsAfterCatalogAuditCleanup() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("catalog_audit_cleanup");
                Connection connection = database.openConnection()) {
            runLiquibase(connection, "db/changelog/db.changelog-before-catalog-audit-cleanup.yaml");

            assertThat(tableExists(connection, "catalog_audit_entry")).isTrue();
            assertThat(count(connection, "catalog_audit_entry")).isPositive();
            int ingredientCount = count(connection, "ingredient_concept");
            int exclusionCount = count(connection, "exclusion_rule");
            int exclusionVersionSum = integerValue(connection,
                    "select coalesce(sum(version), 0) from exclusion_rule");

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(tableExists(connection, "catalog_audit_entry")).isFalse();
            assertThat(count(connection, "ingredient_concept")).isEqualTo(ingredientCount);
            assertThat(count(connection, "exclusion_rule")).isEqualTo(exclusionCount);
            assertThat(integerValue(connection, "select coalesce(sum(version), 0) from exclusion_rule"))
                    .isEqualTo(exclusionVersionSum);
            assertThat(countWhere(connection, "databasechangelog", "id = '022-remove-runtime-catalog-audit'"))
                    .isOne();
            assertThat(countWhere(connection, "databasechangelog",
                    "id = '038-availability-note-sentence-capitalization'"))
                    .isOne();

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(tableExists(connection, "catalog_audit_entry")).isFalse();
            assertThat(countWhere(connection, "databasechangelog", "id = '022-remove-runtime-catalog-audit'"))
                    .isOne();
            assertThat(countWhere(connection, "databasechangelog",
                    "id = '038-availability-note-sentence-capitalization'"))
                    .isOne();
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
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
                        "select count(*) from " + table + " where " + whereClause)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static boolean tableExists(Connection connection, String table) throws Exception {
        try (var statement = connection.prepareStatement("""
                select exists (
                    select 1 from information_schema.tables
                    where table_schema = 'public' and table_name = ?
                )
                """)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static int integerValue(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }
}
