package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer.TemporaryDatabase;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Savepoint;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Exercises issue #217's generic migration rule with synthetic availability rows only. */
@Tag("migration")
class AvailabilityNoteSentenceCapitalizationMigrationIntegrationTest {

    private static final String MIGRATION =
            "db/changelog/catalog/038-availability-note-sentence-capitalization.sql";
    private static final TemporaryDatabase DATABASE =
            PostgreSqlTestServer.createTemporaryDatabase("availability_note_sentence_capitalization");

    private Connection connection;
    private JdbcTemplate jdbc;
    private String migration;

    @BeforeAll
    static void buildsEmptyDatabaseThroughTheFullMaster() throws Exception {
        try (Connection connection = DATABASE.openConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database)
                    .update(new Contexts(), new LabelExpression());
        }
    }

    @AfterAll
    static void dropsTemporaryDatabase() {
        DATABASE.close();
    }

    @BeforeEach
    void fixture() throws Exception {
        connection = DATABASE.openConnection();
        connection.setAutoCommit(false);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        jdbc.execute("""
                INSERT INTO ingredient_concept (code, display_name, challenge_specificity,
                    active, random_draw_enabled, curator_note)
                VALUES ('TEST_CAPITAL_SIMPLE', 'Technical simple', 'SPECIFIC', false, false, 'Concept metadata.'),
                       ('TEST_CAPITAL_PREFIX', 'Technical prefix', 'SPECIFIC', false, false, 'Concept metadata.'),
                       ('TEST_CAPITAL_UPPER', 'Technical upper', 'SPECIFIC', false, false, 'Concept metadata.'),
                       ('TEST_CAPITAL_NO_ALPHA', 'Technical symbols', 'SPECIFIC', false, false, 'Concept metadata.'),
                       ('TEST_CAPITAL_SHARED', 'Technical shared', 'SPECIFIC', false, false, 'Concept metadata.');
                INSERT INTO participant (code, display_name)
                VALUES ('TEST_CAPITAL_OTHER', 'Technical other participant');
                INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level, curator_note)
                SELECT c.id, p.id, 'EASY', note.curator_note
                FROM (VALUES
                    ('TEST_CAPITAL_SIMPLE', 'GEORGIA', 'lowercase start.'),
                    ('TEST_CAPITAL_SIMPLE', 'TEST_CAPITAL_OTHER', 'other participant.'),
                    ('TEST_CAPITAL_PREFIX', 'GEORGIA', '1äpfel remain unchanged after the first letter.'),
                    ('TEST_CAPITAL_UPPER', 'GEORGIA', 'Already correct.'),
                    ('TEST_CAPITAL_NO_ALPHA', 'GEORGIA', '123?!'),
                    ('TEST_CAPITAL_SHARED', 'GEORGIA', 'shared start.'),
                    ('TEST_CAPITAL_SHARED', 'TOBIAS', 'shared start.')
                ) AS note(concept_code, participant_code, curator_note)
                JOIN ingredient_concept c ON c.code = note.concept_code
                JOIN participant p ON p.code = note.participant_code;
                """);
        try (var input = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
            assertThat(input).isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @AfterEach
    void rollsBackFixture() throws Exception {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void capitalizesOnlyTheFirstLowercaseAlphabeticCharacterAndVersionsEachConceptOnce() {
        String protectedBefore = protectedState();

        jdbc.execute(migration);

        assertThat(protectedState()).isEqualTo(protectedBefore);
        assertThat(notes()).containsExactly(
                "TEST_CAPITAL_NO_ALPHA/GEORGIA=123?!",
                "TEST_CAPITAL_PREFIX/GEORGIA=1Äpfel remain unchanged after the first letter.",
                "TEST_CAPITAL_SHARED/GEORGIA=Shared start.",
                "TEST_CAPITAL_SHARED/TOBIAS=Shared start.",
                "TEST_CAPITAL_SIMPLE/GEORGIA=Lowercase start.",
                "TEST_CAPITAL_SIMPLE/TEST_CAPITAL_OTHER=Other participant.",
                "TEST_CAPITAL_UPPER/GEORGIA=Already correct.");
        assertThat(versions()).containsExactly(
                "TEST_CAPITAL_NO_ALPHA=0",
                "TEST_CAPITAL_PREFIX=1",
                "TEST_CAPITAL_SHARED=1",
                "TEST_CAPITAL_SIMPLE=1",
                "TEST_CAPITAL_UPPER=0");
        assertThat(jdbc.queryForObject("""
                SELECT count(*)
                FROM ingredient_availability availability
                JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
                WHERE concept.code LIKE 'TEST_CAPITAL_%'
                  AND availability.curator_note IS NOT NULL
                  AND (regexp_match(availability.curator_note, '[[:alpha:]]'))[1] ~ '^[[:lower:]]$'
                """, Integer.class)).isZero();
    }

    @Test
    void abortsBeforeAnyWriteWhenAUnicodeLowercaseLetterHasNoDistinctUppercaseReplacement() throws Exception {
        jdbc.execute("""
                INSERT INTO ingredient_concept (code, display_name, challenge_specificity,
                    active, random_draw_enabled, curator_note)
                VALUES ('TEST_CAPITAL_UNSUPPORTED', 'Technical unsupported', 'SPECIFIC', false, false, 'Concept metadata.');
                INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level, curator_note)
                SELECT c.id, p.id, 'EASY', 'ßeta requires an explicit decision.'
                FROM ingredient_concept c JOIN participant p ON p.code = 'GEORGIA'
                WHERE c.code = 'TEST_CAPITAL_UNSUPPORTED';
                """);
        String before = fullState();
        Savepoint checkpoint = connection.setSavepoint();

        assertThatThrownBy(() -> jdbc.execute(migration))
                .hasStackTraceContaining("Availability note sentence capitalization: unsupported uppercase replacement");

        connection.rollback(checkpoint);
        assertThat(fullState()).isEqualTo(before);
    }

    private java.util.List<String> notes() {
        return jdbc.queryForList("""
                SELECT concept.code || '/' || participant.code || '=' || availability.curator_note
                FROM ingredient_availability availability
                JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
                JOIN participant ON participant.id = availability.participant_id
                WHERE concept.code LIKE 'TEST_CAPITAL_%'
                ORDER BY concept.code, participant.code
                """, String.class);
    }

    private java.util.List<String> versions() {
        return jdbc.queryForList("""
                SELECT code || '=' || version
                FROM ingredient_concept
                WHERE code LIKE 'TEST_CAPITAL_%'
                ORDER BY code
                """, String.class);
    }

    private String protectedState() {
        return jdbc.queryForObject("""
                SELECT jsonb_agg(state ORDER BY state::text)::text
                FROM (
                    SELECT CASE
                        WHEN concept.code IN ('TEST_CAPITAL_SIMPLE', 'TEST_CAPITAL_PREFIX', 'TEST_CAPITAL_SHARED')
                            THEN to_jsonb(availability) - ARRAY['curator_note', 'updated_at']
                        ELSE to_jsonb(availability)
                    END AS state
                    FROM ingredient_availability availability
                    JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
                    WHERE concept.code LIKE 'TEST_CAPITAL_%'
                    UNION ALL
                    SELECT to_jsonb(concept) - ARRAY['version', 'updated_at']
                    FROM ingredient_concept concept
                    WHERE concept.code LIKE 'TEST_CAPITAL_%'
                ) protected_values
                """, String.class);
    }

    private String fullState() {
        return jdbc.queryForObject("""
                SELECT jsonb_agg(state ORDER BY state::text)::text
                FROM (
                    SELECT to_jsonb(availability) AS state
                    FROM ingredient_availability availability
                    JOIN ingredient_concept concept ON concept.id = availability.ingredient_concept_id
                    WHERE concept.code LIKE 'TEST_CAPITAL_%'
                    UNION ALL
                    SELECT to_jsonb(concept)
                    FROM ingredient_concept concept
                    WHERE concept.code LIKE 'TEST_CAPITAL_%'
                ) values_before
                """, String.class);
    }
}
