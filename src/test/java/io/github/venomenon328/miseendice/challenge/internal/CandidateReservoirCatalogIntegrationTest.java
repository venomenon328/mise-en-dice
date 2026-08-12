package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine.GeneratedReservoir;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RequirementSource;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleChallenge;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot.VisibleRequirement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class CandidateReservoirCatalogIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private CatalogGeneratorProjection catalogProjection;

    @Autowired
    private CandidateReservoirEngine reservoirEngine;

    @Autowired
    private GeneratorProperties generatorProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void representativeMonthsAndSeedsReplayCompleteReservoirsFromThePostgresqlCatalog() {
        List<Scenario> scenarios = List.of(
                new Scenario(1, -8_120_001L),
                new Scenario(6, 35_000_036L),
                new Scenario(12, Long.MIN_VALUE + 2_026L));
        int attemptsBefore = count("generation_attempt");
        int candidatesBefore = count("challenge_candidate");

        for (Scenario scenario : scenarios) {
            var request = request(scenario.month(), scenario.seed(), VisibleHistorySnapshot.empty());
            var prepared = reservoirEngine.prepare(request);
            var first = reservoirEngine.generate(prepared, 1);
            var replay = reservoirEngine.generate(reservoirEngine.prepare(request), 1);

            assertThat(first).as("month %s seed %s", scenario.month(), scenario.seed())
                    .isInstanceOf(GeneratedReservoir.class)
                    .isEqualTo(replay);
            assertThat(first.candidates()).hasSize(144);
            assertThat(first.candidates()).extracting(candidate -> candidate.canonicalSignature())
                    .doesNotHaveDuplicates();
            assertThat(first.metrics().proposalAttempts()).isBetween(144, 5_000);
            assertThat(first.metrics().acceptedProposalHits())
                    .isEqualTo(first.metrics().uniqueAcceptedCandidates() + first.metrics().duplicateHits());
        }

        assertThat(count("generation_attempt")).isEqualTo(attemptsBefore);
        assertThat(count("challenge_candidate")).isEqualTo(candidatesBefore);
    }

    @Test
    void recoveryAfterVisibleLevelFiveBlocksEveryRandomLevelFiveConcept() {
        VisibleHistorySnapshot history = new VisibleHistorySnapshot(List.of(new VisibleChallenge(
                Instant.parse("2026-08-01T12:00:00Z"), "visible-level-five", AttemptType.INITIAL, "COMPLETED",
                IntStream.range(0, 4).mapToObj(index -> new VisibleRequirement(
                        "HISTORY_" + index, index == 0 ? 5 : 1, Set.of("VEGETABLE"), Set.of(), Set.of())).toList(),
                CandidateProfile.FLEXIBLE_BALANCED, NoveltyBand.ADVENTUROUS, null)));

        var prepared = reservoirEngine.prepare(request(8, 8_555_008L, history));
        var result = reservoirEngine.generate(prepared, 1);

        assertThat(prepared.noveltyCadence()).isEqualTo(NoveltyCadence.RECOVERY);
        assertThat(result).isInstanceOf(GeneratedReservoir.class);
        assertThat(result.candidates()).flatExtracting(candidate -> candidate.requirements())
                .filteredOn(requirement -> requirement.source() == RequirementSource.RANDOM)
                .allSatisfy(requirement -> assertThat(requirement.concept().noveltyLevel()).isNotEqualTo(5));
    }

    private GenerationAttemptRequest request(int month, long seed, VisibleHistorySnapshot history) {
        return new GenerationAttemptRequest(AttemptType.INITIAL, LocalDate.of(2026, month, 12), month,
                catalogProjection.snapshotForMonth(month), history, List.of(), Set.of(),
                generatorProperties.configuration(), seed);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private record Scenario(int month, long seed) {
    }
}
