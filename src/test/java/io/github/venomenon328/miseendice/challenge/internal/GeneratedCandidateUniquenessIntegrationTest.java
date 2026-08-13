package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.GenerationQueries;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
class GeneratedCandidateUniquenessIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_candidate_uniqueness")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired GenerationCommands commands;
    @Autowired GenerationQueries queries;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanGenerationData() {
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void postgresqlRejectsDuplicateCanonicalSignaturesInsideAGeneratedBatch() {
        var outcome = commands.startNewSession(new StartNewSession(
                LocalDate.of(2026, 8, 13), List.of(), 47_000_001L));
        assertThat(outcome).isInstanceOf(Generated.class);
        Generated generated = (Generated) outcome;
        var candidates = queries.findBatch(generated.attemptId(), 1).orElseThrow().candidates();
        assertThat(candidates).hasSize(12);

        var first = candidates.get(0);
        var second = candidates.get(1);
        assertThat(first.canonicalSignature()).isNotEqualTo(second.canonicalSignature());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update challenge_candidate set canonical_signature = ? where id = ?",
                first.canonicalSignature(), second.candidateId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
