package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorSimulation;
import io.github.venomenon328.miseendice.challenge.api.PreparedGenerationAttempt;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CurrentGeneratorContractRegressionTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 18);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void current12CanonicalContextKeepsThePublishedSnapshotShapeAndReplays() {
        GeneratorConfiguration configuration = TestGeneratorConfiguration.defaults();
        Fixture fixture = fixture(configuration);
        GenerationAttemptRequest request = request(configuration);
        PreparedGenerationAttempt prepared = fixture.reservoir().prepare(request);

        GenerationSnapshotCodec.EncodedContext encoded = fixture.codec().encode(request, prepared);

        assertThat(encoded.requestSnapshot()).contains("\"rerollBlockedConceptCodes\":[]");
        assertThat(encoded.preparedAttemptSnapshot()).contains("\"exclusionRuleEvaluations\"")
                .doesNotContain("\"restrictionRuleEvaluations\"");
        assertThat(fixture.codec().decodeAndVerify(stored(encoded))).isEqualTo(prepared);
    }

    @Test
    void frozenConfigurationMustMatchTheCurrentEngineCompletely() {
        GeneratorConfiguration current = TestGeneratorConfiguration.defaults();
        GeneratorConfiguration altered = TestGeneratorConfiguration.withLimits(72, 36, 5_000);
        Fixture currentFixture = fixture(current);
        Fixture alteredFixture = fixture(altered);
        GenerationAttemptRequest alteredRequest = request(altered);
        PreparedGenerationAttempt alteredPrepared = alteredFixture.reservoir().prepare(alteredRequest);
        var alteredContext = alteredFixture.reservoir().contextForBatch(alteredPrepared, 1);

        assertThatThrownBy(() -> currentFixture.proposal().validateAndPlan(alteredContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONTEXT_SNAPSHOT_INVALID");

        GenerationSnapshotCodec.EncodedContext encoded = alteredFixture.codec().encode(alteredRequest, alteredPrepared);
        assertThatThrownBy(() -> currentFixture.codec().decodeAndVerify(stored(encoded)))
                .isInstanceOf(GenerationSnapshotCodec.InvalidContextSnapshotException.class)
                .hasMessageContaining("not the currently supported configuration");
    }

    @Test
    void currentSimulationReportSchemaHasItsOwnVersion() {
        assertThat(GeneratorSimulation.REPORT_VERSION).isEqualTo("2026-08-18.1");
    }

    private Fixture fixture(GeneratorConfiguration configuration) {
        DefaultCandidateProposalEngine proposal = new DefaultCandidateProposalEngine(configuration,
                new CanonicalConfigurationSnapshot(objectMapper).serialize(configuration));
        DefaultCandidateReservoirEngine reservoir = new DefaultCandidateReservoirEngine(proposal);
        return new Fixture(proposal, reservoir, new GenerationSnapshotCodec(objectMapper, reservoir, configuration));
    }

    private GenerationAttemptRequest request(GeneratorConfiguration configuration) {
        var catalog = CandidateSetTestData.catalog(List.of(CandidateSetTestData.concept(
                1L, "VEGETABLE_A", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of(), Map.of(),
                Set.of(), Set.of(), Availability.EASY)));
        return new GenerationAttemptRequest(AttemptType.INITIAL, DATE, 8, catalog, VisibleHistorySnapshot.empty(),
                List.of(), configuration, 97_001L, RestrictionMode.AUTO);
    }

    private static GenerationSnapshotCodec.StoredContext stored(GenerationSnapshotCodec.EncodedContext encoded) {
        return new GenerationSnapshotCodec.StoredContext(encoded.configurationSnapshot(), encoded.catalogSnapshot(),
                encoded.requestSnapshot(), encoded.visibleHistorySnapshot(), encoded.preparedAttemptSnapshot(),
                encoded.contextFingerprint(), encoded.configurationFingerprint(), encoded.catalogFingerprint(),
                encoded.requestFingerprint(), encoded.historyFingerprint());
    }

    private record Fixture(
            DefaultCandidateProposalEngine proposal,
            DefaultCandidateReservoirEngine reservoir,
            GenerationSnapshotCodec codec
    ) {
    }
}
