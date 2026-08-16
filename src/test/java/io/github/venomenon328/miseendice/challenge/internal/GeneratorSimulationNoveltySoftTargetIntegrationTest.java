package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.ExclusionVariant;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SeedRange;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationReport;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation.SimulationScenario;
import java.time.LocalDate;
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

@SpringBootTest(classes = MiseEnDiceApplication.class)
@Testcontainers
class GeneratorSimulationNoveltySoftTargetIntegrationTest {
    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_simulation_novelty")
            .withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GeneratorSimulation simulation;

    @Test
    void normalNoveltyTargetDeviationsRemainSoftSimulationSignals() {
        SimulationScenario scenario = new SimulationScenario(
                "SOFT_NOVELTY",
                new SeedRange(40_410_000L, 1),
                List.of(LocalDate.of(2026, 1, 15)),
                HistoryScenario.EMPTY_HISTORY,
                AttemptType.INITIAL,
                List.of(),
                Set.of(),
                1,
                ExclusionVariant.DEFAULT);
        SimulationReport report = simulation.simulate(new SimulationRequest(
                "ISSUE_40_SOFT_NOVELTY_V1",
                List.of(scenario),
                1,
                GeneratorSimulation.SimulationControl.unbounded()));

        assertThat(report.completion().status()).isEqualTo(GeneratorSimulation.CompletionStatus.COMPLETED);
        assertThat(report.metrics().successfulSets()).isEqualTo(1);
        assertThat(report.metrics().targetNoveltyBandFrequency().entries())
                .isNotEqualTo(report.metrics().actualNoveltyBandFrequency().entries());
        assertThat(report.metrics().quotaViolations()).isZero();
        assertThat(report.metrics().hardRuleViolations()).isZero();
    }
}
