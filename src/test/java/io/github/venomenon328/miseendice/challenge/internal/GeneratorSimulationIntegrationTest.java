package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ManualInput;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = { MiseEnDiceApplication.class, GeneratorSimulationIntegrationTest.TrackingConfiguration.class })
@Testcontainers
class GeneratorSimulationIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_simulation")
            .withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GeneratorSimulation simulation;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired TrackingCatalogProjection trackingCatalogProjection;
    @Autowired FixedSeedSource fixedSeedSource;
    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired JdbcGenerationRepository repository;
    @Autowired CandidateReservoirEngine reservoirEngine;
    @Autowired GeneratorProperties generatorProperties;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void ciScenarioSetIsReadOnlySequentialAndCanonicallyReproducible() throws IOException {
        SimulationRequest request = new SimulationRequest("ISSUE_53_CI_V1", scenarios(), 8,
                GeneratorSimulation.SimulationControl.unbounded());
        List<Integer> before = operationalCounts();
        trackingCatalogProjection.reset();
        fixedSeedSource.reset();
        SimulationReport first = simulation.simulate(request);
        assertThat(trackingCatalogProjection.months()).containsExactly(8, 9);
        assertThat(fixedSeedSource.calls()).isZero();

        assertThat(first.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(first.completion().plannedCases()).isEqualTo(8);
        assertThat(first.completion().processedCases()).isEqualTo(8);
        assertThat(first.metrics().successfulSets()).isEqualTo(8);
        assertThat(first.metrics().exhaustedSets()).isZero();
        assertThat(first.metrics().technicalErrors()).isZero();
        assertThat(first.metrics().replayChecks()).isEqualTo(8);
        assertThat(first.metrics().replayIntegrityMismatches()).isZero();
        assertThat(first.metrics().hardRuleViolations()).isZero();
        assertThat(first.metrics().quotaViolations()).isZero();
        assertThat(first.metrics().setCapViolations()).isZero();
        assertThat(first.metrics().strictPairMeanViolations()).isZero();
        assertThat(first.metrics().recoveryCadenceViolations()).isZero();
        assertThat(first.metadata().catalogFingerprintsByMonth()).hasSize(2);
        assertThat(operationalCounts()).isEqualTo(before);

        Path output = Path.of("target", "generator-simulation", "ci-scenarios-report.json");
        GeneratorSimulationReportCodec.write(first, output);
        String json = Files.readString(output);
        assertThat(json).contains("canonicalReport", "runCatalogFingerprint", "elapsedMillis", "ISSUE_53_CI_V1");
    }

    @Test void frozenInputsProduceTheSameCanonicalReport() {
        SimulationScenario oneCase = scenario("REPRODUCIBLE", new SeedRange(53_200_001L, 1),
                List.of(LocalDate.of(2026, 8, 20)), HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL,
                List.of(), RestrictionMode.AUTO);
        SimulationRequest request = new SimulationRequest("ISSUE_53_REPRODUCIBLE_V1", List.of(oneCase), 1,
                GeneratorSimulation.SimulationControl.unbounded());
        assertThat(simulation.simulate(request).canonicalFingerprint())
                .isEqualTo(simulation.simulate(request).canonicalFingerprint());
    }

    @Test void caseBoundsFailBeforeAnySimulationWork() {
        SimulationScenario atLimit = scenario("BOUND_4096", new SeedRange(1, 4_096), List.of(LocalDate.of(2026, 8, 20)),
                HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL, List.of(), RestrictionMode.AUTO);
        assertThat(new SimulationRequest("BOUND_V1", List.of(atLimit), 4_096,
                GeneratorSimulation.SimulationControl.unbounded()).plannedCases()).isEqualTo(4_096);

        SimulationScenario overLimit = scenario("BOUND_4097", new SeedRange(1, 4_097),
                List.of(LocalDate.of(2026, 8, 20)), HistoryScenario.EMPTY_HISTORY, AttemptType.INITIAL, List.of(), RestrictionMode.AUTO);
        assertThatThrownBy(() -> new SimulationRequest("BOUND_V1", List.of(overLimit), 4_096,
                GeneratorSimulation.SimulationControl.unbounded()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("4096-case bound");
    }

    @Test void timeoutAndTechnicalFailuresStayExplicitlyIncomplete() {
        SimulationScenario sequence = scenario("TECHNICAL_SEQUENCE", new SeedRange(53_100_001L, 1),
                List.of(LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 27)), HistoryScenario.EMPTY_HISTORY,
                AttemptType.INITIAL, List.of(), RestrictionMode.AUTO);
        SimulationReport timedOut = simulation.simulate(new SimulationRequest("ISSUE_53_TIMEOUT_V1", List.of(sequence),
                2, new GeneratorSimulation.SimulationControl(Instant.now(), () -> false,
                GeneratorSimulation.TechnicalErrorMode.CONTINUE)));
        assertThat(timedOut.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.TIMED_OUT);
        assertThat(timedOut.completion().processedCases()).isZero();
        assertThat(timedOut.completion().skippedCases()).isEqualTo(2);

        CandidateSetEngine failingSetEngine = (prepared, batchNumber) -> {
            throw new IllegalStateException("synthetic test failure");
        };
        GeneratorSimulation failingSimulation = new GeneratorSimulationService(catalogProjection, repository,
                reservoirEngine, failingSetEngine, generatorProperties, transactionManager);
        SimulationReport technical = failingSimulation.simulate(new SimulationRequest("ISSUE_53_TECHNICAL_V1",
                List.of(sequence), 2, GeneratorSimulation.SimulationControl.unbounded()));
        assertThat(technical.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.INCOMPLETE);
        assertThat(technical.completion().processedCases()).isEqualTo(1);
        assertThat(technical.completion().skippedCases()).isEqualTo(1);
        assertThat(technical.metrics().technicalErrors()).isEqualTo(1);
        assertThat(technical.metrics().exhaustedSets()).isZero();
        assertThat(technical.metrics().successfulSets()).isZero();
    }

    private List<SimulationScenario> scenarios() {
        LocalDate august = LocalDate.of(2026, 8, 20);
        return List.of(
                scenario("CI_EMPTY", new SeedRange(53_000_001L, 1), List.of(august), HistoryScenario.EMPTY_HISTORY,
                        AttemptType.INITIAL, List.of(), RestrictionMode.AUTO),
                scenario("CI_VISIBLE_HISTORY", new SeedRange(53_000_010L, 1), List.of(august),
                        HistoryScenario.PRODUCTION_VISIBLE, AttemptType.INITIAL, List.of(), RestrictionMode.AUTO),
                scenario("CI_REROLL", new SeedRange(53_000_060L, 1), List.of(august), HistoryScenario.EMPTY_HISTORY,
                        AttemptType.REROLL, List.of(), RestrictionMode.REQUIRED),
                scenario("CI_ONE_MANUAL", new SeedRange(53_000_070L, 1), List.of(august), HistoryScenario.EMPTY_HISTORY,
                        AttemptType.INITIAL, List.of(new ManualInput(1, "Synthetic manual ingredient", null)),
                        RestrictionMode.AUTO),
                scenario("CI_TWO_MANUALS", new SeedRange(53_000_080L, 1), List.of(august), HistoryScenario.EMPTY_HISTORY,
                        AttemptType.INITIAL, List.of(new ManualInput(1, "Synthetic manual ingredient", null),
                                new ManualInput(2, "Synthetic free-text constraint", null)), RestrictionMode.AUTO),
                scenario("CI_SEQUENCE", new SeedRange(53_000_090L, 1), List.of(LocalDate.of(2026, 9, 3),
                        LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 17)), HistoryScenario.EMPTY_HISTORY,
                        AttemptType.INITIAL, List.of(), RestrictionMode.NONE));
    }

    private static SimulationScenario scenario(
            String code, SeedRange seeds, List<LocalDate> dates, HistoryScenario history, AttemptType attempt,
            List<ManualInput> manuals, RestrictionMode restrictionMode
    ) {
        return new SimulationScenario(code, seeds, dates, history, attempt, manuals, 1, restrictionMode);
    }

    private List<Integer> operationalCounts() {
        return List.of(count("challenge_session"), count("generation_attempt"), count("generation_batch"),
                count("challenge"));
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    static final class FixedSeedSource implements SeedSource {
        private final AtomicInteger calls = new AtomicInteger();

        @Override public long nextSeed() {
            calls.incrementAndGet();
            return 53_999_999L;
        }

        int calls() { return calls.get(); }
        void reset() { calls.set(0); }
    }

    static final class TrackingCatalogProjection implements CatalogGeneratorProjection {
        private final CatalogGeneratorProjection delegate;
        private final List<Integer> months = new ArrayList<>();

        TrackingCatalogProjection(CatalogGeneratorProjection delegate) { this.delegate = delegate; }

        @Override public synchronized CatalogGeneratorSnapshot snapshotForMonth(
                int month, List<CatalogGeneratorProjection.SessionParticipant> sessionParticipants) {
            months.add(month);
            return delegate.snapshotForMonth(month, sessionParticipants);
        }

        synchronized List<Integer> months() { return List.copyOf(months); }
        synchronized void reset() { months.clear(); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TrackingConfiguration {
        @Bean @Primary FixedSeedSource fixedSeedSource() { return new FixedSeedSource(); }

        @Bean @Primary TrackingCatalogProjection trackingCatalogProjection(
                @Qualifier("jdbcCatalogGeneratorProjection") CatalogGeneratorProjection delegate
        ) {
            return new TrackingCatalogProjection(delegate);
        }
    }
}
