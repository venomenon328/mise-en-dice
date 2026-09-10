package io.github.venomenon328.miseendice.testsupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One PostgreSQL 17 server per Surefire fork, with logical databases providing the required isolation.
 *
 * <p>The container is deliberately not managed by the JUnit extension: Testcontainers/Ryuk owns its process
 * lifetime, while this singleton keeps it alive for the complete test JVM instead of stopping it after each class.
 */
public final class PostgreSqlTestServer {

    private static final String CONTROL_DATABASE = "postgres";
    private static final String CURRENT_SCHEMA_DATABASE = "mise_en_dice_current";
    private static final AtomicLong DATABASE_SEQUENCE = new AtomicLong();
    private static final PostgreSQLContainer SERVER = startServer();
    private static boolean currentSchemaDatabaseCreated;

    private PostgreSqlTestServer() {
    }

    public static synchronized String currentSchemaJdbcUrl() {
        if (!currentSchemaDatabaseCreated) {
            createDatabase(CURRENT_SCHEMA_DATABASE);
            currentSchemaDatabaseCreated = true;
        }
        return jdbcUrl(CURRENT_SCHEMA_DATABASE);
    }

    public static String username() {
        return SERVER.getUsername();
    }

    public static String password() {
        return SERVER.getPassword();
    }

    public static TemporaryDatabase createTemporaryDatabase(String purpose) {
        String normalizedPurpose = purpose.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalizedPurpose.isBlank()) {
            normalizedPurpose = "test";
        }
        if (normalizedPurpose.length() > 30) {
            normalizedPurpose = normalizedPurpose.substring(0, 30);
        }
        String name = "test_" + normalizedPurpose + "_" + DATABASE_SEQUENCE.incrementAndGet();
        createDatabase(name);
        return new TemporaryDatabase(name, jdbcUrl(name));
    }

    static boolean databaseExists(String name) {
        try (Connection connection = controlConnection();
                var statement = connection.prepareStatement("select exists (select from pg_database where datname = ?)")
        ) {
            statement.setString(1, name);
            try (var result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect PostgreSQL test database " + name, exception);
        }
    }

    private static PostgreSQLContainer startServer() {
        PostgreSQLContainer server = new PostgreSQLContainer("postgres:17.6")
                .withDatabaseName(CONTROL_DATABASE)
                .withUsername("mise_en_dice")
                .withPassword("mise_en_dice")
                .withCommand("postgres", "-c", "max_connections=200");
        server.start();
        return server;
    }

    private static void createDatabase(String name) {
        try (Connection connection = controlConnection(); Statement statement = connection.createStatement()) {
            statement.execute("create database " + quotedIdentifier(name));
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create PostgreSQL test database " + name, exception);
        }
    }

    private static void dropDatabase(String name) {
        try (Connection connection = controlConnection(); Statement statement = connection.createStatement()) {
            statement.execute("drop database if exists " + quotedIdentifier(name) + " with (force)");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not drop PostgreSQL test database " + name, exception);
        }
    }

    private static Connection controlConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(CONTROL_DATABASE), username(), password());
    }

    private static String jdbcUrl(String databaseName) {
        return SERVER.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + databaseName);
    }

    private static String quotedIdentifier(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    public static final class TemporaryDatabase implements AutoCloseable {

        private final String name;
        private final String jdbcUrl;
        private final AtomicBoolean closed = new AtomicBoolean();

        private TemporaryDatabase(String name, String jdbcUrl) {
            this.name = name;
            this.jdbcUrl = jdbcUrl;
        }

        public String name() {
            return name;
        }

        public String jdbcUrl() {
            return jdbcUrl;
        }

        public Connection openConnection() throws SQLException {
            if (closed.get()) {
                throw new IllegalStateException("PostgreSQL test database is already closed: " + name);
            }
            return DriverManager.getConnection(jdbcUrl, username(), password());
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                dropDatabase(name);
            }
        }
    }
}
