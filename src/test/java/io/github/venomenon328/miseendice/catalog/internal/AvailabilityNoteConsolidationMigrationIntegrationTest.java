package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Savepoint;
import java.util.LinkedHashMap;
import java.util.Map;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/** Executes the migration algorithm with synthetic review rows, never editorial content assertions. */
@Testcontainers
class AvailabilityNoteConsolidationMigrationIntegrationTest {
    private static final String MIGRATION = "db/changelog/catalog/037-availability-note-consolidation.sql";
    private static final String ACTOR = "liquibase:037-availability-note-consolidation";
    private static final String FIXTURE = """
            INSERT INTO availability_note_consolidation_review VALUES
                ('TEST_NOTE_SHARED', 'EASY', 'EASY', 'Old Georgia.', 'Old Tobias.', 'Shared target.', 'Shared target.'),
                ('TEST_NOTE_SPLIT', 'PLANNED', 'SPECIALTY', 'Old Georgia.', 'Old Tobias.', 'Georgia target.', 'Tobias target.'),
                ('TEST_NOTE_ALREADY', 'EASY', 'EASY', 'Earlier Georgia.', 'Earlier Tobias.', 'Shared target.', 'Shared target.');
            """;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6");

    private Connection connection;
    private JdbcTemplate jdbc;
    private JdbcCatalogQueries catalogQueries;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String migration;

    @BeforeAll
    static void buildsEmptyDatabaseThroughTheFullMaster() throws Exception {
        try (Connection connection = connect()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            new Liquibase("db/changelog/db.changelog-master.yaml",
                    new ClassLoaderResourceAccessor(), database).update(new Contexts(), new LabelExpression());
        }
    }

    @BeforeEach
    void fixture() throws Exception {
        connection = connect();
        connection.setAutoCommit(false);
        jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        catalogQueries = new JdbcCatalogQueries(jdbc);
        jdbc.execute("""
                INSERT INTO ingredient_concept (code, display_name, challenge_specificity,
                    active, random_draw_enabled, curator_note)
                VALUES ('TEST_NOTE_SHARED', 'Technical shared', 'SPECIFIC', false, false, 'Concept metadata.'),
                       ('TEST_NOTE_SPLIT', 'Technical split', 'SPECIFIC', true, false, 'Concept metadata.'),
                       ('TEST_NOTE_ALREADY', 'Technical installed', 'SPECIFIC', true, false, 'Concept metadata.'),
                       ('TEST_NOTE_PARENT_Z', 'A technical parent', 'OPEN', true, false, 'Concept metadata.'),
                       ('TEST_NOTE_PARENT_A', 'Z technical parent', 'OPEN', true, false, 'Concept metadata.'),
                       ('TEST_NOTE_CHILD_Z', 'B technical child', 'SPECIFIC', true, false, 'Concept metadata.'),
                       ('TEST_NOTE_CHILD_A', 'Y technical child', 'SPECIFIC', true, false, 'Concept metadata.');
                INSERT INTO participant (code, display_name) VALUES ('TEST_NOTE_OTHER', 'Technical other');
                INSERT INTO ingredient_availability (ingredient_concept_id, participant_id, availability_level, curator_note)
                SELECT c.id, p.id,
                       CASE WHEN c.code = 'TEST_NOTE_SPLIT'
                            THEN CASE WHEN p.code = 'GEORGIA' THEN 'PLANNED' ELSE 'SPECIALTY' END
                            ELSE 'EASY' END,
                       CASE WHEN p.code = 'TEST_NOTE_OTHER' THEN 'Unrelated participant.'
                            WHEN c.code = 'TEST_NOTE_ALREADY' THEN 'Shared target.'
                            WHEN p.code = 'GEORGIA' THEN 'Old Georgia.' ELSE 'Old Tobias.' END
                FROM ingredient_concept c CROSS JOIN participant p
                WHERE c.code IN ('TEST_NOTE_SHARED', 'TEST_NOTE_SPLIT', 'TEST_NOTE_ALREADY')
                  AND p.code IN ('GEORGIA', 'TOBIAS', 'TEST_NOTE_OTHER');
                INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
                SELECT parent.id, child.id
                FROM ingredient_concept parent CROSS JOIN ingredient_concept child
                WHERE parent.code IN ('TEST_NOTE_PARENT_Z', 'TEST_NOTE_PARENT_A')
                  AND child.code = 'TEST_NOTE_SHARED';
                INSERT INTO ingredient_refinement (parent_concept_id, child_concept_id)
                SELECT parent.id, child.id
                FROM ingredient_concept parent CROSS JOIN ingredient_concept child
                WHERE parent.code = 'TEST_NOTE_SHARED'
                  AND child.code IN ('TEST_NOTE_CHILD_Z', 'TEST_NOTE_CHILD_A');
                INSERT INTO functional_role (code, display_name, description)
                VALUES ('TEST_NOTE_ROLE_Z', 'A technical role', 'First by display name.'),
                       ('TEST_NOTE_ROLE_A', 'Z technical role', 'Last by display name.');
                INSERT INTO ingredient_functional_role (ingredient_concept_id, functional_role_id)
                SELECT concept.id, role.id
                FROM ingredient_concept concept CROSS JOIN functional_role role
                WHERE concept.code = 'TEST_NOTE_SHARED' AND role.code LIKE 'TEST_NOTE_ROLE_%';
                INSERT INTO culinary_flag (code, display_name, description)
                VALUES ('TEST_NOTE_FLAG_Z', 'A technical flag', 'First by display name.'),
                       ('TEST_NOTE_FLAG_A', 'Z technical flag', 'Last by display name.');
                INSERT INTO ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
                SELECT concept.id, flag.id
                FROM ingredient_concept concept CROSS JOIN culinary_flag flag
                WHERE concept.code = 'TEST_NOTE_SHARED' AND flag.code LIKE 'TEST_NOTE_FLAG_%';
                INSERT INTO ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
                SELECT concept.id, dimension.id, 4
                FROM ingredient_concept concept CROSS JOIN culinary_dimension dimension
                WHERE concept.code = 'TEST_NOTE_SHARED' AND dimension.code = 'DOMINANCE';
                INSERT INTO ingredient_culinary_country (ingredient_concept_id, country_code)
                SELECT id, 'DE' FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED';
                INSERT INTO ingredient_seasonality (ingredient_concept_id, month, weight_multiplier)
                SELECT id, 7, 1.2500 FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED';
                INSERT INTO exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                SELECT rule.id, concept.id, false
                FROM exclusion_rule rule CROSS JOIN ingredient_concept concept
                WHERE rule.code = 'NO_COCONUT_MILK' AND concept.code = 'TEST_NOTE_SHARED';
                """);
        try (var input = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
            assertThat(input).isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            int start = sql.indexOf("-- BEGIN APPROVED NOTES");
            int end = sql.indexOf("-- END APPROVED NOTES");
            assertThat(start).isPositive();
            assertThat(end).isGreaterThan(start);
            migration = sql.substring(0, start) + FIXTURE + sql.substring(end);
        }
    }

    @AfterEach
    void rollbackFixture() throws Exception {
        if (connection != null) {
            connection.rollback();
            connection.close();
        }
    }

    @Test
    void changesOnlyNotesAndVersionsWithCanonicalCompleteGroupedAudit() throws Exception {
        var protectedBefore = protectedState();
        var snapshotsBefore = runtimeSnapshots("TEST_NOTE_SHARED", "TEST_NOTE_SPLIT");
        var representative = catalogQueries.findConcept(conceptId("TEST_NOTE_SHARED")).orElseThrow();
        assertThat(representative.directParents())
                .extracting(value -> value.code())
                .containsExactly("TEST_NOTE_PARENT_Z", "TEST_NOTE_PARENT_A");
        assertThat(representative.directChildren())
                .extracting(value -> value.code())
                .containsExactly("TEST_NOTE_CHILD_Z", "TEST_NOTE_CHILD_A");
        assertThat(representative.functionalRoles())
                .extracting(value -> value.code())
                .containsExactly("TEST_NOTE_ROLE_Z", "TEST_NOTE_ROLE_A");
        assertThat(representative.culinaryFlags())
                .extracting(value -> value.code())
                .containsExactly("TEST_NOTE_FLAG_Z", "TEST_NOTE_FLAG_A");
        assertThat(representative.culinaryDimensions()).anyMatch(value -> value.level() == null);
        assertThat(representative.availability()).hasSize(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM ingredient_availability
                WHERE ingredient_concept_id = ?
                """, Integer.class, representative.id())).isEqualTo(3);
        assertThat(representative.seasonality()).hasSize(12);
        assertThat(representative.seasonality().stream()
                .filter(value -> value.month() == 6)
                .map(value -> value.weightMultiplier())
                .findFirst()).contains(java.math.BigDecimal.ONE);
        assertThat(representative.directExclusionRules()).contains("keine Kokosmilch");
        int auditsBefore = auditCount();
        jdbc.execute(migration);
        var snapshotsAfter = runtimeSnapshots("TEST_NOTE_SHARED", "TEST_NOTE_SPLIT");
        assertThat(protectedState()).isEqualTo(protectedBefore);
        assertThat(jdbc.queryForList("""
                SELECT a.curator_note FROM ingredient_availability a
                JOIN ingredient_concept c ON c.id = a.ingredient_concept_id
                JOIN participant p ON p.id = a.participant_id
                WHERE c.code = 'TEST_NOTE_SHARED' AND p.code IN ('GEORGIA', 'TOBIAS') ORDER BY p.code
                """, String.class)).containsExactly("Shared target.", "Shared target.");
        assertThat(jdbc.queryForList("""
                SELECT a.curator_note FROM ingredient_availability a
                JOIN ingredient_concept c ON c.id = a.ingredient_concept_id
                JOIN participant p ON p.id = a.participant_id
                WHERE c.code = 'TEST_NOTE_SPLIT' AND p.code IN ('GEORGIA', 'TOBIAS') ORDER BY p.code
                """, String.class)).containsExactly("Georgia target.", "Tobias target.");
        assertThat(jdbc.queryForList("""
                SELECT version FROM ingredient_concept
                WHERE code IN ('TEST_NOTE_SHARED', 'TEST_NOTE_SPLIT', 'TEST_NOTE_ALREADY') ORDER BY code
                """, Long.class)).containsExactly(0L, 1L, 1L);
        assertThat(auditCount() - auditsBefore).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT count(DISTINCT change_group_id) FROM catalog_audit_entry
                WHERE actor_key = ? AND after_state->>'code' LIKE 'TEST_NOTE_%'
                """, Integer.class, ACTOR)).isOne();
        assertThat(jdbc.queryForObject("""
                SELECT bool_and(
                    (before_state - 'version' - 'availability') = (after_state - 'version' - 'availability')
                    AND (after_state->>'version')::bigint = (before_state->>'version')::bigint + 1
                    AND jsonb_array_length(before_state->'availability') = 2
                    AND jsonb_array_length(after_state->'availability') = 2
                    AND (before_state->'availability') IS DISTINCT FROM (after_state->'availability')
                    AND payload_version = 1 AND entity_type = 'INGREDIENT_CONCEPT' AND action = 'UPDATE'
                ) FROM catalog_audit_entry WHERE actor_key = ? AND after_state->>'code' LIKE 'TEST_NOTE_%'
                """, Boolean.class, ACTOR)).isTrue();
        snapshotsBefore.forEach((conceptId, before) -> assertThat(jdbc.queryForObject("""
                SELECT before_state = cast(? as jsonb) AND after_state = cast(? as jsonb)
                FROM catalog_audit_entry WHERE actor_key = ? AND entity_id = ?
                """, Boolean.class, before, snapshotsAfter.get(conceptId), ACTOR, conceptId)).isTrue());
    }

    @Test
    void acceptsPartiallyInstalledTargetsAndSkipsFullyInstalledPairs() {
        jdbc.update("""
                UPDATE ingredient_availability SET curator_note = 'Shared target.'
                WHERE ingredient_concept_id = (SELECT id FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED')
                  AND participant_id = (SELECT id FROM participant WHERE code = 'GEORGIA')
                """);
        jdbc.execute(migration);
        var after = fullState();
        // ON COMMIT DROP tables deliberately remain until commit; remove only the
        // migration's temporary tables to exercise a second algorithm invocation.
        jdbc.execute("""
                DROP TABLE availability_note_consolidation_before;
                DROP TABLE availability_note_consolidation;
                DROP TABLE availability_note_consolidation_review;
                """);
        jdbc.execute(migration);
        assertThat(fullState()).isEqualTo(after);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "UPDATE ingredient_availability SET curator_note = 'Unreviewed.' WHERE ingredient_concept_id = (SELECT id FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED')",
            "UPDATE ingredient_availability SET curator_note = NULL WHERE ingredient_concept_id = (SELECT id FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED')",
            "UPDATE ingredient_availability SET availability_level = 'DIFFICULT' WHERE ingredient_concept_id = (SELECT id FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED')",
            "DELETE FROM ingredient_availability WHERE ingredient_concept_id = (SELECT id FROM ingredient_concept WHERE code = 'TEST_NOTE_SHARED')",
            "UPDATE ingredient_concept SET code = 'TEST_NOTE_RENAMED' WHERE code = 'TEST_NOTE_SHARED'"
    })
    void rejectsUnreviewedOrMissingValuesBeforeAnyWrite(String mutation) throws Exception {
        jdbc.execute(mutation);
        var before = fullState();
        Savepoint checkpoint = connection.setSavepoint();
        assertThatThrownBy(() -> jdbc.execute(migration))
                .hasStackTraceContaining("Availability note consolidation: unreviewed or missing values for")
                .hasStackTraceContaining("TEST_NOTE_SHARED/");
        connection.rollback(checkpoint);
        assertThat(fullState()).isEqualTo(before);
    }

    @Test
    void rollsBackNotesAndVersionsWhenAuditFails() throws Exception {
        jdbc.execute("""
                CREATE FUNCTION pg_temp.reject_note_audit() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN RAISE EXCEPTION 'Synthetic audit failure'; END $$;
                CREATE TRIGGER test_note_audit_failure BEFORE INSERT ON catalog_audit_entry
                FOR EACH ROW EXECUTE FUNCTION pg_temp.reject_note_audit();
                """);
        var before = fullState();
        Savepoint checkpoint = connection.setSavepoint();
        assertThatThrownBy(() -> jdbc.execute(migration)).hasStackTraceContaining("Synthetic audit failure");
        connection.rollback(checkpoint);
        assertThat(fullState()).isEqualTo(before);
    }

    private int auditCount() {
        return jdbc.queryForObject("SELECT count(*) FROM catalog_audit_entry WHERE actor_key = ?", Integer.class, ACTOR);
    }

    private Map<Long, String> runtimeSnapshots(String... conceptCodes) throws Exception {
        Map<Long, String> snapshots = new LinkedHashMap<>();
        for (String conceptCode : conceptCodes) {
            long conceptId = conceptId(conceptCode);
            snapshots.put(conceptId, objectMapper.writeValueAsString(
                    CatalogIngredientSnapshotFactory.snapshot(
                            catalogQueries.findConcept(conceptId).orElseThrow()).values()));
        }
        return snapshots;
    }

    private long conceptId(String conceptCode) {
        return jdbc.queryForObject(
                "SELECT id FROM ingredient_concept WHERE code = ?", Long.class, conceptCode);
    }

    private Map<String, String> protectedState() {
        return state(true);
    }

    private Map<String, String> fullState() {
        return state(false);
    }

    private Map<String, String> state(boolean protectedOnly) {
        Map<String, String> values = new LinkedHashMap<>();
        jdbc.queryForList("""
                SELECT tablename FROM pg_tables WHERE schemaname = 'public'
                AND tablename NOT IN ('databasechangelog', 'databasechangeloglock') ORDER BY tablename
                """, String.class).forEach(table -> {
            if (protectedOnly && table.equals("catalog_audit_entry")) return;
            String projection = "to_jsonb(t)";
            if (protectedOnly && table.equals("ingredient_concept")) {
                projection = "to_jsonb(t) - ARRAY['version', 'updated_at']";
            } else if (protectedOnly && table.equals("ingredient_availability")) {
                // Preserve complete unrelated rows; only the reviewed G/T texts may change.
                projection = """
                        CASE WHEN t.ingredient_concept_id IN
                            (SELECT id FROM ingredient_concept WHERE code IN ('TEST_NOTE_SHARED', 'TEST_NOTE_SPLIT'))
                            AND t.participant_id IN (SELECT id FROM participant WHERE code IN ('GEORGIA', 'TOBIAS'))
                        THEN to_jsonb(t) - ARRAY['curator_note', 'updated_at'] ELSE to_jsonb(t) END
                        """;
            }
            values.put(table, jdbc.queryForObject("SELECT coalesce(jsonb_agg(" + projection
                    + " ORDER BY (" + projection + ")::text), '[]'::jsonb)::text FROM " + table + " t", String.class));
        });
        return values;
    }

    private static Connection connect() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
