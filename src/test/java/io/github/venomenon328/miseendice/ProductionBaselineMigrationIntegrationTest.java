package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("migration")
class ProductionBaselineMigrationIntegrationTest {

    private static final String PRODUCTION_BASELINE =
            "db/changelog/db.changelog-production-baseline.yaml";
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final int PRODUCTION_BASELINE_CHANGESET_COUNT = 51;
    private static final List<String> POST_PRODUCTION_CHANGESETS = List.of(
            "031-vietnam-curation",
            "032-thailand-curation",
            "018-five-level-availability",
            "019-availability-curator-note",
            "033-availability-novelty-final-review",
            "020-remove-generator-replay-result",
            "034-austria-curation",
            "021-culinary-country-subdivision-codes",
            "003-england-culinary-country",
            "035-england-curation",
            "036-availability-note-prefix-cleanup",
            "037-availability-note-consolidation",
            "022-remove-runtime-catalog-audit",
            "038-availability-note-sentence-capitalization",
            "039-availability-r3-corrections",
            "004-scotland-culinary-country",
            "040-scotland-curation"
    );

    @Test
    void upgradesTheConfirmedProductionCutoffToMasterAndTheSecondRunIsANoOp() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("production_baseline");
                Connection connection = database.openConnection()) {
            runLiquibase(connection, PRODUCTION_BASELINE);

            assertThat(changesetIds(connection)).hasSize(PRODUCTION_BASELINE_CHANGESET_COUNT);
            assertThat(lastChangesetId(connection)).isEqualTo("030-veal-concept-expansion");
            assertThat(changesetIds(connection)).doesNotContainAnyElementsOf(POST_PRODUCTION_CHANGESETS);

            runLiquibase(connection, MASTER);

            assertThat(changesetIdsAfter(connection, PRODUCTION_BASELINE_CHANGESET_COUNT))
                    .containsExactlyElementsOf(POST_PRODUCTION_CHANGESETS);
            assertThat(changesetIds(connection))
                    .hasSize(PRODUCTION_BASELINE_CHANGESET_COUNT + POST_PRODUCTION_CHANGESETS.size());
            assertThat(tableExists(connection, "catalog_audit_entry")).isFalse();
            assertThat(columnExists(connection, "generation_batch", "result_snapshot")).isFalse();
            assertThat(columnExists(connection, "ingredient_availability", "curator_note")).isTrue();

            runLiquibase(connection, MASTER);
            assertThat(changesetIds(connection))
                    .hasSize(PRODUCTION_BASELINE_CHANGESET_COUNT + POST_PRODUCTION_CHANGESETS.size());
            assertThat(changesetIdsAfter(connection, PRODUCTION_BASELINE_CHANGESET_COUNT))
                    .containsExactlyElementsOf(POST_PRODUCTION_CHANGESETS);
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
    }

    private static List<String> changesetIds(Connection connection) throws Exception {
        return stringValues(connection, "select id from databasechangelog order by orderexecuted");
    }

    private static List<String> changesetIdsAfter(Connection connection, int orderExecuted) throws Exception {
        return stringValues(connection, "select id from databasechangelog where orderexecuted > "
                + orderExecuted + " order by orderexecuted");
    }

    private static String lastChangesetId(Connection connection) throws Exception {
        return stringValues(connection,
                "select id from databasechangelog order by orderexecuted desc limit 1").getFirst();
    }

    private static List<String> stringValues(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            var values = new java.util.ArrayList<String>();
            while (result.next()) {
                values.add(result.getString(1));
            }
            return values;
        }
    }

    private static boolean tableExists(Connection connection, String table) throws Exception {
        return schemaObjectExists(connection, "information_schema.tables", "table_name", table);
    }

    private static boolean columnExists(Connection connection, String table, String column) throws Exception {
        try (var statement = connection.prepareStatement("""
                select exists (
                    select 1 from information_schema.columns
                    where table_schema = 'public' and table_name = ? and column_name = ?
                )
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }

    private static boolean schemaObjectExists(
            Connection connection, String informationSchemaTable, String nameColumn, String name) throws Exception {
        String sql = "select exists (select 1 from " + informationSchemaTable
                + " where table_schema = 'public' and " + nameColumn + " = ?)";
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        }
    }
}
