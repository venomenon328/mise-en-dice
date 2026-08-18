package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Exhausted;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = {
        io.github.venomenon328.miseendice.MiseEnDiceApplication.class,
        PersistedGenerationExhaustionIntegrationTest.ExhaustionConfiguration.class
})
@Testcontainers
class PersistedGenerationExhaustionIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generation_exhaustion")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GenerationCommands commands;
    @Autowired GenerationQueries queries;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void publicCommandPersistsTerminalExhaustionWithoutCandidates() {
        var outcome = commands.startNewSession(new StartNewSession(
                LocalDate.of(2026, 8, 13), List.of(), 47_000_061L, 1, RestrictionMode.AUTO));

        assertThat(outcome).isInstanceOf(Exhausted.class);
        Exhausted exhausted = (Exhausted) outcome;
        var attempt = queries.findAttempt(exhausted.attemptId()).orElseThrow();
        var batch = queries.findBatch(exhausted.attemptId(), 1).orElseThrow();
        assertThat(attempt.status()).isEqualTo("EXHAUSTED");
        assertThat(attempt.nextAction()).isEqualTo(GenerationQueries.NextAction.NONE);
        assertThat(batch.status()).isEqualTo("EXHAUSTED");
        assertThat(batch.candidates()).isEmpty();
        assertThat(batch.diagnosticsJson()).contains(GeneratorReasonCode.GENERATION_EXHAUSTED.name());
        assertThat(batch.resultSnapshotJson()).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from generation_batch where generation_attempt_id = ?",
                Integer.class, exhausted.attemptId())).isEqualTo(1);
    }

    @Test
    void unknownRuntimeFailureStaysTechnicalAndDoesNotCreateAnExhaustedBatch() {
        assertThatThrownBy(() -> commands.startNewSession(new StartNewSession(
                LocalDate.of(2026, 8, 13), List.of(), 47_000_062L, 1, RestrictionMode.AUTO)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected technical generator failure");

        long attemptId = jdbcTemplate.queryForObject("select max(id) from generation_attempt", Long.class);
        var attempt = queries.findAttempt(attemptId).orElseThrow();
        assertThat(attempt.status()).isEqualTo("FAILED");
        assertThat(attempt.failureReasonCode())
                .isEqualTo(GeneratorReasonCode.TECHNICAL_GENERATION_FAILURE.name());
        assertThat(queries.findBatch(attemptId, 1)).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExhaustionConfiguration {

        @Bean
        @Primary
        CandidateSetEngine forcedExhaustionEngine(CandidateReservoirEngine reservoirEngine) {
            return (prepared, batchNumber) -> {
                if (prepared.request().attemptSeed() == 47_000_062L) {
                    throw new IllegalStateException("injected technical generator failure");
                }
                var reservoir = reservoirEngine.generate(prepared, batchNumber);
                var diagnostics = new ArrayList<>(reservoir.diagnostics());
                diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
                long batchSeed = SeedDerivation.derive(
                        reservoir.context().configuration().generatorVersion(),
                        reservoir.context().attemptSeed(), SeedDerivation.batchScope(batchNumber),
                        SeedDerivation.Purpose.BATCH_ROOT, 0);
                return new ExhaustedCandidateSet(
                        reservoir, batchNumber, batchSeed, List.of(), diagnostics);
            };
        }
    }
}
