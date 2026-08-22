package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.AcceptedProposal;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine.RequirementSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.CandidateSetResult;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.FallbackLevel;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/**
 * Reproduces the Issue-152 before/after calibration from one frozen PostgreSQL catalog per run.
 * It deliberately only substitutes the availability map and its configuration version.
 */
@SpringBootTest(classes = MiseEnDiceApplication.class)
@Testcontainers
class AvailabilityWeightCalibrationReportIntegrationTest {
    private static final String SCENARIO_VERSION = "ISSUE_152_AVAILABILITY_CALIBRATION_V1";
    private static final Path OUTPUT = Path.of("target", "generator-simulation",
            "availability-weight-calibration-report.json");
    private static final List<LocalDate> SEASONAL_DATES = List.of(
            LocalDate.of(2026, 2, 12), LocalDate.of(2026, 5, 14),
            LocalDate.of(2026, 8, 13), LocalDate.of(2026, 11, 12));

    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_availability_calibration")
            .withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcGenerationRepository generationRepository;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @EnabledIfSystemProperty(named = "issue152.report", matches = "true")
    void writesReproducibleBeforeAfterReportForTheCalibratedAvailabilityFactors() throws IOException {
        GeneratorConfiguration current = generatorProperties.configuration();
        assertThat(current.configurationVersion()).isEqualTo("2026-08-22.1");
        assertThat(current.availabilityFactors().get(Availability.EASY)).isEqualByComparingTo("1.00");
        assertThat(current.availabilityFactors().get(Availability.PLANNED)).isEqualByComparingTo("0.45");
        assertThat(current.availabilityFactors().get(Availability.DIFFICULT)).isEqualByComparingTo("0.03");
        assertThat(current.availabilityFactors().get(Availability.UNAVAILABLE)).isEqualByComparingTo("0.00");

        GeneratorConfiguration previous = withAvailabilityFactors(current, "2026-08-15.1", "0.65", "0.20");
        SimulationRequest request = calibrationRequest();
        CalibrationRun before = run(previous, request);
        CalibrationRun after = run(current, request);
        CalibrationRun firstReproduction = run(current, reproducibilityRequest());
        CalibrationRun repeatedAfter = run(current, reproducibilityRequest());

        assertThat(before.report().completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(after.report().completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(firstReproduction.canonicalDocument()).isEqualTo(repeatedAfter.canonicalDocument());
        assertThat(after.report().metrics().hardRuleViolations()).isZero();
        assertThat(after.report().metrics().replayIntegrityMismatches()).isZero();
        assertThat(after.report().metrics().technicalErrors()).isZero();
        assertThat(after.randomRequirementsByAvailability().getOrDefault("PLANNED", 0L))
                .isLessThanOrEqualTo(before.randomRequirementsByAvailability().getOrDefault("PLANNED", 0L));
        assertThat(after.randomRequirementsByAvailability().getOrDefault("DIFFICULT", 0L))
                .isLessThanOrEqualTo(before.randomRequirementsByAvailability().getOrDefault("DIFFICULT", 0L));

        Map<String, Object> document = new TreeMap<>();
        document.put("calibration", Map.of(
                "previousConfigurationVersion", previous.configurationVersion(),
                "currentConfigurationVersion", current.configurationVersion(),
                "generatorVersion", current.generatorVersion(),
                "changedFactors", Map.of("EASY", "1.00", "PLANNED", "0.45", "DIFFICULT", "0.03",
                        "UNAVAILABLE", "0.00"),
                "unchangedPreviousFactors", Map.of("EASY", "1.00", "PLANNED", "0.65", "DIFFICULT", "0.20",
                        "UNAVAILABLE", "0.00")));
        document.put("method", Map.of(
                "scenarioVersion", SCENARIO_VERSION,
                "seeds", "152000001, 152000002, 152000003, 152000004",
                "seasonalDates", SEASONAL_DATES.stream().map(LocalDate::toString).toList(),
                "historyScenarios", List.of("NEUTRAL_HISTORY", "RECOVERY_AFTER_ADVENTUROUS",
                        "SEEKING_AFTER_THREE_FAMILIAR"),
                "restrictionMode", "AUTO",
                "plannedCasesPerVariant", request.plannedCases(),
                "catalogRows", jdbcTemplate.queryForObject("select count(*) from ingredient_concept", Integer.class)));
        document.put("before", before.canonicalDocument());
        document.put("after", after.canonicalDocument());
        Files.createDirectories(OUTPUT.getParent());
        Files.write(OUTPUT, CanonicalSetFingerprint.canonicalBytes(document));
    }

    private CalibrationRun run(GeneratorConfiguration configuration, SimulationRequest request) {
        String snapshot = new CanonicalConfigurationSnapshot(objectMapper).serialize(configuration);
        CandidateProposalEngine proposalEngine = new DefaultCandidateProposalEngine(configuration, snapshot);
        CandidateReservoirEngine reservoirEngine = new DefaultCandidateReservoirEngine(proposalEngine);
        RecordingSetEngine setEngine = new RecordingSetEngine(new DefaultCandidateSetEngine(reservoirEngine, objectMapper));
        GeneratorSimulation simulation = new GeneratorSimulationService(catalogProjection, generationRepository,
                reservoirEngine, setEngine, new GeneratorProperties(configuration), transactionManager);
        SimulationReport report = simulation.simulate(request);
        return CalibrationRun.from(report, setEngine.primarySets());
    }

    private static SimulationRequest calibrationRequest() {
        List<SimulationScenario> scenarios = List.of(
                scenario("NEUTRAL_WINTER", 152_000_001L, LocalDate.of(2026, 2, 12), HistoryScenario.NEUTRAL_HISTORY),
                scenario("RECOVERY_SPRING", 152_000_002L, LocalDate.of(2026, 5, 14), HistoryScenario.RECOVERY_AFTER_ADVENTUROUS),
                scenario("SEEKING_SUMMER", 152_000_003L, LocalDate.of(2026, 8, 13), HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR),
                scenario("NEUTRAL_AUTUMN", 152_000_004L, LocalDate.of(2026, 11, 12), HistoryScenario.NEUTRAL_HISTORY));
        return new SimulationRequest(SCENARIO_VERSION, scenarios, 4, GeneratorSimulation.SimulationControl.unbounded());
    }

    private static SimulationRequest reproducibilityRequest() {
        SimulationScenario scenario = new SimulationScenario("REPRODUCIBILITY", new SeedRange(152_000_001L, 1),
                List.of(SEASONAL_DATES.getFirst()), HistoryScenario.NEUTRAL_HISTORY, AttemptType.INITIAL,
                List.of(), 1, RestrictionMode.AUTO);
        return new SimulationRequest(SCENARIO_VERSION + "_REPRODUCIBILITY", List.of(scenario), 1,
                GeneratorSimulation.SimulationControl.unbounded());
    }

    private static SimulationScenario scenario(String code, long seed, LocalDate date, HistoryScenario history) {
        return new SimulationScenario(code, new SeedRange(seed, 1), List.of(date), history,
                AttemptType.INITIAL, List.of(), 1, RestrictionMode.AUTO);
    }

    private static GeneratorConfiguration withAvailabilityFactors(
            GeneratorConfiguration source, String configurationVersion, String planned, String difficult
    ) {
        return new GeneratorConfiguration(source.generatorVersion(), configurationVersion, source.rngAlgorithm(),
                source.canonicalPayloadVersion(), source.candidateSetSize(), source.reservoirTarget(),
                source.reservoirStrictMinimum(), source.reservoirRelaxedOneMinimum(), source.maximumProposalAttempts(),
                source.weightQuantization(), source.exclusionProbability(), Map.of(
                        Availability.EASY, decimal("1.00"), Availability.PLANNED, decimal(planned),
                        Availability.DIFFICULT, decimal(difficult), Availability.UNAVAILABLE, decimal("0.00")),
                source.cooldown(), source.exclusion(), source.novelty(), source.anchorRoles(), source.supportRoles(),
                source.flavorRoles(), source.profiles(), source.profileWeights(), source.profileSetTargets(),
                source.specificityWeights(), source.specificitySetTargets(), source.cadenceSetTargets(),
                source.scoreWeights(), source.similarityWeights(), source.similarity(), source.selection(),
                source.fallbacks(), source.processingLease());
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static final class RecordingSetEngine implements CandidateSetEngine {
        private final CandidateSetEngine delegate;
        private final List<GeneratedCandidateSet> primarySets = new ArrayList<>();
        private String expectedReplayFingerprint;

        private RecordingSetEngine(CandidateSetEngine delegate) {
            this.delegate = delegate;
        }

        @Override
        public CandidateSetResult generate(
                io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt attempt, int batchNumber
        ) {
            CandidateSetResult result = delegate.generate(attempt, batchNumber);
            if (result instanceof GeneratedCandidateSet generated) {
                if (expectedReplayFingerprint == null) {
                    primarySets.add(generated);
                    expectedReplayFingerprint = generated.fingerprint();
                } else {
                    assertThat(generated.fingerprint()).isEqualTo(expectedReplayFingerprint);
                    expectedReplayFingerprint = null;
                }
            }
            return result;
        }

        private List<GeneratedCandidateSet> primarySets() {
            assertThat(expectedReplayFingerprint).isNull();
            return List.copyOf(primarySets);
        }
    }

    private record CalibrationRun(
            SimulationReport report,
            Map<String, Long> randomRequirementsByAvailability,
            Map<String, Long> candidatesWithAvailability,
            Map<String, Long> actualNoveltyBands,
            Map<String, Long> availabilityWithHighNovelty,
            Map<String, Long> fallbackUsage,
            Map<String, Long> reservoirSizeClasses,
            BigDecimal meanReservoirFill
    ) {
        private static CalibrationRun from(SimulationReport report, List<GeneratedCandidateSet> sets) {
            Map<String, Long> requirements = counts("NO_MAINTAINED_VALUE", "EASY", "PLANNED", "DIFFICULT");
            Map<String, Long> candidates = counts("PLANNED", "DIFFICULT");
            Map<String, Long> bands = counts("FAMILIAR", "BALANCED", "ADVENTUROUS");
            Map<String, Long> highNovelty = counts("PLANNED_WITH_NOVELTY_4_OR_5", "DIFFICULT_WITH_NOVELTY_4_OR_5");
            Map<String, Long> fallbacks = counts("STRICT", "RELAXED_1", "RELAXED_2");
            Map<String, Long> reservoirClasses = counts("LARGE", "MEDIUM", "SMALL", "INSUFFICIENT");
            BigDecimal fillTotal = BigDecimal.ZERO;
            for (GeneratedCandidateSet set : sets) {
                increment(fallbacks, set.fallbackLevel().name());
                increment(reservoirClasses, set.reservoir().sizeClass().name());
                fillTotal = fillTotal.add(BigDecimal.valueOf(set.reservoir().metrics().uniqueAcceptedCandidates())
                        .divide(BigDecimal.valueOf(set.reservoir().context().configuration().reservoirTarget()), 12,
                                RoundingMode.HALF_EVEN));
                for (AcceptedProposal candidate : set.candidates()) {
                    increment(bands, candidate.evaluation().actualNoveltyBand().name());
                    boolean planned = false;
                    boolean difficult = false;
                    for (RequirementSnapshot requirement : candidate.requirements()) {
                        if (requirement.source() != RequirementSource.RANDOM) {
                            continue;
                        }
                        String availability = availability(requirement);
                        increment(requirements, availability);
                        if (availability.equals("PLANNED")) {
                            planned = true;
                            if (requirement.concept().noveltyLevel() >= 4) {
                                increment(highNovelty, "PLANNED_WITH_NOVELTY_4_OR_5");
                            }
                        }
                        if (availability.equals("DIFFICULT")) {
                            difficult = true;
                            if (requirement.concept().noveltyLevel() >= 4) {
                                increment(highNovelty, "DIFFICULT_WITH_NOVELTY_4_OR_5");
                            }
                        }
                    }
                    if (planned) {
                        increment(candidates, "PLANNED");
                    }
                    if (difficult) {
                        increment(candidates, "DIFFICULT");
                    }
                }
            }
            BigDecimal meanFill = sets.isEmpty() ? BigDecimal.ZERO : fillTotal.divide(BigDecimal.valueOf(sets.size()),
                    12, RoundingMode.HALF_EVEN);
            return new CalibrationRun(report, ordered(requirements), ordered(candidates), ordered(bands),
                    ordered(highNovelty), ordered(fallbacks), ordered(reservoirClasses), meanFill);
        }

        private Map<String, Object> canonicalDocument() {
            Map<String, Object> value = new TreeMap<>();
            value.put("actualNoveltyBands", randomRequirements(actualNoveltyBands,
                    report.metrics().successfulSets() * 12));
            value.put("availabilityWithNovelty4Or5", randomRequirements(availabilityWithHighNovelty,
                    randomRequirementCount()));
            value.put("candidateAvailability", randomRequirements(candidatesWithAvailability,
                    report.metrics().successfulSets() * 12));
            value.put("completion", Map.of("exhaustedSets", report.metrics().exhaustedSets(),
                    "hardRuleViolations", report.metrics().hardRuleViolations(), "successfulSets",
                    report.metrics().successfulSets(), "technicalErrors", report.metrics().technicalErrors(),
                    "replayIntegrityMismatches", report.metrics().replayIntegrityMismatches()));
            value.put("configuration", Map.of("configurationVersion", report.metadata().configurationVersion(),
                    "configurationFingerprint", report.metadata().configurationFingerprintsByVariant().get("CURRENT"),
                    "catalogFingerprint", report.metadata().runCatalogFingerprint()));
            value.put("fallbackUsage", randomRequirements(fallbackUsage, report.metrics().successfulSets()));
            value.put("randomRequirementsByAvailability", randomRequirements(randomRequirementsByAvailability,
                    randomRequirementCount()));
            value.put("reservoir", Map.of("meanFillRatio", meanReservoirFill, "sizeClasses",
                    randomRequirements(reservoirSizeClasses, report.metrics().successfulSets())));
            return value;
        }

        private long randomRequirementCount() {
            return randomRequirementsByAvailability.values().stream().mapToLong(Long::longValue).sum();
        }

        private static String availability(RequirementSnapshot requirement) {
            return requirement.concept().availabilityByParticipant().values().stream()
                    .max(Comparator.comparingInt(Enum::ordinal)).map(Enum::name).orElse("NO_MAINTAINED_VALUE");
        }

        private static Map<String, Long> counts(String... keys) {
            Map<String, Long> values = new LinkedHashMap<>();
            for (String key : keys) {
                values.put(key, 0L);
            }
            return values;
        }

        private static void increment(Map<String, Long> values, String key) {
            values.merge(key, 1L, Long::sum);
        }

        private static Map<String, Long> ordered(Map<String, Long> values) {
            return Map.copyOf(new TreeMap<>(values));
        }

        private static Map<String, Object> randomRequirements(Map<String, Long> values, long denominator) {
            Map<String, Object> result = new TreeMap<>();
            values.forEach((key, count) -> result.put(key, Map.of("count", count, "share", denominator == 0
                    ? BigDecimal.ZERO : BigDecimal.valueOf(count).divide(BigDecimal.valueOf(denominator), 12,
                            RoundingMode.HALF_EVEN))));
            return result;
        }
    }
}
