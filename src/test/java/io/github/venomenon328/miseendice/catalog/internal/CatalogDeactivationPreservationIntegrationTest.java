package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogCommands;
import io.github.venomenon328.miseendice.catalog.api.CatalogCommands.UpdateIngredientConceptCommand;
import io.github.venomenon328.miseendice.catalog.api.CatalogQueries;
import java.math.BigDecimal;
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

/** Verifies that deactivation is an operational flag change, not a destructive graph/history mutation. */
@SpringBootTest
@Testcontainers
class CatalogDeactivationPreservationIntegrationTest {

    private static final String PREFIX = "TEST_ISSUE11_DEACTIVATE_";
    private static final String ACTOR = "issue11-deactivation-admin";

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
    private CatalogCommands catalogCommands;

    @Autowired
    private CatalogQueries catalogQueries;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from catalog_audit_entry where actor_key = ?", ACTOR);
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, PREFIX + "%", PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", PREFIX + "%");
    }

    @Test
    void deactivationKeepsParentChildEdgesAndHistoricalCandidateReference() {
        long parent = insertConcept("PARENT", "Deactivation parent", "OPEN");
        long concept = insertConcept("TARGET", "Deactivation target", "SPECIFIC");
        long child = insertConcept("CHILD", "Deactivation child", "SPECIFIC");
        jdbcTemplate.update(
                "insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?), (?, ?)",
                parent, concept, concept, child
        );

        long sessionId = jdbcTemplate.queryForObject(
                "insert into challenge_session default values returning id",
                Long.class
        );
        try {
            long attemptId = jdbcTemplate.queryForObject("""
                    insert into generation_attempt (challenge_session_id, attempt_type, generator_version)
                    values (?, 'INITIAL', 'issue11-test')
                    returning id
                    """, Long.class, sessionId);
            long roundId = jdbcTemplate.queryForObject("""
                    insert into curation_round (
                        generation_attempt_id, round_number, curator_model, prompt_version, legacy_migrated
                    ) values (?, 1, 'issue11-test', 'issue11-test', true)
                    returning id
                    """, Long.class, attemptId);
            long batchId = jdbcTemplate.queryForObject("""
                    insert into generation_batch
                        (generation_attempt_id, batch_number, status, legacy_migrated)
                    values (?, 1, 'GENERATED', true)
                    returning id
                    """, Long.class, attemptId);
            long candidateId = jdbcTemplate.queryForObject("""
                    insert into challenge_candidate
                        (generation_batch_id, curation_round_id, candidate_number)
                    values (?, ?, 1)
                    returning id
                    """, Long.class, batchId, roundId);
            jdbcTemplate.update("""
                    insert into candidate_requirement (
                        candidate_id, position, source, ingredient_concept_id,
                        challenge_specificity_snapshot, display_text_snapshot
                    ) values (?, 1, 'RANDOM', ?, 'SPECIFIC', 'Deactivation target')
                    """, candidateId, concept);

            var before = catalogQueries.findConcept(concept).orElseThrow();
            catalogCommands.updateIngredientConcept(new UpdateIngredientConceptCommand(
                    concept,
                    before.version(),
                    before.displayName(),
                    false,
                    before.randomDrawEnabled(),
                    before.challengeSpecificity(),
                    before.baseDrawWeight(),
                    before.noveltyLevel(),
                    before.curatorNote(),
                    ACTOR,
                    false
            ));

            assertThat(catalogQueries.findConcept(concept).orElseThrow().active()).isFalse();
            assertThat(jdbcTemplate.queryForObject("""
                    select count(*)
                    from ingredient_refinement
                    where (parent_concept_id = ? and child_concept_id = ?)
                       or (parent_concept_id = ? and child_concept_id = ?)
                    """, Integer.class, parent, concept, concept, child)).isEqualTo(2);
            assertThat(jdbcTemplate.queryForObject("""
                    select count(*)
                    from candidate_requirement
                    where candidate_id = ?
                      and ingredient_concept_id = ?
                      and challenge_specificity_snapshot = 'SPECIFIC'
                      and display_text_snapshot = 'Deactivation target'
                    """, Integer.class, candidateId, concept)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from catalog_audit_entry where actor_key = ?",
                    Integer.class,
                    ACTOR
            )).isEqualTo(1);
        } finally {
            jdbcTemplate.update("delete from generation_attempt where challenge_session_id = ?", sessionId);
            jdbcTemplate.update("delete from challenge_session where id = ?", sessionId);
        }
    }

    private long insertConcept(String suffix, String displayName, String specificity) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight
                ) values (?, ?, true, false, ?, 1.0000)
                returning id
                """, Long.class, PREFIX + suffix, displayName, specificity);
    }
}
