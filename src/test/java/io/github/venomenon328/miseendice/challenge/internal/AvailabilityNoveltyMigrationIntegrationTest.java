package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer;
import io.github.venomenon328.miseendice.testsupport.PostgreSqlTestServer.TemporaryDatabase;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Technical migration contracts only: no review file or ingredient-specific editorial oracle. */
@Tag("migration")
@SpringBootTest(classes = {MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class},
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-before-availability-novelty.yaml")
class AvailabilityNoveltyMigrationIntegrationTest {
    private static final String BEFORE = "db/changelog/db.changelog-before-availability-novelty.yaml";
    private static final String MASTER = "db/changelog/db.changelog-before-remove-generator-replay.yaml";
    private static final TemporaryDatabase DATABASE =
            PostgreSqlTestServer.createTemporaryDatabase("availability_novelty_migration");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::jdbcUrl);
        registry.add("spring.datasource.username", PostgreSqlTestServer::username);
        registry.add("spring.datasource.password", PostgreSqlTestServer::password);
        registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 4);
    }

    @AfterAll
    static void dropsTemporaryDatabase() {
        DATABASE.close();
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired GenerationCommands generation;
    @Autowired GenerationQueries generationQueries;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionCommands decisions;
    @Autowired CatalogGeneratorProjection projection;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;

    @Test
    void upgradesTheImmediatePredecessorPreservingMetadataAndHistoricalResults() throws Exception {
        // The current writer no longer supplies the obsolete duplicate result payload.
        // This test targets the pre-020 schema; its legacy column is irrelevant to catalog migration.
        jdbc.execute("alter table generation_batch alter column result_snapshot set default '{}'::jsonb");
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));
        var generated = (GenerationCommands.Generated) generation.startNewSession(new GenerationCommands.StartNewSession(
                LocalDate.of(2026, 9, 7), List.of(), 76100061L, 1, RestrictionMode.NONE));
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(CurationOrchestrationCommands.OfferReady.class);
        var offers = curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
        decisions.present(new OfferDecisionCommands.PresentOfferSet(offers.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(offers.offerSetId(), offers.offers().getFirst().offerId()));
        assertThat(jdbc.queryForObject("select count(*) from challenge", Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("select count(*) from generation_context_snapshot", Integer.class)).isPositive();

        // An additional participant is valid and remains outside the private G/T migration contract.
        jdbc.update("insert into participant (code, display_name) values ('TEST_189_SPARSE', 'Technical participant')");
        jdbc.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                select (select min(id) from ingredient_concept), id, 'PLANNED'
                from participant where code = 'TEST_189_SPARSE'
                """);
        var storedBatch = generationQueries.findBatch(generated.attemptId(), 1).orElseThrow();
        var preservedTables = preservedTables();
        String protectedConcepts = protectedConcepts(jdbc);
        var versions = jdbc.queryForList("select id, version from ingredient_concept order by id");
        var otherAvailability = otherAvailability();
        var catalogBefore = projection.snapshotForMonth(9, participants());
        try (Connection connection = dataSource.getConnection()) {
            migrate(connection, MASTER);
        }

        assertComplete(jdbc);
        assertThat(protectedConcepts(jdbc)).isEqualTo(protectedConcepts);
        assertThat(preservedTables()).isEqualTo(preservedTables);
        assertThat(otherAvailability()).isEqualTo(otherAvailability);
        versions.forEach(row -> assertThat(jdbc.queryForObject("select version from ingredient_concept where id = ?",
                Long.class, row.get("id"))).isEqualTo(((Number) row.get("version")).longValue() + 1));
        var catalogAfter = projection.snapshotForMonth(9, participants());
        assertThat(catalogAfter).isNotEqualTo(catalogBefore);
        assertThat(generationQueries.findBatch(generated.attemptId(), 1)).contains(storedBatch);

        // Notes are deliberately absent from the generator catalog and thus its serialized fingerprint input.
        jdbc.update("update ingredient_availability set curator_note = 'Technical note-only change.'");
        assertThat(projection.snapshotForMonth(9, participants())).isEqualTo(catalogAfter);
        assertThat(preservedTables()).isEqualTo(preservedTables);
        try (Connection connection = dataSource.getConnection()) {
            migrate(connection, MASTER);
        }
        assertThat(jdbc.queryForObject("select count(*) from ingredient_availability where curator_note <> ?",
                Integer.class, "Technical note-only change.")).isZero();
    }

    @Test
    void buildsAnEmptyPostgresDatabaseWithCompletePrivateMetadataAndNullableNotes() throws Exception {
        try (var database = newDatabase(); Connection connection = database.openConnection()) {
            migrate(connection, MASTER);
            connection.setAutoCommit(true);
            var fresh = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
            assertComplete(fresh);
            assertThat(fresh.queryForObject("""
                    select is_nullable from information_schema.columns
                    where table_name = 'ingredient_availability' and column_name = 'curator_note'
                    """, String.class)).isEqualTo("YES");
            assertThat(fresh.queryForObject("select count(*) from ingredient_concept where novelty_level is null and random_draw_enabled",
                    Integer.class)).isZero();
            for (String blank : List.of("", " ", "\t\n")) {
                assertThatThrownBy(() -> fresh.update("update ingredient_availability set curator_note = ?", blank))
                        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            }
            fresh.update("update ingredient_availability set curator_note = null");
            assertThat(fresh.queryForObject("select count(*) from ingredient_availability where curator_note is not null",
                    Integer.class)).isZero();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "update ingredient_concept set code = 'TEST_RENAMED' where id = (select min(id) from ingredient_concept)",
            "DELETE_CONCEPT",
            "insert into ingredient_concept (code, display_name, challenge_specificity, curator_note) values ('TEST_NEW', 'Technical new concept', 'SPECIFIC', 'Technical note.')",
            "update ingredient_concept set active = not active where id = (select min(id) from ingredient_concept)",
            "update ingredient_concept set base_draw_weight = base_draw_weight + 1 where id = (select min(id) from ingredient_concept)",
            "update ingredient_availability set availability_level = case when availability_level = 'EASY' then 'DIFFICULT' else 'EASY' end where ingredient_concept_id = (select min(ingredient_concept_id) from ingredient_availability)",
            "update ingredient_availability set curator_note = 'Unreviewed note.' where ingredient_concept_id = (select min(ingredient_concept_id) from ingredient_availability)"
    })
    void rejectsUnknownDeltasBeforeAnyEditorialWrite(String mutation) throws Exception {
        try (var temporaryDatabase = newDatabase(); Connection connection = temporaryDatabase.openConnection()) {
            migrate(connection, BEFORE);
            migrate(connection, "db/changelog/schema/019-availability-curator-note.sql");
            var database = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(connection, true));
            if (mutation.equals("DELETE_CONCEPT")) {
                long removedId = database.queryForObject("select max(id) from ingredient_concept", Long.class);
                database.update("delete from ingredient_refinement where parent_concept_id = ? or child_concept_id = ?",
                        removedId, removedId);
                for (String table : List.of("exclusion_rule_target", "ingredient_culinary_country",
                        "ingredient_availability", "ingredient_functional_role", "ingredient_culinary_flag",
                        "ingredient_culinary_dimension", "ingredient_seasonality")) {
                    database.update("delete from " + table + " where ingredient_concept_id = ?", removedId);
                }
                database.update("delete from ingredient_concept where id = ?", removedId);
            } else {
                database.execute(mutation);
            }
            var concepts = database.queryForList("select to_jsonb(ic)::text from ingredient_concept ic order by id", String.class);
            var availability = database.queryForList("select to_jsonb(a)::text from ingredient_availability a order by ingredient_concept_id, participant_id", String.class);
            assertThatThrownBy(() -> migrate(connection, MASTER)).hasStackTraceContaining("Issue #189:");
            assertThat(database.queryForList("select to_jsonb(ic)::text from ingredient_concept ic order by id", String.class)).isEqualTo(concepts);
            assertThat(database.queryForList("select to_jsonb(a)::text from ingredient_availability a order by ingredient_concept_id, participant_id", String.class)).isEqualTo(availability);
            assertThat(database.queryForObject("select count(*) from databasechangelog where id = '033-availability-novelty-final-review'", Integer.class)).isZero();
        }
    }

    private static void assertComplete(JdbcTemplate database) {
        assertThat(database.queryForObject("select count(*) from ingredient_concept where active and random_draw_enabled", Integer.class)).isPositive();
        assertThat(database.queryForObject("""
                select count(*) from ingredient_concept ic cross join participant p
                left join ingredient_availability a on a.ingredient_concept_id = ic.id and a.participant_id = p.id
                where ic.active and ic.random_draw_enabled and p.code in ('GEORGIA','TOBIAS')
                    and (ic.novelty_level is null or a.availability_level is null or a.curator_note is null or btrim(a.curator_note) = '')
                """, Integer.class)).isZero();
        assertThat(database.queryForObject("""
                select count(*) from ingredient_availability a join participant p on p.id = a.participant_id
                where p.code in ('GEORGIA','TOBIAS') and (a.curator_note is null or btrim(a.curator_note) = '')
                """, Integer.class)).isZero();
    }

    private List<CatalogGeneratorProjection.SessionParticipant> participants() {
        return jdbc.query("select id, code from participant where code in ('GEORGIA','TOBIAS') order by code",
                (rs, row) -> new CatalogGeneratorProjection.SessionParticipant(rs.getLong(1), rs.getString(2)));
    }

    private Map<String, String> preservedTables() {
        Map<String, String> result = new LinkedHashMap<>();
        jdbc.queryForList("""
                select tablename from pg_tables where schemaname = 'public'
                and tablename not in ('databasechangelog','databasechangeloglock','ingredient_concept','ingredient_availability')
                order by tablename
                """, String.class).forEach(table -> result.put(table, jdbc.queryForObject(
                "select coalesce(jsonb_agg(to_jsonb(t) order by to_jsonb(t)::text), '[]'::jsonb)::text from " + table + " t", String.class)));
        return result;
    }

    private static String protectedConcepts(JdbcTemplate database) {
        return database.queryForObject("""
                select jsonb_agg(to_jsonb(ic) - array['novelty_level','version','updated_at'] order by id)::text
                from ingredient_concept ic
                """, String.class);
    }

    private List<Map<String, Object>> otherAvailability() {
        return jdbc.queryForList("""
                select a.ingredient_concept_id, a.participant_id, a.availability_level, a.updated_at
                from ingredient_availability a join participant p on p.id = a.participant_id
                where p.code not in ('GEORGIA','TOBIAS') order by a.ingredient_concept_id, a.participant_id
                """);
    }

    private static TemporaryDatabase newDatabase() {
        return PostgreSqlTestServer.createTemporaryDatabase("issue189");
    }

    private static void migrate(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database).update(new Contexts(), new LabelExpression());
    }
}
