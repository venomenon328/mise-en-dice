package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.HistoryScenario;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewResult;
import io.github.venomenon328.miseendice.challenge.api.GeneratorLaboratory.PreviewSuccess;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.SeedSource;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(classes = { MiseEnDiceApplication.class, GeneratorLaboratoryIntegrationTest.SeedConfiguration.class })
@Testcontainers
class GeneratorLaboratoryIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 13);

    @Container static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_generator_laboratory")
            .withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GeneratorLaboratory laboratory;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired FixedSeedSource fixedSeedSource;

    @BeforeEach void reset() {
        fixedSeedSource.reset();
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test void previewIsDeterministicAndWritesNothing() {
        PreviewRequest request = new PreviewRequest(AttemptType.INITIAL, DATE, 37_000_001L,
                List.of(), HistoryScenario.EMPTY_HISTORY, List.of());
        PreviewSuccess first = success(laboratory.preview(request));
        PreviewSuccess second = success(laboratory.preview(request));

        assertThat(second.generatedSet()).isEqualTo(first.generatedSet());
        assertThat(first.generatedSet().candidates()).hasSize(12);
        assertThat(first.pairEvidence()).hasSize(66);
        assertThat(first.pairEvidence().getFirst().assessment())
                .isEqualTo(first.generatedSet().evaluation().pairs().getFirst());
        assertThat(counts()).containsExactly(0, 0, 0, 0);
        assertThat(fixedSeedSource.calls()).isZero();
    }

    @Test void autoSeedAndSyntheticCadenceRemainDiagnosticOnly() {
        PreviewSuccess auto = success(laboratory.preview(new PreviewRequest(AttemptType.INITIAL, DATE, null,
                List.of(), HistoryScenario.EMPTY_HISTORY, List.of())));
        PreviewSuccess recovery = success(laboratory.preview(new PreviewRequest(AttemptType.INITIAL, DATE,
                37_000_011L, List.of(), HistoryScenario.RECOVERY_AFTER_ADVENTUROUS, List.of())));
        PreviewSuccess seeking = success(laboratory.preview(new PreviewRequest(AttemptType.INITIAL, DATE,
                37_000_012L, List.of(), HistoryScenario.SEEKING_AFTER_THREE_FAMILIAR, List.of())));

        assertThat(auto.metadata().seed()).isEqualTo(FixedSeedSource.SEED);
        assertThat(fixedSeedSource.calls()).isEqualTo(1);
        assertThat(recovery.preparedAttempt().noveltyCadence()).isEqualTo(NoveltyCadence.RECOVERY);
        assertThat(seeking.preparedAttempt().noveltyCadence()).isEqualTo(NoveltyCadence.SEEKING_VARIETY);
        assertThat(counts()).containsExactly(0, 0, 0, 0);
    }

    @Test void rerollPreviewIgnoresLegacyHardBlockInputsWithoutCreatingAReroll() {
        List<Long> ids = jdbcTemplate.queryForList("select id from ingredient_concept "
                + "where active and random_draw_enabled order by code, id limit 4", Long.class);
        PreviewSuccess result = success(laboratory.preview(new PreviewRequest(AttemptType.REROLL, DATE,
                37_000_021L, List.of(), HistoryScenario.EMPTY_HISTORY, ids)));

        assertThat(result.preparedAttempt().request().rerollBlockedConceptCodes()).isEmpty();
        assertThat(counts()).containsExactly(0, 0, 0, 0);
    }

    private PreviewSuccess success(PreviewResult result) {
        assertThat(result).isInstanceOf(PreviewSuccess.class);
        return (PreviewSuccess) result;
    }

    private List<Integer> counts() {
        return List.of(count("challenge_session"), count("generation_attempt"), count("generation_batch"), count("challenge"));
    }
    private int count(String table) { return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class); }

    static final class FixedSeedSource implements SeedSource {
        static final long SEED = 37_777_001L;
        private final AtomicInteger calls = new AtomicInteger();
        public long nextSeed() { calls.incrementAndGet(); return SEED; }
        int calls() { return calls.get(); }
        void reset() { calls.set(0); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SeedConfiguration {
        @Bean @Primary FixedSeedSource fixedSeedSource() { return new FixedSeedSource(); }
    }
}
