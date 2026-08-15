package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExclusionVariant;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Testcontainers
class GeneratorSimulationExhaustionRegressionTest {
    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_exhaustion_regression")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcGenerationRepository repository;
    @Autowired CandidateReservoirEngine reservoirEngine;
    @Autowired CandidateSetEngine setEngine;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void domainExhaustionDoesNotAbortLaterStepsOrCreateAnIncompleteSequence() {
        AtomicInteger calls = new AtomicInteger();
        CandidateSetEngine scriptedSetEngine = (prepared, batchNumber) -> {
            CandidateSetEngine.CandidateSetResult actual = setEngine.generate(prepared, batchNumber);
            if (calls.getAndIncrement() != 0) {
                return actual;
            }
            List<GeneratorReasonCode> diagnostics = new ArrayList<>(actual.diagnostics());
            diagnostics.add(GeneratorReasonCode.GENERATION_EXHAUSTED);
            return new ExhaustedCandidateSet(actual.reservoir(), actual.batchNumber(), actual.batchSeed(),
                    actual.fallbackAttempts(), diagnostics);
        };
        GeneratorSimulation simulation = new GeneratorSimulationService(catalogProjection, repository, reservoirEngine,
                scriptedSetEngine, generatorProperties, objectMapper, transactionManager);
        SimulationScenario scenario = new SimulationScenario(
                "EXHAUSTION_CONTINUES",
                new SeedRange(53_000_090L, 1),
                List.of(LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 10)),
                HistoryScenario.EMPTY_HISTORY,
                AttemptType.INITIAL,
                List.of(),
                Set.of(),
                1,
                ExclusionVariant.DEFAULT);

        GeneratorSimulation.SimulationReport report = simulation.simulate(new SimulationRequest(
                "EXHAUSTION_REGRESSION_V1", List.of(scenario), 2, GeneratorSimulation.SimulationControl.unbounded()));

        assertThat(report.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.completion().plannedCases()).isEqualTo(2);
        assertThat(report.completion().processedCases()).isEqualTo(2);
        assertThat(report.completion().skippedCases()).isZero();
        assertThat(report.completion().completedSequences()).isEqualTo(1);
        assertThat(report.completion().incompleteSequences()).isZero();
        assertThat(report.metrics().exhaustedSets()).isEqualTo(1);
        assertThat(report.metrics().successfulSets()).isEqualTo(1);
        assertThat(report.metrics().technicalErrors()).isZero();
        assertThat(report.metrics().replayChecks()).isEqualTo(1);
    }
}
