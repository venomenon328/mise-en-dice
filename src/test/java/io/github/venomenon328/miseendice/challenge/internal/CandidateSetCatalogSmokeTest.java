package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection;
import io.github.venomenon328.miseendice.challenge.api.CandidateReservoirEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
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

@SpringBootTest
@Testcontainers
class CandidateSetCatalogSmokeTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.6")
            .withDatabaseName("mise_en_dice").withUsername("mise_en_dice").withPassword("mise_en_dice");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired CatalogGeneratorProjection catalogProjection;
    @Autowired CandidateReservoirEngine reservoirEngine;
    @Autowired CandidateSetEngine setEngine;
    @Autowired GeneratorProperties generatorProperties;

    @Test
    void realCatalogProducesAndReplaysOneCompleteSelectedSet() {
        var request = new GenerationAttemptRequest(AttemptType.INITIAL, LocalDate.of(2026, 8, 12), 8,
                catalogProjection.snapshotForMonth(8), VisibleHistorySnapshot.empty(), List.of(), Set.of(),
                generatorProperties.configuration(), 47_000_001L);
        var prepared = reservoirEngine.prepare(request);
        var first = setEngine.generate(prepared, 1);
        var replay = setEngine.generate(reservoirEngine.prepare(request), 1);

        assertThat(first).isInstanceOf(GeneratedCandidateSet.class).isEqualTo(replay);
        GeneratedCandidateSet generated = (GeneratedCandidateSet) first;
        assertThat(generated.candidates()).hasSize(12);
        assertThat(generated.evaluation().pairs()).hasSize(66);
    }
}
