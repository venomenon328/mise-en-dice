package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.GeneratorConcept;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyCadence;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Complete immutable input for one deterministic proposal calculation. */
public record GenerationContext(
        AttemptType attemptType,
        LocalDate effectiveDate,
        int seasonMonth,
        CatalogGeneratorSnapshot catalog,
        VisibleHistorySnapshot visibleHistory,
        List<ManualRequirement> manualRequirements,
        NoveltyCadence noveltyCadence,
        Map<NoveltyBand, Integer> noveltyTargetDistribution,
        GeneratorConfiguration configuration,
        long attemptSeed,
        int batchNumber,
        RestrictionMode restrictionMode,
        List<PreparedGenerationAttempt.RestrictionRuleEvaluation> restrictionRuleEvaluations
) {
    public GenerationContext {
        if (attemptType == null || effectiveDate == null || catalog == null || visibleHistory == null
                || noveltyCadence == null || configuration == null || restrictionMode == null
                || restrictionRuleEvaluations == null) {
            throw invalid("Generation context fields must not be null");
        }
        if (seasonMonth < 1 || seasonMonth > 12 || seasonMonth != catalog.seasonMonth()) {
            throw invalid("Season month must match the catalog snapshot month");
        }
        if (batchNumber <= 0) {
            throw invalid("batchNumber must be positive");
        }
        manualRequirements = List.copyOf(manualRequirements);
        if (manualRequirements.size() > 2) {
            throw invalid("At most two manual requirements are supported");
        }
        Set<Integer> positions = new HashSet<>();
        for (ManualRequirement manual : manualRequirements) {
            if (!positions.add(manual.position())) {
                throw invalid("Manual requirement positions must be unique");
            }
        }

        noveltyTargetDistribution = Map.copyOf(noveltyTargetDistribution);
        if (!noveltyTargetDistribution.keySet().equals(Set.of(NoveltyBand.values()))
                || noveltyTargetDistribution.values().stream().anyMatch(value -> value == null || value < 0)
                || noveltyTargetDistribution.values().stream().mapToInt(Integer::intValue).sum()
                != configuration.candidateSetSize()) {
            throw invalid("Novelty target distribution must cover all bands and sum to set size");
        }
        restrictionRuleEvaluations = List.copyOf(restrictionRuleEvaluations);
        if (!configuration.generatorVersion().equals("1.2.0")) {
            throw new GeneratorValidationException(GeneratorReasonCode.UNSUPPORTED_GENERATOR_VERSION,
                    "Only generator version 1.2.0 is implemented");
        }
    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }

    public record ManualRequirement(int position, String displayText, GeneratorConcept matchedConcept) {
        public ManualRequirement {
            displayText = displayText == null ? null : Normalizer.normalize(displayText.strip(), Normalizer.Form.NFC);
            if (position < 1 || position > 4 || displayText == null || displayText.isBlank()
                    || displayText.indexOf('\0') >= 0) {
                throw new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST,
                        "Manual requirements need a position from 1 to 4 and non-blank NFC text without null bytes");
            }
        }
    }
}
