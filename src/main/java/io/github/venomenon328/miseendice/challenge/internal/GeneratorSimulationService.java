package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.CandidateSetResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Completion;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.CompletionStatus;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Concentration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FingerprintVariation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Frequency;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.FrequencyList;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metadata;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.Metrics;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.NumericSummary;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.TechnicalErrorMode;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Phase-9E2 implementation: one frozen read snapshot, then only sequential pure generator calls. */
@Service
class GeneratorSimulationService implements GeneratorSimulation {
    private static final int BATCH_NUMBER = 1;
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private final CatalogGeneratorProjection catalogProjection;
    private final JdbcGenerationRepository repository;
    private final CandidateReservoirEngine defaultReservoirEngine;
    private final CandidateSetEngine defaultSetEngine;
    private final GeneratorProperties properties;
    private final TransactionTemplate repeatableReadTransaction;

    GeneratorSimulationService(
            CatalogGeneratorProjection catalogProjection,
            JdbcGenerationRepository repository,
            CandidateReservoirEngine defaultReservoirEngine,
            CandidateSetEngine defaultSetEngine,
            GeneratorProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.catalogProjection = catalogProjection;
        this.repository = repository;
        this.defaultReservoirEngine = defaultReservoirEngine;
        this.defaultSetEngine = defaultSetEngine;
        this.properties = properties;
        this.repeatableReadTransaction = new TransactionTemplate(transactionManager);
        this.repeatableReadTransaction.setReadOnly(true);
        this.repeatableReadTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public SimulationReport simulate(SimulationRequest request) {
        // The record validates the caller cap; keeping this check here makes the application boundary fail fast too.
        if (request.plannedCases() > MAXIMUM_CASES) {
            throw new IllegalArgumentException("Simulation exceeds the hard 4096-case application bound");
        }
        long startedNanos = System.nanoTime();
        MaterializedInputs inputs = materialize(request);
        validateFrozenReferences(request, inputs.catalogsByMonth());

        Aggregates aggregates = new Aggregates();
        CompletionStatus completionStatus = CompletionStatus.COMPLETED;
        String completionDetail = "All planned cases were processed sequentially.";
        boolean stop = false;

        for (SimulationScenario scenario : request.scenarios()) {
            if (stop) {
                break;
            }
            for (long seed : scenario.seedPlan().seeds()) {
                if (stop) {
                    break;
                }
                VisibleHistorySnapshot history = initialHistory(scenario, inputs);
                boolean sequenceComplete = true;
                for (int step = 0; step < scenario.effectiveDates().size(); step++) {
                    CompletionStatus controlStatus = controlStatus(request);
                    if (controlStatus != null) {
                        completionStatus = controlStatus;
                        completionDetail = controlStatus == CompletionStatus.TIMED_OUT
                                ? "The deadline was reached between simulation cases."
                                : "The caller requested an abort between simulation cases.";
                        sequenceComplete = false;
                        stop = true;
                        break;
                    }

                    LocalDate date = scenario.effectiveDates().get(step);
                    CatalogGeneratorSnapshot catalog = inputs.catalogsByMonth().get(date.getMonthValue());
                    GeneratorRunExecution.Input runInput = input(scenario, date, seed, catalog, history);
                    try {
                        aggregates.attempts++;
                        GeneratorRunExecution.Result execution = GeneratorRunExecution.execute(
                                runInput, properties.configuration(), defaultReservoirEngine, defaultSetEngine);
                        CandidateSetResult result = execution.candidateSet();
                        aggregates.recordCommon(result);
                        if (result instanceof GeneratedCandidateSet generated) {
                            aggregates.recordSuccess(scenario, execution.preparedAttempt(), generated);
                            verifyFrozenReplay(runInput, generated, aggregates);
                            history = appendSyntheticExposure(history, scenario, seed, step, date, generated);
                        } else if (result instanceof ExhaustedCandidateSet) {
                            // Exhaustion is a processed domain result. It creates no exposure, but later steps still run.
                            aggregates.exhaustedSets++;
                        }
                    } catch (RuntimeException exception) {
                        // Deliberately do not reinterpret unknown implementation/JDBC errors as generator exhaustion.
                        aggregates.technicalErrors++;
                        sequenceComplete = false;
                        if (request.control().technicalErrorMode() == TechnicalErrorMode.FAIL_FAST) {
                            completionStatus = CompletionStatus.ABORTED;
                            completionDetail = "A technical generator error stopped the run: "
                                    + exception.getClass().getSimpleName();
                            stop = true;
                        }
                        break;
                    }
                }
                if (sequenceComplete) {
                    aggregates.completedSequences++;
                } else {
                    aggregates.incompleteSequences++;
                }
            }
        }

        if (completionStatus == CompletionStatus.COMPLETED && aggregates.incompleteSequences > 0) {
            completionStatus = CompletionStatus.INCOMPLETE;
            completionDetail = "One or more sequences ended after a technical error.";
        }

        Metadata metadata = metadata(request, inputs);
        Completion completion = new Completion(completionStatus, request.plannedCases(), (int) aggregates.attempts,
                request.plannedCases() - (int) aggregates.attempts, aggregates.completedSequences,
                aggregates.incompleteSequences, completionDetail);
        Metrics metrics = aggregates.toMetrics();
        String canonicalFingerprint = GeneratorSimulationReportCodec.canonicalFingerprint(metadata, completion, metrics);
        long elapsedMillis = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
        return new SimulationReport(metadata, completion, metrics, canonicalFingerprint, elapsedMillis);
    }

    private MaterializedInputs materialize(SimulationRequest request) {
        return repeatableReadTransaction.execute(status -> {
            TreeMap<Integer, CatalogGeneratorSnapshot> catalogs = new TreeMap<>();
            request.scenarios().stream().flatMap(scenario -> scenario.effectiveDates().stream())
                    .map(LocalDate::getMonthValue).distinct().sorted()
                    .forEach(month -> catalogs.put(month, catalogProjection.snapshotForMonth(month)));
            VisibleHistorySnapshot production = request.scenarios().stream()
                    .anyMatch(scenario -> scenario.historyScenario() == HistoryScenario.PRODUCTION_VISIBLE)
                    ? repository.visibleHistory() : VisibleHistorySnapshot.empty();
            return new MaterializedInputs(Map.copyOf(catalogs), production);
        });
    }

    private void validateFrozenReferences(SimulationRequest request, Map<Integer, CatalogGeneratorSnapshot> catalogs) {
        for (SimulationScenario scenario : request.scenarios()) {
            for (LocalDate date : scenario.effectiveDates()) {
                CatalogGeneratorSnapshot catalog = catalogs.get(date.getMonthValue());
                scenario.manualRequirements().stream().filter(manual -> manual.matchedConceptCode() != null)
                        .forEach(manual -> requireConcept(catalog, manual.matchedConceptCode(), "manual match"));
            }
        }
    }

    private static void requireConcept(CatalogGeneratorSnapshot catalog, String code, String use) {
        if (catalog.conceptByCode(code).isEmpty()) {
            throw new IllegalArgumentException("Frozen catalog month " + catalog.seasonMonth()
                    + " does not contain " + use + " concept " + code);
        }
    }

    private VisibleHistorySnapshot initialHistory(SimulationScenario scenario, MaterializedInputs inputs) {
        if (scenario.historyScenario() == HistoryScenario.PRODUCTION_VISIBLE) {
            return inputs.productionHistory();
        }
        CatalogGeneratorSnapshot firstCatalog = inputs.catalogsByMonth()
                .get(scenario.effectiveDates().getFirst().getMonthValue());
        return GeneratorLaboratoryScenarios.synthetic(scenario.historyScenario(), scenario.effectiveDates().getFirst(),
                firstCatalog);
    }

    private static GeneratorRunExecution.Input input(
            SimulationScenario scenario,
            LocalDate date,
            long seed,
            CatalogGeneratorSnapshot catalog,
            VisibleHistorySnapshot history
    ) {
        List<ManualRequirement> manuals = scenario.manualRequirements().stream().map(manual ->
                new ManualRequirement(manual.position(), manual.displayText(), manual.matchedConceptCode() == null
                        ? null : catalog.conceptByCode(manual.matchedConceptCode()).orElseThrow())).toList();
        return new GeneratorRunExecution.Input(scenario.attemptType(), date, seed, manuals,
                catalog, history, BATCH_NUMBER, scenario.restrictionMode());
    }

    private void verifyFrozenReplay(
            GeneratorRunExecution.Input input,
            GeneratedCandidateSet generated,
            Aggregates aggregates
    ) {
        aggregates.replayChecks++;
        try {
            GeneratorRunExecution.Result replay = GeneratorRunExecution.execute(
                    input, properties.configuration(), defaultReservoirEngine, defaultSetEngine);
            if (!(replay.candidateSet() instanceof GeneratedCandidateSet replayed)
                    || !generated.fingerprint().equals(replayed.fingerprint())
                    || !signatures(generated).equals(signatures(replayed))) {
                aggregates.replayIntegrityMismatches++;
            }
        } catch (RuntimeException exception) {
            aggregates.technicalErrors++;
        }
    }

    private static List<String> signatures(GeneratedCandidateSet set) {
        return set.candidates().stream().map(AcceptedProposal::canonicalSignature).toList();
    }

    private static VisibleHistorySnapshot appendSyntheticExposure(
            VisibleHistorySnapshot history,
            SimulationScenario scenario,
            long seed,
            int step,
            LocalDate date,
            GeneratedCandidateSet generated
    ) {
        Instant visibleAt = date.atTime(12, 0).toInstant(ZoneOffset.UTC);
        if (!history.cooldownExposuresNewestFirst().isEmpty()
                && !visibleAt.isAfter(history.cooldownExposuresNewestFirst().getFirst().visibleAt())) {
            throw new IllegalArgumentException("Sequence date " + date
                    + " is not after the frozen visible-history exposure it would extend");
        }
        AcceptedProposal selected = generated.candidates().get(scenario.visibleCandidatePosition() - 1);
        List<VisibleRequirement> requirements = selected.requirements().stream().map(requirement -> {
            GeneratorConcept concept = requirement.concept();
            String code = concept == null ? "SIMULATION_MANUAL_" + requirement.position() : concept.code();
            return new VisibleRequirement(code, concept == null ? null : concept.noveltyLevel(),
                    concept == null ? Set.of() : concept.functionalRoles(),
                    concept == null ? Set.of() : concept.culinaryFlags(),
                    concept == null ? Set.of() : concept.transitiveAncestorCodes());
        }).toList();
        String restrictionRuleCode = selected.restriction().ruleCode();
        VisibleChallenge exposure = new VisibleChallenge(visibleAt,
                "simulation/" + scenario.code() + "/" + seed + "/" + step,
                scenario.attemptType(), "COMPLETED", requirements, selected.profile(),
                selected.evaluation().actualNoveltyBand(), restrictionRuleCode);
        List<VisibleChallenge> extended = new ArrayList<>(history.challengesNewestFirst().size() + 1);
        extended.add(exposure);
        extended.addAll(history.challengesNewestFirst());
        return new VisibleHistorySnapshot(extended, history.rerollExposuresNewestFirst());
    }

    private CompletionStatus controlStatus(SimulationRequest request) {
        if (request.control().abortRequested().getAsBoolean()) {
            return CompletionStatus.ABORTED;
        }
        return request.control().deadline() != null && !Instant.now().isBefore(request.control().deadline())
                ? CompletionStatus.TIMED_OUT : null;
    }

    private Metadata metadata(SimulationRequest request, MaterializedInputs inputs) {
        GeneratorConfiguration defaultConfiguration = properties.configuration();
        TreeMap<Integer, String> catalogFingerprints = new TreeMap<>();
        inputs.catalogsByMonth().forEach((month, catalog) ->
                catalogFingerprints.put(month, GeneratorSimulationReportCodec.catalogFingerprint(catalog)));
        TreeMap<String, String> configurationFingerprints = new TreeMap<>();
        configurationFingerprints.put("CURRENT", GeneratorSimulationReportCodec.configurationFingerprint(defaultConfiguration));
        List<String> descriptions = request.scenarios().stream().map(scenario -> scenario.code()
                + ";history=" + scenario.historyScenario().name()
                + ";attempt=" + scenario.attemptType().name()
                + ";restrictionMode=" + scenario.restrictionMode().name()
                + ";seeds=" + seedDescription(scenario.seedPlan())
                + ";dates=" + scenario.effectiveDates()).toList();
        return new Metadata(REPORT_VERSION, defaultConfiguration.generatorVersion(),
                defaultConfiguration.configurationVersion(), defaultConfiguration.rngAlgorithm().name(),
                defaultConfiguration.canonicalPayloadVersion(), request.scenarioVersion(), catalogFingerprints,
                GeneratorSimulationReportCodec.runCatalogFingerprint(catalogFingerprints), configurationFingerprints,
                descriptions);
    }

    private static String seedDescription(GeneratorSimulation.SeedPlan seedPlan) {
        if (seedPlan instanceof GeneratorSimulation.SeedRange range) {
            return range.startSeed() + "+" + range.count();
        }
        return "explicit:" + seedPlan.seeds();
    }

    private record MaterializedInputs(
            Map<Integer, CatalogGeneratorSnapshot> catalogsByMonth,
            VisibleHistorySnapshot productionHistory
    ) {
    }

    private static final class Aggregates {
        private long attempts;
        private long successfulSets;
        private long exhaustedSets;
        private long technicalErrors;
        private long replayChecks;
        private long replayIntegrityMismatches;
        private long hardRuleViolations;
        private long cooldownViolations;
        private long restrictionViolations;
        private long quotaViolations;
        private long setCapViolations;
        private long strictPairMeanViolations;
        private long recoveryCadenceViolations;
        private long incompleteSuccesses;
        private long restrictedCandidates;
        private int completedSequences;
        private int incompleteSequences;
        private final Map<String, Long> fallbackUsage = new TreeMap<>();
        private final Map<String, Long> hardRejections = new TreeMap<>();
        private final Map<String, Long> fallbackRejections = new TreeMap<>();
        private final Map<String, Long> concepts = new TreeMap<>();
        private final Map<String, Long> roles = new TreeMap<>();
        private final Map<String, Long> profiles = new TreeMap<>();
        private final Map<String, Long> targetNoveltyBands = new TreeMap<>();
        private final Map<String, Long> actualNoveltyBands = new TreeMap<>();
        private final Map<String, Long> specificities = new TreeMap<>();
        private final Map<String, Long> restrictions = new TreeMap<>();
        private final Map<String, Long> ancestors = new TreeMap<>();
        private final List<BigDecimal> proposalAttempts = new ArrayList<>();
        private final List<BigDecimal> noveltyLoads = new ArrayList<>();
        private final List<BigDecimal> availabilityLoads = new ArrayList<>();
        private final List<BigDecimal> confidences = new ArrayList<>();
        private final List<BigDecimal> pairMeans = new ArrayList<>();
        private final List<BigDecimal> pairPercentile95s = new ArrayList<>();
        private final List<BigDecimal> pairMaximums = new ArrayList<>();
        private final List<BigDecimal> difficultCandidates = new ArrayList<>();
        private final Map<String, Set<String>> fingerprintsByScenario = new TreeMap<>();
        private final Map<String, Integer> successesByScenario = new TreeMap<>();

        private void recordCommon(CandidateSetResult result) {
            result.reservoir().metrics().hardRejectionsByReason().forEach((reason, count) ->
                    increment(hardRejections, reason.name(), count));
            result.fallbackAttempts().forEach(attempt -> attempt.rejectionsByReason().forEach((reason, count) ->
                    increment(fallbackRejections, attempt.fallbackLevel().name() + "/" + reason.name(), count)));
        }

        private void recordSuccess(
                SimulationScenario scenario,
                io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt prepared,
                GeneratedCandidateSet generated
        ) {
            successfulSets++;
            increment(fallbackUsage, generated.fallbackLevel().name(), 1);
            proposalAttempts.add(BigDecimal.valueOf(generated.reservoir().metrics().proposalAttempts()));
            pairMeans.add(generated.evaluation().pairStatistics().mean());
            pairPercentile95s.add(generated.evaluation().pairStatistics().percentile95());
            pairMaximums.add(generated.evaluation().pairStatistics().maximum());
            difficultCandidates.add(BigDecimal.valueOf(generated.evaluation().difficultCandidateCount()));
            generated.evaluation().informativeAncestorUsage().forEach((code, count) -> increment(ancestors, code, count));
            fingerprintsByScenario.computeIfAbsent(scenario.code(), ignored -> new HashSet<>()).add(generated.fingerprint());
            successesByScenario.merge(scenario.code(), 1, Integer::sum);

            if (generated.candidates().size() != 12
                    || generated.candidates().stream().map(AcceptedProposal::canonicalSignature).distinct().count() != 12
                    || generated.candidates().stream().anyMatch(candidate -> candidate.requirements().size() != 4)) {
                incompleteSuccesses++;
                hardRuleViolations++;
            }
            if (generated.fallbackLevel() == FallbackLevel.STRICT) {
                // Actual novelty distribution remains a soft target outside the separate Recovery hard rule.
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
                if (candidate.restriction().ruleCode() != null) {
                    restrictedCandidates++;
                    increment(restrictions, candidate.restriction().ruleCode(), 1);
                }
                increment(profiles, candidate.profile().name(), 1);
                increment(targetNoveltyBands, candidate.targetNoveltyBand().name(), 1);
                increment(actualNoveltyBands, candidate.evaluation().actualNoveltyBand().name(), 1);
                int specificity = (int) candidate.requirements().stream()
                        .filter(requirement -> requirement.specificity()
                                == io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSpecificity.SPECIFIC)
                        .count();
                increment(specificities, Integer.toString(specificity), 1);
                noveltyLoads.add(BigDecimal.valueOf(candidate.evaluation().knownNoveltyLoad()));
                confidences.add(candidate.evaluation().dataConfidence());
                List<RequirementSnapshot> randomRequirements = candidate.requirements().stream()
                        .filter(requirement -> requirement.source() == RequirementSource.RANDOM).toList();
                if (!randomRequirements.isEmpty()) {
                    BigDecimal availabilityLoad = randomRequirements.stream()
                            .map(RequirementSnapshot::weightEvaluation)
                            .map(weight -> BigDecimal.ONE.subtract(weight.availabilityFactor()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(randomRequirements.size()), SCALE, ROUNDING);
                    availabilityLoads.add(availabilityLoad);
                }
                for (RequirementSnapshot requirement : randomRequirements) {
                    increment(concepts, requirement.concept().code(), 1);
                    requirement.concept().functionalRoles().forEach(role -> increment(roles, role, 1));
                    // Invariant counters consume the existing selection/weight diagnostics; no second rule engine lives here.
                    if (requirement.weightEvaluation().cooldownFactor().signum() == 0) {
                        cooldownViolations++;
                        hardRuleViolations++;
                    }
                    if (requirement.weightEvaluation().diagnostics()
                            .contains(GeneratorReasonCode.EXCLUSION_TARGET_BLOCKED)) {
                        restrictionViolations++;
                        hardRuleViolations++;
                    }
                }
                if (prepared.noveltyCadence()
                        == io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence.RECOVERY
                        && candidate.evaluation().actualNoveltyBand() == NoveltyBand.ADVENTUROUS
                        && !candidate.evaluation().reasonCodes().contains(GeneratorReasonCode.MANUAL_NOVELTY_FORCED)) {
                    recoveryCadenceViolations++;
                    hardRuleViolations++;
                }
            }
        }

        private Metrics toMetrics() {
            List<FingerprintVariation> variations = successesByScenario.entrySet().stream()
                    .map(entry -> new FingerprintVariation(entry.getKey(), entry.getValue(),
                            fingerprintsByScenario.getOrDefault(entry.getKey(), Set.of()).size()))
                    .sorted(Comparator.comparing(FingerprintVariation::scenarioCode))
                    .toList();
            long omitted = Math.max(0, variations.size() - MAXIMUM_REPORT_ENTRIES);
            if (omitted > 0) {
                variations = variations.subList(0, MAXIMUM_REPORT_ENTRIES);
            }
            return new Metrics(attempts, successfulSets, exhaustedSets, technicalErrors, replayChecks,
                    replayIntegrityMismatches, hardRuleViolations, cooldownViolations, restrictionViolations,
                    quotaViolations, setCapViolations, strictPairMeanViolations,
                    recoveryCadenceViolations, incompleteSuccesses, restrictedCandidates, frequencies(fallbackUsage),
                    frequencies(hardRejections), frequencies(fallbackRejections), frequencies(concepts), concentration(concepts),
                    frequencies(roles),
                    frequencies(profiles), frequencies(targetNoveltyBands), frequencies(actualNoveltyBands),
                    frequencies(specificities), frequencies(restrictions), frequencies(ancestors), summary(proposalAttempts),
                    summary(noveltyLoads), summary(availabilityLoads), summary(confidences), summary(pairMeans),
                    summary(pairPercentile95s), summary(pairMaximums), summary(difficultCandidates), variations, omitted);
        }

        private static void increment(Map<String, Long> values, String key, long count) {
            values.merge(key, count, Long::sum);
        }

        private static FrequencyList frequencies(Map<String, Long> values) {
            List<Frequency> all = values.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue).reversed()
                            .thenComparing(Map.Entry::getKey))
                    .map(entry -> new Frequency(entry.getKey(), entry.getValue())).toList();
            long omitted = Math.max(0, all.size() - MAXIMUM_REPORT_ENTRIES);
            return new FrequencyList(omitted == 0 ? all : all.subList(0, MAXIMUM_REPORT_ENTRIES), omitted);
        }

        private static NumericSummary summary(List<BigDecimal> values) {
            if (values.isEmpty()) {
                BigDecimal zero = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                return new NumericSummary(zero, zero, zero);
            }
            List<BigDecimal> sorted = values.stream().sorted().toList();
            BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING);
            BigDecimal percentile95 = sorted.get((int) Math.ceil(values.size() * 0.95d) - 1)
                    .setScale(SCALE, ROUNDING);
            return new NumericSummary(mean, percentile95, sorted.getLast().setScale(SCALE, ROUNDING));
        }

        private static Concentration concentration(Map<String, Long> frequencies) {
            long slots = frequencies.values().stream().mapToLong(Long::longValue).sum();
            if (slots == 0) {
                return new Concentration(BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                        BigDecimal.ZERO.setScale(SCALE, ROUNDING), 0);
            }
            List<Long> counts = frequencies.values().stream().sorted(Comparator.reverseOrder()).toList();
            BigDecimal denominator = BigDecimal.valueOf(slots);
            return new Concentration(BigDecimal.valueOf(counts.getFirst()).divide(denominator, SCALE, ROUNDING),
                    BigDecimal.valueOf(counts.stream().limit(10).mapToLong(Long::longValue).sum())
                            .divide(denominator, SCALE, ROUNDING), slots);
        }
    }
}
