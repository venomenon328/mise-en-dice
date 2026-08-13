package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.AttemptExclusionDecision;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Explicit issue-47 2,304-attempt baseline using twelve once-materialized PostgreSQL snapshots.
 * Excluded from normal verify; run deliberately with the generator-baseline Maven profile.
 */
@SpringBootTest(properties = "logging.level.root=WARN")
@Testcontainers
class CandidateSetBaselineIntegrationTest {
    private static final String FIXTURE_VERSION = "ISSUE_47_V1";
    private static final long DEFAULT_SEED_START = 47_000_000L;
    private static final long FOCUS_SEED_START = 47_100_000L;
    private static final List<String> DEFAULT_FIXTURES = List.of("EMPTY_INITIAL", "NEUTRAL_HISTORY",
            "RECOVERY_AFTER_ADVENTUROUS", "SEEKING_AFTER_THREE_FAMILIAR", "LOADED_COOLDOWN_HISTORY",
            "REROLL_EXACT_BLOCK", "ONE_MATCHED_MANUAL", "TWO_MIXED_MANUALS");
    private static final List<String> FOCUS_FIXTURES = List.of("EMPTY_INITIAL", "NEUTRAL_HISTORY",
            "LOADED_COOLDOWN_HISTORY", "REROLL_EXACT_BLOCK");

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice").withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired CandidateReservoirEngine reservoirEngine;
    @Autowired CandidateSetEngine setEngine;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Engines> variantEngines = new ConcurrentHashMap<>();

    @Test
    void completeIssue47MatrixReplaysAndPassesEveryQualityGate() throws IOException {
        Map<Integer, CatalogGeneratorSnapshot> snapshots = new HashMap<>();
        IntStream.rangeClosed(1, 12).forEach(month -> snapshots.put(month, catalogProjection.snapshotForMonth(month)));
        Metrics metrics = new Metrics();
        List<Case> cases = new ArrayList<>(2_304);
        for (int month = 1; month <= 12; month++) {
            for (int fixtureIndex = 0; fixtureIndex < DEFAULT_FIXTURES.size(); fixtureIndex++) {
                String fixture = DEFAULT_FIXTURES.get(fixtureIndex);
                for (int seed = 0; seed < 16; seed++) {
                    long offset = ((month - 1L) * DEFAULT_FIXTURES.size() + fixtureIndex) * 16L + seed;
                    cases.add(new Case("DEFAULT", month, fixture, DEFAULT_SEED_START + offset, null));
                }
            }
            for (int fixtureIndex = 0; fixtureIndex < FOCUS_FIXTURES.size(); fixtureIndex++) {
                String fixture = FOCUS_FIXTURES.get(fixtureIndex);
                List<BigDecimal> probabilities = List.of(BigDecimal.ZERO, BigDecimal.ONE);
                for (int probabilityIndex = 0; probabilityIndex < probabilities.size(); probabilityIndex++) {
                    BigDecimal probability = probabilities.get(probabilityIndex);
                    for (int seed = 0; seed < 8; seed++) {
                        long offset = (((month - 1L) * FOCUS_FIXTURES.size() + fixtureIndex)
                                * probabilities.size() + probabilityIndex) * 8L + seed;
                        cases.add(new Case(probability.signum() == 0 ? "EXCLUSION_OFF" : "EXCLUSION_ON",
                                month, fixture, FOCUS_SEED_START + offset, probability));
                    }
                }
            }
        }
        assertThat(cases).hasSize(2_304);

        cases.parallelStream().forEach(testCase -> run(testCase, snapshots.get(testCase.month()), metrics));

        BaselineReport report = metrics.report();
        writeReports(report);
        assertGates(report);
    }

    private void run(Case testCase, CatalogGeneratorSnapshot catalog, Metrics metrics) {
        GeneratorConfiguration configuration = testCase.exclusionProbability() == null
                ? generatorProperties.configuration()
                : withExclusionProbability(generatorProperties.configuration(), testCase.exclusionProbability());
        Engines engines = engines(configuration);
        GenerationAttemptRequest request = request(testCase, catalog, configuration);
        PreparedGenerationAttempt prepared = engines.reservoir().prepare(request);
        var result = engines.set().generate(prepared, 1);
        metrics.attempts.increment();
        if (prepared.exclusionDecision() instanceof AttemptExclusionDecision.Selected) {
            metrics.exclusions.computeIfAbsent(testCase.variant(), ignored -> new LongAdder()).increment();
        }
        result.fallbackAttempts().forEach(attempt -> attempt.rejectionsByReason().forEach((reason, count) ->
                metrics.fallbackRejections.computeIfAbsent(
                        testCase.variant() + "/" + attempt.fallbackLevel() + "/" + reason,
                        ignored -> new LongAdder()).add(count)));
        if (testCase.variant().equals("DEFAULT") && result.reservoir() instanceof GeneratedReservoir generatedReservoir) {
            Map<NoveltyBand, Long> actual = generatedReservoir.candidates().stream().collect(
                    java.util.stream.Collectors.groupingBy(candidate -> candidate.evaluation().actualNoveltyBand(),
                            () -> new EnumMap<>(NoveltyBand.class), java.util.stream.Collectors.counting()));
            generatedReservoir.candidates().forEach(candidate -> metrics.noveltyTransitions
                    .computeIfAbsent(testCase.fixture() + "/" + candidate.targetNoveltyBand() + "->"
                            + candidate.evaluation().actualNoveltyBand(), ignored -> new LongAdder()).increment());
            boolean anyShortfall = false;
            for (Map.Entry<NoveltyBand, Integer> entry : generatedReservoir.plan().novelty().setTargets().entrySet()) {
                if (actual.getOrDefault(entry.getKey(), 0L) < entry.getValue()) {
                    anyShortfall = true;
                    metrics.noveltyShortfallsByFixtureAndBand
                            .computeIfAbsent(testCase.fixture() + "/" + entry.getKey(), ignored -> new LongAdder())
                            .increment();
                }
            }
            if (anyShortfall) {
                metrics.defaultReservoirNoveltyShortfalls.increment();
            }
        }
        if (!(result instanceof GeneratedCandidateSet generated)) {
            metrics.exhaustions.increment();
            return;
        }
        var replay = engines.set().generate(engines.reservoir().prepare(request), 1);
        metrics.replays.increment();
        if (!result.equals(replay)) {
            metrics.replayMismatches.increment();
        }
        metrics.successes.increment();
        metrics.proposalAttempts.add(generated.reservoir().metrics().proposalAttempts());
        metrics.proposalAttemptValues.add(generated.reservoir().metrics().proposalAttempts());
        metrics.fallbacks.computeIfAbsent(testCase.variant() + "/" + generated.fallbackLevel(),
                ignored -> new LongAdder()).increment();
        metrics.maximumPair.accumulateAndGet(generated.evaluation().pairStatistics().maximum(),
                (left, right) -> left.compareTo(right) >= 0 ? left : right);
        metrics.pairMeanSum.add(generated.evaluation().pairStatistics().mean().doubleValue());
        if (testCase.variant().equals("DEFAULT")) {
            metrics.fingerprints.computeIfAbsent(testCase.month() + "/" + testCase.fixture(),
                    ignored -> ConcurrentHashMap.newKeySet()).add(generated.fingerprint());
        }
        verifySet(testCase, generated, metrics);
    }

    private Engines engines(GeneratorConfiguration configuration) {
        if (configuration.equals(generatorProperties.configuration())) {
            return new Engines(reservoirEngine, setEngine);
        }
        return variantEngines.computeIfAbsent(configuration.exclusionProbability().toPlainString(), ignored -> {
            var proposal = new DefaultCandidateProposalEngine(configuration,
                    new CanonicalConfigurationSnapshot(objectMapper).serialize(configuration));
            CandidateReservoirEngine reservoir = new DefaultCandidateReservoirEngine(proposal);
            return new Engines(reservoir, new DefaultCandidateSetEngine(reservoir, objectMapper));
        });
    }

    private void verifySet(Case testCase, GeneratedCandidateSet generated, Metrics metrics) {
        if (generated.candidates().size() != 12
                || generated.candidates().stream().map(AcceptedProposal::canonicalSignature).distinct().count() != 12
                || generated.candidates().stream().anyMatch(candidate -> candidate.requirements().size() != 4)) {
            metrics.incompleteSuccesses.increment();
        }
        if (generated.fallbackLevel() == FallbackLevel.STRICT) {
            if (generated.evaluation().specificity().deviations().values().stream().anyMatch(value -> value != 0)
                    || generated.evaluation().profiles().deviations().values().stream().anyMatch(value -> value != 0)) {
                metrics.quotaViolations.increment();
            }
            if (generated.evaluation().pairStatistics().mean().compareTo(new BigDecimal("0.42")) > 0) {
                metrics.strictPairMeanViolations.increment();
            }
        }
        var fallback = generated.reservoir().context().configuration().fallbacks().get(generated.fallbackLevel());
        if (generated.evaluation().pairStatistics().maximum().compareTo(fallback.maximumPairSimilarity()) > 0
                || generated.evaluation().randomConceptUsage().values().stream().anyMatch(v -> v > fallback.conceptCap())
                || generated.evaluation().informativeAncestorUsage().values().stream().anyMatch(v -> v > fallback.ancestorCap())
                || generated.evaluation().profileUsage().values().stream().anyMatch(v -> v > fallback.profileCap())
                || generated.evaluation().difficultCandidateCount() > fallback.difficultCandidateCap()) {
            metrics.capViolations.increment();
        }
        Set<String> randomCodes = new HashSet<>();
        for (AcceptedProposal candidate : generated.candidates()) {
            for (var requirement : candidate.requirements()) {
                if (requirement.source() != RequirementSource.RANDOM) {
                    continue;
                }
                randomCodes.add(requirement.concept().code());
                metrics.randomSlots.increment();
                metrics.conceptFrequency.computeIfAbsent(requirement.concept().code(), ignored -> new LongAdder())
                        .increment();
                if (generated.reservoir().context().rerollBlockedConceptCodes().contains(requirement.concept().code())
                        || generated.reservoir().context().visibleHistory().challengesNewestFirst().stream().limit(6)
                        .flatMap(challenge -> challenge.requirements().stream())
                        .anyMatch(visible -> requirement.concept().code().equals(visible.conceptCode()))
                        || generated.reservoir().context().exclusionDecision()
                        instanceof AttemptExclusionDecision.Selected selected
                        && selected.rule().expandedTargetCodes().contains(requirement.concept().code())) {
                    metrics.hardRuleViolations.increment();
                }
            }
        }
        if (testCase.fixture().equals("RECOVERY_AFTER_ADVENTUROUS")
                && generated.candidates().stream().anyMatch(candidate ->
                candidate.evaluation().actualNoveltyBand() == NoveltyBand.ADVENTUROUS
                        && !candidate.evaluation().reasonCodes().contains(GeneratorReasonCode.MANUAL_NOVELTY_FORCED))) {
            metrics.cadenceViolations.increment();
        }
    }

    private GenerationAttemptRequest request(Case testCase, CatalogGeneratorSnapshot catalog,
                                             GeneratorConfiguration configuration) {
        Fixture fixture = fixture(testCase.fixture(), catalog);
        return new GenerationAttemptRequest(fixture.attemptType(), LocalDate.of(2026, testCase.month(), 12),
                testCase.month(), catalog, fixture.history(), fixture.manuals(), fixture.rerollBlock(), configuration,
                testCase.seed());
    }

    private Fixture fixture(String name, CatalogGeneratorSnapshot catalog) {
        List<GeneratorConcept> drawable = catalog.concepts().stream()
                .filter(concept -> concept.active() && concept.randomDrawEnabled()).toList();
        return switch (name) {
            case "EMPTY_INITIAL" -> new Fixture(AttemptType.INITIAL, VisibleHistorySnapshot.empty(), List.of(), Set.of());
            case "NEUTRAL_HISTORY" -> new Fixture(AttemptType.INITIAL,
                    history(drawable, List.of(NoveltyBand.BALANCED, NoveltyBand.FAMILIAR)), List.of(), Set.of());
            case "RECOVERY_AFTER_ADVENTUROUS" -> new Fixture(AttemptType.INITIAL,
                    history(drawable, List.of(NoveltyBand.ADVENTUROUS)), List.of(), Set.of());
            case "SEEKING_AFTER_THREE_FAMILIAR" -> new Fixture(AttemptType.INITIAL,
                    history(drawable, List.of(NoveltyBand.FAMILIAR, NoveltyBand.FAMILIAR, NoveltyBand.FAMILIAR)),
                    List.of(), Set.of());
            case "LOADED_COOLDOWN_HISTORY" -> new Fixture(AttemptType.INITIAL,
                    history(drawable, List.of(NoveltyBand.BALANCED, NoveltyBand.FAMILIAR,
                            NoveltyBand.BALANCED, NoveltyBand.FAMILIAR, NoveltyBand.BALANCED, NoveltyBand.FAMILIAR)),
                    List.of(), Set.of());
            case "REROLL_EXACT_BLOCK" -> new Fixture(AttemptType.REROLL, VisibleHistorySnapshot.empty(), List.of(),
                    drawable.stream().limit(4).map(GeneratorConcept::code).collect(java.util.stream.Collectors.toSet()));
            case "ONE_MATCHED_MANUAL" -> {
                GeneratorConcept match = drawable.stream().filter(c -> c.functionalRoles().contains("VEGETABLE"))
                        .findFirst().orElseThrow();
                yield new Fixture(AttemptType.INITIAL, VisibleHistorySnapshot.empty(),
                        List.of(new ManualRequirement(1, match.displayName(), match)), Set.of());
            }
            case "TWO_MIXED_MANUALS" -> {
                GeneratorConcept match = drawable.stream().filter(c -> c.functionalRoles().contains("ANIMAL_PROTEIN"))
                        .findFirst().orElseThrow();
                yield new Fixture(AttemptType.INITIAL, VisibleHistorySnapshot.empty(), List.of(
                        new ManualRequirement(1, match.displayName(), match),
                        new ManualRequirement(2, "Use a waffle iron", null)), Set.of());
            }
            default -> throw new IllegalArgumentException("Unknown fixture " + name);
        };
    }

    private VisibleHistorySnapshot history(List<GeneratorConcept> concepts, List<NoveltyBand> bands) {
        List<VisibleChallenge> challenges = new ArrayList<>();
        for (int index = 0; index < bands.size(); index++) {
            NoveltyBand band = bands.get(index);
            List<GeneratorConcept> requirements = concepts.stream().skip(index * 4L).limit(4).toList();
            List<VisibleRequirement> snapshots = requirements.stream().map(concept -> new VisibleRequirement(
                    concept.code(), band == NoveltyBand.ADVENTUROUS && concept == requirements.getFirst()
                    ? 5 : band == NoveltyBand.FAMILIAR ? 1 : 3,
                    concept.functionalRoles(), concept.culinaryFlags(), concept.transitiveAncestorCodes())).toList();
            challenges.add(new VisibleChallenge(Instant.parse("2026-08-11T12:00:00Z").minusSeconds(index * 86_400L),
                    "fixture-" + index, AttemptType.INITIAL, "COMPLETED", snapshots,
                    CandidateProfile.FLEXIBLE_BALANCED, band, null));
        }
        return new VisibleHistorySnapshot(challenges);
    }

    private GeneratorConfiguration withExclusionProbability(GeneratorConfiguration value, BigDecimal probability) {
        return new GeneratorConfiguration(value.generatorVersion(), value.configurationVersion(), value.rngAlgorithm(),
                value.canonicalPayloadVersion(), value.candidateSetSize(), value.reservoirTarget(),
                value.reservoirStrictMinimum(), value.reservoirRelaxedOneMinimum(), value.maximumProposalAttempts(),
                value.weightQuantization(), probability, value.availabilityFactors(), value.cooldown(), value.exclusion(),
                value.novelty(), value.anchorRoles(), value.supportRoles(), value.flavorRoles(), value.profiles(),
                value.profileWeights(), value.profileSetTargets(), value.specificityWeights(), value.specificitySetTargets(),
                value.cadenceSetTargets(), value.scoreWeights(), value.similarityWeights(), value.similarity(),
                value.selection(), value.fallbacks(), value.processingLease());
    }

    private void assertGates(BaselineReport report) {
        assertThat(report.attempts()).isEqualTo(2_304);
        assertThat(report.successes()).isEqualTo(2_304);
        assertThat(report.replays()).isEqualTo(report.successes());
        assertThat(report.exhaustions()).isZero();
        assertThat(report.replayMismatches()).isZero();
        assertThat(report.incompleteSuccesses()).isZero();
        assertThat(report.hardRuleViolations()).isZero();
        assertThat(report.quotaViolations()).isZero();
        assertThat(report.capViolations()).isZero();
        assertThat(report.strictPairMeanViolations()).isZero();
        assertThat(report.cadenceViolations()).isZero();
        assertThat(report.defaultStrictRate()).isGreaterThanOrEqualTo(new BigDecimal("0.95"));
        assertThat(report.defaultRelaxedOneRate()).isLessThanOrEqualTo(new BigDecimal("0.05"));
        assertThat(report.defaultRelaxedTwoRate()).isZero();
        assertThat(report.proposalP95()).isLessThanOrEqualTo(4_000);
        assertThat(report.proposalMaximum()).isLessThanOrEqualTo(5_000);
        assertThat(report.defaultExclusionRate()).isBetween(new BigDecimal("0.25"), new BigDecimal("0.35"));
        assertThat(report.exclusionOffRate()).isZero();
        assertThat(report.exclusionOnRate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(report.topOneConceptShare()).isLessThanOrEqualTo(new BigDecimal("0.05"));
        assertThat(report.topTenConceptShare()).isLessThanOrEqualTo(new BigDecimal("0.30"));
    }

    private void writeReports(BaselineReport report) throws IOException {
        Path directory = Path.of("target", "candidate-generator-baseline");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("issue-47-baseline.json"), objectMapper.writeValueAsString(report));
        Files.writeString(directory.resolve("issue-47-baseline.txt"), """
                Issue #47 candidate-set baseline
                fixtureVersion=%s configurationVersion=%s
                attempts=%d successes=%d replays=%d
                fallbackRates strict=%s relaxed1=%s relaxed2=%s
                exclusions default=%s off=%s on=%s
                proposals p95=%d max=%d
                pairMean=%s pairMaximum=%s
                concentration top1=%s top10=%s
                variation=%s
                defaultReservoirNoveltyShortfalls=%d
                noveltyShortfallsByFixtureAndBand=%s
                noveltyTransitions=%s
                fallbackRejections=%s
                syntheticCoverage=%s
                """.formatted(report.fixtureVersion(), report.configurationVersion(), report.attempts(),
                report.successes(), report.replays(), report.defaultStrictRate(), report.defaultRelaxedOneRate(),
                report.defaultRelaxedTwoRate(), report.defaultExclusionRate(), report.exclusionOffRate(),
                report.exclusionOnRate(), report.proposalP95(), report.proposalMaximum(), report.pairMean(),
                report.pairMaximum(), report.topOneConceptShare(), report.topTenConceptShare(),
                report.fingerprintVariation(), report.defaultReservoirNoveltyShortfalls(),
                report.noveltyShortfallsByFixtureAndBand(), report.noveltyTransitions(),
                report.fallbackRejections(), report.syntheticCoverage()));
    }

    private record Case(String variant, int month, String fixture, long seed, BigDecimal exclusionProbability) { }
    private record Fixture(AttemptType attemptType, VisibleHistorySnapshot history,
                           List<ManualRequirement> manuals, Set<String> rerollBlock) { }
    private record Engines(CandidateReservoirEngine reservoir, CandidateSetEngine set) { }

    private final class Metrics {
        final LongAdder attempts = new LongAdder();
        final LongAdder successes = new LongAdder();
        final LongAdder replays = new LongAdder();
        final LongAdder exhaustions = new LongAdder();
        final LongAdder replayMismatches = new LongAdder();
        final LongAdder incompleteSuccesses = new LongAdder();
        final LongAdder hardRuleViolations = new LongAdder();
        final LongAdder quotaViolations = new LongAdder();
        final LongAdder capViolations = new LongAdder();
        final LongAdder strictPairMeanViolations = new LongAdder();
        final LongAdder cadenceViolations = new LongAdder();
        final LongAdder proposalAttempts = new LongAdder();
        final java.util.concurrent.ConcurrentLinkedQueue<Integer> proposalAttemptValues =
                new java.util.concurrent.ConcurrentLinkedQueue<>();
        final ConcurrentHashMap<String, LongAdder> fallbacks = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, LongAdder> fallbackRejections = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, LongAdder> exclusions = new ConcurrentHashMap<>();
        final LongAdder defaultReservoirNoveltyShortfalls = new LongAdder();
        final ConcurrentHashMap<String, LongAdder> noveltyShortfallsByFixtureAndBand = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, LongAdder> noveltyTransitions = new ConcurrentHashMap<>();
        final LongAdder randomSlots = new LongAdder();
        final ConcurrentHashMap<String, LongAdder> conceptFrequency = new ConcurrentHashMap<>();
        final ConcurrentHashMap<String, Set<String>> fingerprints = new ConcurrentHashMap<>();
        final java.util.concurrent.atomic.AtomicReference<BigDecimal> maximumPair =
                new java.util.concurrent.atomic.AtomicReference<>(BigDecimal.ZERO);
        final java.util.concurrent.atomic.DoubleAdder pairMeanSum = new java.util.concurrent.atomic.DoubleAdder();

        BaselineReport report() {
            List<Integer> proposalValues = proposalAttemptValues.stream().sorted().toList();
            int p95 = proposalValues.get((int) Math.ceil(proposalValues.size() * 0.95d) - 1);
            int maximum = proposalValues.getLast();
            long defaultAttempts = 1_536L;
            long strict = count(FallbackLevel.STRICT);
            long relaxedOne = count(FallbackLevel.RELAXED_1);
            long relaxedTwo = count(FallbackLevel.RELAXED_2);
            List<Long> frequencies = conceptFrequency.values().stream().map(LongAdder::sum)
                    .sorted(Comparator.reverseOrder()).toList();
            BigDecimal topOne = ratio(frequencies.getFirst(), randomSlots.sum());
            BigDecimal topTen = ratio(frequencies.stream().limit(10).mapToLong(Long::longValue).sum(), randomSlots.sum());
            Map<String, BigDecimal> variation = new java.util.TreeMap<>();
            fingerprints.forEach((key, values) -> variation.put(key,
                    BigDecimal.valueOf(values.size()).divide(BigDecimal.valueOf(16), 12, RoundingMode.HALF_EVEN)));
            Map<String, Long> rejectionCounts = counts(fallbackRejections);
            Map<String, Long> shortfallCounts = counts(noveltyShortfallsByFixtureAndBand);
            Map<String, Long> transitionCounts = counts(noveltyTransitions);
            Map<String, String> syntheticCoverage = Map.of(
                    "DIFFICULT", "CandidateSetEngineTest: DIFFICULT cap causes typed exhaustion",
                    "EXHAUSTION", "CandidateSetEngineTest: score floor never returns a partial success",
                    "MISSING_DIMENSIONS", "CandidateSimilarityCalculatorTest: optional weight redistribution",
                    "THIN_POOL", "CandidateSetEngineTest: 12/36/72 reservoir start levels");
            return new BaselineReport(FIXTURE_VERSION, generatorProperties.configuration().configurationVersion(),
                    DEFAULT_SEED_START, DEFAULT_SEED_START + 1_535, FOCUS_SEED_START, FOCUS_SEED_START + 767,
                    attempts.intValue(), successes.intValue(), replays.intValue(), exhaustions.intValue(),
                    replayMismatches.intValue(),
                    incompleteSuccesses.intValue(), hardRuleViolations.intValue(), quotaViolations.intValue(),
                    capViolations.intValue(), strictPairMeanViolations.intValue(), cadenceViolations.intValue(),
                    ratio(strict, defaultAttempts), ratio(relaxedOne, defaultAttempts), ratio(relaxedTwo, defaultAttempts),
                    ratio(exclusions.getOrDefault("DEFAULT", new LongAdder()).sum(), defaultAttempts),
                    ratio(exclusions.getOrDefault("EXCLUSION_OFF", new LongAdder()).sum(), 384),
                    ratio(exclusions.getOrDefault("EXCLUSION_ON", new LongAdder()).sum(), 384),
                    p95, maximum, BigDecimal.valueOf(pairMeanSum.sum())
                    .divide(BigDecimal.valueOf(successes.sum()), 12, RoundingMode.HALF_EVEN),
                    maximumPair.get(), topOne, topTen, variation, defaultReservoirNoveltyShortfalls.intValue(),
                    shortfallCounts, transitionCounts, rejectionCounts, syntheticCoverage);
        }

        private Map<String, Long> counts(ConcurrentHashMap<String, LongAdder> source) {
            Map<String, Long> result = new java.util.TreeMap<>();
            source.forEach((key, value) -> result.put(key, value.sum()));
            return result;
        }

        private long count(FallbackLevel level) {
            return fallbacks.getOrDefault("DEFAULT/" + level, new LongAdder()).sum();
        }

        private BigDecimal ratio(long value, long total) {
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(total), 12, RoundingMode.HALF_EVEN);
        }
    }

    private record BaselineReport(
            String fixtureVersion, String configurationVersion,
            long defaultSeedStart, long defaultSeedEnd, long focusSeedStart, long focusSeedEnd,
            int attempts, int successes, int replays, int exhaustions, int replayMismatches, int incompleteSuccesses,
            int hardRuleViolations, int quotaViolations, int capViolations, int strictPairMeanViolations,
            int cadenceViolations, BigDecimal defaultStrictRate, BigDecimal defaultRelaxedOneRate,
            BigDecimal defaultRelaxedTwoRate, BigDecimal defaultExclusionRate, BigDecimal exclusionOffRate,
            BigDecimal exclusionOnRate, int proposalP95, int proposalMaximum, BigDecimal pairMean,
            BigDecimal pairMaximum, BigDecimal topOneConceptShare, BigDecimal topTenConceptShare,
            Map<String, BigDecimal> fingerprintVariation, int defaultReservoirNoveltyShortfalls,
            Map<String, Long> noveltyShortfallsByFixtureAndBand, Map<String, Long> noveltyTransitions,
            Map<String, Long> fallbackRejections, Map<String, String> syntheticCoverage
    ) { }
}
