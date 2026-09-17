package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.SessionParticipant;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExplicitSeeds;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.testsupport.CurrentSchemaPostgresIntegrationTest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

/** Bounded provider-free issue-288 A/B effect report over the unchanged production generator path. */
@EnabledIfSystemProperty(named = "issue288.report", matches = "true")
@SpringBootTest(classes = MiseEnDiceApplication.class)
class CatalogDrawWeightCalibrationReportIntegrationTest extends CurrentSchemaPostgresIntegrationTest {

    private static final String BASE_COMMIT = "c6044088c3ff501261e01e36bdec915f9bfdb1d5";
    private static final String REPORT_VERSION = "ISSUE_288_DRAW_WEIGHT_CALIBRATION_V1";
    private static final Path SOURCE = Path.of("docs/analysis/catalog-draw-weights-source-20260917.jsonl");
    private static final Path DECISIONS = Path.of("docs/analysis/catalog-draw-weights-decisions-20260917.tsv");
    private static final Path OUTPUT = Path.of("target/generator-simulation/catalog-draw-weight-calibration-report.json");
    private static final List<Integer> MONTHS = List.of(2, 8);
    private static final List<Long> CALIBRATION_SEEDS = List.of(288_100_003L, 288_100_181L);
    private static final List<Long> CONTROL_SEEDS = List.of(288_900_029L, 288_900_317L);
    private static final int SCALE = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcParticipantElectorateRepository participantElectorateRepository;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired ObjectMapper objectMapper;

    @Test
    void writesTheDeterministicSameCatalogBeforeAfterEffectReport() throws Exception {
        Decisions decisions = readDecisions();
        Map<Integer, CatalogGeneratorSnapshot> target = snapshots(decisions.targetWeights());
        Map<Integer, CatalogGeneratorSnapshot> baseline = withWeights(target, decisions.currentWeights());
        assertOnlyBaseWeightsDiffer(baseline, target);

        GeneratorConfiguration configuration = generatorProperties.configuration();
        List<SimulationScenario> calibrationScenarios = scenarios("CALIBRATION", CALIBRATION_SEEDS);
        List<SimulationScenario> controlScenarios = scenarios("CONTROL", CONTROL_SEEDS);
        assertThat(calibrationScenarios.stream().mapToInt(value -> value.seedPlan().seeds().size()).sum())
                .isEqualTo(24);
        assertThat(controlScenarios.stream().mapToInt(value -> value.seedPlan().seeds().size()).sum())
                .isEqualTo(24);

        Run beforeCalibration = run("BEFORE_CALIBRATION", baseline, calibrationScenarios, configuration, decisions.groups());
        Run afterCalibration = run("AFTER_CALIBRATION", target, calibrationScenarios, configuration, decisions.groups());
        Run beforeControl = run("BEFORE_CONTROL", baseline, controlScenarios, configuration, decisions.groups());
        Run afterControl = run("AFTER_CONTROL", target, controlScenarios, configuration, decisions.groups());
        for (Run run : List.of(beforeCalibration, afterCalibration, beforeControl, afterControl)) {
            assertThat(run.attempts).isEqualTo(24);
            assertThat(run.successes).isEqualTo(24);
            assertThat(run.exhaustions).isZero();
            assertThat(run.technicalErrors).isZero();
            assertThat(run.hardRuleViolations).isZero();
        }

        Run firstProbe = run("PROBE_1", baseline,
                List.of(scenarios("PROBE", List.of(CONTROL_SEEDS.getFirst())).getFirst()), configuration,
                decisions.groups());
        Run secondProbe = run("PROBE_2", baseline,
                List.of(scenarios("PROBE", List.of(CONTROL_SEEDS.getFirst())).getFirst()), configuration,
                decisions.groups());
        assertThat(firstProbe.document()).isEqualTo(secondProbe.document());

        Map<String, Object> report = map();
        report.put("reportVersion", REPORT_VERSION);
        report.put("base", Map.of(
                "commit", BASE_COMMIT,
                "catalogConcepts", decisions.currentWeights().size(),
                "sourceSha256", sha256(Files.readAllBytes(SOURCE)),
                "decisionSha256", sha256(Files.readAllBytes(DECISIONS))));
        report.put("scope", Map.of(
                "changedSignal", "ingredient_concept.base_draw_weight only",
                "months", MONTHS,
                "casesPerVariantAndSeedSet", 24,
                "calibrationSeeds", CALIBRATION_SEEDS,
                "controlSeeds", CONTROL_SEEDS,
                "configurationVersion", configuration.configurationVersion(),
                "generatorVersion", configuration.generatorVersion(),
                "histories", List.of("EMPTY_HISTORY", "NEUTRAL_HISTORY", "RECOVERY_AFTER_ADVENTUROUS",
                        "SEEKING_AFTER_THREE_FAMILIAR", "LOADED_COOLDOWN_HISTORY"),
                "manualRequirementCounts", List.of(0, 1, 2),
                "restrictionModes", List.of("AUTO", "NONE", "REQUIRED")));
        report.put("calibration", comparison(beforeCalibration, afterCalibration));
        report.put("control", comparison(beforeControl, afterControl));
        report.put("determinismProbe", firstProbe.document());
        report.put("limits", List.of(
                "Provider-free synthetic generator runs are not observed user selection or production frequency.",
                "No production database, Discord connection, or OpenAI connection was used.",
                "The accepted reservoir and final set are observable; rejected proposal internals remain represented by existing reason counters."));
        byte[] bytes = CanonicalSetFingerprint.canonicalBytes(report);
        Files.createDirectories(OUTPUT.getParent());
        Files.write(OUTPUT, bytes);
        assertThat(Files.readString(OUTPUT)).contains(REPORT_VERSION, BASE_COMMIT, "largestFamilyChanges",
                "reservoir", "finalSet", "hardRuleViolations");
    }

    private Run run(
            String label,
            Map<Integer, CatalogGeneratorSnapshot> catalogs,
            List<SimulationScenario> scenarios,
            GeneratorConfiguration configuration,
            Map<String, String> groups
    ) {
        String configurationSnapshot = new CanonicalConfigurationSnapshot(objectMapper).serialize(configuration);
        var proposalEngine = new DefaultCandidateProposalEngine(configuration, configurationSnapshot);
        CandidateReservoirEngine reservoirEngine = new DefaultCandidateReservoirEngine(proposalEngine);
        CandidateSetEngine setEngine = new DefaultCandidateSetEngine(reservoirEngine, objectMapper);
        Run run = new Run(groups);
        for (SimulationScenario scenario : scenarios) {
            CatalogGeneratorSnapshot catalog = catalogs.get(scenario.effectiveDates().getFirst().getMonthValue());
            VisibleHistorySnapshot history = GeneratorLaboratoryScenarios.synthetic(
                    scenario.historyScenario(), scenario.effectiveDates().getFirst(), catalog);
            List<ManualRequirement> manuals = scenario.manualRequirements().stream().map(manual ->
                    new ManualRequirement(manual.position(), manual.displayText(), null)).toList();
            for (long seed : scenario.seedPlan().seeds()) {
                run.attempts++;
                try {
                    var result = GeneratorRunExecution.execute(new GeneratorRunExecution.Input(
                                    scenario.attemptType(), scenario.effectiveDates().getFirst(), seed, manuals,
                                    catalog, history, 1, scenario.restrictionMode()),
                            configuration, reservoirEngine, setEngine).candidateSet();
                    run.recordCommon(result);
                    if (result instanceof GeneratedCandidateSet generated) {
                        run.successes++;
                        run.record(generated, configuration);
                    } else {
                        run.exhaustions++;
                    }
                } catch (RuntimeException exception) {
                    run.technicalErrors++;
                }
            }
        }
        System.out.printf("[issue-288] %s attempts=%d success=%d exhausted=%d hardViolations=%d%n",
                label, run.attempts, run.successes, run.exhaustions, run.hardRuleViolations);
        return run;
    }

    private Map<Integer, CatalogGeneratorSnapshot> snapshots(Map<String, BigDecimal> expectedTargets) {
        List<SessionParticipant> participants = participantElectorateRepository.listDefaultElectorate().stream()
                .map(value -> new SessionParticipant(value.participantId(), value.code())).toList();
        Map<Integer, CatalogGeneratorSnapshot> result = new TreeMap<>();
        for (int month : MONTHS) {
            CatalogGeneratorSnapshot snapshot = catalogProjection.snapshotForMonth(month, participants);
            assertThat(snapshot.concepts()).hasSize(expectedTargets.size());
            snapshot.concepts().forEach(concept -> assertThat(concept.baseDrawWeight())
                    .as("target DB weight for %s", concept.code())
                    .isEqualByComparingTo(expectedTargets.get(concept.code())));
            result.put(month, snapshot);
        }
        return Map.copyOf(result);
    }

    private static Map<Integer, CatalogGeneratorSnapshot> withWeights(
            Map<Integer, CatalogGeneratorSnapshot> source, Map<String, BigDecimal> weights
    ) {
        Map<Integer, CatalogGeneratorSnapshot> result = new TreeMap<>();
        source.forEach((month, snapshot) -> result.put(month, new CatalogGeneratorSnapshot(
                snapshot.seasonMonth(), snapshot.activeParticipantCodes(), snapshot.concepts().stream()
                        .map(concept -> copyWithWeight(concept, weights.get(concept.code()))).toList(),
                snapshot.exclusionRules())));
        return Map.copyOf(result);
    }

    private static GeneratorConcept copyWithWeight(GeneratorConcept concept, BigDecimal weight) {
        if (weight == null) {
            throw new IllegalArgumentException("Missing reviewed weight for " + concept.code());
        }
        return new GeneratorConcept(concept.id(), concept.code(), concept.displayName(), concept.active(),
                concept.randomDrawEnabled(), concept.specificity(), weight, concept.noveltyLevel(),
                concept.functionalRoles(), concept.culinaryFlags(), concept.culinaryDimensions(),
                concept.availabilityByParticipant(), concept.seasonMultiplier(), concept.directAncestorCodes(),
                concept.directDescendantCodes(), concept.transitiveAncestorCodes(), concept.transitiveDescendantCodes());
    }

    private static void assertOnlyBaseWeightsDiffer(
            Map<Integer, CatalogGeneratorSnapshot> before, Map<Integer, CatalogGeneratorSnapshot> after
    ) {
        assertThat(before.keySet()).isEqualTo(after.keySet());
        before.forEach((month, baseline) -> {
            CatalogGeneratorSnapshot target = after.get(month);
            assertThat(baseline.activeParticipantCodes()).isEqualTo(target.activeParticipantCodes());
            assertThat(baseline.exclusionRules()).isEqualTo(target.exclusionRules());
            assertThat(baseline.concepts()).hasSameSizeAs(target.concepts());
            for (int index = 0; index < baseline.concepts().size(); index++) {
                GeneratorConcept left = baseline.concepts().get(index);
                GeneratorConcept right = target.concepts().get(index);
                assertThat(copyWithWeight(left, right.baseDrawWeight())).isEqualTo(right);
            }
        });
    }

    private static List<SimulationScenario> scenarios(String prefix, List<Long> seeds) {
        List<SimulationScenario> values = new ArrayList<>();
        for (int month : MONTHS) {
            LocalDate date = LocalDate.of(2026, month, 15);
            values.add(scenario(prefix + "_" + month + "_EMPTY_AUTO", seeds, date, HistoryScenario.EMPTY_HISTORY,
                    AttemptType.INITIAL, RestrictionMode.AUTO, 0));
            values.add(scenario(prefix + "_" + month + "_NEUTRAL_NONE", seeds, date, HistoryScenario.NEUTRAL_HISTORY,
                    AttemptType.INITIAL, RestrictionMode.NONE, 0));
            values.add(scenario(prefix + "_" + month + "_RECOVERY_REQUIRED", seeds, date,
                    HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, AttemptType.REROLL, RestrictionMode.REQUIRED, 0));
            values.add(scenario(prefix + "_" + month + "_SEEKING_ONE", seeds, date,
                    HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR, AttemptType.INITIAL, RestrictionMode.AUTO, 1));
            values.add(scenario(prefix + "_" + month + "_LOADED_TWO", seeds, date,
                    HistoryScenario.LOADED_COOLDOWN_HISTORY, AttemptType.INITIAL, RestrictionMode.NONE, 2));
            values.add(scenario(prefix + "_" + month + "_NEUTRAL_REROLL_ONE", seeds, date,
                    HistoryScenario.NEUTRAL_HISTORY, AttemptType.REROLL, RestrictionMode.REQUIRED, 1));
        }
        return values;
    }

    private static SimulationScenario scenario(
            String code, List<Long> seeds, LocalDate date, HistoryScenario history, AttemptType attempt,
            RestrictionMode mode, int manuals
    ) {
        List<ManualInput> manualInputs = new ArrayList<>();
        if (manuals >= 1) {
            manualInputs.add(new ManualInput(1, "Synthetic manual ingredient", null));
        }
        if (manuals == 2) {
            manualInputs.add(new ManualInput(2, "Synthetic free-text constraint", null));
        }
        return new SimulationScenario(code, new ExplicitSeeds(seeds), List.of(date), history, attempt,
                manualInputs, 1, mode);
    }

    private static Map<String, Object> comparison(Run before, Run after) {
        Map<String, Object> value = map();
        value.put("before", before.document());
        value.put("after", after.document());
        value.put("delta", Map.of(
                "largestIndividualChanges", largestChanges(before.finalConcepts, after.finalConcepts, 20),
                "largestFamilyChanges", largestChanges(before.finalFamilies, after.finalFamilies, 20),
                "exhaustions", after.exhaustions - before.exhaustions,
                "hardRuleViolations", after.hardRuleViolations - before.hardRuleViolations));
        return value;
    }

    private static List<Map<String, Object>> largestChanges(
            Map<String, Long> before, Map<String, Long> after, int limit
    ) {
        Set<String> keys = new java.util.TreeSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());
        return keys.stream().map(key -> Map.<String, Object>of(
                        "code", key,
                        "before", before.getOrDefault(key, 0L),
                        "after", after.getOrDefault(key, 0L),
                        "delta", after.getOrDefault(key, 0L) - before.getOrDefault(key, 0L)))
                .sorted(Comparator.<Map<String, Object>>comparingLong(value ->
                                Math.abs((Long) value.get("delta"))).reversed()
                        .thenComparing(value -> (String) value.get("code")))
                .limit(limit).toList();
    }

    private static Decisions readDecisions() throws Exception {
        List<String> lines = Files.readAllLines(DECISIONS, StandardCharsets.UTF_8);
        String[] headers = lines.getFirst().split("\t", -1);
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int index = 0; index < headers.length; index++) {
            columns.put(headers[index], index);
        }
        Map<String, BigDecimal> current = new TreeMap<>();
        Map<String, BigDecimal> target = new TreeMap<>();
        Map<String, String> groups = new TreeMap<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] fields = line.split("\t", -1);
            String code = fields[columns.get("concept_code")];
            current.put(code, new BigDecimal(fields[columns.get("current_weight")]));
            target.put(code, new BigDecimal(fields[columns.get("target_weight")]));
            groups.put(code, fields[columns.get("comparison_group")]);
        }
        return new Decisions(Map.copyOf(current), Map.copyOf(target), Map.copyOf(groups));
    }

    private record Decisions(
            Map<String, BigDecimal> currentWeights,
            Map<String, BigDecimal> targetWeights,
            Map<String, String> groups
    ) {
    }

    private static final class Run {
        private final Map<String, String> groups;
        private long attempts;
        private long successes;
        private long exhaustions;
        private long technicalErrors;
        private long hardRuleViolations;
        private final Map<String, Long> reservoirConcepts = new TreeMap<>();
        private final Map<String, Long> reservoirFamilies = new TreeMap<>();
        private final Map<String, Long> finalConcepts = new TreeMap<>();
        private final Map<String, Long> finalFamilies = new TreeMap<>();
        private final Map<String, Long> finalSpecificity = new TreeMap<>();
        private final Map<String, Long> finalRoles = new TreeMap<>();
        private final Map<String, Long> finalProfiles = new TreeMap<>();
        private final Map<String, Long> finalNovelty = new TreeMap<>();
        private final Map<String, Long> finalAvailability = new TreeMap<>();
        private final Map<String, Long> fallbackUsage = new TreeMap<>();
        private final Map<String, Long> reservoirSizeClasses = new TreeMap<>();
        private final Map<String, Long> hardRejections = new TreeMap<>();
        private final Map<String, Long> fallbackRejections = new TreeMap<>();
        private final List<Long> proposalAttempts = new ArrayList<>();
        private final List<Long> reservoirSizes = new ArrayList<>();

        private Run(Map<String, String> groups) {
            this.groups = groups;
        }

        private void recordCommon(CandidateSetEngine.CandidateSetResult result) {
            result.reservoir().metrics().hardRejectionsByReason().forEach((reason, count) ->
                    increment(hardRejections, reason.name(), count));
            result.fallbackAttempts().forEach(attempt -> attempt.rejectionsByReason().forEach((reason, count) ->
                    increment(fallbackRejections, attempt.fallbackLevel() + "/" + reason, count)));
        }

        private void record(GeneratedCandidateSet generated, GeneratorConfiguration configuration) {
            increment(fallbackUsage, generated.fallbackLevel().name(), 1);
            increment(reservoirSizeClasses, generated.reservoir().sizeClass().name(), 1);
            proposalAttempts.add((long) generated.reservoir().metrics().proposalAttempts());
            reservoirSizes.add((long) generated.reservoir().candidates().size());
            generated.reservoir().candidates().forEach(candidate -> recordCandidate(
                    candidate, reservoirConcepts, reservoirFamilies, false));
            generated.candidates().forEach(candidate -> recordCandidate(
                    candidate, finalConcepts, finalFamilies, true));
            validateHardRules(generated, configuration);
        }

        private void recordCandidate(
                AcceptedProposal candidate,
                Map<String, Long> concepts,
                Map<String, Long> families,
                boolean finalStage
        ) {
            if (finalStage) {
                increment(finalProfiles, candidate.profile().name(), 1);
                increment(finalNovelty, "ACTUAL_BAND/" + candidate.evaluation().actualNoveltyBand().name(), 1);
                increment(finalNovelty, "TARGET_BAND/" + candidate.targetNoveltyBand().name(), 1);
            }
            for (RequirementSnapshot requirement : candidate.requirements()) {
                if (requirement.source() != RequirementSource.RANDOM) {
                    continue;
                }
                String code = requirement.concept().code();
                increment(concepts, code, 1);
                increment(families, groups.getOrDefault(code, "UNMAPPED"), 1);
                if (finalStage) {
                    increment(finalSpecificity, requirement.specificity().name(), 1);
                    requirement.concept().functionalRoles().forEach(role -> increment(finalRoles, role, 1));
                    Integer novelty = requirement.concept().noveltyLevel();
                    increment(finalNovelty, "LEVEL/" + (novelty == null ? "NONE" : novelty), 1);
                    requirement.concept().availabilityByParticipant().forEach((participant, availability) ->
                            increment(finalAvailability, participant + "/" + availability.name(), 1));
                }
            }
        }

        private void validateHardRules(GeneratedCandidateSet generated, GeneratorConfiguration configuration) {
            if (generated.candidates().size() != 12
                    || generated.candidates().stream().map(AcceptedProposal::canonicalSignature).distinct().count() != 12
                    || generated.candidates().stream().anyMatch(candidate -> candidate.requirements().size() != 4)) {
                hardRuleViolations++;
            }
            if (generated.fallbackLevel() == FallbackLevel.STRICT
                    && (generated.evaluation().specificity().deviations().values().stream().anyMatch(value -> value != 0)
                    || generated.evaluation().profiles().deviations().values().stream().anyMatch(value -> value != 0)
                    || generated.evaluation().pairStatistics().mean().compareTo(new BigDecimal("0.42")) > 0)) {
                hardRuleViolations++;
            }
            var fallback = configuration.fallbacks().get(generated.fallbackLevel());
            if (generated.evaluation().pairStatistics().maximum().compareTo(fallback.maximumPairSimilarity()) > 0
                    || generated.evaluation().randomConceptUsage().values().stream()
                    .anyMatch(value -> value > fallback.conceptCap())
                    || generated.evaluation().informativeAncestorUsage().values().stream()
                    .anyMatch(value -> value > fallback.ancestorCap())
                    || generated.evaluation().profileUsage().values().stream()
                    .anyMatch(value -> value > fallback.profileCap())
                    || generated.evaluation().difficultCandidateCount() > fallback.difficultCandidateCap()) {
                hardRuleViolations++;
            }
            for (AcceptedProposal candidate : generated.candidates()) {
                for (RequirementSnapshot requirement : candidate.requirements()) {
                    if (requirement.source() == RequirementSource.RANDOM
                            && (requirement.weightEvaluation().cooldownFactor().signum() == 0
                            || requirement.weightEvaluation().diagnostics()
                            .contains(GeneratorReasonCode.EXCLUSION_TARGET_BLOCKED))) {
                        hardRuleViolations++;
                    }
                }
            }
        }

        private Map<String, Object> document() {
            Map<String, Object> value = map();
            value.put("counts", Map.of(
                    "attempts", attempts,
                    "successes", successes,
                    "exhaustions", exhaustions,
                    "technicalErrors", technicalErrors,
                    "hardRuleViolations", hardRuleViolations));
            value.put("proposalPool", Map.of(
                    "proposalAttempts", summary(proposalAttempts),
                    "hardRejectionsByReason", hardRejections));
            value.put("reservoir", Map.of(
                    "acceptedSize", summary(reservoirSizes),
                    "sizeClasses", reservoirSizeClasses,
                    "conceptFrequency", reservoirConcepts,
                    "familyFrequency", reservoirFamilies,
                    "conceptConcentration", concentration(reservoirConcepts),
                    "familyConcentration", concentration(reservoirFamilies)));
            value.put("finalSet", Map.of(
                    "conceptFrequency", finalConcepts,
                    "familyFrequency", finalFamilies,
                    "conceptConcentration", concentration(finalConcepts),
                    "familyConcentration", concentration(finalFamilies),
                    "specificity", finalSpecificity,
                    "roles", finalRoles,
                    "profiles", finalProfiles,
                    "novelty", finalNovelty,
                    "availability", finalAvailability));
            value.put("fallback", Map.of(
                    "usage", fallbackUsage,
                    "rejectionsByReason", fallbackRejections));
            return value;
        }
    }

    private static Map<String, Object> summary(List<Long> values) {
        if (values.isEmpty()) {
            return Map.of("minimum", 0, "mean", BigDecimal.ZERO.setScale(SCALE), "maximum", 0);
        }
        long total = values.stream().mapToLong(Long::longValue).sum();
        return Map.of(
                "minimum", values.stream().mapToLong(Long::longValue).min().orElseThrow(),
                "mean", BigDecimal.valueOf(total).divide(BigDecimal.valueOf(values.size()), SCALE, ROUNDING),
                "maximum", values.stream().mapToLong(Long::longValue).max().orElseThrow());
    }

    private static Map<String, Object> concentration(Map<String, Long> frequencies) {
        long total = frequencies.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return Map.of("top1Share", BigDecimal.ZERO, "top10Share", BigDecimal.ZERO, "hhi", BigDecimal.ZERO);
        }
        List<Long> ordered = frequencies.values().stream().sorted(Comparator.reverseOrder()).toList();
        BigDecimal denominator = BigDecimal.valueOf(total);
        BigDecimal hhi = frequencies.values().stream()
                .map(count -> BigDecimal.valueOf(count).divide(denominator, SCALE, ROUNDING))
                .map(share -> share.multiply(share)).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, ROUNDING);
        return Map.of(
                "top1Share", BigDecimal.valueOf(ordered.getFirst()).divide(denominator, SCALE, ROUNDING),
                "top10Share", BigDecimal.valueOf(ordered.stream().limit(10).mapToLong(Long::longValue).sum())
                        .divide(denominator, SCALE, ROUNDING),
                "hhi", hhi,
                "observations", total);
    }

    private static void increment(Map<String, Long> values, String key, long count) {
        values.merge(key, count, Long::sum);
    }

    private static Map<String, Object> map() {
        return new TreeMap<>();
    }

    private static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }
}
