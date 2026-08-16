package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.catalog.api.CatalogGeneratorProjection.CatalogGeneratorSnapshot;
import io.github.venomenon328.miseendice.challenge.api.GenerationContext.ManualRequirement;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable input whose attempt-wide decisions have not yet been made. */
public record GenerationAttemptRequest(
        AttemptType attemptType,
        LocalDate effectiveDate,
        int seasonMonth,
        CatalogGeneratorSnapshot catalog,
        VisibleHistorySnapshot visibleHistory,
        List<ManualRequirement> manualRequirements,
        Set<String> rerollBlockedConceptCodes,
        GeneratorConfiguration configuration,
        long attemptSeed
) {
    public GenerationAttemptRequest {
        if (attemptType == null || effectiveDate == null || catalog == null || visibleHistory == null
                || manualRequirements == null || rerollBlockedConceptCodes == null || configuration == null) {
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

        // Generator v1.1 no longer interprets a REROLL as rejection of individual ingredients.
        // Keep the record component so historic v1.0 request snapshots remain structurally readable,
        // but normalize all newly constructed requests to the new no-dedicated-block semantics.
        rerollBlockedConceptCodes = Set.of();
    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }
}
