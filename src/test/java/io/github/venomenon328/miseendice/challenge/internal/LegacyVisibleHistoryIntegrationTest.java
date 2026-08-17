package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
class LegacyVisibleHistoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice_legacy_visible_history")
            .withUsername("mise_en_dice")
            .withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired JdbcGenerationRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanGenerationData() {
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void legacyConfirmedChallengeKeepsItsStableExclusionRuleCodeInVisibleHistory() {
        long sessionId = jdbcTemplate.queryForObject(
                "insert into challenge_session default values returning id", Long.class);
        long attemptId = jdbcTemplate.queryForObject("""
                insert into generation_attempt (
                    challenge_session_id, attempt_type, status, generator_version, completed_at
                ) values (?, 'INITIAL', 'GENERATED', 'legacy-generator', now()) returning id
                """, Long.class, sessionId);
        long batchId = jdbcTemplate.queryForObject("""
                insert into generation_batch (
                    generation_attempt_id, batch_number, status, legacy_migrated
                ) values (?, 1, 'GENERATED', true) returning id
                """, Long.class, attemptId);
        long roundId = jdbcTemplate.queryForObject("""
                insert into curation_round (
                    generation_attempt_id, round_number, curator_model, prompt_version, status, completed_at,
                    legacy_migrated
                ) values (?, 1, 'legacy-model', 'legacy-prompt', 'SELECTED', now(), true) returning id
                """, Long.class, attemptId);
        var exclusion = jdbcTemplate.queryForMap("""
                select id, code, display_text from exclusion_rule order by id limit 1
                """);
        long candidateId = jdbcTemplate.queryForObject("""
                insert into challenge_candidate (
                    generation_batch_id, curation_round_id, candidate_number, exclusion_rule_id,
                    exclusion_text_snapshot, is_selected
                ) values (?, ?, 1, ?, ?, true) returning id
                """, Long.class, batchId, roundId, exclusion.get("id"), exclusion.get("display_text"));
        jdbcTemplate.update("""
                insert into candidate_requirement (
                    candidate_id, position, source, ingredient_concept_id,
                    challenge_specificity_snapshot, display_text_snapshot, concept_code_snapshot,
                    novelty_level_snapshot
                )
                select ?, row_number() over (order by id), 'RANDOM', id,
                       challenge_specificity, display_name, code, novelty_level
                from ingredient_concept
                where active and random_draw_enabled and novelty_level is not null
                order by id limit 4
                """, candidateId);
        jdbcTemplate.update("""
                insert into challenge (generation_attempt_id, selected_candidate_id)
                values (?, ?)
                """, attemptId, candidateId);

        var visible = repository.visibleHistory();

        assertThat(visible.challengesNewestFirst()).hasSize(1);
        assertThat(visible.challengesNewestFirst().getFirst().exclusionRuleCode())
                .isEqualTo(exclusion.get("code"));
        assertThat(visible.challengesNewestFirst().getFirst().requirements())
                .hasSize(4)
                .extracting(requirement -> requirement.conceptCode())
                .doesNotContainNull();
    }
}
