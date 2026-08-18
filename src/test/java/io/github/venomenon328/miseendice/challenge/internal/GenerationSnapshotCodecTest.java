package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Availability;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.Specificity;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GenerationSnapshotCodecTest {

    @Test
    void decodesHistoricGenerator11ContextWithoutIntroducingCandidateRestrictionFields() {
        ObjectMapper mapper = new ObjectMapper();
        VersionedCandidateReservoirEngine reservoir = new VersionedCandidateReservoirEngine(mapper);
        GenerationSnapshotCodec codec = new GenerationSnapshotCodec(mapper, reservoir,
                TestGeneratorConfiguration.candidateRestrictionDefaults());
        var catalog = CandidateSetTestData.catalog(List.of(
                CandidateSetTestData.concept(1, "A", Specificity.SPECIFIC, 1, Set.of("VEGETABLE"), Set.of(),
                        Map.of(), Set.of(), Set.of(), Availability.EASY),
                CandidateSetTestData.concept(2, "B", Specificity.OPEN, 2, Set.of("ACID"), Set.of(),
                        Map.of(), Set.of(), Set.of(), Availability.EASY)));
        GenerationAttemptRequest request = new GenerationAttemptRequest(AttemptType.INITIAL,
                LocalDate.of(2026, 8, 18), 8, catalog, VisibleHistorySnapshot.empty(), List.of(), Set.of(),
                TestGeneratorConfiguration.defaults(), 83_001L);

        var prepared = reservoir.prepare(request);
        GenerationSnapshotCodec.EncodedContext encoded = codec.encode(request, prepared);
        GenerationSnapshotCodec.StoredContext stored = new GenerationSnapshotCodec.StoredContext(
                encoded.configurationSnapshot(), encoded.catalogSnapshot(), encoded.requestSnapshot(),
                encoded.visibleHistorySnapshot(), encoded.preparedAttemptSnapshot(), encoded.contextFingerprint(),
                encoded.configurationFingerprint(), encoded.catalogFingerprint(), encoded.requestFingerprint(),
                encoded.historyFingerprint());

        assertThat(encoded.requestSnapshot()).doesNotContain("restrictionMode");
        assertThat(encoded.preparedAttemptSnapshot()).contains("exclusionDecision").doesNotContain("restrictionMode");
        var restored = codec.decodeAndVerify(stored);
        assertThat(restored.request().configuration().generatorVersion()).isEqualTo("1.1.0");
        assertThat(restored.request().attemptSeed()).isEqualTo(prepared.request().attemptSeed());
        assertThat(restored.request().restrictionMode()).isEqualTo(
                io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode.AUTO);
        assertThat(restored.exclusionDecision()).isEqualTo(prepared.exclusionDecision());
        assertThat(restored.exclusionRuleEvaluations()).isEqualTo(prepared.exclusionRuleEvaluations());
    }
}
