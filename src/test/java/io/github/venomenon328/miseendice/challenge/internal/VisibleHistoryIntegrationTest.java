package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.MiseEnDiceApplication;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands;
import io.github.venomenon328.miseendice.challenge.api.CurationOrchestrationCommands.OfferReady;
import io.github.venomenon328.miseendice.challenge.api.CurationQueries;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.Generated;
import io.github.venomenon328.miseendice.challenge.api.GenerationCommands.StartNewSession;
import io.github.venomenon328.miseendice.challenge.api.OfferDecisionCommands;
import java.time.LocalDate;
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

@SpringBootTest(classes = {
        MiseEnDiceApplication.class,
        CurationOrchestrationIntegrationTest.OrchestrationTestConfiguration.class
})
@Testcontainers
class VisibleHistoryIntegrationTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
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
    @Autowired GenerationCommands generationCommands;
    @Autowired CurationOrchestrationCommands curation;
    @Autowired CurationQueries curationQueries;
    @Autowired OfferDecisionCommands decisions;
    @Autowired CurationOrchestrationIntegrationTest.ScriptedCuratorClient curator;

    @AfterEach
    void cleanGenerationData() {
        curator.reset();
        jdbcTemplate.update("delete from challenge");
        jdbcTemplate.update("delete from curated_offer_set");
        jdbcTemplate.update("delete from curation_round");
        jdbcTemplate.update("delete from generation_batch");
        jdbcTemplate.update("delete from generation_attempt");
        jdbcTemplate.update("delete from challenge_session");
    }

    @Test
    void confirmedOfferChallengeKeepsItsStableSnapshotsInVisibleHistory() {
        curator.script(CurationOrchestrationIntegrationTest.Script.success(1));
        Generated generated = (Generated) generationCommands.startNewSession(
                new StartNewSession(DATE, List.of(), 76_200_001L, 1));
        assertThat(curation.curate(generated.attemptId())).isInstanceOf(OfferReady.class);
        CurationQueries.OfferSetView offerSet = curationQueries.findOfferSet(generated.attemptId()).orElseThrow();
        decisions.present(new OfferDecisionCommands.PresentOfferSet(offerSet.offerSetId()));
        decisions.confirm(new OfferDecisionCommands.ConfirmOffer(
                offerSet.offerSetId(), offerSet.offers().getFirst().offerId()));

        var visible = repository.visibleHistory();

        assertThat(visible.challengesNewestFirst()).hasSize(1);
        assertThat(visible.challengesNewestFirst().getFirst().requirements())
                .hasSize(4)
                .extracting(requirement -> requirement.conceptCode())
                .doesNotContainNull();
        String snapshotCode = visible.challengesNewestFirst().getFirst().requirements().getFirst().conceptCode();
        Integer snapshotNovelty = visible.challengesNewestFirst().getFirst().requirements().getFirst().noveltyLevel();
        long conceptId = jdbcTemplate.queryForObject("select id from ingredient_concept where code = ?", Long.class, snapshotCode);
        Integer currentNovelty = jdbcTemplate.queryForObject(
                "select novelty_level from ingredient_concept where id = ?", Integer.class, conceptId);
        int changedNovelty = currentNovelty == null || currentNovelty == 5 ? 1 : currentNovelty + 1;
        try {
            jdbcTemplate.update("update ingredient_concept set novelty_level = ? where id = ?", changedNovelty, conceptId);
            assertThat(repository.visibleHistory().challengesNewestFirst().getFirst().requirements().getFirst().noveltyLevel())
                    .isEqualTo(snapshotNovelty);
        } finally {
            jdbcTemplate.update("update ingredient_concept set novelty_level = ? where id = ?", currentNovelty, conceptId);
        }
    }
}
