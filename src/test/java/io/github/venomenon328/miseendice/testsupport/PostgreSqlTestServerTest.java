package io.github.venomenon328.miseendice.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.Test;

class PostgreSqlTestServerTest {

    @Test
    void temporaryDatabasesAreIsolatedAndDroppedEvenWithAnOpenClientConnection() throws Exception {
        String firstName;
        String secondName;
        Connection forcedConnection;
        try (var first = PostgreSqlTestServer.createTemporaryDatabase("isolation");
                var second = PostgreSqlTestServer.createTemporaryDatabase("isolation")) {
            firstName = first.name();
            secondName = second.name();
            assertThat(firstName).isNotEqualTo(secondName);

            forcedConnection = first.openConnection();
            try (Statement statement = forcedConnection.createStatement()) {
                statement.execute("create table isolated_value (value integer not null)");
                statement.execute("insert into isolated_value values (42)");
            }
            try (Connection secondConnection = second.openConnection(); Statement statement = secondConnection.createStatement()) {
                try (var result = statement.executeQuery("select to_regclass('isolated_value')")) {
                    result.next();
                    assertThat(result.getString(1)).isNull();
                }
            }
        }

        assertThat(PostgreSqlTestServer.databaseExists(firstName)).isFalse();
        assertThat(PostgreSqlTestServer.databaseExists(secondName)).isFalse();
        assertThatThrownBy(() -> forcedConnection.createStatement().execute("select 1"))
                .isInstanceOf(java.sql.SQLException.class);
        forcedConnection.close();
    }
}
