package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.QuotaEvaluation;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.SetEvaluation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class GeneratorSimulationExhaustionRegressionTest extends CurrentSchemaPostgresIntegrationTest {

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcGenerationRepository repository;
    @Autowired CandidateReservoirEngine reservoirEngine;
    @Autowired CandidateSetEngine setEngine;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void domainExhaustionDoesNotAbortLaterStepsOrCreateAnIncompleteSequence() {
        AtomicInteger calls = new AtomicInteger();
        CandidateSetEngine scriptedSetEngine = (prepared, batchNumber) -> {
            if (calls.getAndIncrement() == 0) {
                var reservoir = reservoirEngine.generate(prepared, batchNumber);
                return new ExhaustedCandidateSet(reservoir, batchNumber, prepared.request().attemptSeed(),
                        List.of(), List.of(GeneratorReasonCode.GENERATION_EXHAUSTED));
            }
            return setEngine.generate(prepared, batchNumber);
        };
        GeneratorSimulation simulation = simulationWith(scriptedSetEngine);
        SimulationScenario scenario = new SimulationScenario(
                "EXHAUSTION_CONTINUES",
                new SeedRange(53_000_001L, 1),
                List.of(LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 20)),
                HistoryScenario.EMPTY_HISTORY,
                AttemptType.INITIAL,
                List.of(),
                1,
                RestrictionMode.AUTO);

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
        assertThat(report.metrics().determinismChecks()).isEqualTo(1);
    }

    @Test
    void strictNoveltyQuotaDeviationRemainsASoftSignal() {
        AtomicBoolean alteredStrictSet = new AtomicBoolean();
        CandidateSetEngine scriptedSetEngine = (prepared, batchNumber) -> {
            CandidateSetEngine.CandidateSetResult actual = setEngine.generate(prepared, batchNumber);
            if (!(actual instanceof GeneratedCandidateSet generated)) {
                return actual;
            }
            if (generated.fallbackLevel() != FallbackLevel.STRICT) {
                return generated;
            }
            alteredStrictSet.set(true);
            SetEvaluation evaluation = generated.evaluation();
            Map<NoveltyBand, Integer> deviations = new EnumMap<>(NoveltyBand.class);
            deviations.putAll(evaluation.novelty().deviations());
            NoveltyBand changedBand = deviations.keySet().stream().findFirst().orElse(NoveltyBand.FAMILIAR);
            deviations.put(changedBand, deviations.getOrDefault(changedBand, 0) + 1);
            QuotaEvaluation<NoveltyBand> novelty = new QuotaEvaluation<>(evaluation.novelty().targets(),
                    evaluation.novelty().actual(), deviations);
            SetEvaluation alteredEvaluation = new SetEvaluation(
                    evaluation.specificity(), evaluation.profiles(), novelty, evaluation.pairs(),
                    evaluation.pairStatistics(), evaluation.randomConceptUsage(), evaluation.informativeAncestorUsage(),
                    evaluation.profileUsage(), evaluation.difficultCandidateCount(), evaluation.selectionDecisions(),
                    evaluation.reasonCodes());
            return new GeneratedCandidateSet(generated.reservoir(), generated.batchNumber(), generated.batchSeed(),
                    generated.fallbackLevel(), generated.candidates(), alteredEvaluation, generated.fingerprint(),
                    generated.fallbackAttempts(), generated.diagnostics());
        };
        GeneratorSimulation simulation = simulationWith(scriptedSetEngine);
        SimulationScenario scenario = new SimulationScenario(
                "STRICT_NOVELTY_QUOTA",
                new SeedRange(152_000_001L, 10),
                List.of(LocalDate.of(2026, 2, 12)),
                HistoryScenario.NEUTRAL_HISTORY,
                AttemptType.INITIAL,
                List.of(),
                1,
                RestrictionMode.AUTO);

        GeneratorSimulation.SimulationReport report = simulation.simulate(new SimulationRequest(
                "STRICT_NOVELTY_QUOTA_REGRESSION_V1", List.of(scenario), 10,
                GeneratorSimulation.SimulationControl.unbounded()));

        assertThat(report.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.metrics().successfulSets()).isEqualTo(10);
        assertThat(alteredStrictSet).isTrue();
        assertThat(report.metrics().quotaViolations()).isZero();
        assertThat(report.metrics().hardRuleViolations()).isZero();
    }

    @Test
    void immediateDeterminismCheckDetectsAChangedResult() {
        AtomicInteger calls = new AtomicInteger();
        CandidateSetEngine changingEngine = (prepared, batchNumber) -> {
            var result = (GeneratedCandidateSet) setEngine.generate(prepared, batchNumber);
            if (calls.incrementAndGet() == 1) {
                return result;
            }
            return new GeneratedCandidateSet(result.reservoir(), result.batchNumber(), result.batchSeed(),
                    result.fallbackLevel(), result.candidates(), result.evaluation(), "0".repeat(64),
                    result.fallbackAttempts(), result.diagnostics());
        };
        var scenario = new SimulationScenario("DETERMINISM_MISMATCH", new SeedRange(53_000_001L, 1),
                List.of(LocalDate.of(2026, 8, 13)), HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                List.of(), 1, RestrictionMode.AUTO);
        var report = simulationWith(changingEngine).simulate(new SimulationRequest(
                "DETERMINISM_CHECK_V1", List.of(scenario), 1, GeneratorSimulation.SimulationControl.unbounded()));

        assertThat(calls).hasValue(2);
        assertThat(report.metrics().successfulSets()).isOne();
        assertThat(report.metrics().determinismChecks()).isOne();
        assertThat(report.metrics().determinismMismatches()).isOne();
        assertThat(report.metrics().technicalErrors()).isZero();
        assertThat(report.metrics().exhaustedSets()).isZero();
    }

    private GeneratorSimulation simulationWith(CandidateSetEngine scriptedSetEngine) {
        return new GeneratorSimulationService(catalogProjection, repository, reservoirEngine, scriptedSetEngine,
                generatorProperties, transactionManager);
    }
}
