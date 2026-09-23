package io.github.venomenon328.miseendice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@Tag("migration")
class AvailabilityNoveltyProductionReconciliationIntegrationTest {

    private static final String PRODUCTION_BASELINE =
            "db/changelog/db.changelog-production-baseline.yaml";
    private static final String BEFORE_033 =
            "db/changelog/db.changelog-before-availability-novelty.yaml";
    private static final String CHANGESET_019 =
            "db/changelog/schema/019-availability-curator-note.sql";
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final Path RECONCILIATION_DIRECTORY = Path.of("deploy/reconciliation");
    private static final Path STATE_SQL =
            RECONCILIATION_DIRECTORY.resolve("291-availability-novelty-state.sql");
    private static final Path SETUP_SQL =
            RECONCILIATION_DIRECTORY.resolve("291-availability-novelty-setup.sql");
    private static final Path APPLY_SQL =
            RECONCILIATION_DIRECTORY.resolve("291-availability-novelty-apply.sql");
    private static final Path MANIFEST = Path.of(
            "docs/analysis/availability-novelty-final-review-v1-20260907.tsv");

    @Test
    void reconcilesTheProductionDriftPathAndThenRunsTheNormalMaster() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("issue291_reconcile");
                Connection connection = database.openConnection()) {
            migrateToImmediatePredecessor(connection);
            var jdbc = jdbc(connection);
            assertThat(reconciliationState(connection)).isEqualTo("PENDING");

            jdbc.update("""
                    update ingredient_concept
                    set curator_note = 'Issue 291 preserved operational field.'
                    where code = 'CHAMPIGNON'
                    """);
            jdbc.update("""
                    insert into ingredient_concept (
                        code, display_name, active, random_draw_enabled, challenge_specificity, curator_note
                    ) values (
                        'ISSUE_291_EXTRA', 'Issue 291 extra concept', false, false, 'SPECIFIC',
                        'Additional operational data outside the historical review.'
                    )
                    """);
            jdbc.update("insert into participant (code, display_name) values ('ISSUE_291_OTHER', 'Other participant')");
            jdbc.update("""
                    insert into ingredient_availability (
                        ingredient_concept_id, participant_id, availability_level, curator_note
                    )
                    select concept.id, participant_row.id, 'SPECIALTY', 'Preserve this participant value.'
                    from ingredient_concept concept cross join participant participant_row
                    where concept.code = 'CHAMPIGNON' and participant_row.code = 'ISSUE_291_OTHER'
                    """);
            jdbc.update("update ingredient_concept set novelty_level = 5 where code = 'CHAMPIGNON'");
            jdbc.update("""
                    update ingredient_availability availability
                    set availability_level = 'DIFFICULT', curator_note = 'Drift inside the 033 write scope.'
                    from ingredient_concept concept, participant participant_row
                    where availability.ingredient_concept_id = concept.id
                      and availability.participant_id = participant_row.id
                      and concept.code = 'CHAMPIGNON'
                      and participant_row.code in ('GEORGIA', 'TOBIAS')
                    """);

            long versionBefore = jdbc.queryForObject(
                    "select version from ingredient_concept where code = 'CHAMPIGNON'", Long.class);
            applyReconciliation(connection);

            assertThat(reconciliationState(connection)).isEqualTo("APPLIED");
            assertThat(jdbc.queryForObject(
                    "select novelty_level from ingredient_concept where code = 'CHAMPIGNON'", Integer.class))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "select version from ingredient_concept where code = 'CHAMPIGNON'", Long.class))
                    .isEqualTo(versionBefore + 1);
            assertThat(jdbc.queryForList("""
                    select participant_row.code, availability.availability_level, availability.curator_note
                    from ingredient_availability availability
                    join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                    join participant participant_row on participant_row.id = availability.participant_id
                    where concept.code = 'CHAMPIGNON' and participant_row.code in ('GEORGIA', 'TOBIAS')
                    order by participant_row.code
                    """))
                    .extracting(row -> List.of(row.get("code"), row.get("availability_level")))
                    .containsExactly(List.of("GEORGIA", "EASY"), List.of("TOBIAS", "EASY"));
            assertThat(jdbc.queryForObject("""
                    select curator_note from ingredient_concept where code = 'CHAMPIGNON'
                    """, String.class)).isEqualTo("Issue 291 preserved operational field.");
            assertThat(jdbc.queryForObject("""
                    select availability.curator_note
                    from ingredient_availability availability
                    join ingredient_concept concept on concept.id = availability.ingredient_concept_id
                    join participant participant_row on participant_row.id = availability.participant_id
                    where concept.code = 'CHAMPIGNON' and participant_row.code = 'ISSUE_291_OTHER'
                    """, String.class)).isEqualTo("Preserve this participant value.");
            assertThat(jdbc.queryForObject(
                    "select count(*) from ingredient_concept where code = 'ISSUE_291_EXTRA'", Integer.class))
                    .isOne();
            assertThat(jdbc.queryForMap("""
                    select exectype, md5sum from databasechangelog
                    where id = '033-availability-novelty-final-review'
                    """))
                    .containsEntry("exectype", "MARK_RAN")
                    .containsEntry("md5sum", null);

            String conceptsAfterFirstRun = jsonAggregate(jdbc, "ingredient_concept");
            String availabilityAfterFirstRun = jsonAggregate(jdbc, "ingredient_availability");
            reconcileIfPending(connection);
            assertThat(jsonAggregate(jdbc, "ingredient_concept")).isEqualTo(conceptsAfterFirstRun);
            assertThat(jsonAggregate(jdbc, "ingredient_availability")).isEqualTo(availabilityAfterFirstRun);

            migrate(connection, MASTER);

            assertThat(reconciliationState(connection)).isEqualTo("APPLIED");
            assertThat(jdbc.queryForObject("""
                    select count(*) from databasechangelog
                    where id = '033-availability-novelty-final-review'
                    """, Integer.class)).isOne();
            assertThat(jdbc.queryForObject("""
                    select md5sum from databasechangelog
                    where id = '033-availability-novelty-final-review'
                    """, String.class)).isNotBlank();
            assertThat(jdbc.queryForObject("""
                    select count(*) from databasechangelog
                    where id = '047-catalog-draw-weight-calibration'
                    """, Integer.class)).isOne();
            assertThat(jdbc.queryForObject("""
                    select curator_note from ingredient_concept where code = 'CHAMPIGNON'
                    """, String.class)).isEqualTo("Issue 291 preserved operational field.");
            assertThat(jdbc.queryForObject(
                    "select count(*) from ingredient_concept where code = 'ISSUE_291_EXTRA'", Integer.class))
                    .isOne();

            migrate(connection, MASTER);
            assertThat(jdbc.queryForObject("""
                    select count(*) from databasechangelog
                    where id = '033-availability-novelty-final-review'
                    """, Integer.class)).isOne();
        }
    }

    @Test
    void missingReviewConceptAbortsBeforeAnyPersistentReconciliationWrite() throws Exception {
        assertMissingDependencyAbortsBeforeWrite("""
                update ingredient_concept set code = 'ISSUE_291_RENAMED' where code = 'CHAMPIGNON'
                """, "required review concepts are missing: CHAMPIGNON");
    }

    @Test
    void missingParticipantAbortsBeforeAnyPersistentReconciliationWrite() throws Exception {
        assertMissingDependencyAbortsBeforeWrite("""
                alter table participant disable trigger trg_participant_code_immutable;
                update participant set code = 'ISSUE_291_MISSING_TOBIAS' where code = 'TOBIAS';
                alter table participant enable trigger trg_participant_code_immutable
                """, "required participants are missing: TOBIAS");
    }

    @Test
    void aLaterChangesetMakesTheReconciliationStateInvalid() throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("issue291_later_state");
                Connection connection = database.openConnection()) {
            migrateToImmediatePredecessor(connection);
            migrate(connection, "db/changelog/schema/020-remove-generator-replay-result.sql");
            assertThat(reconciliationState(connection)).isEqualTo("INVALID");
        }
    }

    private static void assertMissingDependencyAbortsBeforeWrite(String mutation, String message) throws Exception {
        try (var database = PostgreSqlTestServer.createTemporaryDatabase("issue291_missing");
                Connection connection = database.openConnection()) {
            migrateToImmediatePredecessor(connection);
            var jdbc = jdbc(connection);
            jdbc.execute(mutation);
            String conceptsBefore = jsonAggregate(jdbc, "ingredient_concept");
            String availabilityBefore = jsonAggregate(jdbc, "ingredient_availability");

            assertThatThrownBy(() -> applyReconciliation(connection))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining(message);

            if (!connection.getAutoCommit()) {
                connection.rollback();
            } else {
                try (var rollback = connection.createStatement()) {
                    rollback.execute("rollback");
                }
            }
            assertThat(jsonAggregate(jdbc, "ingredient_concept")).isEqualTo(conceptsBefore);
            assertThat(jsonAggregate(jdbc, "ingredient_availability")).isEqualTo(availabilityBefore);
            assertThat(jdbc.queryForObject("""
                    select count(*) from databasechangelog
                    where id = '033-availability-novelty-final-review'
                    """, Integer.class)).isZero();
        }
    }

    private static void migrateToImmediatePredecessor(Connection connection) throws Exception {
        migrate(connection, PRODUCTION_BASELINE);
        migrate(connection, BEFORE_033);
        migrate(connection, CHANGESET_019);
    }

    private static void reconcileIfPending(Connection connection) throws Exception {
        if (reconciliationState(connection).equals("PENDING")) {
            applyReconciliation(connection);
        }
    }

    private static void applyReconciliation(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(Files.readString(SETUP_SQL));
        }
        PGConnection postgres = connection.unwrap(PGConnection.class);
        try (Reader manifest = Files.newBufferedReader(MANIFEST)) {
            postgres.getCopyAPI().copyIn("""
                    COPY issue_291_availability_novelty_source FROM STDIN
                    WITH (FORMAT csv, HEADER true, DELIMITER E'\\t', ENCODING 'UTF8')
                    """, manifest);
        }
        try (var statement = connection.createStatement()) {
            statement.execute(Files.readString(APPLY_SQL));
        }
    }

    private static String reconciliationState(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery(Files.readString(STATE_SQL))) {
            result.next();
            return result.getString(1);
        }
    }

    private static JdbcTemplate jdbc(Connection connection) {
        return new JdbcTemplate(new SingleConnectionDataSource(connection, true));
    }

    private static String jsonAggregate(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject(
                "select coalesce(jsonb_agg(to_jsonb(row_value) order by to_jsonb(row_value)::text), '[]'::jsonb)::text from "
                        + table + " row_value",
                String.class);
    }

    private static void migrate(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)
                .update(new Contexts(), new LabelExpression());
    }
}
