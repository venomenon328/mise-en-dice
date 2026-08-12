package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reachable proposal targets after deterministic projection for a fixed manual context. */
public record GenerationPlan(
        ProjectedDistribution<Integer> specificity,
        ProjectedDistribution<CandidateProfile> profiles,
        ProjectedDistribution<NoveltyBand> novelty,
        Set<GeneratorReasonCode> diagnostics,
        List<GeneratorReasonCode> validationErrors
) {
    public GenerationPlan {
        diagnostics = Set.copyOf(diagnostics);
        validationErrors = List.copyOf(validationErrors);
    }

    public boolean valid() {
        return validationErrors.isEmpty();
    }

    public record ProjectedDistribution<T>(Map<T, BigDecimal> normalizedWeights, Map<T, Integer> setTargets) {
        public ProjectedDistribution {
            normalizedWeights = Map.copyOf(normalizedWeights);
            setTargets = Map.copyOf(setTargets);
        }
    }
}
