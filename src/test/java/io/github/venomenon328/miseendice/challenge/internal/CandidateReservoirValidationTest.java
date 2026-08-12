package io.github.venomenon328.miseendice.challenge.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.CandidateProposalEngine;
import io.github.venomenon328.miseendice.challenge.api.GenerationAttemptRequest;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan;
import io.github.venomenon328.miseendice.challenge.api.GenerationPlan.ProjectedDistribution;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorReasonCode;
import io.github.venomenon328.miseendice.challenge.api.GeneratorValidationException;
import io.github.venomenon328.miseendice.challenge.api.VisibleHistorySnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CandidateReservoirValidationTest {

    @Test
    void invalidPreparedContextIsNotMasqueradedAsGenerationExhaustion() {
        CandidateProposalEngine invalidContextProposalEngine = new CandidateProposalEngine() {
            @Override
            public GeneratorDescriptor descriptor() {
                throw new UnsupportedOperationException();
            }

            @Override
            public GenerationPlan validateAndPlan(GenerationContext context) {
                ProjectedDistribution<Integer> emptySpecificity = new ProjectedDistribution<>(Map.of(), Map.of());
                ProjectedDistribution<io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile>
                        emptyProfiles = new ProjectedDistribution<>(Map.of(), Map.of());
                ProjectedDistribution<io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand>
                        emptyNovelty = new ProjectedDistribution<>(Map.of(), Map.of());
                return new GenerationPlan(emptySpecificity, emptyProfiles, emptyNovelty, Set.of(),
                        List.of(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID));
            }

            @Override
            public ProposalResult propose(GenerationContext context, long proposalOrdinal) {
                throw new AssertionError("Invalid contexts must not reach proposal generation");
            }
        };
        DefaultCandidateReservoirEngine engine = new DefaultCandidateReservoirEngine(invalidContextProposalEngine);
        var configuration = TestGeneratorConfiguration.withLimitsAndExclusion(12, 12, 12, "0.00");
        var request = new GenerationAttemptRequest(AttemptType.INITIAL, LocalDate.of(2026, 8, 12), 8,
                new CatalogGeneratorSnapshot(8, List.of(), List.of(), List.of()), VisibleHistorySnapshot.empty(),
                List.of(), Set.of(), configuration, 35L);
        var prepared = engine.prepare(request);

        assertThatThrownBy(() -> engine.generate(prepared, 1))
                .isInstanceOf(GeneratorValidationException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((GeneratorValidationException) exception).reasonCode())
                        .isEqualTo(GeneratorReasonCode.CONTEXT_SNAPSHOT_INVALID));
    }
}
