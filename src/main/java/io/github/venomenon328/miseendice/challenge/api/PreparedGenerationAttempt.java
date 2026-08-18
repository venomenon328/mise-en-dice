package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorExclusionRule;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Attempt-wide immutable decisions reused by every internal batch. */
public record PreparedGenerationAttempt(
        GenerationAttemptRequest request,
        NoveltyCadence noveltyCadence,
        Map<NoveltyBand, Integer> baselineNoveltyTargets,
        List<RestrictionRuleEvaluation> restrictionRuleEvaluations,
        List<GeneratorReasonCode> diagnostics
) {
    public PreparedGenerationAttempt {
        if (request == null || noveltyCadence == null || baselineNoveltyTargets == null
                || restrictionRuleEvaluations == null || diagnostics == null) {
            throw new IllegalArgumentException("Prepared attempt fields must not be null");
        }
        EnumMap<NoveltyBand, Integer> targets = new EnumMap<>(NoveltyBand.class);
        targets.putAll(baselineNoveltyTargets);
        if (!targets.keySet().equals(java.util.Set.of(NoveltyBand.values()))
                || targets.values().stream().anyMatch(value -> value == null || value < 0)
                || targets.values().stream().mapToInt(Integer::intValue).sum()
                != request.configuration().candidateSetSize()) {
            throw new IllegalArgumentException("Baseline novelty targets must cover all bands and sum to set size");
        }
        baselineNoveltyTargets = Collections.unmodifiableMap(targets);
        restrictionRuleEvaluations = restrictionRuleEvaluations.stream()
                .sorted((left, right) -> GeneratorExclusionRule.CANONICAL_ORDER.compare(left.rule(), right.rule()))
                .toList();
        diagnostics = canonicalDiagnostics(diagnostics);
    }

    private static List<GeneratorReasonCode> canonicalDiagnostics(List<GeneratorReasonCode> source) {
        List<GeneratorReasonCode> ordered = new ArrayList<>(new LinkedHashSet<>(source));
        ordered.sort(java.util.Comparator.comparing(Enum::name));
        return List.copyOf(ordered);
    }

    public record RestrictionRuleEvaluation(
            GeneratorExclusionRule rule,
            BigDecimal repetitionFactor,
            BigDecimal effectiveWeight,
            long quantizedWeight,
            List<GeneratorReasonCode> diagnostics
    ) {
        public RestrictionRuleEvaluation {
            if (rule == null || repetitionFactor == null || effectiveWeight == null || quantizedWeight < 0) {
                throw new IllegalArgumentException("Exclusion rule evaluation fields are invalid");
            }
            diagnostics = canonicalDiagnostics(diagnostics);
        }

        public boolean eligible() {
            return diagnostics.isEmpty() && quantizedWeight > 0;
        }
    }
}
