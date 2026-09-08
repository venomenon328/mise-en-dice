package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.ChallengeArchiveQueries;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class},
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-before-remove-generator-replay.yaml")
@Testcontainers
class RemoveGeneratorReplayMigrationIntegrationTest {
    private static final String UPGRADE = "db/changelog/db.changelog-through-remove-generator-replay.yaml";
    private static final String MASTER = "db/changelog/db.changelog-master.yaml";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 8);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired GenerationCommands generation;
    @Autowired GenerationQueries queries;
    @Autowired JdbcGenerationRepository repository;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionCommands decisions;
    @Autowired ChallengeArchiveQueries archive;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;
    @Autowired CurationOrchestrationIntegrationTest.SwitchableCandidateSetEngine generator;

    @Test
    void upgradesImmediateMainPreservingAllHistoryAndResumableContext() throws Exception {
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'generation_batch' and column_name = 'result_snapshot'
                """, Integer.class)).isOne();
        // Bridge only fixture creation by today's writer into yesterday's schema.
        // Replace the temporary default with the actual full old payload below.
        jdbc.execute("alter table generation_batch alter column result_snapshot set default '{}'::jsonb");
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));
        generator.exhaustBatchTwo();
        var historical = generate(206_001L, 3);
        assertThat(curation.curate(historical.attemptId()))
                .isInstanceOf(CurationOrchestrationCommands.OfferReady.class);
        var offers = curationQueries.findOfferSet(historical.attemptId()).orElseThrow();
        decisions.present(new OfferDecisionCommands.PresentOfferSet(offers.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(offers.offerSetId(), offers.offers().getFirst().offerId()));
        long challengeId = jdbc.queryForObject("select id from challenge", Long.class);
        long resultId = jdbc.queryForObject("""
                insert into challenge_result (challenge_id, participant_id, dish_name, description, evaluation)
                values (?, (select id from participant where code = 'GEORGIA'),
                        'Historical dish', 'Historical cooking result', 'Keep this evaluation') returning id
                """, Long.class, challengeId);
        jdbc.update("insert into challenge_result_ingredient (challenge_result_id, display_text) values (?, 'Extra ingredient')",
                resultId);
        var prepared = repository.snapshotCodec().decodeAndVerify(repository.loadContext(historical.attemptId()));
        for (int number : List.of(1, 2)) {
            jdbc.update("update generation_batch set result_snapshot = cast(? as jsonb) where generation_attempt_id = ? and batch_number = ?",
                    repository.snapshotCodec().json(generator.generate(prepared, number)), historical.attemptId(), number);
        }
        assertThat(jdbc.queryForObject("select bool_and(result_snapshot <> '{}'::jsonb) from generation_batch", Boolean.class))
                .isTrue();

        generator.reset();
        var resumable = generate(206_002L, 1);
        jdbc.update("delete from generation_batch where generation_attempt_id = ?", resumable.attemptId());
        jdbc.update("""
                update generation_attempt set status = 'CONTEXT_READY', completed_at = null,
                    operation_token = ?, lease_expires_at = now() - interval '1 minute' where id = ?
                """, UUID.randomUUID(), resumable.attemptId());
        jdbc.execute("alter table generation_batch alter column result_snapshot drop default");
        jdbc.update("update generation_attempt set configuration_version = 'historical-config' where id = ?",
                historical.attemptId());
        var offersBefore = curationQueries.findOfferSet(historical.attemptId()).orElseThrow();
        var tablesBefore = persistedRows(jdbc);
        var attemptBefore = queries.findAttempt(historical.attemptId()).orElseThrow();
        var firstBefore = queries.findBatch(historical.attemptId(), 1).orElseThrow();
        var secondBefore = queries.findBatch(historical.attemptId(), 2).orElseThrow();
        var frozenBefore = queries.findContext(resumable.attemptId()).orElseThrow();
        var archiveBefore = archive.findLatestChallenge().orElseThrow();
        try (Connection connection = dataSource.getConnection()) {
            migrate(connection, UPGRADE);
        }

        assertResultSchema(jdbc);
        assertThat(persistedRows(jdbc)).isEqualTo(tablesBefore);
        assertThat(queries.findAttempt(historical.attemptId())).contains(attemptBefore);
        assertThat(queries.findBatch(historical.attemptId(), 1)).contains(firstBefore);
        assertThat(queries.findBatch(historical.attemptId(), 2)).contains(secondBefore);
        assertThat(firstBefore.candidates()).hasSize(12).allSatisfy(candidate ->
                assertThat(candidate.requirements()).hasSize(4));
        assertThat(secondBefore.status()).isEqualTo("EXHAUSTED");
        assertThat(curationQueries.findOfferSet(historical.attemptId())).contains(offersBefore);
        assertThat(archive.findLatestChallenge()).contains(archiveBefore);
        assertThat(queries.findContext(resumable.attemptId())).contains(frozenBefore);
        assertThat(generation.startInitial(new GenerationCommands.StartExistingSession(
                resumable.sessionId(), DATE.plusMonths(1), List.of(), 999L)))
                .isInstanceOfSatisfying(GenerationCommands.Generated.class,
                        restored -> assertThat(restored.setFingerprint()).isEqualTo(resumable.setFingerprint()));
        generate(206_003L, 1);

        assertThatThrownBy(() -> jdbc.update("update generation_batch set set_evaluation = null where id = ?", firstBefore.batchId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update generation_batch set set_fingerprint = repeat('a', 64) where id = ?", secondBefore.batchId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        var afterFirstUpgrade = persistedRows(jdbc);
        try (Connection connection = dataSource.getConnection()) {
            migrate(connection, UPGRADE);
        }
        assertThat(persistedRows(jdbc)).isEqualTo(afterFirstUpgrade);
    }

    @Test
    void buildsAnEmptyPostgresDatabaseAndDoesNotRepeatTheMigration() throws Exception {
        String name = "issue206_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("create database " + name);
        String url = POSTGRES.getJdbcUrl().replaceFirst("/[^/?]+(?:\\?.*)?$", "/" + name);
        try (Connection connection = DriverManager.getConnection(url, POSTGRES.getUsername(), POSTGRES.getPassword())) {
            migrate(connection);
            var fresh = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            assertResultSchema(fresh);
            assertThat(fresh.queryForObject("select count(*) from ingredient_concept where active and random_draw_enabled", Integer.class))
                    .isPositive();
            var firstBuild = persistedRows(fresh);
            migrate(connection);
            assertThat(persistedRows(fresh)).isEqualTo(firstBuild);
        }
    }

    private GenerationCommands.Generated generate(long seed, int count) {
        return (GenerationCommands.Generated) generation.startNewSession(
                new GenerationCommands.StartNewSession(DATE, List.of(), seed, count, RestrictionMode.NONE));
    }

    private static void assertResultSchema(JdbcTemplate database) {
        assertThat(database.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'generation_batch' and column_name = 'result_snapshot'
                """, Integer.class)).isZero();
        assertThat(database.queryForObject("select count(*) from databasechangelog where id = '020-remove-generator-replay-result'",
                Integer.class)).isOne();
    }

    private static Map<String, String> persistedRows(JdbcTemplate database) {
        Map<String, String> rows = new LinkedHashMap<>();
        database.queryForList("""
                select tablename from pg_tables where schemaname = 'public'
                and tablename not in ('databasechangelog', 'databasechangeloglock') order by tablename
                """, String.class).forEach(table -> {
            String projection = "generation_batch".equals(table) ? "to_jsonb(t) - 'result_snapshot'" : "to_jsonb(t)";
            rows.put(table, database.queryForObject("select coalesce(jsonb_agg(" + projection
                    + " order by (" + projection + ")::text), '[]'::jsonb)::text from " + table + " t", String.class));
        });
        return rows;
    }

    private static void migrate(Connection connection) throws Exception {
        migrate(connection, MASTER);
    }

    private static void migrate(Connection connection, String changelog) throws Exception {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        new Liquibase(changelog, new ClassLoaderResourceAccessor(), database).update(new Contexts(), new LabelExpression());
    }
}
