package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.SessionParticipant;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.CandidateSetResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.Comparability;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration.NoveltyConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ScoreComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.SimilarityComponent;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExplicitSeeds;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Issue-202's bounded offline calibration sample. The class-level condition is deliberately evaluated before
 * Testcontainers, so normal test discovery neither starts PostgreSQL for this class nor executes a calibration case.
 */
@EnabledIfSystemProperty(named = "issue190.report", matches = "true")
@SpringBootTest(classes = MiseEnDiceApplication.class)
class AvailabilityNoveltyCalibrationReportIntegrationTest extends CurrentSchemaPostgresIntegrationTest {
    private static final String BASE_COMMIT = "8b44d103e51505a941396f0d8035039cbb9aa565";
    private static final String REPORT_VERSION = "ISSUE_203_APPLIED_CALIBRATION_RELEASE_QA_V1";
    private static final String SCENARIO_VERSION = "ISSUE_202_CALIBRATION_MATRIX_V1";
    private static final String NOVELTY_AB_SCENARIO_VERSION = "ISSUE_202_NOVELTY_AB_MATRIX_V1";
    private static final String FOCUSED_AVAILABILITY_SCENARIO_VERSION =
            "ISSUE_202_FOCUSED_AVAILABILITY_MATRIX_V1";
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final Path OUTPUT = Path.of("target", "generator-simulation",
            "availability-novelty-calibration-report.json");
    private static final List<Long> SEEDS = List.of(190_202_001L, 190_202_008L);
    private static final List<Integer> SAMPLE_MONTHS = List.of(2, 8);
    private static final List<Integer> MANUAL_MONTHS = SAMPLE_MONTHS;
    private static final Map<NoveltyCadence, HistoryScenario> HISTORIES = Map.of(
            NoveltyCadence.RECOVERY, HistoryScenario.RECOVERY_AFTER_ADVENTUROUS,
            NoveltyCadence.NEUTRAL, HistoryScenario.NEUTRAL_HISTORY,
            NoveltyCadence.SEEKING_VARIETY, HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR);
    private static final List<String> OPERATIONAL_TABLES = List.of(
            "challenge_session", "generation_attempt", "generation_batch", "challenge_candidate",
            "candidate_requirement", "curation_round", "curated_offer_set", "challenge",
            "reroll_offer_exposure");

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcParticipantElectorateRepository participantElectorateRepository;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void writesTheReadOnlyDeterministicCalibrationSampleReport() throws IOException {
        long startedNanos = System.nanoTime();
        GeneratorConfiguration production = generatorProperties.configuration();
        assertProductionConfigurationMatchesTheApprovedRelease(production);

        Map<String, Long> operationalBefore = operationalCounts();
        FrozenCatalog frozenCatalog = materializeCatalog();
        CatalogState catalogBefore = frozenCatalog.state();
        SimulationRequest matrix = calibrationRequest();
        SimulationRequest noveltyAbMatrix = noveltyAbRequest();
        SimulationRequest focusedAvailabilityMatrix = focusedAvailabilityRequest();
        assertThat(matrix.plannedCases()).isEqualTo(96);
        assertThat(noveltyAbMatrix.plannedCases()).isEqualTo(12);
        assertThat(focusedAvailabilityMatrix.plannedCases()).isEqualTo(24);

        Map<String, CalibrationRun> runs = new TreeMap<>();
        Map<String, Long> runtimes = new TreeMap<>();
        ExecutorService variantExecutor = Executors.newFixedThreadPool(Variant.values().length);
        try {
            Map<Variant, CompletableFuture<VariantExecution>> executions = new EnumMap<>(Variant.class);
            for (Variant variant : Variant.values()) {
                executions.put(variant, CompletableFuture.supplyAsync(
                        () -> executeVariant(variant, production, matrix, frozenCatalog.snapshotsByMonth()),
                        variantExecutor));
            }
            for (Variant variant : Variant.values()) {
                VariantExecution execution = executions.get(variant).join();
                runs.put(variant.name(), execution.run());
                runtimes.put(variant.name(), execution.elapsedMillis());
            }
        } finally {
            variantExecutor.shutdownNow();
        }

        Map<String, CalibrationRun> noveltyAbRuns = new TreeMap<>();
        Map<String, Long> noveltyAbRuntimes = new TreeMap<>();
        ExecutorService noveltyExecutor = Executors.newFixedThreadPool(NoveltyVariant.values().length);
        try {
            Map<NoveltyVariant, CompletableFuture<VariantExecution>> executions =
                    new EnumMap<>(NoveltyVariant.class);
            for (NoveltyVariant variant : NoveltyVariant.values()) {
                executions.put(variant, CompletableFuture.supplyAsync(
                        () -> executeNoveltyVariant(variant, production, noveltyAbMatrix,
                                frozenCatalog.snapshotsByMonth()),
                        noveltyExecutor));
            }
            for (NoveltyVariant variant : NoveltyVariant.values()) {
                VariantExecution execution = executions.get(variant).join();
                noveltyAbRuns.put(variant.name(), execution.run());
                noveltyAbRuntimes.put(variant.name(), execution.elapsedMillis());
            }
        } finally {
            noveltyExecutor.shutdownNow();
        }

        Map<String, CalibrationRun> focusedAvailabilityRuns = new TreeMap<>();
        Map<String, Long> focusedAvailabilityRuntimes = new TreeMap<>();
        ExecutorService focusedAvailabilityExecutor =
                Executors.newFixedThreadPool(FocusedAvailabilityVariant.values().length);
        try {
            Map<FocusedAvailabilityVariant, CompletableFuture<VariantExecution>> executions =
                    new EnumMap<>(FocusedAvailabilityVariant.class);
            for (FocusedAvailabilityVariant variant : FocusedAvailabilityVariant.values()) {
                executions.put(variant, CompletableFuture.supplyAsync(
                        () -> executeFocusedAvailabilityVariant(variant, production, focusedAvailabilityMatrix,
                                frozenCatalog.snapshotsByMonth()), focusedAvailabilityExecutor));
            }
            for (FocusedAvailabilityVariant variant : FocusedAvailabilityVariant.values()) {
                VariantExecution execution = executions.get(variant).join();
                focusedAvailabilityRuns.put(variant.name(), execution.run());
                focusedAvailabilityRuntimes.put(variant.name(), execution.elapsedMillis());
            }
        } finally {
            focusedAvailabilityExecutor.shutdownNow();
        }

        CatalogState catalogAfter = materializeCatalog().state();
        Map<String, Long> operationalAfter = operationalCounts();
        assertThat(operationalAfter).isEqualTo(operationalBefore);
        assertThat(catalogAfter.fingerprintsByMonth()).isEqualTo(catalogBefore.fingerprintsByMonth());

        Map<String, Object> canonicalReport = canonicalReport(production, matrix, noveltyAbMatrix,
                focusedAvailabilityMatrix, catalogBefore, operationalBefore, operationalAfter, runs, noveltyAbRuns,
                focusedAvailabilityRuns);
        String canonicalFingerprint = sha256(CanonicalSetFingerprint.canonicalBytes(canonicalReport));
        Map<String, Object> document = map();
        document.put("canonicalFingerprint", canonicalFingerprint);
        document.put("canonicalReport", canonicalReport);
        document.put("runtime", Map.of(
                "elapsedMillisByVariant", runtimes,
                "noveltyAbElapsedMillisByVariant", noveltyAbRuntimes,
                "focusedAvailabilityElapsedMillisByVariant", focusedAvailabilityRuntimes,
                "totalElapsedMillis", elapsedMillis(startedNanos)));
        Files.createDirectories(OUTPUT.getParent());
        Files.write(OUTPUT, CanonicalSetFingerprint.canonicalBytes(document));

        assertThat(Files.readString(OUTPUT)).contains(canonicalFingerprint, REPORT_VERSION,
                "HUMAN_APPROVED_PRODUCTION_CONFIGURATION", "STRONG", "SPECIALTY", "TARGET_FACTOR_REBALANCED",
                "PLANNED_0_15", "targetActualBandComparison", "focusedAvailability");
    }

    private VariantExecution executeVariant(
            Variant variant,
            GeneratorConfiguration production,
            SimulationRequest matrix,
            Map<Integer, CatalogGeneratorSnapshot> catalogsByMonth
    ) {
        GeneratorConfiguration configuration = withAvailabilityFactors(production, variant.factors());
        long variantStarted = System.nanoTime();
        System.out.printf("[issue-202] starting %s (%d cases)%n", variant.name(), matrix.plannedCases());
        CalibrationRun run = run(variant.name(), configuration, matrix, catalogsByMonth);
        long elapsedMillis = elapsedMillis(variantStarted);
        assertComplete(run.report(), matrix.plannedCases());

        CalibrationRun firstProbe = run(variant.name() + "-probe-1", configuration,
                reproducibilityRequest(), catalogsByMonth);
        CalibrationRun secondProbe = run(variant.name() + "-probe-2", configuration,
                reproducibilityRequest(), catalogsByMonth);
        assertThat(firstProbe.canonicalDocument()).isEqualTo(secondProbe.canonicalDocument());
        System.out.printf("[issue-202] completed %s in %d ms%n", variant.name(), elapsedMillis);
        return new VariantExecution(run, elapsedMillis);
    }

    private VariantExecution executeNoveltyVariant(
            NoveltyVariant variant,
            GeneratorConfiguration production,
            SimulationRequest matrix,
            Map<Integer, CatalogGeneratorSnapshot> catalogsByMonth
    ) {
        GeneratorConfiguration cautious = withAvailabilityFactors(production, Variant.CAUTIOUS.factors());
        GeneratorConfiguration configuration = withNovelty(cautious, variant.novelty(cautious.novelty()));
        assertNoveltyAbScope(production, configuration, variant);
        long variantStarted = System.nanoTime();
        System.out.printf("[issue-202] starting novelty A/B %s (%d cases)%n",
                variant.name(), matrix.plannedCases());
        CalibrationRun run = run("NOVELTY_AB_" + variant.name(), configuration, matrix, catalogsByMonth);
        long elapsedMillis = elapsedMillis(variantStarted);
        assertComplete(run.report(), matrix.plannedCases());

        CalibrationRun firstProbe = run("NOVELTY_AB_" + variant.name() + "-probe-1", configuration,
                reproducibilityRequest(), catalogsByMonth);
        CalibrationRun secondProbe = run("NOVELTY_AB_" + variant.name() + "-probe-2", configuration,
                reproducibilityRequest(), catalogsByMonth);
        assertThat(firstProbe.canonicalDocument()).isEqualTo(secondProbe.canonicalDocument());
        System.out.printf("[issue-202] completed novelty A/B %s in %d ms%n", variant.name(), elapsedMillis);
        return new VariantExecution(run, elapsedMillis);
    }

    private VariantExecution executeFocusedAvailabilityVariant(
            FocusedAvailabilityVariant variant,
            GeneratorConfiguration production,
            SimulationRequest matrix,
            Map<Integer, CatalogGeneratorSnapshot> catalogsByMonth
    ) {
        GeneratorConfiguration rebalanced = withNovelty(
                withAvailabilityFactors(production, Variant.CAUTIOUS.factors()),
                NoveltyVariant.TARGET_FACTOR_REBALANCED.novelty(production.novelty()));
        GeneratorConfiguration configuration = withAvailabilityFactors(rebalanced, variant.factors());
        assertFocusedAvailabilityScope(production, configuration, variant);
        long variantStarted = System.nanoTime();
        System.out.printf("[issue-202] starting focused availability %s (%d cases)%n",
                variant.name(), matrix.plannedCases());
        CalibrationRun run = run("FOCUSED_AVAILABILITY_" + variant.name(), configuration, matrix,
                catalogsByMonth);
        long elapsedMillis = elapsedMillis(variantStarted);
        assertComplete(run.report(), matrix.plannedCases());

        CalibrationRun firstProbe = run("FOCUSED_AVAILABILITY_" + variant.name() + "-probe-1", configuration,
                reproducibilityRequest(), catalogsByMonth);
        CalibrationRun secondProbe = run("FOCUSED_AVAILABILITY_" + variant.name() + "-probe-2", configuration,
                reproducibilityRequest(), catalogsByMonth);
        assertThat(firstProbe.canonicalDocument()).isEqualTo(secondProbe.canonicalDocument());
        System.out.printf("[issue-202] completed focused availability %s in %d ms%n",
                variant.name(), elapsedMillis);
        return new VariantExecution(run, elapsedMillis);
    }

    private CalibrationRun run(
            String label,
            GeneratorConfiguration configuration,
            SimulationRequest request,
            Map<Integer, CatalogGeneratorSnapshot> catalogsByMonth
    ) {
        String snapshot = new CanonicalConfigurationSnapshot(objectMapper).serialize(configuration);
        CandidateProposalEngine proposalEngine = new DefaultCandidateProposalEngine(configuration, snapshot);
        CandidateReservoirEngine reservoirEngine = new DefaultCandidateReservoirEngine(proposalEngine);
        CandidateSetEngine setEngine = new DefaultCandidateSetEngine(reservoirEngine, objectMapper);
        CalibrationAggregate aggregate = new CalibrationAggregate();
        DirectRunSummary summary = new DirectRunSummary(request.plannedCases());
        for (SimulationScenario scenario : request.scenarios()) {
            CatalogGeneratorSnapshot catalog = catalogsByMonth.get(
                    scenario.effectiveDates().getFirst().getMonthValue());
            VisibleHistorySnapshot history = GeneratorLaboratoryScenarios.synthetic(
                    scenario.historyScenario(), scenario.effectiveDates().getFirst(), catalog);
            List<ManualRequirement> manuals = scenario.manualRequirements().stream().map(manual ->
                    new ManualRequirement(manual.position(), manual.displayText(), manual.matchedConceptCode() == null
                            ? null : catalog.conceptByCode(manual.matchedConceptCode()).orElseThrow())).toList();
            for (long seed : scenario.seedPlan().seeds()) {
                summary.processed++;
                try {
                    GeneratorRunExecution.Result execution = GeneratorRunExecution.execute(
                            new GeneratorRunExecution.Input(scenario.attemptType(),
                                    scenario.effectiveDates().getFirst(), seed, manuals, catalog, history, 1,
                                    scenario.restrictionMode()),
                            configuration, reservoirEngine, setEngine);
                    if (execution.candidateSet() instanceof GeneratedCandidateSet generated) {
                        summary.successful++;
                        summary.validate(execution.preparedAttempt(), generated);
                        aggregate.record(execution.preparedAttempt(), generated);
                    } else {
                        summary.exhausted++;
                    }
                } catch (RuntimeException exception) {
                    summary.technicalErrors++;
                }
                if (request.plannedCases() > 1 && summary.processed % 32 == 0) {
                    System.out.printf("[issue-202] %s: %d/%d cases%n",
                            label, summary.processed, request.plannedCases());
                }
            }
        }
        return new CalibrationRun(summary, aggregate);
    }

    private static void assertProductionConfigurationMatchesTheApprovedRelease(GeneratorConfiguration current) {
        assertThat(current.generatorVersion()).isEqualTo("1.2.0");
        assertThat(current.configurationVersion()).isEqualTo("2026-09-08.1");
        FocusedAvailabilityVariant.PLANNED_0_22.factors().forEach((availability, expected) ->
                assertThat(current.availabilityFactors().get(availability)).isEqualByComparingTo(expected));
        Map<NoveltyBand, Map<Integer, BigDecimal>> expectedNoveltyFactors =
                NoveltyVariant.TARGET_FACTOR_REBALANCED.novelty(current.novelty()).targetFactors();
        assertThat(current.novelty().targetFactors().keySet())
                .containsExactlyInAnyOrderElementsOf(expectedNoveltyFactors.keySet());
        expectedNoveltyFactors.forEach((band, expectedByLevel) -> {
            Map<Integer, BigDecimal> actualByLevel = current.novelty().targetFactors().get(band);
            assertThat(actualByLevel.keySet()).containsExactlyInAnyOrderElementsOf(expectedByLevel.keySet());
            expectedByLevel.forEach((level, expected) ->
                    assertThat(actualByLevel.get(level)).isEqualByComparingTo(expected));
        });
        assertThat(current.novelty().loadPoints()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 0, 2, 1, 3, 2, 4, 4, 5, 7));
        assertThat(current.novelty().levelFiveCap()).isEqualTo(1);
        assertThat(current.novelty().highLevelCap()).isEqualTo(2);
        assertThat(current.novelty().loadCap()).isEqualTo(11);
    }

    private static void assertComplete(DirectRunSummary report, int plannedCases) {
        assertThat(report.processed).isEqualTo(plannedCases);
        assertThat(report.successful).isEqualTo(plannedCases);
        assertThat(report.exhausted).isZero();
        assertThat(report.technicalErrors).isZero();
        assertThat(report.hardRuleViolations).isZero();
    }

    private static SimulationRequest calibrationRequest() {
        List<SimulationScenario> scenarios = new ArrayList<>();
        ExplicitSeeds seeds = new ExplicitSeeds(SEEDS);
        for (int month : SAMPLE_MONTHS) {
            LocalDate date = LocalDate.of(2026, month, 15);
            for (Map.Entry<NoveltyCadence, HistoryScenario> cadence : orderedHistories()) {
                for (AttemptType attempt : AttemptType.values()) {
                    for (io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode mode
                            : List.of(io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO,
                            io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.NONE)) {
                        scenarios.add(scenario("CORE_%02d_%s_%s_%s".formatted(month, cadence.getKey(), attempt, mode),
                                seeds, date, cadence.getValue(), attempt, List.of(), mode));
                    }
                }
            }
        }
        for (int month : MANUAL_MONTHS) {
            LocalDate date = LocalDate.of(2026, month, 15);
            for (Map.Entry<NoveltyCadence, HistoryScenario> cadence : orderedHistories()) {
                for (AttemptType attempt : AttemptType.values()) {
                    scenarios.add(scenario("MANUAL_ONE_%02d_%s_%s".formatted(month, cadence.getKey(), attempt),
                            seeds, date, cadence.getValue(), attempt,
                            List.of(new ManualInput(1, "Synthetic manual ingredient", null)),
                            io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO));
                    scenarios.add(scenario("MANUAL_TWO_%02d_%s_%s".formatted(month, cadence.getKey(), attempt),
                            seeds, date, cadence.getValue(), attempt,
                            List.of(new ManualInput(1, "Synthetic manual ingredient", null),
                                    new ManualInput(2, "Synthetic free-text constraint", null)),
                            io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO));
                }
            }
        }
        return new SimulationRequest(SCENARIO_VERSION, scenarios, 96,
                GeneratorSimulation.SimulationControl.unbounded());
    }

    private static SimulationRequest reproducibilityRequest() {
        SimulationScenario scenario = scenario("DETERMINISM_PROBE", new ExplicitSeeds(List.of(SEEDS.getFirst())),
                LocalDate.of(2026, 8, 15), HistoryScenario.NEUTRAL_HISTORY, AttemptType.INITIAL, List.of(),
                io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO);
        return new SimulationRequest(SCENARIO_VERSION + "_DETERMINISM", List.of(scenario), 1,
                GeneratorSimulation.SimulationControl.unbounded());
    }

    private static SimulationRequest noveltyAbRequest() {
        List<SimulationScenario> scenarios = new ArrayList<>();
        ExplicitSeeds seeds = new ExplicitSeeds(SEEDS);
        for (int month : SAMPLE_MONTHS) {
            LocalDate date = LocalDate.of(2026, month, 15);
            for (Map.Entry<NoveltyCadence, HistoryScenario> cadence : orderedHistories()) {
                scenarios.add(scenario("NOVELTY_AB_%02d_%s".formatted(month, cadence.getKey()),
                        seeds, date, cadence.getValue(), AttemptType.INITIAL, List.of(),
                        io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.NONE));
            }
        }
        return new SimulationRequest(NOVELTY_AB_SCENARIO_VERSION, scenarios, 12,
                GeneratorSimulation.SimulationControl.unbounded());
    }

    private static SimulationRequest focusedAvailabilityRequest() {
        List<SimulationScenario> scenarios = new ArrayList<>();
        ExplicitSeeds seeds = new ExplicitSeeds(SEEDS);
        for (int month : SAMPLE_MONTHS) {
            LocalDate date = LocalDate.of(2026, month, 15);
            for (Map.Entry<NoveltyCadence, HistoryScenario> cadence : orderedHistories()) {
                for (io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode mode
                        : List.of(io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO,
                        io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.NONE)) {
                    scenarios.add(scenario("FOCUSED_AVAILABILITY_%02d_%s_%s".formatted(month, cadence.getKey(),
                                    mode), seeds, date, cadence.getValue(), AttemptType.INITIAL, List.of(), mode));
                }
            }
        }
        return new SimulationRequest(FOCUSED_AVAILABILITY_SCENARIO_VERSION, scenarios, 24,
                GeneratorSimulation.SimulationControl.unbounded());
    }

    private static List<Map.Entry<NoveltyCadence, HistoryScenario>> orderedHistories() {
        return HISTORIES.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }

    private static SimulationScenario scenario(
            String code,
            ExplicitSeeds seeds,
            LocalDate date,
            HistoryScenario history,
            AttemptType attempt,
            List<ManualInput> manuals,
            io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode restrictionMode
    ) {
        return new SimulationScenario(code, seeds, List.of(date), history, attempt, manuals, 1, restrictionMode);
    }

    private Map<String, Object> canonicalReport(
            GeneratorConfiguration production,
            SimulationRequest matrix,
            SimulationRequest noveltyAbMatrix,
            SimulationRequest focusedAvailabilityMatrix,
            CatalogState catalog,
            Map<String, Long> operationalBefore,
            Map<String, Long> operationalAfter,
            Map<String, CalibrationRun> runs,
            Map<String, CalibrationRun> noveltyAbRuns,
            Map<String, CalibrationRun> focusedAvailabilityRuns
    ) {
        Map<String, Object> root = map();
        Map<String, Object> metadata = map();
        metadata.put("baseCommit", BASE_COMMIT);
        metadata.put("canonicalPayloadVersion", production.canonicalPayloadVersion());
        metadata.put("catalogAvailabilityZeroPathsByMonth", catalog.unavailableDrawableConceptsByMonth());
        metadata.put("catalogFingerprintsByMonth", catalog.fingerprintsByMonth());
        metadata.put("productionAvailabilityFactors", stringFactors(production.availabilityFactors()));
        metadata.put("productionConfigurationFingerprint",
                GeneratorSimulationReportCodec.configurationFingerprint(production));
        metadata.put("productionNovelty", noveltyDocument(production.novelty()));
        metadata.put("configurationVersion", production.configurationVersion());
        metadata.put("decisionGate", "HUMAN_APPROVED_PRODUCTION_CONFIGURATION");
        metadata.put("generatorVersion", production.generatorVersion());
        metadata.put("reportVersion", REPORT_VERSION);
        metadata.put("rngAlgorithm", production.rngAlgorithm().name());
        metadata.put("runCatalogFingerprint",
                GeneratorSimulationReportCodec.runCatalogFingerprint(catalog.fingerprintsByMonth()));
        metadata.put("scenarioVersion", SCENARIO_VERSION);
        root.put("metadata", metadata);

        Map<String, Object> method = map();
        method.put("casesPerVariant", matrix.plannedCases());
        method.put("coreCasesPerVariant", 48);
        method.put("determinismProbeCasesPerVariant", 1);
        method.put("execution", "INDEPENDENT_VARIANT_RUNS_WITH_STRICT_SEQUENTIAL_CASE_LOOPS");
        method.put("manualCasesPerVariant", 48);
        method.put("manualMonths", MANUAL_MONTHS);
        method.put("manualSituations", List.of(
                "ONE_UNCLASSIFIED_MANUAL_AT_POSITION_1",
                "TWO_UNCLASSIFIED_MANUALS_AT_POSITIONS_1_AND_2"));
        method.put("months", SAMPLE_MONTHS);
        method.put("noveltyCadences", List.of("RECOVERY", "NEUTRAL", "SEEKING_VARIETY"));
        method.put("restrictionModesCore", List.of("AUTO", "NONE"));
        method.put("restrictionModesManual", List.of("AUTO"));
        method.put("samplingLimitation",
                "FEBRUARY_AUGUST_AND_TWO_FIXED_SEEDS_BY_EXPLICIT_PROJECT_OWNER_RUNTIME_DECISION");
        method.put("seeds", SEEDS);
        method.put("noveltyAb", Map.of(
                "availabilityVariant", Variant.CAUTIOUS.name(),
                "casesPerVariant", noveltyAbMatrix.plannedCases(),
                "months", SAMPLE_MONTHS,
                "noveltyCadences", List.of("RECOVERY", "NEUTRAL", "SEEKING_VARIETY"),
                "restrictionMode", "NONE",
                "scenarioVersion", NOVELTY_AB_SCENARIO_VERSION,
                "seeds", SEEDS,
                "views", List.of("INITIAL")));
        method.put("focusedAvailability", Map.of(
                "availabilityFactorsFixed", Map.of("DIFFICULT", "0.01", "SPECIALTY", "0.06"),
                "casesPerVariant", focusedAvailabilityMatrix.plannedCases(),
                "months", SAMPLE_MONTHS,
                "noveltyVariant", NoveltyVariant.TARGET_FACTOR_REBALANCED.name(),
                "noveltyCadences", List.of("RECOVERY", "NEUTRAL", "SEEKING_VARIETY"),
                "plannedFactors", List.of("0.30", "0.22", "0.15"),
                "restrictionModes", List.of("AUTO", "NONE"),
                "scenarioVersion", FOCUSED_AVAILABILITY_SCENARIO_VERSION,
                "seeds", SEEDS,
                "views", List.of("INITIAL")));
        method.put("totalCases", matrix.plannedCases() * Variant.values().length
                + noveltyAbMatrix.plannedCases() * NoveltyVariant.values().length
                + focusedAvailabilityMatrix.plannedCases() * FocusedAvailabilityVariant.values().length);
        method.put("views", List.of("INITIAL", "REROLL"));
        root.put("method", method);

        Map<String, Object> configurationVariants = map();
        for (Variant variant : Variant.values()) {
            GeneratorConfiguration configuration = withAvailabilityFactors(production, variant.factors());
            configurationVariants.put(variant.name(), Map.of(
                    "availabilityFactors", stringFactors(variant.factors()),
                    "configurationFingerprint",
                    GeneratorSimulationReportCodec.configurationFingerprint(configuration)));
        }
        root.put("configurationVariants", configurationVariants);

        Map<String, Object> noveltyAbConfigurationVariants = map();
        GeneratorConfiguration cautious = withAvailabilityFactors(production, Variant.CAUTIOUS.factors());
        for (NoveltyVariant variant : NoveltyVariant.values()) {
            GeneratorConfiguration configuration = withNovelty(cautious, variant.novelty(cautious.novelty()));
            noveltyAbConfigurationVariants.put(variant.name(), Map.of(
                    "availabilityFactors", stringFactors(configuration.availabilityFactors()),
                    "configurationFingerprint",
                    GeneratorSimulationReportCodec.configurationFingerprint(configuration),
                    "novelty", noveltyDocument(configuration.novelty())));
        }
        root.put("noveltyAbConfigurationVariants", noveltyAbConfigurationVariants);

        Map<String, Object> focusedAvailabilityConfigurationVariants = map();
        GeneratorConfiguration rebalanced = withNovelty(
                withAvailabilityFactors(production, Variant.CAUTIOUS.factors()),
                NoveltyVariant.TARGET_FACTOR_REBALANCED.novelty(production.novelty()));
        for (FocusedAvailabilityVariant variant : FocusedAvailabilityVariant.values()) {
            GeneratorConfiguration configuration = withAvailabilityFactors(rebalanced, variant.factors());
            focusedAvailabilityConfigurationVariants.put(variant.name(), Map.of(
                    "availabilityFactors", stringFactors(configuration.availabilityFactors()),
                    "configurationFingerprint",
                    GeneratorSimulationReportCodec.configurationFingerprint(configuration),
                    "novelty", noveltyDocument(configuration.novelty())));
        }
        root.put("focusedAvailabilityConfigurationVariants", focusedAvailabilityConfigurationVariants);

        Map<String, Object> variantReports = map();
        runs.forEach((name, run) -> variantReports.put(name, run.canonicalDocument()));
        root.put("variants", variantReports);
        Map<String, Object> noveltyAbReports = map();
        noveltyAbRuns.forEach((name, run) -> noveltyAbReports.put(name, run.canonicalDocument()));
        root.put("noveltyAb", noveltyAbReports);
        Map<String, Object> focusedAvailabilityReports = map();
        focusedAvailabilityRuns.forEach((name, run) -> focusedAvailabilityReports.put(name,
                run.canonicalDocument()));
        root.put("focusedAvailability", focusedAvailabilityReports);
        root.put("readOnlyVerification", Map.of(
                "catalogFingerprintsUnchanged", true,
                "operationalRowCountsAfter", operationalAfter,
                "operationalRowCountsBefore", operationalBefore,
                "operationalRowCountsUnchanged", true,
                "externalCalls", 0,
                "productionConfigurationChanged", false));
        return root;
    }

    private FrozenCatalog materializeCatalog() {
        List<SessionParticipant> electorate = participantElectorateRepository.listDefaultElectorate().stream()
                .map(member -> new SessionParticipant(member.participantId(), member.code())).toList();
        Map<Integer, CatalogGeneratorSnapshot> snapshots = new TreeMap<>();
        Map<Integer, String> fingerprints = new TreeMap<>();
        Map<Integer, Long> unavailable = new TreeMap<>();
        for (int month : SAMPLE_MONTHS) {
            CatalogGeneratorSnapshot snapshot = catalogProjection.snapshotForMonth(month, electorate);
            snapshots.put(month, snapshot);
            fingerprints.put(month, GeneratorSimulationReportCodec.catalogFingerprint(snapshot));
            unavailable.put(month, snapshot.concepts().stream()
                    .filter(GeneratorConcept::active)
                    .filter(GeneratorConcept::randomDrawEnabled)
                    .filter(concept -> !concept.functionalRoles().isEmpty())
                    .filter(concept -> concept.noveltyLevel() != null)
                    .filter(concept -> worstAvailability(concept) == Availability.UNAVAILABLE)
                    .count());
        }
        return new FrozenCatalog(Map.copyOf(snapshots),
                new CatalogState(Map.copyOf(fingerprints), Map.copyOf(unavailable)));
    }

    private Map<String, Long> operationalCounts() {
        Map<String, Long> counts = new TreeMap<>();
        OPERATIONAL_TABLES.forEach(table -> counts.put(table,
                jdbcTemplate.queryForObject("select count(*) from " + table, Long.class)));
        return Map.copyOf(counts);
    }

    private static GeneratorConfiguration withAvailabilityFactors(
            GeneratorConfiguration source,
            Map<Availability, BigDecimal> factors
    ) {
        return new GeneratorConfiguration(source.generatorVersion(), source.configurationVersion(),
                source.rngAlgorithm(), source.canonicalPayloadVersion(), source.candidateSetSize(),
                source.reservoirTarget(), source.reservoirStrictMinimum(), source.reservoirRelaxedOneMinimum(),
                source.maximumProposalAttempts(), source.weightQuantization(), source.exclusionProbability(), factors,
                source.cooldown(), source.exclusion(), source.novelty(), source.anchorRoles(), source.supportRoles(),
                source.flavorRoles(), source.profiles(), source.profileWeights(), source.profileSetTargets(),
                source.specificityWeights(), source.specificitySetTargets(), source.cadenceSetTargets(),
                source.scoreWeights(), source.similarityWeights(), source.similarity(), source.selection(),
                source.fallbacks(), source.processingLease());
    }

    private static GeneratorConfiguration withNovelty(
            GeneratorConfiguration source,
            NoveltyConfiguration novelty
    ) {
        return new GeneratorConfiguration(source.generatorVersion(), source.configurationVersion(),
                source.rngAlgorithm(), source.canonicalPayloadVersion(), source.candidateSetSize(),
                source.reservoirTarget(), source.reservoirStrictMinimum(), source.reservoirRelaxedOneMinimum(),
                source.maximumProposalAttempts(), source.weightQuantization(), source.exclusionProbability(),
                source.availabilityFactors(), source.cooldown(), source.exclusion(), novelty, source.anchorRoles(),
                source.supportRoles(), source.flavorRoles(), source.profiles(), source.profileWeights(),
                source.profileSetTargets(), source.specificityWeights(), source.specificitySetTargets(),
                source.cadenceSetTargets(), source.scoreWeights(), source.similarityWeights(), source.similarity(),
                source.selection(), source.fallbacks(), source.processingLease());
    }

    private static void assertNoveltyAbScope(
            GeneratorConfiguration production,
            GeneratorConfiguration measured,
            NoveltyVariant variant
    ) {
        assertThat(measured.availabilityFactors()).isEqualTo(Variant.CAUTIOUS.factors());
        assertThat(measured.novelty().loadPoints()).isEqualTo(production.novelty().loadPoints());
        assertThat(measured.novelty().levelFiveCap()).isEqualTo(production.novelty().levelFiveCap());
        assertThat(measured.novelty().highLevelCap()).isEqualTo(production.novelty().highLevelCap());
        assertThat(measured.novelty().loadCap()).isEqualTo(production.novelty().loadCap());
        assertThat(measured.novelty().targetFactors())
                .isEqualTo(variant.novelty(production.novelty()).targetFactors());
        assertThat(withAvailabilityFactors(measured, production.availabilityFactors()))
                .isEqualTo(withNovelty(production, measured.novelty()));
    }

    private static void assertFocusedAvailabilityScope(
            GeneratorConfiguration production,
            GeneratorConfiguration measured,
            FocusedAvailabilityVariant variant
    ) {
        assertThat(measured.availabilityFactors()).isEqualTo(variant.factors());
        assertThat(measured.availabilityFactors().get(Availability.SPECIALTY))
                .isEqualByComparingTo(new BigDecimal("0.06"));
        assertThat(measured.availabilityFactors().get(Availability.DIFFICULT))
                .isEqualByComparingTo(new BigDecimal("0.01"));
        assertThat(measured.novelty())
                .isEqualTo(NoveltyVariant.TARGET_FACTOR_REBALANCED.novelty(production.novelty()));
        assertThat(withNovelty(measured, production.novelty()))
                .isEqualTo(withAvailabilityFactors(production, variant.factors()));
    }

    private static Map<String, Object> noveltyDocument(NoveltyConfiguration novelty) {
        Map<String, Object> factors = map();
        novelty.targetFactors().forEach((band, byLevel) -> {
            Map<String, String> values = new TreeMap<>();
            byLevel.forEach((level, factor) -> values.put(Integer.toString(level), factor.toPlainString()));
            factors.put(band.name(), Map.copyOf(values));
        });
        return Map.of(
                "highLevelCap", novelty.highLevelCap(),
                "levelFiveCap", novelty.levelFiveCap(),
                "loadCap", novelty.loadCap(),
                "loadPoints", novelty.loadPoints(),
                "targetFactors", factors);
    }

    private static Map<String, String> stringFactors(Map<Availability, BigDecimal> factors) {
        Map<String, String> result = new TreeMap<>();
        factors.forEach((key, value) -> result.put(key.name(), value.toPlainString()));
        return Map.copyOf(result);
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static Availability worstAvailability(GeneratorConcept concept) {
        return concept.availabilityByParticipant().values().stream()
                .max(Comparator.comparingInt(Enum::ordinal)).orElse(null);
    }

    private static Map<String, Object> map() {
        return new TreeMap<>();
    }

    private enum Variant {
        TRANSITION("1.00", "0.45", "0.15", "0.03", "0.00"),
        CAUTIOUS("1.00", "0.30", "0.06", "0.01", "0.00"),
        STRONG("1.00", "0.22", "0.03", "0.005", "0.00");

        private final Map<Availability, BigDecimal> factors;

        Variant(String easy, String planned, String specialty, String difficult, String unavailable) {
            EnumMap<Availability, BigDecimal> values = new EnumMap<>(Availability.class);
            values.put(Availability.EASY, new BigDecimal(easy));
            values.put(Availability.PLANNED, new BigDecimal(planned));
            values.put(Availability.SPECIALTY, new BigDecimal(specialty));
            values.put(Availability.DIFFICULT, new BigDecimal(difficult));
            values.put(Availability.UNAVAILABLE, new BigDecimal(unavailable));
            factors = Map.copyOf(values);
        }

        Map<Availability, BigDecimal> factors() {
            return factors;
        }
    }

    private enum NoveltyVariant {
        CURRENT,
        TARGET_FACTOR_REBALANCED;

        NoveltyConfiguration novelty(NoveltyConfiguration source) {
            if (this == CURRENT) {
                return new NoveltyConfiguration(source.loadPoints(), Map.of(
                        NoveltyBand.FAMILIAR, factors("1.25", "1.10", "0.70", "0.15", "0.00"),
                        NoveltyBand.BALANCED, factors("0.80", "1.00", "1.20", "0.75", "0.20"),
                        NoveltyBand.ADVENTUROUS, factors("0.35", "0.65", "1.00", "1.30", "1.15")),
                        source.levelFiveCap(), source.highLevelCap(), source.loadCap());
            }
            return new NoveltyConfiguration(source.loadPoints(), Map.of(
                    NoveltyBand.FAMILIAR, factors("1.25", "1.10", "0.70", "0.15", "0.00"),
                    NoveltyBand.BALANCED, factors("0.40", "0.75", "1.50", "1.20", "0.35"),
                    NoveltyBand.ADVENTUROUS, factors("0.05", "0.15", "0.80", "2.00", "2.00")),
                    source.levelFiveCap(), source.highLevelCap(), source.loadCap());
        }

        private static Map<Integer, BigDecimal> factors(
                String one,
                String two,
                String three,
                String four,
                String five
        ) {
            return Map.of(1, new BigDecimal(one), 2, new BigDecimal(two), 3, new BigDecimal(three),
                    4, new BigDecimal(four), 5, new BigDecimal(five));
        }
    }

    private enum FocusedAvailabilityVariant {
        PLANNED_0_30("0.30"),
        PLANNED_0_22("0.22"),
        PLANNED_0_15("0.15");

        private final Map<Availability, BigDecimal> factors;

        FocusedAvailabilityVariant(String planned) {
            EnumMap<Availability, BigDecimal> values = new EnumMap<>(Availability.class);
            values.put(Availability.EASY, new BigDecimal("1.00"));
            values.put(Availability.PLANNED, new BigDecimal(planned));
            values.put(Availability.SPECIALTY, new BigDecimal("0.06"));
            values.put(Availability.DIFFICULT, new BigDecimal("0.01"));
            values.put(Availability.UNAVAILABLE, new BigDecimal("0.00"));
            factors = Map.copyOf(values);
        }

        Map<Availability, BigDecimal> factors() {
            return factors;
        }
    }

    private record CatalogState(
            Map<Integer, String> fingerprintsByMonth,
            Map<Integer, Long> unavailableDrawableConceptsByMonth
    ) {
    }

    private record FrozenCatalog(
            Map<Integer, CatalogGeneratorSnapshot> snapshotsByMonth,
            CatalogState state
    ) {
    }

    private record CalibrationRun(DirectRunSummary report, CalibrationAggregate aggregate) {
        Map<String, Object> canonicalDocument() {
            Map<String, Object> result = map();
            result.put("byCadence", aggregate.cadenceDocuments());
            result.put("byMonth", aggregate.monthDocuments());
            result.put("overall", aggregate.overall().document());
            result.put("simulation", simulationDocument(report));
            return result;
        }

        private static Map<String, Object> simulationDocument(DirectRunSummary report) {
            Map<String, Object> result = map();
            result.put("completion", Map.of(
                    "plannedCases", report.planned,
                    "processedCases", report.processed,
                    "status", report.complete() ? "COMPLETED" : "INCOMPLETE"));
            result.put("invariants", Map.ofEntries(
                    Map.entry("cooldownViolations", report.cooldownViolations),
                    Map.entry("exhaustedSets", report.exhausted),
                    Map.entry("hardRuleViolations", report.hardRuleViolations),
                    Map.entry("incompleteSuccesses", report.incompleteSuccesses),
                    Map.entry("quotaViolations", report.quotaViolations),
                    Map.entry("recoveryCadenceViolations", report.recoveryCadenceViolations),
                    Map.entry("restrictionViolations", report.restrictionViolations),
                    Map.entry("setCapViolations", report.setCapViolations),
                    Map.entry("strictPairMeanViolations", report.strictPairMeanViolations),
                    Map.entry("successfulSets", report.successful),
                    Map.entry("technicalErrors", report.technicalErrors)));
            return result;
        }
    }

    private record VariantExecution(CalibrationRun run, long elapsedMillis) {
    }

    private static final class DirectRunSummary {
        private final int planned;
        private long processed;
        private long successful;
        private long exhausted;
        private long technicalErrors;
        private long hardRuleViolations;
        private long cooldownViolations;
        private long restrictionViolations;
        private long quotaViolations;
        private long setCapViolations;
        private long strictPairMeanViolations;
        private long recoveryCadenceViolations;
        private long incompleteSuccesses;

        private DirectRunSummary(int planned) {
            this.planned = planned;
        }

        private boolean complete() {
            return processed == planned && successful == planned && exhausted == 0 && technicalErrors == 0;
        }

        private void validate(
                io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt prepared,
                GeneratedCandidateSet generated
        ) {
            if (generated.candidates().size() != 12
                    || generated.candidates().stream().map(AcceptedProposal::canonicalSignature).distinct().count() != 12
                    || generated.candidates().stream().anyMatch(candidate -> candidate.requirements().size() != 4)) {
                incompleteSuccesses++;
                hardRuleViolations++;
            }
            if (generated.fallbackLevel()
                    == io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel.STRICT) {
                if (generated.evaluation().specificity().deviations().values().stream().anyMatch(value -> value != 0)
                        || generated.evaluation().profiles().deviations().values().stream()
                        .anyMatch(value -> value != 0)) {
                    quotaViolations++;
                    hardRuleViolations++;
                }
                if (generated.evaluation().pairStatistics().mean().compareTo(new BigDecimal("0.42")) > 0) {
                    strictPairMeanViolations++;
                    hardRuleViolations++;
                }
            }
            var fallback = prepared.request().configuration().fallbacks().get(generated.fallbackLevel());
            if (generated.evaluation().pairStatistics().maximum().compareTo(fallback.maximumPairSimilarity()) > 0
                    || generated.evaluation().randomConceptUsage().values().stream()
                    .anyMatch(value -> value > fallback.conceptCap())
                    || generated.evaluation().informativeAncestorUsage().values().stream()
                    .anyMatch(value -> value > fallback.ancestorCap())
                    || generated.evaluation().profileUsage().values().stream()
                    .anyMatch(value -> value > fallback.profileCap())
                    || generated.evaluation().difficultCandidateCount() > fallback.difficultCandidateCap()) {
                setCapViolations++;
                hardRuleViolations++;
            }
            for (AcceptedProposal candidate : generated.candidates()) {
                for (RequirementSnapshot requirement : candidate.requirements()) {
                    if (requirement.source() == RequirementSource.RANDOM
                            && requirement.weightEvaluation().cooldownFactor().signum() == 0) {
                        cooldownViolations++;
                        hardRuleViolations++;
                    }
                    if (requirement.source() == RequirementSource.RANDOM
                            && requirement.weightEvaluation().diagnostics()
                            .contains(GeneratorReasonCode.EXCLUSION_TARGET_BLOCKED)) {
                        restrictionViolations++;
                        hardRuleViolations++;
                    }
                }
                if (prepared.noveltyCadence() == NoveltyCadence.RECOVERY
                        && candidate.evaluation().actualNoveltyBand() == NoveltyBand.ADVENTUROUS
                        && !candidate.evaluation().reasonCodes().contains(GeneratorReasonCode.MANUAL_NOVELTY_FORCED)) {
                    recoveryCadenceViolations++;
                    hardRuleViolations++;
                }
            }
        }
    }

    private static final class CalibrationAggregate {
        private final Slice overall = new Slice();
        private final Map<Integer, Slice> byMonth = new TreeMap<>();
        private final Map<NoveltyCadence, Slice> byCadence = new EnumMap<>(NoveltyCadence.class);

        private void record(
                io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt attempt,
                GeneratedCandidateSet generated
        ) {
            overall.record(generated);
            byMonth.computeIfAbsent(attempt.request().seasonMonth(), ignored -> new Slice()).record(generated);
            byCadence.computeIfAbsent(attempt.noveltyCadence(), ignored -> new Slice()).record(generated);
        }

        private Slice overall() {
            return overall;
        }

        private Map<String, Object> monthDocuments() {
            Map<String, Object> result = map();
            byMonth.forEach((month, slice) -> result.put("%02d".formatted(month), slice.document()));
            return result;
        }

        private Map<String, Object> cadenceDocuments() {
            Map<String, Object> result = map();
            byCadence.forEach((cadence, slice) -> result.put(cadence.name(), slice.document()));
            return result;
        }
    }

    private static final class Slice {
        private long sets;
        private long candidates;
        private long randomRequirements;
        private long candidatesWithMultipleNoveltyFourOrFive;
        private long requirementsPlannedOrHarderWithNoveltyFourOrFive;
        private final Map<String, Long> fallbackUsage = counts("STRICT", "RELAXED_1", "RELAXED_2");
        private final Map<String, Long> reservoirSizeClasses = counts("LARGE", "MEDIUM", "SMALL", "INSUFFICIENT");
        private final Map<String, Long> availabilityRequirements = counts(
                "NO_MAINTAINED_VALUE", "EASY", "PLANNED", "SPECIALTY", "DIFFICULT", "UNAVAILABLE");
        private final Map<String, Long> candidatesWithAvailability = counts(
                "EASY", "PLANNED", "SPECIALTY", "DIFFICULT", "UNAVAILABLE");
        private final Map<String, Long> noveltyRequirements = counts("1", "2", "3", "4", "5");
        private final Map<String, Long> actualNoveltyBands = counts("FAMILIAR", "BALANCED", "ADVENTUROUS");
        private final Map<String, Long> targetNoveltyBands = counts("FAMILIAR", "BALANCED", "ADVENTUROUS");
        private final Map<String, Long> targetActualNoveltyBandCross = new TreeMap<>();
        private final Map<String, Long> availabilityFactors = new TreeMap<>();
        private final Map<String, Long> availabilityNoveltyCross = new TreeMap<>();
        private final Map<String, Long> hardRejections = new TreeMap<>();
        private final Map<String, Long> fallbackRejections = new TreeMap<>();
        private final Map<String, Long> noveltyThreeByBand = counts("FAMILIAR", "BALANCED", "ADVENTUROUS");
        private final Map<String, Long> randomRequirementsByBand = counts("FAMILIAR", "BALANCED", "ADVENTUROUS");
        private final Map<String, Interaction> interactions = new TreeMap<>();
        private final List<BigDecimal> proposalAttempts = new ArrayList<>();
        private final List<BigDecimal> reservoirFill = new ArrayList<>();
        private final List<BigDecimal> availabilityLoads = new ArrayList<>();
        private final List<BigDecimal> availabilityScores = new ArrayList<>();
        private final List<BigDecimal> availabilitySimilarities = new ArrayList<>();
        private final List<BigDecimal> noveltyLoads = new ArrayList<>();

        private void record(GeneratedCandidateSet set) {
            sets++;
            increment(fallbackUsage, set.fallbackLevel().name());
            increment(reservoirSizeClasses, set.reservoir().sizeClass().name());
            proposalAttempts.add(BigDecimal.valueOf(set.reservoir().metrics().proposalAttempts()));
            reservoirFill.add(BigDecimal.valueOf(set.reservoir().metrics().uniqueAcceptedCandidates())
                    .divide(BigDecimal.valueOf(set.reservoir().context().configuration().reservoirTarget()),
                            SCALE, ROUNDING));
            set.reservoir().metrics().hardRejectionsByReason().forEach((reason, count) ->
                    hardRejections.merge(reason.name(), count, Long::sum));
            set.fallbackAttempts().forEach(attempt -> attempt.rejectionsByReason().forEach((reason, count) ->
                    fallbackRejections.merge(attempt.fallbackLevel().name() + "/" + reason.name(), count, Long::sum)));
            set.evaluation().pairs().forEach(pair -> {
                var component = pair.components().get(SimilarityComponent.AVAILABILITY_LOAD);
                if (component.comparability() == Comparability.COMPARABLE) {
                    availabilitySimilarities.add(component.value());
                }
            });

            for (AcceptedProposal candidate : set.candidates()) {
                candidates++;
                String band = candidate.evaluation().actualNoveltyBand().name();
                String targetBand = candidate.targetNoveltyBand().name();
                increment(actualNoveltyBands, band);
                increment(targetNoveltyBands, targetBand);
                increment(targetActualNoveltyBandCross, targetBand + "/" + band);
                noveltyLoads.add(BigDecimal.valueOf(candidate.evaluation().knownNoveltyLoad()));
                availabilityScores.add(candidate.evaluation().components().get(ScoreComponent.AVAILABILITY_LOAD));
                EnumSet<Availability> present = EnumSet.noneOf(Availability.class);
                int highNovelty = 0;
                BigDecimal load = BigDecimal.ZERO;
                int randomInCandidate = 0;
                for (RequirementSnapshot requirement : candidate.requirements()) {
                    if (requirement.source() != RequirementSource.RANDOM) {
                        continue;
                    }
                    randomRequirements++;
                    randomInCandidate++;
                    GeneratorConcept concept = requirement.concept();
                    Availability availability = worstAvailability(concept);
                    String availabilityKey = availability == null ? "NO_MAINTAINED_VALUE" : availability.name();
                    increment(availabilityRequirements, availabilityKey);
                    if (availability != null) {
                        present.add(availability);
                    }
                    String factor = requirement.weightEvaluation().availabilityFactor().toPlainString();
                    increment(availabilityFactors, factor);
                    int novelty = concept.noveltyLevel();
                    increment(noveltyRequirements, Integer.toString(novelty));
                    increment(availabilityNoveltyCross, availabilityKey + "/N" + novelty);
                    increment(randomRequirementsByBand, band);
                    if (novelty == 3) {
                        increment(noveltyThreeByBand, band);
                    }
                    if (novelty >= 4) {
                        highNovelty++;
                        if (availability == Availability.PLANNED || availability == Availability.SPECIALTY
                                || availability == Availability.DIFFICULT) {
                            requirementsPlannedOrHarderWithNoveltyFourOrFive++;
                        }
                    }
                    load = load.add(BigDecimal.ONE.subtract(requirement.weightEvaluation().availabilityFactor()));
                    interactions.computeIfAbsent(availabilityKey, ignored -> new Interaction())
                            .record(requirement);
                }
                present.forEach(value -> increment(candidatesWithAvailability, value.name()));
                if (highNovelty >= 2) {
                    candidatesWithMultipleNoveltyFourOrFive++;
                }
                if (randomInCandidate > 0) {
                    availabilityLoads.add(load.divide(BigDecimal.valueOf(randomInCandidate), SCALE, ROUNDING));
                }
            }
        }

        private Map<String, Object> document() {
            Map<String, Object> result = map();
            result.put("availability", Map.ofEntries(
                    Map.entry("candidateFrequency", countsAndShares(candidatesWithAvailability, candidates)),
                    Map.entry("factorFrequency", countsAndShares(availabilityFactors, randomRequirements)),
                    Map.entry("load", summary(availabilityLoads)),
                    Map.entry("randomRequirementFrequency", countsAndShares(availabilityRequirements, randomRequirements)),
                    Map.entry("scoreComponent", summary(availabilityScores)),
                    Map.entry("similarityComponent", summary(availabilitySimilarities)),
                    Map.entry("weightedSignalsByLevel", interactionDocuments())));
            result.put("counts", Map.of(
                    "candidates", candidates,
                    "randomRequirements", randomRequirements,
                    "sets", sets));
            result.put("fallback", Map.of(
                    "fallbackRejectionsByReason", ordered(fallbackRejections),
                    "usage", countsAndShares(fallbackUsage, sets)));
            result.put("novelty", Map.ofEntries(
                    Map.entry("actualBandFrequency", countsAndShares(actualNoveltyBands, candidates)),
                    Map.entry("targetActualBandComparison", targetActualBandComparison()),
                    Map.entry("targetActualBandCrossFrequency",
                            countsAndShares(targetActualNoveltyBandCross, candidates)),
                    Map.entry("candidateLoad", summary(noveltyLoads)),
                    Map.entry("candidatesWithMultipleNovelty4Or5", Map.of(
                            "count", candidatesWithMultipleNoveltyFourOrFive,
                            "share", share(candidatesWithMultipleNoveltyFourOrFive, candidates))),
                    Map.entry("level3ByActualBand", noveltyThreeDocuments()),
                    Map.entry("randomRequirementFrequency", countsAndShares(noveltyRequirements, randomRequirements)),
                    Map.entry("targetBandFrequency", countsAndShares(targetNoveltyBands, candidates))));
            result.put("proposalAndReservoir", Map.of(
                    "hardRejectionsByReason", ordered(hardRejections),
                    "proposalAttempts", summary(proposalAttempts),
                    "reservoirFillRatio", summary(reservoirFill),
                    "reservoirSizeClasses", countsAndShares(reservoirSizeClasses, sets)));
            result.put("availabilityNoveltyInteraction", Map.of(
                    "crossFrequency", countsAndShares(availabilityNoveltyCross, randomRequirements),
                    "plannedOrHarderWithNovelty4Or5", Map.of(
                            "count", requirementsPlannedOrHarderWithNoveltyFourOrFive,
                            "share", share(requirementsPlannedOrHarderWithNoveltyFourOrFive, randomRequirements))));
            return result;
        }

        private Map<String, Object> targetActualBandComparison() {
            Map<String, Object> result = map();
            for (NoveltyBand band : NoveltyBand.values()) {
                long target = targetNoveltyBands.get(band.name());
                long actual = actualNoveltyBands.get(band.name());
                result.put(band.name(), Map.of(
                        "actualCount", actual,
                        "actualShare", share(actual, candidates),
                        "gapActualMinusTarget", share(actual - target, candidates),
                        "targetCount", target,
                        "targetShare", share(target, candidates)));
            }
            return result;
        }

        private Map<String, Object> interactionDocuments() {
            Map<String, Object> result = map();
            interactions.forEach((key, value) -> result.put(key, value.document()));
            return result;
        }

        private Map<String, Object> noveltyThreeDocuments() {
            Map<String, Object> result = map();
            for (NoveltyBand band : NoveltyBand.values()) {
                long numerator = noveltyThreeByBand.get(band.name());
                long denominator = randomRequirementsByBand.get(band.name());
                result.put(band.name(), Map.of(
                        "novelty3Count", numerator,
                        "randomRequirementCount", denominator,
                        "share", share(numerator, denominator)));
            }
            return result;
        }
    }

    private static final class Interaction {
        private long count;
        private final List<BigDecimal> baseWeights = new ArrayList<>();
        private final List<BigDecimal> seasonFactors = new ArrayList<>();
        private final List<BigDecimal> cooldownFactors = new ArrayList<>();
        private final List<BigDecimal> noveltyFactors = new ArrayList<>();
        private final List<BigDecimal> effectiveWeights = new ArrayList<>();

        private void record(RequirementSnapshot requirement) {
            count++;
            baseWeights.add(requirement.weightEvaluation().baseWeight());
            seasonFactors.add(requirement.weightEvaluation().seasonFactor());
            cooldownFactors.add(requirement.weightEvaluation().cooldownFactor());
            noveltyFactors.add(requirement.weightEvaluation().noveltyFactor());
            effectiveWeights.add(requirement.weightEvaluation().effectiveWeight());
        }

        private Map<String, Object> document() {
            return Map.of(
                    "baseDrawWeight", summary(baseWeights),
                    "cooldownFactor", summary(cooldownFactors),
                    "count", count,
                    "effectiveWeight", summary(effectiveWeights),
                    "noveltyTargetFactor", summary(noveltyFactors),
                    "seasonFactor", summary(seasonFactors));
        }
    }

    private static Map<String, Long> counts(String... keys) {
        Map<String, Long> result = new TreeMap<>();
        for (String key : keys) {
            result.put(key, 0L);
        }
        return result;
    }

    private static void increment(Map<String, Long> values, String key) {
        values.merge(key, 1L, Long::sum);
    }

    private static Map<String, Long> ordered(Map<String, Long> values) {
        return Map.copyOf(new TreeMap<>(values));
    }

    private static Map<String, Object> countsAndShares(Map<String, Long> counts, long denominator) {
        Map<String, Object> result = map();
        counts.forEach((key, count) -> result.put(key, Map.of(
                "count", count,
                "share", share(count, denominator))));
        return result;
    }

    private static BigDecimal share(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO.setScale(SCALE, ROUNDING)
                : BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), SCALE, ROUNDING);
    }

    private static Map<String, Object> summary(List<BigDecimal> values) {
        if (values.isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            return Map.of("maximum", zero, "mean", zero, "median", zero, "minimum", zero,
                    "percentile95", zero);
        }
        List<BigDecimal> sorted = values.stream().sorted().toList();
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of(
                "maximum", sorted.getLast().setScale(SCALE, ROUNDING),
                "mean", total.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING),
                "median", percentile(sorted, 0.50d),
                "minimum", sorted.getFirst().setScale(SCALE, ROUNDING),
                "percentile95", percentile(sorted, 0.95d));
    }

    private static BigDecimal percentile(List<BigDecimal> sorted, double percentile) {
        return sorted.get((int) Math.ceil(sorted.size() * percentile) - 1).setScale(SCALE, ROUNDING);
    }
}
