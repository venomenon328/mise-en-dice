package io.github.venomenon328.miseendice.challenge.api;

import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.ExhaustedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.GeneratedCandidateSet;
import io.github.venomenon328.miseendice.challenge.api.CandidateSetEngine.PairAssessment;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.AttemptType;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.NoveltyBand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only diagnostic API for generator previews.
 *
 * <p>A preview deliberately bypasses {@link GenerationCommands}: it must never create a challenge session,
 * generation attempt, batch, persisted candidate, or visible challenge exposure.</p>
 */
public interface GeneratorLaboratory {

    String SCENARIO_VERSION = "2026-08-16.1";

    PreviewResult preview(PreviewRequest request);

    List<HistoryScenarioDescriptor> scenarios();

    enum HistoryScenario {
        PRODUCTION_VISIBLE,
        EMPTY_HISTORY,
        NEUTRAL_HISTORY,
        RECOVERY_AFTER_ADVENTUROUS,
        SEEKING_AFTER_THREE_FAMILIAR,
        LOADED_COOLDOWN_HISTORY
    }

    enum PreviewStatus {
        SUCCESS,
        EXHAUSTED
    }

    record HistoryScenarioDescriptor(HistoryScenario code, String version, String displayName, String description) {
    }

    record ManualInput(int position, String displayText, Long matchedConceptId) {
        public ManualInput {
            if (position < 1 || position > 2 || displayText == null || displayText.isBlank()) {
                throw new IllegalArgumentException("Laboratory manuals use positions 1 and 2 and require text");
            }
            displayText = displayText.strip();
            if (matchedConceptId != null && matchedConceptId <= 0) {
                throw new IllegalArgumentException("Matched manual concept ids must be positive");
            }
        }
    }

    record PreviewRequest(
            AttemptType attemptType,
            LocalDate effectiveDate,
            Long explicitSeed,
            List<ManualInput> manualRequirements,
            HistoryScenario historyScenario,
            List<Long> rerollBlockedConceptIds
    ) {
        public PreviewRequest {
            if (attemptType == null || effectiveDate == null || manualRequirements == null
                    || historyScenario == null || rerollBlockedConceptIds == null) {
                throw new IllegalArgumentException("Laboratory preview fields must not be null");
            }
            manualRequirements = List.copyOf(manualRequirements);
            if (manualRequirements.size() > 2) {
                throw new IllegalArgumentException("At most two manual requirements are supported");
            }
            Set<Integer> positions = new LinkedHashSet<>();
            if (manualRequirements.stream().anyMatch(manual -> !positions.add(manual.position()))) {
                throw new IllegalArgumentException("Manual positions must be unique");
            }

            // Compatibility slot for historic v1.0 laboratory payloads. Generator v1.1 uses only visible-history cooldown.
            rerollBlockedConceptIds = List.of();
        }
    }

    sealed interface PreviewResult permits PreviewSuccess, PreviewExhausted {
        PreviewStatus status();
        PreviewMetadata metadata();
        PreparedGenerationAttempt preparedAttempt();
        String rawPreparedAttemptJson();
        String rawSetJson();
    }

    record PreviewMetadata(
            long seed,
            LocalDate effectiveDate,
            int seasonMonth,
            AttemptType attemptType,
            HistoryScenario historyScenario,
            String scenarioVersion,
            String generatorVersion,
            String configurationVersion,
            String rngAlgorithm,
            int canonicalPayloadVersion
    ) {
    }

    record PreviewSuccess(
            PreviewMetadata metadata,
            PreparedGenerationAttempt preparedAttempt,
            GeneratedCandidateSet generatedSet,
            List<PairEvidence> pairEvidence,
            String rawPreparedAttemptJson,
            String rawSetJson
    ) implements PreviewResult {
        public PreviewSuccess {
            pairEvidence = List.copyOf(pairEvidence);
            if (generatedSet == null || generatedSet.candidates().size() != 12 || pairEvidence.size() != 66) {
                throw new IllegalArgumentException("Successful previews require a full generated set and 66 pair diagnostics");
            }
        }

        @Override
        public PreviewStatus status() {
            return PreviewStatus.SUCCESS;
        }

        public Optional<PairEvidence> pair(int firstCandidateNumber, int secondCandidateNumber) {
            int first = Math.min(firstCandidateNumber, secondCandidateNumber);
            int second = Math.max(firstCandidateNumber, secondCandidateNumber);
            return pairEvidence.stream().filter(pair -> pair.firstCandidateNumber() == first
                    && pair.secondCandidateNumber() == second).findFirst();
        }
    }

    record PreviewExhausted(
            PreviewMetadata metadata,
            PreparedGenerationAttempt preparedAttempt,
            ExhaustedCandidateSet exhaustedSet,
            String rawPreparedAttemptJson,
            String rawSetJson
    ) implements PreviewResult {
        public PreviewExhausted {
            if (exhaustedSet == null) {
                throw new IllegalArgumentException("Exhausted previews require the typed exhausted set result");
            }
        }

        @Override
        public PreviewStatus status() {
            return PreviewStatus.EXHAUSTED;
        }
    }

    /** Supplementary explanatory evidence. PairAssessment remains the authoritative similarity calculation. */
    record PairEvidence(
            int firstCandidateNumber,
            int secondCandidateNumber,
            PairAssessment assessment,
            Set<String> sharedRandomConceptCodes,
            Set<String> sharedInformativeAncestorCodes,
            Set<String> sharedRoles,
            Set<String> sharedFlags,
            Set<String> comparableDimensions,
            boolean sameProfile,
            int firstSpecificityCount,
            int secondSpecificityCount,
            NoveltyBand firstNoveltyBand,
            NoveltyBand secondNoveltyBand,
            int firstNoveltyLoad,
            int secondNoveltyLoad,
            BigDecimal firstAvailabilityLoad,
            BigDecimal secondAvailabilityLoad
    ) {
        public PairEvidence {
            if (firstCandidateNumber < 1 || secondCandidateNumber <= firstCandidateNumber || assessment == null) {
                throw new IllegalArgumentException("Pair evidence requires an ordered candidate pair and its assessment");
            }
            sharedRandomConceptCodes = Set.copyOf(sharedRandomConceptCodes);
            sharedInformativeAncestorCodes = Set.copyOf(sharedInformativeAncestorCodes);
            sharedRoles = Set.copyOf(sharedRoles);
            sharedFlags = Set.copyOf(sharedFlags);
            comparableDimensions = Set.copyOf(comparableDimensions);
        }
    }
}
