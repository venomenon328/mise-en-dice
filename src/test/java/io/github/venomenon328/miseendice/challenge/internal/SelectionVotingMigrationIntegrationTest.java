package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class SelectionVotingMigrationIntegrationTest {

    @Test
    void upgradesThePhase11AStateAndTheSecondLiquibaseRunIsANoOp() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("selection_upgrade");
                Connection connection = database.openConnection()) {
            runLiquibase(connection, "db/changelog/db.changelog-before-selection-voting.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(28);
            assertThat(regclass(connection, "selection_voting_round")).isNull();

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(regclass(connection, "selection_voting_round")).isEqualTo("selection_voting_round");
            assertThat(regclass(connection, "challenge_participation")).isEqualTo("challenge_participation");

            int changesetCount = count(connection, "databasechangelog");
            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");
            assertThat(count(connection, "databasechangelog")).isEqualTo(changesetCount);
        }
    }

    @Test
    void upgradesExistingSelectionSnapshotsWithoutChangingTheirMembers() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("participant_electorate_upgrade");
                Connection connection = database.openConnection()) {
            runLiquibase(connection, "db/changelog/db.changelog-before-challenge-archive.yaml");
            long sessionId = scalarLong(connection, "insert into challenge_session default values returning id");
            long georgiaId = scalarLong(connection, "select id from participant where code = 'GEORGIA'");
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("insert into selection_electorate (challenge_session_id, participant_id) values ("
                        + sessionId + ", " + georgiaId + ")");
            }

            runLiquibase(connection, "db/changelog/db.changelog-master.yaml");

            assertThat(scalarLong(connection, "select count(*) from default_electorate_member")).isEqualTo(2);
            assertThat(scalarLong(connection, "select count(*) from challenge_session "
                    + "where id = " + sessionId + " and selection_electorate_materialized_at is not null")).isOne();
            assertThat(scalarLong(connection, "select count(*) from selection_electorate where challenge_session_id = "
                    + sessionId)).isOne();
            assertThatThrownBy(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("insert into selection_electorate (challenge_session_id, participant_id) values ("
                            + sessionId + ", " + scalarLong(connection, "select id from participant where code = 'TOBIAS'") + ")");
                }
            }).isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("selection electorate snapshots are immutable after materialization");
        }
    }

    private static void runLiquibase(Connection connection, String changelog) throws Exception {
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(changelog, new ClassLoaderResourceAccessor(), database);
        liquibase.update(new Contexts(), new LabelExpression());
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                "select count(*) from " + table)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static long scalarLong(Connection connection, String query) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query)) {
            result.next();
            return result.getLong(1);
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
}
