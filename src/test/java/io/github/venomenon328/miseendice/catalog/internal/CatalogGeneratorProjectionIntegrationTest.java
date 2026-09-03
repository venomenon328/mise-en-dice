package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.SessionParticipant;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import java.math.BigDecimal;
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
class CatalogGeneratorProjectionIntegrationTest {
    private static final String TEST_PREFIX = "TEST_GENERATOR_PROJECTION_";

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
    private CatalogGeneratorProjection projection;

    @Autowired
    private CandidateProposalEngine proposalEngine;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeTestRows() {
        jdbcTemplate.update("""
                delete from exclusion_rule_target
                where exclusion_rule_id in (select id from exclusion_rule where code like ?)
                   or ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%", TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_culinary_dimension
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_culinary_flag
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_functional_role
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_availability
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                   or participant_id in (select id from participant where code like ?)
                """, TEST_PREFIX + "%", TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_seasonality
                where ingredient_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%");
        jdbcTemplate.update("""
                delete from ingredient_refinement
                where parent_concept_id in (select id from ingredient_concept where code like ?)
                   or child_concept_id in (select id from ingredient_concept where code like ?)
                """, TEST_PREFIX + "%", TEST_PREFIX + "%");
        jdbcTemplate.update("delete from exclusion_rule where code like ?", TEST_PREFIX + "%");
        jdbcTemplate.update("delete from ingredient_concept where code like ?", TEST_PREFIX + "%");
        jdbcTemplate.update("delete from participant where code like ?", TEST_PREFIX + "%");
    }

    @Test
    void projectsEveryCatalogConceptIncludingManualOnlyConcepts() {
        long firstParticipant = insertParticipant("FIRST");
        long secondParticipant = insertParticipant("SECOND");
        long drawable = insertConcept("DRAWABLE", true, true, "SPECIFIC", 3);
        long manualOnly = insertConcept("MANUAL_ONLY", false, false, "OPEN", null);
        insertRole(drawable, "VEGETABLE");
        insertAvailability(drawable, firstParticipant, "EASY");
        insertAvailability(drawable, secondParticipant, "SPECIALTY");

        var snapshot = projection.snapshotForMonth(8, participants(firstParticipant, secondParticipant));

        assertThat(snapshot.concepts()).extracting(GeneratorConcept::code)
                .contains(TEST_PREFIX + "DRAWABLE", TEST_PREFIX + "MANUAL_ONLY");
        GeneratorConcept drawableConcept = snapshot.conceptByCode(TEST_PREFIX + "DRAWABLE").orElseThrow();
        assertThat(drawableConcept.active()).isTrue();
        assertThat(drawableConcept.randomDrawEnabled()).isTrue();
        assertThat(drawableConcept.functionalRoles()).containsExactly("VEGETABLE");
        assertThat(drawableConcept.availabilityByParticipant())
                .containsEntry(TEST_PREFIX + "FIRST", CatalogGeneratorProjection.Availability.EASY)
                .containsEntry(TEST_PREFIX + "SECOND", CatalogGeneratorProjection.Availability.SPECIALTY);
        assertThat(snapshot.conceptByCode(TEST_PREFIX + "MANUAL_ONLY")).isPresent();
        assertThat(snapshot.concepts()).isSortedAccordingTo(GeneratorConcept.CANONICAL_ORDER);
        assertThat(snapshot.activeParticipantCodes())
                .containsExactly(TEST_PREFIX + "FIRST", TEST_PREFIX + "SECOND");
    }

    @Test
    void resolvesSeasonGraphPropertiesAndExpandedExclusionsInBulkSnapshot() {
        long participant = insertParticipant("GRAPH");
        long parent = insertConcept("PARENT", false, false, "OPEN", null);
        long child = insertConcept("CHILD", true, true, "SPECIFIC", 2);
        long noSeason = insertConcept("NO_SEASON", false, false, "SPECIFIC", null);
        insertRole(child, "VEGETABLE");
        insertAvailability(child, participant, "EASY");
        jdbcTemplate.update("""
                insert into ingredient_refinement (parent_concept_id, child_concept_id) values (?, ?)
                """, parent, child);
        jdbcTemplate.update("""
                insert into ingredient_culinary_flag (ingredient_concept_id, culinary_flag_id)
                select ?, id from culinary_flag where code = 'FERMENTED'
                """, child);
        jdbcTemplate.update("""
                insert into ingredient_culinary_dimension (ingredient_concept_id, culinary_dimension_id, level)
                select ?, id, 5 from culinary_dimension where code = 'SALTINESS'
                """, child);
        long rule = jdbcTemplate.queryForObject("""
                insert into exclusion_rule (code, display_text, base_draw_weight)
                values (?, ?, 1.0000)
                returning id
                """, Long.class, TEST_PREFIX + "RULE", "Test exclusion rule");
        jdbcTemplate.update("""
                insert into exclusion_rule_target (exclusion_rule_id, ingredient_concept_id, include_refinements)
                values (?, ?, true)
                """, rule, parent);

        var snapshot = projection.snapshotForMonth(8, participants(participant));

        assertThat(snapshot.conceptByCode(TEST_PREFIX + "NO_SEASON").orElseThrow().seasonMultiplier())
                .isEqualByComparingTo(BigDecimal.ONE);
        GeneratorConcept childConcept = snapshot.conceptByCode(TEST_PREFIX + "CHILD").orElseThrow();
        assertThat(childConcept.directAncestorCodes()).containsExactly(TEST_PREFIX + "PARENT");
        assertThat(childConcept.culinaryFlags()).containsExactly("FERMENTED");
        assertThat(childConcept.culinaryDimensions()).containsEntry("SALTINESS", 5);
        var exclusion = snapshot.exclusionRules().stream()
                .filter(candidate -> candidate.code().equals(TEST_PREFIX + "RULE"))
                .findFirst()
                .orElseThrow();
        assertThat(exclusion.targets()).singleElement().satisfies(target -> {
            assertThat(target.conceptCode()).isEqualTo(TEST_PREFIX + "PARENT");
            assertThat(target.includeRefinements()).isTrue();
        });
        assertThat(exclusion.expandedTargetCodes()).contains(TEST_PREFIX + "PARENT", TEST_PREFIX + "CHILD");
    }

    @Test
    void springBindsAndValidatesTheVersionedGeneratorConfiguration() {
        var descriptor = proposalEngine.descriptor();

        assertThat(descriptor.generatorVersion()).isEqualTo("1.2.0");
        assertThat(descriptor.configurationVersion()).isEqualTo("2026-09-03.1");
        assertThat(descriptor.canonicalConfigurationSnapshot())
                .contains("PLANNED", "0.45", "SPECIALTY", "0.15", "DIFFICULT", "0.03");
        assertThat(descriptor.canonicalConfigurationSnapshot()).contains(
                "candidateSetSize", "scoreWeights", "SPLITMIX64_V1");
    }

    @Test
    void projectsOnlyMaintainedAvailabilityForTheFixedSessionElectorate() {
        long maintainedParticipantId = insertParticipant("MAINTAINED");
        long sparseParticipantId = insertParticipant("SPARSE");
        long conceptId = insertConcept("SPARSE_AVAILABILITY", true, true, "SPECIFIC", 2);
        insertRole(conceptId, "VEGETABLE");
        insertAvailability(conceptId, maintainedParticipantId, "EASY");

        var snapshot = projection.snapshotForMonth(8, List.of(
                new SessionParticipant(maintainedParticipantId, TEST_PREFIX + "MAINTAINED"),
                new SessionParticipant(sparseParticipantId, TEST_PREFIX + "SPARSE")));

        assertThat(snapshot.activeParticipantCodes())
                .containsExactly(TEST_PREFIX + "MAINTAINED", TEST_PREFIX + "SPARSE");
        assertThat(snapshot.conceptByCode(TEST_PREFIX + "SPARSE_AVAILABILITY").orElseThrow()
                .availabilityByParticipant())
                .containsKey(TEST_PREFIX + "MAINTAINED")
                .doesNotContainKey(TEST_PREFIX + "SPARSE");
    }

    private long insertParticipant(String suffix) {
        return jdbcTemplate.queryForObject("""
                insert into participant (code, display_name) values (?, ?) returning id
                """, Long.class, TEST_PREFIX + suffix, "Projection " + suffix);
    }

    private long insertConcept(String suffix, boolean active, boolean drawable, String specificity, Integer novelty) {
        return jdbcTemplate.queryForObject("""
                insert into ingredient_concept (
                    code, display_name, active, random_draw_enabled, challenge_specificity, base_draw_weight,
                    novelty_level, curator_note
                ) values (?, ?, ?, ?, ?, 1.0000, ?, 'Technische Testnotiz.')
                returning id
                """, Long.class, TEST_PREFIX + suffix, "Projection " + suffix, active, drawable, specificity, novelty);
    }

    private void insertRole(long conceptId, String roleCode) {
        jdbcTemplate.update("""
                insert into ingredient_functional_role (ingredient_concept_id, functional_role_id)
                select ?, id from functional_role where code = ?
                """, conceptId, roleCode);
    }

    private void insertAvailability(long conceptId, long participantId, String level) {
        jdbcTemplate.update("""
                insert into ingredient_availability (ingredient_concept_id, participant_id, availability_level)
                values (?, ?, ?)
                """, conceptId, participantId, level);
    }

    private List<SessionParticipant> participants(long... participantIds) {
        return java.util.Arrays.stream(participantIds)
                .mapToObj(id -> new SessionParticipant(
                        id,
                        jdbcTemplate.queryForObject("select code from participant where id = ?", String.class, id)
                ))
                .sorted(java.util.Comparator.comparing(SessionParticipant::participantCode)
                        .thenComparingLong(SessionParticipant::participantId))
                .toList();
    }
}
