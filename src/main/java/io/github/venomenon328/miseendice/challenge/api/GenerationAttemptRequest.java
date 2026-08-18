package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.RestrictionMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable input for a deterministic candidate-restriction generation attempt. */
public record GenerationAttemptRequest(
        AttemptType attemptType,
        LocalDate effectiveDate,
        int seasonMonth,
        CatalogGeneratorSnapshot catalog,
        VisibleHistorySnapshot visibleHistory,
        List<ManualRequirement> manualRequirements,
        GeneratorConfiguration configuration,
        long attemptSeed,
        RestrictionMode restrictionMode
) {
    public GenerationAttemptRequest {
        if (attemptType == null || effectiveDate == null || catalog == null || visibleHistory == null
                || manualRequirements == null || configuration == null
                || restrictionMode == null) {
            throw invalid("Generation attempt request fields must not be null");
        }
        if (seasonMonth < 1 || seasonMonth > 12 || seasonMonth != catalog.seasonMonth()) {
            throw invalid("Season month must match the catalog snapshot month");
        }
        if (manualRequirements.stream().anyMatch(Objects::isNull)) {
            throw invalid("Manual requirements must not contain null entries");
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

    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }
}
