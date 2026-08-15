package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewSuccess;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExclusionVariant;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FingerprintVariation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Frequency;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Explicit phase-9F calibration gate. This test is excluded unless the {@code generator-calibration} profile is
 * selected. Every partition delegates generation, replay, invariant checks and statistics to
 * {@link GeneratorSimulation}.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@Testcontainers
class CandidateGeneratorCalibrationIntegrationTest {
    private static final String SCENARIO_VERSION = "ISSUE_40_CALIBRATION_V1";
    private static final long MAIN_SEED_START = 40_000_000L;
    private static final long FOCUS_SEED_START = 40_100_000L;
    private static final Path OUTPUT_DIRECTORY = Path.of("target", "generator-calibration", "repository-baseline");
    private static final Path MANUAL_CORPUS_DIRECTORY =
            Path.of("target", "generator-calibration", "manual-corpus");
    private static final Set<String> REROLL_BLOCK =
            Set.of("ARTICHOKE", "ASPARAGUS", "BACON", "BAMBOO_SHOOTS");
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_calibration")
            .withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GeneratorSimulation simulation;
    @Autowired GeneratorLaboratory laboratory;
    @Autowired CatalogGeneratorProjection catalogProjection;

    @Test
    void completePhase9fMatrixPassesTheRepositoryQualityGate() throws IOException {
        List<Partition> partitions = matrix();
        List<SimulationReport> reports = new ArrayList<>();

        for (Partition partition : partitions) {
            SimulationReport report = simulation.simulate(partition.request());
            GeneratorSimulationReportCodec.write(report, OUTPUT_DIRECTORY.resolve(partition.code() + ".json"));
            assertPartitionGate(partition, report);
            reports.add(report);
        }
        assertThat(reports).hasSize(4);
    }

    @Test
    void mixedMultiWeekSequencesRemainReplayableAndWithinTheSameHardGates() throws IOException {
        List<SimulationScenario> scenarios = List.of(
                sequence("SEQUENCE_EMPTY", 40_200_000L, HistoryScenario.EMPTY_HISTORY, 6, 1),
                sequence("SEQUENCE_RECOVERY", 40_200_008L, HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, 4, 6),
                sequence("SEQUENCE_SEEKING", 40_200_016L, HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR, 4, 12));
        SimulationRequest request = new SimulationRequest("ISSUE_40_SEQUENCES_V1", scenarios, 112,
                GeneratorSimulation.SimulationControl.unbounded());

        SimulationReport report = simulation.simulate(request);
        GeneratorSimulationReportCodec.write(report,
                Path.of("target", "generator-calibration", "synthetic-sequences.json"));

        assertThat(report.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.completion().plannedCases()).isEqualTo(112);
        assertThat(report.metrics().attempts()).isEqualTo(112);
        assertThat(report.metrics().successfulSets()).isEqualTo(112);
        assertInvariantCountersAreZero(report);
    }

    @Test
    void aSmallMatrixKeepsTheSameReportedResultsWhenPartitioned() {
        List<SimulationScenario> scenarios = List.of(
                basic("PARTITION_EMPTY", 40_300_000L, LocalDate.of(2026, 2, 15),
                        HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL, List.of(), Set.of(),
                        ExclusionVariant.DEFAULT, 1, 1),
                basic("PARTITION_RECOVERY", 40_300_001L, LocalDate.of(2026, 5, 15),
                        HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, AttemptType.INITIAL, List.of(), Set.of(),
                        ExclusionVariant.DEFAULT, 1, 1),
                basic("PARTITION_MANUAL", 40_300_002L, LocalDate.of(2026, 8, 15),
                        HistoryScenario.NEUTRAL_HISTORY, AttemptType.INITIAL,
                        List.of(new ManualInput(1, "Artischocke", "ARTICHOKE")), Set.of(),
                        ExclusionVariant.DEFAULT, 1, 1),
                basic("PARTITION_REROLL", 40_300_003L, LocalDate.of(2026, 11, 15),
                        HistoryScenario.LOADED_COOLDOWN_HISTORY, AttemptType.REROLL, List.of(), REROLL_BLOCK,
                        ExclusionVariant.DEFAULT, 1, 1));

        SimulationReport combined = simulation.simulate(request("ISSUE_40_PARTITION_COMBINED_V1", scenarios));
        List<SimulationReport> split = List.of(
                simulation.simulate(request("ISSUE_40_PARTITION_A_V1", scenarios.subList(0, 2))),
                simulation.simulate(request("ISSUE_40_PARTITION_B_V1", scenarios.subList(2, 4))));

        Map<String, FingerprintVariation> combinedVariations = variations(combined);
        assertThat(split).allSatisfy(report -> {
            assertInvariantCountersAreZero(report);
            assertThat(variations(report)).allSatisfy((scenario, variation) ->
                    assertThat(variation).isEqualTo(combinedVariations.get(scenario)));
        });
        assertThat(split.stream().flatMap(report -> variations(report).keySet().stream()).toList())
                .containsExactlyInAnyOrderElementsOf(combinedVariations.keySet());
    }

    @Test
    void fixedManualAcceptanceCorpusIsMaterializedFromTheLaboratoryContract() throws IOException {
        List<ManualCorpusCase> corpus = List.of(
                corpus("MC-01", 40_400_001L, 1, HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                        List.of(), List.of()),
                corpus("MC-02", 40_400_002L, 4, HistoryScenario.NEUTRAL_HISTORY, AttemptType.INITIAL,
                        List.of(matchedManual(4, 1, "Artischocke", "ARTICHOKE")), List.of()),
                corpus("MC-03", 40_400_003L, 7, HistoryScenario.RECOVERY_AFTER_ADVENTUROUS,
                        AttemptType.INITIAL,
                        List.of(matchedManual(7, 1, "Speck", "BACON"),
                                new GeneratorLaboratory.ManualInput(2, "Use a waffle iron", null)), List.of()),
                corpus("MC-04", 40_400_004L, 10, HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR,
                        AttemptType.INITIAL, List.of(), List.of()),
                corpus("MC-05", 40_400_005L, 8, HistoryScenario.LOADED_COOLDOWN_HISTORY,
                        AttemptType.INITIAL, List.of(), List.of()),
                corpus("MC-06", 40_400_006L, 3, HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL,
                        List.of(), conceptIds(3, REROLL_BLOCK)),
                corpus("MC-07", 40_400_007L, 11, HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                        List.of(matchedManual(11, 1, "Artischocke", "ARTICHOKE"),
                                matchedManual(11, 2, "Speck", "BACON")), List.of()),
                corpus("MC-08", 40_400_008L, 2, HistoryScenario.NEUTRAL_HISTORY, AttemptType.INITIAL,
                        List.of(new GeneratorLaboratory.ManualInput(1, "Cook without an oven", null)), List.of()));

        Files.createDirectories(MANUAL_CORPUS_DIRECTORY);
        int selectedExclusions = 0;
        for (ManualCorpusCase corpusCase : corpus) {
            GeneratorLaboratory.PreviewResult result = laboratory.preview(corpusCase.request());
            assertThat(result).as(corpusCase.code()).isInstanceOf(PreviewSuccess.class);
            PreviewSuccess success = (PreviewSuccess) result;
            Files.writeString(MANUAL_CORPUS_DIRECTORY.resolve(corpusCase.code() + ".prepared.json"),
                    success.rawPreparedAttemptJson());
            Files.writeString(MANUAL_CORPUS_DIRECTORY.resolve(corpusCase.code() + ".set.json"),
                    success.rawSetJson());
            if (success.preparedAttempt().exclusionDecision() instanceof AttemptExclusionDecision.Selected) {
                selectedExclusions++;
            }
        }

        assertThat(selectedExclusions).as("manual corpus must cover previews with and without an exclusion")
                .isBetween(1, corpus.size() - 1);
    }

    private List<Partition> matrix() {
        List<SimulationScenario> mainA = new ArrayList<>();
        List<SimulationScenario> mainB = new ArrayList<>();
        List<SimulationScenario> focusDisabled = new ArrayList<>();
        List<SimulationScenario> focusRequired = new ArrayList<>();
        long mainASeed = MAIN_SEED_START;
        long mainBSeed = MAIN_SEED_START + 3_072L;
        long disabledSeed = FOCUS_SEED_START;
        long requiredSeed = FOCUS_SEED_START + 10_000L;

        for (int month = 1; month <= 12; month++) {
            LocalDate date = LocalDate.of(2026, month, 15);
            for (HistoryScenario history : List.of(HistoryScenario.EMPTY_HISTORY, HistoryScenario.NEUTRAL_HISTORY,
                    HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR)) {
                mainA.add(basic("MAIN_A_" + month + "_" + history.name(), mainASeed, date, history,
                        AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.DEFAULT, 64, 1));
                mainASeed += 64;
            }
            mainB.add(basic("MAIN_B_" + month + "_LOADED_COOLDOWN_HISTORY", mainBSeed, date,
                    HistoryScenario.LOADED_COOLDOWN_HISTORY, AttemptType.INITIAL, List.of(), Set.of(),
                    ExclusionVariant.DEFAULT, 64, 1));
            mainBSeed += 64;
            mainB.add(basic("MAIN_B_" + month + "_REROLL_EXACT_BLOCK", mainBSeed, date,
                    HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL, List.of(), REROLL_BLOCK,
                    ExclusionVariant.DEFAULT, 64, 2));
            mainBSeed += 64;
            mainB.add(basic("MAIN_B_" + month + "_ONE_MATCHED_MANUAL", mainBSeed, date,
                    HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                    List.of(new ManualInput(1, "Artischocke", "ARTICHOKE")), Set.of(),
                    ExclusionVariant.DEFAULT, 64, 3));
            mainBSeed += 64;
            mainB.add(basic("MAIN_B_" + month + "_TWO_MIXED_MANUALS", mainBSeed, date,
                    HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                    List.of(new ManualInput(1, "Speck", "BACON"), new ManualInput(2, "Use a waffle iron", null)),
                    Set.of(), ExclusionVariant.DEFAULT, 64, 4));
            mainBSeed += 64;

            for (HistoryScenario history : List.of(HistoryScenario.EMPTY_HISTORY, HistoryScenario.NEUTRAL_HISTORY,
                    HistoryScenario.LOADED_COOLDOWN_HISTORY)) {
                focusDisabled.add(basic("FOCUS_OFF_" + month + "_" + history.name(), disabledSeed, date, history,
                        AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.DISABLED, 32, 1));
                disabledSeed += 32;
                focusRequired.add(basic("FOCUS_ON_" + month + "_" + history.name(), requiredSeed, date, history,
                        AttemptType.INITIAL, List.of(), Set.of(), ExclusionVariant.REQUIRED, 32, 1));
                requiredSeed += 32;
            }
            focusDisabled.add(basic("FOCUS_OFF_" + month + "_REROLL_EXACT_BLOCK", disabledSeed, date,
                    HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL, List.of(), REROLL_BLOCK,
                    ExclusionVariant.DISABLED, 32, 2));
            disabledSeed += 32;
            focusRequired.add(basic("FOCUS_ON_" + month + "_REROLL_EXACT_BLOCK", requiredSeed, date,
                    HistoryScenario.EMPTY_HISTORY, AttemptType.REROLL, List.of(), REROLL_BLOCK,
                    ExclusionVariant.REQUIRED, 32, 2));
            requiredSeed += 32;
        }

        return List.of(
                partition("main-a", ExclusionVariant.DEFAULT, mainA, 3_072, 2_920, 61),
                partition("main-b", ExclusionVariant.DEFAULT, mainB, 3_072, 2_920, 61),
                partition("focus-disabled", ExclusionVariant.DISABLED, focusDisabled, 1_536, 1_460, 31),
                partition("focus-required", ExclusionVariant.REQUIRED, focusRequired, 1_536, 1_460, 31));
    }

    private void assertPartitionGate(Partition partition, SimulationReport report) {
        assertThat(report.completion().status()).as(partition.code())
                .isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.completion().plannedCases()).isEqualTo(partition.expectedCases());
        assertThat(report.metrics().attempts()).isEqualTo(partition.expectedCases());
        assertThat(report.metrics().successfulSets()).isEqualTo(partition.expectedCases());
        assertThat(report.metrics().exhaustedSets()).isZero();
        assertThat(report.metrics().technicalErrors()).isZero();
        assertThat(report.metrics().replayChecks()).isEqualTo(partition.expectedCases());
        assertInvariantCountersAreZero(report);
        assertThat(report.metrics().proposalAttempts().percentile95()).isLessThanOrEqualTo(new BigDecimal("4000"));
        assertThat(report.metrics().proposalAttempts().maximum()).isLessThanOrEqualTo(new BigDecimal("5000"));
        assertThat(count(report, partition.variant().name() + "/STRICT"))
                .isGreaterThanOrEqualTo(partition.minimumStrictSets());
        assertThat(count(report, partition.variant().name() + "/RELAXED_2")).isZero();
        if (partition.variant() == ExclusionVariant.DEFAULT) {
            assertThat(report.metrics().selectedExclusions()).isBetween(768L, 1_075L);
        } else if (partition.variant() == ExclusionVariant.DISABLED) {
            assertThat(report.metrics().selectedExclusions()).isZero();
        } else {
            assertThat(report.metrics().selectedExclusions()).isEqualTo(partition.expectedCases());
        }
        assertThat(report.metrics().randomConceptConcentration().topOneShare())
                .isLessThanOrEqualTo(new BigDecimal("0.05"));
        assertThat(report.metrics().randomConceptConcentration().topTenShare())
                .isLessThanOrEqualTo(new BigDecimal("0.30"));
        assertThat(report.metrics().fingerprintVariation()).hasSize(48).allSatisfy(variation -> {
            assertThat(variation.successfulSets()).isEqualTo(partition.expectedCases() / 48);
            assertThat(variation.distinctFingerprints())
                    .isGreaterThanOrEqualTo(partition.minimumDistinctFingerprints());
        });
        assertThat(report.metrics().omittedFingerprintVariations()).isZero();
    }

    private static void assertInvariantCountersAreZero(SimulationReport report) {
        assertThat(report.metrics().replayIntegrityMismatches()).isZero();
        assertThat(report.metrics().hardRuleViolations()).isZero();
        assertThat(report.metrics().cooldownViolations()).isZero();
        assertThat(report.metrics().rerollViolations()).isZero();
        assertThat(report.metrics().exclusionViolations()).isZero();
        assertThat(report.metrics().quotaViolations()).isZero();
        assertThat(report.metrics().setCapViolations()).isZero();
        assertThat(report.metrics().strictPairMeanViolations()).isZero();
        assertThat(report.metrics().recoveryCadenceViolations()).isZero();
        assertThat(report.metrics().incompleteSuccesses()).isZero();
    }

    private static SimulationScenario sequence(
            String code, long seedStart, HistoryScenario history, int steps, int visiblePosition
    ) {
        List<LocalDate> dates = java.util.stream.IntStream.range(0, steps)
                .mapToObj(step -> LocalDate.of(2026, 9, 2).plusWeeks(step)).toList();
        return new SimulationScenario(code, new SeedRange(seedStart, 8), dates, history, AttemptType.INITIAL,
                List.of(), Set.of(), visiblePosition, ExclusionVariant.DEFAULT);
    }

    private ManualCorpusCase corpus(
            String code, long seed, int month, HistoryScenario history, AttemptType attemptType,
            List<GeneratorLaboratory.ManualInput> manuals, List<Long> rerollBlockedConceptIds
    ) {
        return new ManualCorpusCase(code, new PreviewRequest(attemptType, LocalDate.of(2026, month, 15), seed,
                manuals, history, rerollBlockedConceptIds));
    }

    private GeneratorLaboratory.ManualInput matchedManual(
            int month, int position, String displayText, String conceptCode
    ) {
        return new GeneratorLaboratory.ManualInput(position, displayText, conceptId(month, conceptCode));
    }

    private List<Long> conceptIds(int month, Set<String> conceptCodes) {
        return conceptCodes.stream().sorted().map(code -> conceptId(month, code)).toList();
    }

    private long conceptId(int month, String conceptCode) {
        return catalogProjection.snapshotForMonth(month).conceptByCode(conceptCode)
                .orElseThrow(() -> new IllegalStateException("Missing corpus concept " + conceptCode)).id();
    }

    private static SimulationScenario basic(
            String code, long seedStart, LocalDate date, HistoryScenario history, AttemptType attemptType,
            List<ManualInput> manuals, Set<String> rerollBlock, ExclusionVariant variant, int seedCount,
            int visiblePosition
    ) {
        return new SimulationScenario(code, new SeedRange(seedStart, seedCount), List.of(date), history, attemptType,
                manuals, rerollBlock, visiblePosition, variant);
    }

    private static Partition partition(
            String code, ExclusionVariant variant, List<SimulationScenario> scenarios, int expectedCases,
            int minimumStrictSets, int minimumDistinctFingerprints
    ) {
        assertThat(scenarios.stream().mapToInt(SimulationScenario::plannedCases).sum()).isEqualTo(expectedCases);
        return new Partition(code, variant, new SimulationRequest(SCENARIO_VERSION, scenarios, expectedCases,
                GeneratorSimulation.SimulationControl.unbounded()), expectedCases, minimumStrictSets,
                minimumDistinctFingerprints);
    }

    private static SimulationRequest request(String version, List<SimulationScenario> scenarios) {
        int cases = scenarios.stream().mapToInt(SimulationScenario::plannedCases).sum();
        return new SimulationRequest(version, scenarios, cases, GeneratorSimulation.SimulationControl.unbounded());
    }

    private static long count(SimulationReport report, String key) {
        return report.metrics().fallbackUsage().entries().stream().filter(entry -> entry.key().equals(key))
                .mapToLong(Frequency::count).sum();
    }

    private static Map<String, FingerprintVariation> variations(SimulationReport report) {
        return report.metrics().fingerprintVariation().stream()
                .sorted(Comparator.comparing(FingerprintVariation::scenarioCode))
                .collect(java.util.stream.Collectors.toMap(FingerprintVariation::scenarioCode, value -> value,
                        (left, right) -> left, TreeMap::new));
    }

    private record Partition(
            String code,
            ExclusionVariant variant,
            SimulationRequest request,
            int expectedCases,
            int minimumStrictSets,
            int minimumDistinctFingerprints
    ) {
    }

    private record ManualCorpusCase(String code, PreviewRequest request) {
    }
}
