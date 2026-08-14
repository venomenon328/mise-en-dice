package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExclusionVariant;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Explicit issue-47 baseline gate. It intentionally delegates all case execution and aggregation to the shared
 * phase-9E2 simulation core; the test contains only the immutable historical matrix and its acceptance assertions.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@Testcontainers
class CandidateSetBaselineIntegrationTest {
    private static final long DEFAULT_SEED_START = 47_000_000L;
    private static final long FOCUS_SEED_START = 47_100_000L;
    private static final List<HistoryScenario> DEFAULT_SCENARIOS = List.of(
            HistoryScenario.EMPTY_HISTORY,
            HistoryScenario.NEUTRAL_HISTORY,
            HistoryScenario.RECOVERY_AFTER_ADVENTUROUS,
            HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR,
            HistoryScenario.LOADED_COOLDOWN_HISTORY);

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice").withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GeneratorSimulation simulation;

    @Test
    void completeIssue47MatrixReplaysAndPassesEveryQualityGate() throws IOException {
        SimulationReport report = simulation.simulate(new SimulationRequest("ISSUE_47_V1", matrix(), 2_304,
                GeneratorSimulation.SimulationControl.unbounded()));
        GeneratorSimulationReportCodec.write(report,
                Path.of("target", "candidate-generator-baseline", "issue-47-baseline.json"));

        assertThat(report.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.completion().plannedCases()).isEqualTo(2_304);
        assertThat(report.metrics().attempts()).isEqualTo(2_304);
        assertThat(report.metrics().successfulSets()).isEqualTo(2_304);
        assertThat(report.metrics().exhaustedSets()).isZero();
        assertThat(report.metrics().technicalErrors()).isZero();
        assertThat(report.metrics().replayChecks()).isEqualTo(2_304);
        assertThat(report.metrics().replayIntegrityMismatches()).isZero();
        assertThat(report.metrics().hardRuleViolations()).isZero();
        assertThat(report.metrics().quotaViolations()).isZero();
        assertThat(report.metrics().setCapViolations()).isZero();
        assertThat(report.metrics().strictPairMeanViolations()).isZero();
        assertThat(report.metrics().recoveryCadenceViolations()).isZero();
        assertThat(report.metrics().incompleteSuccesses()).isZero();
        assertThat(report.metrics().proposalAttempts().percentile95()).isLessThanOrEqualTo(new BigDecimal("4000"));
        assertThat(report.metrics().proposalAttempts().maximum()).isLessThanOrEqualTo(new BigDecimal("5000"));
        assertThat(count(report.metrics().fallbackUsage(), "DEFAULT/STRICT")).isGreaterThanOrEqualTo(1_460L);
        assertThat(count(report.metrics().fallbackUsage(), "DEFAULT/RELAXED_2")).isZero();
        assertThat(rate(countPrefix(report.metrics().exclusionFrequency(), "DEFAULT/"), 1_536))
                .isBetween(new BigDecimal("0.25"), new BigDecimal("0.35"));
        assertThat(countPrefix(report.metrics().exclusionFrequency(), "DISABLED/")).isZero();
        assertThat(countPrefix(report.metrics().exclusionFrequency(), "REQUIRED/")).isEqualTo(384);
        assertThat(count(report.metrics().fallbackUsage(), "DISABLED/STRICT")
                + count(report.metrics().fallbackUsage(), "REQUIRED/STRICT")).isGreaterThan(700L);
        assertThat(report.metrics().randomConceptConcentration().topOneShare())
                .isLessThanOrEqualTo(new BigDecimal("0.05"));
        assertThat(report.metrics().randomConceptConcentration().topTenShare())
                .isLessThanOrEqualTo(new BigDecimal("0.30"));
    }

    private List<SimulationScenario> matrix() {
        List<SimulationScenario> scenarios = new ArrayList<>();
        long defaultOffset = 0;
        long focusOffset = 0;
        for (int month = 1; month <= 12; month++) {
            LocalDate date = LocalDate.of(2026, month, 12);
            for (HistoryScenario history : DEFAULT_SCENARIOS) {
                scenarios.add(scenario("DEFAULT_" + month + "_" + history.name(), DEFAULT_SEED_START + defaultOffset,
                        16, date, history, AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.DEFAULT));
                defaultOffset += 16;
            }
            scenarios.add(scenario("DEFAULT_" + month + "_REROLL_EXACT_BLOCK", DEFAULT_SEED_START + defaultOffset,
                    16, date, HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL, List.of(),
                    Set.of("ARTICHOKE", "ASPARAGUS", "BACON", "BAMBOO_SHOOTS"), ExclusionVariant.DEFAULT));
            defaultOffset += 16;
            scenarios.add(scenario("DEFAULT_" + month + "_ONE_MATCHED_MANUAL", DEFAULT_SEED_START + defaultOffset,
                    16, date, HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                    List.of(new ManualInput(1, "Artischocke", "ARTICHOKE")), Set.of(), ExclusionVariant.DEFAULT));
            defaultOffset += 16;
            scenarios.add(scenario("DEFAULT_" + month + "_TWO_MIXED_MANUALS", DEFAULT_SEED_START + defaultOffset,
                    16, date, HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                    List.of(new ManualInput(1, "Speck", "BACON"), new ManualInput(2, "Use a waffle iron", null)),
                    Set.of(), ExclusionVariant.DEFAULT));
            defaultOffset += 16;

            for (HistoryScenario history : List.of(HistoryScenario.EMPTY_HISTORY, HistoryScenario.NEUTRAL_HISTORY,
                    HistoryScenario.LOADED_COOLDOWN_HISTORY)) {
                scenarios.add(scenario("DISABLED_" + month + "_" + history.name(), FOCUS_SEED_START + focusOffset,
                        8, date, history, AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.DISABLED));
                focusOffset += 8;
                scenarios.add(scenario("REQUIRED_" + month + "_" + history.name(), FOCUS_SEED_START + focusOffset,
                        8, date, history, AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.REQUIRED));
                focusOffset += 8;
            }
            for (ExclusionVariant variant : List.of(ExclusionVariant.DISABLED, ExclusionVariant.REQUIRED)) {
                scenarios.add(scenario(variant.name() + "_" + month + "_REROLL_EXACT_BLOCK",
                        FOCUS_SEED_START + focusOffset, 8, date, HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL,
                        List.of(), Set.of("ARTICHOKE", "ASPARAGUS", "BACON", "BAMBOO_SHOOTS"), variant));
                focusOffset += 8;
            }
        }
        assertThat(scenarios.stream().mapToInt(SimulationScenario::plannedCases).sum()).isEqualTo(2_304);
        return scenarios;
    }

    private static SimulationScenario scenario(
            String code,
            long startSeed,
            int seedCount,
            LocalDate date,
            HistoryScenario history,
            AttemptType attemptType,
            List<ManualInput> manuals,
            Set<String> rerollBlock,
            ExclusionVariant variant
    ) {
        return new SimulationScenario(code, new SeedRange(startSeed, seedCount), List.of(date), history, attemptType,
                manuals, rerollBlock, 1, variant);
    }

    private static long count(GeneratorSimulation.FrequencyList frequencies, String key) {
        return frequencies.entries().stream().filter(entry -> entry.key().equals(key))
                .mapToLong(GeneratorSimulation.Frequency::count).sum();
    }

    private static long countPrefix(GeneratorSimulation.FrequencyList frequencies, String prefix) {
        return frequencies.entries().stream().filter(entry -> entry.key().startsWith(prefix))
                .mapToLong(GeneratorSimulation.Frequency::count).sum();
    }

    private static BigDecimal rate(long value, long total) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(total), 12, java.math.RoundingMode.HALF_EVEN);
    }
}
