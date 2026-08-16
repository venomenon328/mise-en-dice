package io.github.venomenon328.miseendice.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import java.math.BigDecimal;
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

    @Test
    void projectsTheCompleteCanonicalBaselineIncludingManualOnlyConcepts() {
        var snapshot = projection.snapshotForMonth(8);

        assertThat(snapshot.concepts()).hasSize(698);
        assertThat(snapshot.concepts()).filteredOn(concept -> concept.active() && concept.randomDrawEnabled())
                .hasSize(651);
        assertThat(snapshot.concepts()).filteredOn(concept -> !concept.active() || !concept.randomDrawEnabled())
                .hasSize(47)
                .allSatisfy(concept -> {
                    assertThat(concept.code()).isNotBlank();
                    assertThat(concept.displayName()).isNotBlank();
                    assertThat(concept.specificity()).isNotNull();
                });
        assertThat(snapshot.concepts()).isSortedAccordingTo(GeneratorConcept.CANONICAL_ORDER);
        assertThat(snapshot.activeParticipantCodes()).containsExactly("GEORGIA", "TOBIAS");
        assertThat(snapshot.conceptByCode("FISH_SAUCE").orElseThrow().culinaryDimensions())
                .containsEntry("SALTINESS", 5);
        assertThat(snapshot.concepts()).filteredOn(concept -> concept.active() && concept.randomDrawEnabled())
                .allSatisfy(concept -> {
                    assertThat(concept.functionalRoles()).isNotEmpty();
                    assertThat(concept.noveltyLevel()).isBetween(1, 5);
                    assertThat(concept.availabilityByParticipant()).containsKeys("GEORGIA", "TOBIAS");
                });
    }

    @Test
    void resolvesSeasonGraphPropertiesAndExpandedExclusionsInBulkSnapshot() {
        var snapshot = projection.snapshotForMonth(8);
        String missingSeasonCode = jdbcTemplate.queryForObject("""
                select concept.code
                from ingredient_concept concept
                where not exists (
                    select 1 from ingredient_seasonality season
                    where season.ingredient_concept_id = concept.id and season.month = 8
                )
                order by concept.code, concept.id
                limit 1
                """, String.class);

        assertThat(snapshot.conceptByCode(missingSeasonCode).orElseThrow().seasonMultiplier())
                .isEqualByComparingTo(BigDecimal.ONE);
        assertThat(snapshot.concepts()).anySatisfy(concept -> assertThat(concept.directAncestorCodes()).hasSizeGreaterThan(1));
        assertThat(snapshot.concepts()).anySatisfy(concept -> {
            assertThat(concept.culinaryFlags()).isNotEmpty();
            assertThat(concept.culinaryDimensions()).isNotEmpty();
        });
        assertThat(snapshot.exclusionRules()).hasSize(22);
        assertThat(snapshot.exclusionRules()).anySatisfy(rule -> {
            assertThat(rule.targets()).anyMatch(CatalogGeneratorProjection.GeneratorExclusionTarget::includeRefinements);
            assertThat(rule.expandedTargetCodes().size()).isGreaterThan(rule.targets().size());
        });

        var noBeef = snapshot.exclusionRules().stream()
                .filter(rule -> rule.code().equals("NO_BEEF"))
                .findFirst()
                .orElseThrow();
        assertThat(noBeef.targets()).anySatisfy(target -> {
            assertThat(target.conceptCode()).isEqualTo("VEAL");
            assertThat(target.includeRefinements()).isTrue();
        });
        assertThat(noBeef.expandedTargetCodes())
                .contains("VEAL", "VEAL_CUTLET", "VEAL_LIVER", "VEAL_SHANK", "WHITE_SAUSAGE");
    }

    @Test
    void springBindsAndValidatesTheVersionedGeneratorConfiguration() {
        var descriptor = proposalEngine.descriptor();

        assertThat(descriptor.generatorVersion()).isEqualTo("1.0.0");
        assertThat(descriptor.configurationVersion()).isEqualTo("2026-08-15.1");
        assertThat(descriptor.canonicalConfigurationSnapshot()).contains(
                "candidateSetSize", "scoreWeights", "SPLITMIX64_V1");
    }
}
