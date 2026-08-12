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
        if (rerollBlockedConceptCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw invalid("REROLL block codes must be non-blank catalog concept codes");
        }
        rerollBlockedConceptCodes = Set.copyOf(rerollBlockedConceptCodes);
        if (attemptType == AttemptType.INITIAL && !rerollBlockedConceptCodes.isEmpty()) {
            throw invalid("INITIAL request must not contain a REROLL block");
        }
        if (attemptType == AttemptType.REROLL) {
            if (visibleHistory.challengesNewestFirst().isEmpty()) {
                throw invalid("REROLL requires the immediately previous visible challenge");
            }
            Set<String> expectedBlock = new HashSet<>();
            visibleHistory.challengesNewestFirst().getFirst().requirements().stream()
                    .map(VisibleHistorySnapshot.VisibleRequirement::conceptCode)
                    .filter(Objects::nonNull)
                    .forEach(expectedBlock::add);
            if (!rerollBlockedConceptCodes.equals(expectedBlock)) {
                throw invalid("REROLL block must match the catalog-backed requirements of the immediately previous visible challenge");
            }
        }
    }

    private static GeneratorValidationException invalid(String detail) {
        return new GeneratorValidationException(GeneratorReasonCode.INVALID_GENERATION_REQUEST, detail);
    }
}
